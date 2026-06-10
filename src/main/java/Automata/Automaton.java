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
import Main.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import it.unimi.dsi.fastutil.ints.*;

import static Automata.RichAlphabet.MISSING_REDUCED_DIMENSION_ELT;
import static Main.Prover.*;

/**
 * This class can represent different NFA, DFA, DFAO. (NFAO is not supported.)
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
        AutomatonWriter.writeToGV(this, Session.getAddressForResult() + name + GV_EXTENSION, predicate, isDFAO);
        String firstAddress = Session.getAddressForResult() + name + TXT_EXTENSION;
        AutomatonWriter.writeToTxtFormat(this, firstAddress);
        // Copy to second location, rather than rewriting.
        try {
            Files.copy(Paths.get(firstAddress), Paths.get(outLibrary + name + TXT_EXTENSION),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
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
     * Takes an address and constructs the automaton represented by the file referred to by the address
     * Note: this returns a DFA (or DFAO).
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

    public static Automaton readAutomatonFromFile(String automataName) {
        return new Automaton(Session.getReadFileForAutomataLibrary(automataName + TXT_EXTENSION));
    }
    public static Automaton readAutomatonDFAFromFile(String automataName) {
        return new AutomatonDFA(Session.getReadFileForAutomataLibrary(automataName + TXT_EXTENSION));
    }

    /**
     * Return a DFA-typed version of this automaton. If this object is not already an AutomatonDFA,
     * the returned value is a DFA copy; the original object is not retyped.
     */
    public AutomatonDFA asDFA() {
        return AutomatonDFA.from(this);
    }

        public void normalizeNumberSystems() {
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
            setAlphabet(false, numberSystems, richAlphabet.getA());
            // always print this
            Logging.logMessage(true,
                "WARN: The alphabet of the resulting automaton was changed. Use the alphabet command to change as desired.");
        }
    }


    public void setAlphabet(boolean isDFAO, List<NumberSystem> numberSystems, List<List<Integer>> alphabet) {
        if (alphabet.size() != richAlphabet.getA().size()) {
            throw new WalnutException("The number of alphabets must match the number of alphabets in the input automaton.");
        }
        if (alphabet.size() != numberSystems.size()) {
            throw new WalnutException("The number of alphabets must match the number of number systems.");
        }

        long timeBefore = System.currentTimeMillis();
        if (Logging.shouldPrintDetails()) {
            List<String> nsNames = new ArrayList<>(numberSystems.size());
            for (int i = 0; i < numberSystems.size(); i++) {
                NumberSystem ns = numberSystems.get(i);
                nsNames.add(ns == null ? alphabet.get(i).toString() : ns.toString());
            }
            Logging.logMessage("setting alphabet to " + nsNames);
        }

        Automaton M = clone();
        M.richAlphabet.setA(alphabet);
        M.setNS(numberSystems);
        M.determineAlphabetSize();
        M.richAlphabet.setupEncoder();

        rebuildTransitions(this.getFa(), this.richAlphabet, M);

        if (isDFAO) {
            WordAutomaton.minimizeSelfWithOutput(M);
        } else {
            M.determinizeAndMinimize();
        }

        M.forceCanonize();

        Logging.indent();
        M.applyAllRepresentationsWithOutput();
        Logging.dedent();

        copy(M);

        long timeAfter = System.currentTimeMillis();
        Logging.logMessage("set alphabet complete:" + (timeAfter - timeBefore) + "ms");
    }

    /**
     * Rebuild transitions based on new alphabet
     */
    private static void rebuildTransitions(FA fa, RichAlphabet oldAlphabet, Automaton M) {
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(M.getFa().getQ());
        for (int q = 0; q < M.getFa().getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (Int2ObjectMap.Entry<IntList> entry: fa.getT().getEntriesNfaD(q)) {
                List<Integer> decoded = oldAlphabet.decode(entry.getIntKey());
                if (M.richAlphabet.isInNewAlphabet(decoded)) {
                    // For safety, clone the dest list to avoid aliasing
                    newMap.put(M.richAlphabet.encode(decoded), new IntArrayList(entry.getValue()));
                }
            }
            newD.add(newMap);
        }
        M.getFa().setNfaTransitions(newD);
    }

    // TODO: possibly this can just be determined when setA() is called.
    public void determineAlphabetSize() {
        this.fa.setAlphabetSize(richAlphabet.determineAlphabetSize());
    }

    public void applyAllRepresentations() {
        boolean flag = determineRandomLabel();
        Automaton K = this;
        for (int i = 0; i < richAlphabet.getA().size(); i++) {
            NumberSystem ns = getNS().get(i);
            if (ns != null && ns.useAllRepresentations()) {
                Automaton N = ns.getAllRepresentations();
                N.bind(List.of(getLabel().get(i)));
                Logging.disablePrint();
                K = AutomatonLogicalOps.and(K, N);
                Logging.enablePrint();
            }
        }
        if (flag)
            unlabel();
        copy(K);
    }

    void applyAllRepresentationsWithOutput() {
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
                K = ProductStrategies.crossProduct(this, N, Prover.IF_OTHER_OP);
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
    public void forceCanonize() {
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
    public void sortLabel() {
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
        IntList permutedEncoder = RichAlphabet.determineEncoder(permutedA);

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

    public void determinizeAndMinimize() {
        Logging.indent();
        if (!this.fa.getT().isDeterministic()) {
            // Working with NFA. Let's trim.
            int oldQ = this.fa.getQ();
            Trimmer.trimAutomaton(this.fa);
            if (oldQ != this.fa.getQ()) {
                Logging.logMessage("Trimmed to: " + this.fa.getQ() + " states.");
            }
            IntSet qqq = new IntOpenHashSet();
            qqq.add(this.fa.getQ0());
            DeterminizationStrategies.determinize(this, qqq);
        }
        this.fa.justMinimize();
        Logging.dedent();
    }

    /**
     * Determinize and minimize. Technically, the logging is backwards.
     */
    public void determinizeAndMinimize(IntSet qqq) {
        DeterminizationStrategies.determinize(this, qqq);
        this.fa.justMinimize();
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
    static <T> List<T> permute(List<T> L, int[] permutation) {
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
        removeSameInputs(this, 0);
    }

    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately.
     * So for example an expression like f(a,a) becomes an automaton with one input.
     * After we are done with input i, we call removeSameInputs(i+1)
     */
    private static void removeSameInputs(Automaton A, int i) {
        if (i >= A.richAlphabet.getA().size()) return;
        List<Integer> I = new ArrayList<>();
        I.add(i);
        for (int j = i + 1; j < A.richAlphabet.getA().size(); j++) {
            if (A.getLabel().get(i).equals(A.getLabel().get(j))) {
                if (!UtilityMethods.areEqual(A.richAlphabet.getA().get(i), A.richAlphabet.getA().get(j))) {
                    throw new WalnutException("Inputs " + i + " and " + j + " have the same label but different alphabets.");
                }
                I.add(j);
            }
        }
        if (I.size() > 1) {
            reduceDimension(A, I);
        }
        removeSameInputs(A, i + 1);
    }

    private static void reduceDimension(Automaton A, List<Integer> I) {
        List<Integer> reducedDimensionMap = A.richAlphabet.determineReducedDimensionMap(A.getAlphabetSize(), I);

        int Q = A.fa.getQ();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
        for (int q = 0; q < Q; q++) {
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            newD.add(currentStatesTransition);
            for (Int2ObjectMap.Entry<IntList> entry : A.fa.getT().getEntriesNfaD(q)) {
                int m = reducedDimensionMap.get(entry.getIntKey());
                if (m != MISSING_REDUCED_DIMENSION_ELT) {
                    final int presize = entry.getValue().size();
                    currentStatesTransition.computeIfAbsent(
                        m, key -> new IntArrayList(presize)).addAll(entry.getValue());
                }
            }
        }
        A.fa.setNfaTransitions(newD);
        I.remove(0);
        UtilityMethods.removeIndices(A.getNS(), I);
        A.determineAlphabetSize();
        UtilityMethods.removeIndices(A.getLabel(), I);
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
