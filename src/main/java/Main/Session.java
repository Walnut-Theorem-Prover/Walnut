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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class stores session-specific data, like Results directories.
 * Every run of Walnut starts a new Session.
 * Integration tests are run in a Session.
 */
public class Session {
  static String name; // user-friendly session name, may be a timestamp
  static String mainWalnutDir = "";

  static String WALNUT_VERSION = "7.0.alpha";
  static String PROMPT = "\n[Walnut]$ ";
  static String FRIENDLY_DATE_TIME_PATTERN = "yyyy_MM_dd_HH_mm"; // TODO; what about localization?

  public static void setPaths() {
    String path = System.getProperty("user.dir");
    if (path.endsWith("bin"))
      mainWalnutDir = "../";
  }

  public static String getName() {
    if (name == null) {
      name = LocalDateTime.now().format(DateTimeFormatter.ofPattern(FRIENDLY_DATE_TIME_PATTERN));
    }
    return name;
  }

  // read from, don't need session-specific code
  public static String getReadAddressForCommandFiles() {
    return mainWalnutDir + "Command Files/";
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
  public static String getReadAddressForMacroLibrary() {
    return mainWalnutDir + "Macro Library/";
  }
  public static String getReadAddressForMorphismLibrary() {
    return mainWalnutDir + "Morphism Library/";
  }
  public static String getReadAddressForAutomataLibrary() {
    return mainWalnutDir + "Automata Library/";
  }
  public static String getTransducerFile(String fileName) {
    return mainWalnutDir + "Transducer Library/" + fileName;
  }
  public static String getReadAddressForWordsLibrary() {
    return mainWalnutDir + "Word Automata Library/";
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
}
