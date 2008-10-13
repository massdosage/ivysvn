/*
 * Copyright 2008 Last.fm
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package fm.last.ivy.plugins.svnresolver;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

/**
 * Data access object that performs needed "CRUD" operations in Subversion.
 */
public class SvnDao {

  /**
   * Repository to use to perform read operations.
   */
  private SVNRepository readRepository;

  /**
   * A "cache" of folders known to exist in svn, so we don't have to hit repository to check every time.
   */
  private Set<String> existingFolderPaths = new HashSet<String>();

  /**
   * Constructs a new instance of this class. The passed repository will be used for all "read" operations in
   * subversion. This repository MUST not be used for any commit operations and should preferably not be used outside of
   * this class at all.
   * 
   * @param repository Repository to use for read operations. Needed as SVNKit cannot perform various read operations
   *          while in the process of performing a commit.
   */
  public SvnDao(SVNRepository repository) {
    this.readRepository = repository;
  }

  /**
   * Puts a file into Subversion, does update or add depending on whether file already exists or not.
   * 
   * @param editor An initialised commit editor.
   * @param data File data as a byte array.
   * @param destinationFolder Destination folder in svn.
   * @param fileName File name.
   * @param overwrite Whether existing file should be overwritten or not.
   * @return true if File was updated or added, false if it was ignored (i.e. it already exists and overwrite was
   *         false).
   * @throws SVNException If an error occurs putting the file into Subversion.
   */
  public boolean putFile(ISVNEditor editor, byte[] data, String destinationFolder, String fileName, boolean overwrite)
    throws SVNException {
    String filePath = destinationFolder + "/" + fileName;
    if (fileExists(filePath, -1)) { // updating existing file
      if (overwrite) {
        Message.debug("Updating file " + filePath);
        editor.openFile(filePath, -1);
      } else {
        Message.info("Overwrite set to false, ignoring " + filePath);
        return false;
      }
    } else { // creating new file
      createFolders(editor, destinationFolder, -1);
      Message.debug("Adding file " + filePath);
      editor.addFile(filePath, null, -1);
    }
    editor.applyTextDelta(filePath, null);
    SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
    String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true);
    editor.closeFile(filePath, checksum);
    return true;
  }

  /**
   * Creates any sub folders of the passed folder path and returns the sub folder path.
   * 
   * @param editor An editor opened for performing commits.
   * @param folderPath Path to create sub folders of.
   * @param revision Revision to use.
   * @return The sub folder path or null if there are no subfolders.
   * @throws SVNException If an error occurs creating the sub folders.
   */
  public String createSubFolders(ISVNEditor editor, String folderPath, long revision) throws SVNException {
    int index = folderPath.lastIndexOf("/");
    if (index > 0) {
      String subFolderPath = folderPath.substring(0, index);
      createFolders(editor, subFolderPath, revision);
      return subFolderPath;
    }
    return null;
  }

  /**
   * Creates the passed folder in the repository, existing folders are left alone and only the parts of the path which
   * don't exist are created.
   * 
   * @param editor An editor opened for performing commits.
   * @param folderPath The path of the folder to create, must be relative to the root of the repository.
   * @param revision Revision to use.
   * @throws SVNException If an invalid path is passed or the path could not be created.
   */
  public void createFolders(ISVNEditor editor, String folderPath, long revision) throws SVNException {
    if (!folderPath.startsWith("/")) { // force all paths to be relative to root of repository
      folderPath = "/" + folderPath;
    }
    // run through all directories in path and create whatever is necessary
    String[] folders = folderPath.substring(1).split("/");
    int i = 0;
    StringBuffer existingPath = new StringBuffer("/");
    StringBuffer pathToCheck = new StringBuffer("/" + folders[0]);
    while (folderExists(pathToCheck.toString(), revision, true)) {
      existingPath.append(folders[i] + "/");
      if (++i == folders.length) {
        break;// no more paths to check, entire path exists so break out of here
      } else {
        pathToCheck.append("/" + folders[i]); // add the next part of path
      }
    }

    if (i < folders.length) { // 1 or more dirs need to be created
      // editor.openRoot(-1); // only open root ONCE
      for (; i < folders.length; i++) { // build up path to create
        StringBuffer pathToAdd = new StringBuffer(existingPath);
        if (pathToAdd.charAt(pathToAdd.length() - 1) != '/') { // if we need a separator char
          pathToAdd.append("/");
        }
        pathToAdd.append(folders[i]);
        Message.debug("Creating folder " + pathToAdd);
        editor.addDir(pathToAdd.toString(), null, -1);
        existingPath = pathToAdd; // added to svn so this is new existing path
        existingFolderPaths.add(pathToAdd.toString());
      }
      // editor.closeDir(); // close dir ONCE
    }
  }

  /**
   * Lists the files in the passed folder.
   * 
   * @param folderPath A folder path.
   * @param revision The revision to use.
   * @return A list of file names in the passed folder, this will be empty if the folder does not exist.
   * @throws SVNException If an error occurs listing the contents.
   */
  public List<String> list(String folderPath, long revision) throws SVNException {
    List<String> contents = new ArrayList<String>();
    if (folderExists(folderPath, revision, false)) {
      List<SVNDirEntry> entries = new ArrayList<SVNDirEntry>();
      readRepository.getDir(folderPath, revision, false, entries);
      for (SVNDirEntry entry : entries) {
        contents.add(entry.getRelativePath());
      }
    }
    return contents;
  }

  /**
   * Determines whether the passed folder exists.
   * 
   * @param folderPath Folder path.
   * @param revision Revision to use.
   * @return true if the folder exists, false otherwise.
   * @throws SVNException If an error occurs determining whether the folder exists.
   */
  public boolean folderExists(String folderPath, long revision, boolean useCache) throws SVNException {
    if (useCache && existingFolderPaths.contains(folderPath)) { // first check our cache if this path is known to exist
      return true;
    } else {
      // not previously cached, so check against repository
      SVNNodeKind nodeKind = readRepository.checkPath(folderPath.toString(), revision);
      if (SVNNodeKind.DIR == nodeKind) {
        if (useCache) {
          existingFolderPaths.add(folderPath);
        }
        return true;
      }
      return false;
    }
  }

  /**
   * Determines whether the passed file exists.
   * 
   * @param path File path.
   * @param revision Revision to use.
   * @return true if the file exists, false otherwise.
   * @throws SVNException If an error occurs determining whether the file exists.
   */
  public boolean fileExists(String path, long revision) throws SVNException {
    SVNNodeKind kind = readRepository.checkPath(path, revision);
    if (kind == SVNNodeKind.FILE) {
      return true;
    }
    return false;
  }

  /**
   * Gets a file from the repository.
   * 
   * @param sourceURL The full path to the file, reachable via the read repository.
   * @param destination The destination file.
   * @param revision The subversion revision.
   * @throws SVNException If an error occurs retrieving the file from Subversion.
   * @throws IOException If an error occurs writing the file contents to disk.
   */
  public void getFile(SVNURL sourceURL, File destination, long revision) throws SVNException, IOException {
    readRepository.setLocation(sourceURL, false);
    SVNNodeKind nodeKind = readRepository.checkPath("", revision);
    SVNErrorMessage error = SvnUtils.checkNodeIsFile(nodeKind, sourceURL);
    if (error != null) {
      // TODO: could we just put below message into IOException's message?
      Message.error("Error retrieving" + sourceURL + " [revision=" + revision + "]");
      throw new IOException(error.getMessage());
    }
    BufferedOutputStream output = null;
    try {
      output = new BufferedOutputStream(new FileOutputStream(destination));
      readRepository.getFile("", revision, null, output);
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

}
