package Automata.FA;

import Automata.Automaton;
import Automata.NumberSystem;
import Automata.RichAlphabet;
import Main.EvalComputations.Token.ArithmeticOperator;
import Main.EvalComputations.Token.LogicalOperator;
import Main.EvalComputations.Token.RelationalOperator;
import Main.Logging;
import Main.WalnutException;
import Main.Prover;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;

/**
 * Product strategy logic.
 * Automata are numbered, which is useful for meta-commands like [export]
 */
public class ProductStrategies {

    /**
     * Cross-product of two DFAs. Output is an NFA (for now).
     */
    public static void crossProductInternal(
            FA A, FA B, FA AxB, int combineOut, int[] allInputsOfAxB, String op,
            boolean print, String prefix, StringBuilder log, long timeBefore) {
        List<IntIntPair> statesList = new ArrayList<>();
        Object2IntMap<IntIntPair> statesHash = new Object2IntOpenHashMap<>();
        statesHash.defaultReturnValue(-1);
        AxB.setQ0(0);
        statesList.add(new IntIntImmutablePair(A.getQ0(), B.getQ0()));
        statesHash.put(new IntIntImmutablePair(A.getQ0(), B.getQ0()), 0);
        int currentState = 0;
        while (currentState < statesList.size()) {
            if (print) {
                int statesSoFar = currentState + 1;
                long timeAfter = System.currentTimeMillis();
                Logging.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar == 1e4 || statesSoFar % 1e5 == 0,
                        prefix + "  Progress: Added " + statesSoFar + " states - "
                    + (statesList.size() - statesSoFar) + " states left in queue - "
                    + statesList.size() + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
            }

            IntIntPair s = statesList.get(currentState);

            // s must be an array of length 2, where the first element is a state in this, and the second element is a
            // state in the other Automaton.
            int p = s.leftInt();
            int q = s.rightInt();
            Int2ObjectRBTreeMap<IntList> stateTransitions = new Int2ObjectRBTreeMap<>();
            AxB.getT().addToNfaD(stateTransitions);
            AxB.getO().add(determineOutput(A.getO().getInt(p), B.getO().getInt(q), op, combineOut));

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getT().getEntriesNfaD(q);
            for (Int2ObjectMap.Entry<IntList> entryA : A.getT().getEntriesNfaD(p)) {
                final int AxBalphabet = entryA.getIntKey() * B.getAlphabetSize();
                for (Int2ObjectMap.Entry<IntList> entryB : Bset) {
                    int z = allInputsOfAxB[AxBalphabet + entryB.getIntKey()];
                    if (z == -1) {
                        continue;
                    }
                    IntArrayList dest = new IntArrayList(entryA.getValue().size() * entryB.getValue().size());
                    stateTransitions.put(z, dest);
                    for (int destA : entryA.getValue()) {
                        for (int destB : entryB.getValue()) {
                            // Note: since A and B are DFAs, there's only one value ever here.
                            IntIntPair dest3 = new IntIntImmutablePair(destA, destB);
                            int statesHashVal = statesHash.getInt(dest3);
                            if (statesHashVal == -1) {
                                statesHashVal = statesList.size();
                                statesHash.put(dest3, statesHashVal);
                                statesList.add(dest3);
                            }
                            dest.add(statesHash.getInt(dest3));
                        }
                    }
                    dest.trim(); // save peak memory
                }
            }
            currentState++;
        }
        AxB.setQ(statesList.size());
        long timeAfter = System.currentTimeMillis();
        Logging.logMessage(print,
                prefix + COMPUTED + " cross product:" + AxB.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Cross-product of two DFAs. Output is a DFA.
     */
    public static void crossProductInternalDFA(
            FA A, FA B, FA AxB, int combineOut, int[] allInputsOfAxB, String op,
            boolean print, String prefix, StringBuilder log, long timeBefore) {
        List<IntIntPair> statesList = new ArrayList<>();
        Object2IntMap<IntIntPair> statesHash = new Object2IntOpenHashMap<>();
        statesHash.defaultReturnValue(-1);
        AxB.setQ0(0);
        AxB.getT().setNfaD(null);
        AxB.getT().setDfaD(new ArrayList<>());
        statesList.add(new IntIntImmutablePair(A.getQ0(), B.getQ0()));
        statesHash.put(new IntIntImmutablePair(A.getQ0(), B.getQ0()), 0);
        int currentState = 0;
        while (currentState < statesList.size()) {
            if (print) {
                int statesSoFar = currentState + 1;
                long timeAfter = System.currentTimeMillis();
                Logging.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
                        prefix + "  Progress: Added " + statesSoFar + " states - "
                                + (statesList.size() - statesSoFar) + " states left in queue - "
                                + statesList.size() + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
            }

            IntIntPair s = statesList.get(currentState);

            // s must be an array of length 2, where the first element is a state in this, and the second element is a
            // state in the other Automaton.
            int p = s.leftInt();
            int q = s.rightInt();
            Int2IntMap stateTransitions = new Int2IntOpenHashMap();
            AxB.getT().getDfaD().add(stateTransitions);
            AxB.getO().add(determineOutput(A.getO().getInt(p), B.getO().getInt(q), op, combineOut));

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getT().getEntriesNfaD(q);
            for (Int2ObjectMap.Entry<IntList> entryA : A.getT().getEntriesNfaD(p)) {
                final int AxBalphabet = entryA.getIntKey() * B.getAlphabetSize();
                for (Int2ObjectMap.Entry<IntList> entryB : Bset) {
                    int z = allInputsOfAxB[AxBalphabet + entryB.getIntKey()];
                    if (z == -1) {
                        continue;
                    }
                    for (int destA : entryA.getValue()) {
                        for (int destB : entryB.getValue()) {
                            // Note: since A and B are DFAs, there's only one value ever here.
                            IntIntPair dest3 = new IntIntImmutablePair(destA, destB);
                            int statesHashVal = statesHash.getInt(dest3);
                            if (statesHashVal == -1) {
                                statesHashVal = statesList.size();
                                statesHash.put(dest3, statesHashVal);
                                statesList.add(dest3);
                            }
                            stateTransitions.put(z, statesHashVal);
                        }
                    }
                }
            }
            currentState++;
        }
        AxB.setQ(statesList.size());
        statesList.clear(); // save memory
        AxB.getT().reduceDfaDMemory();

        long timeAfter = System.currentTimeMillis();
        Logging.logMessage(print,
                prefix + COMPUTED + " cross product:" + AxB.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    private static int determineOutput(int aP, int mQ, String op, int combineOut) {
        if (RelationalOperator.RELATIONAL_OPERATORS.containsKey(op)) {
            return RelationalOperator.compare(RelationalOperator.Ops.fromSymbol(op), aP, mQ) ? 1 : 0;
        }
        if (ArithmeticOperator.ARITHMETIC_OPERATORS.containsKey(op)) {
            return ArithmeticOperator.arith(ArithmeticOperator.Ops.fromSymbol(op), aP, mQ);
        }
        return switch (op) {
            case LogicalOperator.AND -> (aP != 0 && mQ != 0) ? 1 : 0;
            case LogicalOperator.OR -> (aP != 0 || mQ != 0) ? 1 : 0;
            case LogicalOperator.XOR -> ((aP != 0 && mQ == 0) || (aP == 0 && mQ != 0)) ? 1 : 0;
            case LogicalOperator.IMPLY -> (aP == 0 || mQ != 0) ? 1 : 0;
            case LogicalOperator.IFF -> ((aP == 0 && mQ == 0) || (aP != 0 && mQ != 0)) ? 1 : 0;
            case Prover.COMBINE -> (mQ == 1) ? combineOut : aP;
            case Prover.FIRST_OP -> aP == 0 ? mQ : aP;
            case Prover.IF_OTHER_OP -> mQ != 0 ? aP : 0;
            default -> throw WalnutException.unexpectedOperator(op);
        };
    }

    /**
     * This method is used in and, or, not, and many others.
     * A and B should have TRUE_FALSE_AUTOMATON = false.
     * A and B must have labeled inputs.
     * For the sake of an example, suppose that Q = 3, q0 = 1, B.Q = 2, and B.q0 = 0. Then N.Q = 6 and the states of N
     * are {0=(0,0),1=(0,1),2=(1,0),3=(1,1),4=(2,0),5=(2,1)} and N.q0 = 2. The transitions of state (a,b) is then
     * based on the transitions of a and b in this and B.
     * To continue with this example suppose that label = ["i","j"] and
     * B.label = ["p","q","j"]. Then N.label = ["i","j","p","q"], and inputs to N are four tuples.
     * Now suppose in this we go from 0 to 1 by reading (i=1,j=2)
     * and in B we go from 1 to 0 by reading (p=-1,q=-2,j=2).
     * Then in N we go from (0,1) to (1,0) by reading (i=1,j=2,p=-1,q=-2).
     *
     * @return A cross product B.
     */
    public static Automaton crossProduct(Automaton A,
                                         Automaton B,
                                         String op,
                                         boolean print,
                                         String prefix,
                                         StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        Automaton AxB = new Automaton();
        int[] allInputsOfN = createBasicAutomaton(A, B, AxB);
        int combineOut = A.determineCombineOutVal(op);
        printAndUpdateIndex(A.fa.getQ(), B.fa.getQ(), print, prefix, log);
        crossProductInternal(
            A.fa, B.fa, AxB.fa, combineOut, allInputsOfN, op, print, prefix, log, timeBefore);
        return AxB;
    }

    public static Automaton crossProductAndMinimize(Automaton A,
                                                    Automaton B,
                                                    String op,
                                                    boolean print,
                                                    String prefix,
                                                    StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        Automaton AxB = new Automaton();
        int[] allInputsOfN = createBasicAutomaton(A, B, AxB);
        int combineOut = A.determineCombineOutVal(op);
        printAndUpdateIndex(A.fa.getQ(), B.fa.getQ(), print, prefix, log);
        crossProductInternalDFA(
                A.fa, B.fa, AxB.fa, combineOut, allInputsOfN, op, print, prefix, log, timeBefore);
        AxB.fa.justMinimize(print, prefix, log);
        if (AxB.fa.getT().getNfaD() == null) {
            throw new WalnutException("Unexpected null");
        }
        return AxB;
    }

    private static void printAndUpdateIndex(int aQ, int bQ, boolean print, String prefix, StringBuilder log) {
        if (print) {
            //FA.IncrementIndex();
            Logging.logMessage(print,
                prefix + COMPUTING + " cross product:" + aQ + " states - " + bQ + " states", log);
        }
    }

    private static int[] createBasicAutomaton(
            Automaton A, Automaton B, Automaton AxB) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            throw new WalnutException("Invalid use of the crossProduct method: " +
                    "the automata for this method cannot be true or false automata.");
        }

        List<String> aLabel = A.getLabel(), bLabel = B.getLabel();
        List<List<Integer>> aA = A.richAlphabet.getA(), bA = B.richAlphabet.getA();

        if (aLabel== null || bLabel == null ||
            aLabel.size() != aA.size() || bLabel.size() != bA.size()) {
            throw new WalnutException("Invalid use of the crossProduct method: " +
                    "the automata for this method must have labeled inputs.");
        }

        int[] sameInputsInAAndB = computeSameInputs(aLabel, aA, bLabel, bA);
        updateAxBFields(aLabel, aA, A.getNS(), bLabel, bA, B.getNS(), AxB, sameInputsInAAndB);

        return computeAllInputsOfAxB(A.getAlphabetSize(), A.richAlphabet, B.getAlphabetSize(), B.richAlphabet,
            AxB.richAlphabet, sameInputsInAAndB);
    }

    /*
     * for example when sameLabelsInAAndB[2] = 3, then input 2 of A has the same label as input 3 of B
     * and when sameLabelsInAAndB[2] = -1, it means that input 2 of A is not an input of B
     */
    private static int[] computeSameInputs(
        List<String> aLabel, List<List<Integer>> aA, List<String> bLabel, List<List<Integer>> bA) {
        int[] sameInputsInMAndThis = new int[bLabel.size()];
        for (int i = 0; i < bLabel.size(); i++) {
            sameInputsInMAndThis[i] = -1;
            int j = aLabel.indexOf(bLabel.get(i));
            if (j >= 0) {
                if (!UtilityMethods.areEqual(aA.get(j), bA.get(i))) {
                    throw new WalnutException("in computing cross product of two automaton, "
                            + "variables with the same label must have the same alphabet");
                }
                sameInputsInMAndThis[i] = j;
            }
        }
        return sameInputsInMAndThis;
    }

    private static void updateAxBFields(
        List<String> aLabel, List<List<Integer>> aA, List<NumberSystem> aNS,
        List<String> bLabel, List<List<Integer>> bA, List<NumberSystem> bNS,
        Automaton AxB, int[] sameInputsInMAndThis) {
        for (int i = 0; i < aLabel.size(); i++) {
            AxB.richAlphabet.getA().add(aA.get(i));
            AxB.getLabel().add(aLabel.get(i));
            AxB.getNS().add(aNS.get(i));
        }
        for (int i = 0; i < bLabel.size(); i++) {
            if (sameInputsInMAndThis[i] == -1) {
                AxB.richAlphabet.getA().add(new ArrayList<>(bA.get(i)));
                AxB.getLabel().add(bLabel.get(i));
                AxB.getNS().add(bNS.get(i));
            } else {
                int j = sameInputsInMAndThis[i];
                if (bNS.get(i) != null && AxB.getNS().get(j) == null) {
                    AxB.getNS().set(j, bNS.get(i));
                }
            }
        }
        AxB.determineAlphabetSize();
    }

    /**
     * Compute all inputs of AxB.
     */
    private static int[] computeAllInputsOfAxB(
        int aAlphSize, RichAlphabet aRichAlphabet, int bAlphSize, RichAlphabet bRichAlphabet,
        RichAlphabet AxBRichAlphabet, int[] sameInputsInMAndThis) {
        int[] allInputsOfN = new int[aAlphSize * bAlphSize];
        int idx = 0;
        for (int i = 0; i < aAlphSize; i++) {
            List<Integer> aDecodeI = aRichAlphabet.decode(i);
            for (int j = 0; j < bAlphSize; j++) {
                List<Integer> inputForN = joinTwoInputsForCrossProduct(
                    aDecodeI, bRichAlphabet.decode(j), sameInputsInMAndThis);
                allInputsOfN[idx++] = inputForN == null ? -1 : AxBRichAlphabet.encode(inputForN);
            }
        }
        return allInputsOfN;
    }

    /**
     * Join inputs for cross product.
     * Add all of first, then nonequal ones of second...
     * unless there's a nonequal element that shouldn't be there.
     * See unit tests for examples.
     */
    static List<Integer> joinTwoInputsForCrossProduct(
        List<Integer> first, List<Integer> second, int[] equalIndices) {
        List<Integer> R = new ArrayList<>(first);
        for (int i = 0; i < second.size(); i++) {
            int equalIndexI = equalIndices[i];
            int secondI = second.get(i);
            if (equalIndexI == -1)
                R.add(secondI);
            else {
                if (!first.get(equalIndexI).equals(secondI))
                    return null;
            }
        }
        return R;
    }
}
