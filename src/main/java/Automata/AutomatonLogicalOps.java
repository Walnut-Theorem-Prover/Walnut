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
import Main.Logging;
import Main.Prover;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.*;

import java.util.*;

import static Main.Logging.*;
import static Main.UtilityMethods.NO_COMMON_ROOT;

// Most logical operations produce DFAs and return AutomatonDFA where the result is guaranteed deterministic.
public class AutomatonLogicalOps {

    /**
     * @return A and B.
     */
    public static AutomatonDFA and(Automaton A, Automaton B) {
        return and(A, B, LogicalOperator.AND);
    }
    public static AutomatonDFA and(Automaton A, Automaton B, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? B.asDFA() : new AutomatonDFA(false);
            }
            return and(B, A); // and is symmetric
        }

        long timeBefore = System.currentTimeMillis();
        logMessage(COMPUTING + " " + friendlyOp + ":" + A.fa.getQ() + " states - " + B.fa.getQ() + " states");
        Logging.indent();
        AutomatonDFA N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp).asDFA();

        Logging.dedent();
        long timeAfter = System.currentTimeMillis();
        logMessage(COMPUTED + " " + friendlyOp + ":" + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");

        return N;
    }

    /**
     * @return this A or M
     */
    public static AutomatonDFA or(Automaton A, Automaton B, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? new AutomatonDFA(true): B.asDFA();
            }
            return or(B, A, friendlyOp); // or is symmetric
        }
        return totalizeCrossProduct(A, B, friendlyOp);
    }

    /**
     * @return A xor B
     */
    public static AutomatonDFA xor(Automaton A, Automaton B, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                if (A.fa.isTRUE_AUTOMATON()) {
                    not(B);
                }
                return B.asDFA();
            }
            return xor(B, A, friendlyOp); // xor is symmetric
        }
      return totalizeCrossProduct(A, B, friendlyOp);
    }

    /**
     * @return A imply B
     */
    public static AutomatonDFA imply(Automaton A, Automaton B, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            // not a or b
            if (A.fa.isTRUE_FALSE_AUTOMATON()) {
                return A.fa.isTRUE_AUTOMATON() ? B.asDFA() : new AutomatonDFA(true);
            }
            if (B.fa.isTRUE_AUTOMATON()) {
                return new AutomatonDFA(true);
            } else {
                not(A);
                return A.asDFA();
            }
        }
      return totalizeCrossProduct(A, B, friendlyOp);
    }

    private static AutomatonDFA totalizeCrossProduct(Automaton A, Automaton B, String friendlyOp) {
        long timeBefore = System.currentTimeMillis();
        logMessage(COMPUTING + " " + friendlyOp + ":" + A.fa.getQ() + " states - " + B.fa.getQ() + " states");

        Logging.indent();
        A.fa.totalize();
        B.fa.totalize();
        AutomatonDFA N = ProductStrategies.crossProductAndMinimize(A, B, friendlyOp).asDFA();
        Logging.dedent();
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        logMessage(COMPUTED + " " + friendlyOp + ":" + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        return N;
    }

    /**
     * @return A iff B
     */
    public static AutomatonDFA iff(Automaton A, Automaton B, String friendlyOp) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            Automaton C = imply(A, B, LogicalOperator.IMPLY);
            Automaton D = imply(B, A, LogicalOperator.IMPLY);
            return and(C, D);
        }

      return totalizeCrossProduct(A, B, friendlyOp);
    }

    /**
     * Negate automaton. NOTE: A is deterministic.
     */
    public static void not(Automaton A) {
        boolean print = Logging.shouldPrintDetails();
        if (A.fa.isTRUE_FALSE_AUTOMATON()) {
            A.fa.setTRUE_AUTOMATON(!A.fa.isTRUE_AUTOMATON());
            return;
        }

        // Automaton A *must* be deterministic, based on algorithm used. Assert this.
        if (!A.getFa().getT().isDeterministic()) {
            throw WalnutException.nonDeterministic();
        }

        // TODO: convert to DFA before calling internal NOT (or pass a DFA into this method)
        //   However, totalize needs to support this
        // A.getFa().convertNFAtoDFA();

        long timeBefore = System.currentTimeMillis();
        logMessage(print, COMPUTING + " " + Operator.NEGATE + ":" + A.fa.getQ() + " states");

        Logging.indent();
        A.getFa().totalize();
        A.getFa().flipOutput();

        // TODO: Since we're already in a DFA, we don't need to determinize
        A.determinizeAndMinimize();
        A.applyAllRepresentations();

        Logging.dedent();
        long timeAfter = System.currentTimeMillis();
        logMessage(print, COMPUTED + " " + Operator.NEGATE + ":" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
    }

    /**
     * If this automaton's language is L_1 and the language of "B" is L_2, the result accepts the language
     * L_1 / L_2 = { x : exists y in L_2 such that xy in L_1 }
     */
    public static Automaton rightQuotient(Automaton A, Automaton B, boolean skipSubsetCheck) {
        long timeBefore = System.currentTimeMillis();
        logMessage("right quotient: " + A.fa.getQ() + " state A with " + B.fa.getQ() + " state A");

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
            for (Int2ObjectMap.Entry<IntList> entry : otherClone.fa.getT().getEntriesNfaD(q)) {
                newMap.put(A.richAlphabet.encode(otherClone.richAlphabet.decode(entry.getIntKey())),
                    new IntArrayList(entry.getValue()));
            }
            newOtherD.add(newMap);
        }
        otherClone.fa.setNfaTransitions(newOtherD);
        otherClone.richAlphabet.setEncoder(A.richAlphabet.getEncoder());
        otherClone.richAlphabet.setA(A.richAlphabet.getA());
        otherClone.setAlphabetSize(A.getAlphabetSize());
        otherClone.setNS(A.getNS());

        for (int i = 0; i < A.fa.getQ(); i++) {
            // this will be a temporary A that will be the same as self except it will start from the A
            Automaton T = A.clone();

            if (i != 0) {
                T.fa.setQ0(i);
                T.forceCanonize();
            }

            // need to have the same label for cross product (including "and")
            T.randomLabel();
            otherClone.setLabel(T.getLabel());

            Automaton I = and(T, otherClone);

            M.fa.setOutputIfEqual(i, !I.isEmpty());
        }

        M.determinizeAndMinimize();
        M.applyAllRepresentations();
        M.forceCanonize();

        long timeAfter = System.currentTimeMillis();
        logMessage("right quotient complete: " + M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");

        return M;
    }

    public static Automaton leftQuotient(Automaton A, Automaton B) {
        long timeBefore = System.currentTimeMillis();
        logMessage("left quotient: " + A.fa.getQ() + " state A with " + B.fa.getQ() + " state A");

        // check whether the alphabet of self is a subset of the alphabet of B. If not, throw an error.
        if (!RichAlphabet.isSubsetA(A.richAlphabet, B.richAlphabet)) {
            throw new WalnutException("First A's alphabet must be a subset of the second A's alphabet for left quotient.");
        }

        Automaton M1 = reverseAndCanonize(A);
        Automaton M2 = reverseAndCanonize(B);
        Automaton M = rightQuotient(M1, M2, true);

        reverse(M, true);

        long timeAfter = System.currentTimeMillis();
        logMessage("left quotient complete: " + M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");

        return M;
    }

    private static Automaton reverseAndCanonize(Automaton A) {
        Automaton M1 = A.clone();
        reverse(M1, true);
        M1.forceCanonize();
        return M1;
    }

    /**
     * Make A accept 0*x, iff it used to accept x.
     */
    public static void fixLeadingZerosProblem(Automaton A) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;
        long timeBefore = System.currentTimeMillis();
        logMessage(FIXING + " leading zeros:" + A.fa.getQ() + " states");
        Logging.indent();
        A.fa.setCanonized(false);
        int zero = A.richAlphabet.determineZero();

        // Subset Construction with different initial state
        IntSet initialState = zeroReachableStates(A.fa, zero);
        A.determinizeAndMinimize(initialState);

        long timeAfter = System.currentTimeMillis();
        Logging.dedent();
        logMessage(FIXED + " leading zeros:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
    }

    /**
     * Returns the set of states reachable from the initial state by reading 0*
     * This can alter FA itself
     */
    private static IntSet zeroReachableStates(FA fa, int zero) {
        fa.ensureNfaTransitions();
        // Ensure q0 is initialized in nfaD
        IntList dQ0 = fa.getT().getNfaState(fa.getQ0()).computeIfAbsent(zero, k -> new IntArrayList());
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
                IntList transitions = fa.getT().getNfaStateDests(q, zero);
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

    /**
     * Make automaton accept x0*, iff it used to accept x.
     */
    public static void fixTrailingZerosProblem(Automaton A) {
        if (A.fa.setStatesReachableToFinalStatesByZeros(A.richAlphabet.determineZero())) {
            long timeBefore = System.currentTimeMillis();
            logMessage(FIXING + " trailing zeros:" + A.fa.getQ() + " states");
            Logging.indent();
            A.fa.setCanonized(false);
            // We don't have to determinize, since all that was altered was final states
            A.fa.justMinimize();
            Logging.dedent();
            long timeAfter = System.currentTimeMillis();
            logMessage(FIXED + " trailing zeros:" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        } else {
            logMessage(FIXING + " trailing zeros: no change necessary.");
        }
    }

    /**
     * Used for the "I" quantifier. If some input is in msd, then we remove leading zeros,
     * if some input is in lsd, then we remove trailing zeros, otherwise, we do nothing.
     * To do this, for each input, we construct an automaton which accepts if the leading/trailing input is non-zero,
     * union all these automata together, and intersect with our original automaton.
     */
    public static Automaton removeLeadingZeros(Automaton A, List<String> listOfLabels) {
        AutomatonQuantification.validateLabels(A, listOfLabels);
        if (listOfLabels.isEmpty()) {
            return A.clone();
        }
        long timeBefore = System.currentTimeMillis();
        logMessage(REMOVING + " leading zeros for:" + A.fa.getQ() + " states");
        Logging.indent();
        List<Integer> listOfInputs = new ArrayList<>(listOfLabels.size());
        //extract the list of indices of inputs from the list of labels
        for (String l : listOfLabels) {
            listOfInputs.add(A.getLabel().indexOf(l));
        }
        Automaton M = new Automaton(false);
        for (int n : listOfInputs) {
            Automaton N = removeLeadingZerosHelper(A, n);
            M = or(M, N, LogicalOperator.OR);
        }
        M = and(A, M);

        Logging.dedent();
        long timeAfter = System.currentTimeMillis();
        logMessage(REMOVED + ":" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        return M;
    }

    /**
     * Returns the automaton with the same alphabet as the current A, which requires the nth input to start with a
     * non-zero symbol (if msd), end with a non-zero symbol (if lsd), otherwise, return the true automaton.
     * The returned automaton is meant to be intersected with the current A to remove leading/trailing * zeros
     * from the nth input.
     */
    private static Automaton removeLeadingZerosHelper(Automaton A, int n) {
        if (n >= A.richAlphabet.getA().size() || n < 0) {
            throw new WalnutException("Cannot remove leading zeros for the "
                    + (n + 1) + "-th input when A only has " + A.richAlphabet.getA().size() + " inputs.");
        }

        if (A.getNS().get(n) == null) {
            return new AutomatonDFA(true);
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
                M.fa.getT().setNfaDTransition(0, i, new IntArrayList(dest));
            }
            M.fa.getT().setNfaDTransition(1, i, new IntArrayList(dest));
        }
        if (!A.getNS().get(n).isMsd()) {
            reverse(M, false);
        }
        return M;
    }

    /**
     * The input automaton should not be a word Automaton (i.e., with output). However, it can be NFA.
     * Enabling the reverseMsd flag will flip the number system of the A from msd to lsd, and vice versa.
     * Reversing the Msd will also call this function as reversals are done in the NumberSystem class upon
     * initializing.
     * NOTE: the output of this is a DFA.
     */
    public static void reverse(Automaton A, boolean reverseMsd) {
        if (A.fa.isTRUE_FALSE_AUTOMATON()) return;

        long timeBefore = System.currentTimeMillis();
        logMessage(REVERSING + ":" + A.fa.getQ() + " states");
        Logging.indent();
        IntSet setOfFinalStates = A.fa.reverseToNFAInternal(IntSet.of(A.fa.getQ0()));
        A.determinizeAndMinimize(setOfFinalStates);

        if (reverseMsd) {
            NumberSystem.flipNS(A.getNS());
        }

        Logging.dedent();
        long timeAfter = System.currentTimeMillis();
        logMessage(REVERSED + ":" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
    }

    /**
     * Deletes all states whose output equals the given value, remaps remaining states, and preserves only transitions among kept states.
     * @param fa - deterministic FA
     */
    static void removeStatesWithOutputRebuild(FA fa, int minOutput) {
        fa.ensureNfaTransitions();
        Set<Integer> statesToRemove = new HashSet<>();
        for (int q = 0; q < fa.getQ(); q++) {
            if (fa.getO().getInt(q) == minOutput) {
                statesToRemove.add(q);
            }
        }
        for (int q = 0; q < fa.getQ(); q++) {
            fa.getT().getEntriesNfaD(q).removeIf(entry -> !entry.getValue().isEmpty()
                && statesToRemove.contains(entry.getValue().getInt(0)));
        }
    }

    /**
     * Convert the number system of an automaton from [msd/lsd]_k^i to [msd/lsd]_k^j.
     * TODO: this assumes that A is a word automaton when it may not be.
     * It probably doesn't matter, but it would be good to separate the two.
     */
    public static void convertNS(Automaton A, boolean toMsd, int toBase) {
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
                // The conversion routines assume a complete transition function; totalize before reversal.
                if (!A.fa.isDeterministicAndTotal()) {
                    A.fa.totalize();
                }
                // If only msd <-> lsd differs, just reverse A
                Logging.indent();
                WordAutomaton.reverseWithOutput(A, true);
                Logging.dedent();
                return;
            }
        }

        // 2) Check if fromBase and toBase are powers of the same root
        int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);
        if (commonRoot == NO_COMMON_ROOT) {
            throw new WalnutException("New and old number systems must have bases k^i and k^j for some integer k.");
        }

        // Base conversion groups or ungroups digits and assumes every grouped digit has a transition, so totalize.
        if (!A.fa.isDeterministicAndTotal()) {
            A.fa.totalize();
        }

        Logging.indent();

        // If originally LSD, we need to reverse to treat it as MSD for the conversions
        if (!ns.isMsd()) {
            WordAutomaton.reverseWithOutput(A, true);
        }

        // We'll track if A is reversed relative to original
        boolean currentlyReversed = false;

        // 3) Convert from k^i -> k if needed
        if (fromBase != commonRoot) {
            int exponent = (int) (Math.log(fromBase) / Math.log(commonRoot));
            WordAutomaton.reverseWithOutput(A, true);
            currentlyReversed = true;

            convertLsdBaseToRoot(A, commonRoot, exponent);
            WordAutomaton.minimizeSelfWithOutput(A);
        }

        // 4) Convert from k -> k^j if needed
        if (toBase != commonRoot) {
            if (currentlyReversed) {
                // Undo reversal from the previous step
                WordAutomaton.reverseWithOutput(A, true);
                currentlyReversed = false;
            }
            int exponent = (int) (Math.log(toBase) / Math.log(commonRoot));
            convertMsdBaseToExponent(A, exponent);
            WordAutomaton.minimizeSelfWithOutput(A);
        }

        // 5) If final desired base is LSD but we are still in MSD form, reverse again
        if (toMsd == currentlyReversed) {
            WordAutomaton.reverseWithOutput(A, true);
        }
        Logging.dedent();
    }

    /**
     * Assuming this automaton is in number system msd_k with one input,
     * convert it to number system msd_{k^exponent} with one input.
     */
    private static void convertMsdBaseToExponent(Automaton A, int exponent) {
        if (!A.fa.isDeterministicAndTotal()) {
            throw new WalnutException("Automaton must be deterministic for msd_k^j conversion");
        }

        int base = A.getNS().get(0).parseBase();

        long timeBefore = System.currentTimeMillis();
        int newBase = (int) Math.pow(base, exponent);
        String msdUnderscore = NumberSystem.MSD_UNDERSCORE;

        logMessage(CONVERTING + ": " + msdUnderscore + base + " to " +
            msdUnderscore + newBase +
            ", " + A.fa.getQ() + " states"
        );

        updateTransitionsFromMorphism(A.fa, exponent);

        // Update number system: msd_{base^exponent}
        A.getNS().set(0, new NumberSystem(msdUnderscore + newBase));
        setAutomatonAlphabet(A, newBase);

        logMessage(CONVERTED + ": " + msdUnderscore + base + " to " +
            msdUnderscore + newBase +
            ", " + A.fa.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms");
    }

    /**
     * Assuming this automaton is in number system lsd_{k^j} with one input,
     * convert it to number system lsd_k with one input.
     */
    private static void convertLsdBaseToRoot(Automaton A, int root, int exponent) {
        // Parse base and validate
        int base = A.getNS().get(0).parseBase();
        int expected = (int)Math.pow(root, exponent);
        if (base != expected) {
            throw new WalnutException("Base mismatch: expected " + expected + ", found " + base);
        }
        final String lsdUnderscore = NumberSystem.LSD_UNDERSCORE;

        long timeBefore = System.currentTimeMillis();
        logMessage(CONVERTING + ": " + lsdUnderscore + base + " to " +
            lsdUnderscore + expected +
            ", " + A.fa.getQ() + " states"
        );

        IntList oldO = A.fa.getO();
        List<Int2ObjectRBTreeMap<IntList>> oldD = A.fa.getT().getNfaD();

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

        logMessage(CONVERTED + ": " + lsdUnderscore + base +
            " to " + lsdUnderscore + expected +
            ", " + A.fa.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms"
        );
    }

    /**
     * Updates the automaton's alphabet to [0..newBase-1] and sets alphabetSize accordingly.
     */
    private static void setAutomatonAlphabet(Automaton A, int newBase) {
        A.richAlphabet.setA(List.of(UtilityMethods.intRangeList(newBase)));
        A.setAlphabetSize(newBase);
    }

    /**
     * Compute the numeric value of a 'digit' list in the given root^position sense.
     * (Used in convertLsdBaseToRoot BFS)
     */
    private static int computeStringValue(List<Integer> digits, int root) {
        int value = 0;
        for (int i = 0; i < digits.size(); i++) {
            value = Math.addExact(value, Math.multiplyExact(digits.get(i), (int)Math.pow(root, i)));
        }
        return value;
    }

    public static Automaton combine(Automaton A, Queue<Automaton> subautomata, IntList outputs) {
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
            logMessage(COMPUTING + " =>:" + first.fa.getQ() + " states - " + next.fa.getQ() + " states");
            Logging.indent();

            // crossProduct requires labeling; make an arbitrary labeling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.setLabel(first.getLabel());
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            first.fa.totalize();
            next.fa.totalize();
            Automaton product = ProductStrategies.crossProduct(first, next, Prover.COMBINE);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            Logging.dedent();
            long timeAfter = System.currentTimeMillis();
            logMessage(COMPUTED + " =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        }

        // totalize the resulting A
        Logging.indent();
        first.fa.totalize();
        first.forceCanonize();
        first.applyAllRepresentationsWithOutput();
        Logging.dedent();

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
              for (int di : fa.getT().getNfaStateKeySet(j)) {
                int nextState = fa.getT().getNfaStateDests(prevMorphism.get(j).get(k), di).getInt(0);
                extendedRow.add(nextState);
              }
            }
            newMorphism.add(extendedRow);
          }
          prevMorphism = newMorphism;
        }
        // Create new transitions from the final morphism
        fa.setNfaTransitions(buildTransitionsFromMorphism(fa, prevMorphism));
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
          row.add(fa.getT().getNfaStateDests(q, di).getInt(0));
        }
        result.add(row);
      }
      return result;
    }
}
