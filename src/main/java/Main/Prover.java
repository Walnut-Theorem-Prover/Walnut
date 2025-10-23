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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import static Automata.NumberSystem.MSD_2;
import static Automata.NumberSystem.MSD_UNDERSCORE;
import static Main.TestCase.DEFAULT_TESTFILE;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 */
public class Prover {
  static final String RE_FOR_THE_LIST_OF_CMDS = "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|describe|export|help)";
  static final String RE_START = "^";
  static final String RE_WORD_OF_CMD_NO_SPC = "([a-zA-Z]\\w*)";

  static final String RE_WORD_OF_CMD = "\\s+" + RE_WORD_OF_CMD_NO_SPC;

  /**
   * the high-level scheme of a command is a name followed by some arguments and ending in either ; : or ::
   */
  static final String RE_FOR_CMD = RE_START + "(\\w+)(\\s+.*)?";
  static final Pattern PAT_FOR_CMD = Pattern.compile(RE_FOR_CMD);

  public static final String FIXTRAILZERO = "fixtrailzero";
  public static final String CONVERT = "convert";
  public static final String REVERSE_SPLIT = "reverse split";
  public static final String REG = "reg";
  public static final String LOAD = "load";
  public static final String ALPHABET = "alphabet";
  public static final String HELP = "help";
  public static final String CLEAR = "clear";
  public static final String CLS = "cls";
  public static final String DEF = "def";
  public static final String EVAL = "eval";
  public static final String EXIT = "exit";
  public static final String QUIT = "quit";
  public static final String LEFT_BRACKET = "[";
  public static final String DOT = ".";
  public static final String TXT_STRING = "txt";
  public static final String TXT_EXTENSION = DOT + TXT_STRING;
  public static final String GV_STRING = "gv";
  public static final String GV_EXTENSION = DOT + GV_STRING;
  public static final String BA_STRING = "ba";
  public static final String BA_EXTENSION = DOT + BA_STRING;
  public static final String MPL_STRING = "mpl";
  public static final String MPL_EXTENSION = DOT + MPL_STRING;
  public static final String FIRST_OP = "first";
  public static final String IF_OTHER_OP = "if_other";

  /**
   * group for filename in RE_FOR_load_CMD
   */
  static int L_FILENAME = 1;
  static final String RE_FOR_load_CMD = RE_START + LOAD + "\\s+(\\w+\\.txt)";
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

  // Meta-commands: [...] at the beginning of the command
  static final Pattern PAT_META_CMD = Pattern.compile("^\\[([^]]*)](.*)$");
  static final int GROUP_META_CMD = 1, GROUP_FINAL_CMD = 2;

  static final String STRATEGY = "strategy";
  static final String EXPORT = "export";
  static final String EARLY_EXIST_TERMINATION = "earlyExistTermination";

  static final String RE_FOR_export_CMD = RE_START + EXPORT + DOLLAR + RE_WORD_OF_CMD_NO_SPC + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_export_CMD = Pattern.compile(RE_FOR_export_CMD);
  static final int GROUP_export_DOLLAR_SIGN = 1, GROUP_export_NAME = 2, GROUP_export_TYPE = 3;

  public static final String DESCRIBE = "describe";
  static final String RE_FOR_describe_CMD = RE_START + DESCRIBE + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_describe_CMD = Pattern.compile(RE_FOR_describe_CMD);
  static final int GROUP_describe_DOLLAR_SIGN = 1, GROUP_describe_NAME = 2;

  public static String prefix = ""; // Declare here instead of passing around everywhere
  public static StringBuilder log = new StringBuilder(); // Declare here instead of passing around everywhere

  public MetaCommands metaCommands = new MetaCommands();

  public static Prover mainProver = new Prover();
  public boolean printSteps;
  public boolean printDetails;
  public boolean printFlag; // helper variable, could be eliminated

  public static String currentEvalName; // current evaluation name, used for export metacommand
  public static boolean usingOTF = false; // whether the current command is using OTF algorithms
  public static boolean earlyExistTermination = false; // earlyExistTermination metacommand
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

  static String parseArgs(String[] args) {
    String filename = null;
    String sessionDir = null;
    String homeDir = null;

    for (String arg : args) {
      if (arg.startsWith("--help")) {
        System.out.println("Usage: walnut [--session-dir=<SESSION_DIR>] [--home-dir=<HOME_DIR>] <filename>");
        System.out.println("If Session directory is specified, use instead of generated Session directory.");
        System.out.println("If Home directory is specified, use instead of current directory.");
        System.out.println("If filename is specified, read commands from file. Similar to `load` command.");
        System.exit(0);
      }
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
      throw WalnutException.invalidCommand();
    }

    String commandName = matcher_for_command.group(1);
    if (!commandName.matches(RE_FOR_THE_LIST_OF_CMDS)) {
      throw WalnutException.noSuchCommand();
    }

    boolean exitVal = !(commandName.equals(EXIT) || commandName.equals(QUIT));
    if (commandName.equals(LOAD)) {
      exitVal = loadCommand(s); // special-case, since load is a batch command
    } else {
      processCommand(s, commandName);
    }

    if (Prover.usingOTF) {
      UtilityMethods.logAndPrint(true,
          "\nIf the CCL(S) or BRZ-CCL(S) algorithms are used in a result," +
              "please cite the OTF paper by John Nicol and Markus Frohme." +
              "the latest citation information is available at https://github.com/jn1z/OTF/blob/main/README.md", log);
    }
    return exitVal;
  }

  private String parseSetup(String s) {
    metaCommands = new MetaCommands();
    prefix = ""; // reset prefix
    log = new StringBuilder(); // reset log
    printSteps = printDetails = printFlag = false; // reset flags

    if (!s.endsWith(";") && !s.endsWith(":")) {
      throw WalnutException.invalidCommand();
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

    s = metaCommands.parseMetaCommands(s, printDetails);

    return s;
  }

  public TestCase dispatchForIntegrationTest(String s, String msg) throws IOException {
    s = s.strip(); // remove start and end whitespace, Unicode-aware
    s = parseSetup(s);

    if (s.isEmpty() || s.startsWith("#")) {
      return null;
    }

    Matcher matcher_for_command = PAT_FOR_CMD.matcher(s);
    if (!matcher_for_command.find()) throw WalnutException.invalidCommand();

    String commandName = matcher_for_command.group(1);
    if (!commandName.matches(RE_FOR_THE_LIST_OF_CMDS)) {
      throw WalnutException.noSuchCommand();
    }

    return processCommand(s, commandName);
  }

  private TestCase processCommand(String s, String commandName) throws IOException {
    switch (commandName) {
      case ALPHABET -> {
        return alphabetCommand(s);
      }
      case CLEAR, CLS -> {
        ProverHelper.clearScreen();
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
      case DESCRIBE -> {
        return describeCommand(s);
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
      default -> throw WalnutException.invalidCommand(commandName);
    }
    return null;
  }

  /**
   * load x.p; loads commands from the file x.p. The file can contain any command except for load x.p;
   * The user don't get a warning if the x.p contains load x.p but the program might end up in an infinite loop.
   * Note that the file can contain load y.p; whenever y != x and y exist.
   */
  public boolean loadCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_load_CMD, s, LOAD);

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
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_eval_def_CMDS, s, "eval/def");

    String predicate = m.group(ED_PREDICATE);
    String evalName = m.group(ED_NAME);
    currentEvalName = evalName; // used for export metacommand

    EvalComputer c = new EvalComputer(predicate, printSteps, printDetails);
    Automaton M = c.result.M;

    String resultName = Session.getAddressForResult() + evalName;
    String gvAddress = resultName + GV_EXTENSION;

    M.writeAutomata(predicate, Session.getWriteAddressForAutomataLibrary(), evalName, false);

    String mplAddress = writeMatricesIfFreeVariables(m.group(ED_FREE_VARIABLES), resultName, M);

    c.writeLogs(resultName, printDetails);

    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      System.out.println("____\n" + (M.fa.isTRUE_AUTOMATON() ? "TRUE" : "FALSE"));
    }

    return new TestCase(M, "", mplAddress, gvAddress, printDetails ? c.logDetails.toString() : "",
        List.of(new TestCase.AutomatonFilenamePair(M, DEFAULT_TESTFILE)));
  }

  private static String writeMatricesIfFreeVariables(String freeVariablesStr, String resultName, Automaton M) {
    String mplAddress = null;
    List<String> freeVariables = determineFreeVariables(freeVariablesStr);
    if (!freeVariables.isEmpty()) {
      mplAddress = resultName + MPL_EXTENSION;
      AutomatonWriter.writeMatrices(M, mplAddress, freeVariables);
    }
    return mplAddress;
  }

  static List<String> determineFreeVariables(String freeVarString) {
    List<String> free_variables = new ArrayList<>();
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
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_macro_CMD, s, MACRO);
    File f = new File(Session.getWriteAddressForMacroLibrary() + m.group(M_NAME) + TXT_EXTENSION);
    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
      out.write(m.group(M_DEFINITION));
    } catch (IOException o) {
      System.out.println("Could not write the macro " + m.group(M_NAME));
    }
    return null;
  }

  public static TestCase regCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_reg_CMD, s, REG);

    List<List<Integer>> alphabets = new ArrayList<>();
    List<NumberSystem> NS = new ArrayList<>();
    if (m.group(R_LIST_OF_ALPHABETS) == null) {
      NumberSystem ns = ProverHelper.getNumberSystem(MSD_2, NS, m.start(Prover.R_NUMBER_SYSTEM));
      alphabets.add(ns.getAlphabet());
    }
    determineAlphabetsAndNS(m, NS, alphabets);
    // To support regular expressions with multiple arity (eg. "[1,0][0,1][0,0]*"), we must translate each of these vectors to an
    // encoding, which will then be turned into a unicode character that dk.brics can work with when constructing an automaton
    // from a regular expression. Since the encoding method is within the Automaton class, we create a dummy instance and load it
    // with our sequence of number systems in order to access it. After the regex automaton is created, we set its alphabet to be the
    // one requested, instead of the unicode alphabet that dk.brics uses.
    Automaton M = new Automaton();
    M.richAlphabet.setA(alphabets);
    M.determineAlphabetSize();

    String regex = ProverHelper.determineEncodedRegex(m.group(R_REGEXP), M.richAlphabet.getA().size(), M.richAlphabet);

    Automaton R = new Automaton(regex, M.getAlphabetSize());
    R.richAlphabet.setA(M.richAlphabet.getA());
    R.determineAlphabetSize();
    R.setNS(NS);

    R.writeAutomata(m.group(R_REGEXP), Session.getWriteAddressForAutomataLibrary(), m.group(R_NAME), false);
    return new TestCase(R);
  }

  public TestCase combineCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_combine_CMD, s, COMBINE);
    
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
      throw new WalnutException("Combine requires at least one automaton as input.");
    }
    Automaton first = Automaton.readAutomatonFromFile(automataNames.get(0));
    automataNames.remove(0);

    Automaton C = first.combine(automataNames, outputs, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_COMBINE_NAME), true);
    return new TestCase(C);
  }

  public TestCase describeCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_describe_CMD, s, DESCRIBE);
    String inFileName = m.group(GROUP_describe_NAME) + TXT_EXTENSION;
    boolean isDFAO = !m.group(GROUP_describe_DOLLAR_SIGN).equals("$");
    return ProverHelper.describe(isDFAO, inFileName);
  }

  public static void morphismCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_morphism_CMD, s, MORPHISM);
    String name = m.group(GROUP_MORPHISM_NAME);
    String morphismDefinition = m.group(GROUP_MORPHISM_DEFINITION);
    ProverHelper.morphismCommand(morphismDefinition, name);
  }

  public static TestCase promoteCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_promote_CMD, s, PROMOTE);

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_PROMOTE_MORPHISM) + TXT_EXTENSION));
    Automaton P = h.toWordAutomaton();

    P.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_PROMOTE_NAME), true);
    return new TestCase(P);
  }

  public TestCase imageCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_image_CMD, s, IMAGE);

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_IMAGE_MORPHISM) + TXT_EXTENSION));
    if (!h.isUniform()) {
      throw new WalnutException("A morphism applied to a word automaton must be uniform.");
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
    if (retrieval.getAutomatonPairs().size() != 1) {
      throw new WalnutException("Unexpected combine output");
    }
    Automaton I = retrieval.getAutomatonPairs().get(0).automaton().clone();

    I.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_IMAGE_NEW_NAME), true);
    return new TestCase(I);
  }

  public static boolean infCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_inf_CMD, s, INF);
    String address = m.group(GROUP_INF_NAME);
    return ProverHelper.infFromAddress(address);
  }

  public TestCase splitCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_split_CMD, s, SPLIT);
    return ProverHelper.processSplitCommand(s, false,
        m.group(GROUP_SPLIT_AUTOMATA), m.group(GROUP_SPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_SPLIT_INPUT)), printFlag);
  }

  public TestCase rsplitCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_rsplit_CMD, s, REVERSE_SPLIT);
    return ProverHelper.processSplitCommand(s, true,
        m.group(GROUP_RSPLIT_AUTOMATA), m.group(GROUP_RSPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_RSPLIT_INPUT)), printFlag);
  }

  public TestCase joinCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_join_CMD, s, JOIN);
    
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
        M = new Automaton(addressForAutomaton);
      }

      String automatonInputs = m1.group(GROUP_JOIN_AUTOMATON_INPUT);
      Matcher m2 = PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD.matcher(automatonInputs);
      List<String> label = new ArrayList<>();
      while (m2.find()) {
        String t = m2.group(1);
        label.add(t);
      }
      if (label.size() != M.richAlphabet.getA().size()) {
        throw new WalnutException("Number of inputs of word automata " + automatonName + " does not match number of inputs specified.");
      }
      M.setLabel(label);
      subautomata.add(M);
    }
    Automaton N = subautomata.remove(0);
    N = N.join(new LinkedList<>(subautomata), printFlag, prefix, log);

    N.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), m.group(GROUP_JOIN_NAME), isDFAO);
    return new TestCase(N);
  }


  public static boolean testCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_test_CMD, s, TEST);
    return ProverHelper.testCommand(m.group(GROUP_TEST_NAME), Integer.parseInt(m.group(GROUP_TEST_NUM)));
  }

  public static TestCase ostCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_ost_CMD, s, OST);

    String name = m.group(GROUP_OST_NAME);
    Ostrowski ostr = new Ostrowski(name, m.group(GROUP_OST_PREPERIOD), m.group(GROUP_OST_PERIOD));
    Automaton repr = ostr.createRepresentationAutomaton();
    String msdName = NumberSystem.MSD_UNDERSCORE + name;
    Ostrowski.writeAutomaton(name, msdName + TXT_EXTENSION, repr);
    Automaton adder = ostr.createAdderAutomaton();
    Ostrowski.writeAutomaton(name, msdName + NumberSystem.UNDERSCORE_ADDITION_AUTOMATON, adder);

    return new TestCase(adder,
        List.of(new TestCase.AutomatonFilenamePair(adder, DEFAULT_TESTFILE),
            new TestCase.AutomatonFilenamePair(repr, TestCase.OST_REPR_TESTFILE)));
  }

  public TestCase transduceCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_transduce_CMD, s, TRANSDUCE);
    
    Transducer T = new Transducer(Session.getTransducerFile(m.group(GROUP_TRANSDUCE_TRANSDUCER) + TXT_EXTENSION));
    String inFileName = m.group(GROUP_TRANSDUCE_OLD_NAME) + TXT_EXTENSION;
    boolean isDFAO = !(m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$"));
    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));

    Automaton C = T.transduceNonDeterministic(M, printFlag, prefix, log);
    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_TRANSDUCE_NEW_NAME), true);
    return new TestCase(C);
  }

  public TestCase reverseCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_reverse_CMD, s, REVERSE);
    boolean isDFAO = !m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$");
    String inFileName = m.group(GROUP_REVERSE_OLD_NAME) + TXT_EXTENSION;
    String newName = m.group(GROUP_REVERSE_NEW_NAME);
    return ProverHelper.reverseCommand(s, inFileName, isDFAO, newName, printFlag);
  }

  public TestCase minimizeCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_minimize_CMD, s, MINIMIZE);
    
    Automaton M = new Automaton(
        Session.getReadFileForWordsLibrary(m.group(GROUP_MINIMIZE_OLD_NAME) + TXT_EXTENSION));

    WordAutomaton.minimizeSelfWithOutput(M, printFlag, prefix, log);

    M.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_MINIMIZE_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase convertCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_convert_CMD, s, CONVERT);

    String newDollarSign = m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN);
    boolean newIsDFAO = !newDollarSign.equals("$");
    String oldDollarSign = m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN);
    boolean oldIsDFAO = !oldDollarSign.equals("$");
    if (oldIsDFAO && !newIsDFAO) {
      throw WalnutException.convertDFAOIntoFunction();
    }
    
    String inFileName = m.group(GROUP_CONVERT_OLD_NAME) + TXT_EXTENSION;
    String inLibrary = ProverHelper.determineInLibrary(oldIsDFAO, inFileName);
    Automaton M = new Automaton(inLibrary);

    AutomatonLogicalOps.convertNS(M, m.group(GROUP_CONVERT_MSD_OR_LSD).equals(NumberSystem.MSD),
        Integer.parseInt(m.group(GROUP_CONVERT_BASE)), printFlag,
        prefix, log);

    M.writeAutomata(s, ProverHelper.determineOutLibrary(newIsDFAO), m.group(GROUP_CONVERT_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase fixLeadZeroCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_fixleadzero_CMD, s, FIXLEADZERO);
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXLEADZERO_OLD_NAME));
    AutomatonLogicalOps.fixLeadingZerosProblem(M, printFlag, prefix, log);
    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXLEADZERO_NEW_NAME), false);
    return new TestCase(M);
  }


  public TestCase fixTrailZeroCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_fixtrailzero_CMD, s, FIXTRAILZERO);
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXTRAILZERO_OLD_NAME));
    AutomatonLogicalOps.fixTrailingZerosProblem(M, printFlag, prefix, log);
    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXTRAILZERO_NEW_NAME), false);
    return new TestCase(M);
  }

  public TestCase alphabetCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_alphabet_CMD, s, ALPHABET);

    if (m.group(GROUP_alphabet_LIST_OF_ALPHABETS) == null) {
      throw new WalnutException("List of alphabets for alphabet command must not be empty.");
    }
    
    boolean isDFAO = !m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$");

    String inFileName = m.group(GROUP_alphabet_OLD_NAME) + TXT_EXTENSION;

    List<NumberSystem> NS = new ArrayList<>();
    List<List<Integer>> alphabets = new ArrayList<>();
    determineAlphabetsAndNS(m, NS, alphabets);

    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));

    // here, call the function to set the number system.
    M.setAlphabet(isDFAO, NS, alphabets, printFlag, prefix, log);

    M.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), m.group(GROUP_alphabet_NEW_NAME), false);
    return new TestCase(M);
  }

  private static void determineAlphabetsAndNS(Matcher m, List<NumberSystem> NS, List<List<Integer>> alphabets) {
    Matcher m1 = PAT_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
    int counter = 1;
    while (m1.find()) {
      if ((m1.group(R_NUMBER_SYSTEM) != null)) {
        String base = determineBase(m1);
        NumberSystem ns = ProverHelper.getNumberSystem(base, NS, m.start(Prover.R_NUMBER_SYSTEM));
        alphabets.add(ns.getAlphabet());
      } else if (m1.group(R_SET) != null) {
        alphabets.add(ProverHelper.determineAlphabet(m1.group(R_SET)));
        NS.add(null);
      } else {
        throw new WalnutException("Alphabet at position " + counter + " not recognized in alphabet command");
      }
      counter += 1;
    }
  }

  public static String determineBase(Matcher m1) {
    String base = MSD_2;
    if (m1.group(3) != null) base = m1.group(3);
    if (m1.group(6) != null) base = m1.group(7) + "_" + m1.group(8);
    if (m1.group(9) != null) base = m1.group(9) + "_2";
    if (m1.group(10) != null) base = MSD_UNDERSCORE + m1.group(10);
    return base;
  }

  public TestCase unionCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_union_CMD, s, UNION);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_union_CMD.matcher(m.group(GROUP_UNION_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new WalnutException("Union requires at least one automaton as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, UNION, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_UNION_NAME), true);
    return new TestCase(C);
  }

  public TestCase intersectCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_intersect_CMD, s, INTERSECT);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_intersect_CMD.matcher(m.group(GROUP_INTERSECT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new WalnutException("Intersect requires at least one automaton as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, INTERSECT, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_INTERSECT_NAME), true);
    return new TestCase(C);
  }


  public TestCase starCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_star_CMD, s, STAR);

    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_STAR_OLD_NAME) + TXT_EXTENSION));

    Automaton C = M.star(printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_STAR_NEW_NAME), false);
    return new TestCase(C);
  }

  public TestCase concatCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_concat_CMD, s, CONCAT);
    
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_concat_CMD.matcher(m.group(GROUP_CONCAT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.size() < 2) {
      throw new WalnutException("Concatenation requires at least two automata as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.concat(automataNames, printFlag, prefix, log);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_CONCAT_NAME), true);
    return new TestCase(C);
  }

  public TestCase rightquoCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_rightquo_CMD, s, RIGHTQUO);
    Automaton M1 = Automaton.readAutomatonFromFile(m.group(GROUP_rightquo_OLD_NAME1));
    Automaton M2 = Automaton.readAutomatonFromFile(m.group(GROUP_rightquo_OLD_NAME2));
    Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false, printFlag, prefix, log);
    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_rightquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public TestCase leftquoCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_leftquo_CMD, s, LEFTQUO);
    Automaton M1 = Automaton.readAutomatonFromFile(m.group(GROUP_leftquo_OLD_NAME1));
    Automaton M2 = Automaton.readAutomatonFromFile(m.group(GROUP_leftquo_OLD_NAME2));
    Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2, printFlag, prefix, log);
    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_leftquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public static TestCase exportCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_export_CMD, s, EXPORT);
    String filename = m.group(GROUP_export_NAME);
    String inFileName = filename + TXT_EXTENSION;
    String exportType = m.group(GROUP_export_TYPE);
    boolean isDFAO = !m.group(GROUP_export_DOLLAR_SIGN).equals("$");
    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));
    ProverHelper.exportAutomata(s, filename, exportType, M, isDFAO);
    return new TestCase(M);
  }
}