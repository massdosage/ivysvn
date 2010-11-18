package fm.last.ivy.plugins.svnresolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyPublish;
import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy publish" calls on the SvnRepository with binary diff set to true.
 */
public class SvnRepositoryBinaryDiffPublishTest extends BaseSvnRepositoryPublishTestCase {

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
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true);
  }

  @Test
  public void testBinaryDiff_OverwriteFalse() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
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
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
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
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\" binaryDiffFolderName=\""
        + binaryDiffFolderName + "\"");
    publish(ivySettingsFile, defaultFileContents);
    assertPublish("1.0", defaultFileContents, true, binaryDiffFolderName);
  }

  @Test
  public void testCleanupPublishFolderTrue_BinaryDiffTrue() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = defaultOrganisation + "/" + defaultModule + "/1.0/";
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
    String publishPath = defaultOrganisation + "/" + defaultModule + "/1.0/";
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
  public void testMultiplePublish_BinaryDiffTrue() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, true);
    assertPublish("1.0", defaultFileContents, true); // check 1.0 with binary diff
    String fileContents2 = "2.0 contents";
    publish(ivySettingsFile, fileContents2, "2.0", true);
    assertPublish("2.0", fileContents2, true); // check 2.0 with binary diff
    // check that 1.0 publish is still there
    Map<String, String> artifacts = new HashMap<String, String>();
    artifacts.put(defaultArtifactName, defaultFileContents);
    assertNonBinaryDiffPublish(defaultOrganisation, defaultModule, "1.0", artifacts, defaultIvyFileName);
  }

  /**
   * Tests publishing where there is a [type] folder under the revision folder.
   * 
   * @see http://code.google.com/p/ivysvn/issues/detail?id=20
   * @throws IOException
   * @throws SVNException
   */
  @Test
  public void testPublish_Issue20() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(ivySettingsDataFolder, "ivysettings-issue20.xml"));
    publish(ivySettingsFile, defaultFileContents);

    String artifactPublishFolder = defaultOrganisation + "/" + defaultModule + "/1.0/jars/";
    String ivyPublishFolder = defaultOrganisation + "/" + defaultModule + "/1.0/ivys/";
    Map<String, String> artifacts = new HashMap<String, String>();
    artifacts.put("testartifact.jar", defaultFileContents);
    assertPublication(artifactPublishFolder, artifacts, ivyPublishFolder, "ivy.xml");

    String artifactBinaryDiffPublishFolder = defaultOrganisation + "/" + defaultModule + "/1.0/jars/";
    String ivyBinaryDiffPublishFolder = defaultOrganisation + "/" + defaultModule + "/1.0/ivys/";
    artifacts = new HashMap<String, String>();
    artifacts.put("testartifact.jar", defaultFileContents);
    assertPublication(artifactBinaryDiffPublishFolder, artifacts, ivyBinaryDiffPublishFolder, "ivy.xml");
  }

  @Test
  public void testNonIncrementalVersions_BinaryDiff() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    publish(ivySettingsFile, defaultFileContents, "2.5.5", false);
    assertPublish("2.5.5", defaultFileContents, true);
    publish(ivySettingsFile, defaultFileContents, "2.0.8", false);
    assertPublish("2.0.8", defaultFileContents, true);
  }

  /**
   * This checks that a revision can be called TEST when doing a default binary diff. This used to cause an issue as
   * TEST was seen as a revision within the default binary publish path of LATEST.
   * 
   * @throws IOException
   * @throws SVNException
   */
  @Test
  public void testBinaryDiff_TESTRevision() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    // revision TEST is contained within binary diff folder name LATEST
    String revision = "TEST";
    IvyPublish ivyPublish = createIvyPublish(revision, false);

    File fileToPublish1 = new File(DIST_PATH + "/" + "testartifact1.jar");
    String fileContents1 = "testArtifact1 - contents";
    FileUtils.writeStringToFile(fileToPublish1, fileContents1);

    File fileToPublish2 = new File(DIST_PATH + "/" + "testartifact2.jar");
    String fileContents2 = "testArtifact2 - contents";
    FileUtils.writeStringToFile(fileToPublish2, fileContents2);

    File ivyPublishFile = new File(ivysDataFolder, "ivy-test-publish-multiple-artifacts.xml");
    publish(ivyPublishFile, ivySettingsFile, ivyPublish);

    Map<String, String> expectedArtifacts = new HashMap<String, String>();
    assertPublish(revision, expectedArtifacts, true);
  }

  /**
   * When doing a binary diff we need to replace the revision with "LATEST", if the revision occurs more than once in
   * the path we can't figure out which part of the path is the revision and thus cannot continue.
   * 
   * @throws Throwable
   */
  @Test(expected = IllegalStateException.class)
  public void testBinaryDiff_testRevision() throws Throwable {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    // revision "test" is contained within the organisation, module and artifact names
    String revision = "test";
    IvyPublish ivyPublish = createIvyPublish(revision, false);

    File fileToPublish1 = new File(DIST_PATH + "/" + "testartifact.jar");
    String fileContents1 = "testArtifact1 - contents";
    FileUtils.writeStringToFile(fileToPublish1, fileContents1);

    File ivyPublishFile = new File(ivysDataFolder, "ivy-test-publish.xml");
    try {
      publish(ivyPublishFile, ivySettingsFile, ivyPublish);
    } catch (BuildException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testBinaryDiff_RevisionInArtifactName() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    // revision "artifact" is contained within the artifact names
    String revision = "artifact";
    IvyPublish ivyPublish = createIvyPublish(revision, false);

    File fileToPublish1 = new File(DIST_PATH + "/" + "testartifact1.jar");
    String fileContents1 = "testArtifact1 - contents";
    FileUtils.writeStringToFile(fileToPublish1, fileContents1);

    File fileToPublish2 = new File(DIST_PATH + "/" + "testartifact2.jar");
    String fileContents2 = "testArtifact2 - contents";
    FileUtils.writeStringToFile(fileToPublish2, fileContents2);

    File ivyPublishFile = new File(ivysDataFolder, "ivy-test-publish-multiple-artifacts.xml");
    publish(ivyPublishFile, ivySettingsFile, ivyPublish);

    Map<String, String> expectedArtifacts = new HashMap<String, String>();
    assertPublish(revision, expectedArtifacts, true);
  }

  @Ignore("See http://code.google.com/p/ivysvn/issues/detail?id=22 - this test reproduces the issue but we don't have a fix yet")
  @Test
  public void testBinaryDiff_PublishSubFolderIssue22() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");

    String revision = "1.0";
    IvyPublish ivyPublish = createIvyPublish(revision, false);

    File artifactFolder = new File(DIST_PATH, "SubFolder");
    artifactFolder.mkdir();
    File fileToPublish1 = new File(artifactFolder, "MyArtifact.txt");
    String fileContents1 = "my artifact - contents";
    FileUtils.writeStringToFile(fileToPublish1, fileContents1);

    File ivyPublishFile = new File(ivysDataFolder, "ivy-test-publish-sub-folder.xml");
    publish(ivyPublishFile, ivySettingsFile, ivyPublish);

    // TODO: the asserts for this.
    // Map<String, String> expectedArtifacts = new HashMap<String, String>();
    // assertPublish(revision, expectedArtifacts, true);
  }

}
