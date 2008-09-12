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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.tmatesoft.svn.core.SVNException;

/**
 * Operation that represents adding/updating a file in svn.
 */
public class PutOperation {

  /**
   * The file to put.
   */
  private File file;

  /**
   * Whether any existing file data in svn should be overwritten or not.
   */
  private boolean overwrite;

  /**
   * The contents of the file.
   */
  byte[] data;

  /**
   * The folder path of the file.
   */
  private String folderPath;

  /**
   * The path to the file relative to the repository root.
   */
  private String destination;

  /**
   * The name of the file (without any folder path).
   */
  private String fileName;

  /**
   * Constructs a new PutOperation.
   * 
   * @param file The file to be added/updated in svn.
   * @param destination The full svn destination path of the file.
   * @param overwrite Whether any existing file data should be overwritten or not.
   * @throws IOException If the file data cannot be read from disk or the file paths cannot be determined.
   */
  public PutOperation(File file, String destination, boolean overwrite) throws IOException {
    this.file = file;
    if (file.getName().startsWith("ivytemp") || file.getAbsolutePath().startsWith(System.getProperty("java.io.tmpdir"))) {
      // most likely a checksum generated by ivy, we need to store file in memory as ivy deletes this
      // inbetween calls to put so it will be gone when we try publish transaction
      loadFileData();
    }
    this.destination = destination;
    this.overwrite = overwrite;
    determinePaths();
  }

  /**
   * Determine the various file-related paths that are needed to put this file into svn.
   * 
   * @throws SVNException If an error occurs parsing the destination path.
   */
  private void determinePaths() {
    // make sure the destination is a file and not a folder
    int fileNameIndex = destination.lastIndexOf("/");
    if (fileNameIndex == destination.length() - 1) {
      throw new IllegalArgumentException("Can only publish files (not folders), check your publish pattern");
    }
    this.folderPath = destination.substring(0, fileNameIndex);
    this.fileName = destination.substring(fileNameIndex + 1);
  }

  /**
   * Loads the file data from disk into an in-memory byte array.
   * 
   * @throws IOException If an error occurs reading the file.
   */
  private void loadFileData() throws IOException {
    BufferedInputStream fileInputStream = null;
    try {
      if (file != null && file.isFile()) {
        data = new byte[(int) file.length()];
        fileInputStream = new BufferedInputStream(new FileInputStream(file));
        fileInputStream.read(data); // convert from file to byte array
      } else {
        throw new IOException("No file data found.");
      }
    } finally {
      if (fileInputStream != null) {
        fileInputStream.close();
      }
    }
  }

  /**
   * For the passed values, determine the full binary diff folder path that should be used for the file represented by
   * this operation.
   * 
   * @param revision The revision.
   * @param binaryDiffFolderName The name of the binary diff folder.
   * @return The binary diff folder path for this put operation's file.
   * @throws IllegalStateException If this operations fodler path does not contain the revision ONCE ONLY.
   */
  public String determineBinaryDiffFolderPath(String revision, String binaryDiffFolderName) {
    if (!this.folderPath.contains(revision)) {
      throw new IllegalStateException("Ivy destination folder '" + folderPath + "' does not contain revision '"
          + revision + "'");
    }
    String binaryDiffFolderPath = folderPath.replaceFirst(revision, binaryDiffFolderName);
    if (binaryDiffFolderPath.contains(revision)) {
      throw new IllegalStateException("Ivy destination folder '" + folderPath + "' contains revision '" + revision
          + "' more than once");
    }
    return binaryDiffFolderPath;
  }

  /**
   * Gets the full path to the file.
   * 
   * @return The file path.
   */
  public String getFilePath() {
    return folderPath + "/" + fileName;
  }

  /**
   * @return the folderPath
   */
  public String getFolderPath() {
    return folderPath;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @return the overwrite
   */
  public boolean isOverwrite() {
    return overwrite;
  }

  /**
   * Returns this operations file data as a byte[].
   * 
   * @return The file data as a byte array.
   * @throws IOException If an error occurs reading the file data.
   */
  public byte[] getData() throws IOException {
    if (data == null) {
      loadFileData();
    }
    return data;
  }

}