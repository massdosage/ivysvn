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
