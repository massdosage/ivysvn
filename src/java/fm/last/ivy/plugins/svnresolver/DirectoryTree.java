package fm.last.ivy.plugins.svnresolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree that holds PutOperations per directory, where a DirectoryTree contains references to other DirectoryTree's based
 * on relative path information, so a depth-first traversal can be performed over the tree.
 */
public class DirectoryTree {

  private final String path;
  private final DirectoryTree parent;

  /**
   * A map holding this tree's direct sub directories. Key is the name of the sub directory, value is the DirectoryTree
   * for the sub directory.
   */
  private final Map<String, DirectoryTree> subDirs = new HashMap<String, DirectoryTree>();
  private final List<PutOperation> putOperations = new ArrayList<PutOperation>();

  /**
   * Constructs a new instance.
   * 
   * @param path Path of this tree.
   * @param parent Tree's parent (can be null if this is the root of the tree).
   */
  public DirectoryTree(String path, DirectoryTree parent) {
    this.path = path;
    this.parent = parent;
  }

  /**
   * Adds the passed sub-directory to this tree.
   * 
   * @param subDirName The name of the sub directory.
   * @return The DirectoryTree for the passed sub-directory.
   */
  public DirectoryTree subDir(String subDirName) {
    if (subDirs.containsKey(subDirName)) {
      return subDirs.get(subDirName);
    } else {
      StringBuilder subDirPath = new StringBuilder(this.getPath());
      if (!this.getPath().endsWith("/")) {
        subDirPath.append('/');
      }
      subDirPath.append(subDirName); // path now contains full sub dir path
      DirectoryTree subTree = new DirectoryTree(subDirPath.toString(), this);
      subDirs.put(subDirName, subTree);
      return subTree;
    }
  }

  /**
   * Gets this tree's parent tree.
   * 
   * @return This tree's parent tree, or null if this tree is the root.
   */
  public DirectoryTree getParent() {
    return this.parent;
  }

  /**
   * Gets the PutOperations associated with this tree.
   * 
   * @return This tree's PutOperations.
   */
  public List<PutOperation> getPutOperations() {
    return putOperations;
  }

  /**
   * Gets the trees for this tree's sub directories.
   * 
   * @return This tree's sub directory trees.
   */
  public Collection<DirectoryTree> getSubDirectoryTrees() {
    return subDirs.values();
  }

  /**
   * Add a PutOperation to this tree.
   * 
   * @param putOperation PutOperation to add.
   */
  public void addPutOperation(PutOperation putOperation) {
    putOperations.add(putOperation);
  }

  /**
   * Gets the path representing this tree's directory.
   * 
   * @return The path representing this tree's directory.
   */
  public String getPath() {
    return path;
  }

}
