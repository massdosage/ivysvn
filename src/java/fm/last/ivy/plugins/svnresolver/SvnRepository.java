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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Ivy repository that uses Subversion for artifact storage.
 */
public class SvnRepository extends AbstractRepository {

  /**
   * The default SSH port.
   */
  public static final int DEFAULT_SSH_PORT_NUMBER = 22;

  /**
   * The user name to use to connect to Subversion.
   */
  private String userName;

  /**
   * The password to use to connect to Subversion.
   */
  private String userPassword;

  /**
   * Private key file to use for SSH authentication to Subversion.
   */
  private File keyFile;

  /**
   * Passphrase for SSH private key file.
   */
  private String passPhrase = "";

  /**
   * Port number that SSH server is listening on.
   */
  private int portNumber = DEFAULT_SSH_PORT_NUMBER;

  /**
   * SSL Certificate file to use for SSL authentication to Subversion.
   */
  private File certFile;

  /**
   * Whether to store credentials in global authentication cache or not.
   */
  private boolean storageAllowed = false;

  private Map<String, Resource> resourcesCache = new HashMap<String, Resource>();

  /**
   * The (optional) path to the base of the repository.
   */
  private String repositoryPath;

  /**
   * The svn transaction for putting files.
   */
  private SvnPublishTransaction publishTransaction;

  /**
   * Whether to perform binary diffs or not.
   */
  private boolean binaryDiff = true;

  /**
   * The default folder name for binary diffs.
   */
  private static final String DEFAULT_BINARY_DIFF_FOLDER_NAME = "LATEST";

  /**
   * The folder name for binary diffs.
   */
  private String binaryDiffFolderName = DEFAULT_BINARY_DIFF_FOLDER_NAME;

  /**
   * The revision id of the module being published.
   */
  private ModuleRevisionId moduleRevisionId;

  /**
   * Initialises repository to accept requests for svn protocol.
   */
  public SvnRepository() {
    super();
    FSRepositoryFactory.setup();
    DAVRepositoryFactory.setup();
    SVNRepositoryFactoryImpl.setup();
    try {
      Manifest manifest = getManifest(this.getClass());
      Attributes attributes = manifest.getMainAttributes();
      Message.info("IvySvn Build-Version: " + attributes.getValue("Build-Version"));
      Message.info("IvySvn Build-DateTime: " + attributes.getValue("Build-DateTime"));
    } catch (IOException e) {
      Message.warn(("Could not load manifest: " + e.getMessage()));
    }
  }

  /**
   * Gets the manifest associated with the passed class.
   * 
   * @param someClass Class to find manifest for.
   * @return The manifest.
   * @throws IOException If the manifest could not be found.
   */
  private Manifest getManifest(Class<?> someClass) throws IOException {
    String className = someClass.getSimpleName();
    String classFileName = className + ".class";
    String classFilePath = someClass.getPackage().toString().replace('.', '/') + "/" + className;
    String pathToThisClass = someClass.getResource(classFileName).toString();
    String pathToManifest = pathToThisClass.toString().substring(0,
        pathToThisClass.length() + 2 - ("/" + classFilePath).length())
        + "/META-INF/MANIFEST.MF";
    Manifest manifest = new Manifest(new URL(pathToManifest).openStream());
    return manifest;
  }

  /**
   * Gets a repository referencing the passed URL, with authentication setup based on the values currently set in this
   * object.
   * 
   * @param url Subversion repository URL.
   * @param cache Whether to cache repository reference.
   * @return An initialised repository object.
   * @throws SVNException If the URL or authentication credentials are invalid.
   */
  private SVNRepository getRepository(SVNURL url, boolean cache) throws SVNException {
    SVNRepository repository = null;
    if (cache) {
      repository = SVNRepositoryCache.getInstance().getRepository(url, userName, userPassword, keyFile, passPhrase,
          portNumber, certFile, storageAllowed);
    } else {
      repository = SvnUtils.createRepository(url, userName, userPassword, keyFile, passPhrase, portNumber, certFile,
          storageAllowed);
      repository.setLocation(url, false);
    }
    return repository;
  }

  /**
   * Starts a publish transaction.
   * 
   * @param mrid The SVN commit message to use for this publish transaction.
   */
  public void beginPublishTransaction(ModuleRevisionId mrid) {
    Message.debug("Starting transaction " + mrid + "...");
    this.moduleRevisionId = mrid;
  }

  /**
   * Commits the previously started publish transaction.
   * 
   * @throws IOException If an error occurs committing the transaction.
   */
  public void commitPublishTransaction() throws IOException {
    ensurePublishTransaction();
    Message.debug("Committing transaction...");
    try {
      publishTransaction.commit();
    } catch (SVNException e) {
      throw new IOException("SVN Error committing transaction: " + e.getMessage());
    }
  }

  /**
   * Aborts a previously started publish transaction.
   * 
   * @throws IOException If an error occurs aborting the publish transaction.
   */
  public void abortPublishTransaction() throws IOException {
    if (publishTransaction == null) {
      Message.info("Transaction not created, nothing to abort");
      return;
    }
    if (!publishTransaction.commitStarted()) {
      Message.info("Commit transaction not started, nothing to abort");
      return;
    }
    Message.info("Aborting transaction");
    try {
      publishTransaction.abort();
    } catch (SVNException e) {
      throw new IOException("SVN Error aborting transaction: " + e.getMessage());
    }
  }

  /**
   * Handles a request to add/update a file to/in the repository.
   * 
   * @param source The source file.
   * @param destination The location of the file in the repository.
   * @param overwrite Whether to overwite the file if it already exists.
   * @throws IOException If an error occurs putting a file (invalid path, invalid login credentials etc.)
   */
  public void put(File source, String destination, boolean overwrite) throws IOException {
    fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
    Message.debug("Scheduling publish from " + source.getAbsolutePath() + " to " + destination);

    try {
      SVNURL destinationURL = SVNURL.parseURIEncoded(destination);
      if (publishTransaction == null) { // haven't initialised transaction on a previous put
        // first create a repository which transaction can use for various file checks
        SVNRepository ancillaryRepository = getRepository(destinationURL, false);
        SVNURL root = ancillaryRepository.getRepositoryRoot(false);
        ancillaryRepository.setLocation(root, false);
        SvnDao svnDAO = new SvnDao(ancillaryRepository);

        publishTransaction = new SvnPublishTransaction(svnDAO, moduleRevisionId, binaryDiff, binaryDiffFolderName);
        // now create another repository which transaction will use to do actual commits
        SVNRepository commitRepository = getRepository(destinationURL, false);
        publishTransaction.setCommitRepository(commitRepository);
      }
      // add all info needed to put the file to the transaction
      publishTransaction.addPutOperation(source, destination, overwrite, repositoryPath);
    } catch (SVNException e) {
      throw new IOException("SVN Error putting " + destination + ": " + e.getMessage());
    }
  }

  /**
   * Ensures that a transaction has been created.
   * 
   * @throws IllegalArgumentException If a transaction has not been created.
   */
  private void ensurePublishTransaction() {
    if (publishTransaction == null) {
      throw new IllegalStateException("Transaction not initialised");
    }
  }

  /**
   * Handles a request to retrieve a file from the repository.
   * 
   * @param source The location in the repository of the file to retrieve.
   * @param destination The location where the file should be retrieved to.
   * @throws IOException If an error occurs retrieving the file.
   */
  public void get(String source, File destination) throws IOException {
    fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
    Message.debug("Getting file for user " + userName + " from  " + source + " to " + destination.getAbsolutePath());
    BufferedOutputStream output = null;
    try {
      SVNURL url = SVNURL.parseURIEncoded(source);
      SVNRepository repository = getRepository(url, true);
      repository.setLocation(url, false);

      Resource resource = getResource(source);
      fireTransferInitiated(resource, TransferEvent.REQUEST_GET);

      SVNNodeKind nodeKind = repository.checkPath("", -1);
      SVNErrorMessage error = SvnUtils.checkNodeIsFile(nodeKind, url);
      if (error != null) {
        throw new IOException(error.getMessage());
      }

      output = new BufferedOutputStream(new FileOutputStream(destination));
      // Gets the contents of the latest revision of the file
      repository.getFile("", -1, null, output);

      fireTransferCompleted(destination.length());
    } catch (SVNException e) {
      e.printStackTrace();
      fireTransferError(e);
    } catch (RuntimeException e) {
      e.printStackTrace();
      fireTransferError(e);
      throw e;
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

  /**
   * Gets a SvnResource for the passed svn string.
   * 
   * @param source Subversion string identifying the resource.
   * @return The resource.
   * @throws IOException Never thrown, just here to satisfy interface.
   */
  public Resource getResource(String source) throws IOException {
    Resource resource = (Resource) resourcesCache.get(source);
    if (resource == null) {
      resource = new SvnResource(this, source);
      resourcesCache.put(source, resource);
    }
    return resource;
  }

  /**
   * Fetch the needed file information for a given file (size, last modification time) and report it back in a
   * SvnResource.
   * 
   * @param source Subversion string identifying the resource.
   * @return SvnResource filled with the needed informations
   */
  public SvnResource resolveResource(String source) {
    Message.debug("Resolving resource for " + source);
    SvnResource result = null;
    try {
      SVNURL url = SVNURL.parseURIEncoded(source);
      SVNRepository repository = getRepository(url, true);
      SVNNodeKind nodeKind = repository.checkPath("", -1);
      if (nodeKind == SVNNodeKind.NONE) {
        Message.debug("No resource found at " + source + ", returning default resource");
        result = new SvnResource();
      } else {
        Message.debug("Resource found at " + source + ", returning resolved resource");
        SVNDirEntry entry = repository.info("", -1);
        result = new SvnResource(this, source, true, entry.getDate().getTime(), entry.getSize());
      }
    } catch (SVNException e) {
      Message.error("Error resolving resource " + source + ", " + e.getMessage());
      result = new SvnResource();
    }
    return result;
  }

  /**
   * Return a listing of resource names.
   * 
   * @param parent The parent directory from which to generate the listing.
   * @return A listing of the parent directory's file content, as a List of Strings.
   * @throws IOException On listing failure.
   */
  public List<String> list(String source) throws IOException {
    Message.debug("Getting list for " + source);
    try {
      SVNURL url = SVNURL.parseURIEncoded(source);
      SVNRepository repository = getRepository(url, true);
      SvnDao svnDAO = new SvnDao(repository);
      List<String> list = svnDAO.list("", -1); // repository is already set to full path, so list ""
      return list;
    } catch (SVNException e) {
      throw new IOException("SVN error listing " + source + ":" + e.getMessage());
    }
  }

  /**
   * Set the user name to use to connect to the svn repository.
   * 
   * @param userName
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Set the password to use to connect to the svn repository.
   * 
   * @param userPassword
   */
  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  /**
   * Set the private key file to use for SSH authentication to Subversion.
   * 
   * @param keyFile Key file.
   */
  public void setKeyFile(File keyFile) {
    this.keyFile = keyFile;
  }

  /**
   * Set the passphrase for the SSH private key file.
   * 
   * @param passPhrase
   */
  public void setPassPhrase(String passPhrase) {
    this.passPhrase = passPhrase;
  }

  /**
   * Set the port number for SSH server.
   * 
   * @param portNumber
   */
  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  /**
   * Set the SSL Certificate file to use for SSL authentication to Subversion.
   * 
   * @param certFile SSL Certificate file.
   */
  public void setCertFile(File certFile) {
    this.certFile = certFile;
  }

  /**
   * Set whether to store authentication credentials in global authentication cache or not.
   * 
   * @param storageAllowed Whether to store authentication credentials or not.
   */
  public void setStorageAllowed(boolean storageAllowed) {
    this.storageAllowed = storageAllowed;
  }

  /**
   * The Subversion repository URL
   * 
   * @param svnRepositoryURL
   * @throws SVNException
   */
  public void setSvnRepositoryURL(String svnRepositoryURL) throws SVNException {
    SVNURL url = SVNURL.parseURIEncoded(svnRepositoryURL);
    repositoryPath = url.getPath();
    // append a slash, makes it compatible with previous code base
    if (!repositoryPath.endsWith("/")) {
      repositoryPath += "/";
    }
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

}
