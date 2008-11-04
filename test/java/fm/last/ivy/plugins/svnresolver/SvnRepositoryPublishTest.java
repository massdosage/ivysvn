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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy publish" calls on the SvnRepository.
 */
public class SvnRepositoryPublishTest extends BaseSvnRepositoryPublishTestCase {

  @Test
  public void testDefaultPublish() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile);
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true);
  }

  @Test
  public void testPublishOverwrite_False() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile);
    publish(ivySettingsFile, defaultFileContents, false);
    assertPublish("1.0", defaultFileContents, true);
    // now try publish again
    String fileContents2 = "overwrite set to false so this should not get published";
    // FileUtils.writeStringToFile(defaultFileToPublish, fileContents2);
    publish(ivySettingsFile, fileContents2, false);
    assertPublish("1.0", defaultFileContents, true); // overwrite was false, so fileContents2 should not be published
  }

  @Test
  public void testPublishOverwrite_True() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile);
    publish(ivySettingsFile, defaultFileContents, true);
    assertPublish("1.0", defaultFileContents, true);
    // now publish again
    String fileContents2 = "overwrite set to true so this should overwrite previous contents";
    // FileUtils.writeStringToFile(defaultFileToPublish, fileContents2);
    publish(ivySettingsFile, fileContents2, true);
    assertPublish("1.0", fileContents2, true); // overwrite was true so defaultFileContents should have been overwritten
  }

  @Test
  public void testBinaryDiff() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true);
  }

  @Test
  public void testBinaryDiff_OverwriteFalse() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, false);
    assertPublish("1.0", defaultFileContents, true);
    // now publish again
    String fileContents2 = "overwrite set to false so this should not get published";
    // FileUtils.writeStringToFile(defaultFileToPublish, fileContents2);
    publish(ivySettingsFile, fileContents2, false);
    assertPublish("1.0", defaultFileContents, true); // overwrite was false so defaultFileContents should not change
  }

  @Test
  public void testBinaryDiff_OverwriteTrue() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, true);
    assertPublish("1.0", defaultFileContents, true);
    // now publish again
    String fileContents2 = "overwrite set to true so this should overwrite previous contents";
    // FileUtils.writeStringToFile(defaultFileToPublish, fileContents2);
    publish(ivySettingsFile, fileContents2, true);
    assertPublish("1.0", fileContents2, true); // overwrite was true so defaultFileContents should be overwritten
  }

  @Test
  public void testBinaryDiff_BinaryDiffFolderName() throws IOException, SVNException {
    String binaryDiffFolderName = "BINARYDIFF"; // use a folder name other than default of "LATEST"
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"true\" binaryDiffFolderName=\"" + binaryDiffFolderName + "\"");
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true, binaryDiffFolderName);
  }

  @Test
  public void testCleanupPublishFolderTrue_BinaryDiffFalse() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = BASE_PUBLISH_PATH + "/" + defaultOrganisation + "/" + defaultModule + "/1.0/";
    svnDAO.createFolders(commitEditor, publishPath, -1);
    svnDAO.putFile(commitEditor, "previous file".getBytes(), publishPath, "previous.jar", false);
    commitEditor.closeEdit();

    // now do a publish with cleanup set to true
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "cleanupPublishFolder=\"true\" binaryDiff=\"false\"");
    publish(ivySettingsFile, defaultFileContents, true); // publish folder exists due to previous, so set overwrite
    assertPublish("1.0", defaultFileContents, false);

    // previously created file should have been cleaned up
    assertFalse(publishPath + "previous.jar exists", svnDAO.fileExists(publishPath + "previous.jar", -1));
  }

  @Test
  public void testCleanupPublishFolderFalse_BinaryDiffFalse() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = BASE_PUBLISH_PATH + "/" + defaultOrganisation + "/" + defaultModule + "/1.0/";
    svnDAO.createFolders(commitEditor, publishPath, -1);
    svnDAO.putFile(commitEditor, "previous file".getBytes(), publishPath, "previous.jar", false);
    commitEditor.closeEdit();

    // now do a publish with cleanup set to false
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "cleanupPublishFolder=\"false\" binaryDiff=\"false\"");
    publish(ivySettingsFile, defaultFileContents, true); // publish folder exists due to previous, so set overwrite
    assertPublish("1.0", defaultFileContents, false);

    // previously created file should not have been cleaned up
    assertTrue(publishPath + "previous.jar doesn't exist", svnDAO.fileExists(publishPath + "previous.jar", -1));
  }

  @Test
  public void testCleanupPublishFolderTrue_BinaryDiffTrue() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = BASE_PUBLISH_PATH + "/" + defaultOrganisation + "/" + defaultModule + "/1.0/";
    svnDAO.createFolders(commitEditor, publishPath, -1);
    svnDAO.putFile(commitEditor, "previous file".getBytes(), publishPath, "previous.jar", false);
    commitEditor.closeEdit();

    // now do a publish with cleanup set to true
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "cleanupPublishFolder=\"true\" binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, true); // publish folder exists due to previous, so set overwrite
    assertPublish("1.0", defaultFileContents, true);

    // previously created file should have been cleaned up
    assertFalse(publishPath + "previous.jar exists", svnDAO.fileExists(publishPath + "previous.jar", -1));
  }

  @Test
  public void testCleanupPublishFolderFalse_BinaryDiffTrue() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = BASE_PUBLISH_PATH + "/" + defaultOrganisation + "/" + defaultModule + "/1.0/";
    svnDAO.createFolders(commitEditor, publishPath, -1);
    svnDAO.putFile(commitEditor, "previous file".getBytes(), publishPath, "previous.jar", false);
    commitEditor.closeEdit();

    // now do a publish with cleanup set to true
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "cleanupPublishFolder=\"false\" binaryDiff=\"true\"");
    try {
      publish(ivySettingsFile, defaultFileContents, true); // publish folder exists due to previous, so set overwrite
      Assert.fail("Publish with cleanup set to false and binaryDiff set to true should not be possible");
    } catch (BuildException e) {
      // expected
    }
    // publish did not take place so file should still be there
    assertTrue(publishPath + "previous.jar doesn't exist", svnDAO.fileExists(publishPath + "previous.jar", -1));
    assertEquals(1, svnDAO.list(publishPath, -1).size()); // and that should be the only file there
  }

  @Test
  public void testPublishMultiple_BinaryDiffFalse() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"false\"");
    publish(ivySettingsFile, defaultFileContents, true);
    String fileContents2 = "2.0 contents";
    publish(ivySettingsFile, fileContents2, "2.0", true);
    assertPublish("1.0", defaultFileContents, false);
    assertPublish("2.0", fileContents2, false);
  }

  @Test
  public void testPublishMultiple_BinaryDiffTrue() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile,
        "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, true);
    assertPublish("1.0", defaultFileContents, true); // check 1.0 with binary diff
    String fileContents2 = "2.0 contents";
    publish(ivySettingsFile, fileContents2, "2.0", true);
    assertPublish("2.0", fileContents2, true); // check 2.0 with binary diff
    // check that 1.0 publish is still there
    assertNonBinaryDiffPublish(defaultOrganisation, defaultModule, "1.0", defaultArtifactName, defaultFileContents);
  }
  
}
