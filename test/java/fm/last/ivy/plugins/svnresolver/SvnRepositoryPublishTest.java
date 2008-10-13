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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyPublish;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Tests "ivy publish" calls on the SvnRepository.
 */
public class SvnRepositoryPublishTest extends BaseIvyTestCase {

  private IvyPublish publish;

  private static final String DIST_PATH = TEST_TMP_PATH + "/build/dist";

  private String organisation = "testorg";

  private String module = "testmodule";

  // set in ivy-test-publish.xml
  private String artifactName = "testartifact.jar";

  @Before
  public void setUp() throws SVNException {
    super.setUp();
    publish = new IvyPublish();
    publish.setTaskName("publish");
    publish.setProject(project);
    publish.setArtifactspattern(DIST_PATH + "/[artifact].[ext]");
    publish.setOrganisation(organisation);
    publish.setModule(module);
    publish.setResolver("ivysvn");
  }

  private void assertPublish(String pubRevision, String fileContents) throws SVNException, IOException {
    this.assertPublish(organisation, module, pubRevision, artifactName, fileContents);
  }

  private void assertPublish(String organisation, String module, String pubRevision, String artifactName,
      String fileContents) throws SVNException, IOException {
    readRepository.setLocation(readRepository.getRepositoryRoot(true), true);

    String publishFolder = BASE_PUBLISH_PATH + "/" + organisation + "/" + module + "/" + pubRevision + "/";
    assertTrue(svnDAO.fileExists(publishFolder + "ivy.xml", -1));
    assertTrue(svnDAO.fileExists(publishFolder + "ivy.xml.sha1", -1));
    assertTrue(svnDAO.fileExists(publishFolder + "ivy.xml.md5", -1));
    assertTrue(svnDAO.fileExists(publishFolder + artifactName, -1));
    assertTrue(svnDAO.fileExists(publishFolder + artifactName + ".sha1", -1));
    assertTrue(svnDAO.fileExists(publishFolder + artifactName + ".md5", -1));

    File tempFile = new File(testTempFolder, "retrieved-" + artifactName);
    SVNURL sourceURL = SVNURL.parseURIEncoded(repositoryRoot + "/" + publishFolder + "/" + artifactName);
    svnDAO.getFile(sourceURL, tempFile, -1);
    assertEquals(fileContents, FileUtils.readFileToString(tempFile));
  }

  /**
   * Tests a publish call with default values.
   * 
   * @throws IOException
   * @throws SVNException
   */
  @Test
  public void testPublish() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    publish.setPubrevision("1.0");
    resolve(project, new File(TEST_CONF_PATH + "/ivy-test-publish.xml"));
    File fileToPublish = new File(DIST_PATH + "/" + artifactName);
    String fileContents = "testartifact 1.0";
    FileUtils.writeStringToFile(fileToPublish, fileContents);
    publish.execute();
    assertPublish("1.0", fileContents);
  }

  @Test
  public void testPublishOverwrite_False() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    publish.setPubrevision("1.0");
    publish.setOverwrite(false);
    resolve(project, new File(TEST_CONF_PATH + "/ivy-test-publish.xml"));
    File fileToPublish = new File(DIST_PATH + "/" + artifactName);
    String fileContents = "testartifact 1.0";
    FileUtils.writeStringToFile(fileToPublish, fileContents);
    publish.execute();
    assertPublish("1.0", fileContents);
    String fileContents2 = "overwrite set to false so this should not get published";
    FileUtils.writeStringToFile(fileToPublish, fileContents2);
    publish.execute();
    assertPublish("1.0", fileContents);
  }

  @Test
  public void testPublishOverwrite_True() throws IOException, SVNException {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    publish.setPubrevision("1.0");
    publish.setOverwrite(true);
    resolve(project, new File(TEST_CONF_PATH + "/ivy-test-publish.xml"));
    File fileToPublish = new File(DIST_PATH + "/" + artifactName);
    String fileContents = "testartifact 1.0";
    FileUtils.writeStringToFile(fileToPublish, fileContents);
    publish.execute();
    assertPublish("1.0", fileContents);
    String fileContents2 = "overwrite set to true so this should overwrite previous contents";
    FileUtils.writeStringToFile(fileToPublish, fileContents2);
    publish.execute();
    assertPublish("1.0", fileContents2);
  }

  // TODO: test publish with overwrite false, binarydiff true
  // TODO: test publish with overwrite true, binarydiff true
  // TODO: test publish with binarydiff true, different binary diff folder name
  // TODO: test publish with cleanup folders true
  // TODO: read through documentation and see if there are other publish test cases we have missed

  // TODO: sort out issue with readRepository reset root stuff, maybe this is in some way related to
  // web dav path problem???

}
