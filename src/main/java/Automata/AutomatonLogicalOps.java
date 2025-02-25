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
import Main.EvalComputations.Token.RelationalOperator;
import Main.UtilityMethods;
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
        return and(A, B, print, prefix, log, "&");
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
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.getQ() + " states - " + B.getQ() + " states", log);

        Automaton N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

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

        System.out.println("IMPLY");

      return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    private static Automaton totalizeCrossProduct(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.getQ() + " states - " + B.getQ() + " states", log);

        A.fa.totalize(print, prefix + " ", log);
        B.fa.totalize(print, prefix + " ", log);
        Automaton N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return N;
    }

    /**
     * @param A
     * @param B
     * @param friendlyOp
     * @return A iff B
     */
    public static Automaton iff(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            Automaton C = imply(A, B, print, prefix, log, "=>");
            Automaton D = imply(B, A, print, prefix, log, "=>");
            return and(C, D, print, prefix, log, "&");
        }

      return totalizeCrossProduct(A, B, print, prefix, log, friendlyOp);
    }

    /**
     * @return negation of A
     */
    public static void not(Automaton A, boolean print, String prefix, StringBuilder log) {
        not(A, print, prefix, log, "~");
    }

    private static void not(Automaton A, boolean print, String prefix, StringBuilder log, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) {
            A.fa.setTRUE_AUTOMATON(!A.fa.isTRUE_AUTOMATON());
            return;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing " + friendlyOp + ":" + A.getQ() + " states", log);

        A.fa.totalize(print, prefix + " ", log);
        for (int q = 0; q < A.getQ(); q++)
            A.getO().set(q, A.getFa().isAccepting(q) ? 0 : 1);

        A.fa.determinizeAndMinimize(print, prefix + " ", log);
        A.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed " + friendlyOp + ":" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * If this A's language is L_1 and the language of "B" is L_2, this A accepts the language
     * L_1 / L_2 = { x : exists y in L_2 such that xy in L_1 }
     * @return
     */
    public static Automaton rightQuotient(Automaton A, Automaton B, boolean skipSubsetCheck, boolean print, String prefix, StringBuilder log) {

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient: " + A.getQ() + " state A with " + B.getQ() + " state A", log);

        if (!skipSubsetCheck) {
            // check whether the alphabet of B is a subset of the alphabet of self. If not, throw an error.
            if (!isSubsetA(B.getA(), A.getA())) {
                throw new RuntimeException("Second A's alphabet must be a subset of the first A's alphabet for right quotient.");
            }
        }

        // The returned A will have the same states and transition function as this A, but
        // the final states will be different.
        Automaton M = A.clone();

        Automaton otherClone = B.clone();

        List<Int2ObjectRBTreeMap<IntList>> newOtherD = new ArrayList<>(otherClone.getQ());

        for (int q = 0; q < otherClone.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (Int2ObjectMap.Entry<IntList> entry : otherClone.getFa().getEntriesNfaD(q)) {
                newMap.put(A.richAlphabet.encode(otherClone.richAlphabet.decode(entry.getIntKey())), entry.getValue());
            }
            newOtherD.add(newMap);
        }
        otherClone.setD(newOtherD);
        otherClone.richAlphabet.setEncoder(A.richAlphabet.getEncoder());
        otherClone.setA(A.getA());
        otherClone.setAlphabetSize(A.getAlphabetSize());
        otherClone.setNS(A.getNS());

        for (int i = 0; i < A.getQ(); i++) {
            // this will be a temporary A that will be the same as self except it will start from the A
            Automaton T = A.clone();

            if (i != 0) {
                T.setQ0(i);
                T.fa.setCanonized(false);
                T.canonize();
            }

            // need to have the same label for cross product (including "and")
            T.randomLabel();
            otherClone.setLabel(T.getLabel());

            Automaton I = and(T, otherClone, print, prefix, log);

            M.getO().set(i, I.isEmpty() ? 0 : 1);
        }

        M.fa.determinizeAndMinimize(print, prefix, log);
        M.applyAllRepresentations();
        M.fa.setCanonized(false);
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    public static Automaton leftQuotient(Automaton A, Automaton B, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient: " + A.getQ() + " state A with " + B.getQ() + " state A", log);

        // check whether the alphabet of self is a subset of the alphabet of B. If not, throw an error.
        if (!isSubsetA(A.getA(), B.getA())) {
            throw new RuntimeException("First A's alphabet must be a subset of the second A's alphabet for left quotient.");
        }

        Automaton M1 = reverseAndCanonize(A, print, prefix, log);
        Automaton M2 = reverseAndCanonize(B, print, prefix, log);
        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        reverse(M, print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    private static Automaton reverseAndCanonize(Automaton A, boolean print, String prefix, StringBuilder log) {
        Automaton M1 = A.clone();
        reverse(M1, print, prefix, log, true);
        M1.fa.setCanonized(false);
        M1.canonize();
        return M1;
    }

    private static boolean isSubsetA(List<List<Integer>> aA, List<List<Integer>> otherA) {
        if (aA.size() != otherA.size()) {
            return false;
        }
        for (int i = 0; i < aA.size(); i++) {
            if (!new HashSet<>(otherA.get(i)).containsAll(aA.get(i))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Make A accept 0*x, iff it used to accept x.
     */
    public static void fixLeadingZerosProblem(Automaton A, boolean print, String prefix, StringBuilder log) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixing leading zeros:" + A.getQ() + " states", log);
        A.fa.setCanonized(false);
        int zero = determineZero(A);

        // Subset Construction with different initial state
        IntSet initial_state = A.fa.zeroReachableStates(zero);
        A.fa.determinizeAndMinimize(initial_state, print, prefix, log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixed leading zeros:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    private static int determineZero(Automaton A) {
        List<Integer> ZERO = new ArrayList<>(A.getA().size());//all zero input
        for (List<Integer> i : A.getA()) ZERO.add(i.indexOf(0));
        return A.richAlphabet.encode(ZERO);
    }

    /**
     * Make Automaton accept x0*, iff it used to accept x.
     */
    public static void fixTrailingZerosProblem(Automaton A, boolean print, String prefix, StringBuilder log) {
        if (A.fa.setStatesReachableToFinalStatesByZeros(determineZero(A))) {
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "fixing trailing zeros:" + A.getQ() + " states", log);
            A.fa.setCanonized(false);
            // We don't have to determinize, since all that was altered was final states
            A.fa.justMinimize(print, prefix + " ", log);
            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "fixed trailing zeros:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        } else {
            UtilityMethods.logMessage(print, prefix + "fixing trailing zeros: no change necessary.", log);
        }
    }

    /**
     * Used for the "I" quantifier. If some input is in msd, then we remove leading zeroes,
     * if some input is in lsd, then we remove trailing zeroes, otherwise, we do nothing.
     * To do this, for each input, we construct an A which accepts if the leading/trailing input is non-zero,
     * union all these automata together, and intersect with our original A.
     *
     * @param A
     * @param listOfLabels
     * @return
     */
    public static Automaton removeLeadingZeroes(Automaton A, List<String> listOfLabels, boolean print, String prefix, StringBuilder log) {
        for (String s : listOfLabels) {
            if (!A.getLabel().contains(s)) {
                throw new RuntimeException("Variable " + s + " in the list of quantified variables is not a free variable.");
            }
        }
        if (listOfLabels.isEmpty()) {
            return A.clone();
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "removing leading zeroes for:" + A.getQ() + " states", log);

        List<Integer> listOfInputs = new ArrayList<>(listOfLabels.size());
        //extract the list of indices of inputs from the list of labels
        for (String l : listOfLabels) {
            listOfInputs.add(A.getLabel().indexOf(l));
        }
        Automaton M = new Automaton(false);
        for (int n : listOfInputs) {
            Automaton N = removeLeadingZeroesHelper(A, n, print, prefix + " ", log);
            M = or(M, N, print, prefix + " ", log, "|");
        }
        M = and(A, M, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantified:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    /**
     * Returns the A with the same alphabet as the current A, which requires the nth input to
     * start with a non-zero symbol (if msd), end with a non-zero symbol (if lsd), otherwise, return the true
     * A. The returned A is meant to be intersected with the current A to remove
     * leading/trailing * zeroes from the nth input.
     *
     * @param A
     * @param n
     * @return
     */
    private static Automaton removeLeadingZeroesHelper(
        Automaton A, int n, boolean print, String prefix, StringBuilder log) {
        if (n >= A.getA().size() || n < 0) {
            throw new RuntimeException("Cannot remove leading zeroes for the "
                    + (n + 1) + "-th input when A only has " + A.getA().size() + " inputs.");
        }

        if (A.getNS().get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.getFa().initBasicFA(IntList.of(1,1));
        M.setNS(A.getNS());
        M.setA(A.getA());
        M.setLabel(A.getLabel());
        M.setAlphabetSize(A.getAlphabetSize());

        IntList dest = new IntArrayList();
        dest.add(1);
        for (int i = 0; i < A.getAlphabetSize(); i++) {
            List<Integer> list = A.richAlphabet.decode(i);
            if (list.get(n) != 0) {
                M.getD().get(0).put(i, new IntArrayList(dest));
            }
            M.getD().get(1).put(i, new IntArrayList(dest));
        }
        if (!A.getNS().get(n).isMsd()) {
            reverse(M, print, prefix, log, false);
        }
        return M;
    }


    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately. So for example an
     * expression like f(a,a) becomes
     * an A with one input. After we are done with input i, we call removeSameInputs(i+1)
     *
     * @param A
     * @param i
     */
    static void removeSameInputs(Automaton A, int i) {
        if (i >= A.getA().size()) return;
        List<Integer> I = new ArrayList<>();
        I.add(i);
        for (int j = i + 1; j < A.getA().size(); j++) {
            if (A.getLabel().get(i).equals(A.getLabel().get(j))) {
                if (!UtilityMethods.areEqual(A.getA().get(i), A.getA().get(j))) {
                    throw new RuntimeException("Inputs " + i + " and " + j + " have the same label but different alphabets.");
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

        int Q = A.getFa().getQ();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
        for (int q = 0; q < Q; q++) {
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            newD.add(currentStatesTransition);
            for (Int2ObjectMap.Entry<IntList> entry : A.getFa().getEntriesNfaD(q)) {
                int m = map.get(entry.getIntKey());
                if (m != -1) {
                    currentStatesTransition.computeIfAbsent(m, key -> new IntArrayList()).addAll(entry.getValue());
                }
            }
        }
        A.setD(newD);
        I.remove(0);
        UtilityMethods.removeIndices(A.getNS(), I);
        A.determineAlphabetSize();
        UtilityMethods.removeIndices(A.getLabel(), I);
    }

    /**
     * The operator can be one of "+" "-" "*" "/".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs this[x] + B[x] on input x.
     * To be used only when this A and M are DFAOs (words).
     *
     * @param A
     * @param B
     * @param operator
     * @return
     */
    public static Automaton applyOperator(Automaton A, Automaton B, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applying operator (" + operator + "):" + A.getQ() + " states - " + B.getQ() + " states", log);
        Automaton N = ProductStrategies.crossProduct(A, B, operator, print, prefix + " ", log);
        N.minimizeWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applied operator (" + operator + "):" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return N;
    }

    public static void quantify(Automaton A, String labelToQuantify, boolean print, String prefix, StringBuilder log) {
        quantify(A, Set.of(labelToQuantify), print, prefix, log);
    }
    public static void quantify(Automaton A, List<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
        quantify(A, new HashSet<>(labelsToQuantify), print, prefix, log);
    }

        /**
         * This method computes the existential quantification of this A.
         * Takes a list of labels and performs the existential quantifier over
         * the inputs with labels in listOfLabelsToQuantify. It simply eliminates inputs in listOfLabelsToQuantify.
         * After the quantification is done, we address the issue of
         * leadingZeros or trailingZeros (depending on the value of leadingZeros), if all of the inputs
         * of the resulting A are of type arithmetic.
         * This is why we mandate that an input of type arithmetic must have 0 in its alphabet, also that
         * every number system must use 0 to denote its additive identity.
         *
         * @param A
         * @param labelsToQuantify must contain at least one elemen, and must be a subset of this.label.
         */
    public static void quantify(Automaton A, Set<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
        quantifyHelper(A, labelsToQuantify, print, prefix, log);
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;

        Boolean isMsd = NumberSystem.determineMsd(A.getNS());
        if (isMsd == null) return;
        if (isMsd)
            fixLeadingZerosProblem(A, print, prefix, log);
        else
            fixTrailingZerosProblem(A, print, prefix, log);
    }

    /**
     * This method is very similar to public void quantify(Set<String> listOfLabelsToQuantify,boolean leadingZeros)
     * with the exception that, this method does not deal with leading/trailing zeros problem.
     */
    private static void quantifyHelper(
        Automaton A, Set<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
        if (labelsToQuantify.isEmpty() || A.getLabel() == null) {
            return;
        }

        for (String s : labelsToQuantify) {
            if (!A.getLabel().contains(s)) {
                throw new RuntimeException(
                        "Variable " + s + " in the list of quantified variables is not a free variable.");
            }
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantifying:" + A.getQ() + " states", log);

        //If this is the case, then the quantified automaton is either the true or false automaton.
        //It is true if the language is not empty.
        if (labelsToQuantify.size() == A.getA().size()) {
            A.fa.setTRUE_AUTOMATON(!A.isEmpty());
            A.fa.setTRUE_FALSE_AUTOMATON(true);
            A.clear();
            return;
        }

        List<Integer> listOfInputsToQuantify = new ArrayList<>(labelsToQuantify.size());
        //extract the list of indices of inputs we would like to quantify
        for (String l : labelsToQuantify)
            listOfInputsToQuantify.add(A.getLabel().indexOf(l));
        List<List<Integer>> allInputs = new ArrayList<>(A.getAlphabetSize());
        for (int i = 0; i < A.getAlphabetSize(); i++)
            allInputs.add(A.richAlphabet.decode(i));
        //now we remove those indices in listOfInputsToQuantify from A,T,label, and allInputs
        UtilityMethods.removeIndices(A.richAlphabet.getA(), listOfInputsToQuantify);
        A.richAlphabet.setEncoder(null);
        A.determineAlphabetSize();
        UtilityMethods.removeIndices(A.getNS(), listOfInputsToQuantify);
        UtilityMethods.removeIndices(A.getLabel(), listOfInputsToQuantify);
        for (List<Integer> i : allInputs)
            UtilityMethods.removeIndices(i, listOfInputsToQuantify);
        //example: permutation[1] = 7 means that encoded old input 1 becomes encoded new input 7
        List<Integer> permutation = new ArrayList<>(allInputs.size());
        for (List<Integer> i : allInputs)
            permutation.add(A.richAlphabet.encode(i));

        int Q = A.getFa().getQ();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
        for (int q = 0; q < Q; q++) {
            Int2ObjectRBTreeMap<IntList> newMemDTransitionFunction = new Int2ObjectRBTreeMap<>();
            newD.add(newMemDTransitionFunction);
            for (Int2ObjectMap.Entry<IntList> transition : A.getFa().getEntriesNfaD(q)) {
                int mappedKey = permutation.get(transition.getIntKey());
                IntList existingTransitions = newMemDTransitionFunction.get(mappedKey);
                if (existingTransitions != null) {
                    addAllWithoutRepetition(existingTransitions, transition.getValue());
                } else {
                    newMemDTransitionFunction.put(mappedKey, new IntArrayList(transition.getValue()));
                }
            }
        }
        A.setD(newD);
        A.fa.determinizeAndMinimize(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantified:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * add elements of R that do not exist in L to L.
     * Also: keep order of previous elements of L and new elements (w.r.t. R).
     */
    private static <T> void addAllWithoutRepetition(List<T> L, List<T> R) {
        if (R == null || R.isEmpty()) return;
        R.stream().filter(x -> !L.contains(x)).forEach(L::add);
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
        UtilityMethods.logMessage(print, prefix + "Reversing:" + A.getQ() + " states", log);

        IntSet setOfFinalStates = A.fa.reverseToNFAInternal(IntSet.of(A.fa.getQ0()));
        A.fa.determinizeAndMinimize(setOfFinalStates, print, prefix + " ", log);

        if (reverseMsd) {
            NumberSystem.flipNS(A.getNS());
        }

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "reversed:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
     */
    public static void reverseWithOutput(Automaton A, boolean reverseMsd, boolean print, String prefix, StringBuilder log) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) {
            return;
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "reversing: " + A.getQ() + " states", log);

        boolean addedDeadState = A.fa.addDistinguishedDeadState(print, prefix, log);

        int minOutput = 0;
        if (addedDeadState) {
            // get state with smallest output. all states with this output will be removed.
            // after transducing, all states with this minimum output will be removed.
            minOutput = A.fa.determineMinOutput();
        }

        // need to define states, an initial state, transitions, and outputs.
        Map<Integer, Integer> newInitState = new HashMap<>();
        for (int i = 0; i < A.getQ(); i++) {
            newInitState.put(i, A.getO().getInt(i));
        }

        IntList newO = new IntArrayList();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

        List<Map<Integer, Integer>> newStates = new ArrayList<>();
        newStates.add(newInitState);

        Map<Map<Integer, Integer>, Integer> newStatesHash = new HashMap<>();
        newStatesHash.put(newInitState, newStates.size() - 1);

        Queue<Map<Integer, Integer>> newStatesQueue = new LinkedList<>();
        newStatesQueue.add(newInitState);

        while (!newStatesQueue.isEmpty()) {
            Map<Integer, Integer> currState = newStatesQueue.remove();

            // set up the output of this state to be g(q0), where g = currState.
            newO.add((int) currState.get(A.getQ0()));

            newD.add(new Int2ObjectRBTreeMap<>());

            if (A.getD().get(A.getQ0()).keySet().size() != A.getAlphabetSize()) {
                throw new RuntimeException("Automaton should be deterministic!");
            }
            for (int l : A.getD().get(A.getQ0()).keySet()) {
                Map<Integer, Integer> toState = new HashMap<>();

                for (int i = 0; i < A.getQ(); i++) {
                    toState.put(i, currState.get(A.getD().get(i).get(l).getInt(0)));
                }

                if (!newStatesHash.containsKey(toState)) {
                    newStates.add(toState);
                    newStatesQueue.add(toState);
                    newStatesHash.put(toState, newStates.size() - 1);
                }

                // set up the transition.
                IntList newList = new IntArrayList();
                newList.add((int) newStatesHash.get(toState));
                newD.get(newD.size() - 1).put(l, newList);
            }
        }

        A.fa.setFields(newStates.size(), newO, newD);

        if (reverseMsd) {
            NumberSystem.flipNS(A.getNS());
        }

        A.minimizeSelfWithOutput(print, prefix + " ", log);

        if (addedDeadState) {
            removeStatesWithMinOutput(A, minOutput);
        }

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "reversed: " + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    static void removeStatesWithMinOutput(Automaton N, int minOutput) {
        // remove all states that have an output of minOutput
        Set<Integer> statesToRemove = new HashSet<>();
        for (int q = 0; q < N.getQ(); q++) {
            if (N.getO().getInt(q) == minOutput) {
                statesToRemove.add(q);
            }
        }
        for (int q = 0; q < N.getQ(); q++) {
          N.getFa().getEntriesNfaD(q).removeIf(entry -> statesToRemove.contains(entry.getValue().getInt(0)));
        }
        N.fa.setCanonized(false);
        N.canonize();
    }

    /**
     * Convert the number system of a word A from [msd/lsd]_k^i to [msd/lsd]_k^j.
     */
    public static void convertNS(Automaton A, boolean toMsd, int toBase,
                                 boolean print, String prefix, StringBuilder log) {
        if (A.getNS().size() != 1) {
            throw new RuntimeException("Automaton must have exactly one input to be converted.");
        }

        NumberSystem ns = A.getNS().get(0);
        // 1) Parse the base from the A’s NS
        int fromBase = ns.parseBase();

        // If the old and new bases are the same, check if only MSD/LSD is changing
        if (fromBase == toBase) {
            if (ns.isMsd() == toMsd) {
                throw new RuntimeException("New and old number systems are identical: " + ns.getName());
            } else {
                // If only msd <-> lsd differs, just reverse the A
                reverseWithOutput(A, true, print, prefix + " ", log);
                return;
            }
        }

        // 2) Check if fromBase and toBase are powers of the same root
        int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);
        if (commonRoot == -1) {
            throw new RuntimeException("New and old number systems must have bases k^i and k^j for some integer k.");
        }

        // If originally LSD, we need to reverse to treat it as MSD for the conversions
        if (!ns.isMsd()) {
            reverseWithOutput(A, true, print, prefix + " ", log);
        }

        // We'll track if the A is reversed relative to original
        boolean currentlyReversed = false;

        // 3) Convert from k^i -> k if needed
        if (fromBase != commonRoot) {
            int exponent = (int) (Math.log(fromBase) / Math.log(commonRoot));
            reverseWithOutput(A, true, print, prefix + " ", log);
            currentlyReversed = true;

            convertLsdBaseToRoot(A, commonRoot, exponent, print, prefix + " ", log);
            A.minimizeSelfWithOutput(print, prefix + " ", log);
        }

        // 4) Convert from k -> k^j if needed
        if (toBase != commonRoot) {
            if (currentlyReversed) {
                // Undo reversal from the previous step
                reverseWithOutput(A, true, print, prefix + " ", log);
                currentlyReversed = false;
            }
            int exponent = (int) (Math.log(toBase) / Math.log(commonRoot));
            convertMsdBaseToExponent(A, exponent, print, prefix + " ", log);
            A.minimizeSelfWithOutput(print, prefix + " ", log);
        }

        // 5) If final desired base is LSD but we are still in MSD form, reverse again
        if (toMsd == currentlyReversed) {
            reverseWithOutput(A, true, print, prefix + " ", log);
        }
    }

    /**
     * Assuming this A is in number system msd_k with one input,
     * convert it to number system msd_{k^exponent} with one input.
     */
    private static void convertMsdBaseToExponent(Automaton A, int exponent,
                                                 boolean print, String prefix, StringBuilder log) {
        int base = A.getNS().get(0).parseBase();

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(
                print,
                prefix + "Converting: msd_" + base + " to msd_" + (int) Math.pow(base, exponent)
                        + ", " + A.getQ() + " states",
                log
        );

        if (!A.fa.isDeterministic()) {
            throw new RuntimeException("Automaton must be deterministic for msd_k^j conversion");
        }

        A.fa.updateTransitionsFromMorphism(exponent);

        // Update number system: msd_{base^exponent}
        int newBase = (int) Math.pow(base, exponent);
        A.getNS().set(0, new NumberSystem("msd_" + newBase));
        setAutomatonAlphabet(A, newBase);

        UtilityMethods.logMessage(print, prefix + "Converted: msd_" + base + " to msd_" + newBase
                + ", " + A.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms", log);
    }

    /**
     * Assuming this A is in number system lsd_{k^j} with one input,
     * convert it to number system lsd_k with one input.
     */
    private static void convertLsdBaseToRoot(Automaton A, int root, int exponent,
                                             boolean print, String prefix, StringBuilder log) {
        // Parse base and validate
        int base = A.getNS().get(0).parseBase();
        double expected = Math.pow(root, exponent);
        if (base != (int) expected) {
            throw new RuntimeException("Base mismatch: expected " + (int) expected + ", found " + base);
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(
                print,
                prefix + "Converting: lsd_" + base + " to lsd_" + (int) expected
                        + ", " + A.getQ() + " states",
                log
        );

        IntList oldO = A.getO();
        List<Int2ObjectRBTreeMap<IntList>> oldD = A.getD();

        // Prepare BFS structures
        List<IntObjectPair<IntList>> newStates = new ArrayList<>();
        Queue<IntObjectPair<IntList>> queue = new LinkedList<>();
        Map<IntObjectPair<IntList>, Integer> stateMap = new HashMap<>();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
        IntList newO = new IntArrayList();

        // Initialize BFS with the A's Q0
        IntObjectPair<IntList> init = new IntObjectImmutablePair<>(A.getQ0(), IntList.of());
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
        A.getNS().set(0, new NumberSystem("lsd_" + root));
        setAutomatonAlphabet(A, root);

        UtilityMethods.logMessage(
                print,
                prefix + prefix + "Converted: lsd_" + base + " to lsd_" + (int) expected
                        + ", " + A.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms",
                log);
    }

    /**
     * Updates the A's alphabet to [0..newBase-1] and sets alphabetSize accordingly.
     */
    private static void setAutomatonAlphabet(Automaton A, int newBase) {
        List<Integer> ints = new ArrayList<>(newBase);
        for (int i = 0; i < newBase; i++) {
            ints.add(i);
        }
        A.setA(List.of(ints));
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
        for (int q = 0; q < first.getQ(); q++) {
            if (first.getFa().isAccepting(q)) {
                first.getO().set(q, outputs.getInt(0));
            }
        }
        first.combineIndex = 1;
        first.combineOutputs = outputs;
        while (!subautomata.isEmpty()) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computing =>:" + first.getQ() + " states - " + next.getQ() + " states", log);

            // crossProduct requires labelling; make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.setLabel(first.getLabel());
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            first.fa.totalize(print, prefix + " ", log);
            next.fa.totalize(print, prefix + " ", log);
            Automaton product = ProductStrategies.crossProduct(first, next, "combine", print, prefix + " ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computed =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }

        // totalize the resulting A
        first.fa.totalize(print, prefix + " ", log);
        first.canonizeAndApplyAllRepresentationsWithOutput(print, prefix + " ", log);

        return first;
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method returns
     * a DFA that accepts x iff this[x] < W[x] lexicographically.
     * To be used only when this A and M are DFAOs (words).
     *
     * @param A
     * @param B
     * @param operator
     * @return
     */
    public static Automaton compare(
            Automaton A, Automaton B, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print,
            prefix + "comparing (" + operator + "):" + A.getFa().getQ() + " states - " + B.getFa().getQ() + " states", log);
        Automaton M = ProductStrategies.crossProductAndMinimize(A, B, operator, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print,
            prefix + "compared (" + operator + "):" + M.getFa().getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method changes the word A
     * to a DFA that accepts x iff this[x] < o lexicographically.
     * To be used only when this A is a DFAO (word).
     */
    public static void compareWordAutomaton(
        FA fa, int o, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "comparing (" + operator + ") against " + o + ":" + fa.getQ() + " states", log);
        for (int p = 0; p < fa.getQ(); p++) {
            fa.getO().set(p, RelationalOperator.compare(operator, fa.getO().getInt(p), o) ? 1 : 0);
        }
        fa.determinizeAndMinimize(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "compared (" + operator + ") against " + o + ":" + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }
}
