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
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.resolver.RepositoryResolver;
import org.tmatesoft.svn.core.SVNException;

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
  public void beginPublishTransaction(ModuleRevisionId mrid, boolean b) throws IOException {
    StringBuilder comment = new StringBuilder();
    comment.append("Ivy publishing ").append(mrid.getOrganisation()).append("/");
    comment.append(mrid.getName()).append(" [");
    comment.append(mrid.getRevision()).append("]");
    getSvnRepository().beginPublishTransaction(comment.toString());
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
  public void setKeyfile(String keyFilePath) {
    Repository repository = getRepository();
    if (repository != null && validParameter(keyFilePath) && repository instanceof SvnRepository) {
      File keyFile = new File(keyFilePath.trim());
      if (keyFile.exists()) {
        ((SvnRepository) repository).setKeyFile(keyFile);
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
  public void setPassphrase(String passPhrase) {
    Repository repository = getRepository();
    if (repository != null && validParameter(passPhrase) && repository instanceof SvnRepository) {
      ((SvnRepository) repository).setPassPhrase(passPhrase.trim());
    }
  }

  /**
   * Set the port number for the SSH server.
   * 
   * @param portNumber
   */
  public void setPort(String port) {
    Repository repository = getRepository();
    if (repository != null && validParameter(port) && repository instanceof SvnRepository) {
      int portNumber = Integer.parseInt(port.trim());
      ((SvnRepository) repository).setPortNumber(portNumber);
    }
  }

  /**
   * Set the SSL Certificate file to use for SSL authentication to Subversion.
   * 
   * @param certFile SSL Certificate file.
   */
  public void setCertfile(String certFilePath) {
    Repository repository = getRepository();
    if (repository != null && validParameter(certFilePath) && repository instanceof SvnRepository) {
      File certFile = new File(certFilePath.trim());
      if (certFile.exists()) {
        ((SvnRepository) repository).setCertFile(certFile);
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
  public void setStorageallowed(String storageAllowedString) {
    Repository repository = getRepository();
    if (repository != null && validParameter(storageAllowedString) && repository instanceof SvnRepository) {
      boolean storageAllowed = Boolean.parseBoolean(storageAllowedString.trim());
      ((SvnRepository) repository).setStorageAllowed(storageAllowed);
    }
  }

  /**
   * Sets the path to the base of the repository. This is optional, if not set then the base of the repository is
   * assumed to be the path up to the first "/" in paths to artifacts to be published. Set this if the base of the
   * repository does not match this assumption.
   * 
   * @param repositoryURL SVN URL representing the base of the repository.
   * @throws SVNException If an error occurs parsing the passed repositoryURL as a SVNURL.
   */
  public void setRepositoryURL(String repositoryURL) throws SVNException {
    Repository repository = getRepository();
    if (repository != null && validParameter(repositoryURL) && repository instanceof SvnRepository) {
      ((SvnRepository) repository).setSvnRepositoryURL(repositoryURL.trim());
    }
  }

  /**
   * Set the user name to use to connect to the svn repository.
   * 
   * @param userName The svn username.
   */
  public void setUsername(String userName) {
    Repository repository = getRepository();
    if (repository != null && validParameter(userName) && repository instanceof SvnRepository) {
      ((SvnRepository) repository).setUserName(userName.trim());
    }
  }

  /**
   * Set the password to use to connect to the svn repository.
   * 
   * @param userPassword The svn password.
   */
  public void setUserpassword(String userPassword) {
    Repository repository = getRepository();
    if (repository != null && validParameter(userPassword) && repository instanceof SvnRepository) {
      ((SvnRepository) repository).setUserPassword(userPassword.trim());
    }
  }

}
