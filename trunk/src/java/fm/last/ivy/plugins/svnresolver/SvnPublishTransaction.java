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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Class that encapsulates actions required to perform a number of Ivy put requests as a single SVN commit. Note: All
 * operations are performed relative to the repository root, this is done to simplify dealing with SVNKit API so we
 * never have to worry about where in the repository we are, always assume relative to root.
 */
public class SvnPublishTransaction {

  /**
   * Data access object for accessing subversion. Can be used to check for file existence and perform other operations
   * without affecting commit editor (required as SVNKit doesn't allow many operations once commit editor is opened for
   * a certain repository, but we need to figure out what files and folders to add/update).
   */
  private SvnDao svnDAO = null;

  /**
   * Repository to use to perform commit operations.
   */
  private SVNRepository commitRepository = null;

  /**
   * Tree representing the directories and artifacts that need to be added/updated.
   */
  private Directory transactionContent = new Directory("", "/", null);

  /**
   * Svn commit message.
   */
  private String commitMessage;

  /**
   * Editor that to use for performing various operations within a commit transaction.
   */
  private ISVNEditor commitEditor;

  /**
   * Indicates whether a SVN commit has been started or not.
   */
  private boolean commitStarted = false;

  /**
   * Indicates whether a binary diff should be performed or not.
   */
  private boolean binaryDiff = true;

  /**
   * The Ivy module revision string.
   */
  private String revision;

  /**
   * The name of the binary diff folder.
   */
  private String binaryDiffFolderName;

  /**
   * Whether to cleanup the contents of the publish folder during publish.
   */
  private Boolean cleanupPublishFolder = null;

  /**
   * Constructs a new instance of this class.
   * 
   * @param svnDAO The subversion DAO to use.
   * @param mrid The ivy Module Revision ID.
   * @param commitRepository The repository to use for commits. This will have its location set to the repository root
   *          and should not be used outside of this class.
   * @throws SVNException If an error occurs setting the commit repository to its root.
   */
  public SvnPublishTransaction(SvnDao svnDAO, ModuleRevisionId mrid, SVNRepository commitRepository)
      throws SVNException {
    this.svnDAO = svnDAO;
    this.revision = mrid.getRevision();
    setCommitRepository(commitRepository);
    StringBuilder comment = new StringBuilder();
    comment.append("Ivy publishing ").append(mrid.getOrganisation()).append("#");
    comment.append(mrid.getName()).append(";").append(mrid.getRevision());
    this.commitMessage = comment.toString();
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
    // TODO: do we need to do this here as we also seem to do it in commit()?
    SVNURL commitRoot = commitRepository.getRepositoryRoot(false);
    commitRepository.setLocation(commitRoot, false);
  }

  /**
   * Adds an operation representing a file "put" to the transaction.
   * 
   * @param source The file to put.
   * @param destinationPath The full svn path to the file's location in svn.
   * @param overwrite Whether an overwrite should be performed if the file already exists.
   * @throws IOException If the file data cannot be read from disk or the file paths cannot be determined.
   */
  public void addPutOperation(File source, String destinationPath, boolean overwrite) throws SVNException, IOException {
    PutOperation operation = new PutOperation(source, destinationPath, overwrite);

    String destinationFolderPath = operation.getFolderPath();
    if (binaryDiff) { // publishing to intermediate binary diff location, override values set above
      if (!operation.isOverwrite() && svnDAO.folderExists(operation.getFolderPath(), -1, true)) {
        Message.info("Overwrite set to false, ignoring " + operation.getFilePath());
        return;
      }
      destinationFolderPath = operation.determineBinaryDiffFolderPath(revision, binaryDiffFolderName);
      overwrite = true; // force overwrite for binary diff
    }

    if (destinationFolderPath.startsWith("/")) {
      destinationFolderPath = destinationFolderPath.substring(1);
    }
    String[] pathComponents = destinationFolderPath.split("/");
    int pathIndex = 0;
    Directory currentPos = transactionContent;
    while (pathIndex < pathComponents.length) {
      currentPos = currentPos.subdir(pathComponents[pathIndex]);
      pathIndex++;
    }
    currentPos.addFile(operation);
  }

  /**
   * Commits all files scheduled to be put.
   * 
   * @throws SVNException If an error occurs committing the transaction.
   * @throws IOException If an error occurs reading any file data.
   */
  public void commit() throws SVNException, IOException {
    // reset the repository to its root and tell it to connect if necessary
    SVNURL root = commitRepository.getRepositoryRoot(true);
    commitRepository.setLocation(root, false);
    commitEditor = commitRepository.getCommitEditor(commitMessage, null);
    commitStarted = true;
    commitEditor.openRoot(-1);
    int putFileCount = transactionContent.commit();
    if (putFileCount == 0) {
      commitEditor.abortEdit();
      Message.info("Nothing to commit");
    } else {
      Map<String, String> foldersToCopy = prepareBinaryDiff(); // prepare binary diff in existing transaction
      commitEditor.closeDir(); // close root
      SVNCommitInfo info = commitEditor.closeEdit();
      Message.info("Commit finished " + info);
      copyDiff(foldersToCopy);
    }
  }

  /**
   * Performs all necessary put operations. The commitEditor is assumed to be in a valid state before this is called.
   * 
   * @return The number of put operations that were performed.
   * @throws SVNException If an error occurs performing any of the put operations.
   * @throws IOException If an error occurs reading any file data.
   */
  private int performPutOperations(Iterable<PutOperation> putOperations) throws SVNException, IOException {
    checkCommitEditor();
    int putFileCount = 0;
    Map<String, Set<String>> putFiles = new HashMap<String, Set<String>>();
    for (PutOperation operation : putOperations) {
      String destinationFolderPath = operation.getFolderPath(); // assume no binary diff
      boolean overwrite = operation.isOverwrite();
      if (binaryDiff) { // publishing to intermediate binary diff location, override values set above
        if (!operation.isOverwrite() && svnDAO.folderExists(operation.getFolderPath(), -1, true)) {
          Message.info("Overwrite set to false, ignoring " + operation.getFilePath());
          continue;
        }
        destinationFolderPath = operation.determineBinaryDiffFolderPath(revision, binaryDiffFolderName);
        overwrite = true; // force overwrite for binary diff
      }
      // destinationFolderPath and overwrite will be set according to whether binary diff or not
      if (svnDAO.putFile(commitEditor, operation.getData(), destinationFolderPath, operation.getFileName(), overwrite)) {
        putFileCount++;
      }

      Set<String> files = putFiles.get(destinationFolderPath);
      if (files == null) {
        files = new HashSet<String>();
      }
      files.add(operation.getFileName());
      putFiles.put(destinationFolderPath, files); // keep track of each file we have put per folder
    }

    // only clean up if set to AND we are actually going to publish something
    if (cleanupPublishFolder != null && cleanupPublishFolder && putFileCount > 0) {
      cleanupPublishFolder(putFiles);
    }

    return putFileCount;
  }

  /**
   * Deletes any files in the publish folder which are not part of this transaction's set of files to publish.
   * 
   * @param putFiles Files to publish as part of this transaction.
   * @throws SVNException If an error occurs listing the publish folder or deleting files.
   */
  private void cleanupPublishFolder(Map<String, Set<String>> putFiles) throws SVNException {
    // compare the files we have just put with current contents of folder
    for (Entry<String, Set<String>> entry : putFiles.entrySet()) {
      String folderPath = entry.getKey();
      List<String> existingFiles = svnDAO.list(folderPath, -1);
      for (String existingFile : existingFiles) {
        if (!entry.getValue().contains(existingFile)) {
          // existing file not in put set, i.e. no longer part of this publication, so delete it
          Message.info("Deleting " + folderPath + "/" + existingFile);
          commitEditor.deleteEntry(folderPath + "/" + existingFile, -1);
        }
      }
    }
  }

  /**
   * Prepares the repository for the binary diff transaction, if necessary.
   * 
   * @return A Map of folders which need to be copied in the binary diff transaction, where the key is the ultimate
   *         destination folder and the value is the intermediate binary diff folder.
   * @throws SVNException If an error occurs deleting previous releases in the repository.
   */
  private Map<String, String> prepareBinaryDiff() throws SVNException {
    return transactionContent.prepareBinaryDiff();
  }

  /**
   * Performs any necessary binary diff copy operations as contained in the passed Map.
   * 
   * @param foldersToCopy Map of folders to copy where key is destination and value is source.
   * @throws SVNException If an error occurs copying one or more folders.
   */
  private void copyDiff(Map<String, String> foldersToCopy) throws SVNException {
    if (foldersToCopy.size() > 0) {
      long rev = commitRepository.getLatestRevision(); // copying dirs requires valid revision
      commitEditor = commitRepository.getCommitEditor(commitMessage, null);
      commitEditor.openRoot(-1);
      for (Entry<String, String> entry : foldersToCopy.entrySet()) {
        Message.info("Copying from " + entry.getValue() + " to " + entry.getKey());
        // addDir can't handle creating sub folders so we have to do it
        String subFolder = svnDAO.createSubFolders(commitEditor, entry.getKey(), rev);
        if (subFolder != null) { // add dir is relative to last path, so change to subfolder first
          commitEditor.openDir(subFolder, rev);
        }
        commitEditor.addDir(entry.getKey(), entry.getValue(), rev);
        commitEditor.closeDir();
      }
      commitEditor.closeDir();
      Message.info("Binary diff finished : " + commitEditor.closeEdit());
    }
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
   * Returns whether a commit operation has been started or not.
   * 
   * @return True if a commit has been started, false otherwise.
   */
  public boolean commitStarted() {
    return commitStarted;
  }

  /**
   * Set whether to perform a binary diff or not.
   * 
   * @param binaryDiff
   */
  public void setBinaryDiff(boolean binaryDiff) {
    this.binaryDiff = binaryDiff;
  }

  /**
   * Sets the folder name to use for binary diffs, if not set will default to DEFAULT_BINARY_DIFF_LOCATION
   * 
   * @param binaryDiffFolderName The folder name to use for binary diffs.
   */
  public void setBinaryDiffFolderName(String binaryDiffFolderName) {
    this.binaryDiffFolderName = binaryDiffFolderName;
  }

  /**
   * Set whether to cleanup (i.e. delete the contents of) the folder being published to during the publish operation.
   * 
   * @param cleanupPublishFolder Whether to cleanup the publish folder or not.
   */
  public void setCleanupPublishFolder(Boolean cleanupPublishFolder) {
    this.cleanupPublishFolder = cleanupPublishFolder;
  }

  private class Directory {
    private final String path;
    private final String name;
    private final Directory parent;
    private final Map<String,Directory> subDirs = new HashMap<String,Directory>();
    private final List<PutOperation> files = new ArrayList<PutOperation>();

    private Directory(String name, String path, Directory parent) {
      this.name = name;
      this.path = path;
      this.parent = parent;
    }

    public String getName() {
      return name;
    }

    public String getPath() {
      return path;
    }

    public Directory subdir(String name) {
      if (subDirs.containsKey(name)) {
        return subDirs.get(name);
      } else {
        StringBuilder subdirPath = new StringBuilder(this.getPath());
        if (!this.getPath().endsWith("/")) {
          subdirPath.append('/');
        }
        subdirPath.append(name);
        Directory subdir = new Directory(name, subdirPath.toString(), this);
        subDirs.put(name, subdir);
        return subdir;
      }
    }

    public Directory updir() {
      return parent;
    }

    public void addFile(PutOperation op) {
      files.add(op);
    }

    public int commit() throws SVNException, IOException {
      int fileCount = 0;
      if (parent != null) {
        if (svnDAO.folderExists(getPath(), -1, true)) {
          commitEditor.openDir(getPath(), -1);
        } else {
          Message.debug("Creating folder " + getPath());
          commitEditor.addDir(getPath(), null, -1);
        }
      }
      for (Directory subdir: subDirs.values()) {
        fileCount += subdir.commit();
      }
      fileCount += performPutOperations(files);
      if (parent != null) {
        commitEditor.closeDir();
      }
      return fileCount;
    }

    public Map<String, String> prepareBinaryDiff() throws SVNException {
      Map<String, String> binaryDiffs = new HashMap<String, String>();
      if (binaryDiff) {
        Set<String> processedFolders = new HashSet<String>();
        prepareBinaryDiff(binaryDiffs, processedFolders);
      }
      return binaryDiffs;
    }

    private void prepareBinaryDiff(Map<String, String> binaryDiffs, Set<String> processedFolders) throws SVNException {
      for (Directory subdir: subDirs.values()) {
        subdir.prepareBinaryDiff(binaryDiffs, processedFolders);
      }
      for (PutOperation operation : files) {
        String currentFolder = operation.getFolderPath();
        if (!processedFolders.contains(currentFolder)) { // we haven't dealt with this folder yet
          String binaryDiffFolderPath = operation.determineBinaryDiffFolderPath(revision, binaryDiffFolderName);
          binaryDiffs.put(currentFolder, binaryDiffFolderPath); // schedule this to be processed later
          if (svnDAO.folderExists(currentFolder, -1, true)) {
            if (operation.isOverwrite()) {
              // delete old release, we will copy over to release folder again in binary diff commit later
              Message.info("Binary diff deleting " + currentFolder);
              commitEditor.deleteEntry(currentFolder, -1);
            } else {
              Message.info("Overwrite set to false, ignoring copy to " + currentFolder);
              binaryDiffs.remove(currentFolder);
            }
          }
        }
        processedFolders.add(currentFolder);
      }
    }
  }

}
