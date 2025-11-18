package Automata;

import Main.Logging;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.*;
import java.util.*;

import static Automata.ParseMethods.PATTERN_COMMENT;
import static Automata.ParseMethods.PATTERN_WHITESPACE;

public class AutomatonReader {
    private static final int INVALID_STATE = -1;

    static void readAutomaton(Automaton A, String address) {
        File f = new File(address);

        long lineNumber = 0;
        A.setAlphabetSize(1);

        Boolean[] trueFalseSingleton = new Boolean[1];
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            lineNumber = firstParse(A, address, in, lineNumber, trueFalseSingleton);
            if (trueFalseSingleton[0] != null) {
                return;
            }

            int[] pair = new int[2];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            int currentState = INVALID_STATE;
            int currentOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            Map<Integer, Integer> output = new TreeMap<>();
            Map<Integer, Int2ObjectRBTreeMap<IntList>> transitions = new TreeMap<>();

            int Q = 0, q0 = 0;
            Set<Integer> setOfDestinationStates = new HashSet<>();
            boolean outputLongFile = false;

            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                outputLongFile = debugPrintLongFile(address, lineNumber, outputLongFile);

                if (shouldSkipLine(line)) {
                    continue;
                }

                if (ParseMethods.parseStateDeclaration(line, pair)) {
                    Q++;
                    if (currentState == INVALID_STATE) {
                        q0 = pair[0];
                    }

                    currentState = pair[0];
                    currentOutput = pair[1];
                    output.put(currentState, currentOutput);
                    currentStateTransitions = new Int2ObjectRBTreeMap<>();
                    transitions.put(currentState, currentStateTransitions);
                } else if (ParseMethods.parseTransition(line, input, dest)) {
                    validateTransition(A, address, currentState, lineNumber, input);
                    setOfDestinationStates.addAll(dest);
                    List<List<Integer>> inputs = A.richAlphabet.expandWildcard(input);
                    for (List<Integer> i : inputs) {
                        currentStateTransitions.put(A.richAlphabet.encode(i), dest);
                    }
                    input = new ArrayList<>();
                    dest = new IntArrayList();
                } else {
                    throw WalnutException.undefinedStatement(lineNumber, address);
                }
            }
            if (outputLongFile) {
                System.out.println("...finished");
            }

            validateDeclaredStates(setOfDestinationStates, output, address);

            A.fa.setFieldsFromFile(Q, q0, output, transitions);
        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
            throw WalnutException.fileDoesNotExist(address);
        }
    }

    static boolean debugPrintLongFile(String address, long lineNumber, boolean outputLongFile) {
        if (lineNumber % 1000000 == 0) {
            if (!outputLongFile) {
                outputLongFile = true;
                System.out.print("Parsing " + address + " ...");
            }
            System.out.print("line " + lineNumber + "...");
        }
        return outputLongFile;
    }

    static void validateTransition(Automaton automaton, String address, int currentState, long lineNumber, List<Integer> input) {
        if (currentState == INVALID_STATE) {
            throw new WalnutException(
                "Must declare a state before declaring a list of transitions: line " +
                    lineNumber + " of file " + address);
        }

        if (input.size() != automaton.richAlphabet.getA().size()) {
            throw new WalnutException("This automaton requires a " + automaton.richAlphabet.getA().size() +
                "-tuple as input: line " + lineNumber + " of file " + address);
        }
    }

    static long firstParse(Automaton automaton,
                           String address, BufferedReader in, long lineNumber, Boolean[] trueFalseSingleton) throws IOException {
        String line;
        boolean sawHeader = false;

        while ((line = in.readLine()) != null) {
            lineNumber++;

            if (shouldSkipLine(line)) {
                continue;
            }

            // Handle true/false files (automata only)
            if (trueFalseSingleton != null && ParseMethods.parseTrueFalse(line, trueFalseSingleton)) {
                automaton.fa.setTRUE_FALSE_AUTOMATON(true);
                automaton.fa.setTRUE_AUTOMATON(trueFalseSingleton[0]);
                // ensure nothing else follows except comments/whitespace
                while ((line = in.readLine()) != null) {
                    lineNumber++;
                    if (!shouldSkipLine(line)) {
                        throw WalnutException.fileHasConflict(address, lineNumber);
                    }
                }
                return lineNumber; // done
            }

            // Handle alphabet declaration
            if (ParseMethods.parseAlphabetDeclaration(line, automaton.richAlphabet.getA(), automaton.getNS())) {
                for (int i = 0; i < automaton.richAlphabet.getA().size(); i++) {
                    if (automaton.getNS().get(i) != null &&
                        (!automaton.richAlphabet.getA().get(i).contains(0)
                            || !automaton.richAlphabet.getA().get(i).contains(1))) {
                        throw new WalnutException(
                            "The " + (i + 1) + "th input of type arithmetic of the automaton declared in file " +
                                address + " requires 0 and 1 in its input alphabet: line " + lineNumber);
                    }
                    UtilityMethods.removeDuplicates(automaton.richAlphabet.getA().get(i));
                }
                automaton.determineAlphabetSize();
                sawHeader = true;
                break;
            } else {
                throw WalnutException.undefinedStatement(lineNumber, address);
            }
        }

        // raise on empty/comments-only file
        if (!sawHeader) {
            throw WalnutException.fileEmpty(address);
        }

        return lineNumber;
    }

    // Ignore blank and comment (#) lines.
    static boolean shouldSkipLine(String line) {
        return PATTERN_WHITESPACE.matcher(line).matches() || PATTERN_COMMENT.matcher(line).matches();
    }

    static void validateDeclaredStates(Set<Integer> destinationStates, Map<Integer, ?> declaredStates, String address) {
        for (Integer q : destinationStates) {
            if (!declaredStates.containsKey(q)) {
                throw new WalnutException("State " + q + " is used but never declared anywhere in file: " + address);
            }
        }
    }

    public static void readTransducer(Transducer transducer, String address) {
        File f = new File(address);

        long lineNumber = 0;
        transducer.setAlphabetSize(1);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            lineNumber = firstParse(transducer, address, in, lineNumber, null);

            int[] singleton = new int[1];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            List<Integer> output = new ArrayList<>();
            int currentState = INVALID_STATE;
            int currentStateOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            Map<Integer, Integer> currentStateTransitionOutputs = new TreeMap<>();
            Map<Integer, Integer> stateOutput = new TreeMap<>();
            Map<Integer, Int2ObjectRBTreeMap<IntList>> stateTransition = new TreeMap<>();
            Map<Integer, Map<Integer, Integer>> stateTransitionOutput = new TreeMap<>();

            int Q = 0, q0 = 0;
            Set<Integer> setOfDestinationStates = new HashSet<>();
            boolean outputLongFile = false;

            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                outputLongFile = debugPrintLongFile(address, lineNumber, outputLongFile);

                if (shouldSkipLine(line)) {
                    continue;
                }

                if (ParseMethods.parseTransducerStateDeclaration(line, singleton)) {
                    Q++;
                    if (currentState == INVALID_STATE) {
                        q0 = singleton[0];
                    }

                    currentState = singleton[0];
                    currentStateOutput = 0; // state output does not matter for transducers.
                    stateOutput.put(currentState, currentStateOutput);
                    currentStateTransitions = new Int2ObjectRBTreeMap<>();
                    stateTransition.put(currentState, currentStateTransitions);
                    currentStateTransitionOutputs = new TreeMap<>();
                    stateTransitionOutput.put(currentState, currentStateTransitionOutputs);
                } else if (ParseMethods.parseTransducerTransition(line, input, dest, output)) {
                    validateTransition(transducer, address, currentState, lineNumber, input);
                    if (output.size() != 1) {
                        throw new WalnutException("Transducers must have one output for each transition: line "
                            + lineNumber + " of file " + address);
                    }
                    setOfDestinationStates.addAll(dest);
                    List<List<Integer>> inputs = transducer.richAlphabet.expandWildcard(input);
                    for (List<Integer> i : inputs) {
                        currentStateTransitions.put(transducer.richAlphabet.encode(i), dest);
                        currentStateTransitionOutputs.put(transducer.richAlphabet.encode(i), output.get(0));
                    }
                    input = new ArrayList<>();
                    dest = new IntArrayList();
                    output = new ArrayList<>();
                } else {
                    throw WalnutException.undefinedStatement(lineNumber, address);
                }
            }

            validateDeclaredStates(setOfDestinationStates, stateOutput, address);

            transducer.fa.setFieldsFromFile(Q, q0, stateOutput, stateTransition);
            for (int q = 0; q < Q; q++) {
                transducer.sigma.add(stateTransitionOutput.get(q));
            }

        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
            throw WalnutException.fileDoesNotExist(address);
        }
    }

    /**
     * Usually we skip comments. Here we skip everything else and return them.
     */
    public static String readComments(String address) {
        StringBuilder sb = new StringBuilder();
        File f = new File(address);
        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            while ((line = in.readLine()) != null) {
                if (PATTERN_COMMENT.matcher(line).matches()) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
            throw WalnutException.fileDoesNotExist(address);
        }
        return sb.toString().strip();
    }
}