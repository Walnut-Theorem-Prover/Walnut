package Automata.FA;

import Automata.Automaton;
import Automata.NumberSystem;
import Main.EvalComputations.Token.ArithmeticOperator;
import Main.EvalComputations.Token.RelationalOperator;
import Main.ExceptionHelper;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

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
                UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
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
            AxB.getNfaD().add(stateTransitions);
            AxB.getO().add(determineOutput(A.getO().getInt(p), B.getO().getInt(q), op, combineOut));

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getEntriesNfaD(q);
            for (Int2ObjectMap.Entry<IntList> entryA : A.getEntriesNfaD(p)) {
                final int AxBalphabet = entryA.getIntKey() * B.getAlphabetSize();
                for (Int2ObjectMap.Entry<IntList> entryB : Bset) {
                    int z = allInputsOfAxB[AxBalphabet + entryB.getIntKey()];
                    if (z == -1) {
                        continue;
                    }
                    IntList dest = new IntArrayList(entryA.getValue().size() * entryB.getValue().size());
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
                }
            }
            currentState++;
        }
        AxB.setQ(statesList.size());
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print,
                prefix + "computed cross product:" + AxB.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
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
        AxB.setNfaD(null);
        AxB.setDfaD(new ArrayList<>());
        statesList.add(new IntIntImmutablePair(A.getQ0(), B.getQ0()));
        statesHash.put(new IntIntImmutablePair(A.getQ0(), B.getQ0()), 0);
        int currentState = 0;
        while (currentState < statesList.size()) {
            if (print) {
                int statesSoFar = currentState + 1;
                long timeAfter = System.currentTimeMillis();
                UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
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
            AxB.getDfaD().add(stateTransitions);
            AxB.getO().add(determineOutput(A.getO().getInt(p), B.getO().getInt(q), op, combineOut));

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getEntriesNfaD(q);
            for (Int2ObjectMap.Entry<IntList> entryA : A.getEntriesNfaD(p)) {
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
        AxB.reduceDfaDMemory();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print,
                prefix + "computed cross product:" + AxB.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    private static int determineOutput(int aP, int mQ, String op, int combineOut) {
        return switch (op) {
            case "&" -> (aP != 0 && mQ != 0) ? 1 : 0;
            case "|" -> (aP != 0 || mQ != 0) ? 1 : 0;
            case "^" -> ((aP != 0 && mQ == 0) || (aP == 0 && mQ != 0)) ? 1 : 0;
            case "=>" -> (aP == 0 || mQ != 0) ? 1 : 0;
            case "<=>" -> ((aP == 0 && mQ == 0) || (aP != 0 && mQ != 0)) ? 1 : 0;
            case RelationalOperator.LESS_THAN, RelationalOperator.GREATER_THAN, RelationalOperator.EQUAL, RelationalOperator.NOT_EQUAL, RelationalOperator.LESS_EQ_THAN, RelationalOperator.GREATER_EQ_THAN -> RelationalOperator.compare(op, aP, mQ) ? 1 : 0;
            case ArithmeticOperator.PLUS, ArithmeticOperator.MINUS, ArithmeticOperator.MULT, ArithmeticOperator.DIV -> ArithmeticOperator.arith(op, aP, mQ);
            case "combine" -> (mQ == 1) ? combineOut : aP;
            case "first" -> aP == 0 ? mQ : aP;
            case "if_other" -> mQ != 0 ? aP : 0;
            default -> throw ExceptionHelper.unexpectedOperator(op);
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
     * @param A
     * @param B
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
        printAndUpdateIndex(A.getQ(), B.getQ(), print, prefix, log);
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
        printAndUpdateIndex(A.getQ(), B.getQ(), print, prefix, log);
        crossProductInternalDFA(
                A.fa, B.fa, AxB.fa, combineOut, allInputsOfN, op, print, prefix, log, timeBefore);
        AxB.fa.justMinimize(print, prefix, log);
        if (AxB.fa.getNfaD() == null) {
            throw new RuntimeException("Unexpected null");
        }
        return AxB;
    }

    private static void printAndUpdateIndex(int aQ, int bQ, boolean print, String prefix, StringBuilder log) {
        if (print) {
            //FA.IncrementIndex();
            UtilityMethods.logMessage(print,
                prefix + "Computing cross product:" + aQ + " states - " + bQ + " states", log);
        }
    }

    private static int[] createBasicAutomaton(
            Automaton A, Automaton B, Automaton AxB) {
        if (A.fa.isTRUE_FALSE_AUTOMATON() || B.fa.isTRUE_FALSE_AUTOMATON()) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method cannot be true or false automata.");
        }

        if (A.getLabel() == null ||
                B.getLabel() == null ||
                A.getLabel().size() != A.getA().size() ||
                B.getLabel().size() != B.getA().size()
        ) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method must have labeled inputs.");
        }


        /**
         * for example when sameLabelsInMAndThis[2] = 3, then input 2 of B has the same label as input 3 of this
         * and when sameLabelsInMAndThis[2] = -1, it means that input 2 of B is not an input of this
         */
        int[] sameInputsInMAndThis = new int[B.getA().size()];
        for (int i = 0; i < B.getLabel().size(); i++) {
            sameInputsInMAndThis[i] = -1;
            if (A.getLabel().contains(B.getLabel().get(i))) {
                int j = A.getLabel().indexOf(B.getLabel().get(i));
                if (!UtilityMethods.areEqual(A.getA().get(j), B.getA().get(i))) {
                    throw new RuntimeException("in computing cross product of two automaton, "
                            + "variables with the same label must have the same alphabet");
                }
                sameInputsInMAndThis[i] = j;
            }
        }
        for (int i = 0; i < A.getA().size(); i++) {
            AxB.getA().add(A.getA().get(i));
            AxB.getLabel().add(A.getLabel().get(i));
            AxB.getNS().add(A.getNS().get(i));
        }
        for (int i = 0; i < B.getA().size(); i++) {
            NumberSystem bNS = B.getNS().get(i);
            if (sameInputsInMAndThis[i] == -1) {
                AxB.getA().add(new ArrayList<>(B.getA().get(i)));
                AxB.getLabel().add(B.getLabel().get(i));
                AxB.getNS().add(bNS);
            } else {
                int j = sameInputsInMAndThis[i];
                if (bNS != null && AxB.getNS().get(j) == null) {
                    AxB.getNS().set(j, bNS);
                }
            }
        }
        AxB.determineAlphabetSize();

        IntList allInputsOfN = new IntArrayList();
        for (int i = 0; i < A.getAlphabetSize(); i++) {
            for (int j = 0; j < B.getAlphabetSize(); j++) {
                List<Integer> inputForN = joinTwoInputsForCrossProduct(
                    A.richAlphabet.decode(i), B.richAlphabet.decode(j), sameInputsInMAndThis);
                if (inputForN == null)
                    allInputsOfN.add(-1);
                else
                    allInputsOfN.add(AxB.richAlphabet.encode(inputForN));
            }
        }
        return allInputsOfN.toArray(new int[0]);
    }

    /**
     * For example, suppose that first = [1,2,3], second = [-1,4,2], and equalIndices = [-1,-1,1].
     * Then the result is [1,2,3,-1,4].
     * However, if second = [-1,4,3] then the result is null
     * because 3rd element of the second does not equal the 2nd element of the first.
     *
     * @param first
     * @param second
     * @param equalIndices
     * @return
     */
    private static List<Integer> joinTwoInputsForCrossProduct(
        List<Integer> first, List<Integer> second, int[] equalIndices) {
        List<Integer> R = new ArrayList<>(first);
        for (int i = 0; i < second.size(); i++)
            if (equalIndices[i] == -1)
                R.add(second.get(i));
            else {
                if (!first.get(equalIndices[i]).equals(second.get(i)))
                    return null;
            }
        return R;
    }
}
