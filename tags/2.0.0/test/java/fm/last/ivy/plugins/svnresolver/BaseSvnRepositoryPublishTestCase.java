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
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Base class for test cases which test publish functionality.
 */
public abstract class BaseSvnRepositoryPublishTestCase extends BaseIvyTestCase {

  /**
   * Asserts all the effects of a publish action, using default values where necessary.
   * 
   * @param pubRevision The publication revision.
   * @param artifactFileContents The expected artifact file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertPublish(String pubRevision, String artifactFileContents, boolean binaryDiff,
      String binaryDiffFolderName) throws SVNException, IOException {
    this.assertPublish(defaultOrganisation, defaultModule, pubRevision, defaultArtifactName, artifactFileContents,
        defaultIvyFileName, binaryDiff, binaryDiffFolderName);
  }

  /**
   * Asserts all the effects of a publish action, using default values where necessary.
   * 
   * @param pubRevision The publication revision.
   * @param artifactFileContents The expected artifact file contents.
   * @param binaryDiff Binary diff value for the publish action.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertPublish(String pubRevision, String artifactFileContents, boolean binaryDiff)
    throws SVNException, IOException {
    this.assertPublish(defaultOrganisation, defaultModule, pubRevision, defaultArtifactName, artifactFileContents,
        defaultIvyFileName, binaryDiff, SvnRepository.DEFAULT_BINARY_DIFF_FOLDER_NAME);
  }

  /**
   * Asserts all the effects of a publish action.
   * 
   * @param organisation The organisation.
   * @param module The module.
   * @param pubRevision The publication revision.
   * @param artifactName The name of the published artifact.
   * @param artifactFileContents The expected artifact file contents.
   * @param ivyFileName Expected published ivy file name.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertPublish(String organisation, String module, String pubRevision, String artifactName,
      String artifactFileContents, String ivyFileName, boolean binaryDiff, String binaryDiffFolderName)
    throws SVNException, IOException {
    assertBinaryDiffPublish(organisation, module, artifactName, artifactFileContents, ivyFileName, binaryDiff,
        binaryDiffFolderName);
    // now check publish to actual revision folder
    assertNonBinaryDiffPublish(organisation, module, pubRevision, artifactName, artifactFileContents, ivyFileName);
  }

  /**
   * Asserts the binary diff portion of a publish action.
   * 
   * @param organisation The organisation.
   * @param module The module.
   * @param artifactName The artifact name.
   * @param artifactFileContents The expected artifact file contents.
   * @param ivyFileName Expected published ivy file name.
   * @param binaryDiff Binary diff value for the publish action.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertBinaryDiffPublish(String organisation, String module, String artifactName,
      String artifactFileContents, String ivyFileName, boolean binaryDiff, String binaryDiffFolderName)
    throws SVNException, IOException {
    // first setup binary diff path
    String publishFolder = BASE_PUBLISH_PATH + "/" + organisation + "/" + module + "/" + binaryDiffFolderName + "/";
    if (binaryDiff) {
      assertPublication(publishFolder, artifactName, artifactFileContents, publishFolder, ivyFileName);
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
   * @param artifactFileContents The expected artifact file contents.
   * @param ivyFileName Expected published ivy file name.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertNonBinaryDiffPublish(String organisation, String module, String pubRevision,
      String artifactName, String artifactFileContents, String ivyFileName) throws SVNException, IOException {
    String publishFolder = BASE_PUBLISH_PATH + "/" + organisation + "/" + module + "/" + pubRevision + "/";
    assertPublication(publishFolder, artifactName, artifactFileContents, publishFolder, ivyFileName);
  }

  /**
   * Asserts all the effects of a publish action under a single folder in Subversion.
   * 
   * @param artifactPublishFolder The folder in Subversion that artifact files should have been published to.
   * @param artifactName The name of the published artifact.
   * @param artifactFileContents The expected artifact file contents.
   * @param ivyPublishFolder The folder in Subversion that ivy files should have been published to.
   * @param ivyFileName Expected published ivy file name.
   * @throws SVNException If an error occurs checking the files in Subversion.
   * @throws IOException If an error occurs reading the file contents.
   */
  protected void assertPublication(String artifactPublishFolder, String artifactName, String artifactFileContents,
      String ivyPublishFolder, String ivyFileName) throws SVNException, IOException {
    assertArtifactPublished(ivyPublishFolder, ivyFileName);
    assertArtifactPublished(artifactPublishFolder, artifactName);
    File tempFile = new File(testTempFolder, "retrieved-" + artifactName);
    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + artifactPublishFolder + "/" + artifactName);
    svnDAO.getFile(sourceURL, tempFile, -1);
    assertEquals(artifactFileContents, FileUtils.readFileToString(tempFile));
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

}
