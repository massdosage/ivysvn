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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

/**
 * Class that encapsulates actions required to perform a number of Ivy put requests as a single SVN commit. Note: All
 * operations are performed relative to the repository root, this is done to simplify dealing with SVNKit API so we
 * never have to worry about where in the repository we are, always assume relative to root.
 */
public class SvnPublishTransaction {

  /**
   * Repository that can be used to check for file existence and perform other operations without affecting commit
   * editor (required as SVNKit doesn't allow many operations once commit editor is opened for a certain repository, but
   * we need to figure out what files and folders to add/update).
   */
  private SVNRepository ancillaryRepository = null;

  /**
   * Repository to use to perform commit operations.
   */
  private SVNRepository commitRepository = null;

  /**
   * List of Entry objects that need to be added/updated.
   */
  private List<PutOperation> entries = new ArrayList<PutOperation>();

  /**
   * A "cache" of folders known to exist in svn, so we don't have to hit repository to check every time.
   */
  private Set<String> existingFolderPaths = new HashSet<String>();

  /**
   * Svn commit message.
   */
  private String commitMessage;

  /**
   * Editor that to use for performing various operations within a commit transaction.
   */
  private ISVNEditor commitEditor;

  private boolean binaryDiff;

  private String binaryDiffLocation;

  /**
   * Constructs a new instance of this class, which will use the passed message when a commit is performed.
   * 
   * @param commitMessage Commit message.
   * @param binaryDiffLocation
   * @param binaryDiff
   */
  public SvnPublishTransaction(String commitMessage, boolean binaryDiff, String binaryDiffLocation) {
    this.commitMessage = commitMessage;
    this.binaryDiff = binaryDiff;
    this.binaryDiffLocation = binaryDiffLocation;
  }

  /**
   * Set the repository to use for performing non-commit operations. This will have its location set to the repository
   * root and should not be used outside of this class.
   * 
   * @param repository The repository to use for ancillary operations.
   * @throws SVNException If an error occurs setting the repository to its root.
   */
  public void setAncillaryRepository(SVNRepository repository) throws SVNException {
    this.ancillaryRepository = repository;
    SVNURL root = ancillaryRepository.getRepositoryRoot(false);
    ancillaryRepository.setLocation(root, false);
  }

  /**
   * Set the repository to use for performing commit operations. This will have its location set to the repository root
   * and should not be used outside of this class.
   * 
   * @param repository The repository to use for commits.
   * @throws SVNException If an error occurs setting the repository to its root.
   */
  public void setCommitRepository(SVNRepository repository) throws SVNException {
    commitRepository = repository;
    SVNURL commitRoot = commitRepository.getRepositoryRoot(false);
    commitRepository.setLocation(commitRoot, false);
  }

  /**
   * Adds an operation representing a file "put" to the transaction.
   * 
   * @param source The file to put.
   * @param destinationPath The full svn path to the file's location in svn.
   * @param overwrite Whether an overwrite should be performed if the file already exists.
   * @param repositoryPath The SVN base repository path, if null, the base with will be assumed to be the path up to the
   *          first "/" in any svn paths (often the case).
   * @throws IOException If the file data cannot be read from disk or the file paths cannot be determined.
   */
  public void addPutOperation(File source, String destinationPath, boolean overwrite, String repositoryPath)
    throws IOException {
    // NOTE: file contents are stored in memory...
    this.entries.add(new PutOperation(source, destinationPath, overwrite, repositoryPath));
  }

  /**
   * Commits all files scheduled to be put.
   * 
   * @throws SVNException If an error occurs committing the transaction.
   */
  public void commit() throws SVNException {
    // reset the repository to its root and tell it to connect if necessary
    SVNURL root = commitRepository.getRepositoryRoot(true);
    commitRepository.setLocation(root, false);
    commitEditor = commitRepository.getCommitEditor(commitMessage, null);
    commitEditor.openRoot(-1);
    int committedEntryCount = performPutOperations();
    commitEditor.closeDir();
    if (committedEntryCount == 0) {
      commitEditor.abortEdit();
      Message.info("Nothing to commit");
    } else {
      SVNCommitInfo info = commitEditor.closeEdit();
      Message.info("Commit finished " + info);
    }
  }

  /**
   * Performs all necessary put operations. The commitEditor is assumed to be in a valid state before this is called.
   * 
   * @return The number of put operations that were performed.
   * @throws SVNException If an error occurs performing any of the put operations.
   */
  private int performPutOperations() throws SVNException {
    checkCommitEditor();
    int committedEntryCount = 0;
    for (PutOperation entry : entries) {
      String destinationPath = entry.getFullPath();
      boolean ignoreEntry = false;
      if (fileExists(destinationPath)) {
        if (entry.isOverwrite()) {
          Message.info("Updating file " + destinationPath);
          commitEditor.openFile(destinationPath, -1);
        } else {
          Message.info("Overwrite set to false, ignoring " + destinationPath);
          ignoreEntry = true;
        }
      } else {
        createFolders(commitEditor, entry.getFolderPath());
        Message.info("Adding file " + destinationPath);
        commitEditor.addFile(destinationPath, null, -1);
      }
      if (!ignoreEntry) {
        committedEntryCount++;
        commitEditor.applyTextDelta(destinationPath, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
        String checksum = deltaGenerator.sendDelta(destinationPath, new ByteArrayInputStream(entry.getData()),
            commitEditor, true);
        commitEditor.closeFile(destinationPath, checksum);
      }
    }
    return committedEntryCount;
  }

  /**
   * Creates the passed path in the repository, existing folders are left alone and only the parts of the path which
   * don't exist are created.
   * 
   * @param repository An initialised and authenticated SVNRepository.
   * @param directoryPath The directory path to create.
   * @throws SVNException If an invalid path is passed or the path could not be created.
   */
  private void createFolders(ISVNEditor editor, String directoryPath) throws SVNException {
    // run through all directories in path and create whatever is necessary
    String[] folders = directoryPath.substring(1).split("/");
    int i = 0;
    StringBuffer existingPath = new StringBuffer("/");
    StringBuffer pathToCheck = new StringBuffer("/" + folders[0]);
    while (folderExists(pathToCheck.toString())) {
      existingPath.append(folders[i] + "/");
      if (++i == folders.length) {
        break;// no more paths to check, entire path exists so break out of here
      } else {
        pathToCheck.append("/" + folders[i]); // add the next part of path
      }
    }

    if (i < folders.length) { // 1 or more dirs need to be created
      editor.openRoot(-1); // only open root ONCE
      for (; i < folders.length; i++) { // build up path to create
        StringBuffer pathToAdd = new StringBuffer(existingPath);
        if (pathToAdd.charAt(pathToAdd.length() - 1) != '/') { // if we need a separator char
          pathToAdd.append("/");
        }
        pathToAdd.append(folders[i]);
        Message.info("Creating folder " + pathToAdd);
        editor.addDir(pathToAdd.toString(), null, -1);
        existingPath = pathToAdd; // added to svn so this is new existing path
        existingFolderPaths.add(pathToAdd.toString());
      }
      editor.closeDir(); // close dir ONCE
    }
  }

  /**
   * Determines whether the passed folder exists.
   * 
   * @param folderPath Folder path.
   * @return true if the folder exists, false otherwise.
   * @throws SVNException If an error occurs determining whether the folder exists.
   */
  private boolean folderExists(String folderPath) throws SVNException {
    if (existingFolderPaths.contains(folderPath)) { // first check if this path is known to exist
      return true;
    } else {
      // not previously cached, so check against repository
      SVNNodeKind nodeKind = ancillaryRepository.checkPath(folderPath.toString(), -1);
      if (SVNNodeKind.DIR == nodeKind) {
        existingFolderPaths.add(folderPath);
        return true;
      }
      return false;
    }
  }

  /**
   * Determines whether the passed file exists.
   * 
   * @param path File path.
   * @return true if the file exists, false otherwise.
   * @throws SVNException If an error occurs determining whether the file exists.
   */
  private boolean fileExists(String path) throws SVNException {
    SVNNodeKind kind = ancillaryRepository.checkPath(path, -1);
    if (kind == SVNNodeKind.FILE) {
      return true;
    }
    return false;
  }

  /**
   * Abort the transaction.
   * 
   * @throws SVNException if something goes wrong
   */
  public void abort() throws SVNException {
    checkCommitEditor();
    commitEditor.abortEdit();
    commitEditor = null;
  }

  /**
   * Check if a commit editor has been set.
   * 
   * @throws SVNException If the commit editor has not been set.
   */
  private void checkCommitEditor() throws SVNException {
    if (commitEditor == null) {
      throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Commit not initialized"));
    }
  }

  /**
   * @return the rootRepository
   */
  public SVNRepository getAncillaryRepository() {
    return ancillaryRepository;
  }

}
