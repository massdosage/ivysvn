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

import java.io.File;
import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

import fm.last.ivy.plugins.svnresolver.test.TestProperties;

/**
 * Base class for IvySvn tests.
 */
public abstract class BaseTestCase {

  protected static final String TEST_PATH = "test";
  protected static final String TEST_TMP_PATH = TEST_PATH + "/tmp";
  protected static final String TEST_DATA_PATH = TEST_PATH + "/data";
  protected static final String TEST_CONF_PATH = TEST_PATH + "/conf";

  protected String repositoryRoot = TestProperties.getInstance().getProperty(
      TestProperties.PROPERTY_SVN_REPOSITORY_ROOT);
  protected String svnUserName = TestProperties.getInstance().getProperty(TestProperties.PROPERTY_SVN_USER_NAME);
  protected String svnPassword = TestProperties.getInstance().getProperty(TestProperties.PROPERTY_SVN_PASSWORD);

  /**
   * A temporary folder which tests can use to write data while they run, will be cleaned inbetween each test.
   */
  protected File testTempFolder = new File(TEST_TMP_PATH);

  /**
   * The base test data folder (i.e. test/data);
   */
  protected File baseTestDataFolder = new File(TEST_DATA_PATH);

  /**
   * A test data folder for a specific test (only valid for tests which put their data in a folder under "data" which
   * matches their fully qualified class name).
   */
  protected File testDataFolder = new File(baseTestDataFolder, getClass().getName().replaceAll("\\.", "/"));

  /**
   * Controls whether the temp folder should be deleted between tests.
   */
  protected boolean cleanupTempFolder = true;

  /**
   * Controls whether the temp folder in the Subversion repository should be deleted between tests.
   */
  protected boolean cleanupSvn = true;

  protected SVNURL repositoryRootURL = null;
  protected SVNRepository readRepository;

  protected SvnDao svnDAO = null;

  @Before
  public void setUp() throws SVNException {
    FSRepositoryFactory.setup();
    DAVRepositoryFactory.setup();
    SVNRepositoryFactoryImpl.setup();
    testTempFolder.mkdirs();

    repositoryRootURL = SVNURL.parseURIEncoded(repositoryRoot);
    readRepository = SvnUtils
        .createRepository(repositoryRootURL, svnUserName, svnPassword, null, null, -1, null, false);
    svnDAO = new SvnDao(readRepository);
  }

  @After
  public void cleanupRepository() throws SVNException {
    if (cleanupSvn) {
      ISVNEditor commitEditor = getCommitEditor();
      commitEditor.deleteEntry(TEST_PATH, -1);
      commitEditor.closeEdit();
    }
  }

  /**
   * Cleanup the temp folder which tests can use to write data.
   * 
   * @throws IOException
   */
  @After
  public void cleanupTempFolder() throws IOException {
    if (cleanupTempFolder) {
      FileUtils.deleteDirectory(testTempFolder);
      if (testTempFolder.exists()) {
        Assert.fail("Failed to delete " + testTempFolder.getAbsolutePath());
      }
    }
  }

  /**
   * Utility function to get an editor that can be used for commit operations.
   * 
   * @return A commit editor.
   * @throws SVNException If an error occurs getting the commit editor.
   */
  protected ISVNEditor getCommitEditor() throws SVNException {
    SVNRepository commitRepository = SvnUtils.createRepository(repositoryRootURL, svnUserName, svnPassword, null, null,
        -1, null, false);
    SVNURL root = commitRepository.getRepositoryRoot(true);
    commitRepository.setLocation(root, false);
    ISVNEditor commitEditor = commitRepository.getCommitEditor("unit testing " + this.getClass().getName(), null);
    commitEditor.openRoot(-1);
    return commitEditor;
  }

  /**
   * Fails the test.
   * 
   * @param t Throwable that should be used as the reason for failing.
   */
  protected void fail(Throwable t) {
    t.printStackTrace();
    throw new AssertionFailedError(t.getMessage());
  }

}
