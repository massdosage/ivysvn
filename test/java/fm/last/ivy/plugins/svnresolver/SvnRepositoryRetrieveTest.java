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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy retrieve" calls on the SvnRepository.
 */
public class SvnRepositoryRetrieveTest extends BaseIvyTestCase {

  @Before
  public void setUp() throws SVNException {
    super.setUp();
    setUpRepository();
  }

  /**
   * Sets up the repository by adding some dummy data.
   * 
   * @throws SVNException If an error occurs adding the default repository data to Subversion.
   */
  private void setUpRepository() throws SVNException {
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, BASE_PUBLISH_PATH, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme", null, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme/widgets", null, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme/widgets/4.5", null, -1);
    svnDAO.putFile(commitEditor, "acme widgets 4.5".getBytes(), BASE_PUBLISH_PATH + "/acme/widgets/4.5", "widgets.jar",
        false);
    commitEditor.closeDir(); // acme/widgets/4.5
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme/widgets/4.4", null, -1);
    svnDAO.putFile(commitEditor, "acme widgets 4.4".getBytes(), BASE_PUBLISH_PATH + "/acme/widgets/4.4", "widgets.jar",
        false);
    commitEditor.closeDir(); // acme/widgets/4.4
    commitEditor.closeDir(); // acme/widgets
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme/gizmos", null, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/acme/gizmos/1.0", null, -1);
    svnDAO.putFile(commitEditor, "acme gizmos 1.0".getBytes(), BASE_PUBLISH_PATH + "/acme/gizmos/1.0", "gizmos.jar",
        false);
    commitEditor.closeDir(); // acme/gizmos/1.0
    commitEditor.closeDir(); // acme/gizmos
    commitEditor.closeDir(); // acme
    commitEditor.addDir(BASE_PUBLISH_PATH + "/constructus", null, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/constructus/toolkit", null, -1);
    commitEditor.addDir(BASE_PUBLISH_PATH + "/constructus/toolkit/1.1", null, -1);
    svnDAO.putFile(commitEditor, "constructus toolkit 1.1".getBytes(), BASE_PUBLISH_PATH + "/constructus/toolkit/1.1",
        "toolkit.jar", false);
    commitEditor.closeDir(); // constructus/toolkit/1.1
    commitEditor.closeDir(); // constructus/toolkit
    commitEditor.closeDir(); // constructus
    commitEditor.closeEdit();
  }

  @Test
  public void testRetrieve() throws IOException {
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve.xml"));
    assertEquals("acme widgets 4.4", FileUtils.readFileToString(new File(testTempFolder, "widgets.jar")));
  }

  @Test
  public void testRetrieveLatestIntegration() throws IOException {
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve-latest-integration.xml"));
    // latest.integration should resolve to 4.5
    assertEquals("acme widgets 4.5", FileUtils.readFileToString(new File(testTempFolder, "widgets.jar")));
    // 1.1 should have been resolved directly
    assertEquals("constructus toolkit 1.1", FileUtils.readFileToString(new File(testTempFolder, "toolkit.jar")));
  }

  @Test
  public void testRetrieve_Dependent() throws SVNException, IOException {
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, BASE_PUBLISH_PATH + "/constructus/toolkituser/2.0", -1);
    svnDAO.putFile(commitEditor, "constructus toolkit user 2.0".getBytes(), BASE_PUBLISH_PATH
        + "/constructus/toolkituser/2.0", "toolkituser.jar", false);

    String toolkitUserIvyFile = FileUtils.readFileToString(new File(ivysDataFolder, "ivy-constructus-toolkituser.xml"));
    svnDAO.putFile(commitEditor, toolkitUserIvyFile.getBytes(), BASE_PUBLISH_PATH + "/constructus/toolkituser/2.0",
        "ivy.xml", false);

    commitEditor.closeEdit();
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve-dependent.xml"));
    // check the required file was downloaded
    assertEquals("constructus toolkit user 2.0", FileUtils
        .readFileToString(new File(testTempFolder, "toolkituser.jar")));
    // now check that the artifact the toolkituser was marked to depend on was also downloaded
    assertEquals("constructus toolkit 1.1", FileUtils.readFileToString(new File(testTempFolder, "toolkit.jar")));
  }

  @Test
  public void testRetrieve_Dependent_TransitiveFalse() throws SVNException, IOException {
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.createFolders(commitEditor, BASE_PUBLISH_PATH + "/constructus/toolkituser/2.0", -1);
    svnDAO.putFile(commitEditor, "constructus toolkit user 2.0".getBytes(), BASE_PUBLISH_PATH
        + "/constructus/toolkituser/2.0", "toolkituser.jar", false);
    String toolkitUserIvyFile = FileUtils.readFileToString(new File(ivysDataFolder, "ivy-constructus-toolkituser.xml"));
    svnDAO.putFile(commitEditor, toolkitUserIvyFile.getBytes(), BASE_PUBLISH_PATH + "/constructus/toolkituser/2.0",
        "ivy.xml", false);
    commitEditor.closeEdit();
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve-dependent-transitive-false.xml"));
    // check the required file was downloaded
    assertEquals("constructus toolkit user 2.0", FileUtils
        .readFileToString(new File(testTempFolder, "toolkituser.jar")));
    // now check that the artifact the toolkituser was marked to depend on was NOT downloaded
    assertFalse(new File(testTempFolder, "toolkit.jar").exists());
  }

  @Test(expected = BuildException.class)
  public void testRetrieve_NonExistent() throws IOException {
    // this ivy xml points to a file which is not in created by setUpRepository()
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve-dependent.xml"));
  }
  
  @Test
  public void testRetrieve_Issue16() throws IOException {
    File ivySettingsFile = prepareTestIvySettings(new File(ivySettingsDataFolder, "ivysettings-issue16.xml"));
    // easier to just do a publish rather than create files, folders by hand
    publish(ivySettingsFile, defaultFileContents, "1.0", false);
    String fileContents2 = "testartifact 2.0";
    publish(ivySettingsFile, fileContents2, "2.0", false);
    
    retrieve(new File(ivysDataFolder, "ivy-test-retrieve-issue16.xml"), DEFAULT_RETRIEVE_TO_PATTERN, ivySettingsFile);
    assertEquals(fileContents2, FileUtils.readFileToString(new File(testTempFolder, "testartifact.jar")));
  }

}
