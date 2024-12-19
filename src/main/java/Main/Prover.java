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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class contains the main method. It is responsible to get a command from user
 * and parse and dispatch the command appropriately.
 *
 * @author Hamoon
 */
public class Prover {
    private static final Logger LOGGER = LogManager.getLogger(Prover.class);

    static Pattern PATTERN_FOR_THE_LIST_OF_COMMANDS = Pattern.compile(
        "(eval|def|macro|reg|load|ost|exit|quit|cls|clear|combine|morphism|promote|image|inf|split|rsplit|join|test|transduce|reverse|minimize|convert|fixleadzero|fixtrailzero|alphabet|union|intersect|star|concat|rightquo|leftquo|draw|help)");
    static Pattern PATTERN_FOR_EMPTY_COMMAND = Pattern.compile("^\\s*(;|::|:)\\s*$");
    /**
     * the high-level scheme of a command is a name followed by some arguments and ending in either ; : or ::
     */
    static String REGEXP_FOR_COMMAND = "^\\s*(\\w+)(\\s+.*)?(;|::|:)\\s*$";
    static Pattern PATTERN_FOR_COMMAND = Pattern.compile(REGEXP_FOR_COMMAND);

    static Pattern PATTERN_FOR_EXIT_COMMAND = Pattern.compile("^\\s*(exit|quit)\\s*(;|::|:)$");

    /**
     * group for filename in REGEXP_FOR_load_COMMAND
     */
    static int L_FILENAME = 1;
    static Pattern PATTERN_FOR_LOAD_COMMAND = Pattern.compile("^\\s*load\\s+(\\w+\\.txt)\\s*(;|::|:)\\s*$");

    /**
     * important groups in REGEXP_FOR_eval_def_COMMANDS
     */
    static int ED_TYPE = 1, ED_NAME = 2, ED_FREE_VARIABLES = 3, ED_PREDICATE = 6, ED_ENDING = 7;
    static Pattern PATTERN_FOR_eval_def_COMMANDS = Pattern.compile("^\\s*(eval|def)\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s+\"(.*)\"\\s*(;|::|:)\\s*$");
    static Pattern PATTERN_FOR_A_FREE_VARIABLE_IN_eval_def_COMMANDS = Pattern.compile("[a-zA-Z]\\w*");

    static int M_NAME = 1, M_DEFINITION = 2;
    static Pattern PATTERN_FOR_macro_COMMAND = Pattern.compile("^\\s*macro\\s+([a-zA-Z]\\w*)\\s+\"(.*)\"\\s*(;|::|:)\\s*$");

    /**
     * important groups in REGEXP_FOR_reg_COMMAND
     */
    static int R_NAME = 2, R_LIST_OF_ALPHABETS = 3, R_REGEXP = 20;
    static Pattern PATTERN_FOR_reg_COMMAND = Pattern.compile(
        "^\\s*(reg)\\s+([a-zA-Z]\\w*)\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)\"(.*)\"\\s*(;|::|:)\\s*$");
    static Pattern PATTERN_FOR_A_SINGLE_ELEMENT_OF_A_SET = Pattern.compile("(\\+|\\-)?\\s*\\d+");
    static Pattern PATTERN_FOR_AN_ALPHABET =
        Pattern.compile("((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+");
    static int R_NUMBER_SYSTEM = 2, R_SET = 11;

    static Pattern PATTERN_FOR_AN_ALPHABET_VECTOR =
        Pattern.compile("(\\[(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\])|(\\d)");

    static Pattern PATTERN_FOR_ost_COMMAND =
        Pattern.compile("^\\s*ost\\s+([a-zA-Z]\\w*)\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*\\[\\s*((\\d+\\s*)*)\\]\\s*(;|:|::)\\s*$");
    static int GROUP_OST_NAME = 1;
    static int GROUP_OST_PREPERIOD = 2;
    static int GROUP_OST_PERIOD = 4;
    static int GROUP_OST_END = 6;

    static Pattern PATTERN_FOR_combine_COMMAND = Pattern.compile("^\\s*combine\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*(=-?\\d+)?))*)\\s*(;|::|:)\\s*$");
    static int GROUP_COMBINE_NAME = 1, GROUP_COMBINE_AUTOMATA = 2, GROUP_COMBINE_END = 6;
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_combine_COMMAND = Pattern.compile("([a-zA-Z]\\w*)((=-?\\d+)?)");

    static Pattern PATTERN_FOR_morphism_COMMAND = Pattern.compile(
        "^\\s*morphism\\s+([a-zA-Z]\\w*)\\s+\"(\\d+\\s*\\-\\>\\s*(.)*(,\\d+\\s*\\-\\>\\s*(.)*)*)\"\\s*(;|::|:)\\s*$");
    static int GROUP_MORPHISM_NAME = 1, GROUP_MORPHISM_DEFINITION;

    static Pattern PATTERN_FOR_promote_COMMAND =
        Pattern.compile("^\\s*promote\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_PROMOTE_NAME = 1, GROUP_PROMOTE_MORPHISM = 2;

    static Pattern PATTERN_FOR_image_COMMAND =
        Pattern.compile("^\\s*image\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_IMAGE_NEW_NAME = 1, GROUP_IMAGE_MORPHISM = 2, GROUP_IMAGE_OLD_NAME = 3;

    static Pattern PATTERN_FOR_inf_COMMAND = Pattern.compile("^\\s*inf\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_INF_NAME = 1;

    static Pattern PATTERN_FOR_split_COMMAND =
        Pattern.compile("^\\s*split\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[+-]?\\s*])+)\\s*(;|::|:)\\s*$");
    static int GROUP_SPLIT_NAME = 1, GROUP_SPLIT_AUTOMATA = 2, GROUP_SPLIT_INPUT = 3, GROUP_SPLIT_END = 5;
    static Pattern PATTERN_FOR_INPUT_IN_split_COMMAND = Pattern.compile("\\[\\s*([+-]?)\\s*]");

    static Pattern PATTERN_FOR_rsplit_COMMAND = Pattern.compile(
        "^\\s*rsplit\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[+-]?\\s*])+)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_RSPLIT_NAME = 1, GROUP_RSPLIT_AUTOMATA = 4, GROUP_RSPLIT_INPUT = 2, GROUP_RSPLIT_END = 5;
    static Pattern PATTERN_FOR_INPUT_IN_rsplit_COMMAND = Pattern.compile("\\[\\s*([+-]?)\\s*]");

    static Pattern PATTERN_FOR_join_COMMAND = Pattern.compile(
        "^\\s*join\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*)((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+))*)\\s*(;|::|:)\\s*");
    static int GROUP_JOIN_NAME = 1, GROUP_JOIN_AUTOMATA = 2, GROUP_JOIN_END = 7;
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_join_COMMAND = Pattern.compile(
        "([a-zA-Z]\\w*)((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+)");
    static int GROUP_JOIN_AUTOMATON_NAME = 1, GROUP_JOIN_AUTOMATON_INPUT = 2;
    static Pattern PATTERN_FOR_AN_AUTOMATON_INPUT_IN_join_COMMAND = Pattern.compile(
        "\\[\\s*([a-zA-Z&&[^AE]]\\w*)\\s*]");

    static Pattern PATTERN_FOR_test_COMMAND = Pattern.compile(
        "^\\s*test\\s+([a-zA-Z]\\w*)\\s*(\\d+)\\s*(;|::|:)\\s*$");
    static int GROUP_TEST_NAME = 1, GROUP_TEST_NUM = 2;

    static Pattern PATTERN_FOR_transduce_COMMAND = Pattern.compile(
        "^\\s*transduce\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_TRANSDUCE_NEW_NAME = 1, GROUP_TRANSDUCE_TRANSDUCER = 2,
            GROUP_TRANSDUCE_DOLLAR_SIGN = 3, GROUP_TRANSDUCE_OLD_NAME = 4, GROUP_TRANSDUCE_END = 5;

    static Pattern PATTERN_FOR_reverse_COMMAND = Pattern.compile(
        "^\\s*reverse\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_REVERSE_NEW_NAME = 1, GROUP_REVERSE_DOLLAR_SIGN = 2, GROUP_REVERSE_OLD_NAME = 3, GROUP_REVERSE_END = 4;

    static Pattern PATTERN_FOR_minimize_COMMAND = Pattern.compile(
        "^\\s*minimize\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_MINIMIZE_NEW_NAME = 1, GROUP_MINIMIZE_OLD_NAME = 2, GROUP_MINIMIZE_END = 3;

    static Pattern PATTERN_FOR_convert_COMMAND = Pattern.compile(
        "^\\s*convert\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s+((msd|lsd)_(\\d+))\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_CONVERT_NEW_NAME = 2, GROUP_CONVERT_OLD_NAME = 7, GROUP_CONVERT_END = 8,
            GROUP_CONVERT_NEW_DOLLAR_SIGN = 1, GROUP_CONVERT_OLD_DOLLAR_SIGN = 6,
            GROUP_CONVERT_NUMBER_SYSTEM = 3, GROUP_CONVERT_MSD_OR_LSD = 4,
            GROUP_CONVERT_BASE = 5;

    static Pattern PATTERN_FOR_fixleadzero_COMMAND = Pattern.compile(
        "^\\s*fixleadzero\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_FIXLEADZERO_NEW_NAME = 1, GROUP_FIXLEADZERO_DOLLAR_SIGN = 2, GROUP_FIXLEADZERO_OLD_NAME = 3, GROUP_FIXLEADZERO_END = 4;

    static Pattern PATTERN_FOR_fixtrailzero_COMMAND = Pattern.compile(
        "^\\s*fixtrailzero\\s+([a-zA-Z]\\w*)\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_FIXTRAILZERO_NEW_NAME = 1, GROUP_FIXTRAILZERO_DOLLAR_SIGN = 2, GROUP_FIXTRAILZERO_OLD_NAME = 3, GROUP_FIXTRAILZERO_END = 4;

    static Pattern PATTERN_FOR_alphabet_COMMAND = Pattern.compile(
        "^\\s*(alphabet)\\s+([a-zA-Z]\\w*)\\s+((((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+)+)(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_alphabet_NEW_NAME = 2, GROUP_alphabet_LIST_OF_ALPHABETS = 3, GROUP_alphabet_DOLLAR_SIGN = 20, GROUP_alphabet_OLD_NAME = 21, GROUP_alphabet_END = 22;

    static Pattern PATTERN_FOR_union_COMMAND = Pattern.compile(
        "^\\s*union\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$");
    static int GROUP_UNION_NAME = 1, GROUP_UNION_AUTOMATA = 2, GROUP_UNION_END = 5;
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_union_COMMAND = Pattern.compile("([a-zA-Z]\\w*)");

    static Pattern PATTERN_FOR_intersect_COMMAND = Pattern.compile(
        "^\\s*intersect\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$");
    static int GROUP_INTERSECT_NAME = 1, GROUP_INTERSECT_AUTOMATA = 2, GROUP_INTERSECT_END = 5;
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_intersect_COMMAND = Pattern.compile("([a-zA-Z]\\w*)");

    static Pattern PATTERN_FOR_star_COMMAND = Pattern.compile(
        "^\\s*star\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_STAR_NEW_NAME = 1, GROUP_STAR_OLD_NAME = 2, GROUP_STAR_END = 3;

    static Pattern PATTERN_FOR_concat_COMMAND = Pattern.compile(
        "^\\s*concat\\s+([a-zA-Z]\\w*)((\\s+([a-zA-Z]\\w*))*)\\s*(;|::|:)\\s*$");
    static int GROUP_CONCAT_NAME = 1, GROUP_CONCAT_AUTOMATA = 2, GROUP_CONCAT_END = 5;
    static Pattern PATTERN_FOR_AN_AUTOMATON_IN_concat_COMMAND = Pattern.compile("([a-zA-Z]\\w*)");

    static Pattern PATTERN_FOR_rightquo_COMMAND = Pattern.compile(
        "^\\s*rightquo\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_rightquo_NEW_NAME = 1, GROUP_rightquo_OLD_NAME1 = 2, GROUP_rightquo_OLD_NAME2 = 3, GROUP_rightquo_END = 4;

    static Pattern PATTERN_FOR_leftquo_COMMAND = Pattern.compile(
        "^\\s*leftquo\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s+([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_leftquo_NEW_NAME = 1, GROUP_leftquo_OLD_NAME1 = 2, GROUP_leftquo_OLD_NAME2 = 3, GROUP_leftquo_END = 4;

    static Pattern PATTERN_FOR_draw_COMMAND = Pattern.compile(
        "^\\s*draw\\s+(\\$|\\s*)([a-zA-Z]\\w*)\\s*(;|::|:)\\s*$");
    static int GROUP_draw_DOLLAR_SIGN = 1, GROUP_draw_NAME = 2, GROUP_draw_END = 3;

    static Pattern PATTERN_FOR_help_COMMAND = Pattern.compile(
        "^\\s*help(\\s*|\\s+(\\w*))\\s*(;|::|:)\\s*$");
    static int GROUP_help_NAME = 2, GROUP_help_END = 3;

    /**
     * if the command line argument is not empty, we treat args[0] as a filename.
     * if this is the case, we read from the file and load its commands before we submit control to user.
     * if the the address is not a valid address or the file does not exist, we print an appropriate error message
     * and submit control to the user.
     * if the file contains the exit command we terminate the program.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        UtilityMethods.setPaths();

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
        if (args.length >= 1) {
            // Reading commands from the file with address args[0]
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(
                        UtilityMethods.get_address_for_command_files() + args[0]),
                    StandardCharsets.UTF_8))) {

                if (!readBuffer(in, false)) {
                    return;
                }
            } catch (IOException e) {
                System.out.flush();
                System.err.println(e.getMessage());
            }
        }

        // Now we parse commands from the console.
        LOGGER.info("Welcome to Walnut! Type \"help;\" to see all available commands.");
        readBuffer(new BufferedReader(new InputStreamReader(System.in)), true);
    }

    /**
     * Takes a BufferedReader and reads from it until we hit end of file or exit command.
     *
     * @param in
     * @param console = true if in = System.in
     * @return
     */
    public static boolean readBuffer(BufferedReader in, boolean console) {
        StringBuilder buffer = new StringBuilder();
        try {
            while (true) {
                if (console) {
                    System.out.print(UtilityMethods.PROMPT);
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
                        LOGGER.info(s);
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
     * If both delimiters are present, the smaller index is returned. If only one
     * delimiter is present, its index is returned. If no delimiters are found, -1 is returned.
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
        Object result = dispatchCommand(s);
        // In the context of dispatch, return true for non-null, false for null
        return result != null;
    }

    public static TestCase dispatchForIntegrationTest(String s) throws IOException {
        Object result = dispatchCommand(s);
        // In the context of integration tests, return TestCase or null
        return (TestCase) result;
    }

    private static Object dispatchCommand(String s) throws IOException {
        if (PATTERN_FOR_EMPTY_COMMAND.matcher(s).matches()) {
            // If the command is just ; or :, do nothing
            return null;
        }

        Matcher matcher_for_command = PATTERN_FOR_COMMAND.matcher(s);
        if (!matcher_for_command.find()) {
            throw ExceptionHelper.invalidCommand();
        }

        String commandName = matcher_for_command.group(1);
        if (!PATTERN_FOR_THE_LIST_OF_COMMANDS.matcher(commandName).matches()) {
            throw ExceptionHelper.noSuchCommand();
        }

        switch (commandName) {
            case "exit", "quit" -> {
                if (PATTERN_FOR_EXIT_COMMAND.matcher(s).matches()) {
                    return null; // exit/quit leads to null in both cases
                }
                throw ExceptionHelper.invalidCommand();
            }
            case "load" -> loadCommand(s);
            case "eval", "def" -> {
                return eval_def_commands(s); // Returns a TestCase or an object specific to eval/def
            }
            case "macro" -> {
                return macroCommand(s);
            }
            case "reg" -> {
                return regCommand(s);
            }
            case "ost" -> ostCommand(s);
            case "cls", "clear" -> {
                clearScreen(); // Clears the screen; no return value
                return new Object(); // Non-null to indicate success
            }
            case "combine" -> {
                return combineCommand(s);
            }
            case "morphism" -> morphismCommand(s);
            case "promote" -> {
                return promoteCommand(s);
            }
            case "image" -> {
                return imageCommand(s);
            }
            case "inf" -> {
                return infCommand(s);
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
            case "test" -> testCommand(s);
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
        return new Object(); // Default success indicator
    }


    /**
     * load x.p; loads commands from the file x.p. The file can contain any command except for load x.p;
     * The user don't get a warning if the x.p contains load x.p but the program might end up in an infinite loop.
     * Note that the file can contain load y.p; whenever y != x and y exist.
     *
     * @param s
     */
    public static void loadCommand(String s) {
        Matcher m = PATTERN_FOR_LOAD_COMMAND.matcher(s);
        if (!m.find()) throw ExceptionHelper.invalidCommandUse("load");
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(
                                    UtilityMethods.get_address_for_command_files() +
                                            m.group(L_FILENAME)),
                        StandardCharsets.UTF_8));
            readBuffer(in, false);
        } catch (IOException e) {
            System.out.flush();
            System.err.println(e.getMessage());
            System.err.flush();
        }
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
        c.write(UtilityMethods.get_address_for_result() + m.group(ED_NAME) + ".txt");
        c.drawAutomaton(UtilityMethods.get_address_for_result() + m.group(ED_NAME) + ".gv");

        if (!free_variables.isEmpty()) {
            c.mpl = AutomatonWriter.write_matrices(c.getTheFinalResult(),
                UtilityMethods.get_address_for_result() + m.group(ED_NAME) + ".mpl", free_variables);
        }

        c.writeLog(UtilityMethods.get_address_for_result() + m.group(ED_NAME) + "_log.txt");
        if (printDetails) {
            c.writeDetailedLog(
                    UtilityMethods.get_address_for_result() + m.group(ED_NAME) + "_detailed_log.txt");
        }

        if (m.group(ED_TYPE).equals("def")) {
            c.write(UtilityMethods.get_address_for_automata_library() + m.group(ED_NAME) + ".txt");
        }

        M = c.getTheFinalResult();
        if (M.TRUE_FALSE_AUTOMATON) {
            if (M.TRUE_AUTOMATON) {
                LOGGER.info("____\nTRUE");
            } else {
                LOGGER.info("_____\nFALSE");
            }
        }

        return new TestCase(s, M, "", c.mpl, printDetails ? c.log_details.toString() : "");
    }

    public static TestCase macroCommand(String s) {
        Matcher m = PATTERN_FOR_macro_COMMAND.matcher(s);
        if (!m.find()) throw ExceptionHelper.invalidCommandUse("macro");
        try (BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(
                    UtilityMethods.get_address_for_macro_library() + m.group(M_NAME) + ".txt"),
                StandardCharsets.UTF_8))) {
            out.write(m.group(M_DEFINITION));
        } catch (IOException o) {
          LOGGER.info("Could not write the macro {}", m.group(M_NAME), o);
        }
        return null;
    }

    public static TestCase regCommand(String s) {
        Matcher m = PATTERN_FOR_reg_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("reg");
        }
        NumberSystem ns;
        List<List<Integer>> alphabets = new ArrayList<>();
        List<NumberSystem> numSys = new ArrayList<>();
        List<Integer> alphabet;
        if (m.group(R_LIST_OF_ALPHABETS) == null) {
            String base = "msd_2";
            try {
                if (!Predicate.numberSystemHash.containsKey(base))
                    Predicate.numberSystemHash.put(base, new NumberSystem(base));
                ns = Predicate.numberSystemHash.get(base);
                numSys.add(Predicate.numberSystemHash.get(base));
            } catch (RuntimeException e) {
                throw new RuntimeException("number system " + base + " does not exist: char at " + m.start(R_NUMBER_SYSTEM) + System.lineSeparator() + "\t:" + e.getMessage());
            }
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
                try {
                    if (!Predicate.numberSystemHash.containsKey(base))
                        Predicate.numberSystemHash.put(base, new NumberSystem(base));
                    ns = Predicate.numberSystemHash.get(base);
                    numSys.add(Predicate.numberSystemHash.get(base));
                } catch (RuntimeException e) {
                    throw new RuntimeException("number system " + base + " does not exist: char at " + m.start(R_NUMBER_SYSTEM) + System.lineSeparator() + "\t:" + e.getMessage());
                }
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
        M.A = alphabets;
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
            if (L.size() != M.A.size()) {
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
             * Instead, we replace all occurences of "[-2]" with "%PLACEHOLDER%",
             * then run baseexp.replace(alphabetVectorCopy, replacementStr),
             * and then replace "%PLACEHOLDER%" with "[-2]".
             */
            baseexp = baseexp
                    .replace("[-" + alphabetVectorCopy + "]", "§")
                    .replace(alphabetVectorCopy, replacementStr)
                    .replace("§", "[-" + alphabetVectorCopy + "]");
        }
        M.alphabetSize = 1;
        for (List<Integer> alphlist : M.A) {
            M.alphabetSize *= alphlist.size();
        }

        // We should always do this with replacement, since we may have regexes such as "...", which accepts any three characters
        // in a row, on an alphabet containing bracketed characters. We don't make any replacements here, but they are implicitly made
        // when we intersect with our alphabet(s).

        // remove all whitespace from regular expression.
        baseexp = baseexp.replaceAll("\\s", "");

        Automaton R = new Automaton(baseexp, M.A, M.alphabetSize);
        R.A = M.A;
        R.alphabetSize = M.alphabetSize;
        R.NS = numSys;

        AutomatonWriter.draw(R, UtilityMethods.get_address_for_result() + m.group(R_NAME) + ".gv", m.group(R_REGEXP), false);
        AutomatonWriter.write(R, UtilityMethods.get_address_for_result() + m.group(R_NAME) + ".txt");
        AutomatonWriter.write(R, UtilityMethods.get_address_for_automata_library() + m.group(R_NAME) + ".txt");

        return new TestCase(s, R, "", "", "");
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
        Automaton first = new Automaton(UtilityMethods.get_address_for_automata_library() + automataNames.get(0) + ".txt");
        automataNames.remove(0);

        Automaton C = first.combine(automataNames, outputs, printSteps, prefix, log);

        AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_COMBINE_NAME) + ".gv", s, true);
        AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_COMBINE_NAME) + ".txt");
        AutomatonWriter.write(C, UtilityMethods.get_address_for_words_library() + m.group(GROUP_COMBINE_NAME) + ".txt");

        return new TestCase(s, C, "", "", "");
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
        M.write(UtilityMethods.get_address_for_result() + name + ".txt");
        M.write(UtilityMethods.get_address_for_morphism_library() + name + ".txt");
    }

    public static TestCase promoteCommand(String s) throws IOException {
        Matcher m = PATTERN_FOR_promote_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("prmote");
        }
        Morphism h = new Morphism(UtilityMethods.get_address_for_morphism_library() + m.group(GROUP_PROMOTE_MORPHISM) + ".txt");
        Automaton P = h.toWordAutomaton();
        AutomatonWriter.draw(P, UtilityMethods.get_address_for_result() + m.group(GROUP_PROMOTE_NAME) + ".gv", s, true);
        AutomatonWriter.write(P, UtilityMethods.get_address_for_result() + m.group(GROUP_PROMOTE_NAME) + ".txt");
        AutomatonWriter.write(P, UtilityMethods.get_address_for_words_library() + m.group(GROUP_PROMOTE_NAME) + ".txt");

        return new TestCase(s, P, "", "", "");
    }

    public static TestCase imageCommand(String s) throws IOException {
        Matcher m = PATTERN_FOR_image_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("image");
        }
        Morphism h = new Morphism(UtilityMethods.get_address_for_morphism_library() + m.group(GROUP_IMAGE_MORPHISM) + ".txt");
        if (!h.isUniform()) {
            throw new RuntimeException("A morphism applied to a word automaton must be uniform.");
        }
        String combineString = "combine " + m.group(GROUP_IMAGE_NEW_NAME);

        // We need to know the number system of our old automaton: the new one should match, as should intermediary expressions
        Automaton M = new Automaton(UtilityMethods.get_address_for_words_library() + m.group(GROUP_IMAGE_OLD_NAME) + ".txt");
        String numSysName = "";
        if (!M.NS.isEmpty()) {
            numSysName = M.NS.get(0).toString();
        }

        // we construct a define command for a DFA for each x that accepts iff x appears at the nth position
        for (Integer value : h.range) {
            eval_def_commands(h.makeInterCommand(value, m.group(GROUP_IMAGE_OLD_NAME), numSysName));
            combineString += " " + m.group(GROUP_IMAGE_OLD_NAME) + "_" + value + "=" + value;
        }
        combineString += ":";

        TestCase retrieval = combineCommand(combineString);
        Automaton I = retrieval.result.clone();

        AutomatonWriter.draw(I, UtilityMethods.get_address_for_result() + m.group(GROUP_IMAGE_NEW_NAME) + ".gv", s, true);
        AutomatonWriter.write(I, UtilityMethods.get_address_for_result() + m.group(GROUP_IMAGE_NEW_NAME) + ".txt");
        AutomatonWriter.write(I, UtilityMethods.get_address_for_words_library() + m.group(GROUP_IMAGE_NEW_NAME) + ".txt");
        return new TestCase(s, I, "", "", "");
    }

    public static boolean infCommand(String s) {
        Matcher m = PATTERN_FOR_inf_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("inf");
        }
        Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_INF_NAME) + ".txt");
        M = removeLeadTrailZeroes(M, m.group(GROUP_INF_NAME));
        String infReg = M.infinite();
        if (infReg == "") {
            LOGGER.info("Automaton " + m.group(GROUP_INF_NAME) + " accepts finitely many values.");
            return false;
        } else {
            LOGGER.info(infReg);
            return true;
        }
    }

    public static TestCase splitCommand(String s) {
        Matcher m = PATTERN_FOR_split_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("split");
        }
        String addressForWordAutomaton
                = UtilityMethods.get_address_for_words_library() + m.group(GROUP_SPLIT_AUTOMATA) + ".txt";
        String addressForAutomaton
                = UtilityMethods.get_address_for_automata_library() + m.group(GROUP_SPLIT_AUTOMATA) + ".txt";
        Automaton M;
        boolean isDFAO;
        if ((new File(addressForWordAutomaton)).exists()) {
            M = new Automaton(addressForWordAutomaton);
            isDFAO = true;
        } else if ((new File(addressForAutomaton)).exists()) {
            M = new Automaton(addressForAutomaton);
            isDFAO = false;
        } else {
            throw new RuntimeException("Automaton " + m.group(GROUP_SPLIT_AUTOMATA) + " does not exist.");
        }

        boolean printSteps = m.group(GROUP_SPLIT_END).equals(":");
        String prefix = "";
        StringBuilder log = new StringBuilder();

        Matcher m1 = PATTERN_FOR_INPUT_IN_split_COMMAND.matcher(m.group(GROUP_SPLIT_INPUT));
        List<String> inputs = new ArrayList<>();
        boolean hasInput = false;
        while (m1.find()) {
            String t = m1.group(1);
            hasInput = hasInput || t.equals("+") || t.equals("-");
            inputs.add(t);
        }
        if (!hasInput || inputs.isEmpty()) {
            throw new RuntimeException("Cannot split without inputs.");
        }
        IntList outputs = new IntArrayList(M.O);
        UtilityMethods.removeDuplicates(outputs);
        List<Automaton> subautomata = M.uncombine(outputs);
        for (int i = 0; i < subautomata.size(); i++) {
            Automaton N = subautomata.get(i).split(inputs, printSteps, prefix, log);
            subautomata.set(i, N);
        }
        Automaton N = subautomata.remove(0);
        N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printSteps, prefix, log);

        AutomatonWriter.draw(N, UtilityMethods.get_address_for_result() + m.group(GROUP_SPLIT_NAME) + ".gv", s, isDFAO);
        AutomatonWriter.write(N, UtilityMethods.get_address_for_result() + m.group(GROUP_SPLIT_NAME) + ".txt");
        if (isDFAO) {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_words_library() + m.group(GROUP_SPLIT_NAME) + ".txt");
        } else {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_SPLIT_NAME) + ".txt");
        }
        return new TestCase(s, N, "", "", "");
    }

    public static TestCase rsplitCommand(String s) {
        Matcher m = PATTERN_FOR_rsplit_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("reverse split");
        }
        String addressForWordAutomaton
                = UtilityMethods.get_address_for_words_library() + m.group(GROUP_RSPLIT_AUTOMATA) + ".txt";
        String addressForAutomaton
                = UtilityMethods.get_address_for_automata_library() + m.group(GROUP_RSPLIT_AUTOMATA) + ".txt";
        Automaton M;
        boolean isDFAO;
        if ((new File(addressForWordAutomaton)).exists()) {
            M = new Automaton(addressForWordAutomaton);
            isDFAO = true;
        } else if ((new File(addressForAutomaton)).exists()) {
            M = new Automaton(addressForAutomaton);
            isDFAO = false;
        } else {
            throw new RuntimeException("Automaton " + m.group(GROUP_RSPLIT_AUTOMATA) + " does not exist.");
        }

        boolean printSteps = m.group(GROUP_RSPLIT_END).equals(":");
        String prefix = "";
        StringBuilder log = new StringBuilder();

        Matcher m1 = PATTERN_FOR_INPUT_IN_rsplit_COMMAND.matcher(m.group(GROUP_RSPLIT_INPUT));
        List<String> inputs = new ArrayList<>();
        boolean hasInput = false;
        while (m1.find()) {
            String t = m1.group(1);
            hasInput = hasInput || t.equals("+") || t.equals("-");
            inputs.add(t);
        }
        if (!hasInput || inputs.isEmpty()) {
            throw new RuntimeException("Cannot split without inputs.");
        }
        IntList outputs = new IntArrayList(M.O);
        UtilityMethods.removeDuplicates(outputs);
        List<Automaton> subautomata = M.uncombine(outputs);
        for (int i = 0; i < subautomata.size(); i++) {
            Automaton N = subautomata.get(i).reverseSplit(inputs, printSteps, prefix, log);
            subautomata.set(i, N);
        }
        Automaton N = subautomata.remove(0);
        N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printSteps, prefix, log);

        AutomatonWriter.draw(N, UtilityMethods.get_address_for_result() + m.group(GROUP_RSPLIT_NAME) + ".gv", s, isDFAO);
        AutomatonWriter.write(N, UtilityMethods.get_address_for_result() + m.group(GROUP_RSPLIT_NAME) + ".txt");
        if (isDFAO) {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_words_library() + m.group(GROUP_RSPLIT_NAME) + ".txt");
        } else {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_RSPLIT_NAME) + ".txt");
        }
        return new TestCase(s, N, "", "", "");
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
                    = UtilityMethods.get_address_for_words_library() + automatonName + ".txt";
            String addressForAutomaton
                    = UtilityMethods.get_address_for_automata_library() + automatonName + ".txt";
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
            if (label.size() != M.A.size()) {
                throw new RuntimeException("Number of inputs of word automata " + automatonName + " does not match number of inputs specified.");
            }
            M.label = label;
            subautomata.add(M);
        }
        Automaton N = subautomata.remove(0);
        N = N.join(new LinkedList<>(subautomata), printSteps, prefix, log);

        AutomatonWriter.draw(N, UtilityMethods.get_address_for_result() + m.group(GROUP_JOIN_NAME) + ".gv", s, isDFAO);
        AutomatonWriter.write(N, UtilityMethods.get_address_for_result() + m.group(GROUP_JOIN_NAME) + ".txt");
        if (isDFAO) {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_words_library() + m.group(GROUP_JOIN_NAME) + ".txt");
        } else {
            AutomatonWriter.write(N, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_JOIN_NAME) + ".txt");
        }
        return new TestCase(s, N, "", "", "");
    }

    public static void testCommand(String s) {
        Matcher m = PATTERN_FOR_test_COMMAND.matcher(s);
        if (!m.find()) {
            throw ExceptionHelper.invalidCommandUse("test");
        }

        int needed = Integer.parseInt(m.group(GROUP_TEST_NUM));

        // We find the first n inputs accepted by our automaton, lexicographically. If less than n inputs are accepted,
        // we output all that are.
        Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_TEST_NAME) + ".txt");

        // we don't want to count multiple representations of the same value as distinct accepted values
        M = removeLeadTrailZeroes(M, m.group(GROUP_TEST_NAME));

        // We will be intersecting this automaton with various regex automata, so it needs to be labelled.
        M.randomLabel();

        String infSubcommand = "inf " + m.group(GROUP_TEST_NAME) + ";";
        boolean infinite = infCommand(infSubcommand);

        String incLengthReg = "";
        incLengthReg += "reg " + m.group(GROUP_TEST_NAME) + "_len ";
        for (int i = 0; i < M.A.size(); i++) {
            String alphaString = M.A.get(i).toString();
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
            Automaton R = retrieval.result.clone();

            // and-ing automata uses the cross product routine, which requires labeled automata
            R.label = M.label;
            Automaton N = AutomatonLogicalOps.and(M, R, false, null, null);
            N.findAccepted(searchLength, needed - accepted.size());
            accepted.addAll(N.accepted);
            if (accepted.size() >= needed) {
                break;
            }

            // If our automaton accepts finitely many inputs, it does not have a non-redundant cycle, and so the highest length input that could be
            // accepted is equal to the number of states in the automaton
            if (!(infinite) && (searchLength >= M.Q)) {
                break;
            }
        }
        if (accepted.size() < needed) {
            LOGGER.info(m.group(GROUP_TEST_NAME) + " only accepts " + accepted.size() + " inputs, which are as follows: ");
        }
        for (String input : accepted) {
            LOGGER.info(input);
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

            Transducer T = new Transducer(UtilityMethods.get_address_for_transducer_library() + m.group(GROUP_TRANSDUCE_TRANSDUCER) + ".txt");
            String library = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_TRANSDUCE_DOLLAR_SIGN).equals("$")) {
                library = UtilityMethods.get_address_for_automata_library();
            }
            Automaton M = new Automaton(library + m.group(GROUP_TRANSDUCE_OLD_NAME) + ".txt");

            Automaton C = T.transduceNonDeterministic(M, printSteps || printDetails, prefix, log);
            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_TRANSDUCE_NEW_NAME) + ".gv", s, true);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_TRANSDUCE_NEW_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_words_library() + m.group(GROUP_TRANSDUCE_NEW_NAME) + ".txt");
            return new TestCase(s, C, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            String library = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_REVERSE_DOLLAR_SIGN).equals("$")) {
                library = UtilityMethods.get_address_for_automata_library();
                isDFAO = false;
            }

            Automaton M = new Automaton(library + m.group(GROUP_REVERSE_OLD_NAME) + ".txt");

            if (isDFAO) {
                AutomatonLogicalOps.reverseWithOutput(M, true, printSteps || printDetails, prefix, log);
            } else {
                AutomatonLogicalOps.reverse(M, printSteps || printDetails, prefix, log, true);
            }

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_REVERSE_NEW_NAME) + ".gv", s, true);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_REVERSE_NEW_NAME) + ".txt");
            AutomatonWriter.write(M, library + m.group(GROUP_REVERSE_NEW_NAME) + ".txt");
            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M = new Automaton(UtilityMethods.get_address_for_words_library() +
                    m.group(GROUP_MINIMIZE_OLD_NAME) + ".txt");

            M.minimizeSelfWithOutput(printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_MINIMIZE_NEW_NAME) + ".gv", s, true);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_MINIMIZE_NEW_NAME) + ".txt");
            AutomatonWriter.write(M, UtilityMethods.get_address_for_words_library() + m.group(GROUP_MINIMIZE_NEW_NAME) + ".txt");
            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            String library = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_CONVERT_OLD_DOLLAR_SIGN).equals("$")) {
                library = UtilityMethods.get_address_for_automata_library();
            }
            Automaton M = new Automaton(library + m.group(GROUP_CONVERT_OLD_NAME) + ".txt");

            AutomatonLogicalOps.convertNS(M, m.group(GROUP_CONVERT_MSD_OR_LSD).equals("msd"),
                    Integer.parseInt(m.group(GROUP_CONVERT_BASE)), printSteps || printDetails,
                    prefix, log);

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_CONVERT_NEW_NAME) + ".gv", s, true);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_CONVERT_NEW_NAME) + ".txt");

            String outLibrary = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_CONVERT_NEW_DOLLAR_SIGN).equals("$")) {
                outLibrary = UtilityMethods.get_address_for_automata_library();
            }
            AutomatonWriter.write(M, outLibrary + m.group(GROUP_CONVERT_NEW_NAME) + ".txt");
            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_FIXLEADZERO_OLD_NAME) + ".txt");

            AutomatonLogicalOps.fixLeadingZerosProblem(M, printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_FIXLEADZERO_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_FIXLEADZERO_NEW_NAME) + ".txt");
            AutomatonWriter.write(M, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_FIXLEADZERO_NEW_NAME) + ".txt");
            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_FIXTRAILZERO_OLD_NAME) + ".txt");

            AutomatonLogicalOps.fixTrailingZerosProblem(M, printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_FIXTRAILZERO_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_FIXTRAILZERO_NEW_NAME) + ".txt");
            AutomatonWriter.write(M, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_FIXTRAILZERO_NEW_NAME) + ".txt");
            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            NumberSystem ns;
            List<List<Integer>> alphabets = new ArrayList<>();
            List<NumberSystem> numSys = new ArrayList<>();
            List<Integer> alphabet;

            boolean printSteps = m.group(GROUP_alphabet_END).equals(":");
            boolean printDetails = m.group(GROUP_alphabet_END).equals("::");
            String prefix = "";
            StringBuilder log = new StringBuilder();

            boolean isDFAO = true;

            String library = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_alphabet_DOLLAR_SIGN).equals("$")) {
                library = UtilityMethods.get_address_for_automata_library();
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
                    try {
                        if (!Predicate.numberSystemHash.containsKey(base))
                            Predicate.numberSystemHash.put(base, new NumberSystem(base));
                        ns = Predicate.numberSystemHash.get(base);
                        numSys.add(Predicate.numberSystemHash.get(base));
                    } catch (RuntimeException e) {
                        throw new RuntimeException("number system " + base + " does not exist: char at " + m.start(R_NUMBER_SYSTEM) + System.lineSeparator() + "\t:" + e.getMessage());
                    }
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

            Automaton M = new Automaton(library + m.group(GROUP_alphabet_OLD_NAME) + ".txt");

            // here, call the function to set the number system.
            M.setAlphabet(isDFAO, numSys, alphabets, printDetails || printSteps, prefix, log);

            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_alphabet_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(M, UtilityMethods.get_address_for_result() + m.group(GROUP_alphabet_NEW_NAME) + ".txt");
            AutomatonWriter.write(M, library + m.group(GROUP_alphabet_NEW_NAME) + ".txt");

            return new TestCase(s, M, "", "", "");
        } catch (RuntimeException e) {
            LOGGER.catching(e);
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
            Automaton C = new Automaton(UtilityMethods.get_address_for_automata_library() + automataNames.get(0) + ".txt");

            automataNames.remove(0);

            C = C.unionOrIntersect(automataNames, "union", printDetails || printSteps, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_UNION_NAME) + ".gv", s, true);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_UNION_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_UNION_NAME) + ".txt");

            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
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
            Automaton C = new Automaton(UtilityMethods.get_address_for_automata_library() + automataNames.get(0) + ".txt");

            automataNames.remove(0);

            C = C.unionOrIntersect(automataNames, "intersect", printDetails || printSteps, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_INTERSECT_NAME) + ".gv", s, true);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_INTERSECT_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_INTERSECT_NAME) + ".txt");

            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_STAR_OLD_NAME) + ".txt");

            Automaton C = M.star(printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_STAR_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_STAR_NEW_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_STAR_NEW_NAME) + ".txt");
            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
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
            Automaton C = new Automaton(UtilityMethods.get_address_for_automata_library() + automataNames.get(0) + ".txt");

            automataNames.remove(0);

            C = C.concat(automataNames, printDetails || printSteps, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_CONCAT_NAME) + ".gv", s, true);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_CONCAT_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_CONCAT_NAME) + ".txt");

            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M1 = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_rightquo_OLD_NAME1) + ".txt");
            Automaton M2 = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_rightquo_OLD_NAME2) + ".txt");

            Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false, printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_rightquo_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_rightquo_NEW_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_rightquo_NEW_NAME) + ".txt");
            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
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

            Automaton M1 = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_leftquo_OLD_NAME1) + ".txt");
            Automaton M2 = new Automaton(UtilityMethods.get_address_for_automata_library() + m.group(GROUP_leftquo_OLD_NAME2) + ".txt");

            Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2, printSteps || printDetails, prefix, log);

            AutomatonWriter.draw(C, UtilityMethods.get_address_for_result() + m.group(GROUP_leftquo_NEW_NAME) + ".gv", s, false);
            AutomatonWriter.write(C, UtilityMethods.get_address_for_result() + m.group(GROUP_leftquo_NEW_NAME) + ".txt");
            AutomatonWriter.write(C, UtilityMethods.get_address_for_automata_library() + m.group(GROUP_leftquo_NEW_NAME) + ".txt");
            return new TestCase(s, C, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
            throw new RuntimeException("Error using the leftquo command");
        }
    }

    public static TestCase drawCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_draw_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("draw");
            }

            String library = UtilityMethods.get_address_for_words_library();
            if (m.group(GROUP_draw_DOLLAR_SIGN).equals("$")) {
                library = UtilityMethods.get_address_for_automata_library();
            }
            Automaton M = new Automaton(library + m.group(GROUP_draw_NAME) + ".txt");
            AutomatonWriter.draw(M, UtilityMethods.get_address_for_result() + m.group(GROUP_draw_NAME) + ".gv", s, false);

            return new TestCase(s, M, "", "", "");

        } catch (RuntimeException e) {
            LOGGER.catching(e);
            throw new RuntimeException("Error using the draw command");
        }
    }

    public static void helpCommand(String s) {
        try {
            Matcher m = PATTERN_FOR_help_COMMAND.matcher(s);

            if (!m.find()) {
                throw ExceptionHelper.invalidCommandUse("help");
            }

            File f = new File(UtilityMethods.get_address_for_help_commands());

            ArrayList<String> pathnames = new ArrayList<>(Arrays.asList(f.list()));

            String commandName = m.group(GROUP_help_NAME);
            if (commandName == null) {
                // default help message

                LOGGER.info("Walnut provides documentation for the following commands.\nType \"help <command>;\" to view documentation for a specific command.");
                for (String pathname : pathnames) {
                    LOGGER.info(" - " + pathname.substring(0, pathname.length() - 4));
                }
            } else {
                // help with a specific command.
                int index = pathnames.indexOf(commandName + ".txt");
                if (index == -1) {
                    LOGGER.info("There is no documentation for \"" + commandName + "\". Type \"help;\" to list all commands.");
                } else {
                    try (BufferedReader br = new BufferedReader(new FileReader(UtilityMethods.get_address_for_help_commands() + commandName + ".txt"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            LOGGER.info(line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.catching(e);
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
        // When dealing with enumerating values (eg. inf and test commands), we remove leading zeroes in the case of msd
        // and trailing zeroes in the case of lsd. To do this, we construct a reg subcommand that generates the complement
        // of zero-prefixed strings for msd and zero suffixed strings for lsd, then intersect this with our original automaton.
        M.randomLabel();
        return AutomatonLogicalOps.removeLeadingZeroes(M, M.label, false, null, null);
    }
}
