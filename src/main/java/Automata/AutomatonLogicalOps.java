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
import Main.EvalComputations.Token.LogicalOperator;
import Main.EvalComputations.Token.Operator;
import Main.Prover;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.*;

import java.util.*;

public class AutomatonLogicalOps {

    /**
     * @return A and B.
     */
    public static Automaton and(Automaton A,
                                Automaton B,
                                boolean print,
                                String prefix,
                                StringBuilder log) {
        return and(A, B, print, prefix, log, LogicalOperator.AND);
    }
    public static Automaton and(Automaton A,
                                Automaton B,
                                boolean print,
                                String prefix,
                                StringBuilder log,
                                String friendlyOp) {

        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? B : new Automaton(false);
            }
            return and(B, A, print, prefix, log, friendlyOp); // and is symmetric
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.fa.getQ() + " states - " + B.fa.getQ() + " states", log);

        Automaton N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    /**
     * @return this A or M
     */
    public static Automaton or(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? new Automaton(true): B;
            }
            return or(B, A, print, prefix, log, friendlyOp); // or is symmetric
        }
        return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    /**
     * @return A xor B
     */
    public static Automaton xor(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                if (A.fa.isTRUE_AUTOMATON()) {
                    not(B, print, prefix, log);
                }
                return B;
            }
            return xor(B, A, print, prefix, log, friendlyOp); // xor is symmetric
        }
      return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    /**
     * @return A imply B
     */
    public static Automaton imply(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            // not a or b
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? B : new Automaton(true);
            }
            if (B.fa.isTRUE_AUTOMATON()) {
                return new Automaton(true);
            } else {
                not(A, print, prefix, log);
                return A;
            }
        }
      return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    private static Automaton totalizeCrossProduct(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.fa.getQ() + " states - " + B.fa.getQ() + " states", log);

        A.fa.totalize(print, prefix + " ", log);
        B.fa.totalize(print, prefix + " ", log);
        Automaton N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return N;
    }

    /**
     * @return A iff B
     */
    public static Automaton iff(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            Automaton C = imply(A, B, print, prefix, log, LogicalOperator.IMPLY);
            Automaton D = imply(B, A, print, prefix, log, LogicalOperator.IMPLY);
            return and(C, D, print, prefix, log, LogicalOperator.AND);
        }

      return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    /**
     * @return negation of A
     */
    public static void not(Automaton A, boolean print, String prefix, StringBuilder log) {
        not(A, print, prefix, log, Operator.NEGATE);
    }

    private static void not(Automaton A, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) {
            A.fa.setTRUE_AUTOMATON(!A.fa.isTRUE_AUTOMATON());
            return;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.fa.getQ() + " states", log);

        A.fa.totalize(print, prefix + " ", log);
        A.fa.flipOutput();

        A.determinizeAndMinimize(print, prefix + " ", log);
        A.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * If this automaton's language is L_1 and the language of "B" is L_2, the result accepts the language
     * L_1 / L_2 = { x : exists y in L_2 such that xy in L_1 }
     */
    public static Automaton rightQuotient(Automaton A, Automaton B, boolean skipSubsetCheck,
                                          boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient: " + A.fa.getQ() + " state A with " + B.fa.getQ() + " state A", log);

        if (!skipSubsetCheck) {
            // check whether the alphabet of B is a subset of the alphabet of self. If not, throw an error.
            if (!RichAlphabet.isSubsetA(B.richAlphabet, A.richAlphabet)) {
                throw new WalnutException("Second A's alphabet must be a subset of the first A's alphabet for right quotient.");
            }
        }

        // The returned A will have the same states and transition function as this A, but
        // the final states will be different.
        Automaton M = A.clone();

        Automaton otherClone = B.clone();

        List<Int2ObjectRBTreeMap<IntList>> newOtherD = new ArrayList<>(otherClone.fa.getQ());

        for (int q = 0; q < otherClone.fa.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (Int2ObjectMap.Entry<IntList> entry : otherClone.fa.t.getEntriesNfaD(q)) {
                newMap.put(A.richAlphabet.encode(otherClone.richAlphabet.decode(entry.getIntKey())), entry.getValue());
            }
            newOtherD.add(newMap);
        }
        otherClone.fa.t.setNfaD(newOtherD);
        otherClone.richAlphabet.setEncoder(A.richAlphabet.getEncoder());
        otherClone.richAlphabet.setA(A.richAlphabet.getA());
        otherClone.setAlphabetSize(A.getAlphabetSize());
        otherClone.setNS(A.getNS());

        for (int i = 0; i < A.fa.getQ(); i++) {
            // this will be a temporary A that will be the same as self except it will start from the A
            Automaton T = A.clone();

            if (i != 0) {
                T.fa.setQ0(i);
                T.fa.setCanonized(false);
                T.canonize();
            }

            // need to have the same label for cross product (including "and")
            T.randomLabel();
            otherClone.setLabel(T.getLabel());

            Automaton I = and(T, otherClone, print, prefix, log);

            M.fa.setOutputIfEqual(i, !I.isEmpty());
        }

        M.determinizeAndMinimize(print, prefix, log);
        M.applyAllRepresentations();
        M.fa.setCanonized(false);
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient complete: " + M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    public static Automaton leftQuotient(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient: " + A.fa.getQ() + " state A with " + B.fa.getQ() + " state A", log);

        // check whether the alphabet of self is a subset of the alphabet of B. If not, throw an error.
        if (!RichAlphabet.isSubsetA(A.richAlphabet, B.richAlphabet)) {
            throw new WalnutException("First A's alphabet must be a subset of the second A's alphabet for left quotient.");
        }

        Automaton M1 = reverseAndCanonize(A, print, prefix, log);
        Automaton M2 = reverseAndCanonize(B, print, prefix, log);
        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        reverse(M, print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient complete: " + M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    private static Automaton reverseAndCanonize(Automaton A, boolean print, String prefix, StringBuilder log) {
        Automaton M1 = A.clone();
        reverse(M1, print, prefix, log, true);
        M1.fa.setCanonized(false);
        M1.canonize();
        return M1;
    }

    /**
     * Make A accept 0*x, iff it used to accept x.
     */
    public static void fixLeadingZerosProblem(Automaton A, boolean print, String prefix, StringBuilder log) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixing leading zeros:" + A.fa.getQ() + " states", log);
        A.fa.setCanonized(false);
        int zero = determineZero(A.richAlphabet);

        // Subset Construction with different initial state
        IntSet initial_state = zeroReachableStates(A.fa, zero);
        A.determinizeAndMinimize(initial_state, print, prefix, log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixed leading zeros:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Returns the set of states reachable from the initial state by reading 0*
     * This can alter FA itself
     */
    private static IntSet zeroReachableStates(FA fa, int zero) {
        // Ensure q0 is initialized in nfaD
        IntList dQ0 = fa.t.getNfaState(fa.getQ0()).computeIfAbsent(zero, k -> new IntArrayList());
        if (!dQ0.contains(fa.getQ0())) {
            dQ0.add(fa.getQ0());
        }

        // Perform BFS to find zero-reachable states
        IntSet result = new IntOpenHashSet();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(fa.getQ0());

        while (!queue.isEmpty()) {
            int q = queue.poll();
            if (result.add(q)) { // Add q to result; skip if already processed
                IntList transitions = fa.t.getNfaStateDests(q, zero);
                if (transitions != null) {
                    for (int p : transitions) {
                        if (!result.contains(p)) {
                            queue.add(p);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static int determineZero(RichAlphabet richAlphabet) {
        List<Integer> ZERO = new ArrayList<>(richAlphabet.getA().size());//all zero input
        for (List<Integer> i : richAlphabet.getA()) ZERO.add(i.indexOf(0));
        return richAlphabet.encode(ZERO);
    }

    /**
     * Make automaton accept x0*, iff it used to accept x.
     */
    public static void fixTrailingZerosProblem(Automaton A, boolean print, String prefix, StringBuilder log) {
        if (A.fa.setStatesReachableToFinalStatesByZeros(determineZero(A.richAlphabet))) {
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "fixing trailing zeros:" + A.fa.getQ() + " states", log);
            A.fa.setCanonized(false);
            // We don't have to determinize, since all that was altered was final states
            A.fa.justMinimize(print, prefix + " ", log);
            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "fixed trailing zeros:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        } else {
            UtilityMethods.logMessage(print, prefix + "fixing trailing zeros: no change necessary.", log);
        }
    }

    /**
     * Used for the "I" quantifier. If some input is in msd, then we remove leading zeroes,
     * if some input is in lsd, then we remove trailing zeroes, otherwise, we do nothing.
     * To do this, for each input, we construct an automaton which accepts if the leading/trailing input is non-zero,
     * union all these automata together, and intersect with our original automaton.
     */
    public static Automaton removeLeadingZeroes(Automaton A, List<String> listOfLabels, boolean print, String prefix, StringBuilder log) {
        AutomatonQuantification.validateLabels(A, listOfLabels);
        if (listOfLabels.isEmpty()) {
            return A.clone();
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "removing leading zeroes for:" + A.fa.getQ() + " states", log);

        List<Integer> listOfInputs = new ArrayList<>(listOfLabels.size());
        //extract the list of indices of inputs from the list of labels
        for (String l : listOfLabels) {
            listOfInputs.add(A.getLabel().indexOf(l));
        }
        Automaton M = new Automaton(false);
        for (int n : listOfInputs) {
            Automaton N = removeLeadingZeroesHelper(A, n, print, prefix + " ", log);
            M = or(M, N, print, prefix + " ", log, LogicalOperator.OR);
        }
        M = and(A, M, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantified:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    /**
     * Returns the automaton with the same alphabet as the current A, which requires the nth input to start with a
     * non-zero symbol (if msd), end with a non-zero symbol (if lsd), otherwise, return the true automaton.
     * The returned automaton is meant to be intersected with the current A to remove leading/trailing * zeroes
     * from the nth input.
     */
    private static Automaton removeLeadingZeroesHelper(
        Automaton A, int n, boolean print, String prefix, StringBuilder log) {
        if (n >= A.richAlphabet.getA().size() || n < 0) {
            throw new WalnutException("Cannot remove leading zeroes for the "
                    + (n + 1) + "-th input when A only has " + A.richAlphabet.getA().size() + " inputs.");
        }

        if (A.getNS().get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.fa.initBasicFA(IntList.of(1,1));
        M.setNS(A.getNS());
        M.richAlphabet.setA(A.richAlphabet.getA());
        M.setLabel(A.getLabel());
        M.setAlphabetSize(A.getAlphabetSize());

        IntList dest = new IntArrayList();
        dest.add(1);
        for (int i = 0; i < A.getAlphabetSize(); i++) {
            List<Integer> list = A.richAlphabet.decode(i);
            if (list.get(n) != 0) {
                M.fa.t.setNfaDTransition(0, i, new IntArrayList(dest));
            }
            M.fa.t.setNfaDTransition(1, i, new IntArrayList(dest));
        }
        if (!A.getNS().get(n).isMsd()) {
            reverse(M, print, prefix, log, false);
        }
        return M;
    }


    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately.
     * So for example an expression like f(a,a) becomes an automaton with one input.
     * After we are done with input i, we call removeSameInputs(i+1)
     */
    static void removeSameInputs(Automaton A, int i) {
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
        List<Integer> map = A.richAlphabet.determineReducedDimensionMap(A.getAlphabetSize(), I);

        int Q = A.fa.getQ();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
        for (int q = 0; q < Q; q++) {
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            newD.add(currentStatesTransition);
            for (Int2ObjectMap.Entry<IntList> entry : A.fa.t.getEntriesNfaD(q)) {
                int m = map.get(entry.getIntKey());
                if (m != -1) {
                    currentStatesTransition.computeIfAbsent(m, key -> new IntArrayList()).addAll(entry.getValue());
                }
            }
        }
        A.fa.t.setNfaD(newD);
        I.remove(0);
        UtilityMethods.removeIndices(A.getNS(), I);
        A.determineAlphabetSize();
        UtilityMethods.removeIndices(A.getLabel(), I);
    }

    /**
     * This automaton should not be a word Automaton (i.e., with output). However, it can be NFA.
     * Enabling the reverseMsd flag will flip the number system of the A from msd to lsd, and vice versa.
     * Reversing the Msd will also call this function as reversals are done in the NumberSystem class upon
     * initializing.
     */
    public static void reverse(
        Automaton A, boolean print, String prefix, StringBuilder log, boolean reverseMsd) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "Reversing:" + A.fa.getQ() + " states", log);

        IntSet setOfFinalStates = A.fa.reverseToNFAInternal(IntSet.of(A.fa.getQ0()));
        A.determinizeAndMinimize(setOfFinalStates, print, prefix + " ", log);

        if (reverseMsd) {
            NumberSystem.flipNS(A.getNS());
        }

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "reversed:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    static void removeStatesWithMinOutput(Automaton N, int minOutput) {
        removeStatesWithMinOutput(N.fa, minOutput);
        N.fa.setCanonized(false);
        N.canonize();
    }

    // remove all states that have an output of minOutput
    private static void removeStatesWithMinOutput(FA fa, int minOutput) {
        Set<Integer> statesToRemove = new HashSet<>();
        for (int q = 0; q < fa.getQ(); q++) {
            if (fa.getO().getInt(q) == minOutput) {
                statesToRemove.add(q);
            }
        }
        for (int q = 0; q < fa.getQ(); q++) {
            fa.t.getEntriesNfaD(q).removeIf(entry -> statesToRemove.contains(entry.getValue().getInt(0)));
        }
    }

    /**
     * Convert the number system of an automaton from [msd/lsd]_k^i to [msd/lsd]_k^j.
     * TODO: this assumes that A is a word automaton when it may not be.
     * It probably doesn't matter, but it would be good to separate the two.
     */
    public static void convertNS(Automaton A, boolean toMsd, int toBase,
                                 boolean print, String prefix, StringBuilder log) {
        if (A.getNS().size() != 1) {
            throw new WalnutException("Automaton must have exactly one input to be converted.");
        }

        NumberSystem ns = A.getNS().get(0);
        // 1) Parse the base from the A’s NS
        int fromBase = ns.parseBase();

        // If the old and new bases are the same, check if only MSD/LSD is changing
        if (fromBase == toBase) {
            if (ns.isMsd() == toMsd) {
                throw new WalnutException("New and old number systems are identical: " + ns.getName());
            } else {
                // If only msd <-> lsd differs, just reverse A
                WordAutomaton.reverseWithOutput(A, true, print, prefix + " ", log);
                return;
            }
        }

        // 2) Check if fromBase and toBase are powers of the same root
        int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);
        if (commonRoot == -1) {
            throw new WalnutException("New and old number systems must have bases k^i and k^j for some integer k.");
        }

        // If originally LSD, we need to reverse to treat it as MSD for the conversions
        if (!ns.isMsd()) {
            WordAutomaton.reverseWithOutput(A, true, print, prefix + " ", log);
        }

        // We'll track if A is reversed relative to original
        boolean currentlyReversed = false;

        // 3) Convert from k^i -> k if needed
        if (fromBase != commonRoot) {
            int exponent = (int) (Math.log(fromBase) / Math.log(commonRoot));
            WordAutomaton.reverseWithOutput(A, true, print, prefix + " ", log);
            currentlyReversed = true;

            convertLsdBaseToRoot(A, commonRoot, exponent, print, prefix + " ", log);
            WordAutomaton.minimizeSelfWithOutput(A, print, prefix + " ", log);
        }

        // 4) Convert from k -> k^j if needed
        if (toBase != commonRoot) {
            if (currentlyReversed) {
                // Undo reversal from the previous step
                WordAutomaton.reverseWithOutput(A, true, print, prefix + " ", log);
                currentlyReversed = false;
            }
            int exponent = (int) (Math.log(toBase) / Math.log(commonRoot));
            convertMsdBaseToExponent(A, exponent, print, prefix + " ", log);
            WordAutomaton.minimizeSelfWithOutput(A, print, prefix + " ", log);
        }

        // 5) If final desired base is LSD but we are still in MSD form, reverse again
        if (toMsd == currentlyReversed) {
            WordAutomaton.reverseWithOutput(A, true, print, prefix + " ", log);
        }
    }

    /**
     * Assuming this automaton is in number system msd_k with one input,
     * convert it to number system msd_{k^exponent} with one input.
     */
    private static void convertMsdBaseToExponent(Automaton A, int exponent,
                                                 boolean print, String prefix, StringBuilder log) {
        if (!A.fa.isDeterministicAndTotal()) {
            throw new WalnutException("Automaton must be deterministic for msd_k^j conversion");
        }

        int base = A.getNS().get(0).parseBase();

        long timeBefore = System.currentTimeMillis();
        int newBase = (int) Math.pow(base, exponent);
        String msdUnderscore = NumberSystem.MSD_UNDERSCORE;

        UtilityMethods.logMessage(
                print,
                prefix + "Converting: " + msdUnderscore + base + " to " +
                    msdUnderscore + newBase +
                    ", " + A.fa.getQ() + " states",
                log
        );

        updateTransitionsFromMorphism(A.fa, exponent);

        // Update number system: msd_{base^exponent}
        A.getNS().set(0, new NumberSystem(msdUnderscore + newBase));
        setAutomatonAlphabet(A, newBase);

        UtilityMethods.logMessage(print, prefix + "Converted: " + msdUnderscore + base + " to " +
            msdUnderscore + newBase +
            ", " + A.fa.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms", log);
    }

    /**
     * Assuming this automaton is in number system lsd_{k^j} with one input,
     * convert it to number system lsd_k with one input.
     */
    private static void convertLsdBaseToRoot(Automaton A, int root, int exponent,
                                             boolean print, String prefix, StringBuilder log) {
        // Parse base and validate
        int base = A.getNS().get(0).parseBase();
        double expected = Math.pow(root, exponent);
        if (base != (int) expected) {
            throw new WalnutException("Base mismatch: expected " + (int) expected + ", found " + base);
        }
        final String lsdUnderscore = NumberSystem.LSD_UNDERSCORE;

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(
                print,
                prefix + "Converting: " + lsdUnderscore + base + " to " +
                    lsdUnderscore + (int) expected +
                    ", " + A.fa.getQ() + " states",
                log
        );

        IntList oldO = A.fa.getO();
        List<Int2ObjectRBTreeMap<IntList>> oldD = A.fa.t.getNfaD();

        // Prepare BFS structures
        List<IntObjectPair<IntList>> newStates = new ArrayList<>();
        Queue<IntObjectPair<IntList>> queue = new LinkedList<>();
        Map<IntObjectPair<IntList>, Integer> stateMap = new HashMap<>();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
        IntList newO = new IntArrayList();

        // Initialize BFS with the A's Q0
        IntObjectPair<IntList> init = new IntObjectImmutablePair<>(A.fa.getQ0(), IntList.of());
        newStates.add(init);
        queue.add(init);
        stateMap.put(init, newStates.size() - 1);

        // BFS
        while (!queue.isEmpty()) {
            IntObjectPair<IntList> curr = queue.remove();

            // Create a new transition map in newD
            newD.add(new Int2ObjectRBTreeMap<>());

            // Output logic
            if (curr.right().isEmpty()) {
                newO.add(oldO.getInt(curr.leftInt()));
            } else {
                int stringVal = computeStringValue(curr.right(), root);
                // The next real state is oldD.get(curr.state).get(stringVal).getInt(0)
                int realState = oldD.get(curr.leftInt()).get(stringVal).getInt(0);
                newO.add(oldO.getInt(realState));
            }

            // Build transitions for each possible digit di in [0..root-1]
            for (int di = 0; di < root; di++) {
                IntList nextString = new IntArrayList(curr.right());
                nextString.add(di);

                IntObjectPair<IntList> next;
                if (curr.right().size() < exponent - 1) {
                    // Haven't reached exponent length yet
                    next = new IntObjectImmutablePair<>(curr.leftInt(), nextString);
                } else {
                    // We have a full 'digit string', so jump to an actual next state
                    int nextStringVal = computeStringValue(nextString, root);
                    int realState = oldD.get(curr.leftInt()).get(nextStringVal).getInt(0);
                    next = new IntObjectImmutablePair<>(realState, IntList.of());
                }

                // If this state is new, register it
                if (!stateMap.containsKey(next)) {
                    newStates.add(next);
                    queue.add(next);
                    stateMap.put(next, newStates.size() - 1);
                }

                // Add transition
                IntList destList = new IntArrayList();
                destList.add((int)stateMap.get(next));
                newD.get(stateMap.get(curr)).put(di, destList);
            }
        }

        // Update A
        A.fa.setFields(newStates.size(), newO, newD);

        A.fa.setCanonized(false);

        // Update number system to lsd_root
        A.getNS().set(0, new NumberSystem(lsdUnderscore + root));
        setAutomatonAlphabet(A, root);

        UtilityMethods.logMessage(
                print,
                prefix + prefix + "Converted: " + lsdUnderscore + base +
                    " to " + lsdUnderscore + (int) expected +
                    ", " + A.fa.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms",
                log);
    }

    /**
     * Updates the automaton's alphabet to [0..newBase-1] and sets alphabetSize accordingly.
     */
    private static void setAutomatonAlphabet(Automaton A, int newBase) {
        List<Integer> ints = new ArrayList<>(newBase);
        for (int i = 0; i < newBase; i++) {
            ints.add(i);
        }
        A.richAlphabet.setA(List.of(ints));
        A.setAlphabetSize(newBase);
    }

    /**
     * Compute the numeric value of a 'digit' list in the given root^position sense.
     * (Used in convertLsdBaseToRoot BFS)
     */
    private static int computeStringValue(List<Integer> digits, int root) {
        int value = 0;
        for (int i = 0; i < digits.size(); i++) {
            value += (int) (digits.get(i) * Math.pow(root, i));
        }
        return value;
    }

    public static Automaton combine(
            Automaton A, Queue<Automaton> subautomata, IntList outputs, boolean print, String prefix, StringBuilder log) {
        Automaton first = A.clone();

        // In an A without output, every non-zero output value represents an accepting state
        // we change this to correspond to the value assigned to the first A by our command
        for (int q = 0; q < first.fa.getQ(); q++) {
            if (first.fa.isAccepting(q)) {
                first.fa.getO().set(q, outputs.getInt(0));
            }
        }
        first.combineIndex = 1;
        first.combineOutputs = outputs;
        while (!subautomata.isEmpty()) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computing =>:" + first.fa.getQ() + " states - " + next.fa.getQ() + " states", log);

            // crossProduct requires labelling; make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.setLabel(first.getLabel());
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            first.fa.totalize(print, prefix + " ", log);
            next.fa.totalize(print, prefix + " ", log);
            Automaton product = ProductStrategies.crossProduct(first, next, Prover.COMBINE, print, prefix + " ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computed =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }

        // totalize the resulting A
        first.fa.totalize(print, prefix + " ", log);
        first.canonizeAndApplyAllRepresentationsWithOutput(print, prefix + " ", log);

        return first;
    }

    /**
     * Build transitions from the final morphism matrix. Used in convertMsdBaseToExponent.
     */
    private static List<Int2ObjectRBTreeMap<IntList>> buildTransitionsFromMorphism(FA fa, List<List<Integer>> morphism) {
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(fa.getQ());
        for (int q = 0; q < fa.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> transitionMap = new Int2ObjectRBTreeMap<>();
            List<Integer> row = morphism.get(q);
            for (int di = 0; di < row.size(); di++) {
                IntList list = new IntArrayList();
                list.add((int)row.get(di));
                transitionMap.put(di, list);
            }
            newD.add(transitionMap);
        }
        return newD;
    }

    /**
     * Extend morphism by applying the automaton transitions again.
     */
    private static void updateTransitionsFromMorphism(FA fa, int exponent) {
        List<List<Integer>> prevMorphism = buildInitialMorphism(fa);
        // Repeatedly extend the morphism exponent-1 more times
        for (int i = 2; i <= exponent; i++) {
          List<List<Integer>> newMorphism = new ArrayList<>(fa.getQ());
          for (int j = 0; j < fa.getQ(); j++) {
            List<Integer> extendedRow = new ArrayList<>();
            for (int k = 0; k < prevMorphism.get(j).size(); k++) {
              // For each digit di in state j:
              for (int di : fa.t.getNfaStateKeySet(j)) {
                int nextState = fa.t.getNfaStateDests(prevMorphism.get(j).get(k), di).getInt(0);
                extendedRow.add(nextState);
              }
            }
            newMorphism.add(extendedRow);
          }
          prevMorphism = newMorphism;
        }
        // Create new transitions from the final morphism
        fa.t.setNfaD(buildTransitionsFromMorphism(fa, prevMorphism));
    }

    /**
     * Build the initial morphism from the automaton transitions.
     * (Used in convertMsdBaseToExponent)
     */
    private static List<List<Integer>> buildInitialMorphism(FA fa) {
      List<List<Integer>> result = new ArrayList<>(fa.getQ());
      for (int q = 0; q < fa.getQ(); q++) {
        List<Integer> row = new ArrayList<>(fa.getAlphabetSize());
        for (int di = 0; di < fa.getAlphabetSize(); di++) {
          row.add(fa.t.getNfaStateDests(q, di).getInt(0));
        }
        result.add(row);
      }
      return result;
    }
}
