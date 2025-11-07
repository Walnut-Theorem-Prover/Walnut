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

import Automata.FA.*;
import Automata.Writer.AutomatonWriter;
import Main.EvalComputations.Token.ArithmeticOperator;
import Main.EvalComputations.Token.LogicalOperator;
import Main.WalnutException;
import Main.Prover;
import Main.Session;
import Main.UtilityMethods;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import it.unimi.dsi.fastutil.ints.*;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;
import static Main.Prover.GV_EXTENSION;
import static Main.Prover.TXT_EXTENSION;

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
        AutomatonWriter.draw(this, Session.getAddressForResult() + name + GV_EXTENSION, predicate, isDFAO);
        String firstAddress = Session.getAddressForResult() + name + TXT_EXTENSION;
        AutomatonWriter.writeToTxtFormat(this, firstAddress);
        // Copy to second location, rather than rewriting.
        try {
            Files.copy(Paths.get(firstAddress), Paths.get(outLibrary + name + TXT_EXTENSION),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            UtilityMethods.printTruncatedStackTrace(e);
        }
    }

    public int determineCombineOutVal(String op) {
      return op.equals(Prover.COMBINE) ? this.combineOutputs.getInt(this.combineIndex) : -1;
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
    @SuppressWarnings("this-escape")
    public Automaton() {
        fa = new FA();
        richAlphabet = new RichAlphabet();
        setNS(new ArrayList<>());
        setLabel(new ArrayList<>());
    }

    /**
     * Initializes a special automaton: true or false.
     * A true automaton, is an automaton that accepts everything. A false automaton is an automaton that accepts nothing.
     * Therefore, M and false is false for every automaton M. We also have that M or true is true for every automaton M.
     *
     * @param truthValue - truth value of special automaton
     */
    public Automaton(boolean truthValue) {
        this();
        fa.setTRUE_FALSE_AUTOMATON(true);
        this.fa.setTRUE_AUTOMATON(truthValue);
    }

    /**
     * Takes a regular expression and the alphabet for that regular expression and constructs the corresponding automaton.
     * For example if the regularExpression = "01*" and alphabet = [0,1,2], then the resulting automaton accepts
     * words of the form 01* over the alphabet {0,1,2}.
     * We actually compute the automaton for regularExpression intersected with alphabet*.
     * So for example if regularExpression = [^4]* and alphabet is [1,2,4], then the resulting
     * automaton accepts (1|2)*
     * An important thing to note here is that the automaton being constructed
     * with this constructor, has only one input, and it is of type AlphabetLetter.
     */
    @SuppressWarnings("this-escape")
    public Automaton(
            String regularExpression,
            List<Integer> alphabet,
            NumberSystem numSys) {
        this();
        if (alphabet == null || alphabet.isEmpty()) throw new WalnutException("empty alphabet is not accepted");
        alphabet = new ArrayList<>(alphabet);
        //The alphabet is a set and does not allow repeated elements. However, the user might enter the command
        //reg myreg {1,1,0,0,0} "10*"; and therefore alphabet = [1,1,0,0,0]. So remove duplicates.
        UtilityMethods.removeDuplicates(alphabet);
        this.richAlphabet.getA().add(alphabet);

        BricsConverter.convertFromBrics(this.fa, alphabet, regularExpression);
        getNS().add(numSys);
    }

    // This handles the generalised case of vectors such as "[0,1]*[0,0][0,1]"
    // TODO - maybe exactly the same as above
    public Automaton(String regularExpression, Integer alphabetSize) {
        this();
        BricsConverter.setFromBricsAutomaton(this.fa, alphabetSize, regularExpression);
    }

    /**
     * Takes an address and constructs the automaton represented by the file referred to by the address
     */
    @SuppressWarnings("this-escape")
    public Automaton(String address) {
        this();
        AutomatonReader.readAutomaton(this, address);
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
        for (int i = 0; i < this.richAlphabet.getA().size(); i++) {
            M.getNS().add(getNS().get(i));
            if (this.isBound())
                M.getLabel().add(getLabel().get(i));
        }
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
            if (NumberSystem.isNSDiffering(N.getNS(), first.getNS(), N.richAlphabet.getA(), first.richAlphabet.getA())) {
                throw new WalnutException("Automata to be unioned must have the same number system(s).");
            }

            // crossProduct requires labelling; make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            N.setLabel(first.getLabel());

            if (op.equals("union")) {
                first = AutomatonLogicalOps.or(first, N, print, prefix, log, LogicalOperator.OR);
            } else if (op.equals("intersect")) {
                first = AutomatonLogicalOps.and(first, N, print, prefix, log);
            } else {
                throw new WalnutException("Internal union/intersect error");
            }

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + COMPUTED + " =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }
    
    public static Automaton readAutomatonFromFile(String automataName) {
        return new Automaton(Session.getReadFileForAutomataLibrary(automataName + TXT_EXTENSION));
    }

    private void normalizeNumberSystems(boolean print, String prefix, StringBuilder log) {
        // set all the number systems to be null.
        boolean switchNS = false;
        List<NumberSystem> numberSystems = new ArrayList<>(getNS().size());
        for (int i = 0; i < getNS().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null && ns.useAllRepresentations()) {
                switchNS = true;
                int max = Collections.max(richAlphabet.getA().get(i));
                numberSystems.add(new NumberSystem(ns.determineBaseNameUnderscore() + (max + 1)));
            } else {
                numberSystems.add(ns);
            }
        }

        if (switchNS) {
            setAlphabet(false, numberSystems, richAlphabet.getA(), print, prefix, log);
            // always print this
            UtilityMethods.logMessage(true,
                prefix + "WARN: The alphabet of the resulting automaton was changed. Use the alphabet command to change as desired.", log);
        }
    }

    public Automaton star(boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "star: " + fa.getQ() + " state automaton", log);

        Automaton N = clone();
        FA.starStates(this.fa, N.fa);
        N.normalizeNumberSystems(print, prefix, log);
        N.forceCanonize();
        N.determinizeAndMinimize(print, prefix, log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "star complete: " + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

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
            UtilityMethods.logMessage(print, prefix + "concatenated =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }

    private Automaton concat(Automaton other, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "concat: " + this.fa.getQ() + " state automaton with " + other.fa.getQ() + " state automaton", log);

        // ensure that N has the same number system as first.
        if (NumberSystem.isNSDiffering(other.getNS(), this.getNS(), this.richAlphabet.getA(), other.richAlphabet.getA())) {
            throw new WalnutException("Automata to be concatenated must have the same number system(s).");
        }

        Automaton N = this.clone();

        int originalQ = this.fa.getQ();

        FA.concatStates(other.fa, N.fa, originalQ);

        N.normalizeNumberSystems(print, prefix, log);

        N.determinizeAndMinimize(print, prefix, log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "concat complete: " + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }


    public void setAlphabet(boolean isDFAO, List<NumberSystem> numberSystems, List<List<Integer>> alphabet, boolean print, String prefix, StringBuilder log) {
        if (alphabet.size() != richAlphabet.getA().size()) {
            throw new WalnutException("The number of alphabets must match the number of alphabets in the input automaton.");
        }
        if (alphabet.size() != numberSystems.size()) {
            throw new WalnutException("The number of alphabets must match the number of number systems.");
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
        M.richAlphabet.setA(alphabet);
        M.setNS(numberSystems);
        M.determineAlphabetSize();
        M.richAlphabet.setupEncoder();

        rebuildTransitions(this.getFa(), this.richAlphabet, M);

        if (isDFAO) {
            WordAutomaton.minimizeSelfWithOutput(M, print, prefix, log);
        } else {
            M.determinizeAndMinimize(print, prefix, log);
        }

        M.forceCanonize();
        M.applyAllRepresentationsWithOutput(print, prefix + " ", log);

        copy(M);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "set alphabet complete:" + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Rebuild transitions based on new alphabet
     */
    private static void rebuildTransitions(FA fa, RichAlphabet oldAlphabet, Automaton M) {
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(M.getFa().getQ());
        for (int q = 0; q < M.getFa().getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (Int2ObjectMap.Entry<IntList> entry: fa.t.getEntriesNfaD(q)) {
                List<Integer> decoded = oldAlphabet.decode(entry.getIntKey());
                if (M.richAlphabet.isInNewAlphabet(decoded)) {
                    // For safety, clone the dest list to avoid aliasing
                    newMap.put(M.richAlphabet.encode(decoded), new IntArrayList(entry.getValue()));
                }
            }
            newD.add(newMap);
        }
        M.getFa().t.setNfaD(newD);
    }

    // TODO: possibly this can just be determined when setA() is called.
    public void determineAlphabetSize() {
        this.fa.setAlphabetSize(richAlphabet.determineAlphabetSize());
    }

    /**
     * @param inputs A list of "+", "-" or null. Indicating how our input will be interpreted in the output automata.
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
     * @param inputs A list of "+", "-" or null. Indicating how our input will be interpreted in the output automata.
     * @param reverse Whether to perform the reverse split operation.
     * @return The modified automaton after the split/reverse split operation.
     */
    public Automaton processSplit(List<ArithmeticOperator.Ops> inputs, boolean reverse, boolean print, String prefix, StringBuilder log) {
        if (getAlphabetSize() == 0) {
            throw new WalnutException("Cannot process split automaton with no inputs.");
        }
        if (inputs.size() != richAlphabet.getA().size()) {
            throw new WalnutException("Split automaton has incorrect number of inputs.");
        }

        Automaton M = clone();
        Set<String> quantifiers = new HashSet<>();
        // Label M with [b0, b1, ..., b(A.size() - 1)]
        List<String> names = new ArrayList<>(richAlphabet.getA().size());
        for (int i = 0; i < richAlphabet.getA().size(); i++) {
            names.add("b" + i);
        }
        M.setLabel(names);

        for (int i = 0; i < inputs.size(); i++) {
            // input is "", "+", or "-"
            ArithmeticOperator.Ops input = inputs.get(i);
            if (input == null) {
                continue;
            }
            NumberSystem ns = getNS().get(i);
            if (ns == null)
                throw new WalnutException("Number system for input " + i + " must be defined.");
            NumberSystem negativeNumberSystem = ns.determineNegativeNS();

            Automaton baseChange = negativeNumberSystem.baseChange.clone();
            String a = "a" + i, b = "b" + i, c = "c" + i;

            if (input.equals(ArithmeticOperator.Ops.PLUS)) {
                baseChange.bind(reverse ? List.of(b, a) : List.of(a, b)); // Use ternary for binding logic
                M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                quantifiers.add(b);
            } else { // inputs.get(i).equals(BasicOp.MINUS)
                baseChange.bind(List.of(reverse ? b : a, c)); // Use ternary for binding logic
                M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                M = AutomatonLogicalOps.and(
                    M,
                    negativeNumberSystem.arithmetic(reverse ? a : b, c, 0, ArithmeticOperator.Ops.PLUS), // Use ternary for arithmetic logic
                    print, prefix, log
                );
                quantifiers.add(b);
                quantifiers.add(c);
            }
        }
        AutomatonQuantification.quantify(M, quantifiers, print, prefix, log);
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
            UtilityMethods.logMessage(print, prefix + COMPUTING + " =>:" + first.fa.getQ() + " states - " + next.fa.getQ() + " states", log);

            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            first.fa.totalize(print, prefix + " ", log);
            next.fa.totalize(print, prefix + " ", log);
            first = ProductStrategies.crossProduct(first, next, Prover.FIRST_OP, print, prefix + " ", log);
            first = WordAutomaton.minimizeWithOutput(first, print, prefix + " ", log);

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + COMPUTED + " =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }
        return first;
    }

    public List<String> findAccepted(int searchLength, int maxNeeded) {
        List<String> accepted = new ArrayList<>(maxNeeded);
        findAcceptedHelper(accepted, maxNeeded, searchLength, 0, new StringBuilder(), this.fa.getQ0());
        return accepted;
    }

    /*
    Find accepted inputs.
     */
    private boolean findAcceptedHelper(
        List<String> accepted, int maxNeeded, int searchLength, int curLength, StringBuilder path, int state) {
        if (curLength == searchLength) {
            // if we reach an accepting state of desired length, we add the string we've formed to our subautomata list
            if (getFa().isAccepting(state)) {
                accepted.add(path.toString());
                if (accepted.size() >= maxNeeded) {
                    return true;
                }
            } else {
                return false;
            }
        }
        boolean singleArity = richAlphabet.getA().size() == 1;
        for (Int2ObjectMap.Entry<IntList> entry : getFa().t.getEntriesNfaD(state)) {
            List<Integer> decodeAx = richAlphabet.decode(entry.getIntKey());
            String input = decodeAx.toString();
            // we remove brackets if we have a single arity input that is between 0 and 9 (and hence unambiguous)
            if (singleArity) {
                if (decodeAx.get(0) >= 0 && decodeAx.get(0) <= 9) {
                    input = input.substring(1, input.length() - 1);
                }
            }
            final int oldLen = path.length();
            StringBuilder appendPath = path.append(input);
            for (int y : entry.getValue()) {
                // if we've already found as much as we need, then there's no need to search further; we propagate the signal
                if (findAcceptedHelper(accepted, maxNeeded, searchLength, curLength + 1, appendPath, y)) {
                    return true;
                }
            }
            path.setLength(oldLen); // <-- backtrack
        }
        return false;
    }

    public void applyAllRepresentations() {
        boolean flag = determineRandomLabel();
        Automaton K = this;
        for (int i = 0; i < richAlphabet.getA().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null && ns.useAllRepresentations()) {
                Automaton N = ns.getAllRepresentations();
                N.bind(List.of(getLabel().get(i)));
                K = AutomatonLogicalOps.and(K, N, false, null, null);
            }
        }
        if (flag)
            unlabel();
        copy(K);
    }

    void applyAllRepresentationsWithOutput(boolean print, String prefix, StringBuilder log) {
        // this can be a word automaton
        boolean flag = determineRandomLabel();
        Automaton K = this;
        for (int i = 0; i < richAlphabet.getA().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null && ns.useAllRepresentations()) {
                Automaton N = ns.getAllRepresentations();
                N.bind(List.of(getLabel().get(i)));
                // NOTE: unlike applyAllRepresentations(), the following combines with "this" automaton rather than K.
                // This appears to be by design, and causes a bug in combine() otherwise.
                K = ProductStrategies.crossProduct(this, N, Prover.IF_OTHER_OP, print, prefix, log);
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
        int aSize = richAlphabet.getA().size();
        List<String> randomNames = new ArrayList<>(aSize);
        for(int i=0;i<aSize;i++) {
            randomNames.add(Integer.toString(i));}
        setLabel(randomNames);
    }

    private void unlabel() {
        setLabel(new ArrayList<>());
        labelSorted = false;
    }

    void copy(Automaton M) {
        fa.setTRUE_FALSE_AUTOMATON(M.fa.isTRUE_FALSE_AUTOMATON());
        fa.setTRUE_AUTOMATON(M.fa.isTRUE_AUTOMATON());
        fa = M.fa.clone();
        richAlphabet = M.richAlphabet.clone();
        setNS(M.getNS());
        setLabel(M.getLabel());
        labelSorted = M.labelSorted;
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
    void forceCanonize() {
        this.fa.setCanonized(false);
        this.canonize();
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
        List<String> sortedLabel = new ArrayList<>(getLabel());
        Collections.sort(sortedLabel);

        /*
         * permutedA is going to hold the alphabet of the sorted inputs.
         * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
         * then labelPermutation = [2,0,1] and permutedA = [[0,1],[1,2,3],[-1,2]].
         * The same logic is behind permutedEncoder.
         */
        int[] labelPermutation = getLabelPermutation(getLabel(), sortedLabel);
        List<List<Integer>> permutedA = permute(richAlphabet.getA(), labelPermutation);
        List<Integer> permutedEncoder = RichAlphabet.determineEncoder(permutedA);

        //For example encoded_input_permutation[2] = 5 means that encoded input 2 becomes 5 after sorting.
        int[] encodedInputPermutation = new int[getAlphabetSize()];
        for (int i = 0; i < getAlphabetSize(); i++) {
            List<Integer> input = richAlphabet.decode(i);
            List<Integer> permutedInput = permute(input, labelPermutation);
            encodedInputPermutation[i] = RichAlphabet.encode(permutedInput, permutedA, permutedEncoder);
        }

        setLabel(sortedLabel);
        richAlphabet.setA(permutedA);
        richAlphabet.setEncoder(permutedEncoder);
        setNS(permute(getNS(), labelPermutation));

        this.fa.permuteNfaD(encodedInputPermutation);
    }

    public void determinizeAndMinimize(boolean print, String prefix, StringBuilder log) {
        // Working with NFA. Let's trim.
        int oldQ = this.fa.getQ();
        Trimmer.trimAutomaton(this.fa);
        if (oldQ != this.fa.getQ()) {
            UtilityMethods.logMessage(print, prefix + "Trimmed to: " + this.fa.getQ() + " states.", log);
        }
        IntSet qqq = new IntOpenHashSet();
        qqq.add(this.fa.getQ0());
        this.determinizeAndMinimize(qqq, print, prefix, log);
    }

    /**
     * Determinize and minimize. Technically, the logging is backwards.
     */
    public void determinizeAndMinimize(IntSet qqq, boolean print, String prefix, StringBuilder log) {
        DeterminizationStrategies.determinize(this, qqq, print, prefix + " ", log);
        this.fa.justMinimize(print, prefix + " ", log);
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
    static int[] getLabelPermutation(List<String> label, List<String> sortedLabel) {
        int[] labelPermutation = new int[label.size()];
        for (int i = 0; i < label.size(); i++) {
            labelPermutation[i] = sortedLabel.indexOf(label.get(i));
        }
        return labelPermutation;
    }

    public void bind(List<String> names) {
        if (fa.isTRUE_FALSE_AUTOMATON() || richAlphabet.getA().size() != names.size()) throw WalnutException.invalidBind();
        setLabel(new ArrayList<>(names));
        labelSorted = false;
        fa.setCanonized(false);
        AutomatonLogicalOps.removeSameInputs(this, 0);
    }

    public boolean isBound() {
      return getLabel() != null && getLabel().size() == richAlphabet.getA().size();
    }

    public int getArity() {
        if (fa.isTRUE_FALSE_AUTOMATON()) return 0;
        return richAlphabet.getA().size();
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
        return this.fa.isLanguageEmpty();
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

    public FA getFa() {
        return fa;
    }

    @Override
    public String toString() {
        return "FA:" + fa + richAlphabet + "\nlabel:" + this.label;
    }
}
