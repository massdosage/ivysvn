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
   * List of PutOperations representing artifacts that need to be added/updated.
   */
  private List<PutOperation> putOperations = new ArrayList<PutOperation>();

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
  private boolean binaryDiff = false;

  /**
   * The Ivy module revision string.
   */
  private String revision;

  /**
   * The name of the binary diff folder.
   */
  private String binaryDiffFolderName;

  /**
   * Constructs a new instance of this class, which will use the passed message when a commit is performed.
   * 
   * @param svnDAO The subversion DAO to use.
   * @param mrid The ivy Module Revision ID.
   * @param binaryDiff Whether to perform a binary diff.
   * @param binaryDiffFolderName The name of the binary diff folder.
   */
  public SvnPublishTransaction(SvnDao svnDAO, ModuleRevisionId mrid, boolean binaryDiff, String binaryDiffFolderName) {
    this.svnDAO = svnDAO;
    this.binaryDiff = binaryDiff;
    this.binaryDiffFolderName = binaryDiffFolderName;
    this.revision = mrid.getRevision();
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
    PutOperation operation = new PutOperation(source, destinationPath, overwrite, repositoryPath);
    this.putOperations.add(operation);
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
    int putFileCount = performPutOperations(); // put all the files scheduled to be put
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
  private int performPutOperations() throws SVNException, IOException {
    checkCommitEditor();
    int putFileCount = 0;
    for (PutOperation operation : putOperations) {
      String destinationFolderPath = operation.getFolderPath(); // assume no binary diff
      boolean overwrite = operation.isOverwrite();
      if (binaryDiff) { // publishing to intermediate binary diff location, override values set above
        destinationFolderPath = operation.determineBinaryDiffFolderPath(revision, binaryDiffFolderName);
        overwrite = true; // force overwrite for binary diff
      }
      // destinationFolderPath and overwrite will be set according to whether binary diff or not
      if (svnDAO.putFile(commitEditor, operation.getData(), destinationFolderPath, operation.getFileName(), overwrite)) {
        putFileCount++;
      }
    }
    return putFileCount;
  }

  /**
   * Prepares the repository for the binary diff transaction, if necessary.
   * 
   * @return A Map of folders which need to be copied in the binary diff transaction, where the key is the ultimate
   *         destination folder and the value is the intermediate binary diff folder.
   * @throws SVNException If an error occurs deleting previous releases in the repository.
   */
  private Map<String, String> prepareBinaryDiff() throws SVNException {
    Map<String, String> binaryDiffs = new HashMap<String, String>();
    if (binaryDiff) {
      Set<String> processedFolders = new HashSet<String>(); // most operations share a folder, so "cache" this
      for (PutOperation operation : putOperations) {
        String currentFolder = operation.getFolderPath();
        if (!processedFolders.contains(currentFolder)) { // we haven't dealt with this folder yet
          String binaryDiffFolderPath = operation.determineBinaryDiffFolderPath(revision, binaryDiffFolderName);
          binaryDiffs.put(currentFolder, binaryDiffFolderPath); // schedule this to be processed later
          if (svnDAO.folderExists(currentFolder, -1)) {
            if (operation.isOverwrite()) {
              // delete old release, we will copy over to release folder again in binary diff commit later
              Message.info("Deleting " + currentFolder);
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
    return binaryDiffs;
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

}
