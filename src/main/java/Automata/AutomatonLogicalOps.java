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

import Main.ExceptionHelper;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.*;

import java.util.*;

public class AutomatonLogicalOps {
    /**
     * This method is used in and, or, not, and many others.
     * This automaton and M should have TRUE_FALSE_AUTOMATON = false.
     * Both this automaton and M must have labeled inputs.
     * For the sake of an example, suppose that Q = 3, q0 = 1, M.Q = 2, and M.q0 = 0. Then N.Q = 6 and the states of N
     * are {0=(0,0),1=(0,1),2=(1,0),3=(1,1),4=(2,0),5=(2,1)} and N.q0 = 2. The transitions of state (a,b) is then
     * based on the transitions of a and b in this and M.
     * To continue with this example suppose that label = ["i","j"] and
     * M.label = ["p","q","j"]. Then N.label = ["i","j","p","q"], and inputs to N are four tuples.
     * Now suppose in this we go from 0 to 1 by reading (i=1,j=2)
     * and in M we go from 1 to 0 by reading (p=-1,q=-2,j=2).
     * Then in N we go from (0,1) to (1,0) by reading (i=1,j=2,p=-1,q=-2).
     *
     * @param automaton
     * @param M
     * @return this automaton cross product M.
     */
    static Automaton crossProduct(Automaton automaton,
                                  Automaton M,
                                  String op,
                                  boolean print,
                                  String prefix,
                                  StringBuilder log) {

        if (automaton.isTRUE_FALSE_AUTOMATON() || M.isTRUE_FALSE_AUTOMATON()) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method cannot be true or false automata.");
        }

        if (automaton.getLabel() == null ||
                M.getLabel() == null ||
                automaton.getLabel().size() != automaton.getA().size() ||
                M.getLabel().size() != M.getA().size()
        ) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method must have labeled inputs.");
        }

        /**N is going to hold the cross product*/
        Automaton N = new Automaton();

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "Computing cross product:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        /**
         * for example when sameLabelsInMAndThis[2] = 3, then input 2 of M has the same label as input 3 of this
         * and when sameLabelsInMAndThis[2] = -1, it means that input 2 of M is not an input of this
         */
        int[] sameInputsInMAndThis = new int[M.getA().size()];
        for (int i = 0; i < M.getLabel().size(); i++) {
            sameInputsInMAndThis[i] = -1;
            if (automaton.getLabel().contains(M.getLabel().get(i))) {
                int j = automaton.getLabel().indexOf(M.getLabel().get(i));
                if (!UtilityMethods.areEqual(automaton.getA().get(j), M.getA().get(i))) {
                    throw new RuntimeException("in computing cross product of two automaton, "
                            + "variables with the same label must have the same alphabet");
                }
                sameInputsInMAndThis[i] = j;
            }
        }
        for (int i = 0; i < automaton.getA().size(); i++) {
            N.getA().add(automaton.getA().get(i));
            N.getLabel().add(automaton.getLabel().get(i));
            N.getNS().add(automaton.getNS().get(i));
        }
        for (int i = 0; i < M.getA().size(); i++) {
            if (sameInputsInMAndThis[i] == -1) {
                N.getA().add(new ArrayList<>(M.getA().get(i)));
                N.getLabel().add(M.getLabel().get(i));
                N.getNS().add(M.getNS().get(i));
            } else {
                int j = sameInputsInMAndThis[i];
                if (M.getNS().get(i) != null && N.getNS().get(j) == null) {
                    N.getNS().set(j, M.getNS().get(i));
                }
            }
        }
        N.determineAlphabetSizeFromA();

        List<Integer> allInputsOfN = new ArrayList<>();
        for (int i = 0; i < automaton.getAlphabetSize(); i++) {
            for (int j = 0; j < M.getAlphabetSize(); j++) {
                List<Integer> inputForN = joinTwoInputsForCrossProduct(
                    Automaton.decode(automaton.getA(), i), Automaton.decode(M.getA(), j), sameInputsInMAndThis);
                if (inputForN == null)
                    allInputsOfN.add(-1);
                else
                    allInputsOfN.add(N.encode(inputForN));
            }
        }
        int combineOut = op.equals("combine") ? automaton.combineOutputs.getInt(automaton.combineIndex) : -1;
        crossProductInternal(
            automaton.getFa(), M.getFa(), N.getFa(), combineOut, allInputsOfN, op, print, prefix, log, timeBefore);
        return N;
    }

    private static void crossProductInternal(
        FA automaton, FA M, FA N, int combineOut, List<Integer> allInputsOfN, String op,
        boolean print, String prefix, StringBuilder log, long timeBefore) {
        ArrayList<List<Integer>> statesList = new ArrayList<>();
        Map<List<Integer>, Integer> statesHash = new HashMap<>();
        N.setQ0(0);
        statesList.add(Arrays.asList(automaton.getQ0(), M.getQ0()));
        statesHash.put(Arrays.asList(automaton.getQ0(), M.getQ0()), 0);
        int currentState = 0;
        while (currentState < statesList.size()) {
            if (print) {
                int statesSoFar = currentState + 1;
                long timeAfter = System.currentTimeMillis();
                UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0, prefix + "  Progress: Added " + statesSoFar + " states - "
                    + (statesList.size() - statesSoFar) + " states left in queue - "
                    + statesList.size() + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
            }

            List<Integer> s = statesList.get(currentState);

            // s must be an array of length 2, where the first element is a state in this, and the second element is a
            // state in the other Automaton.
            int p = s.get(0);
            int q = s.get(1);
            Int2ObjectRBTreeMap<IntList> thisStatesTransitions = new Int2ObjectRBTreeMap<>();
            N.getD().add(thisStatesTransitions);
            N.getO().add(determineOutput(automaton, M, op, p, q, combineOut));

            for (int x : automaton.getD().get(p).keySet()) {
                for (int y : M.getD().get(q).keySet()) {
                    int z = allInputsOfN.get(x * M.getAlphabetSize() + y);
                    if (z != -1) {
                        IntList dest = new IntArrayList();
                        thisStatesTransitions.put(z, dest);
                        for (int dest1 : automaton.getD().get(p).get(x)) {
                            for (int dest2 : M.getD().get(q).get(y)) {
                                List<Integer> dest3 = Arrays.asList(dest1, dest2);
                                if (!statesHash.containsKey(dest3)) {
                                    statesList.add(dest3);
                                    statesHash.put(dest3, statesList.size() - 1);
                                }
                                dest.add((int) statesHash.get(dest3));
                            }
                        }
                    }
                }
            }
            currentState++;
        }
        N.setQ(statesList.size());
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed cross product:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    private static int determineOutput(FA automaton, FA M, String op, int p, int q, int combineOut) {
        int aP = automaton.getO().getInt(p);
        int mQ = M.getO().getInt(q);
        return switch (op) {
            case "&" -> (aP != 0 && mQ != 0) ? 1 : 0;
            case "|" -> (aP != 0 || mQ != 0) ? 1 : 0;
            case "^" -> ((aP != 0 && mQ == 0) || (aP == 0 && mQ != 0)) ? 1 : 0;
            case "=>" -> (aP == 0 || mQ != 0) ? 1 : 0;
            case "<=>" -> ((aP == 0 && mQ == 0) || (aP != 0 && mQ != 0)) ? 1 : 0;
            case "<" -> (aP < mQ) ? 1 : 0;
            case ">" -> (aP > mQ) ? 1 : 0;
            case "=" -> (aP == mQ) ? 1 : 0;
            case "!=" -> (aP != mQ) ? 1 : 0;
            case "<=" -> (aP <= mQ) ? 1 : 0;
            case ">=" -> (aP >= mQ) ? 1 : 0;
            case "+" -> aP + mQ;
            case "-" -> aP - mQ;
            case "*" -> aP * mQ;
            case "/" -> {
                if (mQ == 0) throw ExceptionHelper.divisionByZero();
                yield Math.floorDiv(aP, mQ);
            }
            case "combine" -> (mQ == 1) ? combineOut : aP;
            case "first" -> aP == 0 ? mQ : aP;
            default -> mQ != 0 ? aP : 0;
        };
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton and M.
     */
    public static Automaton and(Automaton automaton,
                                Automaton M,
                                boolean print,
                                String prefix,
                                StringBuilder log) {
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) &&
                (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON())) {
            return new Automaton(true);
        }

        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) ||
                (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON())) {
            return new Automaton(false);
        }

        if (automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) {
            return M;
        }

        if (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()) {
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing &:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        Automaton N = crossProduct(automaton, M, "&", print, prefix, log);
        N.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed &:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton or M
     */
    public static Automaton or(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) {
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) || (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()))
            return new Automaton(true);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()))
            return new Automaton(false);

        if (automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) return M;
        if (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()) return automaton;

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing |:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        totalize(automaton.getFa(), print, prefix + " ", log);
        totalize(M.getFa(), print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "|", print, prefix, log);

        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed |:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton xor M
     */
    public static Automaton xor(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) {
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()))
            return new Automaton(true);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()))
            return new Automaton(true);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()))
            return new Automaton(false);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()))
            return new Automaton(false);

        if (automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) return M;
        if (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()) return automaton;

        if (automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) {
            not(M, print, prefix, log);
            return M;
        }
        if (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing ^:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        totalize(automaton.getFa(), print, prefix + " ", log);
        totalize(M.getFa(), print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "^", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed ^:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton imply M
     */
    public static Automaton imply(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) {
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()))
            return new Automaton(false);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) || (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()))
            return new Automaton(true);
        if (automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) return M;
        if (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing =>:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        totalize(automaton.getFa(), print, prefix + " ", log);
        totalize(M.getFa(), print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "=>", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed =>:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton iff M
     */
    public static Automaton iff(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) {
        if (((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON())) ||
                ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON())))
            return new Automaton(true);
        if (((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON())) ||
                ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON())))
            return new Automaton(false);

        if (automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) return M;
        if (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()) return automaton;
        if (automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) {
            not(M, print, prefix, log);
            return M;
        }
        if (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing <=>:" + automaton.getQ() + " states - " + M.getQ() + " states", log);

        totalize(automaton.getFa(), print, prefix + " ", log);
        totalize(M.getFa(), print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "<=>", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed <=>:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }

    /**
     * @return changes this automaton to its negation
     */
    public static void not(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            automaton.setTRUE_AUTOMATON(!automaton.isTRUE_AUTOMATON());
            return;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computing ~:" + automaton.getQ() + " states", log);

        totalize(automaton.getFa(), print, prefix + " ", log);
        for (int q = 0; q < automaton.getQ(); q++)
            automaton.getO().set(q, automaton.getO().getInt(q) != 0 ? 0 : 1);

        automaton.minimize(null, print, prefix + " ", log);
        automaton.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "computed ~:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * If this automaton's language is L_1 and the language of "other" is L_2, this automaton accepts the language
     * L_1 / L_2 = { x : exists y in L_2 such that xy in L_1 }
     *
     * @param automaton
     * @param other
     * @param print
     * @param prefix
     * @param log
     * @return
     */
    public static Automaton rightQuotient(Automaton automaton, Automaton other, boolean skipSubsetCheck, boolean print, String prefix, StringBuilder log) {

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient: " + automaton.getQ() + " state automaton with " + other.getQ() + " state automaton", log);

        if (!skipSubsetCheck) {
            // check whether the alphabet of other is a subset of the alphabet of self. If not, throw an error.
            if (!isSubsetA(other.getA(), automaton.getA())) {
                throw new RuntimeException("Second automaton's alphabet must be a subset of the first automaton's alphabet for right quotient.");
            }
        }

        // The returned automaton will have the same states and transition function as this automaton, but
        // the final states will be different.
        Automaton M = automaton.clone();

        Automaton otherClone = other.clone();

        List<Int2ObjectRBTreeMap<IntList>> newOtherD = new ArrayList<>();

        for (int q = 0; q < otherClone.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (int x : otherClone.getD().get(q).keySet()) {
                newMap.put(automaton.encode(Automaton.decode(otherClone.getA(), x)), otherClone.getD().get(q).get(x));
            }
            newOtherD.add(newMap);
        }
        otherClone.setD(newOtherD);
        otherClone.setEncoder(automaton.getEncoder());
        otherClone.setA(automaton.getA());
        otherClone.setAlphabetSize(automaton.getAlphabetSize());
        otherClone.setNS(automaton.getNS());

        for (int i = 0; i < automaton.getQ(); i++) {
            // this will be a temporary automaton that will be the same as as self except it will start from the automaton
            Automaton T = automaton.clone();

            if (i != 0) {
                T.setQ0(i);
                T.setCanonized(false);
                T.canonize();
            }

            // need to have the same label for cross product (including "and")
            T.randomLabel();
            otherClone.setLabel(T.getLabel());

            Automaton I = and(T, otherClone, print, prefix, log);

            M.getO().set(i, I.isEmpty() ? 0 : 1);
        }

        M.minimize(null, print, prefix, log);
        M.applyAllRepresentations();
        M.setCanonized(false);
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "right quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    public static Automaton leftQuotient(Automaton automaton, Automaton other, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient: " + automaton.getQ() + " state automaton with " + other.getQ() + " state automaton", log);

        // check whether the alphabet of self is a subset of the alphabet of other. If not, throw an error.
        if (!isSubsetA(automaton.getA(), other.getA())) {
            throw new RuntimeException("First automaton's alphabet must be a subset of the second automaton's alphabet for left quotient.");
        }

        Automaton M1 = reverseAndCanonize(automaton, print, prefix, log);
        Automaton M2 = reverseAndCanonize(other, print, prefix, log);
        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        reverse(M, print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "left quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return M;
    }

    private static Automaton reverseAndCanonize(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        Automaton M1 = automaton.clone();
        reverse(M1, print, prefix, log, true);
        M1.setCanonized(false);
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
     * This method adds a dead state to totalize the transition function
     *
     */
    static void totalize(FA automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "totalizing:" + automaton.getQ() + " states", log);
        automaton.totalize();
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "totalized:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }


    /**
     * For example, suppose that first = [1,2,3], second = [-1,4,2], and equalIndices = [-1,-1,1].
     * Then the result is [1,2,3,-1,4].
     * However if second = [-1,4,3] then the result is null
     * because 3rd element of second is not equal two 2nd element of first.
     *
     * @param first
     * @param second
     * @param equalIndices
     * @return
     */
    private static List<Integer> joinTwoInputsForCrossProduct(
            List<Integer> first, List<Integer> second, int[] equalIndices) {
        List<Integer> R = new ArrayList<>();
        R.addAll(first);
        for (int i = 0; i < second.size(); i++)
            if (equalIndices[i] == -1)
                R.add(second.get(i));
            else {
                if (!first.get(equalIndices[i]).equals(second.get(i)))
                    return null;
            }
        return R;
    }

    public static void fixLeadingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) return;
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixing leading zeros:" + automaton.getQ() + " states", log);
        automaton.setCanonized(false);
        int zero = determineZero(automaton);

        IntSet initial_state = automaton.getFa().zeroReachableStates(zero);
        List<Int2IntMap> newMemD = automaton.getFa().subsetConstruction(null, initial_state, print, prefix + " ", log);
        automaton.minimize(newMemD, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixed leading zeros:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    private static int determineZero(Automaton automaton) {
        List<Integer> ZERO = new ArrayList<>(automaton.getA().size());//all zero input
        for (List<Integer> i : automaton.getA()) ZERO.add(i.indexOf(0));
        return automaton.encode(ZERO);
    }

    public static void fixTrailingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixing trailing zeros:" + automaton.getQ() + " states", log);
        automaton.setCanonized(false);

        automaton.getFa().setStatesReachableToFinalStatesByZeros(determineZero(automaton));

        automaton.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "fixed trailing zeros:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * Used for the "I" quantifier. If some input is in msd, then we remove leading zeroes,
     * if some input is in lsd, then we remove trailing zeroes, otherwise, we do nothing.
     * To do this, for each input, we construct an automaton which accepts if the leading/trailing input is non-zero,
     * union all these automata together, and intersect with our original automaton.
     *
     * @param automaton
     * @param listOfLabels
     * @return
     */
    public static Automaton removeLeadingZeroes(Automaton automaton, List<String> listOfLabels, boolean print, String prefix, StringBuilder log) {
        for (String s : listOfLabels) {
            if (!automaton.getLabel().contains(s)) {
                throw new RuntimeException("Variable " + s + " in the list of quantified variables is not a free variable.");
            }
        }
        if (listOfLabels.isEmpty()) {
            return automaton.clone();
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "removing leading zeroes for:" + automaton.getQ() + " states", log);

        List<Integer> listOfInputs = new ArrayList<>();//extract the list of indices of inputs from the list of labels
        for (String l : listOfLabels) {
            listOfInputs.add(automaton.getLabel().indexOf(l));
        }
        Automaton M = new Automaton(false);
        for (int n : listOfInputs) {
            Automaton N = removeLeadingZeroesHelper(automaton, n, print, prefix + " ", log);
            M = or(M, N, print, prefix + " ", log);
        }
        M = and(automaton, M, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantified:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    /**
     * Returns the automaton with the same alphabet as the current automaton, which requires the nth input to
     * start with a non-zero symbol (if msd), end with a non-zero symbol (if lsd), otherwise, return the true
     * automaton. The returned automaton is meant to be intersected with the current automaton to remove
     * leading/trailing * zeroes from the nth input.
     *
     * @param automaton
     * @param n
     * @return
     */
    private static Automaton removeLeadingZeroesHelper(
        Automaton automaton, int n, boolean print, String prefix, StringBuilder log) {
        if (n >= automaton.getA().size() || n < 0) {
            throw new RuntimeException("Cannot remove leading zeroes for the "
                    + (n + 1) + "-th input when automaton only has " + automaton.getA().size() + " inputs.");
        }

        if (automaton.getNS().get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.setQ(2);
        M.getO().add(1);
        M.getO().add(1);
        M.getD().add(new Int2ObjectRBTreeMap<>());
        M.getD().add(new Int2ObjectRBTreeMap<>());
        M.setNS(automaton.getNS());
        M.setA(automaton.getA());
        M.setLabel(automaton.getLabel());
        M.setAlphabetSize(automaton.getAlphabetSize());

        IntList dest = new IntArrayList();
        dest.add(1);
        for (int i = 0; i < automaton.getAlphabetSize(); i++) {
            List<Integer> list = Automaton.decode(automaton.getA(), i);
            if (list.get(n) != 0) {
                M.getD().get(0).put(i, new IntArrayList(dest));
            }
            M.getD().get(1).put(i, new IntArrayList(dest));
        }
        if (!automaton.getNS().get(n).isMsd()) {
            reverse(M, print, prefix, log, false);
        }
        return M;
    }


    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately. So for example an
     * expression like f(a,a) becomes
     * an automaton with one input. After we are done with input i, we call removeSameInputs(i+1)
     *
     * @param automaton
     * @param i
     */
    static void removeSameInputs(Automaton automaton, int i) {
        if (i >= automaton.getA().size()) return;
        List<Integer> I = new ArrayList<>();
        I.add(i);
        for (int j = i + 1; j < automaton.getA().size(); j++) {
            if (automaton.getLabel().get(i).equals(automaton.getLabel().get(j))) {
                if (!UtilityMethods.areEqual(automaton.getA().get(i), automaton.getA().get(j))) {
                    throw new RuntimeException("Inputs " + i + " and " + j + " have the same label but different alphabets.");
                }
                I.add(j);
            }
        }
        if (I.size() > 1) {
            reduceDimension(automaton, I);
        }
        removeSameInputs(automaton, i + 1);
    }

    private static void reduceDimension(Automaton automaton, List<Integer> I) {
        List<List<Integer>> newAlphabet = new ArrayList<>();
        List<Integer> newEncoder = new ArrayList<>();
        newEncoder.add(1);
        for (int i = 0; i < automaton.getA().size(); i++)
            if (!I.contains(i) || I.indexOf(i) == 0)
                newAlphabet.add(new ArrayList<>(automaton.getA().get(i)));
        for (int i = 0; i < newAlphabet.size() - 1; i++)
            newEncoder.add(newEncoder.get(i) * newAlphabet.get(i).size());
        List<Integer> map = new ArrayList<>();
        for (int n = 0; n < automaton.getAlphabetSize(); n++)
            map.add(automaton.mapToReducedEncodedInput(n, I, newEncoder, automaton.getA(), newAlphabet));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            new_d.add(currentStatesTransition);
            for (int n : automaton.getD().get(q).keySet()) {
                int m = map.get(n);
                if (m != -1) {
                    IntList dest = automaton.getD().get(q).get(n);
                    if (currentStatesTransition.containsKey(m))
                        currentStatesTransition.get(m).addAll(dest);
                    else
                        currentStatesTransition.put(m, new IntArrayList(dest));
                }
            }
        }
        automaton.setD(new_d);
        I.remove(0);
        automaton.setA(newAlphabet);
        UtilityMethods.removeIndices(automaton.getNS(), I);
        automaton.setEncoder(null);
        automaton.determineAlphabetSizeFromA();
        UtilityMethods.removeIndices(automaton.getLabel(), I);
    }

    /**
     * The operator can be one of "+" "-" "*" "/".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs this[x] + W[x] on input x.
     * To be used only when this automaton and M are DFAOs (words).
     *
     * @param automaton
     * @param W
     * @param operator
     * @return
     */
    public static Automaton applyOperator(Automaton automaton, Automaton W, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applying operator (" + operator + "):" + automaton.getQ() + " states - " + W.getQ() + " states", log);
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimizeWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "applied operator (" + operator + "):" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    public static void quantify(Automaton automaton, String labelToQuantify, boolean print, String prefix, StringBuilder log) {
        Set<String> listOfLabelsToQuantify = new HashSet<>();
        listOfLabelsToQuantify.add(labelToQuantify);
        quantify(automaton, listOfLabelsToQuantify, print, prefix, log);
    }

    public static void quantify(Automaton automaton, String labelToQuantify1, String labelToQuantify2, boolean leadingZeros, boolean print, String prefix, StringBuilder log) {
        Set<String> listOfLabelsToQuantify = new HashSet<>();
        listOfLabelsToQuantify.add(labelToQuantify1);
        listOfLabelsToQuantify.add(labelToQuantify2);
        quantify(automaton, listOfLabelsToQuantify, print, prefix, log);
    }

    /**
     * This method computes the existential quantification of this automaton.
     * Takes a list of labels and performs the existential quantifier over
     * the inputs with labels in listOfLabelsToQuantify. It simply eliminates inputs in listOfLabelsToQuantify.
     * After the quantification is done, we address the issue of
     * leadingZeros or trailingZeros (depending on the value of leadingZeros), if all of the inputs
     * of the resulting automaton are of type arithmetic.
     * This is why we mandate that an input of type arithmetic must have 0 in its alphabet, also that
     * every number system must use 0 to denote its additive identity.
     *
     * @param automaton
     * @param listOfLabelsToQuantify must contain at least one element. listOfLabelsToQuantify must be a subset of this.label.
     * @return
     */
    public static void quantify(Automaton automaton, Set<String> listOfLabelsToQuantify, boolean print, String prefix, StringBuilder log) {
        quantifyHelper(automaton, listOfLabelsToQuantify, print, prefix, log);
        if (automaton.isTRUE_FALSE_AUTOMATON()) return;

        boolean isMsd = true;
        boolean flag = false;
        for (NumberSystem ns : automaton.getNS()) {
            if (ns == null)
                return;
            if (flag && (ns.isMsd() != isMsd))
                return;
            isMsd = ns.isMsd();
            flag = true;
        }
        if (isMsd)
            fixLeadingZerosProblem(automaton, print, prefix, log);
        else
            fixTrailingZerosProblem(automaton, print, prefix, log);
    }

    /**
     * This method is very similar to public void quantify(Set<String> listOfLabelsToQuantify,boolean leadingZeros)
     * with the exception that, this method does not deal with leading/trailing zeros problem.
     *
     * @param automaton
     * @param listOfLabelsToQuantify
     */
    private static void quantifyHelper(Automaton automaton,
                                       Set<String> listOfLabelsToQuantify,
                                       boolean print,
                                       String prefix,
                                       StringBuilder log) {
        if (listOfLabelsToQuantify.isEmpty() || automaton.getLabel() == null) {
            return;
        }

        String name_of_labels = "";
        for (String s : listOfLabelsToQuantify) {
            if (!automaton.getLabel().contains(s)) {
                throw new RuntimeException(
                        "Variable " + s + " in the list of quantified variables is not a free variable.");
            }
            if (name_of_labels.isEmpty()) {
                name_of_labels += s;
            } else {
                name_of_labels += "," + s;
            }
        }
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantifying:" + automaton.getQ() + " states", log);

        /**
         * If this is the case, then the quantified automaton is either the true or false automaton.
         * It is true if this's language is not empty.
         */
        if (listOfLabelsToQuantify.size() == automaton.getA().size()) {
            automaton.setTRUE_AUTOMATON(!automaton.isEmpty());
            automaton.setTRUE_FALSE_AUTOMATON(true);
            automaton.clear();
            return;
        }

        List<Integer> listOfInputsToQuantify = new ArrayList<>();//extract the list of indices of inputs we would like to quantify
        for (String l : listOfLabelsToQuantify)
            listOfInputsToQuantify.add(automaton.getLabel().indexOf(l));
        List<List<Integer>> allInputs = new ArrayList<>();
        for (int i = 0; i < automaton.getAlphabetSize(); i++)
            allInputs.add(Automaton.decode(automaton.getA(), i));
        //now we remove those indices in listOfInputsToQuantify from A,T,label, and allInputs
        UtilityMethods.removeIndices(automaton.getA(), listOfInputsToQuantify);
        automaton.setEncoder(null);
        automaton.determineAlphabetSizeFromA();
        UtilityMethods.removeIndices(automaton.getNS(), listOfInputsToQuantify);
        UtilityMethods.removeIndices(automaton.getLabel(), listOfInputsToQuantify);
        for (List<Integer> i : allInputs)
            UtilityMethods.removeIndices(i, listOfInputsToQuantify);
        //example: permutation[1] = 7 means that encoded old input 1 becomes encoded new input 7
        List<Integer> permutation = new ArrayList<>();
        for (List<Integer> i : allInputs)
            permutation.add(automaton.encode(i));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.getQ(); q++) {
            Int2ObjectRBTreeMap<IntList> newMemDransitionFunction = new Int2ObjectRBTreeMap<>();
            new_d.add(newMemDransitionFunction);
            for (int x : automaton.getD().get(q).keySet()) {
                int y = permutation.get(x);
                if (newMemDransitionFunction.containsKey(y))
                    UtilityMethods.addAllWithoutRepetition(newMemDransitionFunction.get(y), automaton.getD().get(q).get(x));
                else
                    newMemDransitionFunction.put(y, new IntArrayList(automaton.getD().get(q).get(x)));
            }
        }
        automaton.setD(new_d);
        automaton.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "quantified:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    /**
     * this automaton should not be a word automaton (automaton with output). However, it can be non deterministic.
     * Enabling the reverseMsd flag will flip the number system of the automaton from msd to lsd, and vice versa.
     * Note that reversing the Msd will also call this function as reversals are done in the NumberSystem class upon
     * initializing.
     *
     * @return the reverse of this automaton
     */
    public static void reverse(
        Automaton automaton, boolean print, String prefix, StringBuilder log, boolean reverseMsd, boolean skipMinimize) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            return;
        }

        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "Reversing:" + automaton.getQ() + " states", log);

        IntSet setOfFinalStates = automaton.getFa().reverseDFAtoNFAInternal();

        List<Int2IntMap> newMemD = automaton.getFa().subsetConstruction(null, setOfFinalStates, print, prefix + " ", log);

        if (!skipMinimize) {
            automaton.minimize(newMemD, print, prefix + " ", log);
        }

        if (reverseMsd) {
            NumberSystem.flipNS(automaton.getNS());
        }

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "reversed:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    public static void reverse(
        Automaton automaton, boolean print, String prefix, StringBuilder log, boolean reverseMsd) {
        reverse(automaton, print, prefix, log, reverseMsd, false);
    }

    public static void reverse(
        Automaton automaton, boolean print, String prefix, StringBuilder log) {
        reverse(automaton, print, prefix, log, false);
    }

    /**
     * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
     *
     */
    public static void reverseWithOutput(Automaton automaton, boolean reverseMsd, boolean print, String prefix, StringBuilder log) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            return;
        }
        try {
            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "reversing: " + automaton.getQ() + " states", log);

            boolean addedDeadState = automaton.getFa().addDistinguishedDeadState(print, prefix, log);

            int minOutput = 0;
            if (addedDeadState) {
                // get state with smallest output. all states with this output will be removed.
                // after transducing, all states with this minimum output will be removed.

                for (int i = 0; i < automaton.getO().size(); i++) {
                    if (automaton.getO().getInt(i) < minOutput) {
                        minOutput = automaton.getO().getInt(i);
                    }
                }
            }


            // need to define states, an initial state, transitions, and outputs.

            ArrayList<Map<Integer, Integer>> newStates = new ArrayList<>();

            HashMap<Map<Integer, Integer>, Integer> newStatesHash = new HashMap<>();

            Queue<Map<Integer, Integer>> newStatesQueue = new LinkedList<>();

            Map<Integer, Integer> newInitState = new HashMap<>();

            IntList newO = new IntArrayList();

            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

            for (int i = 0; i < automaton.getQ(); i++) {
                newInitState.put(i, automaton.getO().getInt(i));
            }

            newStates.add(newInitState);

            newStatesHash.put(newInitState, newStates.size() - 1);

            newStatesQueue.add(newInitState);

            while (!newStatesQueue.isEmpty()) {
                Map<Integer, Integer> currState = newStatesQueue.remove();

                // set up the output of this state to be g(q0), where g = currState.
                newO.add((int) currState.get(automaton.getQ0()));

                newD.add(new Int2ObjectRBTreeMap<>());

                // assume that the
                // System.out.println("alphabet: " + d.get(q0) + ", " + d + ", " + alphabetSize);
                if (automaton.getD().get(automaton.getQ0()).keySet().size() != automaton.getAlphabetSize()) {
                    throw new RuntimeException("Automaton should be deterministic!");
                }
                for (int l : automaton.getD().get(automaton.getQ0()).keySet()) {
                    Map<Integer, Integer> toState = new HashMap<>();

                    for (int i = 0; i < automaton.getQ(); i++) {
                        toState.put(i, currState.get(automaton.getD().get(i).get(l).getInt(0)));
                    }

                    if (!newStatesHash.containsKey(toState)) {
                        newStates.add(toState);
                        newStatesHash.put(toState, newStates.size() - 1);
                        newStatesQueue.add(toState);
                    }

                    // set up the transition.
                    IntList newList = new IntArrayList();
                    newList.add((int) newStatesHash.get(toState));
                    newD.get(newD.size() - 1).put(l, newList);
                }
            }

            automaton.setQ(newStates.size());

            automaton.setO(newO);

            automaton.setD(newD);

            if (reverseMsd) {
                NumberSystem.flipNS(automaton.getNS());
            }

            automaton.minimizeSelfWithOutput(print, prefix + " ", log);

            if (addedDeadState) {
                // remove all states that have an output of minOutput
                HashSet<Integer> statesRemoved = new HashSet<>();

                for (int q = 0; q < automaton.getQ(); q++) {
                    if (automaton.getO().getInt(q) == minOutput) {
                        statesRemoved.add(q);
                    }
                }
                for (int q = 0; q < automaton.getQ(); q++) {

                    Iterator<Integer> iter = automaton.getD().get(q).keySet().iterator();

                    while (iter.hasNext()) {
                        int x = iter.next();

                        if (statesRemoved.contains(automaton.getD().get(q).get(x).getInt(0))) {
                            iter.remove();
                        }
                    }
                }

                automaton.setCanonized(false);
                automaton.canonize();
            }

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "reversed: " + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reversing word automaton");
        }
    }

    /**
     * Convert the number system of a word automaton from [msd/lsd]_k^i to [msd/lsd]_k^j.
     */
    public static void convertNS(Automaton automaton, boolean toMsd, int toBase,
                                 boolean print, String prefix, StringBuilder log) {
        try {
            if (automaton.getNS().size() != 1) {
                throw new RuntimeException("Automaton must have exactly one input to be converted.");
            }

            // 1) Parse the base from the automaton’s NS
            int fromBase = parseBase(automaton.getNS().get(0).getName());

            // If the old and new bases are the same, check if only MSD/LSD is changing
            if (fromBase == toBase) {
                if (automaton.getNS().get(0).isMsd() == toMsd) {
                    throw new RuntimeException("New and old number systems are identical: " +
                        automaton.getNS().get(0).getName());
                } else {
                    // If only msd <-> lsd differs, just reverse the automaton
                    reverseWithOutput(automaton, true, print, prefix + " ", log);
                    return;
                }
            }

            // 2) Check if fromBase and toBase are powers of the same root
            int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);
            if (commonRoot == -1) {
                throw new RuntimeException("New and old number systems must have bases k^i and k^j for some integer k.");
            }

            // If originally LSD, we need to reverse to treat it as MSD for the conversions
            if (!automaton.getNS().get(0).isMsd()) {
                reverseWithOutput(automaton, true, print, prefix + " ", log);
            }

            // We'll track if the automaton is reversed relative to original
            boolean currentlyReversed = false;

            // 3) Convert from k^i -> k if needed
            if (fromBase != commonRoot) {
                int exponent = (int) (Math.log(fromBase) / Math.log(commonRoot));
                reverseWithOutput(automaton, true, print, prefix + " ", log);
                currentlyReversed = true;

                convertLsdBaseToRoot(automaton, commonRoot, exponent, print, prefix + " ", log);
                automaton.minimizeSelfWithOutput(print, prefix + " ", log);
            }

            // 4) Convert from k -> k^j if needed
            if (toBase != commonRoot) {
                if (currentlyReversed) {
                    // Undo reversal from the previous step
                    reverseWithOutput(automaton, true, print, prefix + " ", log);
                    currentlyReversed = false;
                }
                int exponent = (int) (Math.log(toBase) / Math.log(commonRoot));
                convertMsdBaseToExponent(automaton, exponent, print, prefix + " ", log);
                automaton.minimizeSelfWithOutput(print, prefix + " ", log);
            }

            // 5) If final desired base is LSD but we are still in MSD form, reverse again
            if (toMsd == currentlyReversed) {
                reverseWithOutput(automaton, true, print, prefix + " ", log);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting the number system of an automaton", e);
        }
    }

    /**
     * Assuming this automaton is in number system msd_k with one input,
     * convert it to number system msd_{k^exponent} with one input.
     */
    private static void convertMsdBaseToExponent(Automaton automaton, int exponent,
                                                 boolean print, String prefix, StringBuilder log) {
        try {
            int base = parseBase(automaton.getNS().get(0).getName());

            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(
                print,
                prefix + "Converting: msd_" + base + " to msd_" + (int) Math.pow(base, exponent)
                    + ", " + automaton.getQ() + " states",
                log
            );

            if (!automaton.getFa().isDeterministic()) {
                throw new RuntimeException("Automaton must be deterministic for msd_k^j conversion");
            }

            automaton.getFa().updateTransitionsFromMorphism(exponent);

            // Update number system: msd_{base^exponent}
            int newBase = (int) Math.pow(base, exponent);
            automaton.getNS().set(0, new NumberSystem("msd_" + newBase));
            setAutomatonAlphabet(automaton, newBase);

            UtilityMethods.logMessage(print, prefix + "Converted: msd_" + base + " to msd_" + newBase
                + ", " + automaton.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms", log);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting msd_k to msd_k^j", e);
        }
    }

    /**
     * Assuming this automaton is in number system lsd_{k^j} with one input,
     * convert it to number system lsd_k with one input.
     */
    private static void convertLsdBaseToRoot(Automaton automaton, int root, int exponent,
                                             boolean print, String prefix, StringBuilder log) {
        try {
            // Parse base and validate
            int base = parseBase(automaton.getNS().get(0).getName());
            double expected = Math.pow(root, exponent);
            if (base != (int) expected) {
                throw new RuntimeException("Base mismatch: expected " + (int) expected + ", found " + base);
            }

            long timeBefore = System.currentTimeMillis();
            UtilityMethods.logMessage(
                print,
                prefix + "Converting: lsd_" + base + " to lsd_" + (int) expected
                    + ", " + automaton.getQ() + " states",
                log
            );

            // BFS-like approach with StateTuple
            class StateTuple {
                final int state;
                final List<Integer> string;

                StateTuple(int state, List<Integer> string) {
                    this.state = state;
                    this.string = string;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    StateTuple other = (StateTuple) o;
                    // Compare both the state and the string for uniqueness
                    return this.state == other.state && this.string.equals(other.string);
                }

                @Override
                public int hashCode() {
                    int result = Integer.hashCode(this.state);
                    result = 31 * result + this.string.hashCode();
                    return result;
                }
            }

            // Prepare BFS structures
            ArrayList<StateTuple> newStates = new ArrayList<>();
            Queue<StateTuple> queue = new LinkedList<>();
            Map<StateTuple, Integer> stateMap = new HashMap<>();
            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
            IntList newO = new IntArrayList();

            // Initialize BFS with the automaton's Q0
            StateTuple init = new StateTuple(automaton.getQ0(), List.of());
            newStates.add(init);
            queue.add(init);
            stateMap.put(init, newStates.size() - 1);

            // BFS
            while (!queue.isEmpty()) {
                StateTuple curr = queue.remove();

                // Create a new transition map in newD
                newD.add(new Int2ObjectRBTreeMap<>());

                // Output logic
                if (curr.string.isEmpty()) {
                    newO.add(automaton.getO().getInt(curr.state));
                } else {
                    int stringVal = computeStringValue(curr.string, root);
                    // The next real state is automaton.getD().get(curr.state).get(stringVal).getInt(0)
                    int realState = automaton.getD().get(curr.state).get(stringVal).getInt(0);
                    newO.add(automaton.getO().getInt(realState));
                }

                // Build transitions for each possible digit di in [0..root-1]
                for (int di = 0; di < root; di++) {
                    List<Integer> nextString = new ArrayList<>(curr.string);
                    nextString.add(di);

                    StateTuple next;
                    if (curr.string.size() < exponent - 1) {
                        // Haven't reached exponent length yet
                        next = new StateTuple(curr.state, nextString);
                    } else {
                        // We have a full 'digit string', so jump to an actual next state
                        int nextStringVal = computeStringValue(nextString, root);
                        int realState = automaton.getD().get(curr.state).get(nextStringVal).getInt(0);
                        next = new StateTuple(realState, List.of());
                    }

                    // If this state is new, register it
                    if (!stateMap.containsKey(next)) {
                        newStates.add(next);
                        queue.add(next);
                        stateMap.put(next, newStates.size() - 1);
                    }

                    // Add transition
                    int mappedIndex = stateMap.get(next);
                    IntList destList = new IntArrayList();
                    destList.add(mappedIndex);
                    newD.get(stateMap.get(curr)).put(di, destList);
                }
            }

            // Update automaton
            automaton.setQ(newStates.size());
            automaton.setO(newO);
            automaton.setD(newD);
            automaton.setCanonized(false);

            // Update number system to lsd_root
            automaton.getNS().set(0, new NumberSystem("lsd_" + root));
            setAutomatonAlphabet(automaton, root);

            UtilityMethods.logMessage(
                print,
                prefix + prefix + "Converted: lsd_" + base + " to lsd_" + (int) expected
                    + ", " + automaton.getQ() + " states - " + (System.currentTimeMillis() - timeBefore) + "ms",
            log);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting lsd_{k^j} to lsd_k", e);
        }
    }

    /**
     * Parse the base (k) from an Automaton's number system name, e.g. "msd_10" => 10.
     * Throws if base is invalid.
     */
    private static int parseBase(String nsName) {
        String baseStr = nsName.substring(nsName.indexOf("_") + 1);
        if (!UtilityMethods.isNumber(baseStr) || Integer.parseInt(baseStr) <= 1) {
            throw new RuntimeException("Base of automaton's number system must be > 1, found: " + baseStr);
        }
        return Integer.parseInt(baseStr);
    }


    /**
     * Updates the automaton's alphabet to [0..newBase-1] and sets alphabetSize accordingly.
     */
    private static void setAutomatonAlphabet(Automaton automaton, int newBase) {
        ArrayList<Integer> ints = new ArrayList<>(newBase);
        for (int i = 0; i < newBase; i++) {
            ints.add(i);
        }
        automaton.setA(List.of(ints));
        automaton.setAlphabetSize(newBase);
    }

    /**
     * Compute the numeric value of a 'digit' list in the given root^position sense.
     * (Used in convertLsdBaseToRoot BFS)
     */
    private static int computeStringValue(List<Integer> digits, int root) {
        int value = 0;
        for (int i = 0; i < digits.size(); i++) {
            value += digits.get(i) * Math.pow(root, i);
        }
        return value;
    }



    public static Automaton combine(Automaton automaton, Queue<Automaton> subautomata, IntList outputs, boolean print, String prefix, StringBuilder log) {

        Automaton first = automaton.clone();

        // In an automaton without output, every non-zero output value represents an accepting state
        // we change this to correspond to the value assigned to the first automaton by our command
        for (int q = 0; q < first.getQ(); q++) {
            if (first.getO().getInt(q) != 0) {
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
            totalize(first.getFa(), print, prefix + " ", log);
            totalize(next.getFa(), print, prefix + " ", log);
            Automaton product = crossProduct(first, next, "combine", print, prefix + " ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(print, prefix + "computed =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        }

        // totalize the resulting automaton
        totalize(first.getFa(), print, prefix + " ", log);
        first.canonizeAndApplyAllRepresentationsWithOutput(print, prefix + " ", log);

        return first;
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method returns
     * a DFA that accepts x iff this[x] < W[x] lexicographically.
     * To be used only when this automaton and M are DFAOs (words).
     *
     * @param automaton
     * @param W
     * @param operator
     * @return
     */
    public static Automaton compare(
            Automaton automaton, Automaton W, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "comparing (" + operator + "):" + automaton.getQ() + " states - " + W.getQ() + " states", log);
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "compared (" + operator + "):" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
        return M;
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method changes the word automaton
     * to a DFA that accepts x iff this[x] < o lexicographically.
     * To be used only when this automaton is a DFAO (word).
     *
     * @param automaton
     * @param operator
     * @return
     */
    public static void compare(
            Automaton automaton, int o, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "comparing (" + operator + ") against " + o + ":" + automaton.getQ() + " states", log);
        IntList aO = automaton.getO();
        for (int p = 0; p < automaton.getQ(); p++) {
            int aP = aO.getInt(p);
            switch (operator) {
                case "<":
                    aO.set(p, (aP < o) ? 1 : 0);
                    break;
                case ">":
                    aO.set(p, (aP > o) ? 1 : 0);
                    break;
                case "=":
                    aO.set(p, (aP == o) ? 1 : 0);
                    break;
                case "!=":
                    aO.set(p, (aP != o) ? 1 : 0);
                    break;
                case "<=":
                    aO.set(p, (aP <= o) ? 1 : 0);
                    break;
                case ">=":
                    aO.set(p, (aP >= o) ? 1 : 0);
                    break;
            }
        }
        automaton.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "compared (" + operator + ") against " + o + ":" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> resultLists = new ArrayList<>();
        if (lists.isEmpty()) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (T condition : firstList) {
                for (List<T> remainingList : remainingLists) {
                    ArrayList<T> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }
}
