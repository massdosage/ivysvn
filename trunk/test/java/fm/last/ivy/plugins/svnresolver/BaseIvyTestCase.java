package fm.last.ivy.plugins.svnresolver;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.ant.IvyResolve;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.tmatesoft.svn.core.SVNException;

import fm.last.ivy.plugins.svnresolver.test.TestProperties;

/**
 * Base class for test cases which emulate running Ivy as an Ant task.
 */
public abstract class BaseIvyTestCase extends BaseTestCase {

  // the base path for published artifacts
  protected static final String BASE_PUBLISH_PATH = TEST_PATH + "/java/repository";

  // set cache location under "test/tmp" so it will get automatically cleaned between tests
  private File cache = new File(TEST_TMP_PATH + "/cache");

  // property values for ant log level
  private static final String MSG_ERR = "MSG_ERR";
  private static final String MSG_WARN = "MSG_WARN";
  private static final String MSG_INFO = "MSG_INFO";
  private static final String MSG_VERBOSE = "MSG_VERBOSE";
  private static final String MSG_DEBUG = "MSG_DEBUG";

  @Before
  public void setUp() throws SVNException {
    super.setUp();
    cache.mkdirs();
    System.setProperty("ivy.cache.dir", cache.getAbsolutePath());
  }

  /**
   * Gets the value set in test.properties for Ant's message output level (defaults to MSG_DEBUG).
   * 
   * @return The value for Ant's message output level.
   */
  private int getAntMessageOutputLevel() {
    String logLevelString = TestProperties.getInstance().getProperty(TestProperties.PROPERTY_ANT_MESSAGE_OUTPUT_LEVEL,
        MSG_DEBUG).trim();
    int logLevel = Project.MSG_DEBUG;
    if (MSG_DEBUG.equals(logLevelString)) {
      logLevel = Project.MSG_DEBUG;
    } else if (MSG_VERBOSE.equals(logLevelString)) {
      logLevel = Project.MSG_VERBOSE;
    } else if (MSG_INFO.equals(logLevelString)) {
      logLevel = Project.MSG_INFO;
    } else if (MSG_WARN.equals(logLevelString)) {
      logLevel = Project.MSG_WARN;
    } else if (MSG_ERR.equals(logLevelString)) {
      logLevel = Project.MSG_ERR;
    } else {
      System.err.println("Undefined value for " + TestProperties.PROPERTY_ANT_MESSAGE_OUTPUT_LEVEL + " '"
          + logLevelString + "' " + MSG_DEBUG + " will be used.");
    }
    return logLevel;
  }

  /**
   * Creates a new Ant Project with logging set up.
   * 
   * @return A new Ant Project.
   */
  protected Project createProject() {
    Project project = new Project();
    // redirect ant output to System streams
    DefaultLogger consoleLogger = new DefaultLogger();
    consoleLogger.setErrorPrintStream(System.err);
    consoleLogger.setOutputPrintStream(System.out);
    consoleLogger.setMessageOutputLevel(getAntMessageOutputLevel());
    project.addBuildListener(consoleLogger);
    return project;
  }

  /**
   * Replaces all occurrences of an Ant property in the passed string.
   * 
   * @param sourceString The source string.
   * @param propertyName The name of the property.
   * @param replacementValue The replacement value for the property.
   * @return The sourceString, but with any Ant properties replaced by new values.
   */
  private String replaceAntProperty(String sourceString, String propertyName, String replacementValue) {
    return sourceString.replace("${" + propertyName + "}", replacementValue);
  }

  /**
   * Creates an Ivy settings file, replacing various values in the passed Ivy settings template file with values valid
   * for testing.
   * 
   * @param ivySettingsTemplate Ivy settings file containing Ant property placeholders for various values.
   * @return A File that can be used for the duration of a test method for Ivy settings.
   * @throws IOException If an error occurs reading the Ivy settings file or writing a new one to disk.
   */
  protected File prepareTestIvySettings(File ivySettingsTemplate) throws IOException {
    return prepareTestIvySettings(ivySettingsTemplate, null);
  }

  /**
   * Creates an Ivy settings file, replacing various values in the passed Ivy settings template file with values valid
   * for testing.
   * 
   * @param ivySettingsTemplate Ivy settings file containing Ant property placeholders for various values.
   * @param extraSvnAttributes XML string which can be appended to the standard "svn" element.
   * @return A File that can be used for the duration of a test method for Ivy settings.
   * @throws IOException If an error occurs reading the Ivy settings file or writing a new one to disk.
   */
  protected File prepareTestIvySettings(File ivySettingsTemplate, String extraSvnAttributes) throws IOException {
    String ivySettings = FileUtils.readFileToString(ivySettingsTemplate);
    ivySettings = replaceAntProperty(ivySettings, TestProperties.PROPERTY_SVN_REPOSITORY_ROOT, repositoryRoot);
    ivySettings = replaceAntProperty(ivySettings, TestProperties.PROPERTY_SVN_USER_NAME, svnUserName);
    ivySettings = replaceAntProperty(ivySettings, TestProperties.PROPERTY_SVN_PASSWORD, svnPassword);
    ivySettings = replaceAntProperty(ivySettings, TestProperties.PROPERTY_SVN_BINARY_DIFF, "false");
    if (extraSvnAttributes != null) {
      ivySettings = ivySettings.replace("><!--@-->", " " + extraSvnAttributes + ">");
    }
    File tempIvySettingsFile = new File(testTempFolder, "ivysettings-test.xml");
    FileUtils.writeStringToFile(tempIvySettingsFile, ivySettings);
    return tempIvySettingsFile;
  }

  /**
   * Performs an Ivy resolve operation for the passed Ant Project.
   * 
   * @param project Ant project.
   * @param ivyFile Ivy File to use for resolving.
   */
  protected void resolve(Project project, File ivyFile) {
    IvyResolve resolve = new IvyResolve();
    resolve.setProject(project);
    resolve.setTaskName("resolve");
    resolve.setFile(ivyFile);
    resolve.execute();
  }

}
