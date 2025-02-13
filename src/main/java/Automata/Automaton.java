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

package Automata;

import Automata.FA.FA;
import Automata.FA.ProductStrategies;
import Main.ExceptionHelper;
import Main.Session;
import Main.UtilityMethods;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import Token.ArithmeticOperator;
import it.unimi.dsi.fastutil.ints.*;

import static Automata.ParseMethods.PATTERN_WHITESPACE;

/**
 * This class can represent different NFA, NFAO, DFA, DFAO.
 * There are also two special automata: true automaton, which accepts everything, and false automaton, which accepts nothing.
 * To represent true/false automata we use the field members: TRUE_FALSE_AUTOMATON and TRUE_AUTOMATA. <br>
 * We use the RichAlphabet encoding in our representation of automaton to refer to a particular input.
 * The output alphabet can be any finite subset of integers.
 * We may give labels to inputs. For example if we set label = ["x","y","z"], the label of the first input is "x".
 * Then in the future, we can refer to this first input by the label "x".
 */
public class Automaton {
    public RichAlphabet richAlphabet;
    private List<NumberSystem> NS;
    private List<String> label;
    private boolean labelSorted;  // hen true, labels are sorted lexicographically. It is used in sortLabel() method.

    public FA fa; // abstract FA fields

    // for use in the combine command, counts how many products we have taken so far, and hence what to set outputs to
    int combineIndex;

    // for use in the combine command, allows crossProduct to determine what to set outputs to
    IntList combineOutputs;

    public void writeAutomata(String predicate, String outLibrary, String name, boolean isDFAO) {
        AutomatonWriter.draw(this, Session.getAddressForResult() + name + ".gv", predicate, isDFAO);
        String firstAddress = Session.getAddressForResult() + name + ".txt";
        AutomatonWriter.write(this, firstAddress);
        // Copy to second location, rather than rewriting.
        try {
            Files.copy(Paths.get(firstAddress), Paths.get(outLibrary + name + ".txt"),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Automaton removeLeadTrailZeroes() {
      // When dealing with enumerating values (e.g. inf and test commands), we remove leading zeroes in the case of msd
      // and trailing zeroes in the case of lsd. To do this, we construct a reg subcommand that generates the complement
      // of zero-prefixed strings for msd and zero suffixed strings for lsd, then intersect this with our original automaton.
      randomLabel();
      return AutomatonLogicalOps.removeLeadingZeroes(this, getLabel(), false, null, null);
    }

    public int determineCombineOutVal(String op) {
      return op.equals("combine") ? this.combineOutputs.getInt(this.combineIndex) : -1;
    }

    /**
     * We would like to give label to inputs.
     * As an example when label = ["a","b","c"], the label of the first, second, and third inputs are a, b, and c respectively.
     * These labels are then useful when we quantify an automaton.
     * For example, in a predicate like E a f(a,b,c) we have an automaton
     * of three inputs, where the inputs are labeled "a","b", and "c".
     * E a f(a,b,c) says, we want to do an existential quantifier on the first input.
     */
    public List<String> getLabel() {
        return label;
    }

    /*
     * Default constructor. It just initializes the field members.
     */
    public Automaton() {
        fa = new FA();
        richAlphabet = new RichAlphabet();
        setNS(new ArrayList<>());
        setLabel(new ArrayList<>());
        dk.brics.automaton.Automaton.setMinimization(dk.brics.automaton.Automaton.MINIMIZE_HOPCROFT);
    }

    /**
     * Initializes a special automaton: true or false.
     * A true automaton, is an automaton that accepts everything. A false automaton is an automaton that accepts nothing.
     * Therefore, M and false is false for every automaton M. We also have that M or true is true for every automaton M.
     *
     * @param truthValue - truth value of special automaton
     */
    public Automaton(boolean truthValue) {
        fa = new FA();
        richAlphabet = new RichAlphabet();
        fa.setTRUE_FALSE_AUTOMATON(true);
        this.fa.setTRUE_AUTOMATON(truthValue);
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
     * with this constructor, has only one input, and it is of type AlphabetLetter.
     */
    public Automaton(String regularExpression, List<Integer> alphabet) {
        this();
        getNS().add(null);
        if (alphabet == null || alphabet.isEmpty()) throw new RuntimeException("empty alphabet is not accepted");
        alphabet = new ArrayList<>(alphabet);
        //The alphabet is a set and does not allow repeated elements. However, the user might enter the command
        //reg myreg {1,1,0,0,0} "10*"; and therefore alphabet = [1,1,0,0,0]. So remove duplicates.
        UtilityMethods.removeDuplicates(alphabet);
        getA().add(alphabet);

        this.fa.convertBrics(alphabet, regularExpression);
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
        fa.setCanonized(false);
        this.fa.setFromBricsAutomaton(alphabetSize, regularExpression);
    }

    /**
     * Takes an address and constructs the automaton represented by the file referred to by the address
     */
    public Automaton(String address) {
        this();
        File f = UtilityMethods.validateFile(address);

        long lineNumber = 0;
        setAlphabetSize(1);

        Boolean[] trueFalseSingleton = new Boolean[1];
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            lineNumber = firstParse(address, in, lineNumber, trueFalseSingleton);
            if (trueFalseSingleton[0] != null) {
                return;
            }

            int[] pair = new int[2];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            int currentState = -1;
            int currentOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            Map<Integer, Integer> output = new TreeMap<>();
            Map<Integer, Int2ObjectRBTreeMap<IntList>> transitions = new TreeMap<>();

            int Q = 0, q0=0;
            Set<Integer> setOfDestinationStates = new HashSet<>();
            boolean outputLongFile = false;

            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                outputLongFile = debugPrintLongFile(address, lineNumber, outputLongFile);

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
                    validateTransition(address, currentState, lineNumber, input);
                    setOfDestinationStates.addAll(dest);
                    List<List<Integer>> inputs = richAlphabet.expandWildcard(input);
                    for (List<Integer> i : inputs) {
                        currentStateTransitions.put(richAlphabet.encode(i), dest);
                    }
                    input = new ArrayList<>();
                    dest = new IntArrayList();
                } else {
                    throw ExceptionHelper.undefinedStatement(lineNumber, address);
                }
            }
            if (outputLongFile) {
                System.out.println("...finished");
            }

            validateDeclaredStates(setOfDestinationStates, output, address);

            this.fa.setFieldsFromFile(Q, q0, output, transitions);
        } catch (IOException e) {
            e.printStackTrace();
            throw ExceptionHelper.fileDoesNotExist(address);
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

    protected void validateTransition(String address, int currentState, long lineNumber, List<Integer> input) {
        if (currentState == -1) {
            throw new RuntimeException(
                    "Must declare a state before declaring a list of transitions: line " +
                        lineNumber + " of file " + address);
        }

        if (input.size() != getA().size()) {
            throw new RuntimeException("This automaton requires a " + getA().size() +
                    "-tuple as input: line " + lineNumber + " of file " + address);
        }
    }

    protected long firstParse(
        String address, BufferedReader in, long lineNumber, Boolean[] trueFalseSingleton) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            lineNumber++;

            if (PATTERN_WHITESPACE.matcher(line).matches()) {
                // Ignore blank lines.
                continue;
            }

            if (trueFalseSingleton != null && ParseMethods.parseTrueFalse(line, trueFalseSingleton)) {
                // It is a true/false automaton.
                fa.setTRUE_FALSE_AUTOMATON(true);
                fa.setTRUE_AUTOMATON(trueFalseSingleton[0]);
                break;
            }

            if (ParseMethods.parseAlphabetDeclaration(line, getA(), getNS())) {
                for (int i = 0; i < getA().size(); i++) {
                    if (getNS().get(i) != null &&
                        (!getA().get(i).contains(0) || !getA().get(i).contains(1))) {
                        throw new RuntimeException(
                            "The " + (i + 1) + "th input of type arithmetic " +
                                "of the automaton declared in file " + address +
                                " requires 0 and 1 in its input alphabet: line " +
                                lineNumber);
                    }
                    UtilityMethods.removeDuplicates(getA().get(i));
                }
                this.determineAlphabetSize();
                break;
            } else {
                throw ExceptionHelper.undefinedStatement(lineNumber, address);
            }
        }
        return lineNumber;
    }

    protected void validateDeclaredStates(Set<Integer> destinationStates, Map<Integer, ?> declaredStates, String address) {
        for (Integer q : destinationStates) {
            if (!declaredStates.containsKey(q)) {
                throw new RuntimeException("State " + q + " is used but never declared anywhere in file: " + address);
            }
        }
    }

    /**
     * Returns a deep copy of this automaton.
     */
    public Automaton clone() {
        if (fa.isTRUE_FALSE_AUTOMATON()) {
            return new Automaton(fa.isTRUE_AUTOMATON());
        }
        return cloneFields(new Automaton());
    }

    Automaton cloneFields(Automaton M) {
        M.fa = fa.clone();
        M.labelSorted = labelSorted;
        clonePartialFields(M);
        return M;
    }

    void clonePartialFields(Automaton M) {
        M.richAlphabet = richAlphabet.clone();
        for (int i = 0; i < getA().size(); i++) {
            M.getNS().add(getNS().get(i));
            if (this.isBound())
                M.getLabel().add(getLabel().get(i));
        }
    }

    public boolean equals(Automaton M) {
        if (M == null) return false;
        return this.fa.equals(M.fa);
    }

    /**
     * Either perform the union or intersection of a list of automata.
     *
     * @param automataNames - list of automata names, saved in Automata Library
     * @param op            - either "union" or "intersect"
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

    public static Automaton readAutomatonFromFile(String automataName) {
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
        List<Automaton> automata = new ArrayList<>(outputs.size());
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
            NumberSystem ns = getNS().get(i);
            if (ns != null && ns.useAllRepresentations()) {
                switchNS = true;
                int max = Collections.max(getA().get(i));
                numberSystems.add(new NumberSystem((ns.isMsd() ? "msd_" : "lsd_") + (max + 1)));
            } else {
                numberSystems.add(ns);
            }
        }

        if (switchNS) {
            setAlphabet(false, numberSystems, getA(), print, prefix, log);
            // always print this
            UtilityMethods.logMessage(true,
                prefix + "WARN: The alphabet of the resulting automaton was changed. Use the alphabet command to change as desired.", log);
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
            UtilityMethods.logMessage(true, msg, log);
        }

        Automaton M = clone();
        M.setA(alphabet);
        M.setNS(numberSystems);
        M.determineAlphabetSize();
        M.richAlphabet.setupEncoder();

        this.getFa().alphabetStates(alphabet, this.getA(), M);

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

    // TODO: possibly this can just be determined when setA() is called.
    public void determineAlphabetSize() {
        this.fa.setAlphabetSize(richAlphabet.determineAlphabetSize());
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
                NumberSystem ns = getNS().get(i);
                if (ns == null)
                    throw new RuntimeException("Number system for input " + i + " must be defined.");
                NumberSystem negativeNumberSystem = ns.determineNegativeNS();

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
            first.fa.totalize(print, prefix + " ", log);
            next.fa.totalize(print, prefix + " ", log);
            first = ProductStrategies.crossProduct(first, next, "first", print, prefix + " ", log);
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
        for (Int2ObjectMap.Entry<IntList> entry : getFa().getEntriesNfaD(state)) {
            for (int y : entry.getValue()) {
                List<Integer> decodeAx = richAlphabet.decode(entry.getIntKey());
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
                    K = ProductStrategies.crossProduct(this, N, "if_other", print, prefix, log);
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
        richAlphabet = M.richAlphabet.clone();
        setNS(M.getNS());
        setLabel(M.getLabel());
        labelSorted = M.labelSorted;
    }

    /**
     * The operator can be one of "_" "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs o+this[x] (or this[x]+p) on input x.
     * To be used only when this automaton and M are DFAOs (words).
     */
    public void applyOperator(int o, String operator, boolean reverse, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applying operator (" + operator + "):" + getQ() + " states", log);
        for (int p = 0; p < getQ(); p++) {
            IntList thisO = this.getO();
            int thisP = thisO.getInt(p);
            thisO.set(p,
                reverse ? ArithmeticOperator.arith2(operator, thisP, o) : ArithmeticOperator.arith2(operator, o, thisP));
        }
        minimizeSelfWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applied operator (" + operator + "):" + getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }


    /**
     * Sorts states based on their breadth-first order. It also calls sortLabel().
     * The method also removes states that are not reachable from the initial state.
     * In draw() and write() methods, we first canonize the automaton.
     * It is also used in write() method.
     * Before we try to canonize, we check if this automaton is already canonized.
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

        int[] label_permutation = getLabelPermutation(getLabel(), sorted_label);

        /*
         * permuted_A is going to hold the alphabet of the sorted inputs.
         * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
         * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
         * The same logic is behind permuted_encoder.
         */
        List<List<Integer>> permuted_A = permute(getA(), label_permutation);
        List<Integer> permuted_encoder = richAlphabet.getPermutedEncoder(permuted_A);

        //For example encoded_input_permutation[2] = 5 means that encoded input 2 becomes 5 after sorting.
        int[] encoded_input_permutation = new int[getAlphabetSize()];
        for (int i = 0; i < getAlphabetSize(); i++) {
            List<Integer> input = richAlphabet.decode(i);
            List<Integer> permuted_input = permute(input, label_permutation);
            encoded_input_permutation[i] = RichAlphabet.encode(permuted_input, permuted_A, permuted_encoder);
        }

        setLabel(sorted_label);
        setA(permuted_A);
        richAlphabet.setEncoder(permuted_encoder);
        setNS(permute(getNS(), label_permutation));

        this.fa.permuteD(encoded_input_permutation);
    }

    /**
     * Permutes L with regard to permutation.
     * @jn1z notes: However, behavior is *not* what was designed:
     * Expected: "if permutation = [1,2,0] then the return value is [L[1],L[2],L[0]]"
     * Actual:   "if permutation = [1,2,0] then the return value is [L[2],L[0],L[1]]", i.e. the inverse
     * Changing this causes other issues, so we're leaving it.
     * (I suspect as this is the inverse, it ends up not being an issue down the line.)
     * Also: behavior is undefined is permutation size != L.size
     */
    public static <T> List<T> permute(List<T> L, int[] permutation) {
        List<T> R = new ArrayList<>(L);
        for (int i = 0; i < L.size(); i++) {
            R.set(permutation[i], L.get(i));
        }
        return R;
    }

    /**
     * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
     * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
     * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
     */
    static int[] getLabelPermutation(List<String> label, List<String> sorted_label) {
        int[] label_permutation = new int[label.size()];
        for (int i = 0; i < label.size(); i++) {
            label_permutation[i] = sorted_label.indexOf(label.get(i));
        }
        return label_permutation;
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
        this.richAlphabet.clear();
        setNS(null);
        setLabel(null);
        labelSorted = false;
    }

    protected boolean isEmpty() {
        if (fa.isTRUE_FALSE_AUTOMATON()) {
            return !fa.isTRUE_AUTOMATON();
        }
        return this.fa.to_dk_brics_automaton().isEmpty();
    }

    public int getQ0() {
        return fa.getQ0();
    }

    public void setQ0(int q0) {
        this.fa.setQ0(q0);
    }

    public int getQ() {
        return this.fa.getQ();
    }

    public void setQ(int q) {
        this.fa.setQ(q);
    }

    public IntList getO() {
        return this.fa.getO();
    }

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
     * For example, we might have A = [[1,-1],[0,1,2],[0,-1]] and T = [Type.alphabetLetter, Type.arithmetic, Type.alphabetLetter]. So
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
        return richAlphabet.getA();
    }

    public void setA(List<List<Integer>> a) {
        richAlphabet.setA(a);
    }

    public FA getFa() {
        return fa;
    }

    @Override
    public String toString() {
        return "FA:" + fa + richAlphabet + "\nlabel:" + this.label;
    }
}
