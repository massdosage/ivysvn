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
 *
 */
package fm.last.ivy.plugins.svnresolver;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.plugins.repository.Resource;

/**
 * A Resource implementation for Subversion.
 */
public class SvnResource implements Resource {

  private String source;
  private boolean exists = false;
  private long lastModified = 0;
  private long contentLength = 0;
  private SvnRepository repository = null;
  private boolean resolved = false;

  /**
   * Constructs a new Subversion resource.
   * 
   * @param repository The repository that was used to resolve this resource.
   * @param source Subversion string identifying the resource
   * @param exists Whether the resource exists or not.
   * @param lastModified When the resource was last modified (committed).
   * @param contentLength The size of the resource in bytes.
   */
  public SvnResource(SvnRepository repository, String source, boolean exists, long lastModified, long contentLength) {
    this.source = source;
    this.exists = exists;
    this.lastModified = lastModified;
    this.contentLength = contentLength;
    this.resolved = true;
  }

  /**
   * Constructs a new Subversion resource.
   * 
   * @param repository The repository that was used to resolve this resource.
   * @param source Subversion string identifying the resource
   */
  public SvnResource(SvnRepository repository, String source) {
    this.repository = repository;
    this.source = source;
    this.resolved = false;
  }

  /**
   * Constructs a new Subversion resource.
   */
  public SvnResource() {
    this.resolved = true;
  }

  /**
   * Resolves this resource via its repository.
   */
  private void resolve() {
    SvnResource resolved = repository.resolveResource(source);
    this.contentLength = resolved.getContentLength();
    this.lastModified = resolved.getLastModified();
    this.exists = resolved.exists();
    this.resolved = true;
  }

  /**
   * Clones this resource.
   * 
   * @param cloneName
   * @return A clone of this resource.
   */
  public Resource clone(String cloneName) {
    return new SvnResource(repository, cloneName);
  }

  /**
   * Checks whether this resource is available.
   * 
   * @return Whether the resource is available or not.
   */
  public boolean exists() {
    if (!resolved) {
      resolve();
    }
    return this.exists;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.ivy.repository.Resource#getContentLength()
   */
  public long getContentLength() {
    if (!resolved) {
      resolve();
    }
    return this.contentLength;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.ivy.repository.Resource#getLastModified()
   */
  public long getLastModified() {
    if (!resolved) {
      resolve();
    }
    return this.lastModified;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.ivy.repository.Resource#getName()
   */
  public String getName() {
    return source;
  }

  /**
   * Returns whether this resource is local or not.
   * 
   * @return Currently always returns false.
   * @see org.apache.ivy.repository.Resource#isLocal()
   */
  public boolean isLocal() {
    // svn resources are not on the file system so return false
    return false;
  }

  /**
   * Gets an input stream for this resource.
   * 
   * @return
   * @throw UnsupportedOperationException This is currently always thrown.
   * @see org.apache.ivy.repository.Resource#openStream()
   */
  public InputStream openStream() throws IOException {
    throw new UnsupportedOperationException("Opening an input stream on a SVN resource not currently supported");
  }

  /**
   * Generates a String representation of this object.
   * 
   * @return A String reprentation of this object.
   */
  public String toString() {
    return source;
  }

}
