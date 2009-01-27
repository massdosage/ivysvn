/*
 * Copyright 2008 Last.fm (Contributed by Steve Brown, Estafet Ltd.)
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

import org.apache.ivy.util.Message;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;

/**
 * Class to handle proxy settings.
 * <p/>
 * Note that <code>java.net.useSystemProxies</code> is not used because it only works on Windows and Linux.
 * </p>
 * This class makes the assumption the the <tt>http</tt> and <tt>https</tt> protocols use the same proxy server.
 * 
 * @author Steve Brown, Estafet Ltd.
 */
public class ProxySettings {
  
  /**
   * Property to enable using System proxy settings on Windows.
   */
  private static final String JAVA_NET_USE_SYSTEM_PROXIES = "java.net.useSystemProxies";

  /**
   * The default proxy port number.
   */
  private static final int DEFAULT_PROXY_PORT_NUMBER = 3128;

  /**
   * The largest valid port number.
   */
  private static final int MAXIMUM_PORT_NUMBER = 65535;

  /**
   * The smallest valid port number.
   */
  private static final int MINIMUM_PORT_NUMBER = 0;

  /**
   * The java property name for the proxy password.
   */
  private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";

  /**
   * The java property name for the proxy username.
   */
  private static final String HTTP_PROXY_USER = "http.proxyUser";

  /**
   * The java property name for the proxy port number.
   */
  private static final String HTTP_PROXY_PORT = "http.proxyPort";

  /**
   * The java property name for the proxy host.
   */
  private static final String HTTP_PROXY_HOST = "http.proxyHost";

  /**
   * The proxy host.
   */
  private final String proxyHost;

  /**
   * The proxy port number.
   */
  private final int proxyPort;

  /**
   * The proxy username
   */
  private final String proxyUser;

  /**
   * The proxy password
   */
  private final String proxyPassword;

  /**
   * Constructor
   */
  public ProxySettings() {
    // Don't use system proxy server settings.
    System.setProperty(JAVA_NET_USE_SYSTEM_PROXIES, Boolean.toString(false));

    proxyHost = getProperty(HTTP_PROXY_HOST);
    if (proxyHost == null) {
      proxyPort = 0;
      proxyUser = null;
      proxyPassword = null;
    } else {
      try {
        final int portNumber = getProxyPortNumber();
        if (portNumber < MINIMUM_PORT_NUMBER || portNumber > MAXIMUM_PORT_NUMBER) {
          throw new ExceptionInInitializerError(String.format(
              "Invalid port number %s. Port numbers must be in the range %s to %s", Integer.toString(portNumber),
              Integer.toString(MINIMUM_PORT_NUMBER), Integer.toString(MAXIMUM_PORT_NUMBER)));
        }
        proxyPort = portNumber;
      } catch (final NumberFormatException e) {
        final ExceptionInInitializerError error = new ExceptionInInitializerError(String.format(
            "\"%s\" is not a valid value for %s", System.getProperty(HTTP_PROXY_PORT), HTTP_PROXY_PORT));
        error.initCause(e);
        throw error;
      }
      proxyUser = getProperty(HTTP_PROXY_USER);
      proxyPassword = System.getProperty(HTTP_PROXY_PASSWORD);
    }
  }

  /**
   * Set the proxy for a {@link BasicAuthenticationManager}.
   * 
   * @param authManager The authentication manager to set the proxy server on.
   */
  public void setProxy(final BasicAuthenticationManager authManager) {
    if (proxyHost != null) {
      Message.debug(String.format("The proxy server is %s:%s. The proxy username is %s", proxyHost, Integer
          .toString(proxyPort), proxyUser));
      authManager.setProxy(proxyHost, proxyPort, proxyUser, proxyPassword);
    }
  }

  /**
   * Get the proxy port number.
   * 
   * @return The proxy port number.
   */
  private int getProxyPortNumber() {
    final String valueString = getProperty(HTTP_PROXY_PORT);
    final int portNumber = valueString != null ? Integer.parseInt(valueString) : DEFAULT_PROXY_PORT_NUMBER;
    return portNumber;
  }

  /**
   * Get a property value. Note - the property value will be trimmed.
   * 
   * @param propertyName The name of the property.
   * @return The property value, or <tt>null</tt> if the property is not defined or is empty.
   */
  private String getProperty(final String propertyName) {
    String valueString = System.getProperty(propertyName);
    if (valueString != null) {
      valueString = valueString.trim();
      if (valueString.length() == 0) {
        valueString = null;
      }
    }
    return valueString;
  }

}
