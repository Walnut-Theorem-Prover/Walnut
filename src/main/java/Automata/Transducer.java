/*   Copyright 2022 Anatoly Zavyalov
 *
 *   This file is part of Walnut.
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

import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import Automata.FA.FA;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import static Automata.ParseMethods.PATTERN_WHITESPACE;

/**
 * The class Transducer represents a deterministic finite-state transducer with all states final that is 1-uniform.
 * <p>
 * It is implemented by constructing a deterministic finite-state automaton with all states final, and adding on top a
 * 1-uniform output function with the domain S x A (where S is the set of states and A is the input alphabet),
 * and the codomain being an output alphabet (subset of the integers).
 *
 * @author Anatoly
 */
public class Transducer extends Automaton {
    /**
     * Output function for the Transducer.
     * For example, when sigma[0] = [(0, 1), (1, 0), (2, -1), (3, 2), (4, -1), (5, 3)]
     * and input alphabet A = [[0, 1], [-1, 2, 3]],
     * then from state 0 on
     * (0, -1) we output 1
     * (0, 2) we output 0
     * (0, 3) we output -1
     * (1, -1) we output 2
     * (1, 2) we output -1
     * (1, 3) we output 3
     * <p>
     * Just like in an Automaton's transition function d, we store the encoded values of inputs in sigma, so instead of
     * saying that "on (0, -1) we output 1", we really store "on 0, output 1".
     */
    private final List<TreeMap<Integer, Integer>> sigma;

    /**
     * Default constructor for Transducer. Calls the default constructor for Automaton.
     */
    public Transducer() {
        super();

        sigma = new ArrayList<>();
    }

    /**
     * Takes an address and constructs the transducer represented by the file referred to by the address.
     *
     * @param address
     * @throws Exception
     */
    public Transducer(String address) {
        this();
        File f = UtilityMethods.validateFile(address);

        // lineNumber will be used in error messages
        int lineNumber = 0;

        setAlphabetSize(1);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;

                if (PATTERN_WHITESPACE.matcher(line).matches()) {
                    // Ignore blank lines.
                    continue;
                }

                boolean flag = ParseMethods.parseAlphabetDeclaration(line, getA(), getNS());

                if (flag) {
                    for (int i = 0; i < getA().size(); i++) {
                        if (getNS().get(i) != null &&
                                (!getA().get(i).contains(0) || !getA().get(i).contains(1))) {
                            throw new RuntimeException(
                                    "The " + (i + 1) + "th input of type arithmetic " +
                                            "of the automaton declared in file " + address +
                                            " requires 0 and 1 in its input alphabet: line " +
                                            lineNumber);
                        }
                        UtilityMethods.removeDuplicates(getA().get(i));
                    }
                    determineAlphabetSizeFromA();
                    break;
                } else {
                    throw new RuntimeException(
                            "Undefined statement: line " +
                                    lineNumber + " of file " + address);
                }
            }

            int[] singleton = new int[1];
            List<Integer> input = new ArrayList<>();
            IntList dest = new IntArrayList();
            List<Integer> output = new ArrayList<>();
            int currentState = -1;
            int currentStateOutput;
            Int2ObjectRBTreeMap<IntList> currentStateTransitions = new Int2ObjectRBTreeMap<>();
            TreeMap<Integer, Integer> currentStateTransitionOutputs = new TreeMap<>();
            TreeMap<Integer, Integer> state_output = new TreeMap<>();
            TreeMap<Integer, Int2ObjectRBTreeMap<IntList>> state_transition =
                    new TreeMap<>();
            TreeMap<Integer, TreeMap<Integer, Integer>> state_transition_output =
                    new TreeMap<>();
            /**
             * This will hold all states that are destination of some transition.
             * Then we make sure all these states are declared.
             */
            Set<Integer> setOfDestinationStates = new HashSet<>();
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (PATTERN_WHITESPACE.matcher(line).matches()) {
                    continue;
                }

                if (ParseMethods.parseTransducerStateDeclaration(line, singleton)) {
                    setQ(getQ() + 1);
                    if (currentState == -1) {
                        setQ0(singleton[0]);
                    }

                    currentState = singleton[0];
                    currentStateOutput = 0; // state output does not matter for transducers.
                    state_output.put(currentState, currentStateOutput);
                    currentStateTransitions = new Int2ObjectRBTreeMap<>();
                    state_transition.put(currentState, currentStateTransitions);
                    currentStateTransitionOutputs = new TreeMap<>();
                    state_transition_output.put(currentState, currentStateTransitionOutputs);
                } else if (ParseMethods.parseTransducerTransition(line, input, dest, output)) {
                    setOfDestinationStates.addAll(dest);

                    if (currentState == -1) {
                        throw new RuntimeException(
                                "Must declare a state before declaring a list of transitions: line " +
                                        lineNumber + " of file " + address);
                    }

                    if (input.size() != getA().size()) {
                        throw new RuntimeException("This automaton requires a " + getA().size() +
                                "-tuple as input: line " + lineNumber + " of file " + address);
                    }
                    List<List<Integer>> inputs = expandWildcard(this.getA(), input);
                    for (List<Integer> i : inputs) {
                        currentStateTransitions.put(encode(i), dest);
                        if (output.size() == 1) {
                            currentStateTransitionOutputs.put(encode(i), output.get(0));
                        } else {
                            throw new RuntimeException("Transducers must have one output for each transition: line "
                                    + lineNumber + " of file " + address);
                        }
                    }

                    input = new ArrayList<>();
                    dest = new IntArrayList();
                    output = new ArrayList<>();
                } else {
                    throw new RuntimeException("Undefined statement: line " + lineNumber + " of file " + address);
                }
            }
            for (int q : setOfDestinationStates) {
                if (!state_output.containsKey(q)) {
                    throw new RuntimeException(
                            "State " + q + " is used but never declared anywhere in file: " + address);
                }
            }

            for (int q = 0; q < getQ(); q++) {
                getO().add((int) state_output.get(q));
                getD().add(state_transition.get(q));
                sigma.add(state_transition_output.get(q));
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File does not exist: " + address);
        }
    }

    /**
     * Returns a deep copy of this transducer.
     */
    public Transducer clone() {
        Transducer T = new Transducer();
        super.cloneFields(T);
        for (int i = 0; i < sigma.size(); i++) {
            T.sigma.add(new TreeMap<>(sigma.get(i)));
        }
        return T;
    }

    /**
     * Transduce an msd-k Automaton M as in Dekking (1994).
     *
     * @param M      - automaton to transduce
     * @param print  - whether to print details
     * @param prefix - prefix for printing details
     * @param log    - log to write the details to
     * @return The transduced Automaton after applying this Transducer to M.
     * @throws Exception
     */
    public Automaton transduceMsdDeterministic(Automaton M, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "transducing: " + M.getQ() + " state automaton - " + getQ() + " state transducer", log);

        /**
         * N will be the returned Automaton, just have to build it up.
         */
        Automaton N = new Automaton();

        // build up the automaton.
        for (int i = 0; i < M.getA().size(); i++) {
            N.getA().add(M.getA().get(i));
            N.getNS().add(M.getNS().get(i));

            // Copy the encoder
            if (M.getEncoder() != null && !M.getEncoder().isEmpty()) {
                if (N.getEncoder() == null) {
                    N.setEncoder(new ArrayList<>());
                }
                N.getEncoder().add(M.getEncoder().get(i));
            }

            // Copy the label
            if (M.isBound()) {
                N.getLabel().add(M.getLabel().get(i));
            }
        }

            /*
                Need to find P and Q so the transition function of the Transducer becomes ultimately periodic with lag Q
                and period P.
             */

        // Will be used for hashing the iterate maps.
        HashMap<List<Map<Integer, Integer>>, Integer> iterateMapHash = new HashMap<>();

        // iterateStrings[i] will be a map from a state q of M to h^i(q).
        List<List<List<Integer>>> iterateStrings = new ArrayList<>();

        // initMaps.get(i) will be the map phi_{M.O(i)}
        List<Map<Integer, Integer>> initMaps = new ArrayList<>();

        // initStrings.get(j) = [j];
        List<List<Integer>> initStrings = new ArrayList<>();

        // start with the empty string.
        Map<Integer, Integer> identity = createIdentityMap(this.getQ());

        // will add M.Q maps to initMaps.
        for (int i = 0; i < M.getQ(); i++) {
            Map<Integer, Integer> map = createMap2(M.getFa(), i);
            initMaps.add(map);
            initStrings.add(List.of(i));
        }

        iterateMapHash.put(initMaps, 0);
        iterateStrings.add(initStrings);

        int mFound = -1, nFound = -1;

        for (int m = 1; ; m++) {
            List<List<Integer>> prevStrings = iterateStrings.get(iterateStrings.size() - 1);
            List<Map<Integer, Integer>> newMaps = new ArrayList<>();
            List<List<Integer>> newStrings = new ArrayList<>();

            for (int i = 0; i < M.getQ(); i++) {
                // will be h^m(i)
                List<Integer> iString = getDestinationForDFA(M, prevStrings.get(i));

                newStrings.add(iString);

                // start off with the identity.
                Map<Integer, Integer> mapSoFar = createMapSoFar(M.getFa(), identity, iString);

                newMaps.add(mapSoFar);

            }

            iterateStrings.add(newStrings);

            if (iterateMapHash.containsKey(newMaps)) {
                nFound = iterateMapHash.get(newMaps);
                mFound = m;
            } else {
                iterateMapHash.put(newMaps, m);
            }

            if (mFound != -1) {
                break;
            }

        }

        int p = mFound - nFound;
        int q = nFound;

        /*
            Make the states of the automaton.
         */

        // now to generate the actual states.

        N.setQ0(0);

        // tuple of the form (a, iters) where iters is a list of p+q maps phi_{M.O(w)}, ..., phi_{h^{p+q-1}(M.O(W))}
        class StateTuple {
            final int state;
            final List<Integer> iList;
            final List<Map<Integer, Integer>> iterates;

            StateTuple(int state, List<Integer> iList, List<Map<Integer, Integer>> iterates) {
                this.state = state;
                this.iList = iList;
                this.iterates = iterates;
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
                if (this.state != other.state) {
                    return false;
                }

                if (this.iterates.size() != other.iterates.size()) {
                    return false;
                }
                for (int i = 0; i < this.iterates.size(); i++) {
                    if (!this.iterates.get(i).equals(other.iterates.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public int hashCode() {
                // DO NOT use the string to hash. Only use the state and the iterates.
                int result = this.state ^ (this.state >>> 32);
                result = 31 * result + this.iterates.hashCode();
                return result;
            }
        }

        List<StateTuple> states = new ArrayList<>();
        Map<StateTuple, Integer> statesHash = new HashMap<>();
        Queue<StateTuple> statesQueue = new LinkedList<>();
        StateTuple initState = new StateTuple(M.getQ0(), List.of(), createIterates(M, List.of(), p + q));
        states.add(initState);
        statesHash.put(initState, states.size() - 1);
        statesQueue.add(initState);

        while (!statesQueue.isEmpty()) {
            StateTuple currState = statesQueue.remove();

            // set up the output of this state.

            N.getO().add((int) sigma.get(currState.iterates.get(0).get(getQ0())).get(encode(List.of(M.getO().getInt(currState.state)))));

            N.getD().add(new Int2ObjectRBTreeMap<>());

            // get h(w) where w = currState.string .
            List<Integer> newString = getDestinationForDFA(M, currState.iList);

            List<Integer> stateMorphed = new ArrayList<>();

            // relying on the di's to be sorted here...
            addFirstEntries(M, currState.state, stateMorphed);

            // look at the states that this state transitions to.

            for (Int2ObjectMap.Entry<IntList> entry : M.getFa().getEntriesNfaD(currState.state)) {
                int di = entry.getIntKey();

                // make new state string
                List<Integer> newStateString = new ArrayList<>(newString);
                for (int u = 0; u < di; u++) {
                    newStateString.add(stateMorphed.get(u));
                }

                // new state
                StateTuple newState = new StateTuple(
                        stateMorphed.get(di),
                        newStateString,
                        createIterates(M, newStateString, p + q)
                );

                // check if the state is already hashed.
                if (!statesHash.containsKey(newState)) {
                    states.add(newState);
                    statesHash.put(newState, states.size() - 1);
                    statesQueue.add(newState);
                }

                // set up the transition.
                IntList newList = new IntArrayList();
                newList.add((int) statesHash.get(newState));
                N.getD().get(N.getD().size() - 1).put(di, newList);
            }
        }

        N.setQ(states.size());
        N.setAlphabetSize(M.getAlphabetSize());

        N.minimizeSelfWithOutput(print, prefix + " ", log);

        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "transduced: " + N.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);

        return N;
    }


    private static List<Integer> getDestinationForDFA(Automaton M, List<Integer> prevString) {
        List<Integer> iString = new ArrayList<>();
        for (Integer integer : prevString) {
            // for every digit in the alphabet of M
            addFirstEntries(M, integer, iString);
        }
        return iString;
    }

    private static void addFirstEntries(Automaton M, Integer integer, List<Integer> iString) {
        for (Int2ObjectMap.Entry<IntList> entry : M.getFa().getEntriesNfaD(integer)) {
            // each list of states that this transition goes to.
            // we assuming it's a DFA for now, so this has length 1 we're assuming...
            // get the first index of M.d on state x and edge label l
            iString.add(entry.getValue().getInt(0));
        }
    }

    /**
     * Transduce an automaton that may have undefined transitions as in Dekking (1994). The automaton may not have
     * more than one transition per input character per state.
     *
     * @param M - automaton to transduce
     * @param print - whether to print details
     * @param prefix - prefix for printing details
     * @param log - log to write the details to
     * @return The transduced Automaton after applying this Transducer to M.
     * @throws Exception
     */
    public Automaton transduceNonDeterministic(Automaton M, boolean print, String prefix, StringBuilder log) {

        // check that the input automaton only has one input!
        if (M.getNS().size() != 1) {
            throw new RuntimeException("Automata with only one input can be transduced.");
        }

        // Check that the output alphabet of the automaton is compatible with the input alphabet of the transducer.
        IntList O = M.getO();
        for (int i = 0; i < O.size(); i++) {
            int encoded = encode(List.of(O.getInt(i)));
            if (!getD().get(0).containsKey(encoded)) {
                throw new RuntimeException("Output alphabet of automaton must be compatible with the transducer input alphabet");
            }
        }

        // make sure the number system is lsd.
        boolean toLsd = false;

        if (!M.getNS().get(0).isMsd()) {
            UtilityMethods.logMessage(print, prefix + "Automaton number system is lsd, reversing", log);
            toLsd = true;
            AutomatonLogicalOps.reverseWithOutput(M, true, print, prefix+" ", log);
        }

        // verify that the automaton is indeed nondeterministic, i.e. it has undefined transitions. If it is not, transduce normally.
        boolean totalized = M.getFa().isTotalized();
        Automaton N;
        if (totalized) {
            // transduce normally
            N = transduceMsdDeterministic(M, print, prefix, log);
        }
        else {
            Automaton Mnew = M.clone();
            Mnew.getFa().addDistinguishedDeadState(print, prefix+" ", log);

            // after transducing, all states with this minimum output will be removed.
            int minOutput = Mnew.getFa().determineMinOutput();

            Transducer Tnew = clone();

            for (int q = 0; q < Tnew.getQ(); q++) {
                IntList newList = new IntArrayList();
                newList.add(q);
                Tnew.getD().get(q).put(minOutput, newList);
                Tnew.sigma.get(q).put(minOutput, minOutput);
            }

            N = Tnew.transduceMsdDeterministic(Mnew, print, prefix+" ", log);

            AutomatonLogicalOps.removeStatesWithMinOutput(N, minOutput);
        }

        if (toLsd) {
            AutomatonLogicalOps.reverseWithOutput(N, true, print, prefix+" ", log);
        }

        return N;
    }



    /**
     * Take a string w of states of automaton M, and return the list
     *      [phi_{M.O(w)}, phi_{M.O(h(w))}, ..., phi_{M.O(h^{size - 1}(w))}]
     * where h is the morphism associated with M.
     * @param M - an Automaton to be transduced
     * @param string - a list of states of M
     * @param size - the size of the resulting list of maps.
     * @return
     */
    private List<Map<Integer, Integer>> createIterates(Automaton M, List<Integer> string, int size) {

        List<Map<Integer, Integer>> iterates = new ArrayList<>();
        // start with the empty string.
        Map<Integer, Integer> identity = createIdentityMap(this.getQ());

        List<Integer> dests = new ArrayList<>(string);

        for (int i = 0; i < size; i++) {
            // make the map associated with currString and add it to the iterates array.
            // start off with the identity.
            iterates.add(createMapSoFar(M.getFa(), identity, dests));

            // make new string currString to be h(currString), where h is the morphism associated with M.
            if (i != size - 1) {
              dests = getDestinationForDFA(M, dests);
            }
        }

        return iterates;
    }

    private static Map<Integer, Integer> createIdentityMap(int Q) {
        Map<Integer, Integer> identity = new HashMap<>();
        for (int i = 0; i < Q; i++) {
            identity.put(i, i);
        }
        return identity;
    }

    private Map<Integer, Integer> createMapSoFar(FA M, Map<Integer, Integer> identity, List<Integer> iString) {
        Map<Integer, Integer> mapSoFar = new HashMap<>(identity);
        for (Integer i : iString) {
            mapSoFar = createMap(M, i, mapSoFar);
        }
        return mapSoFar;
    }

    private Map<Integer, Integer> createMap2(FA M, int i) {
        int encoded = encode(List.of(M.getO().getInt(i)));
        Map<Integer, Integer> map = new HashMap<>();
        for (int j = 0; j < getQ(); j++) {
            map.put(j, getD().get(j).get(encoded).getInt(0));
        }
        return map;
    }

    private Map<Integer, Integer> createMap(FA M, Integer i, Map<Integer, Integer> mapSoFar) {
        int encoded = encode(List.of(M.getO().getInt(i)));
        Map<Integer, Integer> map = new HashMap<>();
        for (int j = 0; j < getQ(); j++) {
            map.put(j, getD().get(mapSoFar.get(j)).get(encoded).getInt(0));
        }
        return map;
    }
}