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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.*;
import Automata.Morphism;
import Main.Commands.*;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 */
public class Prover {
  static final String RE_FOR_THE_LIST_OF_CMDS = "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|describe|export|help)";
  static final String RE_START = "^";
  // Basic identifier: used for free variables, combine, etc.
  public static final String RE_IDENTIFIER = "[a-zA-Z]\\w*";
  public static final String RE_WORD_OF_CMD_NO_SPC = "(" + RE_IDENTIFIER + ")";
  static final String RE_WORD_OF_CMD = "\\s+" + RE_WORD_OF_CMD_NO_SPC;
  // Optional "=<int>"
  public static final String RE_EQ_INT_OPTIONAL = "(=-?\\d+)?";

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
  public static final String FIRST_OP = "first";
  public static final String IF_OTHER_OP = "if_other";

  /**
   * group for filename in RE_FOR_load_CMD
   */
  static int L_FILENAME = 1;
  static final String RE_FOR_load_CMD = RE_START + LOAD + "\\s+(\\w+\\.txt)";
  static final Pattern PAT_FOR_load_CMD = Pattern.compile(RE_FOR_load_CMD);

  // eval/def [<name> [<space-separated variables for matrix>]] "<predicate>"
  static final String RE_FOR_eval_def_CMDS =
      RE_START + "(eval|def)(?:\\s+(" + RE_IDENTIFIER + ")((?:\\s+" + RE_IDENTIFIER + ")*))?\\s+\"(.*)\"";
  /**
   * important groups in RE_FOR_eval_def_CMDS
   */
  static int ED_NAME = 2, ED_FREE_VARIABLES = 3, ED_PREDICATE = 4;
  static final Pattern PAT_FOR_eval_def_CMDS = Pattern.compile(RE_FOR_eval_def_CMDS);

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
  public static final Pattern PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET = Pattern.compile(RE_FOR_A_SINGLE_ELEMENT_OF_A_SET);

  public static final int R_NUMBER_SYSTEM = 2, R_SET = 11;

  public static final String OST = "ost";
  static final String RE_FOR_ost_CMD = RE_START + OST + RE_WORD_OF_CMD + "\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*\\[\\s*((\\d+\\s*)*)\\]$";
  static final Pattern PAT_FOR_ost_CMD = Pattern.compile(RE_FOR_ost_CMD);
  static final int GROUP_OST_NAME = 1, GROUP_OST_PREPERIOD = 2, GROUP_OST_PERIOD = 4;

  public static final String COMBINE = "combine";
  static final String RE_FOR_combine_CMD =
      RE_START + COMBINE + RE_WORD_OF_CMD + "((\\s+(" +RE_IDENTIFIER + RE_EQ_INT_OPTIONAL + "))*)";
  static final Pattern PAT_FOR_combine_CMD = Pattern.compile(RE_FOR_combine_CMD);
  static final int GROUP_COMBINE_NAME = 1, GROUP_COMBINE_AUTOMATA = 2;

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

  public static final String TEST = "test";
  static final String RE_FOR_test_CMD = RE_START + TEST + RE_WORD_OF_CMD + "\\s*(\\d+)";
  static final Pattern PAT_FOR_test_CMD = Pattern.compile(RE_FOR_test_CMD);
  static final int GROUP_TEST_NAME = 1, GROUP_TEST_NUM = 2;

  public static final String TRANSDUCE = "transduce";
  private static final String DOLLAR = "\\s+(\\$|\\s*)";
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

  static final String RE_FOR_alphabet_CMD = RE_START + "(" + ALPHABET + ")" + RE_WORD_OF_CMD +
      "\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_alphabet_CMD = Pattern.compile(RE_FOR_alphabet_CMD);
  static final int GROUP_alphabet_NEW_NAME = 2, GROUP_alphabet_DOLLAR_SIGN = 20, GROUP_alphabet_OLD_NAME = 21;

  public static final String UNION = "union";
  static final String RE_FOR_union_CMD = RE_START + UNION + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_union_CMD = Pattern.compile(RE_FOR_union_CMD);
  static final int GROUP_UNION_NAME = 1, GROUP_UNION_AUTOMATA = 2;

  public static final String INTERSECT = "intersect";
  static final String RE_FOR_intersect_CMD = RE_START + INTERSECT + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_intersect_CMD = Pattern.compile(RE_FOR_intersect_CMD);
  static final int GROUP_INTERSECT_NAME = 1, GROUP_INTERSECT_AUTOMATA = 2;

  public static final String STAR = "star";
  static final String RE_FOR_star_CMD = RE_START + STAR + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_star_CMD = Pattern.compile(RE_FOR_star_CMD);
  static final int GROUP_STAR_NEW_NAME = 1, GROUP_STAR_OLD_NAME = 2;

  public static final String CONCAT = "concat";
  static final String RE_FOR_concat_CMD = RE_START + CONCAT + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)";
  static final Pattern PAT_FOR_concat_CMD = Pattern.compile(RE_FOR_concat_CMD);
  static final int GROUP_CONCAT_NAME = 1, GROUP_CONCAT_AUTOMATA = 2;

  public static final String RIGHTQUO = "rightquo";
  static final String RE_FOR_rightquo_CMD = RE_START + RIGHTQUO + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_rightquo_CMD = Pattern.compile(RE_FOR_rightquo_CMD);
  static final int GROUP_quo_NEW_NAME = 1, GROUP_quo_OLD_NAME1 = 2, GROUP_quo_OLD_NAME2 = 3;

  public static final String LEFTQUO = "leftquo";
  static final String RE_FOR_leftquo_CMD = RE_START + LEFTQUO + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_leftquo_CMD = Pattern.compile(RE_FOR_leftquo_CMD);

  // Meta-commands: [...] at the beginning of the command
  static final Pattern PAT_META_CMD = Pattern.compile("^\\[([^]]*)](.*)$");
  static final int GROUP_META_CMD = 1, GROUP_FINAL_CMD = 2;

  static final String STRATEGY = "strategy";
  static final String EXPORT = "export";
  static final String EARLY_EXIST_TERMINATION = "earlyExistTermination";

  // export <automata> <format>
  static final String RE_FOR_export_CMD = RE_START + EXPORT + DOLLAR + RE_WORD_OF_CMD_NO_SPC + RE_WORD_OF_CMD;
  static final Pattern PAT_FOR_export_CMD = Pattern.compile(RE_FOR_export_CMD);
  static final int GROUP_export_DOLLAR_SIGN = 1, GROUP_export_NAME = 2, GROUP_export_TYPE = 3;

  public static final String DESCRIBE = "describe";
  static final String RE_FOR_describe_CMD = RE_START + DESCRIBE + DOLLAR + RE_WORD_OF_CMD_NO_SPC;
  static final Pattern PAT_FOR_describe_CMD = Pattern.compile(RE_FOR_describe_CMD);
  static final int GROUP_describe_DOLLAR_SIGN = 1, GROUP_describe_NAME = 2;

  public MetaCommands metaCommands = new MetaCommands();

  public static Prover mainProver = new Prover();
  public boolean printDetails;
  public boolean printFlag;

  public static String currentEvalName; // current evaluation name, used for export metacommand
  public static boolean usingOTF = false; // whether the current command is using OTF algorithms
  public static boolean earlyExistTermination = false; // earlyExistTermination metacommand

  private static final String usageMessage = """
      Usage: walnut [OPTIONS] [<filename>]

      Walnut command-line interface.

      Positional arguments:
        <filename>          File of commands to execute (same effect as the `load`
                            command). If omitted, starts an interactive session.

      Options:
        --global-session    Use the old (Walnut 6 and earlier) global session behavior.
        --session-dir PATH  Use PATH instead of an auto-generated Session directory.
        --home-dir PATH     Use PATH instead of the current working directory.
        --help              Show this help message and exit.
      """;

  private static final String OTF_MESSAGE = """
      ---------------------------
      If the CCL(S) or BRZ-CCL(S) algorithms are used, please cite the paper:
      Nicol, John, and Markus Frohme. "Deconstructing Subset Construction: Reducing While Determinizing." International Conference on Tools and Algorithms for the Construction and Analysis of Systems. Cham: Springer Nature Switzerland, 2026.
      ---------------------------""";

  static final String homeDirArg = "--home-dir=";
  static final String sessionDirArg = "--session-dir=";
  private static final String globalSessionArg = "--global-session";
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
    boolean globalSession = false;

    for (String arg : args) {
      if (arg.startsWith("--help") || arg.equals("-h")) {
        System.out.println(usageMessage);
        System.exit(0);
      }
      if (arg.startsWith(sessionDirArg)) {
        sessionDir = arg.substring(sessionDirArg.length());
        if (!sessionDir.endsWith("/")) {
          sessionDir += "/";
        }
      } else if (arg.startsWith(homeDirArg)) {
        homeDir = arg.substring(homeDirArg.length());
        if (!homeDir.endsWith("/")) {
          homeDir += "/";
        }
      } else if (arg.equals(globalSessionArg)) {
        globalSession = true;
      } else if (filename == null) {
        filename = arg; // Assume the first non-flag argument is the filename
        UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(filename));
      }
    }
    Session.setPathsAndNames(sessionDir, homeDir, globalSession);
    return filename;
  }
  public static void run(String filename) {
    if (filename != null) {
      File f = UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(filename));
      // read commands from file
      try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
        if (!mainProver.readBuffer(in, false)) return;
      } catch (IOException e) {
        Logging.printTruncatedStackTrace(e);
      }
    }

    // Parse commands from the console.
    System.out.println("Welcome to Walnut v" + Session.WALNUT_VERSION +
        "! Type \"help;\" to see all available commands.");
    if (Session.globalSession) {
      System.out.println("Using global Walnut session.");
    } else {
      System.out.println("Starting Walnut session: " + Session.getName());
    }
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      mainProver.readBuffer(in, true);
    } catch (Exception e) {
      Logging.printTruncatedStackTrace(e);
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
          Logging.printTruncatedStackTrace(e);
        }
      }
    } catch (IOException e) {
      Logging.printTruncatedStackTrace(e);
    }

    return true;
  }

  public boolean dispatch(String s) throws IOException {
    String originalCommand = s;
    s = parseSetup(s);
    if (s.isEmpty()) {
      return true;
    }
    Logging.logCommand(originalCommand);

    Matcher matcher_for_command = PAT_FOR_CMD.matcher(s);
    if (!matcher_for_command.find()) {
      throw WalnutException.invalidCommand(s);
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
      Logging.logAndPrint(OTF_MESSAGE);
    }
    return exitVal;
  }

  private String parseSetup(String s) {
    metaCommands = new MetaCommands();
    printDetails = printFlag = false; // reset flags

    if (!s.endsWith(";") && !s.endsWith(":")) {
      throw WalnutException.invalidCommand(s);
    }
    int endingToRemove = 1;
    if (s.endsWith(":")) {
      printFlag = true;
      if (s.endsWith("::")) {
        endingToRemove++;
        printDetails = true;
      }
    }
    s = s.substring(0, s.length() - endingToRemove); // remove ;|:|::
    s = s.strip(); // remove end whitespace, Unicode-aware

    s = metaCommands.parseMetaCommands(s, printDetails);
    Logging.configureForCommand(printFlag, printDetails);

    return s;
  }

  public TestCase dispatchForIntegrationTest(String s, String msg) throws IOException {
    s = s.strip(); // remove start and end whitespace, Unicode-aware
    String originalCommand = s;
    s = parseSetup(s);

    if (s.isEmpty() || s.startsWith("#")) {
      return null;
    }
    Logging.logCommand(originalCommand);

    Matcher matcher_for_command = PAT_FOR_CMD.matcher(s);
    if (!matcher_for_command.find()) throw WalnutException.invalidCommand(s);

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
      Logging.printTruncatedStackTrace(e);
    }
    return true;
  }

  public TestCase evalDefCommands(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_eval_def_CMDS, s, "eval/def");
    currentEvalName = m.group(ED_NAME); // null in headless mode; used for export metacommand
    return EvalDef.evalDefCommand(printFlag, printDetails,
        m.group(ED_PREDICATE), currentEvalName, m.group(Prover.ED_FREE_VARIABLES));
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
    return Reg.reg(m.group(R_LIST_OF_ALPHABETS), m.group(R_REGEXP), m.group(R_NAME));
  }

  public TestCase combineCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_combine_CMD, s, COMBINE);
    return Combine.combineCommand(s, m.group(GROUP_COMBINE_AUTOMATA),  m.group(Prover.GROUP_COMBINE_NAME));
  }

  public TestCase describeCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_describe_CMD, s, DESCRIBE);
    return Describe.describe(
        !m.group(GROUP_describe_DOLLAR_SIGN).equals("$"),
        m.group(GROUP_describe_NAME) + TXT_EXTENSION);
  }

  public static void morphismCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_morphism_CMD, s, MORPHISM);
    Main.Commands.Morphism.morphismCommand(m.group(GROUP_MORPHISM_DEFINITION),  m.group(GROUP_MORPHISM_NAME));
  }

  public static TestCase promoteCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_promote_CMD, s, PROMOTE);

    String morphismAddress =
        Session.getReadFileForMorphismLibrary(m.group(GROUP_PROMOTE_MORPHISM) + TXT_EXTENSION);
    String mapString =
        Files.readString(Paths.get(UtilityMethods.validateFile(morphismAddress).toURI()));

    Morphism h = new Morphism(mapString);
    Automaton P = h.toWordAutomaton();

    P.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_PROMOTE_NAME), true);
    return new TestCase(P);
  }

  public TestCase imageCommand(String s) throws IOException {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_image_CMD, s, IMAGE);
    return Image.image(
        s, m.group(GROUP_IMAGE_MORPHISM), m.group(GROUP_IMAGE_OLD_NAME), m.group(GROUP_IMAGE_NEW_NAME), printFlag);
  }

  public static boolean infCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_inf_CMD, s, INF);
    return ProverHelper.infFromAddress(m.group(GROUP_INF_NAME));
  }

  public TestCase splitCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_split_CMD, s, SPLIT);
    return Split.processSplitCommand(s, false,
        m.group(GROUP_SPLIT_AUTOMATA), m.group(GROUP_SPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_SPLIT_INPUT)));
  }

  public TestCase rsplitCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_rsplit_CMD, s, REVERSE_SPLIT);
    return Split.processSplitCommand(s, true,
        m.group(GROUP_RSPLIT_AUTOMATA), m.group(GROUP_RSPLIT_NAME),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_RSPLIT_INPUT)));
  }

  public TestCase joinCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_join_CMD, s, JOIN);
    return Join.joinCommand(s, m.group(GROUP_JOIN_AUTOMATA), m.group(GROUP_JOIN_NAME));
  }


  public static boolean testCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_test_CMD, s, TEST);
    return Test.testCommand(m.group(GROUP_TEST_NAME), Integer.parseInt(m.group(GROUP_TEST_NUM)));
  }

  public static TestCase ostCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_ost_CMD, s, OST);
    return Ost.ostCommand(m.group(GROUP_OST_NAME), m.group(GROUP_OST_PREPERIOD), m.group(GROUP_OST_PERIOD));
  }

  public TestCase transduceCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_transduce_CMD, s, TRANSDUCE);
    
    Transducer T = new Transducer(Session.getTransducerFile(m.group(GROUP_TRANSDUCE_TRANSDUCER) + TXT_EXTENSION));
    String inFileName = m.group(GROUP_TRANSDUCE_OLD_NAME) + TXT_EXTENSION;
    boolean isDFAO = !(m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$"));
    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));

    Automaton C = T.transduceNonDeterministic(M);
    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_TRANSDUCE_NEW_NAME), true);
    return new TestCase(C);
  }

  public TestCase reverseCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_reverse_CMD, s, REVERSE);
    return Reverse.reverseCommand(s, m.group(GROUP_REVERSE_OLD_NAME) + TXT_EXTENSION,
        !m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$"), m.group(GROUP_REVERSE_NEW_NAME));
  }

  public TestCase minimizeCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_minimize_CMD, s, MINIMIZE);
    
    Automaton M = new Automaton(
        Session.getReadFileForWordsLibrary(m.group(GROUP_MINIMIZE_OLD_NAME) + TXT_EXTENSION));

    WordAutomaton.minimizeSelfWithOutput(M);

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
        Integer.parseInt(m.group(GROUP_CONVERT_BASE))
    );

    M.writeAutomata(s, ProverHelper.determineOutLibrary(newIsDFAO), m.group(GROUP_CONVERT_NEW_NAME), true);
    return new TestCase(M);
  }

  public TestCase fixLeadZeroCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_fixleadzero_CMD, s, FIXLEADZERO);
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXLEADZERO_OLD_NAME));
    AutomatonLogicalOps.fixLeadingZerosProblem(M);
    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXLEADZERO_NEW_NAME), false);
    return new TestCase(M);
  }


  public TestCase fixTrailZeroCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_fixtrailzero_CMD, s, FIXTRAILZERO);
    Automaton M = Automaton.readAutomatonFromFile(m.group(GROUP_FIXTRAILZERO_OLD_NAME));
    AutomatonLogicalOps.fixTrailingZerosProblem(M);
    M.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXTRAILZERO_NEW_NAME), false);
    return new TestCase(M);
  }

  public TestCase alphabetCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_alphabet_CMD, s, ALPHABET);
    return Alphabet.alphabetCommand(
        s, m.group(R_LIST_OF_ALPHABETS),
        !m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$"),
        m.group(GROUP_alphabet_OLD_NAME) + TXT_EXTENSION,
        m.group(GROUP_alphabet_NEW_NAME));
  }

  public TestCase unionCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_union_CMD, s, UNION);
    return Union.union(s, m.group(GROUP_UNION_AUTOMATA), m.group(GROUP_UNION_NAME));
  }

  public TestCase intersectCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_intersect_CMD, s, INTERSECT);
    return Intersect.intersect(s, m.group(GROUP_INTERSECT_AUTOMATA), m.group(GROUP_INTERSECT_NAME));
  }

  public TestCase starCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_star_CMD, s, STAR);
    return Star.star(s, m.group(GROUP_STAR_OLD_NAME), m.group(GROUP_STAR_NEW_NAME));
  }

  public TestCase concatCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_concat_CMD, s, CONCAT);
    return Concat.concat(s, m.group(GROUP_CONCAT_AUTOMATA), m.group(GROUP_CONCAT_NAME));
  }

  public TestCase rightquoCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_rightquo_CMD, s, RIGHTQUO);
    return Quotient.rightQuotient(
        s, m.group(GROUP_quo_OLD_NAME1), m.group(GROUP_quo_OLD_NAME2), m.group(GROUP_quo_NEW_NAME));
  }

  public TestCase leftquoCommand(String s) {
    Matcher m = ProverHelper.matchOrFail(PAT_FOR_leftquo_CMD, s, LEFTQUO);
    return Quotient.leftQuotient(
        s, m.group(GROUP_quo_OLD_NAME1), m.group(GROUP_quo_OLD_NAME2), m.group(GROUP_quo_NEW_NAME));
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