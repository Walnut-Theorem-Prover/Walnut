package Automata.FA;

import Main.ExceptionHelper;
import Main.UtilityMethods;
import Token.ArithmeticOperator;
import Token.RelationalOperator;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

public class ProductStrategies {
    /**
     * Cross-product of two DFAs. Output is an NFA (for now).
     */
    public static void crossProductInternal(
            FA A, FA B, FA AxB, int combineOut, int[] allInputsOfAxB, String op,
            boolean print, String prefix, StringBuilder log, long timeBefore) {
        List<IntIntPair> statesList = new ArrayList<>();
        Object2IntMap<IntIntPair> statesHash = new Object2IntOpenHashMap<>();
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
            // List<Int2IntMap> dfaD
            AxB.getNfaD().add(stateTransitions);
            AxB.getO().add(determineOutput(A.getO().getInt(p), B.getO().getInt(q), op, combineOut));

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getNfaD().get(q).int2ObjectEntrySet();
            for (Int2ObjectMap.Entry<IntList> entryA : A.getNfaD().get(p).int2ObjectEntrySet()) {
                final int AxBalphabet = entryA.getIntKey() * B.getAlphabetSize();
                for (Int2ObjectMap.Entry<IntList> entryB : Bset) {
                    int z = allInputsOfAxB[AxBalphabet + entryB.getIntKey()];
                    if (z == -1) {
                        continue;
                    }
                    IntList dest = new IntArrayList();
                    stateTransitions.put(z, dest);
                    for (int destA : entryA.getValue()) {
                        for (int destB : entryB.getValue()) {
                            // Note: since A and B are DFAs, there's only one value ever here.
                            IntIntPair dest3 = new IntIntImmutablePair(destA, destB);
                            if (!statesHash.containsKey(dest3)) {
                                statesList.add(dest3);
                                statesHash.put(dest3, statesList.size() - 1);
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

            Set<Int2ObjectMap.Entry<IntList>> Bset = B.getNfaD().get(q).int2ObjectEntrySet();
            for (Int2ObjectMap.Entry<IntList> entryA : A.getNfaD().get(p).int2ObjectEntrySet()) {
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
                            if (!statesHash.containsKey(dest3)) {
                                statesList.add(dest3);
                                statesHash.put(dest3, statesList.size() - 1);
                            }
                            stateTransitions.put(z, statesHash.getInt(dest3));
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
            case "<", ">", "=", "!=", "<=", ">=" -> RelationalOperator.compare(op, aP, mQ) ? 1 : 0;
            case "+", "-", "*", "/" -> ArithmeticOperator.arith(op, aP, mQ);
            case "combine" -> (mQ == 1) ? combineOut : aP;
            case "first" -> aP == 0 ? mQ : aP;
            case "if_other" -> mQ != 0 ? aP : 0;
            default -> throw ExceptionHelper.unexpectedOperator(op);
        };
    }
}
