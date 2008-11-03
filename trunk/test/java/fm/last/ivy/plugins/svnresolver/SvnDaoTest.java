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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Unit test case for the SvnDao.
 */
public class SvnDaoTest extends BaseTestCase {

  @Test
  public void testPutGetAndFileExists() throws SVNException, IOException {
    String fileName = "testPutFile().txt";
    assertFalse(svnDAO.fileExists(TEST_PATH + "/" + fileName, -1));
    String testData = new String("test data");
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, TEST_PATH, -1);
    svnDAO.putFile(commitEditor, testData.getBytes(), TEST_PATH, fileName, false);
    commitEditor.closeEdit();
    assertTrue(svnDAO.fileExists(TEST_PATH + "/" + fileName, -1));

    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + TEST_PATH + "/" + fileName);
    File retrieved = new File(testTempFolder, fileName);
    svnDAO.getFile(sourceURL, retrieved, -1);
    assertTrue(retrieved.exists());
    assertEquals(testData, FileUtils.readFileToString(retrieved));
  }

  @Test
  public void testPutAndGetFile_Overwrite() throws SVNException, IOException {
    ISVNEditor commitEditor = getCommitEditor();
    String fileName = "testPutFile().txt";
    String testData = new String("test data");
    svnDAO.createFolders(commitEditor, TEST_PATH, -1);
    svnDAO.putFile(commitEditor, testData.getBytes(), TEST_PATH, fileName, false);
    commitEditor.closeEdit();
    assertTrue(svnDAO.fileExists(TEST_PATH + "/" + fileName, -1));

    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + TEST_PATH + "/" + fileName);
    File retrieved = new File(testTempFolder, fileName);
    svnDAO.getFile(sourceURL, retrieved, -1);
    assertEquals(testData, FileUtils.readFileToString(retrieved));

    String modifiedTestData = new String("test data-modified");
    commitEditor = getCommitEditor();
    // send different data to same file with overwrite set to FALSE, put should get ignored
    svnDAO.createFolders(commitEditor, TEST_PATH, -1);
    svnDAO.putFile(commitEditor, modifiedTestData.getBytes(), TEST_PATH, fileName, false);
    commitEditor.closeEdit();

    String fileName2 = "testPutFile2().txt"; // retrieve to a different file name to be safe
    retrieved = new File(testTempFolder, fileName2);
    svnDAO.getFile(sourceURL, retrieved, -1);
    // overwrite was false, so new file should contain old data
    assertEquals(testData, FileUtils.readFileToString(retrieved));

    commitEditor = getCommitEditor();
    // send different data to same file with overwrite set to TRUE
    svnDAO.createFolders(commitEditor, TEST_PATH, -1);
    svnDAO.putFile(commitEditor, modifiedTestData.getBytes(), TEST_PATH, fileName, true);
    commitEditor.closeEdit();

    String fileName3 = "testPutFile3().txt"; // retrieve to a different file name to be safe
    retrieved = new File(testTempFolder, fileName3);
    svnDAO.getFile(sourceURL, retrieved, -1);
    // overwrite was false, so new file should contain old data
    assertEquals(modifiedTestData, FileUtils.readFileToString(retrieved));
  }

  /**
   * Tests creating a file under a path that does not exist in repository, intermediate folders should be created.
   * 
   * @throws SVNException
   * @throws IOException
   */
  @Test
  public void testPutFileAndFolders() throws SVNException, IOException {
    String fileName = "testPutFile().txt";
    String testData = new String("test data");
    String destinationPath = TEST_PATH + "/x/y/z";
    assertFalse(svnDAO.folderExists(destinationPath, -1, false));
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, destinationPath, -1);
    svnDAO.putFile(commitEditor, testData.getBytes(), destinationPath, fileName, false);
    commitEditor.closeEdit();
    assertTrue(svnDAO.folderExists(destinationPath, -1, false));
    assertTrue(svnDAO.fileExists(destinationPath + "/" + fileName, -1));
    // a file should not return true for a folder check
    assertFalse(svnDAO.folderExists(destinationPath + "/" + fileName, -1, false));
    // similarly, a folder should not return true for a file check
    assertFalse(svnDAO.fileExists(destinationPath, -1));

    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + destinationPath + "/" + fileName);
    File retrieved = new File(testTempFolder, fileName);
    svnDAO.getFile(sourceURL, retrieved, -1);
    assertTrue(retrieved.exists());
    assertEquals(testData, FileUtils.readFileToString(retrieved));
  }

  @Test
  public void testCreateSubfolders() throws SVNException {
    ISVNEditor commitEditor = getCommitEditor();
    String subFolders = svnDAO.createSubFolders(commitEditor, TEST_PATH + "/a/b/c/d", -1);
    commitEditor.closeEdit();
    assertEquals(TEST_PATH + "/a/b/c", subFolders);
    assertTrue(svnDAO.folderExists(TEST_PATH + "/a/b/c", -1, false));

    // now create the same folders again
    commitEditor = getCommitEditor();
    subFolders = svnDAO.createSubFolders(commitEditor, TEST_PATH + "/a/b/c/d", -1);
    commitEditor.closeEdit();
    assertEquals(TEST_PATH + "/a/b/c", subFolders);

  }

  @Test
  public void testCreateSubfolders_NoSubFolders() throws SVNException {
    ISVNEditor commitEditor = getCommitEditor();
    String subFolders = svnDAO.createSubFolders(commitEditor, TEST_PATH, -1);
    commitEditor.closeEdit();
    assertNull(subFolders);
  }

  @Test
  public void testCreateFolders() throws SVNException {
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, TEST_PATH + "/a/b/c/d", -1);
    commitEditor.closeEdit();
    assertTrue(svnDAO.folderExists(TEST_PATH + "/a/b/c/d", -1, false));

    // create same path again
    commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, TEST_PATH + "/a/b/c/d", -1);
    commitEditor.closeEdit();
    assertTrue(svnDAO.folderExists(TEST_PATH + "/a/b/c/d", -1, false));
  }

  @Test
  public void testCreateFolders_NoSubFolders() throws SVNException {
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, TEST_PATH, -1);
    commitEditor.closeEdit();
    assertTrue(svnDAO.folderExists(TEST_PATH, -1, false));
  }

  @Test
  public void testList() throws SVNException {
    String testData = new String("test data");
    String destinationPath = TEST_PATH + "/testList";
    assertFalse(svnDAO.folderExists(destinationPath, -1, false));
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, destinationPath, -1);
    svnDAO.putFile(commitEditor, testData.getBytes(), destinationPath, "file1.txt", false);
    svnDAO.putFile(commitEditor, testData.getBytes(), destinationPath, "file2.txt", false);
    svnDAO.createFolders(commitEditor, destinationPath + "/subfolder", -1);
    commitEditor.closeEdit();
    List<String> contents = svnDAO.list(destinationPath, -1);
    assertEquals(3, contents.size());
    assertTrue(contents.contains("file1.txt"));
    assertTrue(contents.contains("file2.txt"));
    assertTrue(contents.contains("subfolder"));
  }

}
