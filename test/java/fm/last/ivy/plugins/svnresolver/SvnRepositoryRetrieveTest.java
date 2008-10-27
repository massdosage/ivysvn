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

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyRetrieve;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * Tests "ivy retrieve" calls on the SvnRepository.
 */
public class SvnRepositoryRetrieveTest extends BaseIvyTestCase {

  // the pattern that Ivy should use to retrieve files *to*
  private static final String RETRIEVE_PATTERN = TEST_TMP_PATH + "/[artifact].[ext]";

  private IvyRetrieve retrieve;

  @Before
  public void setUp() throws SVNException {
    super.setUp();

    retrieve = new IvyRetrieve();
    retrieve.setProject(project);
    retrieve.setTaskName("retrieve");
    retrieve.setPattern(RETRIEVE_PATTERN);

    setUpRepository();
  }

  /**
   * Sets up the repository by adding some dummy data.
   * 
   * @throws SVNException If an error occurs adding the default repository data to Subversion.
   */
  private void setUpRepository() throws SVNException {
    // TODO: when run against WebDav repository even this fails and puts folders underneath each other
    ISVNEditor commitEditor = getCommitEditor();
    svnDAO.putFile(commitEditor, "acme widgets 4.5".getBytes(), BASE_PUBLISH_PATH + "/acme/widgets/4.5", "widgets.jar",
        false);
    svnDAO.putFile(commitEditor, "acme widgets 4.4".getBytes(), BASE_PUBLISH_PATH + "/acme/widgets/4.4", "widgets.jar",
        false);
    svnDAO.putFile(commitEditor, "acme gizmos 1.0".getBytes(), BASE_PUBLISH_PATH + "/acme/gizmos/1.0", "gizmos.jar",
        false);
    svnDAO.putFile(commitEditor, "constructus toolkit 1.1".getBytes(), BASE_PUBLISH_PATH + "/constructus/toolkit/1.1",
        "toolkit.jar", false);
    commitEditor.closeEdit();
  }

  @Test
  public void testRetrieve() throws Exception {
    File ivySettingsFile = prepareTestIvySettings(new File(TEST_CONF_PATH + "/ivysettings-default.xml"));
    project.setProperty("ivy.settings.file", ivySettingsFile.getAbsolutePath());
    resolve(project, new File(TEST_CONF_PATH + "/ivy-test-retrieve.xml"));
    retrieve.execute();

    // latest.integration should have been resolved to 4.5
    assertEquals("acme widgets 4.5", FileUtils.readFileToString(new File(testTempFolder, "widgets.jar")));
    // 1.0 should have been resolved directly
    assertEquals("constructus toolkit 1.1", FileUtils.readFileToString(new File(testTempFolder, "toolkit.jar")));
  }
  
  // TODO: test a retrieve with an inbetween dependency

  // TODO: test a retrieve with specified retrieve revision

}
