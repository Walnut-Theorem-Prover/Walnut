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
        if (print) {
            String msg = prefix + "Computing cross product:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

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
        N.setAlphabetSize(1);
        for (List<Integer> i : N.getA()) {
            N.setAlphabetSize(N.getAlphabetSize() * i.size());
        }

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
                if (statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0) {
                    String msg = prefix + "  Progress: Added " + statesSoFar + " states - "
                            + (statesList.size() - statesSoFar) + " states left in queue - "
                            + statesList.size() + " reachable states - " + (timeAfter - timeBefore) + "ms";
                    log.append(msg + System.lineSeparator());
                    System.out.println(msg);
                }
            }

            List<Integer> s = statesList.get(currentState);

            // s must be an array of length 2, where the first element is a state in this, and the second element is a
            // state in the other Automaton.
            int p = s.get(0);
            int q = s.get(1);
            Int2ObjectRBTreeMap<IntList> thisStatesTransitions = new Int2ObjectRBTreeMap<>();
            N.getD().add(thisStatesTransitions);
            switch (op) {
                case "&":
                    N.getO().add((automaton.getO().getInt(p) != 0 && M.getO().getInt(q) != 0) ? 1 : 0);
                    break;
                case "|":
                    N.getO().add((automaton.getO().getInt(p) != 0 || M.getO().getInt(q) != 0) ? 1 : 0);
                    break;
                case "^":
                    N.getO().add(((automaton.getO().getInt(p) != 0 && M.getO().getInt(q) == 0) || (automaton.getO().getInt(p) == 0 && M.getO().getInt(q) != 0)) ? 1 : 0);
                    break;
                case "=>":
                    N.getO().add((automaton.getO().getInt(p) == 0 || M.getO().getInt(q) != 0) ? 1 : 0);
                    break;
                case "<=>":
                    N.getO().add(((automaton.getO().getInt(p) == 0 && M.getO().getInt(q) == 0) || (automaton.getO().getInt(p) != 0 && M.getO().getInt(q) != 0)) ? 1 : 0);
                    break;
                case "<":
                    N.getO().add((automaton.getO().getInt(p) < M.getO().getInt(q)) ? 1 : 0);
                    break;
                case ">":
                    N.getO().add((automaton.getO().getInt(p) > M.getO().getInt(q)) ? 1 : 0);
                    break;
                case "=":
                    N.getO().add((automaton.getO().getInt(p) == M.getO().getInt(q)) ? 1 : 0);
                    break;
                case "!=":
                    N.getO().add((automaton.getO().getInt(p) != M.getO().getInt(q)) ? 1 : 0);
                    break;
                case "<=":
                    N.getO().add((automaton.getO().getInt(p) <= M.getO().getInt(q)) ? 1 : 0);
                    break;
                case ">=":
                    N.getO().add((automaton.getO().getInt(p) >= M.getO().getInt(q)) ? 1 : 0);
                    break;
                case "+":
                    N.getO().add(automaton.getO().getInt(p) + M.getO().getInt(q));
                    break;
                case "-":
                    N.getO().add(automaton.getO().getInt(p) - M.getO().getInt(q));
                    break;
                case "*":
                    N.getO().add(automaton.getO().getInt(p) * M.getO().getInt(q));
                    break;
                case "/":
                    if (M.getO().getInt(q) == 0) throw ExceptionHelper.divisionByZero();
                    N.getO().add(Math.floorDiv(automaton.getO().getInt(p), M.getO().getInt(q)));
                    break;
                case "combine":
                    N.getO().add((M.getO().getInt(q) == 1) ? automaton.combineOutputs.getInt(automaton.combineIndex) : automaton.getO().getInt(p));
                    break;
                case "first":
                    N.getO().add(automaton.getO().getInt(p) == 0 ? M.getO().getInt(q) : automaton.getO().getInt(p));
                    break;
                case "if_other":
                    N.getO().add(M.getO().getInt(q) != 0 ? automaton.getO().getInt(p) : 0);
                    break;
            }

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
        if (print) {
            String msg = prefix + "computed cross product:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton and M.
     * @throws Exception
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
        if (print) {
            String msg = prefix + "computing &:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        Automaton N = crossProduct(automaton, M, "&", print, prefix, log);
        N.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed &:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton or M
     * @throws Exception
     */
    public static Automaton or(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) {
        if ((automaton.isTRUE_FALSE_AUTOMATON() && automaton.isTRUE_AUTOMATON()) || (M.isTRUE_FALSE_AUTOMATON() && M.isTRUE_AUTOMATON()))
            return new Automaton(true);
        if ((automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) && (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()))
            return new Automaton(false);

        if (automaton.isTRUE_FALSE_AUTOMATON() && !automaton.isTRUE_AUTOMATON()) return M;
        if (M.isTRUE_FALSE_AUTOMATON() && !M.isTRUE_AUTOMATON()) return automaton;

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing |:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        totalize(M, print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "|", print, prefix, log);

        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed |:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton xor M
     * @throws Exception
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
        if (print) {
            String msg = prefix + "computing ^:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        totalize(M, print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "^", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed ^:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton imply M
     * @throws Exception
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
        if (print) {
            String msg = prefix + "computing =>:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        totalize(M, print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "=>", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed =>:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return this automaton iff M
     * @throws Exception
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
        if (print) {
            String msg = prefix + "computing <=>:" + automaton.getQ() + " states - " + M.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        totalize(M, print, prefix + " ", log);
        Automaton N = crossProduct(automaton, M, "<=>", print, prefix + " ", log);
        N.minimize(null, print, prefix + " ", log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed <=>:" + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @return changes this automaton to its negation
     * @throws Exception
     */
    public static void not(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            automaton.setTRUE_AUTOMATON(!automaton.isTRUE_AUTOMATON());
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing ~:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        for (int q = 0; q < automaton.getQ(); q++)
            automaton.getO().set(q, automaton.getO().getInt(q) != 0 ? 0 : 1);

        automaton.minimize(null, print, prefix + " ", log);
        automaton.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed ~:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
     * @throws Exception
     */
    public static Automaton rightQuotient(Automaton automaton, Automaton other, boolean skipSubsetCheck, boolean print, String prefix, StringBuilder log) {

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "right quotient: " + automaton.getQ() + " state automaton with " + other.getQ() + " state automaton";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        if (!skipSubsetCheck) {
            // check whether the alphabet of other is a subset of the alphabet of self. If not, throw an error.
            if (!isSubsetA(other, automaton)) {
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

            if (I.isEmpty()) {
                M.getO().set(i, 0);
            } else {
                M.getO().set(i, 1);
            }
        }

        M.minimize(null, print, prefix, log);
        M.applyAllRepresentations();
        M.setCanonized(false);
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "right quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return M;
    }

    public static Automaton leftQuotient(Automaton automaton, Automaton other, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "left quotient: " + automaton.getQ() + " state automaton with " + other.getQ() + " state automaton";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        // check whether the alphabet of self is a subset of the alphabet of other. If not, throw an error.
        if (!isSubsetA(automaton, other)) {
            throw new RuntimeException("First automaton's alphabet must be a subset of the second automaton's alphabet for left quotient.");
        }

        Automaton M1 = automaton.clone();
        reverse(M1, print, prefix, log, true);
        M1.setCanonized(false);
        M1.canonize();

        Automaton M2 = other.clone();
        reverse(M2, print, prefix, log, true);
        M2.setCanonized(false);
        M2.canonize();

        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        reverse(M, print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "left quotient complete: " + M.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return M;
    }

    private static boolean isSubsetA(Automaton automaton, Automaton other) {
        boolean isSubset = true;

        if (automaton.getA().size() == other.getA().size()) {
            for (int i = 0; i < automaton.getA().size(); i++) {
                if (!other.getA().get(i).containsAll(automaton.getA().get(i))) {
                    isSubset = false;
                    break;
                }
            }
        } else {
            isSubset = false;
        }
        return isSubset;
    }

    /**
     * This method adds a dead state to totalize the transition function
     *
     * @throws Exception
     */
    static void totalize(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "totalizing:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        //we first check if the automaton is totalized
        boolean totalized = true;
        for (int q = 0; q < automaton.getQ(); q++) {
            for (int x = 0; x < automaton.getAlphabetSize(); x++) {
                if (!automaton.getD().get(q).containsKey(x)) {
                    IntList nullState = new IntArrayList();
                    nullState.add(automaton.getQ());
                    automaton.getD().get(q).put(x, nullState);
                    totalized = false;
                }
            }
        }
        if (!totalized) {
            automaton.getO().add(0);
            automaton.setQ(automaton.getQ() + 1);
            automaton.getD().add(new Int2ObjectRBTreeMap<>());
            for (int x = 0; x < automaton.getAlphabetSize(); x++) {
                IntList nullState = new IntArrayList();
                nullState.add(automaton.getQ() - 1);
                automaton.getD().get(automaton.getQ() - 1).put(x, nullState);
            }
        }

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "totalized:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    /**
     * Calculate new state output (O), from previous O and statesList.
     *
     * @param O          - previous O
     * @param statesList
     * @return new O
     */
    static IntList calculateNewStateOutput(IntList O, List<IntSet> statesList) {
        IntList newO = new IntArrayList();
        for (IntSet state : statesList) {
            boolean flag = false;
            for (int q : state) {
                if (O.getInt(q) != 0) {
                    newO.add(1);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                newO.add(0);
            }
        }
        return newO;
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
        if (print) {
            String msg = prefix + "fixing leading zeros:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        automaton.setCanonized(false);
        int zero = determineZero(automaton);
        if (!automaton.getD().get(automaton.getQ0()).containsKey(zero)) {
            automaton.getD().get(automaton.getQ0()).put(zero, new IntArrayList());
        }
        if (!automaton.getD().get(automaton.getQ0()).get(zero).contains(automaton.getQ0())) {
            automaton.getD().get(automaton.getQ0()).get(zero).add(automaton.getQ0());
        }

        IntSet initial_state = zeroReachableStates(automaton);
        List<Int2IntMap> newMemD = automaton.subsetConstruction(null, initial_state, print, prefix + " ", log);
        automaton.minimize(newMemD, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixed leading zeros:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    private static int determineZero(Automaton automaton) {
        List<Integer> ZERO = new ArrayList<>(automaton.getA().size());//all zero input
        for (List<Integer> i : automaton.getA()) ZERO.add(i.indexOf(0));
        return automaton.encode(ZERO);
    }

    public static void fixTrailingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixing trailing zeros:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        automaton.setCanonized(false);
        Set<Integer> newFinalStates = statesReachableToFinalStatesByZeros(automaton);
        for (int q : newFinalStates) {
            automaton.getO().set(q, 1);
        }

        automaton.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixed trailing zeros:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
     * @throws Exception
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
        if (print) {
            String msg = prefix + "removing leading zeroes for:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

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
        if (print) {
            String msg = prefix + "quantified:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
     * @throws Exception
     */
    private static Automaton removeLeadingZeroesHelper(Automaton automaton, int n, boolean print, String prefix, StringBuilder log) {
        if (n >= automaton.getA().size() || n < 0) {
            throw new RuntimeException("Cannot remove leading zeroes for the "
                    + (n + 1) + "-th input when automaton only has " + automaton.getA().size() + " inputs.");
        }

        if (automaton.getNS().get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.setQ(2);
        M.setQ0(0);
        M.getO().add(1);
        M.getO().add(1);
        M.getD().add(new Int2ObjectRBTreeMap<>());
        M.getD().add(new Int2ObjectRBTreeMap<>());
        M.setNS(automaton.getNS());
        M.setA(automaton.getA());
        M.setLabel(automaton.getLabel());
        M.setAlphabetSize(automaton.getAlphabetSize());
        M = M.clone();

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
     * Returns the set of states reachable from the initial state by reading 0*
     *
     * @param automaton
     */
    private static IntSet zeroReachableStates(Automaton automaton) {
        IntSet result = new IntOpenHashSet();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(automaton.getQ0());
        int zero = determineZero(automaton);
        while (!queue.isEmpty()) {
            int q = queue.poll();
            result.add(q);
            if (automaton.getD().get(q).containsKey(zero))
                for (int p : automaton.getD().get(q).get(zero))
                    if (!result.contains(p))
                        queue.add(p);
        }
        return result;
    }

    /**
     * So for example if f is a final state and f is reachable from q by reading 0*
     * then q will be in the resulting set of this method.
     *
     * @param automaton
     * @return
     */
    private static Set<Integer> statesReachableToFinalStatesByZeros(Automaton automaton) {
        Set<Integer> result = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        int zero = determineZero(automaton);
        //this is the adjacency matrix of the reverse of the transition graph of this automaton on 0
        List<List<Integer>> adjacencyList = new ArrayList<>();
        for (int q = 0; q < automaton.getQ(); q++) adjacencyList.add(new ArrayList<>());
        for (int q = 0; q < automaton.getQ(); q++) {
            if (automaton.getD().get(q).containsKey(zero)) {
                List<Integer> destination = automaton.getD().get(q).get(zero);
                for (int p : destination) {
                    adjacencyList.get(p).add(q);
                }
            }
            if (automaton.getO().getInt(q) != 0) queue.add(q);
        }
        while (!queue.isEmpty()) {
            int q = queue.poll();
            result.add(q);
            for (int p : adjacencyList.get(q))
                if (!result.contains(p))
                    queue.add(p);
        }
        return result;
    }

    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately. So for example an
     * expression like f(a,a) becomes
     * an automaton with one input. After we are done with input i, we call removeSameInputs(i+1)
     *
     * @param automaton
     * @param i
     * @throws Exception
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
                    if (currentStatesTransition.containsKey(m))
                        currentStatesTransition.get(m).addAll(automaton.getD().get(q).get(n));
                    else
                        currentStatesTransition.put(m, new IntArrayList(automaton.getD().get(q).get(n)));
                }
            }
        }
        automaton.setD(new_d);
        I.remove(0);
        automaton.setA(newAlphabet);
        UtilityMethods.removeIndices(automaton.getNS(), I);
        automaton.setEncoder(null);
        automaton.setAlphabetSize(1);
        for (List<Integer> x : automaton.getA())
            automaton.setAlphabetSize(automaton.getAlphabetSize() * x.size());
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
        if (print) {
            String msg = prefix + "applying operator (" + operator + "):" + automaton.getQ() + " states - " + W.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimizeWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "applied operator (" + operator + "):" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
     * This method is very similar to public void quantify(Set<String> listOfLabelsToQuantify,boolean leadingZeros)throws Exception
     * with the exception that, this method does not deal with leading/trailing zeros problem.
     *
     * @param automaton
     * @param listOfLabelsToQuantify
     * @throws Exception
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
        if (print) {
            String msg = prefix + "quantifying:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

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
        automaton.setAlphabetSize(1);
        for (List<Integer> x : automaton.getA())
            automaton.setAlphabetSize(automaton.getAlphabetSize() * x.size());
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
        if (print) {
            String msg = prefix + "quantified:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    /**
     * this automaton should not be a word automaton (automaton with output). However, it can be non deterministic.
     * Enabling the reverseMsd flag will flip the number system of the automaton from msd to lsd, and vice versa.
     * Note that reversing the Msd will also call this function as reversals are done in the NumberSystem class upon
     * initializing.
     *
     * @return the reverse of this automaton
     * @throws Exception
     */
    public static void reverse(Automaton automaton, boolean print, String prefix, StringBuilder log, boolean reverseMsd, boolean skipMinimize) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "Reversing:" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        // We change the direction of transitions first.
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.getQ(); q++) new_d.add(new Int2ObjectRBTreeMap<>());
        for (int q = 0; q < automaton.getQ(); q++) {
            for (int x : automaton.getD().get(q).keySet()) {
                for (int dest : automaton.getD().get(q).get(x)) {
                    if (new_d.get(dest).containsKey(x))
                        new_d.get(dest).get(x).add(q);
                    else {
                        IntList destinationSet = new IntArrayList();
                        destinationSet.add(q);
                        new_d.get(dest).put(x, destinationSet);
                    }
                }
            }
        }
        automaton.setD(new_d);
        IntSet setOfFinalStates = new IntOpenHashSet();
        /**final states become non final*/
        for (int q = 0; q < automaton.getQ(); q++) {
            if (automaton.getO().getInt(q) != 0) {
                setOfFinalStates.add(q);
                automaton.getO().set(q, 0);
            }
        }
        automaton.getO().set(automaton.getQ0(), 1);/**initial state becomes the final state.*/

        List<Int2IntMap> newMemD = automaton.subsetConstruction(null, setOfFinalStates, print, prefix + " ", log);

        if (!skipMinimize) {
            automaton.minimize(newMemD, print, prefix + " ", log);
        }

        if (reverseMsd) {
            flipNS(automaton);
        }

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "reversed:" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    // flip the number system from msd to lsd and vice versa.
    private static void flipNS(Automaton automaton) {
        for (int i = 0; i < automaton.getNS().size(); i++) {
            if (automaton.getNS().get(i) == null) {
                continue;
            }
            int indexOfUnderscore = automaton.getNS().get(i).getName().indexOf("_");
            String msd_or_lsd = automaton.getNS().get(i).getName().substring(0, indexOfUnderscore);
            String suffix = automaton.getNS().get(i).getName().substring(indexOfUnderscore);
            String newName = (msd_or_lsd.equals("msd") ? "lsd" : "msd") + suffix;
            automaton.getNS().set(i, new NumberSystem(newName));
        }
    }

    public static void reverse(Automaton automaton, boolean print, String prefix, StringBuilder log, boolean reverseMsd) {
        reverse(automaton, print, prefix, log, reverseMsd, false);
    }

    public static void reverse(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        reverse(automaton, print, prefix, log, false);
    }

    /**
     * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
     *
     * @throws Exception
     */
    public static void reverseWithOutput(Automaton automaton, boolean reverseMsd, boolean print, String prefix, StringBuilder log) {
        if (automaton.isTRUE_FALSE_AUTOMATON()) {
            return;
        }
        try {
            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "reversing: " + automaton.getQ() + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            boolean addedDeadState = automaton.addDistinguishedDeadState(print, prefix, log);

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
                flipNS(automaton);
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
            if (print) {
                String msg = prefix + "reversed: " + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reversing word automaton");
        }
    }

    /**
     * Convert the number system of a word automaton from [msd/lsd]_k^i to [msd/lsd]_k^j.
     *
     * @throws Exception
     */
    public static void convertNS(Automaton automaton, boolean toMsd, int toBase, boolean print,
                                 String prefix, StringBuilder log) {
        try {
            // assume that the format of ns is in the form msd_k or lsd_k. let the Prover.java check it with a regex.

            if (automaton.getNS().size() != 1) {
                throw new RuntimeException("Automaton must have one input to be converted.");
            }

            String nsName = automaton.getNS().get(0).getName();

            String base = nsName.substring(nsName.indexOf("_") + 1);

            if (!UtilityMethods.isNumber(base) || Integer.parseInt(base) <= 1) {
                throw new RuntimeException("Base of number system of original automaton must be a positive integer greater than 1.");
            }

            int fromBase = Integer.parseInt(base);

            if (fromBase == toBase) {
                if (automaton.getNS().get(0).isMsd() == toMsd) {
                    throw new RuntimeException("New and old number systems " + automaton.getNS().get(0).getName() +
                            " to be converted cannot be equal.");
                } else {
                    // if all that differs is msd/lsd, just reverse the automaton.
                    reverseWithOutput(automaton, true, print, prefix + " ", log);
                    return;
                }
            }

            // check that the bases are powers of the same integer, and figure out that integer.
            // make a different function for this.
            int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);

            if (commonRoot == -1) {
                throw new RuntimeException("New and old number systems must have bases of the form k^i and k^j for integers i, j, k.");
            }

            // if originally in lsd, reverse and convert to msd.
            if (!automaton.getNS().get(0).isMsd()) {
                reverseWithOutput(automaton, true, print, prefix + " ", log);
            }

            // run  convert algorithm assuming MSD.

            boolean currentlyReversed = false;

            if (fromBase != commonRoot) {
                // do k^i to k construction.

                int exponent = (int) (Math.log(fromBase) / Math.log(commonRoot));

                reverseWithOutput(automaton, true, print, prefix + " ", log);

                currentlyReversed = true;

                convertLsdBaseToRoot(automaton, commonRoot, exponent, print, prefix + " ", log);

                automaton.minimizeSelfWithOutput(print, prefix + " ", log);

                // reverseWithOutput(true, print, prefix+" ", log);
            }

            if (toBase != commonRoot) {
                // do k to k^i construction.

                if (currentlyReversed) {
                    reverseWithOutput(automaton, true, print, prefix + " ", log);
                    currentlyReversed = false;
                }

                int exponent = (int) (Math.log(toBase) / Math.log(commonRoot));

                convertMsdBaseToExponent(automaton, exponent, print, prefix + " ", log);

                automaton.minimizeSelfWithOutput(print, prefix + " ", log);
            }

            // if desired base is in lsd, reverse again to lsd.
            if (toMsd == currentlyReversed) {
                reverseWithOutput(automaton, true, print, prefix + " ", log);
                currentlyReversed = !currentlyReversed;
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting the number system of an automaton");
        }
    }

    /**
     * Assuming this automaton is in number system msd_k with one input, convert it to number system msd_k^j with one
     * input.
     * Used as a helper method in convert()
     *
     * @param automaton
     * @param exponent
     * @throws Exception
     */
    private static void convertMsdBaseToExponent(Automaton automaton, int exponent, boolean print,
                                                 String prefix, StringBuilder log) {
        try {

            String nsName = automaton.getNS().get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: msd_" + base + " to msd_" + (int) Math.pow(base, exponent) + ", " + automaton.getQ() + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

            // need to generate the new morphism, which is h^{exponent}, where h is the original morphism.

            List<List<Integer>> prevMorphism = new ArrayList<>();

            for (int q = 0; q < automaton.getQ(); q++) {
                List<Integer> morphismString = new ArrayList<>();

                if (automaton.getD().get(q).keySet().size() != automaton.getAlphabetSize()) {
                    throw new RuntimeException("Automaton must be deterministic");
                }

                for (int di = 0; di < automaton.getAlphabetSize(); di++) {
                    morphismString.add(automaton.getD().get(q).get(di).getInt(0));
                }
                prevMorphism.add(morphismString);
            }

            for (int i = 2; i <= exponent; i++) {

                List<List<Integer>> newMorphism = new ArrayList<>();

                for (int j = 0; j < automaton.getQ(); j++) {
                    List<Integer> morphismString = new ArrayList<>();
                    for (int k = 0; k < prevMorphism.get(j).size(); k++) {
                        for (int di : automaton.getD().get(j).keySet()) {
                            morphismString.add(automaton.getD().get(prevMorphism.get(j).get(k)).get(di).getInt(0));
                        }
                    }
                    newMorphism.add(morphismString);
                }
                prevMorphism = new ArrayList<>(newMorphism);
            }

            for (int q = 0; q < automaton.getQ(); q++) {
                newD.add(new Int2ObjectRBTreeMap<>());
                for (int di = 0; di < prevMorphism.get(q).size(); di++) {

                    int toState = prevMorphism.get(q).get(di);

                    // set up transition
                    IntList newList = new IntArrayList();
                    newList.add(toState);
                    newD.get(q).put(di, newList);
                }
            }

            automaton.setD(newD);

            // change number system too.

            automaton.getNS().set(0, new NumberSystem("msd_" + (int) (Math.pow(base, exponent))));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < (int) (Math.pow(base, exponent)); i++) {
                ints.add(i);
            }
            automaton.setA(List.of(ints));
            automaton.setAlphabetSize(ints.size());

            if (print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + "Converted: msd_" + base + " to msd_" + (int) (Math.pow(base, exponent)) + ", " + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting the number system msd_k of an automaton to msd_k^j");
        }

    }

    /**
     * Assuming this automaton is in number system lsd_k^j with one input, convert it to number system lsd_k with one
     * input.
     * Used as a helper method in convert()
     *
     * @param automaton
     * @param exponent
     * @throws Exception
     */
    private static void convertLsdBaseToRoot(Automaton automaton, int root, int exponent, boolean print,
                                             String prefix, StringBuilder log) {

        try {
            String nsName = automaton.getNS().get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: lsd_" + base + " to lsd_" + (int) Math.pow(root, exponent) + ", " + automaton.getQ() + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            if (base != Math.pow(root, exponent)) {
                throw new RuntimeException("Base of automaton must be equal to the given root to the power of the given exponent.");
            }

            class StateTuple {
                final int state;
                final List<Integer> string;

                StateTuple(int state, List<Integer> string) {
                    this.state = state;
                    this.string = string;
                }

                @Override
                public boolean equals(Object o) {

                    // DO NOT compare the string.
                    if (this == o) {
                        return true;
                    }
                    if (o == null || this.getClass() != o.getClass()) {
                        return false;
                    }
                    StateTuple other = (StateTuple) o;
                    return this.state == other.state && this.string.equals(other.string);
                }

                @Override
                public int hashCode() {
                    int result = this.state ^ (this.state >>> 32);
                    result = 31 * result + this.string.hashCode();
                    return result;
                }
            }

            ArrayList<StateTuple> newStates = new ArrayList<>();

            Queue<StateTuple> newStatesQueue = new LinkedList<>();

            HashMap<StateTuple, Integer> newStatesHash = new HashMap<>();

            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

            IntList newO = new IntArrayList();

            StateTuple initState = new StateTuple(automaton.getQ0(), List.of());

            newStates.add(initState);
            newStatesQueue.add(initState);
            newStatesHash.put(initState, newStates.size() - 1);


            while (!newStatesQueue.isEmpty()) {
                StateTuple currState = newStatesQueue.remove();

                newD.add(new Int2ObjectRBTreeMap<>());

                if (currState.string.isEmpty()) {
                    newO.add(automaton.getO().getInt(currState.state));
                } else {
                    int stringValue = 0;

                    for (int i = 0; i < currState.string.size(); i++) {
                        stringValue += currState.string.get(i) * (int) (Math.pow(root, i));
                    }

                    // set up output
                    newO.add(automaton.getO().getInt(automaton.getD().get(currState.state).get(stringValue).getInt(0)));
                }

                for (int di = 0; di < root; di++) {

                    StateTuple toState;

                    List<Integer> toStateString = new ArrayList<>(currState.string);

                    toStateString.add(di);

                    if (currState.string.size() < exponent - 1) {
                        toState = new StateTuple(currState.state, toStateString);
                    } else {

                        int toStateStringValue = 0;

                        for (int i = 0; i < toStateString.size(); i++) {
                            toStateStringValue += toStateString.get(i) * (int) (Math.pow(root, i));
                        }

                        toState = new StateTuple(automaton.getD().get(currState.state).get(toStateStringValue).getInt(0), List.of());
                    }


                    if (!newStatesHash.containsKey(toState)) {
                        newStates.add(toState);
                        newStatesQueue.add(toState);
                        newStatesHash.put(toState, newStates.size() - 1);
                    }

                    IntList newList = new IntArrayList();
                    newList.add((int) newStatesHash.get(toState));
                    newD.get(newD.size() - 1).put(di, newList);

                }
            }

            automaton.setQ(newStates.size());

            automaton.setO(newO);

            automaton.setD(newD);

            automaton.setCanonized(false);

            // change number system too.
            automaton.getNS().set(0, new NumberSystem("lsd_" + root));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < root; i++) {
                ints.add(i);
            }
            automaton.setA(List.of(ints));
            automaton.setAlphabetSize(ints.size());

            if (print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + prefix + "Converted: lsd_" + base + " to lsd_" + (int) (Math.pow(root, exponent)) + ", " + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting the number system msd_k^j of an automaton to msd_k");
        }


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
            if (print) {
                String msg = prefix + "computing =>:" + first.getQ() + " states - " + next.getQ() + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            // crossProduct requires labelling; make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.setLabel(first.getLabel());
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            totalize(first, print, prefix + " ", log);
            totalize(next, print, prefix + " ", log);
            Automaton product = crossProduct(first, next, "combine", print, prefix + " ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "computed =>:" + first.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }
        }

        // totalize the resulting automaton
        totalize(first, print, prefix + " ", log);
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
     * @throws Exception
     */
    public static Automaton compare(
            Automaton automaton, Automaton W, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "comparing (" + operator + "):" + automaton.getQ() + " states - " + W.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "compared (" + operator + "):" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
     * @throws Exception
     */
    public static void compare(
            Automaton automaton, int o, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "comparing (" + operator + ") against " + o + ":" + automaton.getQ() + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        for (int p = 0; p < automaton.getQ(); p++) {
            switch (operator) {
                case "<":
                    automaton.getO().set(p, (automaton.getO().getInt(p) < o) ? 1 : 0);
                    break;
                case ">":
                    automaton.getO().set(p, (automaton.getO().getInt(p) > o) ? 1 : 0);
                    break;
                case "=":
                    automaton.getO().set(p, (automaton.getO().getInt(p) == o) ? 1 : 0);
                    break;
                case "!=":
                    automaton.getO().set(p, (automaton.getO().getInt(p) != o) ? 1 : 0);
                    break;
                case "<=":
                    automaton.getO().set(p, (automaton.getO().getInt(p) <= o) ? 1 : 0);
                    break;
                case ">=":
                    automaton.getO().set(p, (automaton.getO().getInt(p) >= o) ? 1 : 0);
                    break;
            }
        }
        automaton.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "compared (" + operator + ") against " + o + ":" + automaton.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
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
