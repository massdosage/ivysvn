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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyPublish;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;

/**
 * Unit test which tests publishing artifacts and then retrieving them. These aren't unit tests in the strict sense of
 * the term but are easier to create and are usually based on real world examples.
 */
public class SvnRepositoryPublishAndRetrieveTest extends BaseSvnRepositoryPublishTestCase {

  @Test
  public void testMilestone() throws IOException, SVNException, InterruptedException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    IvyPublish publish = createIvyPublish("1.0", false);
    publish.setStatus("milestone");
    String milestone1 = "milestone 1.0";
    publish(ivySettingsFile, milestone1, publish);
    assertPublish("1.0", milestone1, true);

    publish = createIvyPublish("1.0.1", false);
    publish.setStatus("milestone");
    String milestone101 = "milestone 1.0.1";
    publish(ivySettingsFile, milestone101, publish);
    assertPublish("1.0.1", milestone101, true);

    publish = createIvyPublish("1.0.3", false);
    publish.setStatus("release");
    String release103 = "release 1.0.3";
    publish(ivySettingsFile, release103, publish);

    publish = createIvyPublish("1.0.4", false);
    publish.setStatus("milestone");
    String milestone104 = "milestone 1.0.4";
    publish(ivySettingsFile, milestone104, publish);
    assertPublish("1.0.4", milestone104, true);

    publish = createIvyPublish("1.0.5", false);
    String nonMilestone = "non-milestone 1.0.5";
    publish(ivySettingsFile, nonMilestone, publish);
    assertPublish("1.0.5", nonMilestone, true);

    File ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.milestone");
    retrieve(ivyFile);
    assertEquals(milestone104, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
    cleanupTempFolder();

    ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.integration");
    retrieve(ivyFile);
    assertEquals(nonMilestone, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
    cleanupTempFolder();

    ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.release");
    retrieve(ivyFile);
    assertEquals(release103, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
  }

  @Test
  public void testIssue13() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(ivySettingsDataFolder, "ivysettings-issue13.xml"));
    IvyPublish publish = createIvyPublish("1.0.1", false);
    publish.setStatus("milestone");
    String milestone101 = "milestone 1.0.1";
    publish(ivySettingsFile, milestone101, publish);

    Map<String, String> artifacts = new HashMap<String, String>();
    artifacts.put("testartifact-1.0.1.jar", milestone101);
    assertPublish(defaultOrganisation, defaultModule, "1.0.1", artifacts, "ivy-1.0.1.xml", true,
        SvnRepository.DEFAULT_BINARY_DIFF_FOLDER_NAME);

    publish = createIvyPublish("1.0.2", false);
    publish.setStatus("milestone");
    String milestone102 = "milestone 1.0.2";
    publish(ivySettingsFile, milestone102, publish);
    artifacts = new HashMap<String, String>();
    artifacts.put("testartifact-1.0.2.jar", milestone102);
    assertPublish(defaultOrganisation, defaultModule, "1.0.2", artifacts, "ivy-1.0.2.xml",
        true, SvnRepository.DEFAULT_BINARY_DIFF_FOLDER_NAME);

    File ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.milestone");
    retrieve(ivyFile, DEFAULT_RETRIEVE_TO_PATTERN, ivySettingsFile);
    assertEquals(milestone102, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
  }

  // TODO: maybe move these into the other test?
  // TODO: need to re-organise publish tests so that they are more modular, one way would be whether they are with
  // binary diff on or off, and we should have two versions of this test - one for binary diff and one for non
  // TODO: need to figure out if above does not reproduce issue with publishing jms support - maybe it only happens with
  // svn://? otherwise need to investigate further
  // to reproduce
  // also maybe separate tests which are based on issue numbers
  @Test
  @Ignore()
  public void testPublishMultipleArtifacts() throws IOException, SVNException, InterruptedException {
    File ivySettingsFile = prepareTestIvySettings(defaultIvySettingsFile, "binaryDiff=\"true\"");
    IvyPublish ivyPublish = createIvyPublish("1.0", false);
    ivyPublish.setStatus("milestone");

    File fileToPublish1 = new File(DIST_PATH + "/" + "testartifact1.jar");
    String fileContents1 = "testArtifact1 - contents";
    FileUtils.writeStringToFile(fileToPublish1, fileContents1);

    File fileToPublish2 = new File(DIST_PATH + "/" + "testartifact2.jar");
    String fileContents2 = "testArtifact2 - contents";
    FileUtils.writeStringToFile(fileToPublish2, fileContents2);

    File ivyPublishFile = new File(ivysDataFolder, "ivy-test-publish-multiple-artifacts.xml");
    publish(ivyPublishFile, ivySettingsFile, ivyPublish);

    System.out.println("DONE");

    // assertPublish("1.0", milestone1, true);
    // File ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.milestone");
    // retrieve(ivyFile);
    // assertEquals(milestone104, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
    // cleanupTempFolder();
    //
    // ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.integration");
    // retrieve(ivyFile);
    // assertEquals(nonMilestone, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
    // cleanupTempFolder();
    //
    // ivyFile = prepareTestIvyFile(defaultIvyXml, "latest.release");
    // retrieve(ivyFile);
    // assertEquals(release103, FileUtils.readFileToString(new File(testTempFolder, defaultArtifactName)));
  }

}
