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

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.resolver.RepositoryResolver;

/**
 * An Ivy resolver for Subversion repositories.
 */
public class SvnResolver extends RepositoryResolver {

  /**
   * Registers a new resolver for svn+ssh patterns.
   */
  public SvnResolver() {
    setRepository(new SvnRepository());
  }

  /**
   * Gets the Repository in use by this resolver, casting it to the correct type.
   * 
   * @return The repository in use by this resolver.
   */
  protected SvnRepository getSvnRepository() {
    return (SvnRepository) getRepository();
  }

  @Override
  public void beginPublishTransaction(ModuleRevisionId mrid, boolean flag) throws IOException {
    getSvnRepository().beginPublishTransaction(mrid);
  }

  @Override
  public void abortPublishTransaction() throws IOException {
    getSvnRepository().abortPublishTransaction();
  }

  @Override
  public void commitPublishTransaction() throws IOException {
    getSvnRepository().commitPublishTransaction();
  }

  /**
   * Determines whether a parameter is valid or not, parameters that are determined to be "unset" property placeholders
   * will be silently ignored.
   * 
   * @param parameter Parameter to check.
   * @return True if the parameter is valid, false otherwise.
   */
  private boolean validParameter(String parameter) {
    if (parameter != null) {
      if (!parameter.startsWith("${")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Set the private key file to use for SSH authentication to Subversion.
   * 
   * @param keyFile Key file.
   */
  public void setKeyFile(String keyFilePath) {
    if (validParameter(keyFilePath)) {
      File keyFile = new File(keyFilePath.trim());
      if (keyFile.exists()) {
        getSvnRepository().setKeyFile(keyFile);
      } else {
        System.err.println("No key file found at '" + keyFilePath + "'");
      }
    }
  }

  /**
   * Set the passphrase for the SSH private key file.
   * 
   * @param passPhrase
   */
  public void setSshPassphrase(String passPhrase) {
    if (validParameter(passPhrase)) {
      getSvnRepository().setPassPhrase(passPhrase.trim());
    }
  }

  /**
   * Set the port number for the SSH server.
   * 
   * @param portNumber
   */
  public void setSshPort(String port) {
    if (validParameter(port)) {
      int portNumber = Integer.parseInt(port.trim());
      getSvnRepository().setPortNumber(portNumber);
    }
  }

  /**
   * Set the SSL Certificate file to use for SSL authentication to Subversion.
   * 
   * @param certFile SSL Certificate file.
   */
  public void setCertFile(String certFilePath) {
    if (validParameter(certFilePath)) {
      File certFile = new File(certFilePath.trim());
      if (certFile.exists()) {
        getSvnRepository().setCertFile(certFile);
      } else {
        System.err.println("No cert file found at '" + certFilePath + "'");
      }
    }
  }

  /**
   * Set whether to store credentials in global authentication cache or not.
   * 
   * @param storageAllowed Whether to store authentication credentials or not.
   */
  public void setStorageAllowed(String storageAllowedString) {
    if (validParameter(storageAllowedString)) {
      boolean storageAllowed = Boolean.parseBoolean(storageAllowedString.trim());
      getSvnRepository().setStorageAllowed(storageAllowed);
    }
  }

  /**
   * Sets the path to the base of the repository. This must be set for all repositories.
   * 
   * @param repositoryRoot The full repository root including the protocol.
   */
  public void setRepositoryRoot(String repositoryRoot) {
    if (validParameter(repositoryRoot)) {
      getSvnRepository().setRepositoryRoot(repositoryRoot.trim());
    }
  }

  /**
   * Set the user name to use to connect to the svn repository.
   * 
   * @param userName The svn username.
   */
  public void setUserName(String userName) {
    if (validParameter(userName)) {
      getSvnRepository().setUserName(userName.trim());
    }
  }

  /**
   * Set the password to use to connect to the svn repository.
   * 
   * @param userPassword The svn password.
   */
  public void setUserPassword(String userPassword) {
    if (validParameter(userPassword)) {
      getSvnRepository().setUserPassword(userPassword.trim());
    }
  }

  /**
   * Set whether to use binary diff or not (defaults to false).
   * 
   * @param binaryDiff Whether to use binary diff or not.
   */
  public void setBinaryDiff(String binaryDiffString) {
    if (validParameter(binaryDiffString)) {
      boolean binaryDiff = Boolean.parseBoolean(binaryDiffString.trim());
      getSvnRepository().setBinaryDiff(binaryDiff);
    }
  }

  /**
   * Set the name of the folder to use for binary diffs.
   * 
   * @param folderName The binary diff folder name.
   */
  public void setBinaryDiffFolderName(String folderName) {
    if (validParameter(folderName)) {
      getSvnRepository().setBinaryDiffFolderName(folderName.trim());
    }
  }

  /**
   * Set the SVN revision to use for retrieve operations.
   * 
   * @param retrieveRevision The SVN revision to use for retrieve operations.
   */
  public void setRetrieveRevision(String retrieveRevision) {
    if (validParameter(retrieveRevision)) {
      long svnRetrieveRevision = Long.parseLong(retrieveRevision);
      getSvnRepository().setSvnRetrieveRevision(svnRetrieveRevision);
    }
  }

}
