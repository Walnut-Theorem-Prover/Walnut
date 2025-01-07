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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.*;
import Automata.FA.DeterminizationStrategies;
import Automata.FA.FA;
import Automata.Numeration.Ostrowski;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 */
public class Prover {
  static String RE_FOR_THE_LIST_OF_CMDS = "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|draw|help)";
  static String RE_END_CMD = "(;|::|:)$";
  static String RE_START = "^";
  static String RE_WORD_OF_CMD_NO_SPC = "([a-zA-Z]\\w*)";

  static String RE_WORD_OF_CMD = "\\s+" + RE_WORD_OF_CMD_NO_SPC;

  static String RE_FOR_EMPTY_CMD = RE_START + RE_END_CMD;
  /**
   * the high-level scheme of a command is a name followed by some arguments and ending in either ; : or ::
   */
  static String RE_FOR_CMD = RE_START + "(\\w+)(\\s+.*)?" + RE_END_CMD;
  static Pattern PAT_FOR_CMD = Pattern.compile(RE_FOR_CMD);

  static String RE_FOR_exit_CMD = RE_START + "(exit|quit)\\s*(;|::|:)$";

  static String RE_FOR_load_CMD = RE_START + "load\\s+(\\w+\\.txt)\\s*" + RE_END_CMD;
  /**
   * group for filename in RE_FOR_load_CMD
   */
  static int L_FILENAME = 1;
  static Pattern PAT_FOR_load_CMD = Pattern.compile(RE_FOR_load_CMD);

  static String RE_FOR_eval_def_CMDS = RE_START + "(eval|def)" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)\\s+\"(.*)\"\\s*" + RE_END_CMD;
  /**
   * important groups in RE_FOR_eval_def_CMDS
   */
  static int ED_TYPE = 1, ED_NAME = 2, ED_FREE_VARIABLES = 3, ED_PREDICATE = 6, ED_ENDING = 7;
  static Pattern PAT_FOR_eval_def_CMDS = Pattern.compile(RE_FOR_eval_def_CMDS);
  static String REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = "[a-zA-Z]\\w*";
  static Pattern PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = Pattern.compile(REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS);

  static String RE_FOR_macro_CMD = RE_START + "macro" + RE_WORD_OF_CMD + "\\s+\"(.*)\"\\s*" + RE_END_CMD;
  static int M_NAME = 1, M_DEFINITION = 2;
  static Pattern PAT_FOR_macro_CMD = Pattern.compile(RE_FOR_macro_CMD);

  static String RE_FOR_reg_CMD = RE_START + "(reg)" + RE_WORD_OF_CMD + "\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)\"(.*)\"\\s*" + RE_END_CMD;

  /**
   * important groups in RE_FOR_reg_CMD
   */
  static int R_NAME = 2, R_LIST_OF_ALPHABETS = 3, R_REGEXP = 20;
  static Pattern PAT_FOR_reg_CMD = Pattern.compile(RE_FOR_reg_CMD);
  static String RE_FOR_A_SINGLE_ELEMENT_OF_A_SET = "(\\+|\\-)?\\s*\\d+";
  static Pattern PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET = Pattern.compile(RE_FOR_A_SINGLE_ELEMENT_OF_A_SET);
  static String RE_FOR_AN_ALPHABET = "((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+";
  static Pattern PAT_FOR_AN_ALPHABET = Pattern.compile(RE_FOR_AN_ALPHABET);
  static int R_NUMBER_SYSTEM = 2, R_SET = 11;

  static String RE_FOR_AN_ALPHABET_VECTOR = "(\\[(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\])|(\\d)";
  static Pattern PAT_FOR_AN_ALPHABET_VECTOR = Pattern.compile(RE_FOR_AN_ALPHABET_VECTOR);


  static String RE_FOR_ost_CMD = RE_START + "ost" + RE_WORD_OF_CMD + "\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*(;|:|::)\\s*$";
  static Pattern PAT_FOR_ost_CMD = Pattern.compile(RE_FOR_ost_CMD);
  static int GROUP_OST_NAME = 1;
  static int GROUP_OST_PREPERIOD = 2;
  static int GROUP_OST_PERIOD = 4;

  static String RE_FOR_combine_CMD = RE_START + "combine" + RE_WORD_OF_CMD + "((\\s+([a-zA-Z]\\w*(=-?\\d+)?))*)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_combine_CMD = Pattern.compile(RE_FOR_combine_CMD);
  static int GROUP_COMBINE_NAME = 1, GROUP_COMBINE_AUTOMATA = 2, GROUP_COMBINE_END = 6;
  static String RE_FOR_AN_AUTOMATON_IN_combine_CMD = RE_WORD_OF_CMD_NO_SPC + "((=-?\\d+)?)";
  static Pattern PAT_FOR_AN_AUTOMATON_IN_combine_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_combine_CMD);

  static String RE_FOR_morphism_CMD = RE_START + "morphism" + RE_WORD_OF_CMD + "\\s+\"(\\d+\\s*\\-\\>\\s*(.)*(,\\d+\\s*\\-\\>\\s*(.)*)*)\"\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_morphism_CMD = Pattern.compile(RE_FOR_morphism_CMD);
  static int GROUP_MORPHISM_NAME = 1, GROUP_MORPHISM_DEFINITION;

  static String RE_FOR_promote_CMD = RE_START + "promote" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_promote_CMD = Pattern.compile(RE_FOR_promote_CMD);
  static int GROUP_PROMOTE_NAME = 1, GROUP_PROMOTE_MORPHISM = 2;

  static String RE_FOR_image_CMD = RE_START + "image" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_image_CMD = Pattern.compile(RE_FOR_image_CMD);
  static int GROUP_IMAGE_NEW_NAME = 1, GROUP_IMAGE_MORPHISM = 2, GROUP_IMAGE_OLD_NAME = 3;

  static String RE_FOR_inf_CMD = RE_START + "inf" + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_inf_CMD = Pattern.compile(RE_FOR_inf_CMD);
  static int GROUP_INF_NAME = 1;

  static String RE_FOR_split_CMD = RE_START + "split" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "((\\s*\\[\\s*[+-]?\\s*])+)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_split_CMD = Pattern.compile(RE_FOR_split_CMD);
  static int GROUP_SPLIT_NAME = 1, GROUP_SPLIT_AUTOMATA = 2, GROUP_SPLIT_INPUT = 3, GROUP_SPLIT_END = 5;
  static String RE_FOR_INPUT_IN_split_CMD = "\\[\\s*([+-]?)\\s*]";
  static Pattern PAT_FOR_INPUT_IN_split_CMD = Pattern.compile(RE_FOR_INPUT_IN_split_CMD);

  static String RE_FOR_rsplit_CMD = RE_START + "rsplit" + RE_WORD_OF_CMD + "((\\s*\\[\\s*[+-]?\\s*])+)" + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_rsplit_CMD = Pattern.compile(RE_FOR_rsplit_CMD);
  static int GROUP_RSPLIT_NAME = 1, GROUP_RSPLIT_AUTOMATA = 4, GROUP_RSPLIT_INPUT = 2, GROUP_RSPLIT_END = 5;

  static String RE_FOR_join_CMD = RE_START + "join" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + "((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+))*)\\s*(;|::|:)\\s*";
  static Pattern PAT_FOR_join_CMD = Pattern.compile(RE_FOR_join_CMD);
  static int GROUP_JOIN_NAME = 1, GROUP_JOIN_AUTOMATA = 2, GROUP_JOIN_END = 7;
  static String RE_FOR_AN_AUTOMATON_IN_join_CMD = RE_WORD_OF_CMD_NO_SPC + "((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+)";
  static Pattern PAT_FOR_AN_AUTOMATON_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_join_CMD);
  static int GROUP_JOIN_AUTOMATON_NAME = 1, GROUP_JOIN_AUTOMATON_INPUT = 2;
  static String RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = "\\[\\s*([a-zA-Z&&[^AE]]\\w*)\\s*]";
  static Pattern PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD);

  static String RE_FOR_test_CMD = RE_START + "test" + RE_WORD_OF_CMD + "\\s*(\\d+)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_test_CMD = Pattern.compile(RE_FOR_test_CMD);
  static int GROUP_TEST_NAME = 1, GROUP_TEST_NUM = 2;

  static String RE_FOR_transduce_CMD = RE_START + "transduce" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_transduce_CMD = Pattern.compile(RE_FOR_transduce_CMD);
  static int GROUP_TRANSDUCE_NEW_NAME = 1, GROUP_TRANSDUCE_TRANSDUCER = 2,
      GROUP_TRANSDUCE_DOLLAR_SIGN = 3, GROUP_TRANSDUCE_OLD_NAME = 4, GROUP_TRANSDUCE_END = 5;

  static String RE_FOR_reverse_CMD = RE_START + "reverse" + RE_WORD_OF_CMD + "\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_reverse_CMD = Pattern.compile(RE_FOR_reverse_CMD);
  static int GROUP_REVERSE_NEW_NAME = 1, GROUP_REVERSE_DOLLAR_SIGN = 2, GROUP_REVERSE_OLD_NAME = 3, GROUP_REVERSE_END = 4;

  static String RE_FOR_minimize_CMD = RE_START + "minimize" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_minimize_CMD = Pattern.compile(RE_FOR_minimize_CMD);
  static int GROUP_MINIMIZE_NEW_NAME = 1, GROUP_MINIMIZE_OLD_NAME = 2, GROUP_MINIMIZE_END = 3;

  static String RE_FOR_convert_CMD = RE_START + "convert\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s+((msd|lsd)_(\\d+))\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_convert_CMD = Pattern.compile(RE_FOR_convert_CMD);
  static int GROUP_CONVERT_NEW_NAME = 2, GROUP_CONVERT_OLD_NAME = 7, GROUP_CONVERT_END = 8,
      GROUP_CONVERT_NEW_DOLLAR_SIGN = 1, GROUP_CONVERT_OLD_DOLLAR_SIGN = 6,
      GROUP_CONVERT_MSD_OR_LSD = 4,
      GROUP_CONVERT_BASE = 5;

  static String RE_FOR_fixleadzero_CMD = RE_START + "fixleadzero" + RE_WORD_OF_CMD + "\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_fixleadzero_CMD = Pattern.compile(RE_FOR_fixleadzero_CMD);
  static int GROUP_FIXLEADZERO_NEW_NAME = 1, GROUP_FIXLEADZERO_OLD_NAME = 3, GROUP_FIXLEADZERO_END = 4;

  static String RE_FOR_fixtrailzero_CMD = RE_START + "fixtrailzero" + RE_WORD_OF_CMD + "\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_fixtrailzero_CMD = Pattern.compile(RE_FOR_fixtrailzero_CMD);
  static int GROUP_FIXTRAILZERO_NEW_NAME = 1, GROUP_FIXTRAILZERO_OLD_NAME = 3, GROUP_FIXTRAILZERO_END = 4;

  static String RE_FOR_alphabet_CMD = RE_START + "(alphabet)" + RE_WORD_OF_CMD + "\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_alphabet_CMD = Pattern.compile(RE_FOR_alphabet_CMD);
  static int GROUP_alphabet_NEW_NAME = 2, GROUP_alphabet_LIST_OF_ALPHABETS = 3, GROUP_alphabet_DOLLAR_SIGN = 20, GROUP_alphabet_OLD_NAME = 21, GROUP_alphabet_END = 22;

  static String RE_FOR_union_CMD = RE_START + "union" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_union_CMD = Pattern.compile(RE_FOR_union_CMD);
  static int GROUP_UNION_NAME = 1, GROUP_UNION_AUTOMATA = 2, GROUP_UNION_END = 5;
  static String RE_FOR_AN_AUTOMATON_IN_union_CMD = RE_WORD_OF_CMD_NO_SPC;
  static Pattern PAT_FOR_AN_AUTOMATON_IN_union_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_union_CMD);

  static String RE_FOR_intersect_CMD = RE_START + "intersect" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_intersect_CMD = Pattern.compile(RE_FOR_intersect_CMD);
  static int GROUP_INTERSECT_NAME = 1, GROUP_INTERSECT_AUTOMATA = 2, GROUP_INTERSECT_END = 5;
  static String RE_FOR_AN_AUTOMATON_IN_intersect_CMD = RE_WORD_OF_CMD_NO_SPC;
  static Pattern PAT_FOR_AN_AUTOMATON_IN_intersect_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_intersect_CMD);

  static String RE_FOR_star_CMD = RE_START + "star" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_star_CMD = Pattern.compile(RE_FOR_star_CMD);
  static int GROUP_STAR_NEW_NAME = 1, GROUP_STAR_OLD_NAME = 2, GROUP_STAR_END = 3;

  static String RE_FOR_concat_CMD = RE_START + "concat" + RE_WORD_OF_CMD + "((" + RE_WORD_OF_CMD + ")*)\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_concat_CMD = Pattern.compile(RE_FOR_concat_CMD);
  static int GROUP_CONCAT_NAME = 1, GROUP_CONCAT_AUTOMATA = 2, GROUP_CONCAT_END = 5;
  static String RE_FOR_AN_AUTOMATON_IN_concat_CMD = RE_WORD_OF_CMD_NO_SPC;
  static Pattern PAT_FOR_AN_AUTOMATON_IN_concat_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_concat_CMD);

  static String RE_FOR_rightquo_CMD = RE_START + "rightquo" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_rightquo_CMD = Pattern.compile(RE_FOR_rightquo_CMD);
  static int GROUP_rightquo_NEW_NAME = 1, GROUP_rightquo_OLD_NAME1 = 2, GROUP_rightquo_OLD_NAME2 = 3, GROUP_rightquo_END = 4;

  static String RE_FOR_leftquo_CMD = RE_START + "leftquo" + RE_WORD_OF_CMD + RE_WORD_OF_CMD + RE_WORD_OF_CMD + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_leftquo_CMD = Pattern.compile(RE_FOR_leftquo_CMD);
  static int GROUP_leftquo_NEW_NAME = 1, GROUP_leftquo_OLD_NAME1 = 2, GROUP_leftquo_OLD_NAME2 = 3, GROUP_leftquo_END = 4;

  static String RE_FOR_draw_CMD = RE_START + "draw\\s+(\\$|\\s*)" + RE_WORD_OF_CMD_NO_SPC + "\\s*" + RE_END_CMD;
  static Pattern PAT_FOR_draw_CMD = Pattern.compile(RE_FOR_draw_CMD);
  static int GROUP_draw_DOLLAR_SIGN = 1, GROUP_draw_NAME = 2;

  static String STRATEGY_CMD = "^\\[(?:([A-Za-z]+)\\s+(\\d+)(?:,\\s*)?)*\\]";
  static Pattern PAT_STRATEGY = Pattern.compile(STRATEGY_CMD);

  public static String prefix = ""; // Declare here instead of passing around everywhere
  public static StringBuilder log = new StringBuilder(); // Declare here instead of passing around everywhere

  /**
   * if the command line argument is not empty, we treat args[0] as a filename.
   * if this is the case, we read from the file and load its commands before we submit control to user.
   * if the address is not a valid address or the file does not exist, we print an appropriate error message
   * and submit control to the user.
   * if the file contains the exit command we terminate the program.
   **/
  public static void main(String[] args) {
    Session.setPathsAndNames();
    run(args);
  }

  public static void run(String[] args) {
    if (args.length >= 1) {
      File f = UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(args[0]));
      //reading commands from the file with address args[0]
      try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
        if (!readBuffer(in, false)) return;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Now we parse commands from the console.
    System.out.println("Welcome to Walnut v" + Session.WALNUT_VERSION +
        "! Type \"help;\" to see all available commands.");
    System.out.println("Starting Walnut session: " + Session.getName());
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      readBuffer(in, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Takes a BufferedReader and reads from it until we hit end of file or exit command.
   * @param console = true if in = System.in
   */
  public static boolean readBuffer(BufferedReader in, boolean console) {
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

        int index = determineIndex(s);

        if (index != -1) {
          s = s.substring(0, index + 1);
          buffer.append(s);
          s = buffer.toString();
          if (!console) {
            System.out.println(s);
          }

          try {
            if (!dispatch(s)) {
              return false;
            }
          } catch (RuntimeException e) {
            e.printStackTrace();
          }

          buffer = new StringBuilder();
        } else {
          buffer.append(s);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return true;
  }

  /**
   * Determines the index of the first delimiter (';' or ':') in the given string.
   * If both are present, the smaller index is returned. If only one is present, its index is returned.
   * If no delimiters are found, -1 is returned.
   * Additionally, if the character following the found index is a colon (':'),
   * the index is incremented to include it.
   *
   * @param s the input string to search.
   * @return the index of the first delimiter or -1 if no delimiters are found.
   * If the character following the found index is a colon, the index
   * is incremented by one.
   */
  private static int determineIndex(String s) {
    int index1 = s.indexOf(';');
    int index2 = s.indexOf(':');
    int index;
    if (index1 != -1 && index2 != -1) {
      index = Math.min(index1, index2);
    } else if (index1 != -1) {
      index = index1;
    } else {
      index = index2;
    }

    if ((s.length() - 1) > index && s.charAt(index + 1) == ':') {
      index++;
    }
    return index;
  }

  public static boolean dispatch(String s) throws IOException {
    s = parseSetup(s);
    if (s.matches(RE_FOR_EMPTY_CMD)) {
      // If the command is just ; or : do nothing.
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
      case "exit", "quit" -> {
        if (s.matches(RE_FOR_exit_CMD)) {
          return false;
        }
        throw ExceptionHelper.invalidCommand();
      }
      case "load" -> {
        if (!loadCommand(s)) return false;
      }
    }
    processCommand(s, commandName);
    return true;
  }

  private static String parseSetup(String s) {
    FA.resetIndex();
    prefix = ""; // reset prefix
    log = new StringBuilder(); // reset log

    s = s.trim(); // remove start and end whitespace

    Matcher m = PAT_STRATEGY.matcher(s);
    if (m.find()) {
      // Extract the full matched strategy
      String strategy = m.group();
      // Remove brackets and split into individual pairs
      String content = strategy.substring(1, strategy.length() - 1); // Remove '[' and ']'
      List<ObjectIntPair<String>> pairs = new ArrayList<>();

      // Split by commas and process each key-value pair
      String[] entries = content.split("\\s*,\\s*"); // Split by ", " or ","
      for (String entry : entries) {
        String[] keyValue = entry.split("\\s+"); // Split by space between key and value
        String key = keyValue[0];
        int value = Integer.parseInt(keyValue[1]);

        DeterminizationStrategies.getStrategyMap().put(value, DeterminizationStrategies.Strategy.valueOf(key));
      }

      System.out.println("Parsed strategy: " + DeterminizationStrategies.getStrategyMap());

      // Remove the matched strategy from the string
      s = m.replaceFirst("").trim();
    }
    return s;
  }

  public static TestCase dispatchForIntegrationTest(String s, String msg) throws IOException {
    s = parseSetup(s);

    System.out.println("Running integration test: " + msg);
    if (s.matches(RE_FOR_EMPTY_CMD)) {
      //if the command is just ; or : do nothing
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

  private static TestCase processCommand(String s, String commandName) throws IOException {
    switch (commandName) {
      case "exit", "quit" -> {
        if (s.matches(RE_FOR_exit_CMD)) return null;
        throw ExceptionHelper.invalidCommand();
      }
      case "load" -> {
        if (!loadCommand(s)) return null;
      }
      case "eval", "def" -> {
        return eval_def_commands(s);
      }
      case "macro" -> {
        return macroCommand(s);
      }
      case "reg" -> {
        return regCommand(s);
      }
      case "ost" -> {
        return ostCommand(s);
      }
      case "cls", "clear" -> {
        clearScreen();
        return null;
      }
      case "combine" -> {
        return combineCommand(s);
      }
      case "morphism" -> {
        morphismCommand(s);
      }
      case "promote" -> {
        return promoteCommand(s);
      }
      case "image" -> {
        return imageCommand(s);
      }
      case "inf" -> {
        infCommand(s);
      }
      case "split" -> {
        return splitCommand(s);
      }
      case "rsplit" -> {
        return rsplitCommand(s);
      }
      case "join" -> {
        return joinCommand(s);
      }
      case "test" -> {
        testCommand(s);
      }
      case "transduce" -> {
        return transduceCommand(s);
      }
      case "reverse" -> {
        return reverseCommand(s);
      }
      case "minimize" -> {
        return minimizeCommand(s);
      }
      case "convert" -> {
        return convertCommand(s);
      }
      case "fixleadzero" -> {
        return fixLeadZeroCommand(s);
      }
      case "fixtrailzero" -> {
        return fixTrailZeroCommand(s);
      }
      case "alphabet" -> {
        return alphabetCommand(s);
      }
      case "union" -> {
        return unionCommand(s);
      }
      case "intersect" -> {
        return intersectCommand(s);
      }
      case "star" -> {
        return starCommand(s);
      }
      case "concat" -> {
        return concatCommand(s);
      }
      case "rightquo" -> {
        return rightquoCommand(s);
      }
      case "leftquo" -> {
        return leftquoCommand(s);
      }
      case "draw" -> {
        return drawCommand(s);
      }
      case "help" -> HelpMessages.helpCommand(s);
      default -> throw ExceptionHelper.invalidCommand(commandName);
    }
    return null;
  }

  /**
   * load x.p; loads commands from the file x.p. The file can contain any command except for load x.p;
   * The user don't get a warning if the x.p contains load x.p but the program might end up in an infinite loop.
   * Note that the file can contain load y.p; whenever y != x and y exist.
   */
  public static boolean loadCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_load_CMD, s, "load");

    File f = UtilityMethods.validateFile(Session.getReadAddressForCommandFiles(m.group(L_FILENAME)));

    try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
      if (!readBuffer(in, false)) {
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }

  public static TestCase eval_def_commands(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_eval_def_CMDS, s, "eval/def");

    Automaton M;
    List<String> free_variables = new ArrayList<>();
    String freeVarString = m.group(ED_FREE_VARIABLES);
    if (freeVarString != null) {
      Matcher m1 = PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS.matcher(freeVarString);
      while (m1.find()) {
        String t = m1.group();
        free_variables.add(t);
      }
    }

    boolean printSteps = m.group(ED_ENDING).equals(":");
    boolean printDetails = m.group(ED_ENDING).equals("::");

    Computer c = new Computer(m.group(ED_PREDICATE), printSteps, printDetails);
    String resultName = Session.getAddressForResult() + m.group(ED_NAME);
    AutomatonWriter.write(c.result.M, resultName + ".txt");
    AutomatonWriter.draw(
        c.result.M, resultName + ".gv", c.predicateString, false);

    if (!free_variables.isEmpty()) {
      c.mpl = AutomatonWriter.writeMatrices(c.result.M,
          resultName + ".mpl", free_variables);
    }

    c.writeLogs(resultName, c, printDetails);

    // We do this for both eval and def -- they're now the same command
    AutomatonWriter.write(
        c.result.M, Session.getWriteAddressForAutomataLibrary() + m.group(ED_NAME) + ".txt");

    M = c.result.M;
    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      System.out.println("____\n" + (M.fa.isTRUE_AUTOMATON() ? "TRUE" : "FALSE"));
    }

    return new TestCase(M, "", c.mpl, printDetails ? c.logDetails.toString() : "");
  }


  public static TestCase macroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_macro_CMD, s, "macro");
    File f = new File(Session.getWriteAddressForMacroLibrary() + m.group(M_NAME) + ".txt");
    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
      out.write(m.group(M_DEFINITION));
    } catch (IOException o) {
      System.out.println("Could not write the macro " + m.group(M_NAME));
    }
    return null;
  }

  public static TestCase regCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_reg_CMD, s, "reg");

    List<List<Integer>> alphabets = new ArrayList<>();
    List<NumberSystem> numSys = new ArrayList<>();
    List<Integer> alphabet;
    if (m.group(R_LIST_OF_ALPHABETS) == null) {
      String base = "msd_2";
      NumberSystem ns = getNumberSystem(base, numSys, m);
      alphabets.add(ns.getAlphabet());
    }
    Matcher m1 = PAT_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
    while (m1.find()) {
      if ((m1.group(R_NUMBER_SYSTEM) != null)) {
        String base = "msd_2";
        if (m1.group(3) != null) base = m1.group(3);
        if (m1.group(6) != null) base = m1.group(7) + "_" + m1.group(8);
        if (m1.group(9) != null) base = m1.group(9) + "_2";
        if (m1.group(10) != null) base = "msd_" + m1.group(10);
        NumberSystem ns = getNumberSystem(base, numSys, m);
        alphabets.add(ns.getAlphabet());
      } else if (m1.group(R_SET) != null) {
        alphabet = determineAlphabet(m1.group(R_SET));
        alphabets.add(alphabet);
        numSys.add(null);
      }
    }
    // To support regular expressions with multiple arity (eg. "[1,0][0,1][0,0]*"), we must translate each of these vectors to an
    // encoding, which will then be turned into a unicode character that dk.brics can work with when constructing an automaton
    // from a regular expression. Since the encoding method is within the Automaton class, we create a dummy instance and load it
    // with our sequence of number systems in order to access it. After the regex automaton is created, we set its alphabet to be the
    // one requested, instead of the unicode alphabet that dk.brics uses.
    Automaton M = new Automaton();
    M.setA(alphabets);
    String baseexp = m.group(R_REGEXP);

    Matcher m2 = PAT_FOR_AN_ALPHABET_VECTOR.matcher(baseexp);
    // if we haven't had to replace any input vectors with unicode, we use the legacy method of constructing the automaton
    while (m2.find()) {
      List<Integer> L = new ArrayList<>();
      String alphabetVector = m2.group();

      // needed to replace this string with the unicode mapping
      String alphabetVectorCopy = alphabetVector;
      if (alphabetVector.charAt(0) == '[') {
        alphabetVector = alphabetVector.substring(1, alphabetVector.length() - 1); // truncate brackets [ ]
      }

      Matcher m3 = PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(alphabetVector);
      while (m3.find()) {
        L.add(UtilityMethods.parseInt(m3.group()));
      }
      if (L.size() != M.getA().size()) {
        throw new RuntimeException("Mismatch between vector length in regex and specified number of inputs to automaton");
      }
      int vectorEncoding = M.encode(L);
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
    M.determineAlphabetSizeFromA();

    // We should always do this with replacement, since we may have regexes such as "...", which accepts any three characters
    // in a row, on an alphabet containing bracketed characters. We don't make any replacements here, but they are implicitly made
    // when we intersect with our alphabet(s).

    // remove all whitespace from regular expression.
    baseexp = baseexp.replaceAll("\\s", "");

    Automaton R = new Automaton(baseexp, M.getAlphabetSize());
    R.setA(M.getA());
    R.determineAlphabetSizeFromA();
    R.setNS(numSys);

    writeAutomata(m.group(R_REGEXP), R, Session.getWriteAddressForAutomataLibrary(), m.group(R_NAME), false);

    return new TestCase(R);
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

  public static TestCase combineCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_combine_CMD, s, "combine");

    boolean printSteps = m.group(GROUP_COMBINE_END).equals(":");

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
    Automaton first = new Automaton(Session.getReadFileForAutomataLibrary(automataNames.get(0) + ".txt"));
    automataNames.remove(0);

    Automaton C = first.combine(automataNames, outputs, printSteps, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_COMBINE_NAME), true);

    return new TestCase(C);
  }


  public static void morphismCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_morphism_CMD, s, "morphism");

    String name = m.group(GROUP_MORPHISM_NAME);

    Morphism M = new Morphism();
    M.parseMap(m.group(GROUP_MORPHISM_DEFINITION));
    System.out.print("Defined with domain ");
    System.out.print(M.mapping.keySet());
    System.out.print(" and range ");
    System.out.print(M.range);
    M.write(Session.getAddressForResult() + name + ".txt");
    M.write(Session.getWriteAddressForMorphismLibrary() + name + ".txt");
  }

  public static TestCase promoteCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_promote_CMD, s, "promote");

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_PROMOTE_MORPHISM) + ".txt"));
    Automaton P = h.toWordAutomaton();
    writeAutomata(s, P, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_PROMOTE_NAME), true);

    return new TestCase(P);
  }

  public static TestCase imageCommand(String s) throws IOException {
    Matcher m = matchOrFail(PAT_FOR_image_CMD, s, "image");

    Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_IMAGE_MORPHISM) + ".txt"));
    if (!h.isUniform()) {
      throw new RuntimeException("A morphism applied to a word automaton must be uniform.");
    }
    StringBuilder combineString = new StringBuilder("combine " + m.group(GROUP_IMAGE_NEW_NAME));

    // We need to know the number system of our old automaton: the new one should match, as should intermediary expressions
    Automaton M = new Automaton(Session.getReadFileForWordsLibrary(m.group(GROUP_IMAGE_OLD_NAME) + ".txt"));
    String numSysName = "";
    if (!M.getNS().isEmpty()) {
      numSysName = M.getNS().get(0).toString();
    }

    // we construct a define command for a DFA for each x that accepts iff x appears at the nth position
    for (Integer value : h.range) {
      eval_def_commands(h.makeInterCommand(value, m.group(GROUP_IMAGE_OLD_NAME), numSysName));
      combineString.append(" ").append(m.group(GROUP_IMAGE_OLD_NAME)).append("_").append(value).append("=").append(value);
    }
    combineString.append(":");

    TestCase retrieval = combineCommand(combineString.toString());
    Automaton I = retrieval.getResult().clone();

    writeAutomata(s, I, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_IMAGE_NEW_NAME), true);

    return new TestCase(I);
  }

  public static boolean infCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_inf_CMD, s, "inf");

    Automaton M = new Automaton(Session.getReadFileForAutomataLibrary(m.group(GROUP_INF_NAME) + ".txt"));
    M = removeLeadTrailZeroes(M);
    String infReg = M.infinite();
    if (infReg.isEmpty()) {
      System.out.println("Automaton " + m.group(GROUP_INF_NAME) + " accepts finitely many values.");
      return false;
    } else {
      System.out.println(infReg);
      return true;
    }
  }

  public static TestCase processSplitCommand(
      String s, boolean isReverse,
      String automatonName, String name, String end, Matcher inputPattern) {

    String addressForWordAutomaton =
        Session.getReadFileForWordsLibrary(automatonName + ".txt");
    String addressForAutomaton =
        Session.getReadFileForAutomataLibrary(automatonName + ".txt");

    Automaton M;
    boolean isDFAO;
    if ((new File(addressForWordAutomaton)).exists()) {
      M = new Automaton(addressForWordAutomaton);
      isDFAO = true;
    } else if ((new File(addressForAutomaton)).exists()) {
      M = new Automaton(addressForAutomaton);
      isDFAO = false;
    } else {
      throw new RuntimeException("Automaton " + automatonName + " does not exist.");
    }

    boolean printSteps = end.equals(":");

    List<String> inputs = new ArrayList<>();
    boolean hasInput = false;
    while (inputPattern.find()) {
      String t = inputPattern.group(1);
      hasInput = hasInput || t.equals("+") || t.equals("-");
      inputs.add(t);
    }
    if (!hasInput || inputs.isEmpty()) {
      throw new RuntimeException("Cannot split without inputs.");
    }

    IntList outputs = new IntArrayList(M.getO());
    UtilityMethods.removeDuplicates(outputs);
    List<Automaton> subautomata = M.uncombine(outputs);

    subautomata.replaceAll(automaton -> automaton.processSplit(inputs, isReverse, printSteps, prefix, log));

    Automaton N = subautomata.remove(0);
    N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printSteps, prefix, log);

    writeAutomata(s, N,
        isDFAO ? Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary(),
        name, isDFAO);

    return new TestCase(N);
  }

  public static TestCase splitCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_split_CMD, s, "split");
    return processSplitCommand(s, false,
        m.group(GROUP_SPLIT_AUTOMATA), m.group(GROUP_SPLIT_NAME), m.group(GROUP_SPLIT_END),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_SPLIT_INPUT)));
  }

  public static TestCase rsplitCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_rsplit_CMD, s, "reverse split");
    return processSplitCommand(s, true,
        m.group(GROUP_RSPLIT_AUTOMATA), m.group(GROUP_RSPLIT_NAME), m.group(GROUP_RSPLIT_END),
        PAT_FOR_INPUT_IN_split_CMD.matcher(m.group(GROUP_RSPLIT_INPUT)));
  }

  public static TestCase joinCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_join_CMD, s, "join");

    boolean printSteps = m.group(GROUP_JOIN_END).equals(":");

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_join_CMD.matcher(m.group(GROUP_JOIN_AUTOMATA));
    List<Automaton> subautomata = new ArrayList<>();
    boolean isDFAO = false;
    while (m1.find()) {
      String automatonName = m1.group(GROUP_JOIN_AUTOMATON_NAME);
      String addressForWordAutomaton
          = Session.getReadFileForWordsLibrary(automatonName + ".txt");
      String addressForAutomaton
          = Session.getReadFileForAutomataLibrary(automatonName + ".txt");
      Automaton M;
      if ((new File(addressForWordAutomaton)).exists()) {
        M = new Automaton(addressForWordAutomaton);
        isDFAO = true;
      } else if ((new File(addressForAutomaton)).exists()) {
        M = new Automaton(addressForAutomaton);
      } else {
        throw new RuntimeException("Automaton " + m.group(GROUP_RSPLIT_AUTOMATA) + " does not exist.");
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
    N = N.join(new LinkedList<>(subautomata), printSteps, prefix, log);

    writeAutomata(s, N,
        isDFAO ? Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary(),
        m.group(GROUP_JOIN_NAME), isDFAO);

    return new TestCase(N);
  }


  public static void testCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_test_CMD, s, "test");

    int needed = Integer.parseInt(m.group(GROUP_TEST_NUM));

    // We find the first n inputs accepted by our automaton, lexicographically. If less than n inputs are accepted,
    // we output all that are.
    Automaton M = new Automaton(Session.getReadFileForAutomataLibrary(m.group(GROUP_TEST_NAME) + ".txt"));

    // we don't want to count multiple representations of the same value as distinct accepted values
    M = removeLeadTrailZeroes(M);

    String infSubcommand = "inf " + m.group(GROUP_TEST_NAME) + ";";
    boolean infinite = infCommand(infSubcommand);

    StringBuilder incLengthReg = new StringBuilder();
    incLengthReg.append("reg ").append(m.group(GROUP_TEST_NAME)).append("_len ");
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
      System.out.println(m.group(GROUP_TEST_NAME) + " only accepts " + accepted.size() + " inputs, which are as follows: ");
    }
    for (String input : accepted) {
      System.out.println(input);
    }
  }

  public static TestCase ostCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_ost_CMD, s, "ost");

    String name = m.group(GROUP_OST_NAME);
    Ostrowski ostr = new Ostrowski(name, m.group(GROUP_OST_PREPERIOD), m.group(GROUP_OST_PERIOD));
    Ostrowski.writeAutomaton(name, "msd_" + name + ".txt", ostr.createRepresentationAutomaton());
    Automaton adder = ostr.createAdderAutomaton();
    Ostrowski.writeAutomaton(name, "msd_" + name + "_addition.txt", adder);
    return new TestCase(adder);
  }

  public static TestCase transduceCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_transduce_CMD, s, "transduce");

    boolean printSteps = m.group(GROUP_TRANSDUCE_END).equals(":");
    boolean printDetails = m.group(GROUP_TRANSDUCE_END).equals("::");

    Transducer T = new Transducer(Session.getTransducerFile(m.group(GROUP_TRANSDUCE_TRANSDUCER) + ".txt"));
    String inFileName = m.group(GROUP_TRANSDUCE_OLD_NAME) + ".txt";
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);

    Automaton C = T.transduceNonDeterministic(M, printSteps || printDetails, prefix, log);
    writeAutomata(s, C, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_TRANSDUCE_NEW_NAME), true);
    return new TestCase(C);
  }


  public static TestCase reverseCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_reverse_CMD, s, "reverse");

    boolean printSteps = m.group(GROUP_REVERSE_END).equals(":");
    boolean printDetails = m.group(GROUP_REVERSE_END).equals("::");

    boolean isDFAO = true;

    String inFileName = m.group(GROUP_REVERSE_OLD_NAME) + ".txt";
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
      isDFAO = false;
    }

    Automaton M = new Automaton(inLibrary);

    if (isDFAO) {
      AutomatonLogicalOps.reverseWithOutput(M, true, printSteps || printDetails, prefix, log);
    } else {
      AutomatonLogicalOps.reverse(M, printSteps || printDetails, prefix, log, true);
    }

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }

    writeAutomata(s, M, outLibrary, m.group(GROUP_REVERSE_NEW_NAME), true);
    return new TestCase(M);
  }

  public static TestCase minimizeCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_minimize_CMD, s, "minimize");

    boolean printSteps = m.group(GROUP_MINIMIZE_END).equals(":");
    boolean printDetails = m.group(GROUP_MINIMIZE_END).equals("::");

    Automaton M = new Automaton(
        Session.getReadFileForWordsLibrary(m.group(GROUP_MINIMIZE_OLD_NAME) + ".txt"));

    M.minimizeSelfWithOutput(printSteps || printDetails, prefix, log);

    writeAutomata(s, M, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_MINIMIZE_NEW_NAME), true);
    return new TestCase(M);
  }

  public static TestCase convertCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_convert_CMD, s, "convert");

    if (m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN).equals("$")
        && !m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN).equals("$")) {
      throw new RuntimeException("Cannot convert a Word Automaton into a function");
    }

    boolean printSteps = m.group(GROUP_CONVERT_END).equals(":");
    boolean printDetails = m.group(GROUP_CONVERT_END).equals("::");

    String inFileName = m.group(GROUP_CONVERT_OLD_NAME) + ".txt";
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);

    AutomatonLogicalOps.convertNS(M, m.group(GROUP_CONVERT_MSD_OR_LSD).equals("msd"),
        Integer.parseInt(m.group(GROUP_CONVERT_BASE)), printSteps || printDetails,
        prefix, log);

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN).equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }

    writeAutomata(s, M, outLibrary, m.group(GROUP_CONVERT_NEW_NAME), true);

    return new TestCase(M);
  }

  public static TestCase fixLeadZeroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_fixleadzero_CMD, s, "fixleadzero");

    boolean printSteps = m.group(GROUP_FIXLEADZERO_END).equals(":");
    boolean printDetails = m.group(GROUP_FIXLEADZERO_END).equals("::");

    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_FIXLEADZERO_OLD_NAME) + ".txt"));

    AutomatonLogicalOps.fixLeadingZerosProblem(M, printSteps || printDetails, prefix, log);

    writeAutomata(s, M, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXLEADZERO_NEW_NAME), false);
    return new TestCase(M);
  }


  public static TestCase fixTrailZeroCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_fixtrailzero_CMD, s, "fixtrailzero");

    boolean printSteps = m.group(GROUP_FIXTRAILZERO_END).equals(":");
    boolean printDetails = m.group(GROUP_FIXTRAILZERO_END).equals("::");

    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_FIXTRAILZERO_OLD_NAME) + ".txt"));

    AutomatonLogicalOps.fixTrailingZerosProblem(M, printSteps || printDetails, prefix, log);

    writeAutomata(s, M, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXTRAILZERO_NEW_NAME), false);

    return new TestCase(M);
  }

  public static TestCase alphabetCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_alphabet_CMD, s, "alphabet");

    if (m.group(GROUP_alphabet_LIST_OF_ALPHABETS) == null) {
      throw new RuntimeException("List of alphabets for alphabet command must not be empty.");
    }

    List<List<Integer>> alphabets = new ArrayList<>();
    List<NumberSystem> numSys = new ArrayList<>();
    List<Integer> alphabet;

    boolean printSteps = m.group(GROUP_alphabet_END).equals(":");
    boolean printDetails = m.group(GROUP_alphabet_END).equals("::");

    boolean isDFAO = true;

    String inFileName = m.group(GROUP_alphabet_OLD_NAME) + ".txt";
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
      isDFAO = false;
    }

    Matcher m1 = PAT_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
    int counter = 1;
    while (m1.find()) {

      if ((m1.group(R_NUMBER_SYSTEM) != null)) {
        String base = "msd_2";
        if (m1.group(3) != null) base = m1.group(3);
        if (m1.group(6) != null) base = m1.group(7) + "_" + m1.group(8);
        if (m1.group(9) != null) base = m1.group(9) + "_2";
        if (m1.group(10) != null) base = "msd_" + m1.group(10);
        NumberSystem ns = getNumberSystem(base, numSys, m);
        alphabets.add(ns.getAlphabet());
      } else if (m1.group(R_SET) != null) {
        alphabet = determineAlphabet(m1.group(R_SET));
        alphabets.add(alphabet);
        numSys.add(null);
      } else {
        throw new RuntimeException("Alphabet at position " + counter + " not recognized in alphabet command");
      }
      counter += 1;
    }

    Automaton M = new Automaton(inLibrary);

    // here, call the function to set the number system.
    M.setAlphabet(isDFAO, numSys, alphabets, printDetails || printSteps, prefix, log);

    String outLibrary = Session.getWriteAddressForWordsLibrary();
    if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
      outLibrary = Session.getWriteAddressForAutomataLibrary();
    }
    writeAutomata(s, M, outLibrary, m.group(GROUP_alphabet_NEW_NAME), false);

    return new TestCase(M);
  }


  public static TestCase unionCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_union_CMD, s, "union");

    boolean printSteps = m.group(GROUP_UNION_END).equals(":");
    boolean printDetails = m.group(GROUP_UNION_END).equals("::");

    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_union_CMD.matcher(m.group(GROUP_UNION_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new RuntimeException("Union requires at least one automaton as input.");
    }
    Automaton C = new Automaton(
        Session.getReadFileForAutomataLibrary(automataNames.get(0) + ".txt"));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, "union", printDetails || printSteps, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_UNION_NAME), true);

    return new TestCase(C);
  }

  public static TestCase intersectCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_intersect_CMD, s, "intersect");

    boolean printSteps = m.group(GROUP_INTERSECT_END).equals(":");
    boolean printDetails = m.group(GROUP_INTERSECT_END).equals("::");

    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_intersect_CMD.matcher(m.group(GROUP_INTERSECT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new RuntimeException("Intersect requires at least one automaton as input.");
    }
    Automaton C = new Automaton(
        Session.getReadFileForAutomataLibrary(automataNames.get(0) + ".txt"));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, "intersect", printDetails || printSteps, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_INTERSECT_NAME), true);

    return new TestCase(C);
  }


  public static TestCase starCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_star_CMD, s, "star");

    boolean printSteps = m.group(GROUP_STAR_END).equals(":");
    boolean printDetails = m.group(GROUP_STAR_END).equals("::");

    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_STAR_OLD_NAME) + ".txt"));

    Automaton C = M.star(printSteps || printDetails, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_STAR_NEW_NAME), false);
    return new TestCase(C);
  }

  public static TestCase concatCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_concat_CMD, s, "concat");

    boolean printSteps = m.group(GROUP_CONCAT_END).equals(":");
    boolean printDetails = m.group(GROUP_CONCAT_END).equals("::");

    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_concat_CMD.matcher(m.group(GROUP_CONCAT_AUTOMATA));
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.size() < 2) {
      throw new RuntimeException("Concatenation requires at least two automata as input.");
    }
    Automaton C = new Automaton(
        Session.getReadFileForAutomataLibrary(automataNames.get(0) + ".txt"));

    automataNames.remove(0);

    C = C.concat(automataNames, printDetails || printSteps, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_CONCAT_NAME), true);

    return new TestCase(C);
  }


  public static TestCase rightquoCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_rightquo_CMD, s, "rightquo");

    boolean printSteps = m.group(GROUP_rightquo_END).equals(":");
    boolean printDetails = m.group(GROUP_rightquo_END).equals("::");

    Automaton M1 = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_rightquo_OLD_NAME1) + ".txt"));
    Automaton M2 = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_rightquo_OLD_NAME2) + ".txt"));

    Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false, printSteps || printDetails, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_rightquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public static TestCase leftquoCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_leftquo_CMD, s, "leftquo");

    boolean printSteps = m.group(GROUP_leftquo_END).equals(":");
    boolean printDetails = m.group(GROUP_leftquo_END).equals("::");

    Automaton M1 = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_leftquo_OLD_NAME1) + ".txt"));
    Automaton M2 = new Automaton(
        Session.getReadFileForAutomataLibrary(m.group(GROUP_leftquo_OLD_NAME2) + ".txt"));

    Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2, printSteps || printDetails, prefix, log);

    writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_leftquo_NEW_NAME), false);
    return new TestCase(C);
  }

  public static TestCase drawCommand(String s) {
    Matcher m = matchOrFail(PAT_FOR_draw_CMD, s, "draw");

    String inFileName = m.group(GROUP_draw_NAME) + ".txt";
    String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
    if (m.group(GROUP_draw_DOLLAR_SIGN).equals("$")) {
      inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
    }
    Automaton M = new Automaton(inLibrary);
    AutomatonWriter.draw(M, Session.getAddressForResult() + m.group(GROUP_draw_NAME) + ".gv", s, false);

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

  private static Automaton removeLeadTrailZeroes(Automaton M) {
    // When dealing with enumerating values (e.g. inf and test commands), we remove leading zeroes in the case of msd
    // and trailing zeroes in the case of lsd. To do this, we construct a reg subcommand that generates the complement
    // of zero-prefixed strings for msd and zero suffixed strings for lsd, then intersect this with our original automaton.
    M.randomLabel();
    return AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);
  }

  private static void writeAutomata(String s, Automaton M, String outLibrary, String name, boolean isDFAO) {
    AutomatonWriter.draw(M, Session.getAddressForResult() + name + ".gv", s, isDFAO);
    AutomatonWriter.write(M, Session.getAddressForResult() + name + ".txt");
    AutomatonWriter.write(M, outLibrary + name + ".txt");
  }
}
