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

import org.tmatesoft.svn.core.io.ISVNConnectionListener;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * An ISVNConnectionListener that outputs connection opened and closed messages. Useful for debugging connection issues.
 */
public class SysOutConnectionListener implements ISVNConnectionListener {

  /**
   * Name of this listener (output in sysout messages). Useful if you have more than one listener.
   */
  private String name;

  /**
   * Constructs a new connection listener with the passed name.
   * 
   * @param name
   */
  public SysOutConnectionListener(String name) {
    this.name = name;
  }

  public void connectionClosed(SVNRepository svnrepository) {
    System.out.println("\tConnection closed for " + name + " (" + svnrepository + ")");
  }

  public void connectionOpened(SVNRepository svnrepository) {
    System.out.println("\tConnection opened for " + name + " (" + svnrepository + ")");
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

}
