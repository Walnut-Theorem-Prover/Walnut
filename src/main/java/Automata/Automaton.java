
package Automata;
import Main.UtilityMethods;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * This class can represent different types of automaton: deterministic/non-deterministic and/or automata with output/automata without output.<bf>
 * There are also two special automata: true automaton, which accepts everything, and false automaton, which accepts nothing.
 * To represent true/false automata we use the field members: TRUE_FALSE_AUTOMATON and TRUE_AUTOMATA. <br>
 * Let's forget about special true/false automata, and talk about ordinary automata:
 * Inputs to an ordinary automaton are n-tuples. Each coordinate has its own alphabet. <br>
 * Let's see this by means of an example: <br>
 * Suppose our automaton has 3-tuples as its input: <br>
 * -The field A, which is a list of list of integers, is used to store the alphabets of
 * all these three inputs. For example we might have A = [[1,2],[0,-1,1],[1,3]]. This means that the first input is over alphabet
 * {1,2} and the second one is over {0,-1,1},... <br>
 * Note: Input alphabets are subsets of integers. <br>
 * -Each coordinate also has a type which is stored in T. For example we might have T = [Type.arithmetic,Type.arithmetic,Type.arithmetic],
 * which means all three inputs are of type arithmetic. You can find out what this means in the tutorial.
 * -So in total there are 12 = 2*3*2 different inputs (this number is stored in alphabetSize)
 * for this automaton. Here are two example inputs: (2,-1,3),(1,0,3).
 * We can encode these 3-tuples by the following rule:<br>
 * 0 = (1,0,1) <br>
 * 1 = (2,0,1) <br>
 * 2 = (1,-1,1)<br>
 * ...<br>
 * 11 = (2,1,3)<br>
 * We use this encoded numbers in our representation of automaton to refer to a particular input. For example
 * we might say on (2,1,3) we go from state 5 to state 0 by setting d.get(5) = (11,[5]). We'll see more on d (transition function)
 * -Now what about states? Q stores the number of states. For example when Q = 3, the set of states is {0,1,2}.
 * -Initial state: q0 is the initial state. For example we might have q0 = 1.
 * -Now field member O is an important one: O stores the output of an state. Now in the case of DFA/NFA, a value of non-zero
 * in O means a final state, and a value of zero means a non-final state. So for example we might have O = {1,-1,0} which means
 * that the first two states are final states. In the case of an automaton with output O simply represents output of an state.
 * Continuing with this example, in the case of automaton with output,
 * the first state has output 1, the second has output -1, and the third one has output 0. As you
 * have guessed the output alphabet can be any finite subset of integers. <br>
 * We might want to give labels to inputs. For example if we set label = ["x","y","z"], the label of the first input is "x".
 * Then in future, we can refer to this first input by the label "x". <br>
 * -The transition function is d which is a TreeMap<integer,List<Integer>> for each state. For example we might have
 * d.get(1) = {(0,[0]),(1,[1,2]),...} which means that state 1 goes to state 0 on input 0, and goes to states 1 and 2 on 1,....
 *
 * @author Hamoon
 *
 */
public class Automaton {
    /**
     * When TRUE_FALSE_AUTOMATON = false, it means that this automaton is
     * an actual automaton and not one of the special automata: true or false
     * When TRUE_FALSE_AUTOMATON = true and TRUE_AUTOMATON = false then this is a false automaton.
     * When TRUE_FALSE_AUTOMATON = true and TRUE_AUTOMATON = true then this is a true automaton.
    */
    public boolean TRUE_FALSE_AUTOMATON;
    public boolean TRUE_AUTOMATON = false;

    /**
     *  Input Alphabet.
     *  For example when A = [[-1,1],[2,3]], the first and the second inputs are over alphabets
     *  {-1,1} and {2,3} respectively.
     *  Remember that the input to an automaton is a tuple (a pair in this example).
     *  For example a state might make a transition on input (1,3). Here the
     *  first input is 1 and the second input is 3.
     *  Also note that A is a list of sets, but for technical reasons, we just made it a list of lists. However,
     *  we have to make sure, at all times, that the inner lists of A don't contain repeated elements.
     */
    public List<List<Integer>> A;

    /**
     * Alphabet Size. For example, if A = [[-1,1],[2,3]], then alphabetSize = 4 and if A = [[-1,1],[0,1,2]], then alphabetSize = 6
     */
    public int alphabetSize;

    /**
     * This vector is useful in the encode method.
     * When A = [l1,l2,l3,...,ln] then
     * encoder = [1,|l1|,|l1|*|l2|,...,|l1|*|l2|*...*|ln-1|].
     * It is useful, as mentioned earlier, in the encode method. encode method gets a list x, which represents a viable
     * input to this automaton, and returns a non-negative integer, which is the integer represented by x, in base encoder.
     * Note that encoder is a mixed-radix base. We use the encoded integer, returned by encode(), to store transitions.
     * So we don't store the list x.
     * We can decode, the number returned by encode(), and get x back using decode method.
     */
    public List<Integer> encoder;

    /**
     * Types of the inputs to this automaton.
     * There are two possible types for inputs for an automaton:Type.arithmetic or Type.alphabetLetter.
     * In other words, type of inputs to an automaton is either arithmetic or non-arithmetic.
     * For example we might have A = [[1,-1],[0,1,2],[0,-1]] and T = [Type.alphabetLetter, Type.arithmetic, Type.alphabetLetter]. So
     * the first and third inputs are non-arithmetic (and should not be treated as arithmetic).
     * This type is useful in type checking. So for example, we might have f(a,b+1,c+1), where f is the example automaton. Then this
     * is a type error, because the third input to f is non-arithmetic, and hence we cannot have c+1 as our third argument.
     * It is very important to note that, an input of type arithmetic must always contain 0 and 1 in its alphabet.
     */
    public List<NumberSystem> NS;

    /**Number of States. For example when Q = 3, the set of states is {0,1,2}*/
    public int Q;

    /**Initial State.*/
    public int q0;

    /**State Outputs. In case of DFA/NFA accepting states have a nonzero integer as their output.
     * Rejecting states have output 0.
     * Example: O = [-1,2,...] then state 0 and 1 have outputs -1 and 2 respectively.*/
    public IntList O;

    /**We would like to give label to inputs. For example we might want to call the first input by a and so on.
     * As an example when label = ["a","b","c"], the label of the first, second, and third inputs are a, b, and c respectively.
     * These labels are then useful when we quantify an automaton. So for example, in a predicate like E a f(a,b,c) we are having an automaton
     * of three inputs, where the first, second, and third inputs are labeled "a","b", and "c". Therefore E a f(a,b,c) says, we want to
     * do an existential quantifier on the first input.
     * */
    public List<String> label;

    /** When true, states are sorted in breadth-first order and labels are sorted lexicographically.
     *  It is used in canonize method. For more information read about canonize() method.
     * */
    public boolean canonized;

    /** When true, labels are sorted lexicographically. It is used in sortLabel() method.*/
    public boolean labelSorted;

    /**
     * Transition Function for This State. For example, when d[0] = [(0,[1]),(1,[2,3]),(2,[2]),(3,[4]),(4,[1]),(5,[0])]
     * and alphabet A = [[0,1],[-1,2,3]]
     * then from the state 0 on
     * (0,-1) we go to 1
     * (0,2) we go to 2,3
     * (0,3) we go to 2
     * (1,-1) we go to 4
     * ...
     * So we store the encoded values of inputs in d, i.e., instead of saying on (0,-1) we go to state 1, we say on 0, we go
     * to state 1.
     * Recall that (0,-1) represents 0 in mixed-radix base (1,2) and alphabet A. We have this mixed-radix base (1,2) stored as encoder in
     * our program, so for more information on how we compute it read the information on List<Integer> encoder field.
     *
     * For memory reduction, during determinization the transition function d is set to null and temporarily represented with
     * the memory-efficient newMemD, which only stores single-state transitions output by the Subset Construction algorithm.
     * The transition function d is then regenerated during the minimize_valmari method, once the states are minimized.
     */
    public List<Int2ObjectRBTreeMap<IntList>> d;



    // for use in the combine command, counts how many products we have taken so far, and hence what to set outputs to
    public int combineIndex;

    // for use in the combine command, allows crossProduct to determine what to set outputs to
    public IntList combineOutputs;

    // for use in inf command, keeps track of which states we have visited
    public IntSet visited;

    // for use in inf command, records where we started our depth first search to find a cycle
    public int started;

    // for use in test command, to gather a list of all accepted values of a specified length in lexicographic order
    public List<String> accepted;

    // for use in test command, tells us what length of solutions we are currently searching for in this subautomaton
    public int searchLength;

    // for use in test command, tells us the number of accepted strings remaining to be added to the main list, so we can end early if
    // we find that many
    public int maxNeeded;

    /* Minimization algorithm */
    void minimize_valmari(List<Int2IntMap> newMemD, boolean print, String prefix,StringBuilder log) throws Exception{
        IntSet qqq = new IntOpenHashSet();
        qqq.add(q0);
        newMemD = subsetConstruction(newMemD, qqq,print,prefix,log);

        ValmariDFA v = new ValmariDFA(newMemD, Q);
        v.minValmari(O);
        Q = v.blocks.z;
        q0 = v.blocks.S[q0];
        O = v.determineO();
        d = v.determineD();

        canonized = false;
    }

    /**
     * Default constructor. It just initializes the field members.
     */
    public List<String> getLabel() {
        return label;
    }

    public Automaton() {
        TRUE_FALSE_AUTOMATON = false;

        A = new ArrayList<>();

        NS = new ArrayList<>();
        encoder = null;

        O = new IntArrayList();

        d = new ArrayList<>();
        label = new ArrayList<>();
        canonized = false;
        labelSorted = false;
        dk.brics.automaton.Automaton.setMinimization(dk.brics.automaton.Automaton.MINIMIZE_HOPCROFT);
    }

    /**
     * Initializes a special automaton: true or false.
     * A true automaton, is an automaton that accepts everything. A false automaton is an automaton that accepts nothing.
     * Therefore, M and false is false for every automaton M. We also have that M or true is true for every automaton M.
     * @param true_automaton
     */
    public Automaton(boolean true_automaton){
        TRUE_FALSE_AUTOMATON = true;
        this.TRUE_AUTOMATON = true_automaton;
    }

    /**
     * Takes a regular expression and the alphabet for that regular expression and constructs the corresponding automaton.
     * For example if the regularExpression = "01*" and alphabet = [0,1,2], then the resulting automaton accepts
     * words of the form 01* over the alphabet {0,1,2}.<br>
     *
     * We actually compute the automaton for regularExpression intersected with alphabet*.
     * So for example if regularExpression = [^4]* and alphabet is [1,2,4], then the resulting
     * automaton accepts (1|2)*<br>
     *
     * An important thing to note here is that the automaton being constructed
     * with this constructor, has only one input, and it is of type Type.alphabetLetter.
     * @throws Exception
     */
    public Automaton(String regularExpression, List <Integer> alphabet) throws Exception {
        this();
        if(alphabet == null || alphabet.size()== 0)throw new Exception("empty alphabet is not accepted");
        long timeBefore = System.currentTimeMillis();
        alphabet = new ArrayList<>(alphabet);
        NS.add(null);
        UtilityMethods.removeDuplicates(alphabet);
        /**
         * For example if alphabet = {2,4,1} then intersectingRegExp = [241]*
         */
        String intersectingRegExp = "[";
        for(int x:alphabet){
            if(x < 0 || x>9){
                throw new Exception("the input alphabet of an automaton generated from a regular expression must be a subset of {0,1,...,9}");
            }
            intersectingRegExp += x;
        }
        intersectingRegExp += "]*";
        regularExpression = "("+regularExpression+")&"+intersectingRegExp;
        dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
        dk.brics.automaton.Automaton M = RE.toAutomaton();
        M.minimize();
        /**
         * Recall that the alphabet is a set and does not allow repeated elements. However, the user might enter the command
         * reg myreg {1,1,0,0,0} "10*"; and therefore alphabet = [1,1,0,0,0]. So we need remove duplicates before we
         * move forward.
         */
        A.add(alphabet);
        alphabetSize = alphabet.size();
        NS.add(null);
        List<State> setOfStates = new ArrayList<>(M.getStates());
        Q = setOfStates.size();
        q0 = setOfStates.indexOf(M.getInitialState());
        for(int q = 0 ; q < Q;q++){
            State state = setOfStates.get(q);
            if(state.isAccept())O.add(1);
            else O.add(0);
            Int2ObjectRBTreeMap<IntList> currentStatesTransitions = new Int2ObjectRBTreeMap<>();
            d.add(currentStatesTransitions);
            for(Transition t: state.getTransitions()){
                for(char a = UtilityMethods.max(t.getMin(),'0');a <= UtilityMethods.min(t.getMax(),'9');a++){
                    if(alphabet.contains(a-'0')){
                        IntList dest = new IntArrayList();
                        dest.add(setOfStates.indexOf(t.getDest()));
                        currentStatesTransitions.put(alphabet.indexOf(a-'0'), dest);
                    }
                }
            }
        }
        long timeAfter = System.currentTimeMillis();
        String msg = "computed ~:" + Q + " states - "+(timeAfter-timeBefore)+"ms";
        System.out.println(msg);
    }

    public Automaton(
        String regularExpression,
        List<Integer> alphabet,
        NumberSystem numSys) throws Exception {
        this(regularExpression,alphabet);
        NS.set(0,numSys);
    }

    // This handles the generalised case of vectors such as "[0,1]*[0,0][0,1]"
    public Automaton(String regularExpression, List<List<Integer>> alphabet, Integer alphabetSize) throws Exception {

        this();

        if(alphabetSize > ((1<<Character.SIZE) -1)){
            throw new Exception("size of input alphabet exceeds the limit of " + ((1<<Character.SIZE) -1));
        }
        long timeBefore = System.currentTimeMillis();
        String intersectingRegExp = "[";
        for(int x=0; x<alphabetSize; x++){
            char nextChar = (char)(128 + x);
            intersectingRegExp += nextChar;
        }
        intersectingRegExp += "]*";
        regularExpression = "("+regularExpression+")&"+intersectingRegExp;
        dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
        dk.brics.automaton.Automaton M = RE.toAutomaton();
        M.minimize();
        this.setThisAutomatonToRepresent(M);
        // We added 128 to the encoding of every input vector before to avoid reserved characters, now we subtract it again
        // to get back the standard encoding
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for(int q = 0; q < Q;q++)new_d.add(new Int2ObjectRBTreeMap<>());
        for(int q = 0 ; q < Q;q++){
            for(int x:d.get(q).keySet()){
                new_d.get(q).put(x-128, d.get(q).get(x));
            }
        }
        d = new_d;
        long timeAfter = System.currentTimeMillis();
        String msg = "computed ~:" + Q + " states - "+(timeAfter-timeBefore)+"ms";
        System.out.println(msg);
    }

    /**
     * Takes an address and constructs the automaton represented by the file referred to by the address
     * @param address
     * @throws Exception
     */
    public Automaton(String address) throws Exception {
        this();
        final String REGEXP_FOR_WHITESPACE = "^\\s*$";

        //lineNumber will be used in error messages
        int lineNumber = 0;
        alphabetSize = 1;

        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(address), "utf-8"));
            String line;
            boolean[] singleton = new boolean[1];
            while((line = in.readLine())!= null) {
                lineNumber++;
                if(line.matches(REGEXP_FOR_WHITESPACE)) {
                    // Ignore blank lines.
                    continue;
                } else if(ParseMethods.parseTrueFalse(line, singleton)) {
                    // It is a true/false automaton.
                    TRUE_FALSE_AUTOMATON = true;
                    TRUE_AUTOMATON = singleton[0];
                    in.close();
                    return;
                } else {
                    boolean flag;
                    try {
                        flag = ParseMethods.parseAlphabetDeclaration(line, A, NS);
                    } catch(Exception e){
                        in.close();
                        throw new Exception(
                            e.getMessage() + UtilityMethods.newLine() +
                            "\t:line "+ lineNumber + " of file " + address);
                    }

                    if(flag) {
                        for(int i = 0; i < A.size();i++) {
                            if(NS.get(i) != null &&
                                (!A.get(i).contains(0) || !A.get(i).contains(1))) {
                                in.close();
                                throw new Exception(
                                    "The " + (i + 1) + "th input of type arithmetic " +
                                    "of the automaton declared in file " + address +
                                    " requires 0 and 1 in its input alphabet: line " +
                                    lineNumber);
                            }
                            UtilityMethods.removeDuplicates(A.get(i));
                            alphabetSize *= A.get(i).size();
                        }

                        break;
                    } else {
                        in.close();
                        throw new Exception(
                            "Undefined statement: line " +
                            lineNumber + " of file " + address);
                    }
                }
            }

            int[] pair = new int[2];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            int currentState = -1;
            int currentOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            TreeMap<Integer,Integer> state_output = new TreeMap<>();
            TreeMap<Integer,Int2ObjectRBTreeMap<IntList>> state_transition =
                new TreeMap<>();
            /**
             * This will hold all states that are destination of some transition.
             * Then we make sure all these states are declared.
             */
            Set<Integer> setOfDestinationStates = new HashSet<>();
            Q = 0;
            while((line = in.readLine())!= null) {
                lineNumber++;
                if(line.matches(REGEXP_FOR_WHITESPACE)) {
                    continue;
                }

                if(ParseMethods.parseStateDeclaration(line, pair)) {
                    Q++;
                    if(currentState == -1) {
                        q0 = pair[0];
                    }

                    currentState = pair[0];
                    currentOutput = pair[1];
                    state_output.put(currentState, currentOutput);
                    currentStateTransitions = new Int2ObjectRBTreeMap<>();
                    state_transition.put(currentState, currentStateTransitions);
                } else if(ParseMethods.parseTransition(line, input, dest)) {
                    setOfDestinationStates.addAll(dest);
                    if(currentState == -1){
                        in.close();
                        throw new Exception(
                            "Must declare a state before declaring a list of transitions: line " +
                            lineNumber + " of file " + address);
                    }

                    if(input.size() != A.size()) {
                        in.close();
                        throw new Exception("This automaton requires a " + A.size() +
                            "-tuple as input: line " + lineNumber + " of file " + address);
                    }
                    List<List<Integer>> inputs = expandWildcard(input);

                    for(List<Integer> i:inputs){
                        currentStateTransitions.put(encode(i), dest);
                    }

                    input = new ArrayList<>();
                    dest = new IntArrayList();
                }
                else{
                    in.close();
                    throw new Exception("Undefined statement: line "+ lineNumber + " of file " + address);
                }
            }
            in.close();
            for(int q:setOfDestinationStates) {
                if(!state_output.containsKey(q)) {
                    throw new Exception(
                        "State " + q + " is used but never declared anywhere in file: " + address);
                }
            }

            for(int q = 0; q < Q; q++) {
                O.add((int)state_output.get(q));
                d.add(state_transition.get(q));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("File does not exist: " + address);
        }
    }

    /**
     * returns a deep copy of this automaton.
     * @return a deep copy of this automaton
     */
    public Automaton clone(){
        Automaton M;
        if(TRUE_FALSE_AUTOMATON){
            M = new Automaton(TRUE_AUTOMATON);
            return M;
        }
        M = new Automaton();
        M.Q = Q;
        M.q0 = q0;
        M.alphabetSize = alphabetSize;
        M.canonized = canonized;
        M.labelSorted = labelSorted;
        for(int i = 0 ; i < A.size();i++){
            M.A.add(new ArrayList<>(A.get(i)));
            M.NS.add(NS.get(i));
            if(encoder != null && encoder.size()>0){
                if(M.encoder == null) M.encoder = new ArrayList<>();
                M.encoder.add(encoder.get(i));
            }
            if(label != null && label.size() == A.size())
                M.label.add(label.get(i));
        }
        for(int q = 0;q < Q;q++){
            M.O.add(O.getInt(q));
            M.d.add(new Int2ObjectRBTreeMap<>());
            for(int x:d.get(q).keySet()){
                M.d.get(q).put(x, new IntArrayList(d.get(q).get(x)));
            }
        }
        return M;
    }

    public void quantify(String labelToQuantify,boolean print, String prefix,StringBuilder log)throws Exception{
        Set<String> listOfLabelsToQuantify = new HashSet<>();
        listOfLabelsToQuantify.add(labelToQuantify);
        quantify(listOfLabelsToQuantify,print,prefix,log);
    }

    public void quantify(String labelToQuantify1,String labelToQuantify2,boolean leadingZeros,boolean print, String prefix,StringBuilder log)throws Exception{
        Set<String> listOfLabelsToQuantify = new HashSet<>();
        listOfLabelsToQuantify.add(labelToQuantify1);
        listOfLabelsToQuantify.add(labelToQuantify2);
        quantify(listOfLabelsToQuantify,print,prefix,log);
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
     * @param listOfLabelsToQuantify must contain at least one element. listOfLabelsToQuantify must be a subset of this.label.
     * @return
     */
    public void quantify(Set<String> listOfLabelsToQuantify, boolean print, String prefix,StringBuilder log)throws Exception{
        quantifyHelper(listOfLabelsToQuantify,print,prefix,log);
        if(TRUE_FALSE_AUTOMATON)return;

        boolean isMsd = true;
        boolean flag = false;
        for(NumberSystem ns:NS){
            if(ns == null)
                return;
            if(flag && (ns.isMsd() != isMsd) )
                return;
            isMsd = ns.isMsd();
            flag = true;
        }
        if(isMsd)
            AutomatonLogicalOps.fixLeadingZerosProblem(this, print,prefix,log);
        else
            AutomatonLogicalOps.fixTrailingZerosProblem(this, print,prefix,log);
    }

    /**
     * This method is very similar to public void quantify(Set<String> listOfLabelsToQuantify,boolean leadingZeros)throws Exception
     * with the exception that, this method does not deal with leading/trailing zeros problem.
     * @param listOfLabelsToQuantify
     * @throws Exception
     */
    private void quantifyHelper(
        Set<String> listOfLabelsToQuantify,
        boolean print,
        String prefix,
        StringBuilder log) throws Exception {
        if(listOfLabelsToQuantify.isEmpty() || label == null) {
            return;
        }

        String name_of_labels = "";
        for(String s:listOfLabelsToQuantify) {
            if(!label.contains(s)) {
                throw new Exception(
                    "Variable " + s + " in the list of quantified variables is not a free variable.");
            }

            if(name_of_labels.length() == 0) {
                name_of_labels += s;
            } else {
                name_of_labels += "," + s;
            }
        }
        long timeBefore = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "quantifying:" + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        /**
         * If this is the case, then the quantified automaton is either the true or false automaton.
         * It is true if this's language is not empty.
         */
        if(listOfLabelsToQuantify.size() == A.size()){
            if(isEmpty())
                TRUE_AUTOMATON = false;
            else
                TRUE_AUTOMATON = true;
            TRUE_FALSE_AUTOMATON = true;
            clear();
            return;
        }

        List<Integer> listOfInputsToQuantify = new ArrayList<>();//extract the list of indices of inputs we would like to quantify
        for(String l:listOfLabelsToQuantify)
            listOfInputsToQuantify.add(label.indexOf(l));
        List<List<Integer>> allInputs = new ArrayList<>();
        for(int i = 0; i < alphabetSize;i++)
            allInputs.add(decode(this, i));
        //now we remove those indices in listOfInputsToQuantify from A,T,label, and allInputs
        UtilityMethods.removeIndices(A,listOfInputsToQuantify);
        encoder = null;
        alphabetSize = 1;
        for(List<Integer> x:A)
            alphabetSize*=x.size();
        UtilityMethods.removeIndices(NS,listOfInputsToQuantify);
        UtilityMethods.removeIndices(label,listOfInputsToQuantify);
        for(List<Integer> i:allInputs)
            UtilityMethods.removeIndices(i,listOfInputsToQuantify);
        //example: permutation[1] = 7 means that encoded old input 1 becomes encoded new input 7
        List<Integer> permutation = new ArrayList<>();
        for(List<Integer> i:allInputs)
            permutation.add(encode(i));
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for(int q = 0; q < Q;q++){
            Int2ObjectRBTreeMap<IntList> newMemDransitionFunction = new Int2ObjectRBTreeMap<>();
            new_d.add(newMemDransitionFunction);
            for(int x:d.get(q).keySet()){
                int y = permutation.get(x);
                if(newMemDransitionFunction.containsKey(y))
                    UtilityMethods.addAllWithoutRepetition(newMemDransitionFunction.get(y),d.get(q).get(x));
                else
                    newMemDransitionFunction.put(y,new IntArrayList(d.get(q).get(x)));
            }
        }
        d = new_d;
        minimize(null, print,prefix +" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "quantified:" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
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
    public void reverse(boolean print, String prefix, StringBuilder log, boolean reverseMsd, boolean skipMinimize) throws Exception {
        if (TRUE_FALSE_AUTOMATON) {
            return;
        }

        long timeBefore = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "Reversing:" + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        // We change the direction of transitions first.
        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for(int q = 0; q < Q;q++)new_d.add(new Int2ObjectRBTreeMap<>());
        for(int q = 0 ; q < Q;q++){
            for(int x:d.get(q).keySet()){
                for(int dest:d.get(q).get(x)){
                    if(new_d.get(dest).containsKey(x))
                        new_d.get(dest).get(x).add(q);
                    else{
                        IntList destinationSet = new IntArrayList();
                        destinationSet.add(q);
                        new_d.get(dest).put(x, destinationSet);
                    }
                }
            }
        }
        d = new_d;
        IntSet setOfFinalStates = new IntOpenHashSet();
        /**final states become non final*/
        for(int q = 0 ; q < Q;q++){
            if(O.getInt(q) != 0){
                setOfFinalStates.add(q);
                O.set(q, 0);
            }
        }
        O.set(q0, 1);/**initial state becomes the final state.*/

        List<Int2IntMap> newMemD = subsetConstruction(null, setOfFinalStates,print,prefix+" ",log);

        if (!skipMinimize) {
            minimize(newMemD, print,prefix+" ",log);
        }

        // flip the number system from msd to lsd and vice versa.
        if (reverseMsd) {
            for (int i = 0; i < NS.size(); i++) {
                if (NS.get(i) == null) {
                    continue;
                }
                int indexOfUnderscore = NS.get(i).getName().indexOf("_");
                String msd_or_lsd = NS.get(i).getName().substring(0, indexOfUnderscore);
                String suffix = NS.get(i).getName().substring(indexOfUnderscore);
                String newName;
                if (msd_or_lsd.equals("msd")) {
                    newName = "lsd" + suffix;
                }
                else {
                    newName = "msd" + suffix;
                }
                NS.set(i, new NumberSystem(newName));
            }
        }

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "reversed:" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    public void reverse(boolean print, String prefix, StringBuilder log, boolean reverseMsd) throws Exception {
        reverse(print, prefix, log, reverseMsd, false);
    }

    public void reverse(boolean print, String prefix, StringBuilder log) throws Exception {
        reverse(print, prefix, log, false);
    }

    /**
     * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
     * @throws Exception
     */
    public void reverseWithOutput(boolean reverseMsd, boolean print, String prefix, StringBuilder log) throws Exception {
        if (TRUE_FALSE_AUTOMATON) {
            return;
        }
        try {
            long timeBefore = System.currentTimeMillis();
            if(print) {
                String msg = prefix + "reversing: " + Q + " states";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

            boolean addedDeadState = addDistinguishedDeadState(print, prefix, log);

            int minOutput = 0;
            if (addedDeadState) {
                // get state with smallest output. all states with this output will be removed.
                // after transducing, all states with this minimum output will be removed.
                
                for (int i = 0; i < O.size(); i++) {
                    if (O.getInt(i) < minOutput) {
                        minOutput = O.getInt(i);
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

            for (int i = 0; i < Q; i++) {
                newInitState.put(i, O.getInt(i));
            }

            newStates.add(newInitState);

            newStatesHash.put(newInitState, newStates.size() - 1);

            newStatesQueue.add(newInitState);

            while (newStatesQueue.size() > 0) {
                Map<Integer, Integer> currState = newStatesQueue.remove();

                // set up the output of this state to be g(q0), where g = currState.
                newO.add((int)currState.get(q0));

                newD.add(new Int2ObjectRBTreeMap<>());

                // assume that the
                // System.out.println("alphabet: " + d.get(q0) + ", " + d + ", " + alphabetSize);
                if (d.get(q0).keySet().size() != alphabetSize) {
                    throw new Exception("Automaton should be deterministic!");
                }
                for (int l : d.get(q0).keySet()) {
                    Map<Integer, Integer> toState = new HashMap<>();

                    for (int i = 0; i < Q; i++) {
                        toState.put(i, currState.get(d.get(i).get(l).getInt(0)));
                    }

                    if (!newStatesHash.containsKey(toState)) {
                        newStates.add(toState);
                        newStatesHash.put(toState, newStates.size() - 1);
                        newStatesQueue.add(toState);
                    }

                    // set up the transition.
                    IntList newList = new IntArrayList();
                    newList.add((int)newStatesHash.get(toState));
                    newD.get(newD.size() - 1).put(l, newList);
                }
            }

            Q = newStates.size();

            O = newO;

            d = newD;

            // flip the number system from msd to lsd and vice versa.
            if (reverseMsd) {
                for (int i = 0; i < NS.size(); i++) {
                    if (NS.get(i) == null) {
                        continue;
                    }
                    int indexOfUnderscore = NS.get(i).getName().indexOf("_");
                    String msd_or_lsd = NS.get(i).getName().substring(0, indexOfUnderscore);
                    String suffix = NS.get(i).getName().substring(indexOfUnderscore);
                    String newName;
                    if (msd_or_lsd.equals("msd")) {
                        newName = "lsd" + suffix;
                    }
                    else {
                        newName = "msd" + suffix;
                    }
                    NS.set(i, new NumberSystem(newName));
                }
            }

            minimizeSelfWithOutput(print, prefix+" ", log);

            if (addedDeadState) {
                // remove all states that have an output of minOutput
                HashSet<Integer> statesRemoved = new HashSet<>();

                for (int q = 0; q < Q; q++) {
                    if (O.getInt(q) == minOutput) {
                        statesRemoved.add(q);
                    }
                }
                for (int q = 0; q < Q; q++) {

                    Iterator<Integer> iter = d.get(q).keySet().iterator();

                    while (iter.hasNext()) {
                        int x = iter.next();

                        if (statesRemoved.contains(d.get(q).get(x).getInt(0))) {
                            iter.remove();
                        }
                    }
                }

                canonized = false;
                canonize();
            }

            long timeAfter = System.currentTimeMillis();
            if(print){
                String msg = prefix + "reversed: " + Q + " states - "+(timeAfter-timeBefore)+"ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error reversing word automaton");
        }
    }



    /**
     * Convert the number system of a word automaton from [msd/lsd]_k^i to [msd/lsd]_k^j.
     * @throws Exception
     */
    public void convert(boolean toMsd, int toBase, boolean print,
                                    String prefix, StringBuilder log) throws Exception {
        try {
            // assume that the format of ns is in the form msd_k or lsd_k. let the Prover.java check it with a regex.

            if (NS.size() != 1) {
                throw new Exception("Automaton must have one input to be converted.");
            }

            String nsName = NS.get(0).getName();

            String base = nsName.substring(nsName.indexOf("_") + 1);

            if(!UtilityMethods.isNumber(base) || Integer.parseInt(base) <= 1) {
                throw new Exception("Base of number system of original automaton must be a positive integer greater than 1.");
            }

            int fromBase = Integer.parseInt(base);

            if (fromBase == toBase) {
                if (NS.get(0).isMsd() == toMsd) {
                    throw new Exception("New and old number systems " + NS.get(0).getName() +
                            " to be converted cannot be equal.");
                }
                else {
                    // if all that differs is msd/lsd, just reverse the automaton.
                    reverseWithOutput(true, print, prefix+" ", log);
                    return;
                }
            }

            // check that the bases are powers of the same integer, and figure out that integer.
            // make a different function for this.
            int commonRoot = UtilityMethods.commonRoot(fromBase, toBase);

            if (commonRoot == -1) {
                throw new Exception("New and old number systems must have bases of the form k^i and k^j for integers i, j, k.");
            }

            // if originally in lsd, reverse and convert to msd.
            if (!NS.get(0).isMsd()) {
                reverseWithOutput(true, print, prefix+" ", log);
            }

            // run  convert algorithm assuming MSD.

            boolean currentlyReversed = false;

            if (fromBase != commonRoot) {
                // do k^i to k construction.

                int exponent = (int)(Math.log(fromBase) / Math.log(commonRoot));

                reverseWithOutput(true, print, prefix+" ", log);

                currentlyReversed = true;

                convertLsdBaseToRoot(commonRoot, exponent, print, prefix+" ", log);

                minimizeSelfWithOutput(print, prefix+" ", log);

                // reverseWithOutput(true, print, prefix+" ", log);
            }

            if (toBase != commonRoot) {
                // do k to k^i construction.

                if (currentlyReversed) {
                    reverseWithOutput(true, print, prefix+" ", log);
                    currentlyReversed = false;
                }

                int exponent = (int)(Math.log(toBase) / Math.log(commonRoot));

                convertMsdBaseToExponent(exponent, print, prefix+" ", log);

                minimizeSelfWithOutput(print, prefix+" ", log);
            }

            // if desired base is in lsd, reverse again to lsd.
            if (toMsd == currentlyReversed) {
                reverseWithOutput(true, print, prefix+" ", log);
                currentlyReversed = !currentlyReversed;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error converting the number system of an automaton");
        }
    }

    /**
     * Assuming this automaton is in number system msd_k with one input, convert it to number system msd_k^j with one
     * input.
     * Used as a helper method in convert()
     * @param exponent
     * @throws Exception
     */
    private void convertMsdBaseToExponent(int exponent, boolean print,
                                          String prefix, StringBuilder log) throws Exception {
        try {

            String nsName = NS.get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: msd_" + base + " to msd_" + (int)Math.pow(base, exponent) + ", " + Q + " states";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

            List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

            // need to generate the new morphism, which is h^{exponent}, where h is the original morphism.

            List<List<Integer>> prevMorphism = new ArrayList<>();

            for (int q = 0; q < Q; q++) {
                List<Integer> morphismString = new ArrayList<>();

                if (d.get(q).keySet().size() != alphabetSize) {
                    throw new Exception("Automaton must be deterministic");
                }

                for (int di = 0; di < alphabetSize; di++) {
                    morphismString.add(d.get(q).get(di).getInt(0));
                }
                prevMorphism.add(morphismString);
            }

            for (int i = 2; i <= exponent; i++) {

                List<List<Integer>> newMorphism = new ArrayList<>();

                for (int j = 0; j < Q; j++) {
                    List<Integer> morphismString = new ArrayList<>();
                    for (int k = 0; k < prevMorphism.get(j).size(); k++) {
                        for (int di : d.get(j).keySet()) {
                            morphismString.add(d.get(prevMorphism.get(j).get(k)).get(di).getInt(0));
                        }
                    }
                    newMorphism.add(morphismString);
                }
                prevMorphism = new ArrayList<>(newMorphism);
            }

            for (int q = 0; q < Q; q++) {
                newD.add(new Int2ObjectRBTreeMap<>());
                for (int di = 0; di < prevMorphism.get(q).size(); di++) {

                    int toState = prevMorphism.get(q).get(di);

                    // set up transition
                    IntList newList = new IntArrayList();
                    newList.add(toState);
                    newD.get(q).put(di, newList);
                }
            }

            d = newD;

            // change number system too.

            NS.set(0, new NumberSystem("msd_" + (int)(Math.pow(base, exponent))));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < (int)(Math.pow(base, exponent)); i++) {
                ints.add(i);
            }
            A = Arrays.asList(ints);
            alphabetSize = ints.size();

            if (print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + "Converted: msd_" + base + " to msd_" + (int)(Math.pow(base, exponent)) + ", " + Q + " states - " + (timeAfter-timeBefore) + "ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

        } catch(Exception e) {
            e.printStackTrace();
            throw new Exception("Error converting the number system msd_k of an automaton to msd_k^j");
        }

    }

    /**
     * Assuming this automaton is in number system lsd_k^j with one input, convert it to number system lsd_k with one
     * input.
     * Used as a helper method in convert()
     * @param exponent
     * @throws Exception
     */
    private void convertLsdBaseToRoot(int root, int exponent, boolean print,
                                          String prefix, StringBuilder log) throws Exception {

        try {
            String nsName = NS.get(0).getName();
            int base = Integer.parseInt(nsName.substring(nsName.indexOf("_") + 1));

            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "Converting: lsd_" + base + " to lsd_" + (int)Math.pow(root, exponent) + ", " + Q + " states";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

            if (base != Math.pow(root, exponent)) {
                throw new Exception("Base of automaton must be equal to the given root to the power of the given exponent.");
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

            StateTuple initState = new StateTuple(q0, Arrays.asList());

            newStates.add(initState);
            newStatesQueue.add(initState);
            newStatesHash.put(initState, newStates.size() - 1);


            while (newStatesQueue.size() > 0) {
                StateTuple currState = newStatesQueue.remove();

                newD.add(new Int2ObjectRBTreeMap<>());

                if (currState.string.size() == 0) {
                    newO.add(O.getInt(currState.state));
                }
                else {
                    int stringValue = 0;

                    for (int i = 0; i < currState.string.size(); i++) {
                        stringValue += currState.string.get(i) * (int)(Math.pow(root, i));
                    }

                    // set up output
                    newO.add(O.getInt(d.get(currState.state).get(stringValue).getInt(0)));
                }

                for (int di = 0; di < root; di++) {

                    StateTuple toState;

                    List<Integer> toStateString = new ArrayList<>(currState.string);

                    toStateString.add(di);

                    if (currState.string.size() < exponent - 1) {
                        toState = new StateTuple(currState.state, toStateString);
                    }
                    else {

                        int toStateStringValue = 0;

                        for (int i = 0; i < toStateString.size(); i++) {
                            toStateStringValue += toStateString.get(i) * (int)(Math.pow(root, i));
                        }

                        toState = new StateTuple(d.get(currState.state).get(toStateStringValue).getInt(0), Arrays.asList());
                    }


                    if (!newStatesHash.containsKey(toState)) {
                        newStates.add(toState);
                        newStatesQueue.add(toState);
                        newStatesHash.put(toState, newStates.size() - 1);
                    }

                    IntList newList = new IntArrayList();
                    newList.add((int)newStatesHash.get(toState));
                    newD.get(newD.size() - 1).put(di, newList);

                }
            }

            Q = newStates.size();

            O = newO;

            d = newD;

            canonized = false;

            // change number system too.
            NS.set(0, new NumberSystem("lsd_" + root));

            ArrayList<Integer> ints = new ArrayList<>();
            for (int i = 0; i < root; i++) {
                ints.add(i);
            }
            A = Arrays.asList(ints);
            alphabetSize = ints.size();

            if(print) {
                long timeAfter = System.currentTimeMillis();
                String msg = prefix + prefix + "Converted: lsd_" + base + " to lsd_" + (int)(Math.pow(root, exponent)) + ", " + Q + " states - " + (timeAfter-timeBefore) + "ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error converting the number system msd_k^j of an automaton to msd_k");
        }


    }

    public boolean equals(Automaton M)throws Exception{
        if(M == null)return false;
        if(TRUE_FALSE_AUTOMATON != M.TRUE_FALSE_AUTOMATON)return false;
        if(TRUE_FALSE_AUTOMATON && M.TRUE_FALSE_AUTOMATON){
            if(TRUE_AUTOMATON != M.TRUE_AUTOMATON)return false;
            return true;
        }
        dk.brics.automaton.Automaton Y = M.to_dk_bricks_automaton();
        dk.brics.automaton.Automaton X = to_dk_bricks_automaton();
        return X.equals(Y);
    }

    /**
     * Either perform the union or intersection of a list of automata.
     * @param automataNames - list of automata names, saved in Automata Library
     * @param op - either "union" or "intersect"
     * @param print
     * @param prefix
     * @param log
     * @return The union/intersection of all automata in automataNames and this automaton
     * @throws Exception
     */
    public Automaton unionOrIntersect(List<String> automataNames, String op, boolean print, String prefix, StringBuilder log) throws Exception {
        Automaton first = this.clone();

        for (int i = 0; i < automataNames.size(); i++) {
            long timeBefore = System.currentTimeMillis();
            Automaton N = new Automaton(UtilityMethods.get_address_for_automata_library()+automataNames.get(i)+".txt");

            // ensure that N has the same number system as first.
            boolean differingNS = false;
            if (N.NS.size() != first.NS.size()) {
                differingNS = true;
            }
            else {
                for (int j = 0; j < N.NS.size(); j++) {
                    if (
                        (N.NS.get(j) == null && first.NS.get(j) != null) ||
                        (N.NS.get(j) != null && first.NS.get(j) == null) ||
                        (N.NS.get(j) != null && first.NS.get(j) != null && 
                        !N.NS.get(j).getName().equals(first.NS.get(j).getName())) ||
                        !N.A.equals(first.A)
                    ) {
                        differingNS = true;
                        break;
                    }
                }
            }
            if (differingNS) {
                throw new Exception("Automata to be unioned must have the same number system(s).");
            }

            // crossProduct requires labelling so we make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            N.label = first.label;

            if (op.equals("union")) {
                first = AutomatonLogicalOps.or(first, N, print, prefix, log);
            }
            else if (op.equals("intersect")) {
                first = AutomatonLogicalOps.and(first, N, print, prefix, log);
            }
            else {
                throw new Exception("Internal union/intersect error");
            }


            long timeAfter = System.currentTimeMillis();
            if(print){
                String msg = prefix + "computed =>:" + first.Q + " states - "+(timeAfter-timeBefore)+"ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }
        }
        return first;
    }

    public Automaton combine(List<String> automataNames, IntList outputs, boolean print, String prefix, StringBuilder log) throws Exception {
        Queue<Automaton> subautomata =  new LinkedList<>();

        for (String name : automataNames) {
			Automaton M = new Automaton(UtilityMethods.get_address_for_automata_library()+name+".txt");
			subautomata.add(M);
		}
        return combine(subautomata, outputs, print, prefix, log);
    }

    public Automaton combine(Queue<Automaton> subautomata, IntList outputs, boolean print, String prefix, StringBuilder log) throws Exception {

        Automaton first = this.clone();

        // In an automaton without output, every non-zero output value represents an accepting state
        // we change this to correspond to the value assigned to the first automaton by our command
        for (int q = 0; q < first.Q; q++) {
            if (first.O.getInt(q) != 0) {
                first.O.set(q, outputs.getInt(0));
            }
        }
        first.combineIndex = 1;
        first.combineOutputs = outputs;
        while (subautomata.size() > 0) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            if (print) {
                String msg = prefix + "computing =>:" + first.Q + " states - " + next.Q + " states";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

            // crossProduct requires labelling so we make an arbitrary labelling and use it for both: this is valid since
            // input alphabets and arities are assumed to be identical for the combine method
            first.randomLabel();
            next.label = first.label;
            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            AutomatonLogicalOps.totalize(first, print,prefix+" ",log);
            AutomatonLogicalOps.totalize(next, print,prefix+" ",log);
            Automaton product = AutomatonLogicalOps.crossProduct(first, next, "combine", print, prefix+" ", log);
            product.combineIndex = first.combineIndex + 1;
            product.combineOutputs = first.combineOutputs;
            first = product;

            long timeAfter = System.currentTimeMillis();
            if(print){
                String msg = prefix + "computed =>:" + first.Q + " states - "+(timeAfter-timeBefore)+"ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }
        }

        // totalize the resulting automaton
        AutomatonLogicalOps.totalize(first, print, prefix+" ", log);
        first.canonizeAndApplyAllRepresentationsWithOutput(print, prefix+" ", log);

        return first;
    }

    // For use in the "combine" command.
    public void canonizeAndApplyAllRepresentationsWithOutput(boolean print, String prefix, StringBuilder log) throws Exception {
        this.canonized = false;
        this.canonize();
        this.applyAllRepresentationsWithOutput(print, prefix, log);
    }

    /**
     * @param outputs A list of integers, indicating which uncombined automata and in what order to return.
     * @return A list of automata, each corresponding to the list of outputs.
     * For the sake of an example, suppose that outputs is [0,1,2], then we return the list of automaton without output
     * which accepts if the output in our automaton is 0,1 or 2 respectively.
     * @throws Exception
     */
    public List<Automaton> uncombine(List<Integer> outputs, boolean print, String prefix, StringBuilder log) throws Exception {
        List<Automaton> automata = new ArrayList<>();
        for (Integer output : outputs) {
            Automaton M = clone();
            for (int j = 0; j < M.O.size(); j++) {
                if (M.O.getInt(j) == output) {
                    M.O.set(j, 1);
                } else {
                    M.O.set(j, 0);
                }
            }
            automata.add(M);
        }
        return automata;
    }


    /**
     * @return A minimized DFA with output recognizing the same language as the current DFA (possibly also with output).
     * We minimize a DFA with output by first uncombining into automata without output, minimizing the uncombined automata, and
     * then recombining. It follows that if the uncombined automata are minimal, then the combined automata is also minimal
     * @throws Exception
     */
    public Automaton minimizeWithOutput(boolean print, String prefix, StringBuilder log) throws Exception {
        IntList outputs = new IntArrayList(O);
        UtilityMethods.removeDuplicates(outputs);
        List<Automaton> subautomata = uncombine(outputs,print,prefix,log);
        for (Automaton subautomaton : subautomata) {
            subautomaton.minimize(null, print, prefix, log);
        }
        Automaton N = subautomata.remove(0);
        List<String> label = new ArrayList<>(N.label); // We keep the old labels, since they are replaced in the combine
        N = N.combine(new LinkedList<>(subautomata),outputs,print, prefix,log);
        N.label = label;
        return N;
    }

    public void minimizeSelfWithOutput(boolean print, String prefix, StringBuilder log) throws Exception {
        Automaton N = minimizeWithOutput(print, prefix, log);
        copy(N);
    }

    public void normalizeNumberSystems(boolean print, String prefix, StringBuilder log) throws Exception {
        // set all the number systems to be null.
        boolean switchNS = false;
        List<NumberSystem> numberSystems = new ArrayList<>();
        for (int i = 0; i < NS.size(); i++) {
            if (NS.get(i) != null && NS.get(i).should_we_use_allRepresentations()) {
                switchNS = true;
                int max = Collections.max(A.get(i));
                numberSystems.add(new NumberSystem((NS.get(i).isMsd() ? "msd_" : "lsd_") + (max+1)));
            }
            else {
                numberSystems.add(NS.get(i));
            }
        }

        if (switchNS) {
            setAlphabet(false, numberSystems, A, print, prefix, log);

            // do this whether or not you print!
            String msg = prefix + "WARN: The alphabet of the resulting automaton was changed. Use the alphabet command to change as desired.";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

    }

     public Automaton star(boolean print, String prefix, StringBuilder log) throws Exception {
         long timeBefore = System.currentTimeMillis();
         if (print) {
             String msg = prefix + "star: " + Q + " state automaton";
             log.append(msg + UtilityMethods.newLine());
             System.out.println(msg);
         }

         // this will be the returned automaton.
         Automaton N = clone();

         // We clone the current automaton and add a new state which will be our new initial state.
         // We will then canonize the resulting automaton after.
         int newState = N.Q;
         N.O.add(1); // The newly added state is a final state.
         N.d.add(new Int2ObjectRBTreeMap<>());
         for (int q = 0; q < N.Q; q++) {
             if (N.O.getInt(q) == 0) { // if it is NOT a final state
                 continue;
             }
             // otherwise, it is a final state, and we add our transitions.
             for (int x : d.get(q0).keySet()) {
                 if (N.d.get(q).containsKey(x)) {
                     N.d.get(q).get(x).addAll(N.d.get(q).get(x).size(), d.get(q0).get(x));
                 }
                 else {
                     N.d.get(q).put(x, new IntArrayList(d.get(q0).get(x)));
                 }
             }
         }
         for (int x : d.get(q0).keySet()) {
             N.d.get(newState).put(x, new IntArrayList(d.get(q0).get(x)));
         }

         N.Q++;
         N.q0 = newState;

         N.normalizeNumberSystems(print, prefix, log);

         N.canonized = false;
         N.canonize();

         N.minimize(null, print, prefix, log);
         N.applyAllRepresentations();

         long timeAfter = System.currentTimeMillis();
         if(print){
             String msg = prefix + "star complete: " + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
             log.append(msg + UtilityMethods.newLine());
             System.out.println(msg);
         }

         return N;
     }

     // concatenate
    public Automaton concat(List<String> automataNames, boolean print, String prefix, StringBuilder log) throws Exception {
        Automaton first = this.clone();

        for (int i = 0; i < automataNames.size(); i++) {
            long timeBefore = System.currentTimeMillis();
            Automaton N = new Automaton(UtilityMethods.get_address_for_automata_library()+automataNames.get(i)+".txt");


            first = first.concat(N, print, prefix, log);


            long timeAfter = System.currentTimeMillis();
            if(print){
                String msg = prefix + "concatenated =>:" + first.Q + " states - "+(timeAfter-timeBefore)+"ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }
        }
        return first;
    }

    public Automaton concat(Automaton other, boolean print, String prefix, StringBuilder log) throws Exception {

        long timeBefore = System.currentTimeMillis();
        if (print) {
            String msg = prefix + "concat: " + Q + " state automaton with " + other.Q + " state automaton";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        // ensure that N has the same number system as first.
        boolean differingNS = false;
        if (other.NS.size() != NS.size()) {
            differingNS = true;
        }
        else {
            for (int j = 0; j < other.NS.size(); j++) {
                if (
                        (other.NS.get(j) == null && NS.get(j) != null) ||
                                (other.NS.get(j) != null && NS.get(j) == null) ||
                                (other.NS.get(j) != null && NS.get(j) != null &&
                                        !other.NS.get(j).getName().equals(NS.get(j).getName())) ||
                                !A.equals(other.A)
                ) {
                    differingNS = true;
                    break;
                }
            }
        }
        if (differingNS) {
            throw new Exception("Automata to be concatenated must have the same number system(s).");
        }

        Automaton N = clone();

        int otherQ0 = Q;

        // to access the other's states, just do q. To access the other's states in N, do otherQ0 + q.
        for (int q = 0; q < other.Q; q++) {
            N.O.add(other.O.getInt(q)); // add the output
            N.d.add(new Int2ObjectRBTreeMap<>());
            for (int x : other.d.get(q).keySet()) {
                IntArrayList newTransitionMap = new IntArrayList();
                for (int i = 0; i < other.d.get(q).get(x).size(); i++) {
                    newTransitionMap.add(other.d.get(q).get(x).getInt(i) + otherQ0);
                }
                N.d.get(otherQ0 + q).put(x, newTransitionMap);
            }
        }

        // now iterate through all of self's states. If they are final, add a transition to wherever the other's
        // initial state goes.
        for (int q = 0; q < Q; q++) {
            if (N.O.getInt(q) == 0) { // if it is NOT a final state
                continue;
            }

            // otherwise, it is a final state, and we add our transitions.
            for (int x : N.d.get(otherQ0).keySet()) {
                if (N.d.get(q).containsKey(x)) {
                    N.d.get(q).get(x).addAll(N.d.get(q).get(x).size(), N.d.get(otherQ0).get(x));
                }
                else {
                    N.d.get(q).put(x, new IntArrayList(N.d.get(otherQ0).get(x)));
                }
            }
        }

        N.Q = Q + other.Q;

        N.normalizeNumberSystems(print, prefix, log);

        N.minimize(null, print, prefix, log);
        N.applyAllRepresentations();

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "concat complete: " + N.Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        return N;
    }


    public void setAlphabet(boolean isDFAO, List<NumberSystem> numberSystems, List<List<Integer>> alphabet, boolean print, String prefix, StringBuilder log) throws Exception {

        if (alphabet.size() != A.size()) {
            throw new Exception("The number of alphabets must match the number of alphabets in the input automaton.");
        }
        if (alphabet.size() != numberSystems.size()) {
            throw new Exception("The number of alphabets must match the number of number systems.");
        }

        long timeBefore = System.currentTimeMillis();
        if (print) {

            ArrayList<String> nsNames = new ArrayList<>();

            for (int i = 0; i < numberSystems.size(); i++) {
                if (numberSystems.get(i) == null) {
                    nsNames.add(alphabet.get(i).toString());
                }
                else {
                    nsNames.add(numberSystems.get(i).toString());
                }
            }

            String msg = prefix + "setting alphabet to " + nsNames.toString();
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }


        Automaton M = clone();
        M.A = alphabet;
        M.NS = numberSystems;
        M.alphabetSize = 1;
        for(List<Integer> x : M.A) {
            M.alphabetSize *= x.size();
        }
        M.setupEncoder();

        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

        for (int q = 0; q < M.Q; q++) {
            Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
            for (int x : d.get(q).keySet()) {
                List<Integer> decoded = decode(this, x);

                boolean inNewAlphabet = true;

                for (int i = 0; i < decoded.size(); i++) {
                    if (!alphabet.get(i).contains(decoded.get(i))) {
                        inNewAlphabet = false;
                        break;
                    }
                }
                if (inNewAlphabet) {
                    newMap.put(M.encode(decoded), d.get(q).get(x));
                }
            }
            newD.add(newMap);
        }
        M.d = newD;

        if (isDFAO) {
            M.minimizeSelfWithOutput(print, prefix, log);
        }
        else {
            M.minimize(null, print, prefix, log);
        }

        M.canonizeAndApplyAllRepresentationsWithOutput(print, prefix+" ", log);

        copy(M);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "set alphabet complete:" + (timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    // Determines whether an automaton accepts infinitely many values. If it does, a regex of infinitely many accepted values (not all)
    // is given. This is true iff there exists a cycle in a minimized version of the automaton, which previously had leading or
    // trailing zeroes removed according to whether it was msd or lsd
    public String infinite() {
        for (int i=0; i<Q; i++) {
            visited = new IntOpenHashSet();
            started = i;
            String cycle = infiniteHelper(i, "");
            // once a cycle is detected, we compute a prefix leading to state i and a suffix from state i to an accepting state
            if(cycle != "") {
                return constructPrefix(i) + "(" + cycle + ")*" + constructSuffix(i);
            }
        }
        return ""; // an empty string signals that we have failed to find a cycle
    }

    // helper function for our DFS to facilitate recursion
    private String infiniteHelper(int state, String result) {
        if(visited.contains(state)) {
            if(state == started) {
                return result;
            }
            return "";
        }
        visited.add(state);
        for (int x : d.get(state).keySet()) {
            for (Integer y : d.get(state).get(x)) {
                // this adds brackets even when inputs have arity 1 - this is fine, since we just want a usable infinite regex
                String cycle = infiniteHelper(y, result + decode(this, x).toString());
                if (cycle != "") {
                    return cycle;
                }
            }
        }
        visited.remove(state);
        return "";
    }

    /**
     * @param inputs A list of "+", "-" or "". Indicating how our input will be interpreted in the output automata.
     *               Inputs must correspond to inputs of the current automaton
     *               which can be compared to some corresponding negative base.
     * @return The automaton which replaces inputs in negative base with an input in corresponding comparable positive base.
     * For sake of example, suppose the input is [+,-,] and M is the current automata with inputs in base -2.
     * On inputs (x,y,z), where x,y are inputs in base 2, the automaton gives as output M(x',y',z) where
     * x' and y' are in the corresponding base -2 representations of x and -y.
     * @throws Exception
     */
    public Automaton split(List<String> inputs, boolean print, String prefix, StringBuilder log) throws Exception {
        if(alphabetSize == 0) {
            throw new Exception("Cannot split automaton with no inputs.");
        }
        if(inputs.size() != A.size()) {
            throw new Exception("Split automaton has incorrect number of inputs.");
        }

        Automaton M = clone();
        Set<String> quantifiers = new HashSet<>();
        // We label M [b0,b1,...,b(A.size()-1)]
        if(M.label == null || M.label.size() > 0) M.label = new ArrayList<>();
        for(int i = 0 ; i < A.size();i++){
            M.label.add("b" + i);
        }
        for(int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).equals("")) {
                if (NS.get(i) == null)
                    throw new Exception("Number system for input " + i + " must be defined.");
                NumberSystem negativeNumberSystem;
                if (NS.get(i).is_neg) {
                    negativeNumberSystem = NS.get(i);
                } else {
                    try {
                        negativeNumberSystem = NS.get(i).negative_number_system();
                    } catch (Exception e) {
                        throw new Exception("Negative number system for " + NS.get(i) + " must be defined");
                    }
                }
                negativeNumberSystem.setBaseChange();
                if (negativeNumberSystem.baseChange == null) {
                    throw new Exception("Number systems " + NS.get(i) + " and " + negativeNumberSystem + " cannot be compared.");
                }
                
                Automaton baseChange = negativeNumberSystem.baseChange.clone();
                String a = "a"+i, b = "b"+i, c = "c"+i;
                if (inputs.get(i).equals("+")) {
                    baseChange.bind(a, b);
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    quantifiers.add(b);
                } else { // inputs.get(i).equals("-")
                    baseChange.bind(a, c);
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    M = AutomatonLogicalOps.and(M, negativeNumberSystem.arithmetic(b,c,0,"+"), print, prefix, log);
                    quantifiers.add(b); quantifiers.add(c);
                }
            }
        }
        M.quantify(quantifiers, print, prefix, log);
        M.sortLabel();
        M.randomLabel();
        return M;
    }

    /**
     * @param inputs A list of "+", "-" or "". Indicating how our input will be interpreted in the output automata.
     *               Inputs must correspond to inputs of the current automaton
     *               which can be compared to some corresponding negative base.
     * @return The automaton which replaces inputs in positive base with an input in corresponding comparable negative base.
     * For sake of example, suppose the input is [+,-,] and M is the current automata with inputs in base 2.
     * On inputs (x,y,z), where x,y are inputs in base -2, the automaton gives as output M(x',y',z) where
     * x' and y' are in the corresponding base 2 representations of x and -y. If x or -y has no corresponding
     * base 2 representation, then the automaton outputs 0.
     * @throws Exception
     */
    public Automaton reverseSplit(List<String> inputs, boolean print, String prefix, StringBuilder log) throws Exception {
        if(alphabetSize == 0) {
            throw new Exception("Cannot reverse split automaton with no inputs.");
        }
        if(inputs.size() != A.size()) {
            throw new Exception("Split automaton has incorrect number of inputs.");
        }

        Automaton M = clone();
        Set<String> quantifiers = new HashSet<>();
        // We label M [b0,b1,...,b(A.size()-1)]
        if(M.label == null)M.label = new ArrayList<>();
        else if(M.label.size() > 0)M.label = new ArrayList<>();
        for(int i = 0 ; i < A.size();i++){
            M.label.add("b" + i);
        }
        for(int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).equals("")) {
                if (NS.get(i) == null)
                    throw new Exception("Number system for input " + i + " must be defined.");
                NumberSystem negativeNumberSystem;
                if (NS.get(i).is_neg) {
                    negativeNumberSystem = NS.get(i);
                } else {
                    try {
                        negativeNumberSystem = NS.get(i).negative_number_system();
                    } catch (Exception e) {
                        throw new Exception("Negative number system for " + NS.get(i) + " must be defined");
                    }
                }
                negativeNumberSystem.setBaseChange();
                if (negativeNumberSystem.baseChange == null) {
                    throw new Exception("Number systems " + NS.get(i) + " and " + negativeNumberSystem + " cannot be compared.");
                }

                Automaton baseChange = negativeNumberSystem.baseChange.clone();
                String a = "a"+i, b = "b"+i, c = "c"+i;
                if (inputs.get(i).equals("+")) {
                    baseChange.bind(b, a);
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    quantifiers.add(b);
                } else { // inputs.get(i).equals("-")
                    baseChange.bind(b, c);
                    M = AutomatonLogicalOps.and(M, baseChange, print, prefix, log);
                    M = AutomatonLogicalOps.and(M, negativeNumberSystem.arithmetic(a,c,0,"+"), print, prefix, log);
                    quantifiers.add(b); quantifiers.add(c);
                }
            }
        }
        M.quantify(quantifiers, print, prefix, log);
        M.sortLabel();
        M.randomLabel();
        return M;
    }


    /**
     * @param subautomata A queue of automaton which we will "join" with the current automaton.
     * @return The cross product of the current automaton and automaton in subautomata, using the operation "first" on the outputs.
     * For sake of example, the current Automaton is M1, and subautomata consists of M2 and M3.
     * Then on input x, returned automaton should output the first non-zero value of [ M1(x), M2(x), M3(x) ].
     * @throws Exception
     */
    public Automaton join(Queue<Automaton> subautomata, boolean print, String prefix, StringBuilder log) throws Exception {
        Automaton first = this.clone();

        while (subautomata.size() > 0) {
            Automaton next = subautomata.remove();
            long timeBefore = System.currentTimeMillis();
            if(print){
                String msg = prefix + "computing =>:" + first.Q + " states - " + next.Q + " states";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }

            // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
            AutomatonLogicalOps.totalize(first, print,prefix+" ",log);
            AutomatonLogicalOps.totalize(next, print,prefix+" ",log);
            first = AutomatonLogicalOps.crossProduct(first, next, "first", print, prefix+" ", log);
            first = first.minimizeWithOutput(print,prefix+" ",log);

            long timeAfter = System.currentTimeMillis();
            if(print){
                String msg = prefix + "computed =>:" + first.Q + " states - "+(timeAfter-timeBefore)+"ms";
                log.append(msg + UtilityMethods.newLine());
                System.out.println(msg);
            }
        }
        return first;
    }

    // helper function for inf, finds an input string that leads from q0 to the specified state
    private String constructPrefix(Integer target) {
        List<Integer> distance = new ArrayList<>(Collections.nCopies(Q, -1));
        List<Integer> prev = new ArrayList<>(Collections.nCopies(Q, -1));
        List<Integer> input = new ArrayList<>(Collections.nCopies(Q, -1));
        int counter = 0;
        boolean found = false;
        distance.set(q0, 0);

        // we very well could have no prefix
        if (q0 == target) {
            return "";
        }
        while(!found) {
            for (int i=0; i<Q; i++) {
                if (distance.get(i)!=counter)
                    continue;
                for (int x : d.get(i).keySet()) {
                    for (int y : d.get(i).get(x)) {
                        if (y == target)
                            found = true;
                        if (distance.get(y) == -1) {
                            distance.set(y, counter+1);
                            prev.set(y, i);
                            input.set(y, x);
                        }
                    }
                }
            }
            counter++;
        }
        List<Integer> path = new ArrayList<>();
        Integer current = target;

        while (current != q0) {
            path.add(input.get(current));
            current = prev.get(current);
        }
        Collections.reverse(path);
        String result = "";
        for (Integer node : path) {
            result += decode(this, node).toString();
        }
        return result;
    }

    // helper function for inf, find an input string that leads from the specified state to an accepting state
    private String constructSuffix(Integer target) {
        List<Integer> distance = new ArrayList<>(Collections.nCopies(Q, -1));
        List<Integer> prev = new ArrayList<>(Collections.nCopies(Q, -1));
        List<Integer> input = new ArrayList<>(Collections.nCopies(Q, -1));
        int counter = 0;
        boolean found = false;
        int endState = 0;
        distance.set(target, 0);

        // the starting state may indeed by accepting
        if (O.getInt(target) != 0) {
            return "";
        }
        while(!found) {
            for (int i=0; i<Q; i++) {
                if (distance.get(i)!=counter)
                    continue;
                for (int x : d.get(i).keySet()) {
                    for (int y : d.get(i).get(x)) {
                        if (O.getInt(y) != 0) {
                            found = true;
                            endState = y;
                        }
                        if (distance.get(y) == -1) {
                            distance.set(y, counter+1);
                            prev.set(y, i);
                            input.set(y, x);
                        }
                    }
                }
            }
            counter++;
        }
        List<Integer> path = new ArrayList<>();
        int current = endState;

        while (current != target) {
            path.add(input.get(current));
            current = prev.get(current);
        }
        Collections.reverse(path);
        String result = "";
        for (Integer node : path) {
            result += decode(this, node).toString();
        }
        return result;
    }

    public void findAccepted(Integer searchLength, Integer maxNeeded) {
        this.accepted = new ArrayList<>();
        this.searchLength = searchLength;
        this.maxNeeded = maxNeeded;
        findAcceptedHelper(0, "", q0);
    }

    private boolean findAcceptedHelper(Integer curLength, String path, Integer state) {
        if (curLength == searchLength) {
            // if we reach an accepting state of desired length, we add the string we've formed to our subautomata list
            if (O.getInt(state) != 0) {
                accepted.add(path);
                if (accepted.size() >= maxNeeded) {
                    return true;
                }
            }
            else {
                return false;
            }
        }
        for (int x : d.get(state).keySet()) {
            for (Integer y : d.get(state).get(x)) {
                String input = decode(this, x).toString();

                // we remove brackets if we have a single arity input that is between 0 and 9 (and hence unambiguous)
                if (A.size() == 1) {
                    if (decode(this, x).get(0) >= 0 && decode(this, x).get(0) <= 9) {
                        input = input.substring(1, input.length()-1);
                    }
                }
                // if we've already found as much as we need, then there's no need to search further; we propagate the signal
                if (findAcceptedHelper(curLength+1, path + input, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void applyAllRepresentations() throws Exception{
        boolean flag = false;
        if(label == null || label.size() != A.size()){
            flag = true;
            randomLabel();
        }
        Automaton K = this;
        for(int i = 0 ; i < A.size();i++){
            if(NS.get(i) != null){
                Automaton N = NS.get(i).getAllRepresentations();
                if(N != null && NS.get(i).should_we_use_allRepresentations()) {
                    N.bind(label.get(i));
                    K = AutomatonLogicalOps.and(K, N,false,null,null);
                }
            }
        }
        if(flag)
            unlabel();
        copy(K);
    }

    public void applyAllRepresentationsWithOutput(boolean print, String prefix, StringBuilder log) throws Exception{
        // this can be a word automaton
        boolean flag = false;
        if(label == null || label.size() != A.size()){
            flag = true;
            randomLabel();
        }
        Automaton K = this;
        for(int i = 0 ; i < A.size();i++){
            if(NS.get(i) != null){
                Automaton N = NS.get(i).getAllRepresentations();
                if(N != null && NS.get(i).should_we_use_allRepresentations()) {
                    N.bind(label.get(i));

                    K = AutomatonLogicalOps.crossProduct(this, N,"if_other",print,prefix,log);
//                    K.minimizeSelfWithOutput(print, prefix, log);
                }
            }
        }
        if(flag)
            unlabel();
        copy(K);
    }

    public void randomLabel() {
        if(label == null)label = new ArrayList<>();
        else if(label.size() > 0)label = new ArrayList<>();
        for(int i = 0 ; i < A.size();i++){
            label.add(Integer.toString(i));
        }
    }

    private void unlabel(){
        label = new ArrayList<>();
        labelSorted = false;
    }

    private void copy(Automaton M){
        TRUE_FALSE_AUTOMATON = M.TRUE_FALSE_AUTOMATON;
        TRUE_AUTOMATON = M.TRUE_AUTOMATON;
        A = M.A;
        NS = M.NS;
        alphabetSize = M.alphabetSize;
        encoder = M.encoder;
        Q = M.Q;
        q0 = M.q0;
        O = M.O;
        label = M.label;
        canonized = M.canonized;
        labelSorted = M.labelSorted;
        d = M.d;
    }

    /**
     * This method adds a dead state with an output one less than the minimum output number of the word automaton.

        Return whether a dead state was even added.
     * @throws Exception
     */
    public boolean addDistinguishedDeadState(boolean print, String prefix, StringBuilder log) throws Exception {
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "Adding distinguished dead state: " + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        //we first check if the automaton is totalized
        boolean totalized = true;
        for(int q = 0 ; q < Q;q++){
            for(int x = 0; x < alphabetSize;x++){
                if(!d.get(q).containsKey(x)){
                    IntList nullState = new IntArrayList();
                    nullState.add(Q);
                    d.get(q).put(x, nullState);
                    totalized = false;
                }
            }
        }
        int min = 0;

        if (!totalized) {
            // obtain the minimum output
            if (O.size() == 0) {
                throw new Exception("Output alphabet is empty");
            }
            for (int i = 0; i < O.size(); i++) {
                if (O.getInt(i) < min) {
                    min = O.getInt(i);
                }
            }
            O.add(min-1);
            Q++;
            d.add(new Int2ObjectRBTreeMap<>());
            for(int x = 0; x < alphabetSize; x++){
                IntList nullState = new IntArrayList();
                nullState.add(Q-1);
                d.get(Q-1).put(x, nullState);
            }
        }

        long timeAfter = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "Already totalized, no distinguished state added: " + Q + " states - "+(timeAfter-timeBefore)+"ms";
            if (!totalized) {
                msg = prefix + "Added distinguished dead state with output of " + (min-1) + ": " + Q + " states - "+(timeAfter-timeBefore)+"ms";
            }
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return !totalized;
    }

    /**
     * The operator can be one of "+" "-" "*" "/".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs this[x] + W[x] on input x.
     * To be used only when this automaton and M are DFAOs (words).
     * @param W
     * @param operator
     * @return
     * @throws Exception
     */
    public Automaton applyOperator(Automaton W, String operator,boolean print, String prefix,StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applying operator ("+operator+"):" + Q + " states - " + W.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        Automaton M = AutomatonLogicalOps.crossProduct(this, W,operator,print,prefix+" ",log);
        M.minimizeWithOutput(print,prefix+" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applied operator ("+operator+ "):" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return M;
    }

    /**
     * The operator can be one of "_" "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs this[x]+o on input x.
     * To be used only when this automaton and M are DFAOs (words).
     * @param operator
     * @return
     * @throws Exception
     */
    public void applyOperator(String operator,int o,boolean print, String prefix,StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applying operator ("+operator+"):" + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        for(int p = 0 ; p < Q;p++){
            switch(operator){
                case "+":
                    O.set(p,O.getInt(p)+o);
                    break;
                case "-":
                    O.set(p,O.getInt(p)-o);
                    break;
                case "*":
                    O.set(p,O.getInt(p)*o);
                    break;
                case "/":
                    if(o == 0)throw new Exception("division by zero");
                    O.set(p,O.getInt(p)/o);
                    break;
                case "_":
                    O.set(p,-O.getInt(p));
                    break;
            }
        }
        minimizeSelfWithOutput(print,prefix+" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applied operator ("+operator+ "):" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    /**
     * The operator can be one of "_" "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs o+this[x] on input x.
     * To be used only when this automaton and M are DFAOs (words).
     * @param operator
     * @return
     * @throws Exception
     */
    public void applyOperator(int o,String operator,boolean print, String prefix,StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applying operator ("+operator+"):" + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        for(int p = 0 ; p < Q;p++){
            switch(operator){
                case "+":
                    O.set(p,o+O.getInt(p));
                    break;
                case "-":
                    O.set(p,o-O.getInt(p));
                    break;
                case "*":
                    O.set(p,o*O.getInt(p));
                    break;
                case "/":
                    if(O.getInt(p) == 0)throw new Exception("division by zero");
                    O.set(p,o/O.getInt(p));
                    break;
                case "_":
                    O.set(p,-O.getInt(p));
                    break;
            }
        }
        minimizeSelfWithOutput(print,prefix+" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "applied operator ("+operator+ "):" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method returns
     * a DFA that accepts x iff this[x] < W[x] lexicographically.
     * To be used only when this automaton and M are DFAOs (words).
     * @param W
     * @param operator
     * @return
     * @throws Exception
     */
    public Automaton compare(Automaton W, String operator,boolean print, String prefix,StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "comparing ("+operator+"):" + Q + " states - " + W.Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        Automaton M = AutomatonLogicalOps.crossProduct(this, W,operator,print,prefix+" ",log);
        M.minimize(null, print,prefix+" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "compared ("+operator+ "):" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return M;
    }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method changes the word automaton
     * to a DFA that accepts x iff this[x] < o lexicographically.
     * To be used only when this automaton is a DFAO (word).
     * @param operator
     * @return
     * @throws Exception
     */
    public void compare(int o, String operator, boolean print, String prefix,StringBuilder log) throws Exception{
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "comparing ("+operator+") against "+ o +":" + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        for(int p = 0 ; p < Q;p++){
            switch(operator){
            case "<":
                O.set(p,(O.getInt(p) < o) ? 1 : 0);
                break;
            case ">":
                O.set(p,(O.getInt(p) > o) ? 1 : 0);
                break;
            case "=":
                O.set(p,(O.getInt(p) == o) ? 1 : 0);
                break;
            case "!=":
                O.set(p,(O.getInt(p) != o) ? 1 : 0);
                break;
            case "<=":
                O.set(p,(O.getInt(p) <= o) ? 1 : 0);
                break;
            case ">=":
                O.set(p,(O.getInt(p) >= o) ? 1 : 0);
                break;
            }
        }
        minimize(null, print,prefix+" ",log);
        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "compared ("+operator+ ") against "+o+":" + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
    }

    <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> resultLists = new ArrayList<>();
        if (lists.size() == 0) {
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

    /**
     * We can choose to do Valmari or Hopcroft.
     * @throws Exception
     */
    public void minimize(List<Int2IntMap> newMemD, boolean print, String prefix, StringBuilder log) throws Exception {
        long timeBefore = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "Minimizing: " + Q + " states.";
            System.out.println("----- " + msg);
            log.append(msg + UtilityMethods.newLine());
        }

        minimize_valmari(newMemD, print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        if(print) {
            String msg = prefix + "Minimized:" + Q + " states - " + (timeAfter-timeBefore) + "ms.";
            System.out.println("----- " + msg);
            log.append(msg + UtilityMethods.newLine());
        }
    }

    /**
     * Transform this automaton from Automaton to dk.bricks.automaton.Automaton. This automaton can be
     * any automaton (deterministic/non-deterministic and with output/without output).
     * @return
     * @throws Exception
     */
    public dk.brics.automaton.Automaton to_dk_bricks_automaton()throws Exception{
        /**
         * Since the dk.bricks.automaton uses char as its input alphabet for an automaton, then in order to transform
         * Automata.Automaton to dk.bricks.automaton.Automata we've got to make sure, the input alphabet is less than
         * size of char which 2^16 - 1
         */
        if(alphabetSize > ((1<<Character.SIZE) -1)){
            //System.out.println("Character size: " + );
            throw new Exception("size of input alphabet exceeds the limit of " + ((1<<Character.SIZE) -1));
        }
        boolean deterministic = true;
        List<dk.brics.automaton.State> setOfStates = new ArrayList<>();
        for(int q = 0 ; q < Q;q++){
            setOfStates.add(new dk.brics.automaton.State());
            if(O.getInt(q) != 0)setOfStates.get(q).setAccept(true);
        }
        dk.brics.automaton.State initialState = setOfStates.get(q0);
        for(int q = 0 ; q < Q;q++){
            for(int x: d.get(q).keySet()){
                for(int dest:d.get(q).get(x)){
                    setOfStates.get(q).addTransition(new dk.brics.automaton.Transition((char)x,setOfStates.get(dest)));
                }
                if(d.get(q).get(x).size() > 1)deterministic = false;
            }
        }
        dk.brics.automaton.Automaton M = new dk.brics.automaton.Automaton();
        M.setInitialState(initialState);
        M.restoreInvariant();
        M.setDeterministic(deterministic);
        return M;
    }

    /**
     * Set the fields of this automaton to represent a dk.brics.automaton.Automaton.
     * An automata in our program can be of type Automaton or dk.brics.automaton.Automaton. We use package
     * dk.bricks.automaton for automata minimization. This method transforms an automaton of type dk.bricks.automaton.Automaton
     * to an automaton of type Automaton.
     * @param M is a deterministic automaton without output.
     */
    private void setThisAutomatonToRepresent(dk.brics.automaton.Automaton M)throws Exception{
        if(!M.isDeterministic())
            throw new Exception("cannot set an automaton of type Automaton to a non-deterministic automaton of type dk.bricks.automaton.Automaton");
        List<State> setOfStates = new ArrayList<>(M.getStates());
        Q = setOfStates.size();
        q0 = setOfStates.indexOf(M.getInitialState());
        O = new IntArrayList();
        d = new ArrayList<>();
        canonized = false;
        for(int q = 0 ; q < Q;q++){
            State state = setOfStates.get(q);
            if(state.isAccept())O.add(1);
            else O.add(0);
            Int2ObjectRBTreeMap<IntList> currentStatesTransitions = new Int2ObjectRBTreeMap<>();
            d.add(currentStatesTransitions);
            for(Transition t: state.getTransitions()){
                for(char a = t.getMin();a <= t.getMax();a++){
                    IntList dest = new IntArrayList();
                    dest.add(setOfStates.indexOf(t.getDest()));
                    currentStatesTransitions.put((int)a, dest);
                }
            }
        }
    }

    /**
     *  Sorts states in Q based on their breadth-first order. It also calls sortLabel().
     *  The method also removes states that are not reachable from the initial state.
     *  In draw() and write() methods, we first call the canonize the automaton.
     *  It is also used in write() method.
     *  Note that before we try to canonize, we check if this automaton is already canonized.
     *  @throws Exception
     */
    public void canonize(){
        if(canonized) return;

        sortLabel();
        if(TRUE_FALSE_AUTOMATON) return;

        Queue<Integer> state_queue = new LinkedList<>();
        state_queue.add(q0);

        /**map holds the permutation we need to apply to Q. In other words if map = {(0,3),(1,10),...} then
        *we have got to send Q[0] to Q[3] and Q[1] to Q[10]*/
        Int2IntMap map = new Int2IntOpenHashMap();
        map.put(q0,0);
        int i = 1;
        while(!state_queue.isEmpty()) {
            int q = state_queue.poll();
            for(int x:d.get(q).keySet()) {
                for(int p: d.get(q).get(x)) {
                    if(!map.containsKey(p)) {
                        map.put(p, i++);
                        state_queue.add(p);
                    }
                }
            }
        }

        q0 = map.get(q0);
        int newQ = map.size();
        IntList newO = new IntArrayList();
        for(int q = 0 ; q < newQ;q++) {
            newO.add(0);
        }
        for(int q = 0; q < Q;q++) {
            if(map.containsKey(q)) {
                newO.set(map.get(q),O.getInt(q));
            }
        }

        List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
        for(int q = 0 ; q < newQ;q++) {
            new_d.add(null);
        }

        for(int q = 0; q < Q;q++) {
            if(map.containsKey(q)) {
                new_d.set(map.get(q), d.get(q));
            }
        }

        Q = newQ;
        O = newO;
        d = new_d;
        for(int q = 0 ; q < Q;q++) {
            for(int x:d.get(q).keySet()) {
                IntList newDestination = new IntArrayList();
                for(int p:d.get(q).get(x)) {
                    if(map.containsKey(p)) {
                        newDestination.add(map.get(p));
                    }
                }

                if(newDestination.size() > 0) {
                    d.get(q).put(x,newDestination);
                } else {
                    d.get(q).remove(x);
                }
            }
        }

        canonized = true;
    }

    /**
     *  Sorts inputs based on their labels lexicographically.
     *  For example if the labels of the inputs are ["b","c","a"], then the first, second, and third
     *  inputs are "a", "b", and "c". Now if we call sortLabels(), the order of inputs changes: label becomes
     *  sorted in lexicographic order ["a","b","c"], and therefore, the first, second, and third inputs are
     *  now "a", "b", and "c". Before we draw this automaton using draw() method,
     *  we first sort the labels (inside canonize method).
     *  It is also used in write() method.
     *  Note that before we try to sort, we check if the label is already sorted.
     *  The label cannot have repeated element.
     */
    protected void sortLabel() {
        if(labelSorted)return;
        labelSorted = true;
        if(TRUE_FALSE_AUTOMATON)return;
        if(label == null || label.size() != A.size())return;
        //first we check if label is already sorted.
        boolean sorted = true;
        for(int i = 0 ; i < label.size()-1;i++){
            if(label.get(i).compareTo(label.get(i+1)) > 0){
                sorted = false;
                break;
            }
        }
        if(sorted)return;
        List<String> sorted_label = new ArrayList<>(label);
        Collections.sort(sorted_label);
        /**
         * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
         * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
         * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
         */
        int[] label_permutation = new int[label.size()];
        for(int i = 0 ; i < label.size();i++){
            label_permutation[i] = sorted_label.indexOf(label.get(i));
        }
        /**
         * permuted_A is going to hold the alphabet of the sorted inputs.
         * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
         * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
         * The same logic is behind permuted_encoder.
         */
        List<List<Integer>> permuted_A = UtilityMethods.permute(A, label_permutation);
        List<Integer> permuted_encoder = new ArrayList<>();
        permuted_encoder.add(1);
        for(int i = 0 ; i < A.size()-1;i++){
            permuted_encoder.add(permuted_encoder.get(i)*permuted_A.get(i).size());
        }
        /**
         * For example encoded_input_permutation[2] = 5 means that encoded input 2 becomes
         * 5 after sorting.
         */
        int[] encoded_input_permutation = new int[alphabetSize];
        for(int i = 0 ; i < alphabetSize;i++){
            List<Integer> input = decode(this, i);
            List<Integer> permuted_input = UtilityMethods.permute(input, label_permutation);
            encoded_input_permutation[i] = encode(permuted_input, permuted_A,permuted_encoder);
        }

        label = sorted_label;
        A = permuted_A;
        encoder = permuted_encoder;
        NS = UtilityMethods.permute(NS,label_permutation);

        for(int q = 0; q < Q;q++){
            Int2ObjectRBTreeMap<IntList> permuted_d = new Int2ObjectRBTreeMap<>();
            for(int x:d.get(q).keySet())
                permuted_d.put(encoded_input_permutation[x], d.get(q).get(x));
            d.set(q,permuted_d);
        }
    }

    /**
     * Input to dk.brics.automaton.Automata is a char. Input to Automaton is List<Integer>.
     * Thus this method transforms an integer to its corresponding List<Integer>
     * Example: A = [[0,1],[-1,2,3]] and if
     * n = 0 then we return [0,-1]
     * n = 1 then we return [1,-1]
     * n = 2 then we return [0,2]
     * n = 3 then we return [1,2]
     * n = 4 then we return [0,3]
     * n = 5 then we return [1,3]
     * @param automaton
     * @param n
     * @return
     */
    static List<Integer> decode(Automaton automaton, int n){
        List<Integer> l = new ArrayList<>(automaton.A.size());
        for(int i = 0; i < automaton.A.size(); i++){
            l.add(automaton.A.get(i).get(n % automaton.A.get(i).size()));
            n = n / automaton.A.get(i).size();
        }
        return l;
    }

    /**
     * Input to dk.brics.automaton.Automata is a char. Input to Automata.Automaton is List<Integer>.
     * Thus this method transforms a List<Integer> to its corresponding integer.
     * The other application of this function is when we use the transition function d in State. Note that the transtion function
     * maps an integer (encoding of List<Integer>) to a set of states.
     *
     * Example: A = [[0,1],[-1,2,3]] and if
     * l = [0,-1] then we return 0
     * l = [1,-1] then we return 1
     * l = [0,2] then we return 2
     * l = [1,2] then we return 3
     * l = [0,3] then we return 4
     * l = [1,3] then we return 5
     * Second Example: A = [[-2,-1,-3],[0,1],[-1,0,3],[7,8]] and if
     * l = [-2,0,-1,7] then we return 0
     * l = [-1,0,-1,7] then we return 1
     * l = [-3,0,-1,7] then we return 2
     * l = [-2,1,-1,7] then we return 3
     * l = [-1,1,-1,7] then we return 4
     * l = [-3,1,-1,7] then we return 5
     * l = [-2,0,0,7] then we return 6
     * ...
     * @param l
     * @return
     */
    public int encode(List<Integer> l){
        if(encoder == null){
            setupEncoder();
        }
        return encode(l, A, encoder);
    }

    public static int encode(List<Integer> l, List<List<Integer>> A, List<Integer> encoder){
        int encoding = 0;
        for(int i = 0 ; i < l.size();i++){
            encoding += encoder.get(i) * A.get(i).indexOf(l.get(i));
        }
        return encoding;
    }

    public void setupEncoder() {
        encoder = new ArrayList<>();
        encoder.add(1);
        for(int i = 0 ; i < A.size()-1;i++){
            encoder.add(encoder.get(i)*A.get(i).size());
        }
    }

    /**A wildcard is denoted by null in L. What do we mean by expanding wildcard?
     * Here is an example: suppose that A = [[1,2],[0,-1],[3,4,5]] and L = [1,*,4]. Then the method would return
     * [[1,0,4],[1,-1,4]]. In other words, it'll replace * in the second position with 0 and -1.
     * */
    protected List<List<Integer>> expandWildcard(List<Integer> L){
        List<List<Integer>> R = new ArrayList<>();
        R.add(new ArrayList<>(L));
        for(int i = 0 ; i < L.size();i++){
            if(L.get(i) == null){
                List<List<Integer>> tmp = new ArrayList<>();
                for(int x:A.get(i)){
                    for(List<Integer> tmp2:R){
                        tmp.add(new ArrayList<>(tmp2));
                        tmp.get(tmp.size()-1).set(i, x);
                    }
                }
                R = new ArrayList<>(tmp);
            }
        }
        return R;
    }

    public void bind(String a)throws Exception{
        if(TRUE_FALSE_AUTOMATON || A.size() != 1)throw new Exception("invalid use of method bind");
        if(label == null || label.size()!=0)label = new ArrayList<>();
        label.add(a);
        labelSorted = false;
    }

    public void bind(String a,String b)throws Exception{
        if(TRUE_FALSE_AUTOMATON || A.size() != 2)throw new Exception("invalid use of method bind");
        if(label == null || label.size()!=0)label = new ArrayList<>();
        label.add(a);label.add(b);
        canonized = false;
        labelSorted = false;
        AutomatonLogicalOps.removeSameInputs(this, 0);
    }

    public void bind(String a,String b,String c)throws Exception{
        if(TRUE_FALSE_AUTOMATON || A.size() != 3)throw new Exception("invalid use of method bind");
        if(label == null || label.size()!=0)label = new ArrayList<>();
        label.add(a);label.add(b);label.add(c);
        labelSorted = false;
        canonized = false;
        AutomatonLogicalOps.removeSameInputs(this, 0);
    }

    public void bind(List<String> names)throws Exception{
        if(TRUE_FALSE_AUTOMATON || A.size() != names.size())throw new Exception("invalid use of method bind");
        if(label == null || label.size()!=0)label = new ArrayList<>();
        this.label.addAll(names);
        labelSorted = false;
        canonized = false;
        AutomatonLogicalOps.removeSameInputs(this, 0);
    }

    public boolean isBound(){
        if(label == null || label.size() != A.size())
            return false;
        return true;
    }

    public int getArity(){
        if(TRUE_FALSE_AUTOMATON)return 0;
        return A.size();
    }

    /**clears this automaton*/
    private void clear(){
        A = null;
        NS = null;
        encoder = null;
        O = null;
        label = null;
        d = null;
        canonized = false;
        labelSorted = false;
    }

    protected boolean isEmpty()throws Exception{
        if(TRUE_FALSE_AUTOMATON){
            return !TRUE_AUTOMATON;
        }
        return to_dk_bricks_automaton().isEmpty();
    }

    /**
     * Subset Construction (Determinizing).
     * @param initial_state
     * @param print
     * @param prefix
     * @param log
     * @return A memory-efficient representation of a determinized transition function
     * @throws Exception
     */
    List<Int2IntMap> subsetConstruction(
            List<Int2IntMap> newMemD, IntSet initial_state,boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        if(print){
            String msg = prefix + "Determinizing: " + Q + " states";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }

        int number_of_states = 0,current_state = 0;
        Object2IntMap<IntSet> statesHash = new Object2IntOpenHashMap<>();
        List<IntSet> statesList = new ArrayList<>();
        statesList.add(initial_state);
        statesHash.put(initial_state, 0);
        number_of_states++;

        List<Int2IntMap> new_d = new ArrayList<>();

        while(current_state < number_of_states){

            if (print) {
                int statesSoFar = current_state + 1;
                long timeAfter = System.currentTimeMillis();
                if (statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0) {
                    String msg = prefix + "  Progress: Added " + statesSoFar + " states - "
                            + (number_of_states-statesSoFar) + " states left in queue - "
                            + number_of_states + " reachable states - " + (timeAfter- timeBefore)+"ms";
                    log.append(msg + UtilityMethods.newLine());
                    System.out.println(msg);
                }
            }

            IntSet state = statesList.get(current_state);
            new_d.add(new Int2IntOpenHashMap());
            Int2IntMap currentStateMap = new_d.get(current_state);
            for(int in = 0;in!=alphabetSize;++in){
                IntOpenHashSet stateSubset = determineStateSubset(newMemD, state, in);
                if(!stateSubset.isEmpty()){
                    int new_dValue;
                    int key = statesHash.getOrDefault(stateSubset, -1);
                    if (key != -1){
                        new_dValue = key;
                    }
                    else{
                        stateSubset.trim(); // reduce memory footprint of set before storing
                        statesList.add(stateSubset);
                        statesHash.put(stateSubset,number_of_states);
                        new_dValue = number_of_states;
                        number_of_states++;
                    }
                    currentStateMap.put(in, new_dValue);
                }
            }
            current_state++;
        }
        d = null;
        // NOTE: d is now null! This is to save peak memory
        // It's recomputed in minimize_valmari via the memory-efficient newMemD
        Q = number_of_states;
        q0 = 0;
        O = AutomatonLogicalOps.calculateNewStateOutput(O, statesList);

        long timeAfter = System.currentTimeMillis();
        if(print){
            String msg = prefix + "Determinized: " + Q + " states - "+(timeAfter-timeBefore)+"ms";
            log.append(msg + UtilityMethods.newLine());
            System.out.println(msg);
        }
        return new_d;
    }

    /**
     * Build up a new subset of states in the subset construction algorithm.
     * @param newMemD - memory-efficient transition function
     * @param state -
     * @param in - index into alphabet
     * @return Subset of states used in Subset Construction
     */
    private IntOpenHashSet determineStateSubset(List<Int2IntMap> newMemD, IntSet state, int in) {
        IntOpenHashSet dest = new IntOpenHashSet();
        for(int q: state){
            if(newMemD == null) {
                IntList values = d.get(q).get(in);
                if (values != null) {
                    dest.addAll(values);
                }
            } else {
                Int2IntMap iMap = newMemD.get(q);
                int key = iMap.getOrDefault(in, -1);
                if (key != -1) {
                    dest.add(key);
                }
            }
        }
        return dest;
    }

    int mapToReducedEncodedInput(int n,List<Integer> I,List<Integer> newEncoder,List<List<Integer>> newAlphabet){
        if(I.size() <= 1)return n;
        List<Integer> x = decode(this, n);
        for(int i = 1 ; i < I.size();i++)
            if(x.get(I.get(i)) != x.get(I.get(0)))
                return -1;
        List<Integer> y = new ArrayList<>();
        for(int i = 0 ; i < x.size();i++)
            if(!I.contains(i) || I.indexOf(i) == 0)
                y.add(x.get(i));
        return encode(y, newAlphabet, newEncoder);
    }
}
