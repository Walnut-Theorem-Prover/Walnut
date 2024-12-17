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

        if (automaton.TRUE_FALSE_AUTOMATON || M.TRUE_FALSE_AUTOMATON) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method cannot be true or false automata.");
        }

        if (automaton.label == null ||
                M.label == null ||
                automaton.label.size() != automaton.A.size() ||
                M.label.size() != M.A.size()
        ) {
            throw new RuntimeException("Invalid use of the crossProduct method: " +
                    "the automata for this method must have labeled inputs.");
        }

        /**N is going to hold the cross product*/
        Automaton N = new Automaton();

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "Computing cross product:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        /**
         * for example when sameLabelsInMAndThis[2] = 3, then input 2 of M has the same label as input 3 of this
         * and when sameLabelsInMAndThis[2] = -1, it means that input 2 of M is not an input of this
         */
        int[] sameInputsInMAndThis = new int[M.A.size()];
        for (int i = 0; i < M.label.size(); i++) {
            sameInputsInMAndThis[i] = -1;
            if (automaton.label.contains(M.label.get(i))) {
                int j = automaton.label.indexOf(M.label.get(i));
                if (!UtilityMethods.areEqual(automaton.A.get(j), M.A.get(i))) {
                    throw new RuntimeException("in computing cross product of two automaton, "
                            + "variables with the same label must have the same alphabet");
                }
                sameInputsInMAndThis[i] = j;
            }
        }
        for (int i = 0; i < automaton.A.size(); i++) {
            N.A.add(automaton.A.get(i));
            N.label.add(automaton.label.get(i));
            N.NS.add(automaton.NS.get(i));
        }
        for (int i = 0; i < M.A.size(); i++) {
            if (sameInputsInMAndThis[i] == -1) {
                N.A.add(new ArrayList<>(M.A.get(i)));
                N.label.add(M.label.get(i));
                N.NS.add(M.NS.get(i));
            } else {
                int j = sameInputsInMAndThis[i];
                if (M.NS.get(i) != null && N.NS.get(j) == null) {
                    N.NS.set(j, M.NS.get(i));
                }

            }
        }
        N.alphabetSize = 1;
        for (List<Integer> i : N.A) {
            N.alphabetSize *= i.size();
        }

        List<Integer> allInputsOfN = new ArrayList<>();
        for (int i = 0; i < automaton.alphabetSize; i++) {
            for (int j = 0; j < M.alphabetSize; j++) {
                List<Integer> inputForN = joinTwoInputsForCrossProduct(Automaton.decode(automaton, i), Automaton.decode(M, j), sameInputsInMAndThis);
                if (inputForN == null)
                    allInputsOfN.add(-1);
                else
                    allInputsOfN.add(N.encode(inputForN));
            }
        }
        ArrayList<List<Integer>> statesList = new ArrayList<>();
        Map<List<Integer>, Integer> statesHash = new HashMap<>();
        N.q0 = 0;
        statesList.add(Arrays.asList(automaton.q0, M.q0));
        statesHash.put(Arrays.asList(automaton.q0, M.q0), 0);
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
            N.d.add(thisStatesTransitions);
            switch (op) {
                case "&":
                    N.O.add((automaton.O.getInt(p) != 0 && M.O.getInt(q) != 0) ? 1 : 0);
                    break;
                case "|":
                    N.O.add((automaton.O.getInt(p) != 0 || M.O.getInt(q) != 0) ? 1 : 0);
                    break;
                case "^":
                    N.O.add(((automaton.O.getInt(p) != 0 && M.O.getInt(q) == 0) || (automaton.O.getInt(p) == 0 && M.O.getInt(q) != 0)) ? 1 : 0);
                    break;
                case "=>":
                    N.O.add((automaton.O.getInt(p) == 0 || M.O.getInt(q) != 0) ? 1 : 0);
                    break;
                case "<=>":
                    N.O.add(((automaton.O.getInt(p) == 0 && M.O.getInt(q) == 0) || (automaton.O.getInt(p) != 0 && M.O.getInt(q) != 0)) ? 1 : 0);
                    break;
                case "<":
                    N.O.add((automaton.O.getInt(p) < M.O.getInt(q)) ? 1 : 0);
                    break;
                case ">":
                    N.O.add((automaton.O.getInt(p) > M.O.getInt(q)) ? 1 : 0);
                    break;
                case "=":
                    N.O.add((automaton.O.getInt(p) == M.O.getInt(q)) ? 1 : 0);
                    break;
                case "!=":
                    N.O.add((automaton.O.getInt(p) != M.O.getInt(q)) ? 1 : 0);
                    break;
                case "<=":
                    N.O.add((automaton.O.getInt(p) <= M.O.getInt(q)) ? 1 : 0);
                    break;
                case ">=":
                    N.O.add((automaton.O.getInt(p) >= M.O.getInt(q)) ? 1 : 0);
                    break;
                case "+":
                    N.O.add(automaton.O.getInt(p) + M.O.getInt(q));
                    break;
                case "-":
                    N.O.add(automaton.O.getInt(p) - M.O.getInt(q));
                    break;
                case "*":
                    N.O.add(automaton.O.getInt(p) * M.O.getInt(q));
                    break;
                case "/":
                    if (M.O.getInt(q) == 0) throw ExceptionHelper.divisionByZero();
                    N.O.add(Math.floorDiv(automaton.O.getInt(p), M.O.getInt(q)));
                    break;
                case "combine":
                    N.O.add((M.O.getInt(q) == 1) ? automaton.combineOutputs.getInt(automaton.combineIndex) : automaton.O.getInt(p));
                    break;
                case "first":
                    N.O.add(automaton.O.getInt(p) == 0 ? M.O.getInt(q) : automaton.O.getInt(p));
                    break;
                case "if_other":
                    N.O.add(M.O.getInt(q) != 0 ? automaton.O.getInt(p) : 0);
                    break;
            }

            for (int x : automaton.d.get(p).keySet()) {
                for (int y : M.d.get(q).keySet()) {
                    int z = allInputsOfN.get(x * M.alphabetSize + y);
                    if (z != -1) {
                        IntList dest = new IntArrayList();
                        thisStatesTransitions.put(z, dest);
                        for (int dest1 : automaton.d.get(p).get(x)) {
                            for (int dest2 : M.d.get(q).get(y)) {
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
        N.Q = statesList.size();
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed cross product:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if ((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) &&
                (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) {
            return new Automaton(true);
        }

        if ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) ||
                (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) {
            return new Automaton(false);
        }

        if (automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) {
            return M;
        }

        if (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON) {
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing &:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        Automaton N = crossProduct(automaton, M, "&", print, prefix, log);
        N.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed &:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if ((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) || (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON))
            return new Automaton(true);
        if ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON))
            return new Automaton(false);

        if (automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) return M;
        if (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON) return automaton;

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing |:" + automaton.Q + " states - " + M.Q + " states";
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
            String msg = prefix + "computed |:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if ((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON))
            return new Automaton(true);
        if ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON))
            return new Automaton(true);
        if ((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON))
            return new Automaton(false);
        if ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON))
            return new Automaton(false);

        if (automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) return M;
        if (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON) return automaton;

        if (automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) {
            not(M, print, prefix, log);
            return M;
        }
        if (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing ^:" + automaton.Q + " states - " + M.Q + " states";
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
            String msg = prefix + "computed ^:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if ((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON))
            return new Automaton(false);
        if ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) || (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON))
            return new Automaton(true);
        if (automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) return M;
        if (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing =>:" + automaton.Q + " states - " + M.Q + " states";
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
            String msg = prefix + "computed =>:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) ||
                ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)))
            return new Automaton(true);
        if (((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) ||
                ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)))
            return new Automaton(false);

        if (automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) return M;
        if (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON) return automaton;
        if (automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) {
            not(M, print, prefix, log);
            return M;
        }
        if (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON) {
            not(automaton, print, prefix, log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing <=>:" + automaton.Q + " states - " + M.Q + " states";
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
            String msg = prefix + "computed <=>:" + N.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (automaton.TRUE_FALSE_AUTOMATON) {
            automaton.TRUE_AUTOMATON = !automaton.TRUE_AUTOMATON;
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computing ~:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        totalize(automaton, print, prefix + " ", log);
        for (int q = 0; q < automaton.Q; q++)
            automaton.O.set(q, automaton.O.getInt(q) != 0 ? 0 : 1);

        automaton.minimize(null, print, prefix + " ", log);
        automaton.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "computed ~:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
            String msg = prefix + "right quotient: " + automaton.Q + " state automaton with " + other.Q + " state automaton";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        if (!skipSubsetCheck) {
            // check whether the alphabet of other is a subset of the alphabet of self. If not, throw an error.
            boolean isSubset = true;

            if (automaton.A.size() == other.A.size()) {
                for (int i = 0; i < automaton.A.size(); i++) {
                    if (!automaton.A.get(i).containsAll(other.A.get(i))) {
                        isSubset = false;
                        break;
                    }
                }
            } else {
                isSubset = false;
            }

            if (!isSubset) {
                throw new RuntimeException("Second automaton's alphabet must be a subset of the first automaton's alphabet for right quotient.");
            }
        }

        // The returned automaton will have the same states and transition function as this automaton, but
        // the final states will be different.
        Automaton M = automaton.clone();

        Automaton otherClone = other.clone();

        List<Int2ObjectRBTreeMap<IntList>> newOtherD = new ArrayList<>();

        for (int q = 0; q < otherClone.Q; q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (int x : otherClone.d.get(q).keySet()) {
                newMap.put(automaton.encode(Automaton.decode(otherClone, x)), otherClone.d.get(q).get(x));
            }
            newOtherD.add(newMap);
        }
        otherClone.d = newOtherD;
        otherClone.encoder = automaton.encoder;
        otherClone.A = automaton.A;
        otherClone.alphabetSize = automaton.alphabetSize;
        otherClone.NS = automaton.NS;

        for (int i = 0; i < automaton.Q; i++) {
            // this will be a temporary automaton that will be the same as as self except it will start from the automaton
            Automaton T = automaton.clone();

            if (i != 0) {
                T.q0 = i;
                T.canonized = false;
                T.canonize();
            }

            // need to have the same label for cross product (including "and")
            T.randomLabel();
            otherClone.label = T.label;

            Automaton I = and(T, otherClone, print, prefix, log);

            if (I.isEmpty()) {
                M.O.set(i, 0);
            } else {
                M.O.set(i, 1);
            }
        }

        M.minimize(null, print, prefix, log);
        M.applyAllRepresentations();
        M.canonized = false;
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "right quotient complete: " + M.Q + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return M;
    }

    public static Automaton leftQuotient(Automaton automaton, Automaton other, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "left quotient: " + automaton.Q + " state automaton with " + other.Q + " state automaton";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        // check whether the alphabet of self is a subset of the alphabet of other. If not, throw an error.
        boolean isSubset = true;

        if (automaton.A.size() == other.A.size()) {
            for (int i = 0; i < automaton.A.size(); i++) {
                if (!other.A.get(i).containsAll(automaton.A.get(i))) {
                    isSubset = false;
                    break;
                }
            }
        } else {
            isSubset = false;
        }

        if (!isSubset) {
            throw new RuntimeException("First automaton's alphabet must be a subset of the second automaton's alphabet for left quotient.");
        }

        Automaton M1 = automaton.clone();
        reverse(M1, print, prefix, log, true);
        M1.canonized = false;
        M1.canonize();

        Automaton M2 = other.clone();
        reverse(M2, print, prefix, log, true);
        M2.canonized = false;
        M2.canonize();

        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        reverse(M, print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "left quotient complete: " + M.Q + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        return M;
    }

    /**
     * This method adds a dead state to totalize the transition function
     *
     * @throws Exception
     */
    static void totalize(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "totalizing:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        //we first check if the automaton is totalized
        boolean totalized = true;
        for (int q = 0; q < automaton.Q; q++) {
            for (int x = 0; x < automaton.alphabetSize; x++) {
                if (!automaton.d.get(q).containsKey(x)) {
                    IntList nullState = new IntArrayList();
                    nullState.add(automaton.Q);
                    automaton.d.get(q).put(x, nullState);
                    totalized = false;
                }
            }
        }
        if (!totalized) {
            automaton.O.add(0);
            automaton.Q++;
            automaton.d.add(new Int2ObjectRBTreeMap<>());
            for (int x = 0; x < automaton.alphabetSize; x++) {
                IntList nullState = new IntArrayList();
                nullState.add(automaton.Q - 1);
                automaton.d.get(automaton.Q - 1).put(x, nullState);
            }
        }

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "totalized:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (automaton.TRUE_FALSE_AUTOMATON) return;
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixing leading zeros:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        automaton.canonized = false;
        int zero = determineZero(automaton);
        if (!automaton.d.get(automaton.q0).containsKey(zero)) {
            automaton.d.get(automaton.q0).put(zero, new IntArrayList());
        }
        if (!automaton.d.get(automaton.q0).get(zero).contains(automaton.q0)) {
            automaton.d.get(automaton.q0).get(zero).add(automaton.q0);
        }

        IntSet initial_state = zeroReachableStates(automaton);
        List<Int2IntMap> newMemD = automaton.subsetConstruction(null, initial_state, print, prefix + " ", log);
        automaton.minimize(newMemD, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixed leading zeros:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    private static int determineZero(Automaton automaton) {
        List<Integer> ZERO = new ArrayList<>(automaton.A.size());//all zero input
        for (List<Integer> i : automaton.A) ZERO.add(i.indexOf(0));
        return automaton.encode(ZERO);
    }

    public static void fixTrailingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixing trailing zeros:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        automaton.canonized = false;
        Set<Integer> newFinalStates = statesReachableToFinalStatesByZeros(automaton);
        for (int q : newFinalStates) {
            automaton.O.set(q, 1);
        }

        automaton.minimize(null, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "fixed trailing zeros:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
            if (!automaton.label.contains(s)) {
                throw new RuntimeException("Variable " + s + " in the list of quantified variables is not a free variable.");
            }
        }
        if (listOfLabels.isEmpty()) {
            return automaton.clone();
        }
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "removing leading zeroes for:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        List<Integer> listOfInputs = new ArrayList<>();//extract the list of indices of inputs from the list of labels
        for (String l : listOfLabels) {
            listOfInputs.add(automaton.label.indexOf(l));
        }
        Automaton M = new Automaton(false);
        for (int n : listOfInputs) {
            Automaton N = removeLeadingZeroesHelper(automaton, n, print, prefix + " ", log);
            M = or(M, N, print, prefix + " ", log);
        }
        M = and(automaton, M, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "quantified:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (n >= automaton.A.size() || n < 0) {
            throw new RuntimeException("Cannot remove leading zeroes for the "
                    + (n + 1) + "-th input when automaton only has " + automaton.A.size() + " inputs.");
        }

        if (automaton.NS.get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.Q = 2;
        M.q0 = 0;
        M.O.add(1);
        M.O.add(1);
        M.d.add(new Int2ObjectRBTreeMap<>());
        M.d.add(new Int2ObjectRBTreeMap<>());
        M.NS = automaton.NS;
        M.A = automaton.A;
        M.label = automaton.label;
        M.alphabetSize = automaton.alphabetSize;
        M = M.clone();

        IntList dest = new IntArrayList();
        dest.add(1);
        for (int i = 0; i < automaton.alphabetSize; i++) {
            List<Integer> list = Automaton.decode(automaton, i);
            if (list.get(n) != 0) {
                M.d.get(0).put(i, new IntArrayList(dest));
            }
            M.d.get(1).put(i, new IntArrayList(dest));
        }
        if (!automaton.NS.get(n).isMsd()) {
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
        queue.add(automaton.q0);
        int zero = determineZero(automaton);
        while (!queue.isEmpty()) {
            int q = queue.poll();
            result.add(q);
            if (automaton.d.get(q).containsKey(zero))
                for (int p : automaton.d.get(q).get(zero))
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
        for (int q = 0; q < automaton.Q; q++) adjacencyList.add(new ArrayList<>());
        for (int q = 0; q < automaton.Q; q++) {
            if (automaton.d.get(q).containsKey(zero)) {
                List<Integer> destination = automaton.d.get(q).get(zero);
                for (int p : destination) {
                    adjacencyList.get(p).add(q);
                }
            }
            if (automaton.O.getInt(q) != 0) queue.add(q);
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
        if (i >= automaton.A.size()) return;
        List<Integer> I = new ArrayList<>();
        I.add(i);
        for (int j = i + 1; j < automaton.A.size(); j++) {
            if (automaton.label.get(i).equals(automaton.label.get(j))) {
                if (!UtilityMethods.areEqual(automaton.A.get(i), automaton.A.get(j))) {
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
        for (int i = 0; i < automaton.A.size(); i++)
            if (!I.contains(i) || I.indexOf(i) == 0)
                newAlphabet.add(new ArrayList<>(automaton.A.get(i)));
        for (int i = 0; i < newAlphabet.size() - 1; i++)
            newEncoder.add(newEncoder.get(i) * newAlphabet.get(i).size());
        List<Integer> map = new ArrayList<>();
        for (int n = 0; n < automaton.alphabetSize; n++)
            map.add(automaton.mapToReducedEncodedInput(n, I, newEncoder, newAlphabet));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.Q; q++) {
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            new_d.add(currentStatesTransition);
            for (int n : automaton.d.get(q).keySet()) {
                int m = map.get(n);
                if (m != -1) {
                    if (currentStatesTransition.containsKey(m))
                        currentStatesTransition.get(m).addAll(automaton.d.get(q).get(n));
                    else
                        currentStatesTransition.put(m, new IntArrayList(automaton.d.get(q).get(n)));
                }
            }
        }
        automaton.d = new_d;
        I.remove(0);
        automaton.A = newAlphabet;
        UtilityMethods.removeIndices(automaton.NS, I);
        automaton.encoder = null;
        automaton.alphabetSize = 1;
        for (List<Integer> x : automaton.A)
            automaton.alphabetSize *= x.size();
        UtilityMethods.removeIndices(automaton.label, I);
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
            String msg = prefix + "applying operator (" + operator + "):" + automaton.Q + " states - " + W.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimizeWithOutput(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "applied operator (" + operator + "):" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (automaton.TRUE_FALSE_AUTOMATON) return;

        boolean isMsd = true;
        boolean flag = false;
        for (NumberSystem ns : automaton.NS) {
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
        if (listOfLabelsToQuantify.isEmpty() || automaton.label == null) {
            return;
        }

        String name_of_labels = "";
        for (String s : listOfLabelsToQuantify) {
            if (!automaton.label.contains(s)) {
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
            String msg = prefix + "quantifying:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        /**
         * If this is the case, then the quantified automaton is either the true or false automaton.
         * It is true if this's language is not empty.
         */
        if (listOfLabelsToQuantify.size() == automaton.A.size()) {
            automaton.TRUE_AUTOMATON = !automaton.isEmpty();
            automaton.TRUE_FALSE_AUTOMATON = true;
            automaton.clear();
            return;
        }

        List<Integer> listOfInputsToQuantify = new ArrayList<>();//extract the list of indices of inputs we would like to quantify
        for (String l : listOfLabelsToQuantify)
            listOfInputsToQuantify.add(automaton.label.indexOf(l));
        List<List<Integer>> allInputs = new ArrayList<>();
        for (int i = 0; i < automaton.alphabetSize; i++)
            allInputs.add(Automaton.decode(automaton, i));
        //now we remove those indices in listOfInputsToQuantify from A,T,label, and allInputs
        UtilityMethods.removeIndices(automaton.A, listOfInputsToQuantify);
        automaton.encoder = null;
        automaton.alphabetSize = 1;
        for (List<Integer> x : automaton.A)
            automaton.alphabetSize *= x.size();
        UtilityMethods.removeIndices(automaton.NS, listOfInputsToQuantify);
        UtilityMethods.removeIndices(automaton.label, listOfInputsToQuantify);
        for (List<Integer> i : allInputs)
            UtilityMethods.removeIndices(i, listOfInputsToQuantify);
        //example: permutation[1] = 7 means that encoded old input 1 becomes encoded new input 7
        List<Integer> permutation = new ArrayList<>();
        for (List<Integer> i : allInputs)
            permutation.add(automaton.encode(i));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.Q; q++) {
            Int2ObjectRBTreeMap<IntList> newMemDransitionFunction = new Int2ObjectRBTreeMap<>();
            new_d.add(newMemDransitionFunction);
            for (int x : automaton.d.get(q).keySet()) {
                int y = permutation.get(x);
                if (newMemDransitionFunction.containsKey(y))
                    UtilityMethods.addAllWithoutRepetition(newMemDransitionFunction.get(y), automaton.d.get(q).get(x));
                else
                    newMemDransitionFunction.put(y, new IntArrayList(automaton.d.get(q).get(x)));
            }
        }
        automaton.d = new_d;
        automaton.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "quantified:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
        if (automaton.TRUE_FALSE_AUTOMATON) {
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "Reversing:" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }

        // We change the direction of transitions first.
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for (int q = 0; q < automaton.Q; q++) new_d.add(new Int2ObjectRBTreeMap<>());
        for (int q = 0; q < automaton.Q; q++) {
            for (int x : automaton.d.get(q).keySet()) {
                for (int dest : automaton.d.get(q).get(x)) {
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
        automaton.d = new_d;
        IntSet setOfFinalStates = new IntOpenHashSet();
        /**final states become non final*/
        for (int q = 0; q < automaton.Q; q++) {
            if (automaton.O.getInt(q) != 0) {
                setOfFinalStates.add(q);
                automaton.O.set(q, 0);
            }
        }
        automaton.O.set(automaton.q0, 1);/**initial state becomes the final state.*/

        List<Int2IntMap> newMemD = automaton.subsetConstruction(null, setOfFinalStates, print, prefix + " ", log);

        if (!skipMinimize) {
            automaton.minimize(newMemD, print, prefix + " ", log);
        }

        if (reverseMsd) {
            flipNS(automaton);
        }

        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "reversed:" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
    }

    // flip the number system from msd to lsd and vice versa.
    private static void flipNS(Automaton automaton) {
        for (int i = 0; i < automaton.NS.size(); i++) {
            if (automaton.NS.get(i) == null) {
                continue;
            }
            int indexOfUnderscore = automaton.NS.get(i).getName().indexOf("_");
            String msd_or_lsd = automaton.NS.get(i).getName().substring(0, indexOfUnderscore);
            String suffix = automaton.NS.get(i).getName().substring(indexOfUnderscore);
            String newName = (msd_or_lsd.equals("msd") ? "lsd" : "msd") + suffix;
            automaton.NS.set(i, new NumberSystem(newName));
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
        if (automaton.TRUE_FALSE_AUTOMATON) {
            return;
        }
        try {
            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "reversing: " + automaton.Q + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            boolean addedDeadState = automaton.addDistinguishedDeadState(print, prefix, log);

            int minOutput = 0;
            if (addedDeadState) {
                // get state with smallest output. all states with this output will be removed.
                // after transducing, all states with this minimum output will be removed.

                for (int i = 0; i < automaton.O.size(); i++) {
                    if (automaton.O.getInt(i) < minOutput) {
                        minOutput = automaton.O.getInt(i);
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

            for (int i = 0; i < automaton.Q; i++) {
                newInitState.put(i, automaton.O.getInt(i));
            }

            newStates.add(newInitState);

            newStatesHash.put(newInitState, newStates.size() - 1);

            newStatesQueue.add(newInitState);

            while (!newStatesQueue.isEmpty()) {
                Map<Integer, Integer> currState = newStatesQueue.remove();

                // set up the output of this state to be g(q0), where g = currState.
                newO.add((int) currState.get(automaton.q0));

                newD.add(new Int2ObjectRBTreeMap<>());

                // assume that the
                // System.out.println("alphabet: " + d.get(q0) + ", " + d + ", " + alphabetSize);
                if (automaton.d.get(automaton.q0).keySet().size() != automaton.alphabetSize) {
                    throw new RuntimeException("Automaton should be deterministic!");
                }
                for (int l : automaton.d.get(automaton.q0).keySet()) {
                    Map<Integer, Integer> toState = new HashMap<>();

                    for (int i = 0; i < automaton.Q; i++) {
                        toState.put(i, currState.get(automaton.d.get(i).get(l).getInt(0)));
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

            automaton.Q = newStates.size();

            automaton.O = newO;

            automaton.d = newD;

            if (reverseMsd) {
                flipNS(automaton);
            }

            automaton.minimizeSelfWithOutput(print, prefix + " ", log);

            if (addedDeadState) {
                // remove all states that have an output of minOutput
                HashSet<Integer> statesRemoved = new HashSet<>();

                for (int q = 0; q < automaton.Q; q++) {
                    if (automaton.O.getInt(q) == minOutput) {
                        statesRemoved.add(q);
                    }
                }
                for (int q = 0; q < automaton.Q; q++) {

                    Iterator<Integer> iter = automaton.d.get(q).keySet().iterator();

                    while (iter.hasNext()) {
                        int x = iter.next();

                        if (statesRemoved.contains(automaton.d.get(q).get(x).getInt(0))) {
                            iter.remove();
                        }
                    }
                }

                automaton.canonized = false;
                automaton.canonize();
            }

            long timeAfter = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "reversed: " + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }
        } catch (Exception e) {
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

            if (automaton.NS.size() != 1) {
                throw new RuntimeException("Automaton must have one input to be converted.");
            }

            String nsName = automaton.NS.get(0).getName();

            String base = nsName.substring(nsName.indexOf("_") + 1);

            if (!UtilityMethods.isNumber(base) || Integer.parseInt(base) <= 1) {
                throw new RuntimeException("Base of number system of original automaton must be a positive integer greater than 1.");
            }

            int fromBase = Integer.parseInt(base);

            if (fromBase == toBase) {
                if (automaton.NS.get(0).isMsd() == toMsd) {
                    throw new RuntimeException("New and old number systems " + automaton.NS.get(0).getName() +
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
            if (!automaton.NS.get(0).isMsd()) {
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

        } catch (Exception e) {
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

            String nsName = automaton.NS.get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: msd_" + base + " to msd_" + (int) Math.pow(base, exponent) + ", " + automaton.Q + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

            // need to generate the new morphism, which is h^{exponent}, where h is the original morphism.

            List<List<Integer>> prevMorphism = new ArrayList<>();

            for (int q = 0; q < automaton.Q; q++) {
                List<Integer> morphismString = new ArrayList<>();

                if (automaton.d.get(q).keySet().size() != automaton.alphabetSize) {
                    throw new RuntimeException("Automaton must be deterministic");
                }

                for (int di = 0; di < automaton.alphabetSize; di++) {
                    morphismString.add(automaton.d.get(q).get(di).getInt(0));
                }
                prevMorphism.add(morphismString);
            }

            for (int i = 2; i <= exponent; i++) {

                List<List<Integer>> newMorphism = new ArrayList<>();

                for (int j = 0; j < automaton.Q; j++) {
                    List<Integer> morphismString = new ArrayList<>();
                    for (int k = 0; k < prevMorphism.get(j).size(); k++) {
                        for (int di : automaton.d.get(j).keySet()) {
                            morphismString.add(automaton.d.get(prevMorphism.get(j).get(k)).get(di).getInt(0));
                        }
                    }
                    newMorphism.add(morphismString);
                }
                prevMorphism = new ArrayList<>(newMorphism);
            }

            for (int q = 0; q < automaton.Q; q++) {
                newD.add(new Int2ObjectRBTreeMap<>());
                for (int di = 0; di < prevMorphism.get(q).size(); di++) {

                    int toState = prevMorphism.get(q).get(di);

                    // set up transition
                    IntList newList = new IntArrayList();
                    newList.add(toState);
                    newD.get(q).put(di, newList);
                }
            }

            automaton.d = newD;

            // change number system too.

            automaton.NS.set(0, new NumberSystem("msd_" + (int) (Math.pow(base, exponent))));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < (int) (Math.pow(base, exponent)); i++) {
                ints.add(i);
            }
            automaton.A = List.of(ints);
            automaton.alphabetSize = ints.size();

            if (print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + "Converted: msd_" + base + " to msd_" + (int) (Math.pow(base, exponent)) + ", " + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

        } catch (Exception e) {
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
            String nsName = automaton.NS.get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: lsd_" + base + " to lsd_" + (int) Math.pow(root, exponent) + ", " + automaton.Q + " states";
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

            StateTuple initState = new StateTuple(automaton.q0, List.of());

            newStates.add(initState);
            newStatesQueue.add(initState);
            newStatesHash.put(initState, newStates.size() - 1);


            while (!newStatesQueue.isEmpty()) {
                StateTuple currState = newStatesQueue.remove();

                newD.add(new Int2ObjectRBTreeMap<>());

                if (currState.string.isEmpty()) {
                    newO.add(automaton.O.getInt(currState.state));
                } else {
                    int stringValue = 0;

                    for (int i = 0; i < currState.string.size(); i++) {
                        stringValue += currState.string.get(i) * (int) (Math.pow(root, i));
                    }

                    // set up output
                    newO.add(automaton.O.getInt(automaton.d.get(currState.state).get(stringValue).getInt(0)));
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

                        toState = new StateTuple(automaton.d.get(currState.state).get(toStateStringValue).getInt(0), List.of());
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

            automaton.Q = newStates.size();

            automaton.O = newO;

            automaton.d = newD;

            automaton.canonized = false;

            // change number system too.
            automaton.NS.set(0, new NumberSystem("lsd_" + root));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < root; i++) {
                ints.add(i);
            }
            automaton.A = List.of(ints);
            automaton.alphabetSize = ints.size();

            if (print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + prefix + "Converted: lsd_" + base + " to lsd_" + (int) (Math.pow(root, exponent)) + ", " + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting the number system msd_k^j of an automaton to msd_k");
        }


    }

    public static Automaton combine(Automaton automaton, Queue<Automaton> subautomata, IntList outputs, boolean print, String prefix, StringBuilder log) {

        Automaton first = automaton.clone();

        // In an automaton without output, every non-zero output value represents an accepting state
        // we change this to correspond to the value assigned to the first automaton by our command
        for (int q = 0; q < first.Q; q++) {
            if (first.O.getInt(q) != 0) {
                first.O.set(q, outputs.getInt(0));
            }
        }
        first.combineIndex = 1;
        first.combineOutputs = outputs;
        while (!subautomata.isEmpty()) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "computing =>:" + first.Q + " states - " + next.Q + " states";
                log.append(msg + System.lineSeparator());
                System.out.println(msg);
            }

            // crossProduct requires labelling so we make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.label = first.label;
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            totalize(first, print, prefix + " ", log);
            totalize(next, print, prefix + " ", log);
            Automaton product = crossProduct(first, next, "combine", print, prefix + " ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "computed =>:" + first.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
            String msg = prefix + "comparing (" + operator + "):" + automaton.Q + " states - " + W.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        Automaton M = crossProduct(automaton, W, operator, print, prefix + " ", log);
        M.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "compared (" + operator + "):" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
            String msg = prefix + "comparing (" + operator + ") against " + o + ":" + automaton.Q + " states";
            log.append(msg + System.lineSeparator());
            System.out.println(msg);
        }
        for (int p = 0; p < automaton.Q; p++) {
            switch (operator) {
                case "<":
                    automaton.O.set(p, (automaton.O.getInt(p) < o) ? 1 : 0);
                    break;
                case ">":
                    automaton.O.set(p, (automaton.O.getInt(p) > o) ? 1 : 0);
                    break;
                case "=":
                    automaton.O.set(p, (automaton.O.getInt(p) == o) ? 1 : 0);
                    break;
                case "!=":
                    automaton.O.set(p, (automaton.O.getInt(p) != o) ? 1 : 0);
                    break;
                case "<=":
                    automaton.O.set(p, (automaton.O.getInt(p) <= o) ? 1 : 0);
                    break;
                case ">=":
                    automaton.O.set(p, (automaton.O.getInt(p) >= o) ? 1 : 0);
                    break;
            }
        }
        automaton.minimize(null, print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "compared (" + operator + ") against " + o + ":" + automaton.Q + " states - " + (timeAfter - timeBefore) + "ms";
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
