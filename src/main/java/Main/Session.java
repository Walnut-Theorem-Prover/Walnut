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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

  public static void setPathsAndNames() {
    String path = System.getProperty("user.dir");
    if (path.endsWith("bin"))
      mainWalnutDir = "../";
    name = LocalDateTime.now().format(DateTimeFormatter.ofPattern(FRIENDLY_DATE_TIME_PATTERN));
    if (sessionWalnutDir == null) {
      setSessionWalnutDir(mainWalnutDir + name + "/");
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

  // read/write from, don't need session-specific code
  public static String getAddressForIntegrationTestResults() {
    return mainWalnutDir + "Test Results/Integration Tests/";
  }

  // read from, need session-specific code
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
      System.out.println("Overriding global file with session file:" + sessionFile);
    }
    return sessionFile;
  }


  // write to
  public static String getWriteAddressForCustomBases() {
    return mainWalnutDir + "Custom Bases/";
  }
  public static String getWriteAddressForMacroLibrary() {
    return mainWalnutDir + "Macro Library/";
  }
  public static String getWriteAddressForMorphismLibrary() {
    return mainWalnutDir + "Morphism Library/";
  }
  public static String getWriteAddressForAutomataLibrary() {
    return mainWalnutDir + "Automata Library/";
  }
  public static String getAddressForResult() {
    return mainWalnutDir + "Result/";
  }
  public static String getWriteAddressForWordsLibrary() {
    return mainWalnutDir + "Word Automata Library/";
  }

  public static void setSessionWalnutDir(String sessionWalnutDir) {
    Session.sessionWalnutDir = sessionWalnutDir;
  }
}
