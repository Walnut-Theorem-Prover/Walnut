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

import static Main.Prover.homeDirArg;
import static Main.Prover.sessionDirArg;

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
  static boolean globalSession = false;

  static final String WALNUT_VERSION = "7.1.0-beta";
  static final String PROMPT = "\n[Walnut]$ ";
  private static final String FRIENDLY_DATE_TIME_PATTERN = "yyyy_MM_dd_HH_mm"; // TODO; what about localization?

  private static final String GLOBAL_NAME = "Global";
  private static final String SESSION_NAME = "Session";

  private static final String AUTOMATA_LIB = "Automata Library/";
  private static final String WORD_AUTOMATA_LIB = "Word " + AUTOMATA_LIB;
  private static final String CUSTOM_BASES = "Custom Bases/";
  private static final String MACRO_LIBRARY = "Macro Library/";
  private static final String MORPHISM_LIBRARY = "Morphism Library/";
  private static final String TRANSDUCER_LIBRARY = "Transducer Library/";
  private static final String COMMAND_FILES = "Command Files/";
  private static final String RESULT = "Result/";

  public static void setPathsAndNames(String sessionDir, String homeDir, boolean globalSession) {
    if (homeDir == null) {
      String path = System.getProperty("user.dir");
      if (path.endsWith("bin"))
        mainWalnutDir = "../";
    } else {
      mainWalnutDir = homeDir;
    }

    if (globalSession) {
      Session.globalSession = true;
      sessionWalnutDir = mainWalnutDir;
    } else if (sessionDir == null) {
      name = SESSION_NAME + "/" +
          LocalDateTime.now().format(DateTimeFormatter.ofPattern(FRIENDLY_DATE_TIME_PATTERN)) + "/";
      if (sessionWalnutDir == null) {
        sessionWalnutDir = mainWalnutDir + name;
      }
    } else {
      sessionWalnutDir = name = sessionDir;
    }
    createSubdirectories();
  }

  public static void setPathsAndNamesIntegrationTests() {
    String[] args = new String[]{homeDirArg + getAddressForIntegrationTestResults() + GLOBAL_NAME,
    sessionDirArg + getAddressForIntegrationTestResults() + SESSION_NAME};
    Prover.parseArgs(args);
    // clear out directory if it has anything in it
    try {
      Files.list(Paths.get(getAddressForResult()))
          .filter(Files::isRegularFile) // Select only files
          .forEach(path -> path.toFile().delete()); // Delete each file
    } catch (IOException ex) {
      Logging.printTruncatedStackTrace(ex);
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
        Logging.printTruncatedStackTrace(ex);
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
      if (s.isEmpty()) { continue; }
      File f = new File(s);
      if (!f.isDirectory() && !f.mkdir()) {
        throw new WalnutException("Couldn't create directory:" + s);
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
    return mainWalnutDir + COMMAND_FILES + filename;
  }
  public static String getAddressForHelpCommands() {
    return mainWalnutDir + "Help Documentation/Commands/";
  }

  public static String getAddressForTestResources() {
    return "src/test/resources/";
  }
  public static String getAddressForUnitTestResources() {
    return getAddressForTestResources() + "unitTests/";
  }
  // read/write from, don't need session-specific code
  public static String getAddressForIntegrationTestResults() {
    return getAddressForTestResources() + "integrationTests/";
  }

  // read from, session-specific
  public static String getReadAddressForCustomBases(String filename) {
    return globalOrSessionFile(CUSTOM_BASES + filename);
  }
  public static String getReadFileForMacroLibrary(String filename) {
    return globalOrSessionFile(MACRO_LIBRARY + filename);
  }
  public static String getReadFileForMorphismLibrary(String fileName) {
    return globalOrSessionFile(MORPHISM_LIBRARY + fileName);
  }

  public static String getReadFileForAutomataLibrary(String fileName) {
    return globalOrSessionFile(AUTOMATA_LIB + fileName);
  }
  public static String getTransducerFile(String fileName) {
    return globalOrSessionFile(TRANSDUCER_LIBRARY + fileName);
  }
  public static String getReadFileForWordsLibrary(String fileName) {
    return globalOrSessionFile(WORD_AUTOMATA_LIB + fileName);
  }

  private static String globalOrSessionFile(String testAddress) {
    String globalFile = mainWalnutDir + testAddress;
    if (globalSession) {
      return globalFile;
    }

    String sessionFile = sessionWalnutDir + testAddress;
    if (!(new File(sessionFile).isFile())) {
      return globalFile;
    }
    if (new File(globalFile).isFile()) {
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
    return sessionWalnutDir + CUSTOM_BASES;
  }
  public static String getWriteAddressForMacroLibrary() {
    return sessionWalnutDir + MACRO_LIBRARY;
  }
  public static String getWriteAddressForMorphismLibrary() {
    return sessionWalnutDir + MORPHISM_LIBRARY;
  }
  public static String getWriteAddressForAutomataLibrary() {
    return sessionWalnutDir + AUTOMATA_LIB;
  }
  public static String getAddressForResult() {
    return sessionWalnutDir + RESULT;
  }
  public static String getWriteAddressForWordsLibrary() {
    return sessionWalnutDir + WORD_AUTOMATA_LIB;
  }
}
