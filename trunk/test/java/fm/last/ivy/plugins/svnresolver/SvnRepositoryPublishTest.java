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

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyPublish;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy publish" calls on the SvnRepository.
 */
public class SvnRepositoryPublishTest extends BaseIvyTestCase {

  private static final String DIST_PATH = TEST_TMP_PATH + "/build/dist";

  // ivy uses this folder for creating ivy.xml files for publish
  private File tempDistFolder = new File(DIST_PATH);

  private String defaultOrganisation = "testorg";

  private String defaultModule = "testmodule";

  // set in ivy-test-publish.xml
  private String defaultArtifactName = "testartifact.jar";

  private File defaultFileToPublish = new File(DIST_PATH + "/" + defaultArtifactName);

  private String defaultFileContents = "testartifact 1.0";

  @Before
  public void setUp() throws SVNException {
    super.setUp();
  }

  /**
   * Asserts all the effects of a publish action, using default values where necessary.
   * 
   * @param pubRevision The publication revision.
   * @param fileContents The expected file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertPublish(String pubRevision, String fileContents, boolean binaryDiff, String binaryDiffFolderName)
    throws SVNException, IOException {
    this.assertPublish(defaultOrganisation, defaultModule, pubRevision, defaultArtifactName, fileContents, binaryDiff,
        binaryDiffFolderName);
  }

  /**
   * Asserts all the effects of a publish action, using default values where necessary.
   * 
   * @param pubRevision The publication revision.
   * @param fileContents The expected file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertPublish(String pubRevision, String fileContents, boolean binaryDiff) throws SVNException,
    IOException {
    this.assertPublish(defaultOrganisation, defaultModule, pubRevision, defaultArtifactName, fileContents, binaryDiff,
        SvnRepository.DEFAULT_BINARY_DIFF_FOLDER_NAME);
  }

  /**
   * Asserts all the effects of a publish action.
   * 
   * @param organisation The organisation.
   * @param module The module.
   * @param pubRevision The publication revision.
   * @param artifactName The name of the published artifact.
   * @param fileContents The expected file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertPublish(String organisation, String module, String pubRevision, String artifactName,
      String fileContents, boolean binaryDiff, String binaryDiffFolderName) throws SVNException, IOException {
    assertBinaryDiffPublish(organisation, module, artifactName, fileContents, binaryDiff, binaryDiffFolderName);
    // now check publish to actual revision folder
    assertNonBinaryDiffPublish(organisation, module, pubRevision, artifactName, fileContents);
  }

  /**
   * Asserts the binary diff portion of a publish action.
   * 
   * @param organisation The organisation.
   * @param module The module.
   * @param artifactName The artifact name.
   * @param fileContents The expected file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertBinaryDiffPublish(String organisation, String module, String artifactName, String fileContents,
      boolean binaryDiff, String binaryDiffFolderName) throws SVNException, IOException {
    // first setup binary diff path
    String publishFolder = BASE_PUBLISH_PATH + "/" + organisation + "/" + module + "/" + binaryDiffFolderName + "/";
    if (binaryDiff) {
      assertPublication(publishFolder, artifactName, fileContents);
    } else {
      assertFalse("Binary diff folder found at " + publishFolder, svnDAO.folderExists(publishFolder, -1, false));
    }
  }

  /**
   * Asserts the non-binary diff portion of a publish action.
   * 
   * @param organisation The organisation.
   * @param module The module.
   * @param pubRevision The publication revision.
   * @param artifactName The artifact name.
   * @param fileContents The expected file contents.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertNonBinaryDiffPublish(String organisation, String module, String pubRevision, String artifactName,
      String fileContents) throws SVNException, IOException {
    String publishFolder = BASE_PUBLISH_PATH + "/" + organisation + "/" + module + "/" + pubRevision + "/";
    assertPublication(publishFolder, artifactName, fileContents);
  }

  /**
   * Asserts all the effects of a publish action under a single folder in Subversion.
   * 
   * @param publishFolder The folder in Subversion that files should have been published to.
   * @param artifactName The name of the published artifact.
   * @param fileContents The expected file contents.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  private void assertPublication(String publishFolder, String artifactName, String fileContents) throws SVNException,
    IOException {
    assertArtifactPublished(publishFolder, "ivy.xml");
    assertArtifactPublished(publishFolder, artifactName);
    File tempFile = new File(testTempFolder, "retrieved-" + artifactName);
    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + publishFolder + "/" + artifactName);
    svnDAO.getFile(sourceURL, tempFile, -1);
    assertEquals(fileContents, FileUtils.readFileToString(tempFile));

  }

  /**
   * Asserts that an artifact has been published to the repository, along with checksums.
   * 
   * @param publishFolder Folder that artifact should have been published to.
   * @param artifactFileName The name of the artifact.
   * @throws SVNException If an error occurs checking whether the artifact or its checksums exist.
   */
  private void assertArtifactPublished(String publishFolder, String artifactFileName) throws SVNException {
    if (!publishFolder.endsWith("/")) {
      publishFolder = publishFolder + "/";
    }
    assertTrue(publishFolder + artifactFileName + " doesn't exist", svnDAO.fileExists(publishFolder + artifactFileName,
        -1));
    assertTrue(publishFolder + artifactFileName + ".sha1 doesn't exist", svnDAO.fileExists(publishFolder
        + artifactFileName + ".sha1", -1));
    assertTrue(publishFolder + artifactFileName + ".md5 doesn't exist", svnDAO.fileExists(publishFolder
        + artifactFileName + ".md5", -1));
  }

  /**
   * Performs a default publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @param revision Revision to be published.
   * @param overwrite Overwrite value to set on publish operation (if null no value will be set and default will be
   *          used).
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  private void publish(File ivySettingsFile, String fileContents, String revision, Boolean overwrite)
    throws IOException {
    Project project = createProject();
    IvyPublish publish = new IvyPublish();
    publish.setTaskName("publish");
    publish.setProject(project);
    publish.setArtifactspattern(DIST_PATH + "/[artifact].[ext]");
    publish.setOrganisation(defaultOrganisation);
    publish.setModule(defaultModule);
    publish.setResolver("ivysvn");

    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    publish.setPubrevision(revision);
    if (overwrite != null) {
      publish.setOverwrite(overwrite);
    }
    resolve(project, new File(TEST_CONF_PATH + "/ivy-test-publish.xml"));
    FileUtils.writeStringToFile(defaultFileToPublish, fileContents);
    publish.execute();
    FileUtils.deleteDirectory(tempDistFolder);
  }

  /**
   * Performs a default publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @param overwrite Overwrite value to set on publish operation (if null no value will be set and default will be
   *          used).
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  private void publish(File ivySettingsFile, String fileContents, Boolean overwrite) throws IOException {
    this.publish(ivySettingsFile, fileContents, "1.0", overwrite);
  }

  /**
   * Performs a default publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  private void publish(File ivySettingsFile, String fileContents) throws IOException {
    publish(ivySettingsFile, fileContents, null);
  }

  @Test
  public void testDefaultPublish() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true);
  }

  @Test
  public void testPublishOverwrite_False() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
        "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true);
  }

  @Test
  public void testBinaryDiff_OverwriteFalse() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
        "binaryDiff=\"false\"");
    publish(ivySettingsFile, defaultFileContents, true);
    String fileContents2 = "2.0 contents";
    publish(ivySettingsFile, fileContents2, "2.0", true);
    assertPublish("1.0", defaultFileContents, false);
    assertPublish("2.0", fileContents2, false);
  }

  @Test
  public void testPublishMultiple_BinaryDiffTrue() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"),
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
