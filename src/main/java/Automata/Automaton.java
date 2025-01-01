
package Automata;

import Automata.FA.FA;
import Main.ExceptionHelper;
import Main.Session;
import Main.UtilityMethods;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import it.unimi.dsi.fastutil.ints.*;

import static Automata.ParseMethods.PATTERN_WHITESPACE;

/**
 * This class can represent different types of automaton: deterministic/non-deterministic and/or automata with output/automata without output.<bf>
 * There are also two special automata: true automaton, which accepts everything, and false automaton, which accepts nothing.
 * To represent true/false automata we use the field members: TRUE_FALSE_AUTOMATON and TRUE_AUTOMATA. <br>
 * Let's forget about special true/false automata, and talk about ordinary automata:
 * Inputs to an ordinary automaton are n-tuples. Each coordinate has its own alphabet. <br>
 * Let's see this by means of an example: <br>
 * Suppose our automaton has 3-tuples as its input: <br>
 * -The field A, which is a list of list of integers, is used to store the alphabets of
 * all these three inputs. For example we might have A = [[1,2],[0,-1,1],[1,3]]. This means that the first input is over alphabet
 * {1,2} and the second one is over {0,-1,1},... <br>
 * Note: Input alphabets are subsets of integers. <br>
 * -Each coordinate also has a type which is stored in T. For example we might have T = [Type.arithmetic,Type.arithmetic,Type.arithmetic],
 * which means all three inputs are of type arithmetic. You can find out what this means in the tutorial.
 * -So in total there are 12 = 2*3*2 different inputs (this number is stored in alphabetSize)
 * for this automaton. Here are two example inputs: (2,-1,3),(1,0,3).
 * We can encode these 3-tuples by the following rule:<br>
 * 0 = (1,0,1) <br>
 * 1 = (2,0,1) <br>
 * 2 = (1,-1,1)<br>
 * ...<br>
 * 11 = (2,1,3)<br>
 * We use this encoded numbers in our representation of automaton to refer to a particular input. For example
 * we might say on (2,1,3) we go from state 5 to state 0 by setting d.get(5) = (11,[5]). We'll see more on d (transition function)
 * -Now what about states? Q stores the number of states. For example when Q = 3, the set of states is {0,1,2}.
 * -Initial state: q0 is the initial state. For example we might have q0 = 1.
 * -Now field member O is an important one: O stores the output of an state. Now in the case of DFA/NFA, a value of non-zero
 * in O means a final state, and a value of zero means a non-final state. So for example we might have O = {1,-1,0} which means
 * that the first two states are final states. In the case of an automaton with output O simply represents output of an state.
 * Continuing with this example, in the case of automaton with output,
 * the first state has output 1, the second has output -1, and the third one has output 0. As you
 * have guessed the output alphabet can be any finite subset of integers. <br>
 * We might want to give labels to inputs. For example if we set label = ["x","y","z"], the label of the first input is "x".
 * Then in future, we can refer to this first input by the label "x". <br>
 * -The transition function is d which is a TreeMap<integer,List<Integer>> for each state. For example we might have
 * d.get(1) = {(0,[0]),(1,[1,2]),...} which means that state 1 goes to state 0 on input 0, and goes to states 1 and 2 on 1,....
 *
 * @author Hamoon
 */
public class Automaton {

    private List<List<Integer>> A;
    private List<Integer> encoder;
    private List<NumberSystem> NS;
    private List<String> label;
    private boolean labelSorted;  // hen true, labels are sorted lexicographically. It is used in sortLabel() method.

    public FA fa; // abstract FA fields

    // for use in the combine command, counts how many products we have taken so far, and hence what to set outputs to
    int combineIndex;

    // for use in the combine command, allows crossProduct to determine what to set outputs to
    IntList combineOutputs;

    /**
     * We would like to give label to inputs. For example we might want to call the first input by a and so on.
     * As an example when label = ["a","b","c"], the label of the first, second, and third inputs are a, b, and c respectively.
     * These labels are then useful when we quantify an automaton. So for example, in a predicate like E a f(a,b,c) we are having an automaton
     * of three inputs, where the first, second, and third inputs are labeled "a","b", and "c". Therefore E a f(a,b,c) says, we want to
     * do an existential quantifier on the first input.
     */ /**
     * Default constructor. It just initializes the field members.
     */
    public List<String> getLabel() {
        return label;
    }

    public Automaton() {
        fa = new FA();
        setA(new ArrayList<>());
        setNS(new ArrayList<>());
        setLabel(new ArrayList<>());
        dk.brics.automaton.Automaton.setMinimization(dk.brics.automaton.Automaton.MINIMIZE_HOPCROFT);
    }

    /**
     * Initializes a special automaton: true or false.
     * A true automaton, is an automaton that accepts everything. A false automaton is an automaton that accepts nothing.
     * Therefore, M and false is false for every automaton M. We also have that M or true is true for every automaton M.
     *
     * @param true_automaton
     */
    public Automaton(boolean true_automaton) {
        fa = new FA();
        fa.setTRUE_FALSE_AUTOMATON(true);
        this.fa.setTRUE_AUTOMATON(true_automaton);
    }

    /**
     * Takes a regular expression and the alphabet for that regular expression and constructs the corresponding automaton.
     * For example if the regularExpression = "01*" and alphabet = [0,1,2], then the resulting automaton accepts
     * words of the form 01* over the alphabet {0,1,2}.<br>
     * <p>
     * We actually compute the automaton for regularExpression intersected with alphabet*.
     * So for example if regularExpression = [^4]* and alphabet is [1,2,4], then the resulting
     * automaton accepts (1|2)*<br>
     * <p>
     * An important thing to note here is that the automaton being constructed
     * with this constructor, has only one input, and it is of type Type.alphabetLetter.
     */
    public Automaton(String regularExpression, List<Integer> alphabet) {
        this();
        if (alphabet == null || alphabet.isEmpty()) throw new RuntimeException("empty alphabet is not accepted");
        long timeBefore = System.currentTimeMillis();
        alphabet = new ArrayList<>(alphabet);
        getNS().add(null);
        UtilityMethods.removeDuplicates(alphabet);
        /**
         * For example if alphabet = {2,4,1} then intersectingRegExp = [241]*
         */
        String intersectingRegExp = "[";
        for (int x : alphabet) {
            if (x < 0 || x > 9) {
                throw new RuntimeException("the input alphabet of an automaton generated from a regular expression must be a subset of {0,1,...,9}");
            }
            intersectingRegExp += x;
        }
        intersectingRegExp += "]*";
        regularExpression = "(" + regularExpression + ")&" + intersectingRegExp;
        dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
        dk.brics.automaton.Automaton M = RE.toAutomaton();
        M.minimize();
        /**
         * Recall that the alphabet is a set and does not allow repeated elements. However, the user might enter the command
         * reg myreg {1,1,0,0,0} "10*"; and therefore alphabet = [1,1,0,0,0]. So we need remove duplicates before we
         * move forward.
         */
        getA().add(alphabet);
        getNS().add(null);
        this.fa.convertBrics(alphabet, M);
        long timeAfter = System.currentTimeMillis();
        String msg = "computed ~:" + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
        System.out.println(msg);
    }

    public Automaton(
            String regularExpression,
            List<Integer> alphabet,
            NumberSystem numSys) {
        this(regularExpression, alphabet);
        getNS().set(0, numSys);
    }

    // This handles the generalised case of vectors such as "[0,1]*[0,0][0,1]"
    public Automaton(String regularExpression, Integer alphabetSize) {
        this();

        if (alphabetSize > ((1 << Character.SIZE) - 1)) {
            throw new RuntimeException("size of input alphabet exceeds the limit of " + ((1 << Character.SIZE) - 1));
        }
        long timeBefore = System.currentTimeMillis();
        String intersectingRegExp = "[";
        for (int x = 0; x < alphabetSize; x++) {
            char nextChar = (char) (128 + x);
            intersectingRegExp += nextChar;
        }
        intersectingRegExp += "]*";
        regularExpression = "(" + regularExpression + ")&" + intersectingRegExp;
        dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
        dk.brics.automaton.Automaton M = RE.toAutomaton();
        M.minimize();
        this.setThisAutomatonToRepresent(M);
        // We added 128 to the encoding of every input vector before to avoid reserved characters, now we subtract it again
        // to get back the standard encoding
        this.fa.addOffsetToInputs(-128);
        long timeAfter = System.currentTimeMillis();
        String msg = "computed ~:" + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
        System.out.println(msg);
    }

    /**
     * Takes an address and constructs the automaton represented by the file referred to by the address
     *
     * @param address
     */
    public Automaton(String address) {
        this();

        //lineNumber will be used in error messages
        int lineNumber = 0;
        setAlphabetSize(1);

        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(address), StandardCharsets.UTF_8));
            String line;
            boolean[] singleton = new boolean[1];
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (PATTERN_WHITESPACE.matcher(line).matches()) {
                    // Ignore blank lines.
                    continue;
                }
                if (ParseMethods.parseTrueFalse(line, singleton)) {
                    // It is a true/false automaton.
                    fa.setTRUE_FALSE_AUTOMATON(true);
                    fa.setTRUE_AUTOMATON(singleton[0]);
                    in.close();
                    return;
                }

                boolean flag;
                try {
                    flag = ParseMethods.parseAlphabetDeclaration(line, getA(), getNS());
                } catch (RuntimeException e) {
                    in.close();
                    throw new RuntimeException(
                        e.getMessage() + System.lineSeparator() +
                            "\t:line " + lineNumber + " of file " + address);
                }

                if (flag) {
                    for (int i = 0; i < getA().size(); i++) {
                        if (getNS().get(i) != null &&
                            (!getA().get(i).contains(0) || !getA().get(i).contains(1))) {
                            in.close();
                            throw new RuntimeException(
                                "The " + (i + 1) + "th input of type arithmetic " +
                                    "of the automaton declared in file " + address +
                                    " requires 0 and 1 in its input alphabet: line " +
                                    lineNumber);
                        }
                        UtilityMethods.removeDuplicates(getA().get(i));
                    }
                    this.determineAlphabetSizeFromA();

                    break;
                } else {
                    in.close();
                    throw new RuntimeException(
                        "Undefined statement: line " +
                            lineNumber + " of file " + address);
                }
            }

            int[] pair = new int[2];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            int currentState = -1;
            int currentOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            Map<Integer, Integer> output = new TreeMap<>();
            Map<Integer, Int2ObjectRBTreeMap<IntList>> transitions = new TreeMap<>();
            /**
             * This will hold all states that are destination of some transition.
             * Then we make sure all these states are declared.
             */
            int Q = 0, q0=0;
            Set<Integer> setOfDestinationStates = new HashSet<>();
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (PATTERN_WHITESPACE.matcher(line).matches()) {
                    continue;
                }

                if (ParseMethods.parseStateDeclaration(line, pair)) {
                    Q++;
                    if (currentState == -1) {
                        q0 = pair[0];
                    }

                    currentState = pair[0];
                    currentOutput = pair[1];
                    output.put(currentState, currentOutput);
                    currentStateTransitions = new Int2ObjectRBTreeMap<>();
                    transitions.put(currentState, currentStateTransitions);
                } else if (ParseMethods.parseTransition(line, input, dest)) {
                    setOfDestinationStates.addAll(dest);
                    if (currentState == -1) {
                        in.close();
                        throw new RuntimeException(
                                "Must declare a state before declaring a list of transitions: line " +
                                        lineNumber + " of file " + address);
                    }

                    if (input.size() != getA().size()) {
                        in.close();
                        throw new RuntimeException("This automaton requires a " + getA().size() +
                                "-tuple as input: line " + lineNumber + " of file " + address);
                    }
                    List<List<Integer>> inputs = expandWildcard(this.getA(), input);

                    for (List<Integer> i : inputs) {
                        currentStateTransitions.put(encode(i), dest);
                    }

                    input = new ArrayList<>();
                    dest = new IntArrayList();
                } else {
                    in.close();
                    throw new RuntimeException("Undefined statement: line " + lineNumber + " of file " + address);
                }
            }
            in.close();
            for (int q : setOfDestinationStates) {
                if (!output.containsKey(q)) {
                    throw new RuntimeException(
                            "State " + q + " is used but never declared anywhere in file: " + address);
                }
            }

            this.fa.setFieldsFromFile(Q, q0, output, transitions);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File does not exist: " + address);
        }
    }

    /**
     * returns a deep copy of this automaton.
     *
     * @return a deep copy of this automaton
     */
    public Automaton clone() {
        if (fa.isTRUE_FALSE_AUTOMATON()) {
            return new Automaton(fa.isTRUE_AUTOMATON());
        }
        return cloneFields(new Automaton());
    }

    Automaton cloneFields(Automaton M) {
        M.fa = fa.clone();
        M.setAlphabetSize(getAlphabetSize());
        M.labelSorted = labelSorted;
        for (int i = 0; i < getA().size(); i++) {
            M.getA().add(new ArrayList<>(getA().get(i)));
            M.getNS().add(getNS().get(i));
            if (getEncoder() != null && !getEncoder().isEmpty()) {
                if (M.getEncoder() == null) M.setEncoder(new ArrayList<>());
                M.getEncoder().add(getEncoder().get(i));
            }
            if (this.isBound())
                M.getLabel().add(getLabel().get(i));
        }
        return M;
    }


    public boolean equals(Automaton M) {
        if (M == null) return false;
        if (fa.isTRUE_FALSE_AUTOMATON() != M.fa.isTRUE_FALSE_AUTOMATON()) return false;
        if (fa.isTRUE_FALSE_AUTOMATON() && M.fa.isTRUE_FALSE_AUTOMATON()) {
          return fa.isTRUE_AUTOMATON() == M.fa.isTRUE_AUTOMATON();
        }
        dk.brics.automaton.Automaton Y = M.fa.to_dk_brics_automaton();
        dk.brics.automaton.Automaton X = this.fa.to_dk_brics_automaton();
        return X.equals(Y);
    }

    /**
     * Either perform the union or intersection of a list of automata.
     *
     * @param automataNames - list of automata names, saved in Automata Library
     * @param op            - either "union" or "intersect"
     * @param print
     * @param prefix
     * @param log
     * @return The union/intersection of all automata in automataNames and this automaton
     */
    public Automaton unionOrIntersect(List<String> automataNames, String op, boolean print, String prefix, StringBuilder log) {
        Automaton first = this.clone();

        for (String automataName : automataNames) {
            long timeBefore = System.currentTimeMillis();
            Automaton N = readAutomatonFromFile(automataName);

            // ensure that N has the same number system as first.
            if (NumberSystem.isNSDiffering(N.getNS(), first.getNS(), N.getA(), first.getA())) {
                throw new RuntimeException("Automata to be unioned must have the same number system(s).");
            }

            // crossProduct requires labelling; make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            N.setLabel(first.getLabel());

            if (op.equals("union")) {
                first = AutomatonLogicalOps.or(first, N, print, prefix, log, "|");
            } else if (op.equals("intersect")) {
                first = AutomatonLogicalOps.and(first, N, print, prefix, log);
            } else {
                throw new RuntimeException("Internal union/intersect error");
            }


            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computed =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }

    private static Automaton readAutomatonFromFile(String automataName) {
      return new Automaton(Session.getReadFileForAutomataLibrary(automataName + ".txt"));
    }

    public Automaton combine(List<String> automataNames, IntList outputs, boolean print, String prefix, StringBuilder log) {
        Queue<Automaton> subautomata = new LinkedList<>();

        for (String name : automataNames) {
            Automaton M = readAutomatonFromFile(name);
            subautomata.add(M);
        }
        return AutomatonLogicalOps.combine(this, subautomata, outputs, print, prefix, log);
    }

    // For use in the "combine" command.
    public void canonizeAndApplyAllRepresentationsWithOutput(boolean print, String prefix, StringBuilder log) {
        this.fa.setCanonized(false);
        this.canonize();
        this.applyAllRepresentationsWithOutput(print, prefix, log);
    }

    /**
     * @param outputs A list of integers, indicating which uncombined automata and in what order to return.
     * @return A list of automata, each corresponding to the list of outputs.
     * For the sake of an example, suppose that outputs is [0,1,2], then we return the list of automaton without output
     * which accepts if the output in our automaton is 0,1 or 2 respectively.
     */
    public List<Automaton> uncombine(List<Integer> outputs) {
        List<Automaton> automata = new ArrayList<>();
        for (Integer output : outputs) {
            Automaton M = clone();
            M.fa.setOutput(output);
            automata.add(M);
        }
        return automata;
    }

    /**
     * @return A minimized DFA with output recognizing the same language as the current DFA (possibly also with output).
     * We minimize a DFA with output by first uncombining into automata without output, minimizing the uncombined automata, and
     * then recombining. It follows that if the uncombined automata are minimal, then the combined automata is also minimal
     */
    public Automaton minimizeWithOutput(boolean print, String prefix, StringBuilder log) {
        IntList outputs = new IntArrayList(getO());
        UtilityMethods.removeDuplicates(outputs);
        List<Automaton> subautomata = uncombine(outputs);
        for (Automaton subautomaton : subautomata) {
            subautomaton.fa.determinizeAndMinimize(print, prefix, log);
        }
        Automaton N = subautomata.remove(0);
        List<String> label = new ArrayList<>(N.getLabel()); // We keep the old labels, since they are replaced in the combine
        N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, print, prefix, log);
        N.setLabel(label);
        return N;
    }

    public void minimizeSelfWithOutput(boolean print, String prefix, StringBuilder log) {
        Automaton N = minimizeWithOutput(print, prefix, log);
        copy(N);
    }

    private void normalizeNumberSystems(boolean print, String prefix, StringBuilder log) {
        // set all the number systems to be null.
        boolean switchNS = false;
        List<NumberSystem> numberSystems = new ArrayList<>(getNS().size());
        for (int i = 0; i < getNS().size(); i++) {
            if (getNS().get(i) != null && getNS().get(i).useAllRepresentations()) {
                switchNS = true;
                int max = Collections.max(getA().get(i));
                numberSystems.add(new NumberSystem((getNS().get(i).isMsd() ? "msd_" : "lsd_") + (max + 1)));
            } else {
                numberSystems.add(getNS().get(i));
            }
        }

        if (switchNS) {
            setAlphabet(false, numberSystems, getA(), print, prefix, log);
            // do this whether or not you print!
            String msg = prefix + "WARN: The alphabet of the resulting automaton was changed. Use the alphabet command to change as desired.";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

    }

    public Automaton star(boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "star: " + getQ() + " state automaton", log);

        Automaton N = clone();
        FA.starStates(this.fa, N.fa);
        N.normalizeNumberSystems(print, prefix, log);
        N.fa.setCanonized(false);
        N.canonize();
        N.fa.determinizeAndMinimize(print, prefix, log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "star complete: " + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    // concatenate
    public Automaton concat(List<String> automataNames, boolean print, String prefix, StringBuilder log) {
        Automaton first = this.clone();

        for (String automataName : automataNames) {
            long timeBefore = System.currentTimeMillis();
            Automaton N = readAutomatonFromFile(automataName);

            first = first.concat(N, print, prefix, log);

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "concatenated =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }

    private Automaton concat(Automaton other, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "concat: " + getQ() + " state automaton with " + other.getQ() + " state automaton", log);

        // ensure that N has the same number system as first.
        if (NumberSystem.isNSDiffering(other.getNS(), this.getNS(), this.getA(), other.getA())) {
            throw new RuntimeException("Automata to be concatenated must have the same number system(s).");
        }

        Automaton N = this.clone();

        int originalQ = this.getQ();

        FA.concatStates(other.fa, N.fa, originalQ);

        N.normalizeNumberSystems(print, prefix, log);

        N.fa.determinizeAndMinimize(print, prefix, log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "concat complete: " + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }


    public void setAlphabet(boolean isDFAO, List<NumberSystem> numberSystems, List<List<Integer>> alphabet, boolean print, String prefix, StringBuilder log) {

        if (alphabet.size() != getA().size()) {
            throw new RuntimeException("The number of alphabets must match the number of alphabets in the input automaton.");
        }
        if (alphabet.size() != numberSystems.size()) {
            throw new RuntimeException("The number of alphabets must match the number of number systems.");
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            List<String> nsNames = new ArrayList<>(numberSystems.size());
            for (int i = 0; i < numberSystems.size(); i++) {
                NumberSystem ns = numberSystems.get(i);
                nsNames.add(ns == null ? alphabet.get(i).toString() : ns.toString());
            }
            String msg = prefix + "setting alphabet to " + nsNames;
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        Automaton M = clone();
        M.setA(alphabet);
        M.setNS(numberSystems);
        M.determineAlphabetSizeFromA();
        M.setupEncoder();

        FA.alphabetStates(this, alphabet, M);

        if (isDFAO) {
            M.minimizeSelfWithOutput(print, prefix, log);
        } else {
            M.fa.determinizeAndMinimize(print, prefix, log);
        }

        M.canonizeAndApplyAllRepresentationsWithOutput(print, prefix + " ", log);

        copy(M);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "set alphabet complete:" + (timeAfter - timeBefore) + "ms", log);
    }

    public void determineAlphabetSizeFromA() {
        int alphabetSize = 1;
        for (List<Integer> x : this.getA()) {
            alphabetSize *= x.size();
        }
        this.fa.setAlphabetSize(alphabetSize);
    }

    // Determines whether an automaton accepts infinitely many values. If it does, a regex of infinitely many accepted values (not all)
    // is given. This is true iff there exists a cycle in a minimized version of the automaton, which previously had leading or
    // trailing zeroes removed according to whether it was msd or lsd
    public String infinite() {
        for (int i = 0; i < getQ(); i++) {
            IntSet visited = new IntOpenHashSet(); // states we have visited
            String cycle = infiniteHelper(visited, i, i, "");
            // once a cycle is detected, compute a prefix leading to state i and a suffix from state i to an accepting state
            if (!cycle.isEmpty()) {
                final int finalI = i;
                String prefix = findPath(this.fa, this.fa.getQ0(), y -> y == finalI, this.getA());
                String suffix = findPath(this.fa, finalI, y -> getO().getInt(y) != 0, this.getA());
                return prefix + "(" + cycle + ")*" + suffix;
            }
        }
        return ""; // an empty string signals that we have failed to find a cycle
    }

    // helper function for our DFS to facilitate recursion
    private String infiniteHelper(IntSet visited, int started, int state, String result) {
        if (visited.contains(state)) {
            if (state == started) {
                return result;
            }
            return "";
        }
        visited.add(state);
        for (Int2ObjectMap.Entry<IntList> entry : getD().get(state).int2ObjectEntrySet()) {
            for (int y: entry.getValue()) {
                // this adds brackets even when inputs have arity 1 - this is fine, since we just want a usable infinite regex
                String cycle = infiniteHelper(visited, started, y, result + decode(getA(), entry.getIntKey()));
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        visited.remove(state);
        return "";
    }

    // Core pathfinding logic
    private static String findPath(
        FA automaton,
        int startState,
        Predicate<Integer> isFoundCondition,
        List<List<Integer>> A) {
        // Early exit if the start state meets the condition
        if (isFoundCondition.test(startState)) {
            return "";
        }
        List<Integer> distance = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
        List<Integer> prev = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
        List<Integer> input = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
        distance.set(startState, 0);

        Queue<Integer> queue = new LinkedList<>();
        queue.add(startState);

        boolean found = false;
        int endState = -1;

        // BFS to find the path
        while (!queue.isEmpty() && !found) {
            int current = queue.poll();

            for (Int2ObjectMap.Entry<IntList> entry : automaton.getNfaD().get(current).int2ObjectEntrySet()) {
                int x = entry.getIntKey();
                IntList transitions = entry.getValue();

                for (int y : transitions) {
                    if (isFoundCondition.test(y)) {
                        found = true;
                        endState = y;
                    }
                    if (distance.get(y) == -1) { // Unvisited state
                        distance.set(y, distance.get(current) + 1);
                        prev.set(y, current);
                        input.set(y, x);
                        queue.add(y);
                    }
                }
            }
        }

        // Reconstruct the path
        List<Integer> path = new ArrayList<>();
        int current = found ? endState : startState;
        while (current != startState) {
            path.add(input.get(current));
            current = prev.get(current);
        }
        Collections.reverse(path);

        // Convert the path to a string
        StringBuilder result = new StringBuilder();
        for (Integer node : path) {
            result.append(decode(A, node));
        }
        return result.toString();
    }

    /**
     * @param inputs A list of "+", "-" or "". Indicating how our input will be interpreted in the output automata.
     *               Inputs must correspond to inputs of the current automaton
     *               which can be compared to some corresponding negative base.
     * @return The automaton which replaces inputs in negative base with an input in corresponding comparable positive base.
     * For sake of example, suppose the input is [+,-,] and M is the current automata with inputs in base -2.
     * On inputs (x,y,z), where x,y are inputs in base 2, the automaton gives as output M(x',y',z) where
     * x' and y' are in the corresponding base -2 representations of x and -y.
     */
    /**
     * Generalized method to handle split and reverse split operations on the automaton.
     *
     * @param inputs A list of "+", "-" or "". Indicating how our input will be interpreted in the output automata.
     * @param reverse Whether to perform the reverse split operation.
     * @param print Whether to print debug information.
     * @param prefix A prefix for debug messages.
     * @param log A log to store debug information.
     * @return The modified automaton after the split/reverse split operation.
     */
    public Automaton processSplit(List<String> inputs, boolean reverse, boolean print, String prefix, StringBuilder log) {
        if (getAlphabetSize() == 0) {
            throw new RuntimeException("Cannot process split automaton with no inputs.");
        }
        if (inputs.size() != getA().size()) {
            throw new RuntimeException("Split automaton has incorrect number of inputs.");
        }

        Automaton M = clone();
        Set<String> quantifiers = new HashSet<>();
        // Label M with [b0, b1, ..., b(A.size() - 1)]
        List<String> names = new ArrayList<>(getA().size());
        for (int i = 0; i < getA().size(); i++) {
            names.add("b" + i);
        }
        M.setLabels(names);

        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).isEmpty()) {
                if (getNS().get(i) == null)
                    throw new RuntimeException("Number system for input " + i + " must be defined.");
                NumberSystem negativeNumberSystem;
                if (getNS().get(i).is_neg) {
                    negativeNumberSystem = getNS().get(i);
                } else {
                    try {
                        negativeNumberSystem = getNS().get(i).negative_number_system();
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Negative number system for " + getNS().get(i) + " must be defined");
                    }
                }
                negativeNumberSystem.setBaseChange();
                if (negativeNumberSystem.baseChange == null) {
                    throw new RuntimeException("Number systems " + getNS().get(i) + " and " + negativeNumberSystem + " cannot be compared.");
                }

                Automaton baseChange = negativeNumberSystem.baseChange.clone();
                String a = "a" + i, b = "b" + i, c = "c" + i;

                if (inputs.get(i).equals("+")) {
                    baseChange.bind(reverse ? List.of(b,a) : List.of(a,b)); // Use ternary for binding logic
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    quantifiers.add(b);
                } else { // inputs.get(i).equals("-")
                    baseChange.bind(List.of(reverse ? b : a, c)); // Use ternary for binding logic
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    M = AutomatonLogicalOps.and(
                        M,
                        negativeNumberSystem.arithmetic(reverse ? a : b, c, 0, "+"), // Use ternary for arithmetic logic
                        print, prefix, log
                    );
                    quantifiers.add(b);
                    quantifiers.add(c);
                }
            }
        }
        AutomatonLogicalOps.quantify(M, quantifiers, print, prefix, log);
        M.sortLabel();
        M.randomLabel();
        return M;
    }

    /**
     * @param subautomata A queue of automaton which we will "join" with the current automaton.
     * @return The cross product of the current automaton and automaton in subautomata, using the operation "first" on the outputs.
     * For sake of example, the current Automaton is M1, and subautomata consists of M2 and M3.
     * Then on input x, returned automaton should output the first non-zero value of [ M1(x), M2(x), M3(x) ].
     */
    public Automaton join(Queue<Automaton> subautomata, boolean print, String prefix, StringBuilder log) {
        Automaton first = this.clone();

        while (!subautomata.isEmpty()) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computing =>:" + first.getQ() + " states - " + next.getQ() + " states", log);

            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            AutomatonLogicalOps.totalize(first.fa, print, prefix + " ", log);
            AutomatonLogicalOps.totalize(next.fa, print, prefix + " ", log);
            first = AutomatonLogicalOps.crossProduct(first, next, "first", print, prefix + " ", log);
            first = first.minimizeWithOutput(print, prefix + " ", log);

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computed =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }

    public List<String> findAccepted(Integer searchLength, Integer maxNeeded) {
        List<String> accepted = new ArrayList<>();
        findAcceptedHelper(accepted, maxNeeded, searchLength, 0, "", getQ0());
        return accepted;
    }

    private boolean findAcceptedHelper(
        List<String> accepted, int maxNeeded, int searchLength, Integer curLength, String path, Integer state) {
        if (curLength == searchLength) {
            // if we reach an accepting state of desired length, we add the string we've formed to our subautomata list
            if (getO().getInt(state) != 0) {
                accepted.add(path);
                if (accepted.size() >= maxNeeded) {
                    return true;
                }
            } else {
                return false;
            }
        }
        for (Int2ObjectMap.Entry<IntList> entry : getD().get(state).int2ObjectEntrySet()) {
            for (int y : entry.getValue()) {
                List<Integer> decodeAx = decode(getA(), entry.getIntKey());
                String input = decodeAx.toString();

                // we remove brackets if we have a single arity input that is between 0 and 9 (and hence unambiguous)
                if (getA().size() == 1) {
                    if (decodeAx.get(0) >= 0 && decodeAx.get(0) <= 9) {
                        input = input.substring(1, input.length() - 1);
                    }
                }
                // if we've already found as much as we need, then there's no need to search further; we propagate the signal
                if (findAcceptedHelper(
                    accepted, maxNeeded, searchLength, curLength + 1, path + input, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void applyAllRepresentations() {
        boolean flag = determineRandomLabel();
        Automaton K = this;
        for (int i = 0; i < getA().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null) {
                Automaton N = ns.getAllRepresentations();
                if (N != null && ns.useAllRepresentations()) {
                    N.bind(List.of(getLabel().get(i)));
                    K = AutomatonLogicalOps.and(K, N, false, null, null);
                }
            }
        }
        if (flag)
            unlabel();
        copy(K);
    }

    private void applyAllRepresentationsWithOutput(boolean print, String prefix, StringBuilder log) {
        // this can be a word automaton
        boolean flag = determineRandomLabel();
        Automaton K = this;
        for (int i = 0; i < getA().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null) {
                Automaton N = ns.getAllRepresentations();
                if (N != null && ns.useAllRepresentations()) {
                    N.bind(List.of(getLabel().get(i)));
                    K = AutomatonLogicalOps.crossProduct(this, N, "if_other", print, prefix, log);
                }
            }
        }
        if (flag)
            unlabel();
        copy(K);
    }

    private boolean determineRandomLabel() {
        if (!isBound()) {
            randomLabel();
            return true;
        }
        return false;
    }

    public void randomLabel() {
        List<String> randomNames = new ArrayList<>(getA().size());
        for(int i=0;i<getA().size();i++) {
            randomNames.add(Integer.toString(i));}
        setLabels(randomNames);
    }

    private void setLabels(List<String> names) {
        if (getLabel() == null || !getLabel().isEmpty()) setLabel(new ArrayList<>());
        this.getLabel().addAll(names);
    }

    private void unlabel() {
        setLabel(new ArrayList<>());
        labelSorted = false;
    }

    private void copy(Automaton M) {
        fa.setTRUE_FALSE_AUTOMATON(M.fa.isTRUE_FALSE_AUTOMATON());
        fa.setTRUE_AUTOMATON(M.fa.isTRUE_AUTOMATON());
        fa = M.fa.clone();
        setA(M.getA());
        setNS(M.getNS());
        setEncoder(M.getEncoder());
        setLabel(M.getLabel());
        labelSorted = M.labelSorted;
    }


    /**
     * The operator can be one of "_" "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs this[x]+o on input x.
     * To be used only when this automaton and M are DFAOs (words).
     *
     * @param operator
     */
    public void applyOperator(String operator, int o, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applying operator (" + operator + "):" + getQ() + " states", log);
        for (int p = 0; p < getQ(); p++) {
            IntList thisO = this.getO();
            int thisP = thisO.getInt(p);
            switch (operator) {
                case "+":
                    thisO.set(p, thisP + o);
                    break;
                case "-":
                    thisO.set(p, thisP - o);
                    break;
                case "*":
                    thisO.set(p, thisP * o);
                    break;
                case "/":
                    if (o == 0) throw ExceptionHelper.divisionByZero();
                    thisO.set(p, thisP / o);
                    break;
                case "_":
                    thisO.set(p, -thisP);
                    break;
            }
        }
        minimizeSelfWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applied operator (" + operator + "):" + getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * The operator can be one of "_" "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs o+this[x] on input x.
     * To be used only when this automaton and M are DFAOs (words).
     *
     * @param operator
     */
    public void applyOperator(int o, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applying operator (" + operator + "):" + getQ() + " states", log);
        for (int p = 0; p < getQ(); p++) {
            IntList thisO = this.getO();
            int thisP = thisO.getInt(p);
            switch (operator) {
                case "+":
                    thisO.set(p, o + thisP);
                    break;
                case "-":
                    thisO.set(p, o - thisP);
                    break;
                case "*":
                    thisO.set(p, o * thisP);
                    break;
                case "/":
                    if (thisP == 0) throw ExceptionHelper.divisionByZero();
                    thisO.set(p, o / thisP);
                    break;
                case "_":
                    thisO.set(p, -thisP);
                    break;
            }
        }
        minimizeSelfWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applied operator (" + operator + "):" + getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Set the fields of this automaton to represent a dk.brics.automaton.Automaton.
     * An automata in our program can be of type Automaton or dk.brics.automaton.Automaton. We use package
     * dk.brics.automaton for automata minimization. This method transforms an automaton of type dk.brics.automaton.Automaton
     * to an automaton of type Automaton.
     *
     * @param M is a deterministic automaton without output.
     */
    private void setThisAutomatonToRepresent(dk.brics.automaton.Automaton M) {
        if (!M.isDeterministic())
            throw ExceptionHelper.bricsNFA();
        List<State> setOfStates = new ArrayList<>(M.getStates());
        fa.setCanonized(false);
        this.fa.setFromBricsAutomaton(M, setOfStates);
    }

    /**
     * Sorts states in Q based on their breadth-first order. It also calls sortLabel().
     * The method also removes states that are not reachable from the initial state.
     * In draw() and write() methods, we first call the canonize the automaton.
     * It is also used in write() method.
     * Note that before we try to canonize, we check if this automaton is already canonized.
     *
     */
    public void canonize() {
        sortLabel();
        this.fa.canonizeInternal();
    }

    /**
     * Sorts inputs based on their labels lexicographically.
     * For example if the labels of the inputs are ["b","c","a"], then the first, second, and third
     * inputs are "a", "b", and "c". Now if we call sortLabels(), the order of inputs changes: label becomes
     * sorted in lexicographic order ["a","b","c"], and therefore, the first, second, and third inputs are
     * now "a", "b", and "c". Before we draw this automaton using draw() method,
     * we first sort the labels (inside canonize method).
     * It is also used in write() method.
     * Note that before we try to sort, we check if the label is already sorted.
     * The label cannot have repeated element.
     */
    protected void sortLabel() {
        if (labelSorted) return;
        labelSorted = true;
        if (fa.isTRUE_FALSE_AUTOMATON()) return;
        if (!isBound()) return;
        if (UtilityMethods.isSorted(this.getLabel())) return;
        List<String> sorted_label = new ArrayList<>(getLabel());
        Collections.sort(sorted_label);

        int[] label_permutation = UtilityMethods.getLabelPermutation(getLabel(), sorted_label);

        /**
         * permuted_A is going to hold the alphabet of the sorted inputs.
         * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
         * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
         * The same logic is behind permuted_encoder.
         */
        List<List<Integer>> permuted_A = UtilityMethods.permute(getA(), label_permutation);
        List<Integer> permuted_encoder = UtilityMethods.getPermutedEncoder(getA(), permuted_A);
        /**
         * For example encoded_input_permutation[2] = 5 means that encoded input 2 becomes
         * 5 after sorting.
         */
        int[] encoded_input_permutation = new int[getAlphabetSize()];
        for (int i = 0; i < getAlphabetSize(); i++) {
            List<Integer> input = decode(getA(), i);
            List<Integer> permuted_input = UtilityMethods.permute(input, label_permutation);
            encoded_input_permutation[i] = encode(permuted_input, permuted_A, permuted_encoder);
        }

        setLabel(sorted_label);
        setA(permuted_A);
        setEncoder(permuted_encoder);
        setNS(UtilityMethods.permute(getNS(), label_permutation));

        this.fa.permuteD(encoded_input_permutation);
    }


    /**
     * Input to dk.brics.automaton.Automata is a char. Input to Automaton is List<Integer>.
     * Thus this method transforms an integer to its corresponding List<Integer>
     * Example: A = [[0,1],[-1,2,3]] and if
     * n = 0 then we return [0,-1]
     * n = 1 then we return [1,-1]
     * n = 2 then we return [0,2]
     * n = 3 then we return [1,2]
     * n = 4 then we return [0,3]
     * n = 5 then we return [1,3]
     *
     * @param A
     * @param n
     * @return
     */
    public static List<Integer> decode(List<List<Integer>> A, int n) {
        List<Integer> l = new ArrayList<>(A.size());
        for (List<Integer> integers : A) {
            l.add(integers.get(n % integers.size()));
            n = n / integers.size();
        }
        return l;
    }


    /**
     * Input to dk.brics.automaton.Automata is a char. Input to Automata.Automaton is List<Integer>.
     * Thus this method transforms a List<Integer> to its corresponding integer.
     * The other application of this function is when we use the transition function d in State. Note that the transtion function
     * maps an integer (encoding of List<Integer>) to a set of states.
     * <p>
     * Example: A = [[0,1],[-1,2,3]] and if
     * l = [0,-1] then we return 0
     * l = [1,-1] then we return 1
     * l = [0,2] then we return 2
     * l = [1,2] then we return 3
     * l = [0,3] then we return 4
     * l = [1,3] then we return 5
     * Second Example: A = [[-2,-1,-3],[0,1],[-1,0,3],[7,8]] and if
     * l = [-2,0,-1,7] then we return 0
     * l = [-1,0,-1,7] then we return 1
     * l = [-3,0,-1,7] then we return 2
     * l = [-2,1,-1,7] then we return 3
     * l = [-1,1,-1,7] then we return 4
     * l = [-3,1,-1,7] then we return 5
     * l = [-2,0,0,7] then we return 6
     * ...
     *
     * @param l
     * @return
     */
    public int encode(List<Integer> l) {
        if (getEncoder() == null) {
            setupEncoder();
        }
        return encode(l, getA(), getEncoder());
    }

    public static int encode(List<Integer> l, List<List<Integer>> A, List<Integer> encoder) {
        int encoding = 0;
        for (int i = 0; i < l.size(); i++) {
            encoding += encoder.get(i) * A.get(i).indexOf(l.get(i));
        }
        return encoding;
    }

    public void setupEncoder() {
        setEncoder(new ArrayList<>());
        getEncoder().add(1);
        for (int i = 0; i < getA().size() - 1; i++) {
            getEncoder().add(getEncoder().get(i) * getA().get(i).size());
        }
    }

    /**
     * A wildcard is denoted by null in L. What do we mean by expanding wildcard?
     * Here is an example: suppose that A = [[1,2],[0,-1],[3,4,5]] and L = [1,*,4]. Then the method would return
     * [[1,0,4],[1,-1,4]]. In other words, it'll replace * in the second position with 0 and -1.
     */
    public static List<List<Integer>> expandWildcard(List<List<Integer>> A, List<Integer> L) {
        List<List<Integer>> R = new ArrayList<>();
        R.add(new ArrayList<>(L));
        for (int i = 0; i < L.size(); i++) {
            if (L.get(i) == null) {
                List<List<Integer>> tmp = new ArrayList<>();
                for (int x : A.get(i)) {
                    for (List<Integer> tmp2 : R) {
                        tmp.add(new ArrayList<>(tmp2));
                        tmp.get(tmp.size() - 1).set(i, x);
                    }
                }
                R = new ArrayList<>(tmp);
            }
        }
        return R;
    }

    public void bind(List<String> names) {
        if (fa.isTRUE_FALSE_AUTOMATON() || getA().size() != names.size()) throw ExceptionHelper.invalidBind();
        setLabels(names);
        labelSorted = false;
        fa.setCanonized(false);
        AutomatonLogicalOps.removeSameInputs(this, 0);
    }

    public boolean isBound() {
      return getLabel() != null && getLabel().size() == getA().size();
    }

    public int getArity() {
        if (fa.isTRUE_FALSE_AUTOMATON()) return 0;
        return getA().size();
    }

    /**
     * clears this automaton
     */
    void clear() {
        this.fa.clear();
        setA(null);
        setNS(null);
        setEncoder(null);
        setLabel(null);
        labelSorted = false;
    }

    protected boolean isEmpty() {
        if (fa.isTRUE_FALSE_AUTOMATON()) {
            return !fa.isTRUE_AUTOMATON();
        }
        return this.fa.to_dk_brics_automaton().isEmpty();
    }


    static int mapToReducedEncodedInput(int n, List<Integer> I, List<Integer> newEncoder,
                                 List<List<Integer>> oldAlphabet,
                                 List<List<Integer>> newAlphabet) {
        if (I.size() <= 1) return n;
        List<Integer> x = decode(oldAlphabet, n);
        for (int i = 1; i < I.size(); i++)
            if (x.get(I.get(i)) != x.get(I.get(0)))
                return -1;
        List<Integer> y = new ArrayList<>();
        for (int i = 0; i < x.size(); i++)
            if (!I.contains(i) || I.indexOf(i) == 0)
                y.add(x.get(i));
        return encode(y, newAlphabet, newEncoder);
    }

    /**
     * Initial State.
     */
    public int getQ0() {
        return fa.getQ0();
    }

    public void setQ0(int q0) {
        this.fa.setQ0(q0);
    }

    /**
     * Number of States. For example when Q = 3, the set of states is {0,1,2}
     */
    public int getQ() {
        return this.fa.getQ();
    }

    public void setQ(int q) {
        this.fa.setQ(q);
    }

    /**
     * State Outputs. In case of DFA/NFA accepting states have a nonzero integer as their output.
     * Rejecting states have output 0.
     * Example: O = [-1,2,...] then state 0 and 1 have outputs -1 and 2 respectively.
     */
    public IntList getO() {
        return this.fa.getO();
    }

    /**
     * Transition Function for This State. For example, when d[0] = [(0,[1]),(1,[2,3]),(2,[2]),(3,[4]),(4,[1]),(5,[0])]
     * and alphabet A = [[0,1],[-1,2,3]]
     * then from the state 0 on
     * (0,-1) we go to 1
     * (0,2) we go to 2,3
     * (0,3) we go to 2
     * (1,-1) we go to 4
     * ...
     * So we store the encoded values of inputs in d, i.e., instead of saying on (0,-1) we go to state 1, we say on 0, we go
     * to state 1.
     * Recall that (0,-1) represents 0 in mixed-radix base (1,2) and alphabet A. We have this mixed-radix base (1,2) stored as encoder in
     * our program, so for more information on how we compute it read the information on List<Integer> encoder field.
     * <p>
     * For memory reduction, during determinization the transition function d is set to null and temporarily represented with
     * the memory-efficient newMemD, which only stores single-state transitions output by the Subset Construction algorithm.
     * The transition function d is then regenerated during the minimize_valmari method, once the states are minimized.
     */
    public List<Int2ObjectRBTreeMap<IntList>> getD() {
        return this.fa.getNfaD();
    }

    public void setD(List<Int2ObjectRBTreeMap<IntList>> d) {
        this.fa.setNfaD(d);
    }

    public void setLabel(List<String> label) {
        this.label = label;
    }

    /**
     * Types of the inputs to this automaton.
     * There are two possible types for inputs for an automaton:Type.arithmetic or Type.alphabetLetter.
     * In other words, type of inputs to an automaton is either arithmetic or non-arithmetic.
     * For example we might have A = [[1,-1],[0,1,2],[0,-1]] and T = [Type.alphabetLetter, Type.arithmetic, Type.alphabetLetter]. So
     * the first and third inputs are non-arithmetic (and should not be treated as arithmetic).
     * This type is useful in type checking. So for example, we might have f(a,b+1,c+1), where f is the example automaton. Then this
     * is a type error, because the third input to f is non-arithmetic, and hence we cannot have c+1 as our third argument.
     * It is very important to note that, an input of type arithmetic must always contain 0 and 1 in its alphabet.
     */
    public List<NumberSystem> getNS() {
        return NS;
    }

    public void setNS(List<NumberSystem> NS) {
        this.NS = NS;
    }

    /**
     * This vector is useful in the encode method.
     * When A = [l1,l2,l3,...,ln] then
     * encoder = [1,|l1|,|l1|*|l2|,...,|l1|*|l2|*...*|ln-1|].
     * It is useful, as mentioned earlier, in the encode method. encode method gets a list x, which represents a viable
     * input to this automaton, and returns a non-negative integer, which is the integer represented by x, in base encoder.
     * Note that encoder is a mixed-radix base. We use the encoded integer, returned by encode(), to store transitions.
     * So we don't store the list x.
     * We can decode, the number returned by encode(), and get x back using decode method.
     */
    public List<Integer> getEncoder() {
        return encoder;
    }

    public void setEncoder(List<Integer> encoder) {
        this.encoder = encoder;
    }

    /**
     * Alphabet Size. For example, if A = [[-1,1],[2,3]], then alphabetSize = 4 and if A = [[-1,1],[0,1,2]], then alphabetSize = 6
     */
    public int getAlphabetSize() {
        return this.fa.getAlphabetSize();
    }

    public void setAlphabetSize(int alphabetSize) {
        this.fa.setAlphabetSize(alphabetSize);
    }

    /**
     * Input Alphabet.
     * For example when A = [[-1,1],[2,3]], the first and the second inputs are over alphabets
     * {-1,1} and {2,3} respectively.
     * Remember that the input to an automaton is a tuple (a pair in this example).
     * For example a state might make a transition on input (1,3). Here the
     * first input is 1 and the second input is 3.
     * Also note that A is a list of sets, but for technical reasons, we just made it a list of lists. However,
     * we have to make sure, at all times, that the inner lists of A don't contain repeated elements.
     */
    public List<List<Integer>> getA() {
        return A;
    }

    public void setA(List<List<Integer>> a) {
        A = a;
    }

    public FA getFa() {
        return fa;
    }
}
