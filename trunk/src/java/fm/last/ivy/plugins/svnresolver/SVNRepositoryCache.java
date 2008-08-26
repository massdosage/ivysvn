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

  private Map<String, SVNRepository> repositoryCache = new HashMap<String, SVNRepository>();

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
   * combination. The location on the returned repository should be set after this method is called. If the repository
   * cannot be found in the cache a new one will be created, the passed parameters determine the authentication
   * mechanism which will be used for this.
   * 
   * @param url A SVNURL object with at the very least the protocol and host set.
   * @param userName
   * @param userPassword
   * @param keyFile
   * @param passPhrase
   * @param portNumber
   * @param certFile
   * @param storageAllowed
   * @return
   * @throws SVNException
   */
  public synchronized SVNRepository getRepository(SVNURL url, String userName, String userPassword, File keyFile,
      String passPhrase, int portNumber, File certFile, boolean storageAllowed) throws SVNException {
    String key = url.getProtocol() + ":" + url.getHost();
    SVNRepository repository = repositoryCache.get(key);
    if (repository == null) {
      repository = SvnUtils.createRepository(url, userName, userPassword, keyFile, passPhrase, portNumber, certFile,
          storageAllowed);
      repositoryCache.put(key, repository);
    }
    return repository;
  }

}
