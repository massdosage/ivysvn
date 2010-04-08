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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy publish" calls on the SvnRepository with binary diff set to false.
 */
public class SvnRepositoryNoBinaryDiffPublishTest extends BaseSvnRepositoryPublishTestCase {

  @Test
  public void testCleanupPublishFolderTrue_BinaryDiffFalse() throws IOException, SVNException {
    // first emulate a file left over from a previous publish to the same folder
    ISVNEditor commitEditor = getCommitEditor();
    String publishPath = defaultOrganisation + "/" + defaultModule + "/1.0/";
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
    String publishPath = defaultOrganisation + "/" + defaultModule + "/1.0/";
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
  public void testMultiplePublish_BinaryDiffFalse() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"false\"");
    publish(ivySettingsFile, defaultFileContents, true);
    String fileContents2 = "2.0 contents";
    publish(ivySettingsFile, fileContents2, "2.0", true);
    assertPublish("1.0", defaultFileContents, false);
    assertPublish("2.0", fileContents2, false);
  }

  @Test
  public void testPublish_Issue16() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(ivySettingsDataFolder, "ivysettings-issue16.xml"));
    publish(ivySettingsFile, defaultFileContents);

    String artifactPublishFolder = defaultOrganisation + "/" + defaultModule + "/jars/";
    String ivyPublishFolder = defaultOrganisation + "/" + defaultModule + "/ivys/";
    Map<String, String> artifacts = new HashMap<String, String>();
    artifacts.put("testartifact-1.0.jar", defaultFileContents);
    assertPublication(artifactPublishFolder, artifacts, ivyPublishFolder, "testmodule-1.0.xml");
  }

}
