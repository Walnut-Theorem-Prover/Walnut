package Automata;

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
     * @param automaton
     * @param M
     * @return this automaton cross product M.

     */
    static Automaton crossProduct(Automaton automaton,
                                  Automaton M,
                                  String op,
                                  boolean print,
                                  String prefix,
                                  StringBuilder log) throws Exception {

        if (automaton.TRUE_FALSE_AUTOMATON || M.TRUE_FALSE_AUTOMATON) {
            throw new Exception("Invalid use of the crossProduct method: " +
                    "the automata for this method cannot be true or false automata.");
        }

        if (automaton.label == null ||
            M.label == null ||
            automaton.label.size() != automaton.A.size() ||
            M.label.size() != M.A.size()
        ) {
            throw new Exception("Invalid use of the crossProduct method: " +
                    "the automata for this method must have labeled inputs.");
        }

        /**N is going to hold the cross product*/
        Automaton N = new Automaton();

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "Computing cross product:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        /**
         * for example when sameLabelsInMAndThis[2] = 3, then input 2 of M has the same label as input 3 of this
         * and when sameLabelsInMAndThis[2] = -1, it means that input 2 of M is not an input of this
         */
        int[] sameInputsInMAndThis = new int[M.A.size()];
        for (int i = 0 ; i < M.label.size(); i++) {
            sameInputsInMAndThis[i] = -1;
            if (automaton.label.contains(M.label.get(i))) {
                int j = automaton.label.indexOf(M.label.get(i));
                if (!UtilityMethods.areEqual(automaton.A.get(j), M.A.get(i))){
                    throw new Exception("in computing cross product of two automaton, "
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
            }
            else {
                int j = sameInputsInMAndThis[i];
                if(M.NS.get(i) != null && N.NS.get(j) == null) {
                    N.NS.set(j, M.NS.get(i));
                }

            }
        }
        N.alphabetSize = 1;
        for(List<Integer> i : N.A) {
            N.alphabetSize *= i.size();
        }

        List<Integer> allInputsOfN = new ArrayList<>();
        for (int i = 0; i < automaton.alphabetSize; i++) {
            for (int j = 0; j < M.alphabetSize; j++) {
                List<Integer> inputForN = joinTwoInputsForCrossProduct(Automaton.decode(automaton, i), Automaton.decode(M, j),sameInputsInMAndThis);
                if(inputForN == null)
                    allInputsOfN.add(-1);
                else
                    allInputsOfN.add(N.encode(inputForN));
            }
        }
        ArrayList<List<Integer>> statesList = new ArrayList<>();
        Map<List<Integer>,Integer> statesHash = new HashMap<>();
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
                            + statesList.size() + " reachable states - " + (timeAfter-timeBefore)+"ms";
                    log.append(msg + UtilityMethods.newLine());
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
            switch(op){
                case "&":
                    N.O.add((automaton.O.getInt(p) != 0 && M.O.getInt(q) != 0) ? 1 : 0);
                    break;
                case "|":
                    N.O.add((automaton.O.getInt(p) != 0 || M.O.getInt(q) != 0) ? 1 : 0);
                    break;
                case "^":
                    N.O.add(((automaton.O.getInt(p) != 0 && M.O.getInt(q) == 0)||(automaton.O.getInt(p) == 0 && M.O.getInt(q) != 0)) ? 1 : 0);
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
                    if(M.O.getInt(q) == 0)throw new Exception("division by zero");
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

            for(int x: automaton.d.get(p).keySet()){
                for(int y:M.d.get(q).keySet()){
                    int z = allInputsOfN.get(x*M.alphabetSize+y);
                    if(z != -1){
                        IntList dest = new IntArrayList();
                        thisStatesTransitions.put(z, dest);
                        for(int dest1 : automaton.d.get(p).get(x)) {
                            for(int dest2 : M.d.get(q).get(y)) {
                                List<Integer> dest3 = Arrays.asList(dest1, dest2);
                                if(!statesHash.containsKey(dest3)){
                                    statesList.add(dest3);
                                    statesHash.put(dest3, statesList.size()-1);
                                }
                                dest.add((int)statesHash.get(dest3));
                            }
                        }
                    }
                }
            }
            currentState++;
        }
        N.Q = statesList.size();
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed cross product:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
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
                                StringBuilder log) throws Exception {
        if((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) &&
            (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) {
            return new Automaton(true);
        }

        if((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) ||
            (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) {
            return new Automaton(false);
        }

        if(automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) {
            return M;
        }

        if(M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON) {
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "computing &:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        Automaton N = crossProduct(automaton, M,"&",print,prefix,log);
        N.minimize(null, print,prefix+" ",log);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed &:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return  this automaton or M
     * @throws Exception
     */
    public static Automaton or(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) throws Exception{
        if((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) || (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) return new Automaton(true);
        if((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) return new Automaton(false);

        if(automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON)return M;
        if(M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)return automaton;

        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computing |:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        totalize(automaton, print,prefix+" ",log);
        totalize(M, print,prefix+" ",log);
        Automaton N = crossProduct(automaton, M,"|",print,prefix,log);

        N.minimize(null, print,prefix +" ",log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed |:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return N;
    }

    /**
     *
     * @param automaton
     * @param M
     * @return this automaton xor M
     * @throws Exception
     */
    public static Automaton xor(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) throws Exception{
        if((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) return new Automaton(true);
        if((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) return new Automaton(true);
        if((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) return new Automaton(false);
        if((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) return new Automaton(false);

        if(automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON)return M;
        if(M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)return automaton;

        if(automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON){
            not(M, print,prefix,log);
            return M;
        }
        if(M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON){
            not(automaton, print,prefix,log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computing ^:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        totalize(automaton, print,prefix+" ",log);
        totalize(M, print,prefix+" ",log);
        Automaton N = crossProduct(automaton, M,"^",print,prefix + " ", log);
        N.minimize(null, print,prefix+" ",log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed ^:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return  this automaton imply M
     * @throws Exception
     */
    public static Automaton imply(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) throws Exception{
        if((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) return new Automaton(false);
        if((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) || (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) return new Automaton(true);
        if(automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON)return M;
        if(M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON){
            not(automaton, print,prefix,log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computing =>:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        totalize(automaton, print,prefix+" ",log);
        totalize(M, print,prefix+" ",log);
        Automaton N = crossProduct(automaton, M,"=>",print,prefix+" ",log);
        N.minimize(null, print,prefix+" ",log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed =>:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @param automaton
     * @param M
     * @return  this automaton iff M
     * @throws Exception
     */
    public static Automaton iff(Automaton automaton, Automaton M, boolean print, String prefix, StringBuilder log) throws Exception{
        if(((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)) ||
                ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON))) return new Automaton(true);
        if(((automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON)) ||
                ((automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON) && (M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON))) return new Automaton(false);

        if(automaton.TRUE_FALSE_AUTOMATON && automaton.TRUE_AUTOMATON)return M;
        if(M.TRUE_FALSE_AUTOMATON && M.TRUE_AUTOMATON)return automaton;
        if(automaton.TRUE_FALSE_AUTOMATON && !automaton.TRUE_AUTOMATON){
            not(M, print,prefix,log);
            return M;
        }
        if(M.TRUE_FALSE_AUTOMATON && !M.TRUE_AUTOMATON){
            not(automaton, print,prefix,log);
            return automaton;
        }

        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computing <=>:" + automaton.Q + " states - " + M.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        totalize(automaton, print,prefix+" ",log);
        totalize(M, print,prefix+" ",log);
        Automaton N = crossProduct(automaton, M,"<=>",print,prefix+" ",log);
        N.minimize(null, print,prefix+" ",log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed <=>:" + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return N;
    }

    /**
     * @return changes this automaton to its negation
     * @throws Exception
     */
    public static void not(Automaton automaton, boolean print, String prefix, StringBuilder log) throws Exception{
        if(automaton.TRUE_FALSE_AUTOMATON){
            automaton.TRUE_AUTOMATON = !automaton.TRUE_AUTOMATON;
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computing ~:" + automaton.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        totalize(automaton, print,prefix+" ",log);
        for(int q = 0; q < automaton.Q; q++)
            automaton.O.set(q, automaton.O.getInt(q) != 0 ? 0 : 1 );

        automaton.minimize(null, print,prefix+" ",log);
        automaton.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "computed ~:" + automaton.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    /**
     * If this automaton's language is L_1 and the language of "other" is L_2, this automaton accepts the language
     * L_1 / L_2 = { x : exists y in L_2 such that xy in L_1 }
     * @param automaton
     * @param other
     * @param print
     * @param prefix
     * @param log
     * @return
     * @throws Exception
     */
    public static Automaton rightQuotient(Automaton automaton, Automaton other, boolean skipSubsetCheck, boolean print, String prefix, StringBuilder log) throws Exception {

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "right quotient: " + automaton.Q + " state automaton with " + other.Q + " state automaton";
            log.append(msg + UtilityMethods.newLine());
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
            }
            else {
                isSubset = false;
            }

            if (!isSubset) {
                throw new Exception("Second automaton's alphabet must be a subset of the first automaton's alphabet for right quotient.");
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
            }
            else {
                M.O.set(i, 1);
            }
        }

        M.minimize(null, print, prefix, log);
        M.applyAllRepresentations();
        M.canonized = false;
        M.canonize();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "right quotient complete: " + M.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return M;
    }

    public static Automaton leftQuotient(Automaton automaton, Automaton other, boolean print, String prefix, StringBuilder log) throws Exception {
        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "left quotient: " + automaton.Q + " state automaton with " + other.Q + " state automaton";
            log.append(msg + UtilityMethods.newLine());
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
        }
        else {
            isSubset = false;
        }

        if (!isSubset) {
            throw new Exception("First automaton's alphabet must be a subset of the second automaton's alphabet for left quotient.");
        }

        Automaton M1 = automaton.clone();
        M1.reverse(print, prefix, log, true);
        M1.canonized = false;
        M1.canonize();

        Automaton M2 = other.clone();
        M2.reverse(print, prefix, log, true);
        M2.canonized = false;
        M2.canonize();

        Automaton M = rightQuotient(M1, M2, true, print, prefix, log);

        M.reverse(print, prefix, log, true);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "left quotient complete: " + M.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return M;
    }

    /**
     * This method adds a dead state to totalize the transition function
     * @throws Exception
     */
    static void totalize(Automaton automaton, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "totalizing:" + automaton.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        //we first check if the automaton is totalized
        boolean totalized = true;
        for(int q = 0; q < automaton.Q; q++){
            for(int x = 0; x < automaton.alphabetSize; x++){
                if(!automaton.d.get(q).containsKey(x)){
                    IntList nullState = new IntArrayList();
                    nullState.add(automaton.Q);
                    automaton.d.get(q).put(x, nullState);
                    totalized = false;
                }
            }
        }
        if(!totalized){
            automaton.O.add(0);
            automaton.Q++;
            automaton.d.add(new Int2ObjectRBTreeMap<>());
            for(int x = 0; x < automaton.alphabetSize; x++){
                IntList nullState = new IntArrayList();
                nullState.add(automaton.Q -1);
                automaton.d.get(automaton.Q -1).put(x, nullState);
            }
        }

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "totalized:" + automaton.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    /**
     * Calculate new state output (O), from previous O and statesList.
     * @param O - previous O
     * @param statesList
     * @return new O
     */
    static IntList calculateNewStateOutput(IntList O, List<IntSet> statesList) {
        IntList newO = new IntArrayList();
        for(IntSet state: statesList){
            boolean flag = false;
            for(int q:state){
                if(O.getInt(q)!=0){
                    newO.add(1);
                    flag=true;
                    break;
                }
            }
            if(!flag){
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
     * @param first
     * @param second
     * @param equalIndices
     * @return
     */
    private static List<Integer> joinTwoInputsForCrossProduct(
            List<Integer> first,List<Integer> second,int[] equalIndices){
        List<Integer> R = new ArrayList<>();
        R.addAll(first);
        for(int i = 0 ; i < second.size();i++)
            if(equalIndices[i] == -1)
                R.add(second.get(i));
            else{
                if(!first.get(equalIndices[i]).equals(second.get(i)))
                    return null;
            }
        return R;
    }

    public static void fixLeadingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log)throws Exception{
        if(automaton.TRUE_FALSE_AUTOMATON)return;
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "fixing leading zeros:" + automaton.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        automaton.canonized = false;
        List<Integer> ZERO = new ArrayList<>();//all zero input
        for(List<Integer> i: automaton.A)ZERO.add(i.indexOf(0));
        int zero = automaton.encode(ZERO);
        if(!automaton.d.get(automaton.q0).containsKey(zero)){
            automaton.d.get(automaton.q0).put(zero,new IntArrayList());
        }
        if(!automaton.d.get(automaton.q0).get(zero).contains(automaton.q0)){
            automaton.d.get(automaton.q0).get(zero).add(automaton.q0);
        }

        IntSet initial_state = zeroReachableStates(automaton);
        List<Int2IntMap> newMemD = automaton.subsetConstruction(null, initial_state,print,prefix+" ",log);
        automaton.minimize(newMemD, print, prefix+" ", log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "fixed leading zeros:" + automaton.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    public static void fixTrailingZerosProblem(Automaton automaton, boolean print, String prefix, StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "fixing trailing zeros:" + automaton.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        automaton.canonized = false;
        Set<Integer> newFinalStates;// = statesReachableFromFinalStatesByZeros();
        newFinalStates = statesReachableToFinalStatesByZeros(automaton);
        List<Integer> ZERO = new ArrayList<>();//all zero input
        for(List<Integer> i: automaton.A)ZERO.add(i.indexOf(0));
        for(int q:newFinalStates){
            automaton.O.set(q, 1);
        }

        automaton.minimize(null, print,prefix+" ",log);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "fixed trailing zeros:" + automaton.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    /**
     * Used for the "I" quantifier. If some input is in msd, then we remove leading zeroes,
     * if some input is in lsd, then we remove trailing zeroes, otherwise, we do nothing.
     * To do this, for each input, we construct an automaton which accepts if the leading/trailing input is non-zero,
     * union all these automata together, and intersect with our original automaton.
     * @param automaton
     * @param listOfLabels
     * @return
     * @throws Exception
     */
    public static Automaton removeLeadingZeroes(Automaton automaton, List<String> listOfLabels, boolean print, String prefix, StringBuilder log) throws Exception {
        for(String s:listOfLabels) {
            if(!automaton.label.contains(s)) {
                throw new Exception( "Variable " + s + " in the list of quantified variables is not a free variable.");
            }
        }
        if(listOfLabels.size() == 0) {
            return automaton.clone();
        }
        long timeBefore = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "removing leading zeroes for:" + automaton.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        List<Integer> listOfInputs = new ArrayList<>();//extract the list of indices of inputs from the list of labels
        for(String l:listOfLabels) {
            listOfInputs.add(automaton.label.indexOf(l));
        }
        Automaton M = new Automaton(false);
        for(int n:listOfInputs) {
            Automaton N = removeLeadingZeroesHelper(automaton, n, print, prefix+" ", log);
            M = or(M, N, print, prefix+" ", log);
        }
        M = and(automaton, M, print, prefix+" ", log);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "quantified:" + automaton.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return M;
    }

    /**
     * Returns the automaton with the same alphabet as the current automaton, which requires the nth input to
     * start with a non-zero symbol (if msd), end with a non-zero symbol (if lsd), otherwise, return the true
     * automaton. The returned automaton is meant to be intersected with the current automaton to remove
     * leading/trailing * zeroes from the nth input.
     * @param automaton
     * @param n
     * @return
     * @throws Exception
     */
    private static Automaton removeLeadingZeroesHelper(Automaton automaton, int n, boolean print, String prefix, StringBuilder log) throws Exception{
        if (n >= automaton.A.size() || n < 0) {
            throw new Exception("Cannot remove leading zeroes for the "
                    + (n+1) + "-th input when automaton only has " + automaton.A.size() + " inputs.");
        }

        if (automaton.NS.get(n) == null) {
            return new Automaton(true);
        }

        Automaton M = new Automaton();
        M.Q = 2;
        M.q0 = 0;
        M.O.add(1);M.O.add(1);
        M.d.add(new Int2ObjectRBTreeMap<>());
        M.d.add(new Int2ObjectRBTreeMap<>());
        M.NS = automaton.NS;
        M.A = automaton.A;
        M.label = automaton.label;
        M.alphabetSize = automaton.alphabetSize;
        M = M.clone();

        IntList dest = new IntArrayList();
        dest.add(1);
        for(int i = 0; i < automaton.alphabetSize; i++) {
            List<Integer> list = Automaton.decode(automaton, i);
            if (list.get(n) != 0) {
                M.d.get(0).put(i, new IntArrayList(dest));
            }
            M.d.get(1).put(i, new IntArrayList(dest));
        }
        if (!automaton.NS.get(n).isMsd()) {
            M.reverse(print, prefix, log, false);
        }
        return M;
    }

    /**Returns the set of states reachable from the initial state by reading 0*
     * @param automaton
     */
    private static IntSet zeroReachableStates(Automaton automaton){
        IntSet result = new IntOpenHashSet();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(automaton.q0);
        List<Integer> ZERO = new ArrayList<>();//all zero input
        for(List<Integer> i: automaton.A)ZERO.add(i.indexOf(0));
        int zero = automaton.encode(ZERO);
        while(!queue.isEmpty()){
            int q = queue.poll();
            result.add(q);
            if(automaton.d.get(q).containsKey(zero))
                for(int p: automaton.d.get(q).get(zero))
                    if(!result.contains(p))
                        queue.add(p);
        }
        return result;
    }

    /**
     * So for example if f is a final state and f is reachable from q by reading 0*
     * then q will be in the resulting set of this method.
     * @return
     * @param automaton
     */
    private static Set<Integer> statesReachableToFinalStatesByZeros(Automaton automaton){
        Set<Integer> result = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        List<Integer> ZERO = new ArrayList<>();
        for(List<Integer> i: automaton.A)ZERO.add(i.indexOf(0));
        int zero = automaton.encode(ZERO);
        //this is the adjacency matrix of the reverse of the transition graph of this automaton on 0
        List<List<Integer>>adjacencyList = new ArrayList<>();
        for(int q = 0; q < automaton.Q; q++)adjacencyList.add(new ArrayList<>());
        for(int q = 0; q < automaton.Q; q++){
            if(automaton.d.get(q).containsKey(zero)){
                List<Integer> destination = automaton.d.get(q).get(zero);
                for(int p:destination){
                    adjacencyList.get(p).add(q);
                }
            }
            if(automaton.O.getInt(q) != 0)queue.add(q);
        }
        while(!queue.isEmpty()){
            int q = queue.poll();
            result.add(q);
            for(int p:adjacencyList.get(q))
                if(!result.contains(p))
                    queue.add(p);
        }
        return result;
    }

    /**
     * Checks if any input has the same label as input i. It then removes copies of input i appropriately. So for example an
     * expression like f(a,a) becomes
     * an automaton with one input. After we are done with input i, we call removeSameInputs(i+1)
     * @param automaton
     * @param i
     * @throws Exception
     */
    static void removeSameInputs(Automaton automaton, int i) throws Exception{
        if(i >= automaton.A.size())return;
        List<Integer> I = new ArrayList<>();
        I.add(i);
        for(int j = i+1; j < automaton.A.size(); j++){
            if(automaton.label.get(i).equals(automaton.label.get(j))){
                if(!UtilityMethods.areEqual(automaton.A.get(i), automaton.A.get(j))){
                    throw new Exception("Inputs " + i + " and " + j + " have the same label but different alphabets.");
                }
                I.add(j);
            }
        }
        if(I.size() > 1){
            reduceDimension(automaton, I);
        }
        removeSameInputs(automaton, i+1);
    }

    private static void reduceDimension(Automaton automaton, List<Integer> I){
        List<List<Integer>> newAlphabet = new ArrayList<>();
        List<Integer> newEncoder = new ArrayList<>();
        newEncoder.add(1);
        for(int i = 0; i < automaton.A.size(); i++)
            if(!I.contains(i) || I.indexOf(i) == 0)
                newAlphabet.add(new ArrayList<>(automaton.A.get(i)));
        for(int i = 0 ; i < newAlphabet.size()-1;i++)
            newEncoder.add(newEncoder.get(i)*newAlphabet.get(i).size());
        List<Integer> map = new ArrayList<>();
        for(int n = 0; n < automaton.alphabetSize; n++)
            map.add(automaton.mapToReducedEncodedInput(n, I, newEncoder, newAlphabet));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for(int q = 0; q < automaton.Q; q++){
            Int2ObjectRBTreeMap<IntList> currentStatesTransition = new Int2ObjectRBTreeMap<>();
            new_d.add(currentStatesTransition);
            for(int n: automaton.d.get(q).keySet()){
                int m = map.get(n);
                if(m != -1){
                    if(currentStatesTransition.containsKey(m))
                        currentStatesTransition.get(m).addAll(automaton.d.get(q).get(n));
                    else
                        currentStatesTransition.put(m, new IntArrayList(automaton.d.get(q).get(n)));
                }
            }
        }
        automaton.d = new_d;
        I.remove(0);
        automaton.A = newAlphabet;
        UtilityMethods.removeIndices(automaton.NS,I);
        automaton.encoder = null;
        automaton.alphabetSize = 1;
        for(List<Integer> x: automaton.A)
            automaton.alphabetSize *= x.size();
        UtilityMethods.removeIndices(automaton.label, I);
    }
}
