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
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Simple cache of SVNRepository instances stored against their host and protocol. Used by ivy-svn so that the
 * authentication mechanism only needs to be setup once per set of operations (retrieve/publish) instead of per file
 * within an operation.
 */
public class SVNRepositoryCache {

  /**
   * Singleton instance of this cache.
   */
  private static SVNRepositoryCache instance = new SVNRepositoryCache();

  /**
   * Cache of SVNRepository instances where the key is protocol:host.
   */
  private Map<String, SVNRepository> repositoryCache = new HashMap<String, SVNRepository>();

  /**
   * Private constructor to enfore singleton pattern.
   */
  private SVNRepositoryCache() {
  }

  /**
   * Gets the one and only instance of this class.
   * 
   * @return The one and only instance of this class.
   */
  public static SVNRepositoryCache getInstance() {
    return instance;
  }

  /**
   * Gets a repository instance for the passed URL. The same instance will be returned for the same protocol + host
   * combination. If the repository cannot be found in the cache a new one will be created, the passed parameters
   * determine the authentication mechanism which will be used for this. The returned repository will have its location
   * set to the passed url.
   * 
   * @param url A SVNURL object with at the very least the protocol and host set.
   * @param userName Subversion user name.
   * @param userPassword Subversion password.
   * @param keyFile SSH Key file.
   * @param sshPassPhrase SSH key file passphrase.
   * @param portNumber SSH port number.
   * @param certFile SSL certificate file.
   * @param sslPassPhrase SSL certificate passphrase.
   * @param storageAllowed Whether to allow credential storage or not.
   * @return A repository for the passed url.
   * @throws SVNException If an error occurs creating the repository.
   */
  public synchronized SVNRepository getRepository(SVNURL url, String userName, String userPassword, File keyFile,
      String sshPassPhrase, int portNumber, File certFile, String sslPassPhrase, boolean storageAllowed) throws SVNException {
    String key = url.getProtocol() + ":" + url.getHost();
    SVNRepository repository = repositoryCache.get(key);
    if (repository == null) {
      repository = SvnUtils.createRepository(url, userName, userPassword, keyFile, sshPassPhrase, portNumber, certFile, sslPassPhrase,
          storageAllowed);
      repositoryCache.put(key, repository);
    }
    repository.setLocation(url, false);
    return repository;
  }

}
