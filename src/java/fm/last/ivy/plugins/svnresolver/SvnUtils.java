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
 */
package fm.last.ivy.plugins.svnresolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * Utility class for performing common operations in subversion.
 */
public class SvnUtils {
  
  /**
   * The proxy settings.
   */
  private static final ProxySettings proxySettings = new ProxySettings();

  /**
   * Check that the passed node exists and represents a folder.
   * 
   * @param nodeKind The node.
   * @param url The url of the node.
   * @return null if the node is a folder, or an instantiated error message object if not.
   */
  public static SVNErrorMessage checkNodeIsFolder(SVNNodeKind nodeKind, SVNURL url) {
    SVNErrorMessage errorMessage = null;
    if (nodeKind == SVNNodeKind.NONE) {
      errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "There is no entry at URL ''{0}'' ", url);
    } else if (nodeKind != SVNNodeKind.DIR) {
      errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
          "Entry at URL ''{0}'' is not valid (expected folder but was something else)", url);
    }
    return errorMessage;
  }

  /**
   * Check that the passed node exists and represents a file.
   * 
   * @param nodeKind The node.
   * @param url The url of the node.
   * @return null if the node is a file, or an instantiated error message object if not.
   */
  public static SVNErrorMessage checkNodeIsFile(SVNNodeKind nodeKind, SVNURL url) {
    SVNErrorMessage errorMessage = null;
    if (nodeKind == SVNNodeKind.NONE) {
      errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "There is no entry at URL ''{0}'' ", url);
    } else if (nodeKind != SVNNodeKind.FILE) {
      errorMessage = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
          "Entry at URL ''{0}'' is not valid (expected file but was something else)", url);
    }
    return errorMessage;
  }

  /**
   * Creates a reference to a subversion repository, initialised with a valid authentication
   * manager. Based on the passed parameters a set of one or more valid authentication mechanisms
   * will be decided on. These can be:
   * 
   * 1. SSH KeyFile authentication. 
   * 2. SSH User name and Password authentication. 
   * 3. Subversion User name and Password authentication. 
   * 4. Subversion User name authentication. 
   * 5. SSL certificate authentication.
   * 
   * @param url The url of the repository (required).
   * @param userName The user name (required for 1,2,3,4).
   * @param userPassword The password (required for 2,3,5).
   * @param keyFile SSH key file (required for 1).
   * @param sshPassPhrase (required for 1).
   * @param portNumber (required for 1, 2).
   * @param certFile (required for 5).
   * @param sslPassPhrase (required for 5).
   * @param storageAllowed Whether to store authentication credentials in the global auth cache or
   *          not.
   * @return An initialised SVNRepository object.
   * @throws SVNException If an error occurs connecting to repository (e.g. invalid url, invalid
   *           authentication credentials)
   */
  public static SVNRepository createRepository(SVNURL url, String userName, String userPassword, File keyFile,
      String sshPassPhrase, int portNumber, File certFile, String sslPassPhrase, boolean storageAllowed) throws SVNException {
    SVNRepository repository = SVNRepositoryFactory.create(url);
    List<SVNAuthentication> authentications = new ArrayList<SVNAuthentication>();

    if (keyFile != null && userName != null) { // 1. ssh key file authentication
      Message.debug("Adding SSH key file authentication");
      SVNSSHAuthentication svnSSHAuthentication = new SVNSSHAuthentication(userName, keyFile, sshPassPhrase, portNumber,
          storageAllowed);
      authentications.add(svnSSHAuthentication);
    }

    if (userName != null && userPassword != null) { // username and password auth
      String protocol = url.getProtocol();
      if (protocol.startsWith("svn+ssh")) { // 2. SSH User name and Password authentication.
        Message.debug("Adding SSH user/pass authentication");
        SVNSSHAuthentication svnSSHAuthentication = new SVNSSHAuthentication(userName, userPassword, portNumber,
            storageAllowed);
        authentications.add(svnSSHAuthentication);
      } else { // default to SVN password auth, valid for svn://, http(s):// and hopefully others
        // 3. Subversion User name and Password authentication.
        Message.debug("Adding SVN user/pass authentication");
        SVNPasswordAuthentication svnPasswordAuthentication = new SVNPasswordAuthentication(userName, userPassword,
            storageAllowed);
        authentications.add(svnPasswordAuthentication);
      }
    } else if (userName != null && userPassword == null) { // 4. svn username auth
      Message.debug("Adding user authentication");
      SVNUserNameAuthentication userNameAuthentication = new SVNUserNameAuthentication(userName, storageAllowed);
      authentications.add(userNameAuthentication);
    }

    if (certFile != null && sslPassPhrase != null) { // 5.ssl authentication
      Message.debug("Adding SSL certificate authentication");
      SVNSSLAuthentication svnSSLAuthentication = new SVNSSLAuthentication(certFile, sslPassPhrase, storageAllowed);
      authentications.add(svnSSLAuthentication);
    }

    if (authentications.isEmpty()) {
      throw new SVNException(SVNErrorMessage.create(SVNErrorCode.AUTHZ_INVALID_CONFIG, "Missing authentication values"));
    }

    BasicAuthenticationManager authManager = new BasicAuthenticationManager(authentications
        .toArray(new SVNAuthentication[] {}));
    proxySettings.setProxy(authManager); // if there are any proxy settings this will set them on the auth manager
    repository.setAuthenticationManager(authManager);
    return repository;
  }
}
