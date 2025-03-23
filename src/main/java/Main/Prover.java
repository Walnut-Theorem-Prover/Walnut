/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.*;
import Automata.Numeration.Ostrowski;
import Main.EvalComputations.Token.ArithmeticOperator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 */
public class Prover {
  static final String RE_FOR_THE_LIST_OF_CMDS = "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|draw|export|help)";
  static final String RE_START = "^";
  static final String RE_WORD_OF_CMD_NO_SPC = "([a-zA-Z]\\w*)";

  static final String RE_WORD_OF_CMD = "\\s+" + RE_WORD_OF_CMD_NO_SPC;

  /**
   * the high-level scheme of a command is a name followed by some arguments and ending in either ; : or ::
   */
  static final String RE_FOR_CMD = RE_START + "(\\w+)(\\s+.*)?";
  static final Pattern PAT_FOR_CMD = Pattern.compile(RE_FOR_CMD);
  
  static final String RE_FOR_load_CMD = RE_START + "load\\s+(\\w+\\.txt)";
  public static final String FIXTRAILZERO = "fixtrailzero";
  public static final String CONVERT = "convert";
  public static final String REVERSE_SPLIT = "reverse split";
  public static final String REG = "reg";
  public static final String LOAD = "load";
  public static final String ALPHABET = "alphabet";
  public static final String DRAW = "draw";
  public static final String HELP = "help";
  public static final String CLEAR = "clear";
  public static final String CLS = "cls";
  public static final String DEF = "def";
  public static final String EVAL = "eval";
  public static final String EXIT = "exit";
  public static final String QUIT = "quit";
  public static final String LEFT_BRACKET = "[";
  public static final String TXT_EXTENSION = ".txt";
  public static final String GV_EXTENSION = ".gv";
  public static final String BA_EXTENSION = ".ba";
  /**
   * group for filename in RE_FOR_load_CMD
   */
  static int L_FILENAME = 1;
  static final Pattern PAT_FOR_load_CMD = Pattern.compile(RE_FOR_load_CMD);

  static final String RE_FOR_eval_def_CMDS = RE_START + "(eval|def)" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)\\s+\"(.*)\"";
  /**
   * important groups in RE_FOR_eval_def_CMDS
   */
  static int ED_NAME = 2, ED_FREE_VARIABLES = 3, ED_PREDICATE = 6;
  static final Pattern PAT_FOR_eval_def_CMDS = Pattern.compile(RE_FOR_eval_def_CMDS);
  static final String REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = "[a-zA-Z]\\w*";
  static final Pattern PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = Pattern.compile(REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS);

  public static final String MACRO = "macro";
  static final String RE_FOR_macro_CMD = RE_START + MACRO + RE_WORD_OF_CMD + "\\s+\"(.*)\"";
  static int M_NAME = 1, M_DEFINITION = 2;
  static final Pattern PAT_FOR_macro_CMD = Pattern.compile(RE_FOR_macro_CMD);

  static final String RE_FOR_reg_CMD = RE_START + "(reg)" + RE_WORD_OF_CMD + "\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)\"(.*)\"";

  /**
   * important groups in RE_FOR_reg_CMD
   */
  static final int R_NAME = 2, R_LIST_OF_ALPHABETS = 3, R_REGEXP = 20;
  static final Pattern PAT_FOR_reg_CMD = Pattern.compile(RE_FOR_reg_CMD);
  static final String RE_FOR_A_SINGLE_ELEMENT_OF_A_SET = "(\\+|\\-)?\\s*\\d+";
  static final Pattern PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET = Pattern.compile(RE_FOR_A_SINGLE_ELEMENT_OF_A_SET);
  static final String RE_FOR_AN_ALPHABET = "((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+";
  static final Pattern PAT_FOR_AN_ALPHABET = Pattern.compile(RE_FOR_AN_ALPHABET);
  static final int R_NUMBER_SYSTEM = 2, R_SET = 11;

  static final String RE_FOR_AN_ALPHABET_VECTOR = "(\\[(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\])|(\\d)";
  static final Pattern PAT_FOR_AN_ALPHABET_VECTOR = Pattern.compile(RE_FOR_AN_ALPHABET_VECTOR);


  public static final String OST = "ost";
  static final String RE_FOR_ost_CMD = RE_START + OST + RE_WORD_OF_CMD + "\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*\\[\\s*((\\d+\\s*)*)\\]$";
  static final Pattern PAT_FOR_ost_CMD = Pattern.compile(RE_FOR_ost_CMD);
  static final int GROUP_OST_NAME = 1, GROUP_OST_PREPERIOD = 2, GROUP_OST_PERIOD = 4;

  public static final String COMBINE = "combine";
  static final String RE_FOR_combine_CMD = RE_START + COMBINE + RE_WORD_OF_CMD + "((\\s+([a-zA-Z]\\w*(=-?\\d+)?))*)";
  static final Pattern PAT_FOR_combine_CMD = Pattern.compile(RE_FOR_combine_CMD);
  static final int GROUP_COMBINE_NAME = 1, GROUP_COMBINE_AUTOMATA = 2;
  static final String RE_FOR_AN_AUTOMATON_IN_combine_CMD = RE_WORD_OF_CMD_NO_SPC + "((=-?\\d+)?)";
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_combine_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_combine_CMD);

  public static final String MORPHISM = "morphism";
  static final String RE_FOR_morphism_CMD = RE_START + MORPHISM + RE_WORD_OF_CMD + "\\s+\"(\\d+\\s*\\-\\>\\s*(.)*(,\\d+\\s*\\-\\>\\s*(.)*)*)\"";
  static final Pattern PAT_FOR_morphism_CMD = Pattern.compile(RE_FOR_morphism_CMD);
  static int GROUP_MORPHISM_NAME = 1, GROUP_MORPHISM_DEFINITION;

  public static final String PROMOTE = "promote";
  static final String RE_FOR_promote_CMD = RE_START + PROMOTE + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_promote_CMD = Pattern.compile(RE_FOR_promote_CMD);
  static int GROUP_PROMOTE_NAME = 1, GROUP_PROMOTE_MORPHISM = 2;

  public static final String IMAGE = "image";
  static final String RE_FOR_image_CMD = RE_START + IMAGE + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_image_CMD = Pattern.compile(RE_FOR_image_CMD);
  static final int GROUP_IMAGE_NEW_NAME = 1, GROUP_IMAGE_MORPHISM = 2, GROUP_IMAGE_OLD_NAME = 3;

  public static final String INF = "inf";
  static final String RE_FOR_inf_CMD = RE_START + INF + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_inf_CMD = Pattern.compile(RE_FOR_inf_CMD);
  static final int GROUP_INF_NAME = 1;

  public static final String SPLIT = "split";
  static final String RE_FOR_split_CMD = RE_START + SPLIT + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "((\\s*\\[\\s*[+-]?\\s*])+)";
  static final Pattern PAT_FOR_split_CMD = Pattern.compile(RE_FOR_split_CMD);
  static final int GROUP_SPLIT_NAME = 1, GROUP_SPLIT_AUTOMATA = 2, GROUP_SPLIT_INPUT = 3;
  static final String RE_FOR_INPUT_IN_split_CMD = "\\[\\s*([+-]?)\\s*]";
  static final Pattern PAT_FOR_INPUT_IN_split_CMD = Pattern.compile(RE_FOR_INPUT_IN_split_CMD);

  public static final String RSPLIT = "rsplit";
  static final String RE_FOR_rsplit_CMD = RE_START + RSPLIT + RE_WORD_OF_CMD + "((\\s*\\[\\s*[+-]?\\s*])+)" + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_rsplit_CMD = Pattern.compile(RE_FOR_rsplit_CMD);
  static final int GROUP_RSPLIT_NAME = 1, GROUP_RSPLIT_AUTOMATA = 4, GROUP_RSPLIT_INPUT = 2;

  public static final String JOIN = "join";
  static final String RE_FOR_join_CMD = RE_START + JOIN + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + "((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+))*)";
  static final Pattern PAT_FOR_join_CMD = Pattern.compile(RE_FOR_join_CMD);
  static final int GROUP_JOIN_NAME = 1, GROUP_JOIN_AUTOMATA = 2;
  static final String RE_FOR_AN_AUTOMATON_IN_join_CMD = RE_WORD_OF_CMD_NO_SPC + "((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+)";
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_join_CMD);
  static final int GROUP_JOIN_AUTOMATON_NAME = 1, GROUP_JOIN_AUTOMATON_INPUT = 2;
  static final String RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = "\\[\\s*([a-zA-Z&&[^AE]]\\w*)\\s*]";
  static final Pattern PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD);

  public static final String TEST = "test";
  static final String RE_FOR_test_CMD = RE_START + TEST + RE_WORD_OF_CMD + "\\s*(\\d+)";
  static final Pattern PAT_FOR_test_CMD = Pattern.compile(RE_FOR_test_CMD);
  static final int GROUP_TEST_NAME = 1, GROUP_TEST_NUM = 2;

  public static final String TRANSDUCE = "transduce";
  public static final String DOLLAR = "\\s+(\\$|\\s*)";
  static final String RE_FOR_transduce_CMD = RE_START + TRANSDUCE + RE_WORD_OF_CMD + RE_WORD_OF_CMD + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_transduce_CMD = Pattern.compile(RE_FOR_transduce_CMD);
  static final int GROUP_TRANSDUCE_NEW_NAME = 1, GROUP_TRANSDUCE_TRANSDUCER = 2,
      GROUP_TRANSDUCE_DOLLAR_SIGN = 3, GROUP_TRANSDUCE_OLD_NAME = 4;

  public static final String REVERSE = "reverse";
  static final String RE_FOR_reverse_CMD = RE_START + REVERSE + RE_WORD_OF_CMD + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_reverse_CMD = Pattern.compile(RE_FOR_reverse_CMD);
  static final int GROUP_REVERSE_NEW_NAME = 1, GROUP_REVERSE_DOLLAR_SIGN = 2, GROUP_REVERSE_OLD_NAME = 3;

  public static final String MINIMIZE = "minimize";
  static final String RE_FOR_minimize_CMD = RE_START + MINIMIZE + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_minimize_CMD = Pattern.compile(RE_FOR_minimize_CMD);
  static final int GROUP_MINIMIZE_NEW_NAME = 1, GROUP_MINIMIZE_OLD_NAME = 2;

  static final String RE_FOR_convert_CMD = RE_START + "convert" + DOLLAR + RE_WORD_OF_CMD_NO_SPC + "\\s+((msd|lsd)_(\\d+))" + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_convert_CMD = Pattern.compile(RE_FOR_convert_CMD);
  static final int GROUP_CONVERT_NEW_NAME = 2, GROUP_CONVERT_OLD_NAME = 7,
      GROUP_CONVERT_NEW_DOLLAR_SIGN = 1, GROUP_CONVERT_OLD_DOLLAR_SIGN = 6,
      GROUP_CONVERT_MSD_OR_LSD = 4,
      GROUP_CONVERT_BASE = 5;

  public static final String FIXLEADZERO = "fixleadzero";
  static final String RE_FOR_fixleadzero_CMD = RE_START + FIXLEADZERO + RE_WORD_OF_CMD + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_fixleadzero_CMD = Pattern.compile(RE_FOR_fixleadzero_CMD);
  static final int GROUP_FIXLEADZERO_NEW_NAME = 1, GROUP_FIXLEADZERO_OLD_NAME = 3;

  static final String RE_FOR_fixtrailzero_CMD = RE_START + FIXTRAILZERO + RE_WORD_OF_CMD + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_fixtrailzero_CMD = Pattern.compile(RE_FOR_fixtrailzero_CMD);
  static final int GROUP_FIXTRAILZERO_NEW_NAME = 1, GROUP_FIXTRAILZERO_OLD_NAME = 3;

  static final String RE_FOR_alphabet_CMD = RE_START + "(" + ALPHABET + ")" + RE_WORD_OF_CMD +"\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_alphabet_CMD = Pattern.compile(RE_FOR_alphabet_CMD);
  static final int GROUP_alphabet_NEW_NAME = 2, GROUP_alphabet_LIST_OF_ALPHABETS = 3, GROUP_alphabet_DOLLAR_SIGN = 20, GROUP_alphabet_OLD_NAME = 21;

  public static final String UNION = "union";
  static final String RE_FOR_union_CMD = RE_START + UNION + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_union_CMD = Pattern.compile(RE_FOR_union_CMD);
  static final int GROUP_UNION_NAME = 1, GROUP_UNION_AUTOMATA = 2;
  static final String RE_FOR_AN_AUTOMATON_IN_union_CMD = RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_union_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_union_CMD);

  public static final String INTERSECT = "intersect";
  static final String RE_FOR_intersect_CMD = RE_START + INTERSECT + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_intersect_CMD = Pattern.compile(RE_FOR_intersect_CMD);
  static final int GROUP_INTERSECT_NAME = 1, GROUP_INTERSECT_AUTOMATA = 2;
  static final String RE_FOR_AN_AUTOMATON_IN_intersect_CMD = RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_intersect_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_intersect_CMD);

  public static final String STAR = "star";
  static final String RE_FOR_star_CMD = RE_START + STAR + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_star_CMD = Pattern.compile(RE_FOR_star_CMD);
  static final int GROUP_STAR_NEW_NAME = 1, GROUP_STAR_OLD_NAME = 2;

  public static final String CONCAT = "concat";
  static final String RE_FOR_concat_CMD = RE_START + CONCAT + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_concat_CMD = Pattern.compile(RE_FOR_concat_CMD);
  static final int GROUP_CONCAT_NAME = 1, GROUP_CONCAT_AUTOMATA = 2;
  static final String RE_FOR_AN_AUTOMATON_IN_concat_CMD = RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_concat_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_concat_CMD);

  public static final String RIGHTQUO = "rightquo";
  static final String RE_FOR_rightquo_CMD = RE_START + RIGHTQUO + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_rightquo_CMD = Pattern.compile(RE_FOR_rightquo_CMD);
  static final int GROUP_rightquo_NEW_NAME = 1, GROUP_rightquo_OLD_NAME1 = 2, GROUP_rightquo_OLD_NAME2 = 3;

  public static final String LEFTQUO = "leftquo";
  static final String RE_FOR_leftquo_CMD = RE_START + LEFTQUO + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_leftquo_CMD = Pattern.compile(RE_FOR_leftquo_CMD);
  static final int GROUP_leftquo_NEW_NAME = 1, GROUP_leftquo_OLD_NAME1 = 2, GROUP_leftquo_OLD_NAME2 = 3;

  static final String RE_FOR_draw_CMD = RE_START + DRAW + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_draw_CMD = Pattern.compile(RE_FOR_draw_CMD);
  static final int GROUP_draw_DOLLAR_SIGN = 1, GROUP_draw_NAME = 2;

  // Meta-commands: [...] at the beginning of the command
  static final Pattern PAT_META_CMD = Pattern.compile("^\\[([^]]*)](.*)$");
  static final int GROUP_META_CMD = 1, GROUP_FINAL_CMD = 2;

  static final String STRATEGY = "strategy";
  static final String EXPORT = "export";

  static final String RE_FOR_export_CMD = RE_START + "export\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_export_CMD = Pattern.compile(RE_FOR_export_CMD);
  static final int GROUP_export_DOLLAR_SIGN = 1, GROUP_export_NAME = 2;

  public static String prefix = ""; // Declare here instead of passing around everywhere
  public static StringBuilder log = new StringBuilder(); // Declare here instead of passing around everywhere

  public MetaCommands metaCommands = new MetaCommands();

  public static Prover mainProver = new Prover();
  public boolean printSteps;
  public boolean printDetails;
  public boolean printFlag; // helper variable, could be eliminated
  /**
   * if the command line argument is not empty, we treat args[0] as a filename.
   * if this is the case, we read from the file and load its commands before we submit control to user.
   * if the address is not a valid address or the file does not exist, we print an appropriate error message
   * and submit control to the user.
   * if the file contains the exit command we terminate the program.
   **/
  public static void main(String[] args) {
    String filename = parseArgs(args);
    run(filename);
  }

  private static String parseArgs(String[] args) {
    String filename = null;
    String sessionDir = null;
    String homeDir = null;

    for (String arg : args) {
      if (arg.startsWith("--session-dir=")) {
        sessionDir = arg.substring("--session-dir=".length());
        if (!sessionDir.endsWith("/")) {
          sessionDir += "/";
        }
      } else if (arg.startsWith("--home-dir=")) {
        homeDir = arg.substring("--home-dir=".length());
        if (!homeDir.endsWith("/")) {
          homeDir += "/";
        }
      } else if (filename == null) {
        filename = arg; // Assume the first non-flag argument is the filename
        UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(filename));
      }
    }
    Session.setPathsAndNames(sessionDir, homeDir);
    return filename;
  }
  public static void run(String filename) {
    if (filename != null) {
      File f = UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(filename));
      // read commands from file
      try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
        if (!mainProver.readBuffer(in, false)) return;
      } catch (IOException e) {
        UtilityMethods.printTruncatedStackTrace(e);
      }
    }

    // Parse commands from the console.
    System.out.println("Welcome to Walnut v" + Session.WALNUT_VERSION +
        "! Type \"help;\" to see all available commands.");
    System.out.println("Starting Walnut session: " + Session.getName());
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      mainProver.readBuffer(in, true);
    } catch (Exception e) {
      UtilityMethods.printTruncatedStackTrace(e);
    }
  }

  /**
   * Takes a BufferedReader and reads from it until we hit end of file or exit command.
   * @param console = true if in = System.in
   */
  public boolean readBuffer(BufferedReader in, boolean console) {
    try {
      StringBuilder buffer = new StringBuilder();
      while (true) {
        if (console) {
          System.out.print(Session.PROMPT);
        }

        String s = in.readLine();
        if (s == null) {
          return true;
        }
        s = s.strip(); // Remove spaces, Unicode-aware
        if (s.startsWith("#")) {
          System.out.println(s);
          continue;
        }

        buffer.append(s);

        if (!(s.endsWith(";") || s.endsWith(":"))) {
          // keep appending
          continue;
        }

        s = buffer.toString();
        buffer = new StringBuilder();

        if (!console) {
          System.out.println(s);
        }

        try {
          if (!dispatch(s)) {
            return false;
          }
        } catch (RuntimeException e) {
          UtilityMethods.printTruncatedStackTrace(e);
        }
      }
    } catch (IOException e) {
      UtilityMethods.printTruncatedStackTrace(e);
    }

    return true;
  }

  public boolean dispatch(String s) throws IOException {
    s = parseSetup(s);
    if (s.isEmpty()) {
      return true;
    }

    Matcher matcher_for_command = PAT_FOR_CMD.matcher(s);
    if (!matcher_for_command.find()) {
      throw ExceptionHelper.invalidCommand();
    }

    String commandName = matcher_for_command.group(1);
    if (!commandName.matches(RE_FOR_THE_LIST_OF_CMDS)) {
      throw ExceptionHelper.noSuchCommand();
    }

    switch (commandName) {
      case EXIT, QUIT -> {
        return false;
      }
      case LOAD -> {
        if (!loadCommand(s)) return false;
      }
    }
    processCommand(s, commandName);
    return true;
  }

  private String parseSetup(String s) {
    metaCommands = new MetaCommands();
    prefix = ""; // reset prefix
    log = new StringBuilder(); // reset log
    printSteps = printDetails = printFlag = false; // reset flags

    s = metaCommands.parseMetaCommands(s);

    if (!s.endsWith(";") && !s.endsWith(":")) {
      throw ExceptionHelper.invalidCommand();
    }
    int endingToRemove = 1;
    if (s.endsWith(":")) {
      printSteps = true;
      if (s.endsWith("::")) {
        endingToRemove++;
        printDetails = true;
      }
    }
    printFlag = printSteps || printDetails;
    s = s.substring(0, s.length() - endingToRemove); // remove ;|:|::
    s = s.strip(); // remove end whitespace, Unicode-aware

    return s;
  }

  public TestCase dispatchForIntegrationTest(String s, String msg) throws IOException {
    s = s.strip(); // remove start and end whitespace, Unicode-aware
    s = parseSetup(s);

    System.out.println("Running integration test: " + msg);
    if (s.isEmpty() || s.startsWith("#")) {
      return null;
    }

    Matcher matcher_for_command = PAT_FOR_CMD.matcher(s);
    if (!matcher_for_command.find()) throw ExceptionHelper.invalidCommand();

    String commandName = matcher_for_command.group(1);
    if (!commandName.matches(RE_FOR_THE_LIST_OF_CMDS)) {
      throw ExceptionHelper.noSuchCommand();
    }

    return processCommand(s, commandName);
  }

  private TestCase processCommand(String s, String commandName) throws IOException {
    switch (commandName) {
      case ALPHABET -> {
        return alphabetCommand(s);
      }
      case CLEAR, CLS -> {
        clearScreen();
        return null;
      }
      case COMBINE -> {
        return combineCommand(s);
      }
      case CONCAT -> {
        return concatCommand(s);
      }
      case CONVERT -> {
        return convertCommand(s);
      }
      case DEF, EVAL -> {
        return evalDefCommands(s);
      }
      case DRAW -> {
        return drawCommand(s);
      }
      case EXIT, QUIT -> {
        return null;
      }
      case EXPORT -> {
        return exportCommand(s);
      }
      case FIXLEADZERO -> {
        return fixLeadZeroCommand(s);
      }
      case FIXTRAILZERO -> {
        return fixTrailZeroCommand(s);
      }
      case HELP -> HelpMessages.helpCommand(s);
      case IMAGE -> {
        return imageCommand(s);
      }
      case INF -> {
        infCommand(s);
      }
      case INTERSECT -> {
        return intersectCommand(s);
      }
      case JOIN -> {
        return joinCommand(s);
      }
      case LEFTQUO -> {
        return leftquoCommand(s);
      }
      case LOAD -> {
        if (!loadCommand(s)) return null;
      }
      case MACRO -> {
        return macroCommand(s);
      }
      case MINIMIZE -> {
        return minimizeCommand(s);
      }
      case MORPHISM -> {
        morphismCommand(s);
      }
      case OST -> {
        return ostCommand(s);
      }
      case PROMOTE -> {
        return promoteCommand(s);
      }
      case REG -> {
        return regCommand(s);
      }
      case REVERSE -> {
        return reverseCommand(s);
      }
      case RIGHTQUO -> {
        return rightquoCommand(s);
      }
      case RSPLIT -> {
        return rsplitCommand(s);
      }
      case SPLIT -> {
        return splitCommand(s);
      }
      case STAR -> {
        return starCommand(s);
      }
      case TEST -> {
        testCommand(s);
      }
      case TRANSDUCE -> {
        return transduceCommand(s);
      }
      case UNION -> {
        return unionCommand(s);
      }
      default -> throw ExceptionHelper.invalidCommand(commandName);
    }
    return null;
  }

  /**
   * load x.p; loads commands from the file x.p. The file can contain any command except for load x.p;
   * The user don't get a warning if the x.p contains load x.p but the program might end up in an infinite loop.
   * Note that the file can contain load y.p; whenever y != x and y exist.
   */
  public boolean loadCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_load_CMD, s, LOAD);

    File f = UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(m.group(L_FILENAME)));

    try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
      if (!readBuffer(in, false)) {
        return false;
      }
    } catch (IOException e) {
      UtilityMethods.printTruncatedStackTrace(e);
    }
    return true;
  }

  public TestCase evalDefCommands(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_eval_def_CMDS, s, "eval/def");
    String mplAddress = null;

    List<String> freeVariables = determineFreeVariables(m);

    String predicate = m.group(ED_PREDICATE);
    String evalName = m.group(ED_NAME);

    EvalComputer c = new EvalComputer(predicate, printSteps, printDetails);
    Automaton M = c.result.M;

    String resultName = Session.getAddressForResult() + evalName;
    String gvAddress = resultName + GV_EXTENSION;

    M.writeAutomata(predicate, Session.getWriteAddressForAutomataLibrary(), evalName, false);

    if (!freeVariables.isEmpty()) {
      mplAddress = resultName + ".mpl";
      AutomatonWriter.writeMatrices(M, mplAddress, freeVariables);
    }

    c.writeLogs(resultName, printDetails);

    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      System.out.println("____\n" + (M.fa.isTRUE_AUTOMATON() ? "TRUE" : "FALSE"));
    }

    return new TestCase(M, "", mplAddress, gvAddress, printDetails ? c.logDetails.toString() : "");
  }

  private static List<String> determineFreeVariables(Matcher m) {
    List<String> free_variables = new ArrayList<>();
    String freeVarString = m.group(ED_FREE_VARIABLES);
    if (freeVarString != null) {
      Matcher m1 = PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS.matcher(freeVarString);
      while (m1.find()) {
        String t = m1.group();
        free_variables.add(t);
      }
    }
    return free_variables;
  }


  public static TestCase macroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_macro_CMD, s, MACRO);
    File f = new File(Session.getWriteAddressForMacroLibrary() + m.group(M_NAME) + TXT_EXTENSION);
    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
      out.write(m.group(M_DEFINITION));
    } catch (IOException o) {
      System.out.println("Could not write the macro " + m.group(M_NAME));
    }
    return null;
  }

  public static TestCase regCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_reg_CMD, s, REG);

    List<List<Integer>> alphabets = new ArrayList<>();
    List<NumberSystem> NS = new ArrayList<>();
    if (m.group(R_LIST_OF_ALPHABETS) == null) {
      String base = "msd_2";
      NumberSystem ns = getNumberSystem(base, NS, m);
      alphabets.add(ns.getAlphabet());
    }
    determineAlphabetsAndNS(m, NS, alphabets);
    // To support regular expressions with multiple arity (eg. "[1,0][0,1][0,0]*"), we must translate each of these vectors to an
    // encoding, which will then be turned into a unicode character that dk.brics can work with when constructing an automaton
    // from a regular expression. Since the encoding method is within the Automaton class, we create a dummy instance and load it
    // with our sequence of number systems in order to access it. After the regex automaton is created, we set its alphabet to be the
    // one requested, instead of the unicode alphabet that dk.brics uses.
    Automaton M = new Automaton();
    M.setA(alphabets);
    M.determineAlphabetSize();

    String baseExp = determineBaseExp(m.group(R_REGEXP), M.getA().size(), M.richAlphabet);

    Automaton R = new Automaton(baseExp, M.getAlphabetSize());
    R.setA(M.getA());
    R.determineAlphabetSize();
    R.setNS(NS);

    R.writeAutomata(m.group(R_REGEXP), Session.getWriteAddressForAutomataLibrary(), m.group(R_NAME), false);
    return new TestCase(R);
  }

  private static String determineBaseExp(String baseexp, int inputLength, RichAlphabet r) {
    Matcher m2 = PAT_FOR_AN_ALPHABET_VECTOR.matcher(baseexp);
    // if we haven't had to replace any input vectors with unicode, we use the legacy method of constructing the automaton
    while (m2.find()) {
      String alphabetVector = m2.group();

      // needed to replace this string with the unicode mapping
      String alphabetVectorCopy = alphabetVector;
      if (alphabetVector.charAt(0) == '[') {
        alphabetVector = alphabetVector.substring(1, alphabetVector.length() - 1); // truncate brackets [ ]
      }

      List<Integer> L = new ArrayList<>();
      Matcher m3 = PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(alphabetVector);
      while (m3.find()) {
        L.add(UtilityMethods.parseInt(m3.group()));
      }
      if (L.size() != inputLength) {
        throw new RuntimeException("Mismatch between vector length in regex and specified number of inputs to automaton");
      }
      int vectorEncoding = r.encode(L);
      // dk.brics regex has several reserved characters - we cannot use these or the method that generates the automaton will
      // not be able to parse the string properly. All of these reserved characters have UTF-16 values between 0 and 127, so offsetting
      // our encoding by 128 will be enough to ensure that we have no conflicts
      vectorEncoding += 128;
      char replacement = (char) vectorEncoding;
      String replacementStr = Character.toString(replacement);

      /*
       * If alphabetVectorCopy is "2" and baseexp is "(22|[-2][-2])",
       * and you just run
       * baseexp.replace(alphabetVectorCopy, replacementStr)
       * normally, then this will turn baseexp to "(|[-][-])"
       * instead of "(|[-2][-2])".
       * Instead, we replace all occurrences of "[-2]" with "%PLACEHOLDER%",
       * then run baseexp.replace(alphabetVectorCopy, replacementStr),
       * and then replace "%PLACEHOLDER%" with "[-2]".
       */
      baseexp = baseexp
          .replace("[-" + alphabetVectorCopy + "]", "§")
          .replace(alphabetVectorCopy, replacementStr)
          .replace("§", "[-" + alphabetVectorCopy + "]");
    }

    // We should always do this with replacement, since we may have regexes such as "...", which accepts any three characters
    // in a row, on an alphabet containing bracketed characters. We don't make any replacements here, but they are implicitly made
    // when we intersect with our alphabet(s).

    // remove all whitespace from regular expression.
    return baseexp.replaceAll("\\s", "");
  }

  private static NumberSystem getNumberSystem(String base, List<NumberSystem> numSys, Matcher m) {
    try {
      if (!Predicate.numberSystemHash.containsKey(base))
        Predicate.numberSystemHash.put(base, new NumberSystem(base));
      NumberSystem ns = Predicate.numberSystemHash.get(base);
      numSys.add(Predicate.numberSystemHash.get(base));
      return ns;
    } catch (RuntimeException e) {
      throw new RuntimeException("number system " + base + " does not exist: char at " + m.start(R_NUMBER_SYSTEM) + System.lineSeparator() + "\t:" + e.getMessage());
    }
  }

  public TestCase combineCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_combine_CMD, s, COMBINE);
    
    List<String> automataNames = new ArrayList<>();
    IntList outputs = new IntArrayList();
    int argumentCounter = 0;

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_combine_CMD.matcher(m.group(GROUP_COMBINE_AUTOMATA));
    while (m1.find()) {
      argumentCounter++;
      String t = m1.group(1);
      String u = m1.group(2);
      // if no output is specified for a subautomaton, the default output is the index of the subautomaton in the argument list
      if (u.isEmpty()) {
        outputs.add(argumentCounter);
      } else {
        u = u.substring(1);
        // remove colon then convert string to integer
        outputs.add(Integer.parseInt(u));
      }
      automataNames.add(t);
    }

    if (automataNames.isEmpty()) {
      throw new RuntimeException("Combine requires at least one automaton as input.");
    }
    Automaton first = Automaton.readAutomatonFromFile(automataNames.get(0));
    automataNames.remove(0);

    Automaton C = first.combine(automataNames, outputs, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_COMBINE_NAME), true);
    return new TestCase(C);
  }


  public static void morphismCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_morphism_CMD, s, MORPHISM);

    String name = m.group(GROUP_MORPHISM_NAME);

    Morphism M = new Morphism();
    M.parseMap(m.group(GROUP_MORPHISM_DEFINITION));
    System.out.print("Defined with domain ");
    System.out.print(M.mapping.keySet());
    System.out.print(" and range ");
    System.out.print(M.range);
    M.write(Session.getAddressForResult() + name + TXT_EXTENSION);
    M.write(Session.getWriteAddressForMorphismLibrary() + name + TXT_EXTENSION);
  }

  public static TestCase promoteCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_promote_CMD, s, PROMOTE);

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_PROMOTE_MORPHISM) + TXT_EXTENSION));
    Automaton P = h.toWordAutomaton();

    P.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_PROMOTE_NAME), true);
    return new TestCase(P);
  }

  public TestCase imageCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_image_CMD, s, IMAGE);

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_IMAGE_MORPHISM) + TXT_EXTENSION));
    if (!h.isUniform()) {
      throw new RuntimeException("A morphism applied to a word automaton must be uniform.");
    }
    StringBuilder combineString = new StringBuilder("combine " + m.group(GROUP_IMAGE_NEW_NAME));

    // We need to know the number system of our old automaton: the new one should match, as should intermediary expressions
    Automaton M = new Automaton(Session.getReadFileForWordsLibrary(m.group(GROUP_IMAGE_OLD_NAME) + TXT_EXTENSION));
    String numSysName = "";
    if (!M.getNS().isEmpty()) {
      numSysName = M.getNS().get(0).toString();
    }

    // we construct a define command for a DFA for each x that accepts iff x appears at the nth position
    for (Integer value : h.range) {
      evalDefCommands(h.makeInterCommand(value, m.group(GROUP_IMAGE_OLD_NAME), numSysName));
      combineString.append(" ").append(m.group(GROUP_IMAGE_OLD_NAME)).append("_").append(value).append("=").append(value);
    }
    combineString.append(":");

    TestCase retrieval = combineCommand(combineString.toString());
    Automaton I = retrieval.getResult().clone();

    I.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_IMAGE_NEW_NAME), true);
    return new TestCase(I);
  }

  public static boolean infCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_inf_CMD, s, INF);

    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_INF_NAME));
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);
    String infReg = M.fa.infinite(M.richAlphabet);
    if (infReg.isEmpty()) {
      System.out.println("Automaton " + m.group(GROUP_INF_NAME) + " accepts finitely many values.");
      return false;
    } else {
      System.out.println(infReg);
      return true;
    }
  }

  public TestCase processSplitCommand(
      String s, boolean isReverse,
      String automatonName, String name, Matcher inputPattern) {

    String addressForWordAutomaton =
        Session.getReadFileForWordsLibrary(automatonName + TXT_EXTENSION);

    Automaton M;
    boolean isDFAO;
    if ((new File(addressForWordAutomaton)).isFile()) {
      M = new Automaton(addressForWordAutomaton);
      isDFAO = true;
    } else {
      String addressForAutomaton =
          Session.getReadFileForAutomataLibrary(automatonName + TXT_EXTENSION);
      if ((new File(addressForAutomaton)).isFile()) {
        M = new Automaton(addressForAutomaton);
        isDFAO = false;
      } else {
        throw new RuntimeException("Automaton " + automatonName + " does not exist.");
      }
    }

    List<ArithmeticOperator.Ops> plusMinusInputs = new ArrayList<>();
    boolean hasInput = false;
    while (inputPattern.find()) {
      String t = inputPattern.group(1);
      ArithmeticOperator.Ops tOp = t.isEmpty() ? null : ArithmeticOperator.Ops.fromSymbol(t);
      if (tOp != null && tOp != ArithmeticOperator.Ops.PLUS && tOp != ArithmeticOperator.Ops.MINUS) {
        throw ExceptionHelper.invalidCommand(t);
      }
      hasInput = hasInput || (tOp != null);
      plusMinusInputs.add(tOp);
    }
    if (!hasInput || plusMinusInputs.isEmpty()) {
      throw new RuntimeException("Cannot split without inputs.");
    }

    IntList outputs = new IntArrayList(M.getO());
    UtilityMethods.removeDuplicates(outputs);
    List<Automaton> subautomata = WordAutomaton.uncombine(M, outputs);

    subautomata.replaceAll(automaton -> automaton.processSplit(plusMinusInputs, isReverse, printFlag, prefix, log));

    Automaton N = subautomata.remove(0);
    N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printFlag, prefix, log);

    N.writeAutomata(s,
        isDFAO ? Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary(),
        name, isDFAO);
    return new TestCase(N);
  }

  public TestCase splitCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_split_CMD, s, SPLIT);
    return processSplitCommand(s, false,
        m.group(GROUP_SPLIT_AUTOMATA), m.group(GROUP_SPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_SPLIT_INPUT)));
  }

  public TestCase rsplitCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_rsplit_CMD, s, REVERSE_SPLIT);
    return processSplitCommand(s, true,
        m.group(GROUP_RSPLIT_AUTOMATA), m.group(GROUP_RSPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_RSPLIT_INPUT)));
  }

  public TestCase joinCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_join_CMD, s, JOIN);
    
    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_join_CMD.matcher(m.group(GROUP_JOIN_AUTOMATA));
    List<Automaton> subautomata = new ArrayList<>();
    boolean isDFAO = false;
    while (m1.find()) {
      String automatonName = m1.group(GROUP_JOIN_AUTOMATON_NAME);
      String addressForWordAutomaton
          = Session.getReadFileForWordsLibrary(automatonName + TXT_EXTENSION);
      Automaton M;
      if ((new File(addressForWordAutomaton)).isFile()) {
        M = new Automaton(addressForWordAutomaton);
        isDFAO = true;
      } else {
        String addressForAutomaton
            = Session.getReadFileForAutomataLibrary(automatonName + TXT_EXTENSION);
        if ((new File(addressForAutomaton)).isFile()) {
          M = new Automaton(addressForAutomaton);
        } else {
          throw new RuntimeException("Automaton " + automatonName + " does not exist.");
        }
      }

      String automatonInputs = m1.group(GROUP_JOIN_AUTOMATON_INPUT);
      Matcher m2 = PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD.matcher(automatonInputs);
      List<String> label = new ArrayList<>();
      while (m2.find()) {
        String t = m2.group(1);
        label.add(t);
      }
      if (label.size() != M.getA().size()) {
        throw new RuntimeException("Number of inputs of word automata " + automatonName + " does not match number of inputs specified.");
      }
      M.setLabel(label);
      subautomata.add(M);
    }
    Automaton N = subautomata.remove(0);
    N = N.join(new LinkedList<>(subautomata), printFlag, prefix, log);

    N.writeAutomata(s,
        isDFAO ? Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary(),
        m.group(GROUP_JOIN_NAME), isDFAO);
    return new TestCase(N);
  }


  public static void testCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_test_CMD, s, TEST);

    int needed = Integer.parseInt(m.group(GROUP_TEST_NUM));

    String testName = m.group(GROUP_TEST_NAME);

    // We find the first n inputs accepted by our automaton, lexicographically. If less than n inputs are accepted,
    // we output all that are.
    Automaton M = Automaton.readAutomatonFromFile(testName);

    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);

    String infSubcommand = "inf " + testName + ";";
    boolean infinite = infCommand(infSubcommand);

    StringBuilder incLengthReg = new StringBuilder();
    incLengthReg.append("reg ").append(testName).append("_len ");
    for (int i = 0; i < M.getA().size(); i++) {
      String alphaString = M.getA().get(i).toString();
      alphaString = alphaString.substring(1, alphaString.length() - 1);
      alphaString = "{" + alphaString + "} ";
      incLengthReg.append(alphaString);
    }

    StringBuilder dotReg = new StringBuilder();
    int searchLength = 0;
    List<String> accepted = new ArrayList<>();
    while (true) {
      searchLength++;
      dotReg.append(".");
      TestCase retrieval = regCommand(incLengthReg + "\"" + dotReg + "\";");
      Automaton R = retrieval.getResult().clone();

      // and-ing automata uses the cross product routine, which requires labeled automata
      R.setLabel(M.getLabel());
      Automaton N = AutomatonLogicalOps.and(M, R, false, null, null);
      accepted.addAll(N.findAccepted(searchLength, needed - accepted.size()));
      if (accepted.size() >= needed) {
        break;
      }

      // If our automaton accepts finitely many inputs, it does not have a non-redundant cycle, and so the highest length input that could be
      // accepted is equal to the number of states in the automaton
      if (!(infinite) && (searchLength >= M.getQ())) {
        break;
      }
    }
    if (accepted.size() < needed) {
      System.out.println(testName + " only accepts " + accepted.size() + " inputs, which are as follows: ");
    }
    for (String input : accepted) {
      System.out.println(input);
    }
  }

  public static TestCase ostCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_ost_CMD, s, OST);

    String name = m.group(GROUP_OST_NAME);
    Ostrowski ostr = new Ostrowski(name, m.group(GROUP_OST_PREPERIOD), m.group(GROUP_OST_PERIOD));
    Ostrowski.writeAutomaton(name, "msd_" + name + TXT_EXTENSION, ostr.createRepresentationAutomaton());
    Automaton adder = ostr.createAdderAutomaton();
    Ostrowski.writeAutomaton(name, "msd_" + name + "_addition.txt", adder);
    return new TestCase(adder);
  }

  public TestCase transduceCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_transduce_CMD, s, TRANSDUCE);
    
    Transducer T = new Transducer(Session.getTransducerFile(m.group(GROUP_TRANSDUCE_TRANSDUCER) + TXT_EXTENSION));
    String inFileName = m.group(GROUP_TRANSDUCE_OLD_NAME) + TXT_EXTENSION;
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);

    Automaton C = T.transduceNonDeterministic(M, printFlag, prefix, log);
    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_TRANSDUCE_NEW_NAME), true);
    return new TestCase(C);
  }


  public TestCase reverseCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_reverse_CMD, s, REVERSE);
    
    boolean isDFAO = true;

    String inFileName = m.group(GROUP_REVERSE_OLD_NAME) + TXT_EXTENSION;
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
      isDFAO = false;
    }
    Automaton M = new Automaton(inLibrary);

    if (isDFAO) {
      WordAutomaton.reverseWithOutput(M, true, printFlag, prefix, log);
    } else {
      AutomatonLogicalOps.reverse(M, printFlag, prefix, log, true);
    }

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }

    M.writeAutomata(s, outLibrary, m.group(GROUP_REVERSE_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase minimizeCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_minimize_CMD, s, MINIMIZE);
    
    Automaton M = new Automaton(
        Session.getReadFileForWordsLibrary(m.group(GROUP_MINIMIZE_OLD_NAME) + TXT_EXTENSION));

    WordAutomaton.minimizeSelfWithOutput(M, printFlag, prefix, log);

    M.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_MINIMIZE_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase convertCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_convert_CMD, s, CONVERT);

    String newDollarSign = m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN);
    String oldDollarSign = m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN);
    if (newDollarSign.equals("$")
        && !oldDollarSign.equals("$")) {
      throw new RuntimeException("Cannot convert a Word Automaton into a function");
    }
    
    String inFileName = m.group(GROUP_CONVERT_OLD_NAME) + TXT_EXTENSION;
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (oldDollarSign.equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);

    AutomatonLogicalOps.convertNS(M, m.group(GROUP_CONVERT_MSD_OR_LSD).equals(NumberSystem.MSD),
        Integer.parseInt(m.group(GROUP_CONVERT_BASE)), printFlag,
        prefix, log);

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (newDollarSign.equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }

    M.writeAutomata(s, outLibrary, m.group(GROUP_CONVERT_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase fixLeadZeroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_fixleadzero_CMD, s, FIXLEADZERO);
    
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXLEADZERO_OLD_NAME));

    AutomatonLogicalOps.fixLeadingZerosProblem(M, printFlag, prefix, log);

    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXLEADZERO_NEW_NAME), false);
    return new TestCase(M);
  }


  public TestCase fixTrailZeroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_fixtrailzero_CMD, s, FIXTRAILZERO);
    
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXTRAILZERO_OLD_NAME));

    AutomatonLogicalOps.fixTrailingZerosProblem(M, printFlag, prefix, log);

    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXTRAILZERO_NEW_NAME), false);
    return new TestCase(M);
  }

  public TestCase alphabetCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_alphabet_CMD, s, ALPHABET);

    if (m.group(GROUP_alphabet_LIST_OF_ALPHABETS) == null) {
      throw new RuntimeException("List of alphabets for alphabet command must not be empty.");
    }
    
    boolean isDFAO = true;

    String inFileName = m.group(GROUP_alphabet_OLD_NAME) + TXT_EXTENSION;
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
      isDFAO = false;
    }

    List<NumberSystem> NS = new ArrayList<>();
    List<List<Integer>> alphabets = new ArrayList<>();
    determineAlphabetsAndNS(m, NS, alphabets);

    Automaton M = new Automaton(inLibrary);

    // here, call the function to set the number system.
    M.setAlphabet(isDFAO, NS, alphabets, printFlag, prefix, log);

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }

    M.writeAutomata(s, outLibrary, m.group(GROUP_alphabet_NEW_NAME), false);
    return new TestCase(M);
  }

  private static void determineAlphabetsAndNS(Matcher m, List<NumberSystem> NS, List<List<Integer>> alphabets) {
    Matcher m1 = PAT_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
    int counter = 1;
    while (m1.find()) {
      if ((m1.group(R_NUMBER_SYSTEM) != null)) {
        String base = "msd_2";
        if (m1.group(3) != null) base = m1.group(3);
        if (m1.group(6) != null) base = m1.group(7) + "_" + m1.group(8);
        if (m1.group(9) != null) base = m1.group(9) + "_2";
        if (m1.group(10) != null) base = "msd_" + m1.group(10);
        NumberSystem ns = getNumberSystem(base, NS, m);
        alphabets.add(ns.getAlphabet());
      } else if (m1.group(R_SET) != null) {
        alphabets.add(determineAlphabet(m1.group(R_SET)));
        NS.add(null);
      } else {
        throw new RuntimeException("Alphabet at position " + counter + " not recognized in alphabet command");
      }
      counter += 1;
    }
  }

  public TestCase unionCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_union_CMD, s, UNION);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_union_CMD.matcher(m.group(GROUP_UNION_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new RuntimeException("Union requires at least one automaton as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, UNION, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_UNION_NAME), true);
    return new TestCase(C);
  }

  public TestCase intersectCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_intersect_CMD, s, INTERSECT);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_intersect_CMD.matcher(m.group(GROUP_INTERSECT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new RuntimeException("Intersect requires at least one automaton as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, INTERSECT, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_INTERSECT_NAME), true);
    return new TestCase(C);
  }


  public TestCase starCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_star_CMD, s, STAR);
    
    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_STAR_OLD_NAME) + TXT_EXTENSION));

    Automaton C = M.star(printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_STAR_NEW_NAME), false);
    return new TestCase(C);
  }

  public TestCase concatCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_concat_CMD, s, CONCAT);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_concat_CMD.matcher(m.group(GROUP_CONCAT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.size() < 2) {
      throw new RuntimeException("Concatenation requires at least two automata as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.concat(automataNames, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_CONCAT_NAME), true);
    return new TestCase(C);
  }


  public TestCase rightquoCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_rightquo_CMD, s, RIGHTQUO);
    
    Automaton M1 = Automaton.readAutomatonFromFile(m.group(GROUP_rightquo_OLD_NAME1));
    Automaton M2 = Automaton.readAutomatonFromFile(m.group(GROUP_rightquo_OLD_NAME2));

    Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_rightquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public TestCase leftquoCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_leftquo_CMD, s, LEFTQUO);
    
    Automaton M1 = Automaton.readAutomatonFromFile(m.group(GROUP_leftquo_OLD_NAME1));
    Automaton M2 = Automaton.readAutomatonFromFile(m.group(GROUP_leftquo_OLD_NAME2));

    Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_leftquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public static TestCase drawCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_draw_CMD, s, DRAW);

    String inFileName = m.group(GROUP_draw_NAME) + TXT_EXTENSION;
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_draw_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);
    AutomatonWriter.draw(M, Session.getAddressForResult() + m.group(GROUP_draw_NAME) + GV_EXTENSION, s, false);

    return new TestCase(M);
  }

  public static TestCase exportCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_export_CMD, s, EXPORT);

    String inFileName = m.group(GROUP_export_NAME) + TXT_EXTENSION;
    boolean isDFAO = !m.group(GROUP_export_DOLLAR_SIGN).equals("$");
    if (isDFAO) {
      throw new RuntimeException("Can't export DFAO to BA format");
    }
    String inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    Automaton M = new Automaton(inLibrary);
    AutomatonWriter.exportToBA(M.fa, Session.getAddressForResult() + m.group(GROUP_export_NAME) + BA_EXTENSION, false);

    return new TestCase(M);
  }


  private static Matcher matchOrFail(Pattern pattern, String input, String commandName) {
    Matcher m = pattern.matcher(input);
    if (!m.find()) {
      throw ExceptionHelper.invalidCommandUse(commandName);
    }
    return m;
  }

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  private static List<Integer> determineAlphabet(String s) {
    List<Integer> L = new ArrayList<>();
    s = s.substring(1, s.length() - 1); //truncation { and } from beginning and end
    Matcher m = PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(s);
    while (m.find()) {
      L.add(UtilityMethods.parseInt(m.group()));
    }
    UtilityMethods.removeDuplicates(L);

    return L;
  }

}
