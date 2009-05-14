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

import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorMessage;
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

  protected String svnRepositoryRoot = TestProperties.getInstance().getProperty(
      TestProperties.PROPERTY_SVN_REPOSITORY_ROOT);

  protected String ivyRepositoryPath = "test/ivy/repository";
  /**
   * The full svn path to where the Ivy repository is located. This will be created by the tests.
   */
  protected String ivyRepositoryRoot = svnRepositoryRoot + "/" + ivyRepositoryPath;
  protected String svnUserName = TestProperties.getInstance().getProperty(TestProperties.PROPERTY_SVN_USER_NAME);
  protected String svnPassword = TestProperties.getInstance().getProperty(TestProperties.PROPERTY_SVN_PASSWORD);

  /**
   * A temporary folder which tests can use to write data while they run, will be cleaned inbetween each test.
   */
  protected File testTempFolder = new File(TEST_TMP_PATH);

  /**
   * Folder containing test data.
   */
  protected File baseTestDataFolder = new File(TEST_DATA_PATH);

  /**
   * Controls whether the temp folder should be deleted between tests.
   */
  protected boolean cleanupTempFolder = true;

  /**
   * Controls whether the temp folder in the Subversion repository should be deleted between tests.
   */
  protected boolean cleanupSvn = true;

  protected SVNURL ivyRepositoryRootURL = null;
  protected SVNRepository readRepository;

  protected SvnDao svnDAO = null;

  @Before
  public void setUp() throws SVNException {
    FSRepositoryFactory.setup();
    DAVRepositoryFactory.setup();
    SVNRepositoryFactoryImpl.setup();
    testTempFolder.mkdirs();
    creatIvyRepositoryRoot();
    ivyRepositoryRootURL = SVNURL.parseURIEncoded(ivyRepositoryRoot);
    readRepository = SvnUtils.createRepository(ivyRepositoryRootURL, svnUserName, svnPassword, null, null, -1, null,
        null, false);
    svnDAO = new SvnDao(readRepository);
  }

  private void creatIvyRepositoryRoot() throws SVNException {
    SVNURL SVNRepositoryRootURL = SVNURL.parseURIEncoded(svnRepositoryRoot);
    SVNRepository rootReadRepository = SvnUtils.createRepository(SVNRepositoryRootURL, svnUserName, svnPassword, null,
        null, -1, null, null, false);
    SVNRepository rootWriteRepository = SvnUtils.createRepository(SVNRepositoryRootURL, svnUserName, svnPassword, null,
        null, -1, null, null, false);
    SvnDao setupDAO = new SvnDao(rootReadRepository);
    ISVNEditor editor = getCommitEditor(rootWriteRepository);
    setupDAO.createFolders(editor, ivyRepositoryPath, -1);
    SVNCommitInfo info = editor.closeEdit();
    SVNErrorMessage message = info.getErrorMessage();
    assertNull("Expected message to be null but was " + message, message);
  }

  @After
  public void cleanupRepository() throws SVNException {
    if (cleanupSvn) {
      try {
        SVNURL SVNRepositoryRootURL = SVNURL.parseURIEncoded(svnRepositoryRoot);
        SVNRepository rootRepository = SvnUtils.createRepository(SVNRepositoryRootURL, svnUserName, svnPassword, null,
            null, -1, null, null, false);
        ISVNEditor commitEditor = getCommitEditor(rootRepository);
        commitEditor.deleteEntry(TEST_PATH, -1);
        commitEditor.closeEdit();
      } catch (Exception e) {
        e.printStackTrace();
      }
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
   * Utility function to get an editor that can be used for commit operations. This will be initialised to the root of
   * they test Ivy repository.
   * 
   * @return A commit editor.
   * @throws SVNException If an error occurs getting the commit editor.
   */
  protected ISVNEditor getCommitEditor() throws SVNException {
    SVNRepository commitRepository = SvnUtils.createRepository(ivyRepositoryRootURL, svnUserName, svnPassword, null,
        null, -1, null, null, false);
    return getCommitEditor(commitRepository);
  }

  /**
   * Utility function to get an editor that can be used for commit operations. This will be initialised using the passed
   * SVNRepository.
   * 
   * @param An initialised SVN repository against which the commit operations will be performed, relative to its current
   *          path.
   * @return A commit editor.
   * @throws SVNException If an error occurs getting the commit editor.
   */
  protected ISVNEditor getCommitEditor(SVNRepository commitRepository) throws SVNException {
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
