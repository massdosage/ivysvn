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
import org.apache.tools.ant.Project;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Base class for test cases which test publish functionality.
 */
public abstract class BaseSvnRepositoryPublishTestCase extends BaseIvyTestCase {

  protected static final String DIST_PATH = TEST_TMP_PATH + "/build/dist";

  // ivy uses this folder for creating ivy.xml files for publish
  protected File tempDistFolder = new File(DIST_PATH);

  protected String defaultOrganisation = "testorg";

  protected String defaultModule = "testmodule";

  // set in ivy-test-publish.xml
  protected String defaultArtifactName = "testartifact.jar";

  protected File defaultFileToPublish = new File(DIST_PATH + "/" + defaultArtifactName);

  protected String defaultFileContents = "testartifact 1.0";

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
  protected void assertPublish(String pubRevision, String fileContents, boolean binaryDiff, String binaryDiffFolderName)
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
  protected void assertPublish(String pubRevision, String fileContents, boolean binaryDiff) throws SVNException,
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
  protected void assertPublish(String organisation, String module, String pubRevision, String artifactName,
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
  protected void assertBinaryDiffPublish(String organisation, String module, String artifactName, String fileContents,
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
  protected void assertNonBinaryDiffPublish(String organisation, String module, String pubRevision,
      String artifactName, String fileContents) throws SVNException, IOException {
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
  protected void assertPublication(String publishFolder, String artifactName, String fileContents) throws SVNException,
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
  protected void assertArtifactPublished(String publishFolder, String artifactFileName) throws SVNException {
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
   * Creates an IvyPublish object filled in with default values.
   * 
   * @param revision Revision to be published.
   * @param overwrite Overwrite value to set on publish operation (if null no value will be set and default will be
   *          used).
   * @return An IvyPublish object.
   */
  protected IvyPublish createIvyPublish(String revision, Boolean overwrite) {
    IvyPublish ivyPublish = new IvyPublish();
    ivyPublish.setTaskName("publish");
    ivyPublish.setArtifactspattern(DIST_PATH + "/[artifact].[ext]");
    ivyPublish.setOrganisation(defaultOrganisation);
    ivyPublish.setModule(defaultModule);
    ivyPublish.setResolver("ivysvn");
    ivyPublish.setPubrevision(revision);
    if (overwrite != null) {
      ivyPublish.setOverwrite(overwrite);
    }
    return ivyPublish;
  }

  /**
   * Performs a publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @param revision Revision to be published.
   * @param overwrite Overwrite value to set on publish operation (if null no value will be set and default will be
   *          used).
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  protected void publish(File ivySettingsFile, String fileContents, String revision, Boolean overwrite)
    throws IOException {
    IvyPublish publish = createIvyPublish(revision, overwrite);
    publish(ivySettingsFile, fileContents, publish);
  }

  /**
   * Performs a publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @param ivyPublish An initialised IvyPublish object.
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  protected void publish(File ivySettingsFile, String fileContents, IvyPublish ivyPublish) throws IOException {
    Project project = createProject();
    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    ivyPublish.setProject(project);
    resolve(project, new File(ivysDataFolder, "ivy-test-publish.xml"));
    FileUtils.writeStringToFile(defaultFileToPublish, fileContents);
    ivyPublish.execute();
    FileUtils.deleteDirectory(tempDistFolder);
  }

  /**
   * Performs a publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @param overwrite Overwrite value to set on publish operation (if null no value will be set and default will be
   *          used).
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  protected void publish(File ivySettingsFile, String fileContents, Boolean overwrite) throws IOException {
    this.publish(ivySettingsFile, fileContents, "1.0", overwrite);
  }

  /**
   * Performs a default publish operation.
   * 
   * @param ivySettingsFile Ivy settings file.
   * @param fileContents String contents to be published as a file.
   * @throws IOException If an error occurs writing the file contents to a File to be published.
   */
  protected void publish(File ivySettingsFile, String fileContents) throws IOException {
    publish(ivySettingsFile, fileContents, "1.0", null);
  }

}
