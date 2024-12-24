/*	 Copyright 2025 John Nicol
 *
 * 	 This file is part of Walnut.
 *
 *   Walnut is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Walnut is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Walnut.  If not, see <http://www.gnu.org/licenses/>.
 */

package Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * This class stores session-specific data, like Results directories.
 * Every run of Walnut starts a new Session, although a previous session can be loaded.
 * If a previous session is loaded, its files override the ones in the "global" session.
 * Integration tests are run in a Session, which prevents them from affecting user behavior.
 */
public class Session {
  private static String name; // user-friendly session name, may be a timestamp
  private static String mainWalnutDir = "";
  private static String sessionWalnutDir;

  static final String WALNUT_VERSION = "7.0.alpha";
  static final String PROMPT = "\n[Walnut]$ ";
  private static final String FRIENDLY_DATE_TIME_PATTERN = "yyyy_MM_dd_HH_mm"; // TODO; what about localization?

  private static final String GLOBAL_NAME = "Global";
  private static final String SESSION_NAME = "Session";

  public static void setPathsAndNames() {
    String path = System.getProperty("user.dir");
    if (path.endsWith("bin"))
      mainWalnutDir = "../";
    name = SESSION_NAME + "/" +
        LocalDateTime.now().format(DateTimeFormatter.ofPattern(FRIENDLY_DATE_TIME_PATTERN)) + "/";
    if (sessionWalnutDir == null) {
      sessionWalnutDir = mainWalnutDir + name;
    }
    createSubdirectories();
  }

  public static void setPathsAndNamesIntegrationTests() {
    mainWalnutDir = getAddressForIntegrationTestResults() + GLOBAL_NAME + "/";
    sessionWalnutDir = getAddressForIntegrationTestResults() + SESSION_NAME + "/";
    createSubdirectories();
    // clear out directory if it has anything in it
    try {
      Files.list(Paths.get(getAddressForResult()))
          .filter(Files::isRegularFile) // Select only files
          .forEach(path -> path.toFile().delete()); // Delete each file
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // Clean the paths for integration tests, so that we don't re-use previously generated results.
  public static void cleanPathsAndNamesIntegrationTest() {
    List<String> filesToKeep = List.of("PD.txt", "PR.txt", "P.txt", "RS.txt", "T2.txt");
    for (String s : List.of(
        sessionWalnutDir, getAddressForResult(), getWriteAddressForAutomataLibrary(),
        getWriteAddressForCustomBases(), getWriteAddressForMacroLibrary(), getWriteAddressForMorphismLibrary(),
        getWriteAddressForWordsLibrary())) {
      try {
        Files.list(Paths.get(s))
            .filter(Files::isRegularFile) // Select only files
            .filter(path -> !filesToKeep.contains(path.getFileName().toString())) // Exclude files to keep
            .forEach(path -> path.toFile().delete()); // Delete each file
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Make various subdirectories necessary for writing results.
   */
  private static void createSubdirectories() {
    for (String s : List.of(
        mainWalnutDir + SESSION_NAME,
            sessionWalnutDir, getAddressForResult(), getWriteAddressForAutomataLibrary(),
        getWriteAddressForCustomBases(), getWriteAddressForMacroLibrary(), getWriteAddressForMorphismLibrary(),
        getWriteAddressForWordsLibrary())) {
      File f = new File(s);
      if (!f.exists() && !f.mkdir()) {
        throw new RuntimeException("Couldn't create directory:" + s);
      }
    }
  }

  /**
   * Session name, which is built from the current date-time.
   */
  public static String getName() {
    return name;
  }

  // read from, don't need session-specific code
  public static String getReadAddressForCommandFiles(String filename) {
    return mainWalnutDir + "Command Files/" + filename;
  }
  public static String getAddressForHelpCommands() {
    return mainWalnutDir + "Help Documentation/Commands/";
  }

  public static String getAddressForTestResources() {
    return "src/test/resources/";
  }
  // read/write from, don't need session-specific code
  public static String getAddressForIntegrationTestResults() {
    return getAddressForTestResources() + "integrationTests/";
  }

  // read from, session-specific
  public static String getReadAddressForCustomBases() {
    return mainWalnutDir + "Custom Bases/";
  }
  public static String getReadFileForMacroLibrary(String filename) {
    return globalOrSessionFile("Macro Library/" + filename);
  }
  public static String getReadFileForMorphismLibrary(String fileName) {
    return globalOrSessionFile("Morphism Library/" + fileName);
  }

  public static String getReadFileForAutomataLibrary(String fileName) {
    return globalOrSessionFile("Automata Library/" + fileName);
  }
  public static String getTransducerFile(String fileName) {
    return globalOrSessionFile("Transducer Library/" + fileName);
  }
  public static String getReadFileForWordsLibrary(String fileName) {
    return globalOrSessionFile("Word Automata Library/" + fileName);
  }

  private static String globalOrSessionFile(String testAddress) {
    String sessionFile = sessionWalnutDir + testAddress;
    String globalFile = mainWalnutDir + testAddress;
    if (!(new File(sessionFile).exists())) {
      return globalFile;
    }
    if (new File(globalFile).exists()) {
      String overrideMessage = "Overriding global file with session file:" + sessionFile;
      try {
        // Are they the same?
        boolean areFilesNonEqual = Files.mismatch(Path.of(sessionFile), Path.of(globalFile)) != -1;
        if (areFilesNonEqual) {
          System.out.println(overrideMessage);
        }
      } catch (IOException ex) {
        // unclear why there would be an exception, but this is all informational anyway
        System.out.println(overrideMessage);
      }
    }
    return sessionFile;
  }


  // write to, session-specific
  public static String getWriteAddressForCustomBases() {
    return sessionWalnutDir + "Custom Bases/";
  }
  public static String getWriteAddressForMacroLibrary() {
    return sessionWalnutDir + "Macro Library/";
  }
  public static String getWriteAddressForMorphismLibrary() {
    return sessionWalnutDir + "Morphism Library/";
  }
  public static String getWriteAddressForAutomataLibrary() {
    return sessionWalnutDir + "Automata Library/";
  }
  public static String getAddressForResult() {
    return sessionWalnutDir + "Result/";
  }
  public static String getWriteAddressForWordsLibrary() {
    return sessionWalnutDir + "Word Automata Library/";
  }
}
