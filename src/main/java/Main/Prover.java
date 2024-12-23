/*	 Copyright 2016 Hamoon Mousavi
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
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.*;
import Automata.Numeration.Ostrowski;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 *
 * @author Hamoon
 */
public class Prover {
    static String REGEXP_FOR_THE_LIST_OF_COMMANDS = "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|draw|help)";
    static String REGEXP_FOR_EMPTY_COMMAND = "^\\s*(;|::|:)\\s*$";
    /**
     * the high-level scheme of a command is a name followed by some arguments and ending in either ; : or ::
     */
    static String REGEXP_FOR_COMMAND = "^\\s*(\\w+)(\\s+.*)?(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_COMMAND = Pattern.compile(REGEXP_FOR_COMMAND);

    static String REGEXP_FOR_exit_COMMAND = "^\\s*(exit|quit)\\s*(;|::|:)$";

    static String REGEXP_FOR_load_COMMAND = "^\\s*load\\s+(\\w+\\.txt)\\s*(;|::|:)\\s*$";
    /**
     * group for filename in REGEXP_FOR_load_COMMAND
     */
    static int L_FILENAME = 1;
    static Pattern PATTERN_FOR_load_COMMAND = Pattern.compile(REGEXP_FOR_load_COMMAND);

    static String REGEXP_FOR_eval_def_COMMANDS = "^\\s*(eval|def)\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s+\"(.*)\"\\s*(;|::|:)\\s*$";
    /**
     * important groups in REGEXP_FOR_eval_def_COMMANDS
     */
    static int ED_TYPE = 1, ED_NAME = 2, ED_FREE_VARIABLES = 3, ED_PREDICATE = 6, ED_ENDING = 7;
    static Pattern PATTERN_FOR_eval_def_COMMANDS = Pattern.compile(REGEXP_FOR_eval_def_COMMANDS);
    static String REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_COMMANDS = "[a-zA-Z]\\w*";
    static Pattern PATTERN_FOR_A_FREE_VARIABLE_IN_eval_def_COMMANDS = Pattern.compile(REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_COMMANDS);

    static String REGEXP_FOR_macro_COMMAND = "^\\s*macro\\s+([a-zA-Z]\\w*)\\s+\"(.*)\"\\s*(;|::|:)\\s*$";
    static int M_NAME = 1, M_DEFINITION = 2;
    static Pattern PATTERN_FOR_macro_COMMAND = Pattern.compile(REGEXP_FOR_macro_COMMAND);

    static String REGEXP_FOR_reg_COMMAND = "^\\s*(reg)\\s+([a-zA-Z]\\w*)\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)\"(.*)\"\\s*(;|::|:)\\s*$";

    /**
     * important groups in REGEXP_FOR_reg_COMMAND
     */
    static int R_NAME = 2, R_LIST_OF_ALPHABETS = 3, R_REGEXP = 20;
    static Pattern PATTERN_FOR_reg_COMMAND = Pattern.compile(REGEXP_FOR_reg_COMMAND);
    static String REGEXP_FOR_A_SINGLE_ELEMENT_OF_A_SET = "(\\+|\\-)?\\s*\\d+";
    static Pattern PATTERN_FOR_A_SINGLE_ELEMENT_OF_A_SET = Pattern.compile(REGEXP_FOR_A_SINGLE_ELEMENT_OF_A_SET);
    static String REGEXP_FOR_AN_ALPHABET = "((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+";
    static Pattern PATTERN_FOR_AN_ALPHABET = Pattern.compile(REGEXP_FOR_AN_ALPHABET);
    static int R_NUMBER_SYSTEM = 2, R_SET = 11;

    static String REGEXP_FOR_AN_ALPHABET_VECTOR = "(\\[(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\])|(\\d)";
    static Pattern PATTERN_FOR_AN_ALPHABET_VECTOR = Pattern.compile(REGEXP_FOR_AN_ALPHABET_VECTOR);


    static String REGEXP_FOR_ost_COMMAND = "^\\s*ost\\s+([a-zA-Z]\\w*)\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*(;|:|::)\\s*$";
    static Pattern PATTERN_FOR_ost_COMMAND = Pattern.compile(REGEXP_FOR_ost_COMMAND);
    static int GROUP_OST_NAME = 1;
    static int GROUP_OST_PREPERIOD = 2;
    static int GROUP_OST_PERIOD = 4;

    static String REGEXP_FOR_combine_COMMAND = "^\\s*combine\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*(=-?\\d+)?))*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_combine_COMMAND = Pattern.compile(REGEXP_FOR_combine_COMMAND);
    static int GROUP_COMBINE_NAME = 1, GROUP_COMBINE_AUTOMATA = 2, GROUP_COMBINE_END = 6;
    static String REGEXP_FOR_AN_AUTOMATON_IN_combine_COMMAND = "([a-zA-Z]\\w*)((=-?\\d+)?)";
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_combine_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_IN_combine_COMMAND);

    static String REGEXP_FOR_morphism_COMMAND = "^\\s*morphism\\s+([a-zA-Z]\\w*)\\s+\"(\\d+\\s*\\-\\>\\s*(.)*(,\\d+\\s*\\-\\>\\s*(.)*)*)\"\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_morphism_COMMAND = Pattern.compile(REGEXP_FOR_morphism_COMMAND);
    static int GROUP_MORPHISM_NAME = 1, GROUP_MORPHISM_DEFINITION;

    static String REGEXP_FOR_promote_COMMAND = "^\\s*promote\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_promote_COMMAND = Pattern.compile(REGEXP_FOR_promote_COMMAND);
    static int GROUP_PROMOTE_NAME = 1, GROUP_PROMOTE_MORPHISM = 2;

    static String REGEXP_FOR_image_COMMAND = "^\\s*image\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_image_COMMAND = Pattern.compile(REGEXP_FOR_image_COMMAND);
    static int GROUP_IMAGE_NEW_NAME = 1, GROUP_IMAGE_MORPHISM = 2, GROUP_IMAGE_OLD_NAME = 3;

    static String REGEXP_FOR_inf_COMMAND = "^\\s*inf\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_inf_COMMAND = Pattern.compile(REGEXP_FOR_inf_COMMAND);
    static int GROUP_INF_NAME = 1;

    static String REGEXP_FOR_split_COMMAND = "^\\s*split\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[+-]?\\s*])+)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_split_COMMAND = Pattern.compile(REGEXP_FOR_split_COMMAND);
    static int GROUP_SPLIT_NAME = 1, GROUP_SPLIT_AUTOMATA = 2, GROUP_SPLIT_INPUT = 3, GROUP_SPLIT_END = 5;
    static String REGEXP_FOR_INPUT_IN_split_COMMAND = "\\[\\s*([+-]?)\\s*]";
    static Pattern PATTERN_FOR_INPUT_IN_split_COMMAND = Pattern.compile(REGEXP_FOR_INPUT_IN_split_COMMAND);

    static String REGEXP_FOR_rsplit_COMMAND = "^\\s*rsplit\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[+-]?\\s*])+)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_rsplit_COMMAND = Pattern.compile(REGEXP_FOR_rsplit_COMMAND);
    static int GROUP_RSPLIT_NAME = 1, GROUP_RSPLIT_AUTOMATA = 4, GROUP_RSPLIT_INPUT = 2, GROUP_RSPLIT_END = 5;

    static String REGEXP_FOR_join_COMMAND = "^\\s*join\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+))*)\\s*(;|::|:)\\s*";
    static Pattern PATTERN_FOR_join_COMMAND = Pattern.compile(REGEXP_FOR_join_COMMAND);
    static int GROUP_JOIN_NAME = 1, GROUP_JOIN_AUTOMATA = 2, GROUP_JOIN_END = 7;
    static String REGEXP_FOR_AN_AUTOMATON_IN_join_COMMAND = "([a-zA-Z]\\w*)((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+)";
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_join_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_IN_join_COMMAND);
    static int GROUP_JOIN_AUTOMATON_NAME = 1, GROUP_JOIN_AUTOMATON_INPUT = 2;
    static String REGEXP_FOR_AN_AUTOMATON_INPUT_IN_join_COMMAND = "\\[\\s*([a-zA-Z&&[^AE]]\\w*)\\s*]";
    static Pattern PATTERN_FOR_AN_AUTOMATON_INPUT_IN_join_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_INPUT_IN_join_COMMAND);

    static String REGEXP_FOR_test_COMMAND = "^\\s*test\\s+([a-zA-Z]\\w*)\\s*(\\d+)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_test_COMMAND = Pattern.compile(REGEXP_FOR_test_COMMAND);
    static int GROUP_TEST_NAME = 1, GROUP_TEST_NUM = 2;

    static String REGEXP_FOR_transduce_COMMAND = "^\\s*transduce\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_transduce_COMMAND = Pattern.compile(REGEXP_FOR_transduce_COMMAND);
    static int GROUP_TRANSDUCE_NEW_NAME = 1, GROUP_TRANSDUCE_TRANSDUCER = 2,
            GROUP_TRANSDUCE_DOLLAR_SIGN = 3, GROUP_TRANSDUCE_OLD_NAME = 4, GROUP_TRANSDUCE_END = 5;

    static String REGEXP_FOR_reverse_COMMAND = "^\\s*reverse\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_reverse_COMMAND = Pattern.compile(REGEXP_FOR_reverse_COMMAND);
    static int GROUP_REVERSE_NEW_NAME = 1, GROUP_REVERSE_DOLLAR_SIGN = 2, GROUP_REVERSE_OLD_NAME = 3, GROUP_REVERSE_END = 4;

    static String REGEXP_FOR_minimize_COMMAND = "^\\s*minimize\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_minimize_COMMAND = Pattern.compile(REGEXP_FOR_minimize_COMMAND);
    static int GROUP_MINIMIZE_NEW_NAME = 1, GROUP_MINIMIZE_OLD_NAME = 2, GROUP_MINIMIZE_END = 3;

    static String REGEXP_FOR_convert_COMMAND = "^\\s*convert\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s+((msd|lsd)_(\\d+))\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_convert_COMMAND = Pattern.compile(REGEXP_FOR_convert_COMMAND);
    static int GROUP_CONVERT_NEW_NAME = 2, GROUP_CONVERT_OLD_NAME = 7, GROUP_CONVERT_END = 8,
            GROUP_CONVERT_NEW_DOLLAR_SIGN = 1, GROUP_CONVERT_OLD_DOLLAR_SIGN = 6,
            GROUP_CONVERT_MSD_OR_LSD = 4,
            GROUP_CONVERT_BASE = 5;

    static String REGEXP_FOR_fixleadzero_COMMAND = "^\\s*fixleadzero\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_fixleadzero_COMMAND = Pattern.compile(REGEXP_FOR_fixleadzero_COMMAND);
    static int GROUP_FIXLEADZERO_NEW_NAME = 1, GROUP_FIXLEADZERO_OLD_NAME = 3, GROUP_FIXLEADZERO_END = 4;

    static String REGEXP_FOR_fixtrailzero_COMMAND = "^\\s*fixtrailzero\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_fixtrailzero_COMMAND = Pattern.compile(REGEXP_FOR_fixtrailzero_COMMAND);
    static int GROUP_FIXTRAILZERO_NEW_NAME = 1, GROUP_FIXTRAILZERO_OLD_NAME = 3, GROUP_FIXTRAILZERO_END = 4;

    static String REGEXP_FOR_alphabet_COMMAND = "^\\s*(alphabet)\\s+([a-zA-Z]\\w*)\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_alphabet_COMMAND = Pattern.compile(REGEXP_FOR_alphabet_COMMAND);
    static int GROUP_alphabet_NEW_NAME = 2, GROUP_alphabet_LIST_OF_ALPHABETS = 3, GROUP_alphabet_DOLLAR_SIGN = 20, GROUP_alphabet_OLD_NAME = 21, GROUP_alphabet_END = 22;

    static String REGEXP_FOR_union_COMMAND = "^\\s*union\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_union_COMMAND = Pattern.compile(REGEXP_FOR_union_COMMAND);
    static int GROUP_UNION_NAME = 1, GROUP_UNION_AUTOMATA = 2, GROUP_UNION_END = 5;
    static String REGEXP_FOR_AN_AUTOMATON_IN_union_COMMAND = "([a-zA-Z]\\w*)";
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_union_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_IN_union_COMMAND);

    static String REGEXP_FOR_intersect_COMMAND = "^\\s*intersect\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_intersect_COMMAND = Pattern.compile(REGEXP_FOR_intersect_COMMAND);
    static int GROUP_INTERSECT_NAME = 1, GROUP_INTERSECT_AUTOMATA = 2, GROUP_INTERSECT_END = 5;
    static String REGEXP_FOR_AN_AUTOMATON_IN_intersect_COMMAND = "([a-zA-Z]\\w*)";
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_intersect_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_IN_intersect_COMMAND);

    static String REGEXP_FOR_star_COMMAND = "^\\s*star\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_star_COMMAND = Pattern.compile(REGEXP_FOR_star_COMMAND);
    static int GROUP_STAR_NEW_NAME = 1, GROUP_STAR_OLD_NAME = 2, GROUP_STAR_END = 3;

    static String REGEXP_FOR_concat_COMMAND = "^\\s*concat\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_concat_COMMAND = Pattern.compile(REGEXP_FOR_concat_COMMAND);
    static int GROUP_CONCAT_NAME = 1, GROUP_CONCAT_AUTOMATA = 2, GROUP_CONCAT_END = 5;
    static String REGEXP_FOR_AN_AUTOMATON_IN_concat_COMMAND = "([a-zA-Z]\\w*)";
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_concat_COMMAND = Pattern.compile(REGEXP_FOR_AN_AUTOMATON_IN_concat_COMMAND);

    static String REGEXP_FOR_rightquo_COMMAND = "^\\s*rightquo\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_rightquo_COMMAND = Pattern.compile(REGEXP_FOR_rightquo_COMMAND);
    static int GROUP_rightquo_NEW_NAME = 1, GROUP_rightquo_OLD_NAME1 = 2, GROUP_rightquo_OLD_NAME2 = 3, GROUP_rightquo_END = 4;

    static String REGEXP_FOR_leftquo_COMMAND = "^\\s*leftquo\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_leftquo_COMMAND = Pattern.compile(REGEXP_FOR_leftquo_COMMAND);
    static int GROUP_leftquo_NEW_NAME = 1, GROUP_leftquo_OLD_NAME1 = 2, GROUP_leftquo_OLD_NAME2 = 3, GROUP_leftquo_END = 4;

    static String REGEXP_FOR_draw_COMMAND = "^\\s*draw\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_draw_COMMAND = Pattern.compile(REGEXP_FOR_draw_COMMAND);
    static int GROUP_draw_DOLLAR_SIGN = 1, GROUP_draw_NAME = 2;

    static String REGEXP_FOR_help_COMMAND = "^\\s*help(\\s*|\\s+(\\w*))\\s*(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_help_COMMAND = Pattern.compile(REGEXP_FOR_help_COMMAND);
    static int GROUP_help_NAME = 2;

    /**
     * if the command line argument is not empty, we treat args[0] as a filename.
     * if this is the case, we read from the file and load its commands before we submit control to user.
     * if the address is not a valid address or the file does not exist, we print an appropriate error message
     * and submit control to the user.
     * if the file contains the exit command we terminate the program.
     **/
    public static void main(String[] args) {
        Session.setPathsAndNames();

        // to run test cases, run the following lines:
        // IntegrationTest IT = new IntegrationTest(true);
        // IT.runTestCases();


        // WARNING: This will OVERRIDE THE EXISTING TESTS!!!
        // CREATE THE TEST RESULTS MANUALLY INSTEAD, IF YOU CAN!
        // to create test cases, run the following lines:
        // IntegrationTest IT = new IntegrationTest(true);
        // IT.createTestCases();


        // can also run these.
//		IT.runPerformanceTest("Walnut with Valmari without refactoring", 5);
//		IT.runPerformanceTest("Walnut with dk.bricks", 5);

        run(args);
    }

    public static void run(String[] args) {
        BufferedReader in = null;
        if (args.length >= 1) {
            //reading commands from the file with address args[0]
            try {
                in = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(
                                        Session.getReadAddressForCommandFiles(args[0])),
                            StandardCharsets.UTF_8));
                if (!readBuffer(in, false)) return;
            } catch (IOException e) {
                System.out.flush();
                System.err.println(e.getMessage());
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    System.out.flush();
                    System.err.println(ex.getMessage());
                }
            }
        }

        // Now we parse commands from the console.
        System.out.println("Welcome to Walnut v" + Session.WALNUT_VERSION +
            "! Type \"help;\" to see all available commands.");
        System.out.println("Starting Walnut session: " + Session.getName());
        in = new BufferedReader(new InputStreamReader(System.in));
        readBuffer(in, true);
    }

    /**
     * Takes a BufferedReader and reads from it until we hit end of file or exit command.
     *
     * @param in
     * @param console = true if in = System.in
     * @return
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
                        System.out.flush();
                        System.err.println(e.getMessage() + System.lineSeparator() + "\t: " + s);
                        System.err.flush();
                    }

                    buffer = new StringBuilder();
                } else {
                    buffer.append(s);
                }
            }
        } catch (IOException e) {
            System.out.flush();
            System.err.println(e.getMessage());
            System.err.flush();
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
     *         If the character following the found index is a colon, the index
     *         is incremented by one.
     */
    private static int determineIndex(String s) {
        int index1 = s.indexOf(';');
        int index2 = s.indexOf(':');
        int index;
        if (index1 != -1 && index2 != -1) {
            index = (index1 < index2) ? index1 : index2;
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
        if (s.matches(REGEXP_FOR_EMPTY_COMMAND)) {
            // If the command is just ; or : do nothing.
            return true;
        }

        Matcher matcher_for_command = PATTERN_FOR_COMMAND.matcher(s);
        if (!matcher_for_command.find()) {
            throw ExceptionHelper.invalidCommand();
        }

        String commandName = matcher_for_command.group(1);
        if (!commandName.matches(REGEXP_FOR_THE_LIST_OF_COMMANDS)) {
            throw ExceptionHelper.noSuchCommand();
        }

        switch (commandName) {
            case "exit", "quit" -> {
                if (s.matches(REGEXP_FOR_exit_COMMAND)) {
                    return false;
                }
                throw ExceptionHelper.invalidCommand();
            }
            case "load" -> {
                if (!loadCommand(s)) return false;
            }
            case "eval", "def" -> eval_def_commands(s);
            case "macro" -> macroCommand(s);
            case "reg" -> regCommand(s);
            case "ost" -> ostCommand(s);
            case "cls", "clear" -> clearScreen();
            case "combine" -> combineCommand(s);
            case "morphism" -> morphismCommand(s);
            case "promote" -> promoteCommand(s);
            case "image" -> imageCommand(s);
            case "inf" -> infCommand(s);
            case "split" -> splitCommand(s);
            case "rsplit" -> rsplitCommand(s);
            case "join" -> joinCommand(s);
            case "test" -> testCommand(s);
            case "transduce" -> transduceCommand(s);
            case "reverse" -> reverseCommand(s);
            case "minimize" -> minimizeCommand(s);
            case "convert" -> convertCommand(s);
            case "fixleadzero" -> fixLeadZeroCommand(s);
            case "fixtrailzero" -> fixTrailZeroCommand(s);
            case "alphabet" -> alphabetCommand(s);
            case "union" -> unionCommand(s);
            case "intersect" -> intersectCommand(s);
            case "star" -> starCommand(s);
            case "concat" -> concatCommand(s);
            case "rightquo" -> rightquoCommand(s);
            case "leftquo" -> leftquoCommand(s);
            case "draw" -> drawCommand(s);
            case "help" -> helpCommand(s);
            default -> throw ExceptionHelper.invalidCommand(commandName);
        }
        return true;
    }

    public static TestCase dispatchForIntegrationTest(String s, String msg) throws IOException {
        System.out.println("Running integration test: " + msg);
        if (s.matches(REGEXP_FOR_EMPTY_COMMAND)) {//if the command is just ; or : do nothing
            return null;
        }

        Matcher matcher_for_command = PATTERN_FOR_COMMAND.matcher(s);
        if (!matcher_for_command.find()) throw ExceptionHelper.invalidCommand();

        String commandName = matcher_for_command.group(1);
        if (!commandName.matches(REGEXP_FOR_THE_LIST_OF_COMMANDS)) {
            throw ExceptionHelper.noSuchCommand();
        }

        switch (commandName) {
            case "exit", "quit" -> {
                if (s.matches(REGEXP_FOR_exit_COMMAND)) return null;
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
            case "combine" -> {
                return combineCommand(s);
            }
            case "promote" -> {
                return promoteCommand(s);
            }
            case "image" -> {
                return imageCommand(s);
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
            case "help" -> helpCommand(s);
            default -> throw ExceptionHelper.invalidCommand(commandName);
        }
        return null;
    }

    /**
     * load x.p; loads commands from the file x.p. The file can contain any command except for load x.p;
     * The user don't get a warning if the x.p contains load x.p but the program might end up in an infinite loop.
     * Note that the file can contain load y.p; whenever y != x and y exist.
     *
     * @param s
     * @return
     */
    public static boolean loadCommand(String s) {
        Matcher m = PATTERN_FOR_load_COMMAND.matcher(s);
        if (!m.find()) throw ExceptionHelper.invalidCommandUse("load");
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(
                                    Session.getReadAddressForCommandFiles( m.group(L_FILENAME))),
                        StandardCharsets.UTF_8));
            if (!readBuffer(in, false)) {
                return false;
            }
        } catch (IOException e) {
            System.out.flush();
            System.err.println(e.getMessage());
            System.err.flush();
        }
        return true;
    }

    public static TestCase eval_def_commands(String s) throws IOException {
        Automaton M;

        Matcher m = PATTERN_FOR_eval_def_COMMANDS.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("eval/def");
        }

        List<String> free_variables = new ArrayList<>();
        if (m.group(ED_FREE_VARIABLES) != null) {
            determineMatricesToCompute(m.group(ED_FREE_VARIABLES), free_variables);
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

        if (m.group(ED_TYPE).equals("def")) {
            AutomatonWriter.write(
                c.result.M, Session.getWriteAddressForAutomataLibrary() + m.group(ED_NAME) + ".txt");
        }

        M = c.result.M;
        if (M.isTRUE_FALSE_AUTOMATON()) {
            if (M.isTRUE_AUTOMATON()) {
                System.out.println("____\nTRUE");
            } else {
                System.out.println("_____\nFALSE");
            }
        }

        return new TestCase(M, "", c.mpl, printDetails ? c.logDetails.toString() : "");
    }



    public static TestCase macroCommand(String s) {
        Matcher m = PATTERN_FOR_macro_COMMAND.matcher(s);
        if (!m.find()) throw ExceptionHelper.invalidCommandUse("macro");
        try {
            BufferedWriter out =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            Session.getWriteAddressForMacroLibrary() + m.group(M_NAME) + ".txt"), StandardCharsets.UTF_8));
            out.write(m.group(M_DEFINITION));
            out.close();
        } catch (IOException o) {
            System.out.println("Could not write the macro " + m.group(M_NAME));
        }
        return null;
    }

    public static TestCase regCommand(String s) {
        Matcher m = PATTERN_FOR_reg_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("reg");
        }
        List<List<Integer>> alphabets = new ArrayList<>();
        List<NumberSystem> numSys = new ArrayList<>();
        List<Integer> alphabet;
        if (m.group(R_LIST_OF_ALPHABETS) == null) {
            String base = "msd_2";
            NumberSystem ns = getNumberSystem(base, numSys, m);
            alphabets.add(ns.getAlphabet());
        }
        Matcher m1 = PATTERN_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
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

        Matcher m2 = PATTERN_FOR_AN_ALPHABET_VECTOR.matcher(baseexp);
        // if we haven't had to replace any input vectors with unicode, we use the legacy method of constructing the automaton
        while (m2.find()) {
            List<Integer> L = new ArrayList<>();
            String alphabetVector = m2.group();

            // needed to replace this string with the unicode mapping
            String alphabetVectorCopy = alphabetVector;
            if (alphabetVector.charAt(0) == '[') {
                alphabetVector = alphabetVector.substring(1, alphabetVector.length() - 1); // truncate brackets [ ]
            }

            Matcher m3 = PATTERN_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(alphabetVector);
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

            /**
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
        M.setAlphabetSize(1);
        for (List<Integer> alphlist : M.getA()) {
            M.setAlphabetSize(M.getAlphabetSize() * alphlist.size());
        }

        // We should always do this with replacement, since we may have regexes such as "...", which accepts any three characters
        // in a row, on an alphabet containing bracketed characters. We don't make any replacements here, but they are implicitly made
        // when we intersect with our alphabet(s).

        // remove all whitespace from regular expression.
        baseexp = baseexp.replaceAll("\\s", "");

        Automaton R = new Automaton(baseexp, M.getAlphabetSize());
        R.setA(M.getA());
        R.setAlphabetSize(M.getAlphabetSize());
        R.setNS(numSys);

        writeAutomata(m.group(R_REGEXP), R, Session.getWriteAddressForAutomataLibrary(), m.group(R_NAME), false);

        return new TestCase(R, "", "", "");
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
        Matcher m = PATTERN_FOR_combine_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("combine");
        }

        boolean printSteps = m.group(GROUP_COMBINE_END).equals(":");

        String prefix = "";
        StringBuilder log = new StringBuilder();


        List<String> automataNames = new ArrayList<>();
        IntList outputs = new IntArrayList();
        int argumentCounter = 0;

        Matcher m1 = PATTERN_FOR_AN_AUTOMATON_IN_combine_COMMAND.matcher(m.group(GROUP_COMBINE_AUTOMATA));
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

        return new TestCase(C, "", "", "");
    }


    public static void morphismCommand(String s) throws IOException {
        Matcher m = PATTERN_FOR_morphism_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("morphism");
        }
        String name = m.group(GROUP_MORPHISM_NAME);

        Morphism M = new Morphism(name, m.group(GROUP_MORPHISM_DEFINITION));
        System.out.print("Defined with domain ");
        System.out.print(M.mapping.keySet());
        System.out.print(" and range ");
        System.out.print(M.range);
        M.write(Session.getAddressForResult() + name + ".txt");
        M.write(Session.getWriteAddressForMorphismLibrary() + name + ".txt");
    }

    public static TestCase promoteCommand(String s) throws IOException {
        Matcher m = PATTERN_FOR_promote_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("prmote");
        }
        Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_PROMOTE_MORPHISM) + ".txt"));
        Automaton P = h.toWordAutomaton();
        writeAutomata(s, P, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_PROMOTE_NAME), true);

        return new TestCase(P, "", "", "");
    }

    public static TestCase imageCommand(String s) throws IOException {
        Matcher m = PATTERN_FOR_image_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("image");
        }
        Morphism h = new Morphism(Session.getReadFileForMorphismLibrary(m.group(GROUP_IMAGE_MORPHISM) + ".txt"));
        if (!h.isUniform()) {
            throw new RuntimeException("A morphism applied to a word automaton must be uniform.");
        }
        String combineString = "combine " + m.group(GROUP_IMAGE_NEW_NAME);

        // We need to know the number system of our old automaton: the new one should match, as should intermediary expressions
        Automaton M = new Automaton(Session.getReadFileForWordsLibrary(m.group(GROUP_IMAGE_OLD_NAME) + ".txt"));
        String numSysName = "";
        if (!M.getNS().isEmpty()) {
            numSysName = M.getNS().get(0).toString();
        }

        // we construct a define command for a DFA for each x that accepts iff x appears at the nth position
        for (Integer value : h.range) {
            eval_def_commands(h.makeInterCommand(value, m.group(GROUP_IMAGE_OLD_NAME), numSysName));
            combineString += " " + m.group(GROUP_IMAGE_OLD_NAME) + "_" + value + "=" + value;
        }
        combineString += ":";

        TestCase retrieval = combineCommand(combineString);
        Automaton I = retrieval.getResult().clone();

        writeAutomata(s, I, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_IMAGE_NEW_NAME), true);

        return new TestCase(I, "", "", "");
    }

    public static boolean infCommand(String s) {
        Matcher m = PATTERN_FOR_inf_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("inf");
        }
        Automaton M = new Automaton(Session.getReadFileForAutomataLibrary(m.group(GROUP_INF_NAME) + ".txt"));
        M = removeLeadTrailZeroes(M, m.group(GROUP_INF_NAME));
        String infReg = M.infinite();
        if (infReg == "") {
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
        String prefix = "";
        StringBuilder log = new StringBuilder();

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

        for (int i = 0; i < subautomata.size(); i++) {
            Automaton N = isReverse
                ? subautomata.get(i).reverseSplit(inputs, printSteps, prefix, log)
                : subautomata.get(i).split(inputs, printSteps, prefix, log);
            subautomata.set(i, N);
        }

        Automaton N = subautomata.remove(0);
        N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printSteps, prefix, log);

        writeAutomata(s, N,
            isDFAO ? Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary(),
            name, isDFAO);

        return new TestCase(N, "", "", "");
    }

    public static TestCase splitCommand(String s) {
        Matcher m = PATTERN_FOR_split_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("split");
        }
        return processSplitCommand(s, false,
            m.group(GROUP_SPLIT_AUTOMATA), m.group(GROUP_SPLIT_NAME), m.group(GROUP_SPLIT_END),
            PATTERN_FOR_INPUT_IN_split_COMMAND.matcher(m.group(GROUP_SPLIT_INPUT)));
    }

    public static TestCase rsplitCommand(String s) {
        Matcher m = PATTERN_FOR_rsplit_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("reverse split");
        }
        return processSplitCommand(s, true,
            m.group(GROUP_RSPLIT_AUTOMATA), m.group(GROUP_RSPLIT_NAME), m.group(GROUP_RSPLIT_END),
            PATTERN_FOR_INPUT_IN_split_COMMAND.matcher(m.group(GROUP_RSPLIT_INPUT)));
    }

    public static TestCase joinCommand(String s) {
        Matcher m = PATTERN_FOR_join_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("join");
        }

        boolean printSteps = m.group(GROUP_JOIN_END).equals(":");
        String prefix = "";
        StringBuilder log = new StringBuilder();

        Matcher m1 = PATTERN_FOR_AN_AUTOMATON_IN_join_COMMAND.matcher(m.group(GROUP_JOIN_AUTOMATA));
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
            Matcher m2 = PATTERN_FOR_AN_AUTOMATON_INPUT_IN_join_COMMAND.matcher(automatonInputs);
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

        return new TestCase(N, "", "", "");
    }


    public static void testCommand(String s) {
        Matcher m = PATTERN_FOR_test_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("test");
        }

        int needed = Integer.parseInt(m.group(GROUP_TEST_NUM));

        // We find the first n inputs accepted by our automaton, lexicographically. If less than n inputs are accepted,
        // we output all that are.
        Automaton M = new Automaton(Session.getReadFileForAutomataLibrary(m.group(GROUP_TEST_NAME) + ".txt"));

        // we don't want to count multiple representations of the same value as distinct accepted values
        M = removeLeadTrailZeroes(M, m.group(GROUP_TEST_NAME));

        // We will be intersecting this automaton with various regex automata, so it needs to be labelled.
        M.randomLabel();

        String infSubcommand = "inf " + m.group(GROUP_TEST_NAME) + ";";
        boolean infinite = infCommand(infSubcommand);

        String incLengthReg = "";
        incLengthReg += "reg " + m.group(GROUP_TEST_NAME) + "_len ";
        for (int i = 0; i < M.getA().size(); i++) {
            String alphaString = M.getA().get(i).toString();
            alphaString = alphaString.substring(1, alphaString.length() - 1);
            alphaString = "{" + alphaString + "} ";
            incLengthReg += alphaString;
        }

        String dotReg = "";
        int searchLength = 0;
        List<String> accepted = new ArrayList<>();
        while (true) {
            searchLength++;
            dotReg += ".";
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

    public static void ostCommand(String s) {
        Matcher m = PATTERN_FOR_ost_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("ost");
        }

        String name = m.group(GROUP_OST_NAME);
        Ostrowski ostr = new Ostrowski(name, m.group(GROUP_OST_PREPERIOD), m.group(GROUP_OST_PERIOD));
        Ostrowski.writeRepresentation(name, ostr.createRepresentationAutomaton());
        Ostrowski.writeAdder(name, ostr.createAdderAutomaton());
    }

    public static TestCase transduceCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_transduce_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("transduce");
            }

            boolean printSteps = m.group(GROUP_TRANSDUCE_END).equals(":");
            boolean printDetails = m.group(GROUP_TRANSDUCE_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            Transducer T = new Transducer(Session.getTransducerFile(m.group(GROUP_TRANSDUCE_TRANSDUCER) + ".txt"));
            String inFileName = m.group(GROUP_TRANSDUCE_OLD_NAME) + ".txt";
            String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
            if (m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$")) {
                inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
            }
            Automaton M = new Automaton(inLibrary);

            Automaton C = T.transduceNonDeterministic(M, printSteps || printDetails, prefix, log);
            writeAutomata(s, C, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_TRANSDUCE_NEW_NAME), true);
            return new TestCase(C, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error transducing automaton");
        }
    }


    public static TestCase reverseCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_reverse_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("reverse");
            }

            boolean printSteps = m.group(GROUP_REVERSE_END).equals(":");
            boolean printDetails = m.group(GROUP_REVERSE_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

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
            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reversing automaton.");
        }
    }

    public static TestCase minimizeCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_minimize_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("minimize");
            }

            boolean printSteps = m.group(GROUP_MINIMIZE_END).equals(":");
            boolean printDetails = m.group(GROUP_MINIMIZE_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M = new Automaton(
                Session.getReadFileForWordsLibrary(m.group(GROUP_MINIMIZE_OLD_NAME) + ".txt"));

            M.minimizeSelfWithOutput(printSteps || printDetails, prefix, log);

            writeAutomata(s, M, Session.getWriteAddressForWordsLibrary(), m.group(GROUP_MINIMIZE_NEW_NAME), true);
            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error minimizing word automaton.");
        }
    }

    public static TestCase convertCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_convert_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("convert");
            }

            if (m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN).equals("$")
                    && !m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN).equals("$")) {
                throw new RuntimeException("Cannot convert a Word Automaton into a function");
            }

            boolean printSteps = m.group(GROUP_CONVERT_END).equals(":");
            boolean printDetails = m.group(GROUP_CONVERT_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

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

            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting automaton.");
        }
    }

    public static TestCase fixLeadZeroCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_fixleadzero_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("fixleadzero");
            }

            boolean printSteps = m.group(GROUP_FIXLEADZERO_END).equals(":");
            boolean printDetails = m.group(GROUP_FIXLEADZERO_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_FIXLEADZERO_OLD_NAME) + ".txt"));

            AutomatonLogicalOps.fixLeadingZerosProblem(M, printSteps || printDetails, prefix, log);

            writeAutomata(s, M, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXLEADZERO_NEW_NAME), false);
            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error fixing leading zeroes for automaton.");
        }
    }


    public static TestCase fixTrailZeroCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_fixtrailzero_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("fixtrailzero");
            }

            boolean printSteps = m.group(GROUP_FIXTRAILZERO_END).equals(":");
            boolean printDetails = m.group(GROUP_FIXTRAILZERO_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_FIXTRAILZERO_OLD_NAME) + ".txt"));

            AutomatonLogicalOps.fixTrailingZerosProblem(M, printSteps || printDetails, prefix, log);

            writeAutomata(s, M, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_FIXTRAILZERO_NEW_NAME), false);

            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error fixing trailing zeroes for automaton.");
        }
    }

    public static TestCase alphabetCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_alphabet_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("alphabet");
            }

            if (m.group(GROUP_alphabet_LIST_OF_ALPHABETS) == null) {
                throw new RuntimeException("List of alphabets for alphabet command must not be empty.");
            }

            List<List<Integer>> alphabets = new ArrayList<>();
            List<NumberSystem> numSys = new ArrayList<>();
            List<Integer> alphabet;

            boolean printSteps = m.group(GROUP_alphabet_END).equals(":");
            boolean printDetails = m.group(GROUP_alphabet_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            boolean isDFAO = true;

            String inFileName = m.group(GROUP_alphabet_OLD_NAME) + ".txt";
            String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
            if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
                inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
                isDFAO = false;
            }

            Matcher m1 = PATTERN_FOR_AN_ALPHABET.matcher(m.group(R_LIST_OF_ALPHABETS));
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

            return new TestCase(M, "", "", "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the alphabet command.");
        }
    }


    public static TestCase unionCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_union_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("union");
            }

            boolean printSteps = m.group(GROUP_UNION_END).equals(":");
            boolean printDetails = m.group(GROUP_UNION_END).equals("::");

            String prefix = "";
            StringBuilder log = new StringBuilder();


            List<String> automataNames = new ArrayList<>();

            Matcher m1 = PATTERN_FOR_AN_AUTOMATON_IN_union_COMMAND.matcher(m.group(GROUP_UNION_AUTOMATA));
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

            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the union command.");
        }
    }

    public static TestCase intersectCommand(String s) {
        try {

            Matcher m = PATTERN_FOR_intersect_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("intersect");
            }

            boolean printSteps = m.group(GROUP_INTERSECT_END).equals(":");
            boolean printDetails = m.group(GROUP_INTERSECT_END).equals("::");

            String prefix = "";
            StringBuilder log = new StringBuilder();


            List<String> automataNames = new ArrayList<>();

            Matcher m1 = PATTERN_FOR_AN_AUTOMATON_IN_intersect_COMMAND.matcher(m.group(GROUP_INTERSECT_AUTOMATA));
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

            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the intersect command.");
        }
    }


    public static TestCase starCommand(String s) {
        try {

            Matcher m = PATTERN_FOR_star_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("star");
            }

            boolean printSteps = m.group(GROUP_STAR_END).equals(":");
            boolean printDetails = m.group(GROUP_STAR_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_STAR_OLD_NAME) + ".txt"));

            Automaton C = M.star(printSteps || printDetails, prefix, log);

            writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_STAR_NEW_NAME), false);
            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the star command.");
        }
    }

    public static TestCase concatCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_concat_COMMAND.matcher(s);
            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("concat");
            }

            boolean printSteps = m.group(GROUP_CONCAT_END).equals(":");
            boolean printDetails = m.group(GROUP_CONCAT_END).equals("::");

            String prefix = "";
            StringBuilder log = new StringBuilder();


            List<String> automataNames = new ArrayList<>();

            Matcher m1 = PATTERN_FOR_AN_AUTOMATON_IN_concat_COMMAND.matcher(m.group(GROUP_CONCAT_AUTOMATA));
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

            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the concat command.");
        }
    }


    public static TestCase rightquoCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_rightquo_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("rightquo");
            }

            boolean printSteps = m.group(GROUP_rightquo_END).equals(":");
            boolean printDetails = m.group(GROUP_rightquo_END).equals("::");

            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M1 = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_rightquo_OLD_NAME1) + ".txt"));
            Automaton M2 = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_rightquo_OLD_NAME2) + ".txt"));

            Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false, printSteps || printDetails, prefix, log);

            writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_rightquo_NEW_NAME), false);
            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the rightquo command");
        }
    }

    public static TestCase leftquoCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_leftquo_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("leftquo");
            }

            boolean printSteps = m.group(GROUP_leftquo_END).equals(":");
            boolean printDetails = m.group(GROUP_leftquo_END).equals("::");

            String prefix = "";
            StringBuilder log = new StringBuilder();

            Automaton M1 = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_leftquo_OLD_NAME1) + ".txt"));
            Automaton M2 = new Automaton(
                Session.getReadFileForAutomataLibrary(m.group(GROUP_leftquo_OLD_NAME2) + ".txt"));

            Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2, printSteps || printDetails, prefix, log);

            writeAutomata(s, C, Session.getWriteAddressForAutomataLibrary(), m.group(GROUP_leftquo_NEW_NAME), false);
            return new TestCase(C, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the leftquo command");
        }
    }

    public static TestCase drawCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_draw_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("draw");
            }

            String inFileName = m.group(GROUP_draw_NAME) + ".txt";
            String inLibrary = Session.getReadFileForWordsLibrary(inFileName);
            if (m.group(GROUP_draw_DOLLAR_SIGN).equals("$")) {
                inLibrary = Session.getReadFileForAutomataLibrary(inFileName);
            }
            Automaton M = new Automaton(inLibrary);
            AutomatonWriter.draw(M, Session.getAddressForResult() + m.group(GROUP_draw_NAME) + ".gv", s, false);

            return new TestCase(M, "", "", "");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the draw command");
        }
    }

    public static void helpCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_help_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("help");
            }

            String helpAddress = Session.getAddressForHelpCommands();
            File f = new File(helpAddress);

            ArrayList<String> pathnames = new ArrayList<>(Arrays.asList(f.list()));

            String commandName = m.group(GROUP_help_NAME);
            if (commandName == null) {
                // default help message

                System.out.println("Walnut provides documentation for the following commands.\nType \"help <command>;\" to view documentation for a specific command.");
                for (String pathname : pathnames) {
                    System.out.println(" - " + pathname.substring(0, pathname.length() - 4));
                }
            } else {
                // help with a specific command.
                int index = pathnames.indexOf(commandName + ".txt");
                if (index == -1) {
                    System.out.println("There is no documentation for \"" + commandName + "\". Type \"help;\" to list all commands.");
                } else {
                    try (BufferedReader br = new BufferedReader(new FileReader(helpAddress + commandName + ".txt"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error using the help command");
        }
    }


    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void determineMatricesToCompute(String s, List<String> L) {
        Matcher m1 = PATTERN_FOR_A_FREE_VARIABLE_IN_eval_def_COMMANDS.matcher(s);
        while (m1.find()) {
            String t = m1.group();
            L.add(t);
        }
    }

    private static List<Integer> determineAlphabet(String s) {
        List<Integer> L = new ArrayList<>();
        s = s.substring(1, s.length() - 1); //truncation { and } from beginning and end
        Matcher m = PATTERN_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(s);
        while (m.find()) {
            L.add(UtilityMethods.parseInt(m.group()));
        }
        UtilityMethods.removeDuplicates(L);

        return L;
    }

    private static Automaton removeLeadTrailZeroes(Automaton M, String name) {
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
