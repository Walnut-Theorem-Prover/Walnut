/*   Copyright 2019 Aseem Baranwal, 2025 John Nicol
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

package Automata.Numeration;

import java.util.ArrayList;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

import Automata.Automaton;
import Automata.AutomatonWriter;
import Automata.ParseMethods;
import Main.Session;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * The class OstrowskiNumeration includes functionality to produce an adder automaton based on only the
 * quadratic irrational number alpha that characterizes it. The quadratic irrational is
 * represented using the continued fraction expansion with these two things:
 * - Pre-period, and Period.
 * For example, for alpha = sqrt(3) - 1, pre-period = [] and period = [1, 2].
 * We only consider alpha < 1, therefore a 0 is always assumed in the pre-period and need not be
 * mentioned in the command.
 */
public class Ostrowski {
    // The number of states in the 4-input adder is 7.
    private static final int NUM_STATES = 7;

    // The value of (r, s) will be in ({-1, 0, 1}, {-2, -1, 0, 1, 2}), so we use 99 to denote none.
    private static final int NONE = 99;

    // Name of the number system.
    private final String name;

    // The pre-period of the continued fraction.
    IntList preperiod;

    // The pre-period of the continued fraction.
    IntList period;

    // The continued fraction expansion of alpha. This is simply a concatenation of
    // preperiod and period.
    private final IntList alpha;

    // Size of the list alpha.
    private final int sz_alpha;

    // Maximum value in the continued fraction
    private int dMax;

    // Index where the period begins in alpha.
    private final int periodIndex;

    // Transitions in the 4-input adder. transition[p][q] = {r, s}.
    // This means state p transitions to state q on input r*d + s, where d is the current digit in
    // the continued fraction expansion of alpha.
    private int[][][] transition;

    // Maps to keep track of states and transitions.
    private final TreeMap<NodeState, Integer> nodeToIndex;
    private final TreeMap<Integer, NodeState> indexToNode;
    private TreeMap<Integer, Int2ObjectRBTreeMap<IntList>> stateTransitions;

    int totalNodes;

    Automaton adder;
    Automaton repr;

    public String getName() {
        return name;
    }

    public Ostrowski(String name, String preperiod, String period) {
        this.name = name;
        this.preperiod = new IntArrayList();
        this.period = new IntArrayList();
        ParseMethods.parseList(preperiod, this.preperiod);
        ParseMethods.parseList(period, this.period);


        // Remove leading 0's in the preperiod.
        Iterator<Integer> it = this.preperiod.iterator();
        int first_non_zero = 0;
        while (it.hasNext() && it.next() == 0) ++first_non_zero;
        this.preperiod.subList(0, first_non_zero).clear();

        if (this.preperiod.isEmpty()) {
            // Easier implementation.
            this.preperiod.addAll(this.period);
        }

        assertValues(this.preperiod);
        assertValues(this.period);

        if (this.preperiod.get(0) == 1) {
            // We want to restrict alpha < 1/2 because otherwise the first two place values in
            // the number system will be 1, which is troublesome.
            if (this.preperiod.size() > 1) {
                this.preperiod.set(0, this.preperiod.get(1) + 1);
                this.preperiod.remove(1);
            } else {
                this.preperiod.set(0, this.period.get(0) + 1);
                this.period.add(this.period.get(0));
                this.period.remove(0);
            }
        }

        this.alpha = new IntArrayList();
        this.alpha.add(0);
        this.alpha.addAll(this.preperiod);
        this.alpha.addAll(this.period);
        this.periodIndex = this.preperiod.size() + 1;
        this.sz_alpha = alpha.size();

        dMax = alpha.get(1) - 1;
        for (int i = 2; i < sz_alpha; ++i) {
            dMax = Math.max(alpha.get(i), dMax);
        }

        initTransitions();
        this.nodeToIndex = new TreeMap<>();
        this.indexToNode = new TreeMap<>();
        this.totalNodes = 0;
    }

    public Automaton createRepresentationAutomaton() {
        repr = initAutomaton(1);

        performReprBfs();
        repr.setQ(this.totalNodes);
        for (int q = 0; q < this.totalNodes; ++q) {
            repr.getO().add(isReprFinal(q) ? 1 : 0);
            this.stateTransitions.putIfAbsent(q, new Int2ObjectRBTreeMap<>());
            repr.getD().add(this.stateTransitions.get(q));
        }

        repr.minimize(null, false, "", null);
        repr.canonize();

        handleZeroState(repr);
        return repr;
    }

    private boolean isReprFinal(int q) {
        NodeState node = indexToNode.get(q);
        return node != null && node.getState() == 0 && node.getSeenIndex() == 1;
    }

    public static void writeRepresentation(String name, Automaton repr) {
        String repr_file_name =
                Session.getWriteAddressForCustomBases() + "msd_" + name + ".txt";
        System.out.println("Writing to: " + repr_file_name);
        File f = new File(repr_file_name);
        if (f.exists() && !f.isDirectory()) {
            throw new RuntimeException("Error: number system " + name + " already exists.");
        }
        AutomatonWriter.write(repr, repr_file_name);
        System.out.println("Ostrowski representation automaton created and written to file " + repr_file_name);
    }

    public Automaton createAdderAutomaton() {
        adder = initAutomaton(3);

        performAdderBfs();
        adder.setQ(this.totalNodes);
        for (int q = 0; q < this.totalNodes; q++) {
            adder.getO().add(isAdderFinal(q) ? 1 : 0);
            this.stateTransitions.putIfAbsent(q, new Int2ObjectRBTreeMap<>());
            adder.getD().add(this.stateTransitions.get(q));
        }

        adder.minimize(null, false, "", null);

        // We need to canonize and remove the first state.
        // The automaton will work with this state as well, but it is useless. This happens
        // because the Automaton class does not support an epsilon transition for NFAs.
        adder.canonize();

        handleZeroState(adder);
        return adder;
    }


    public static void writeAdder(String name, Automaton adder) {
        String adder_file_name =
                Session.getWriteAddressForCustomBases() + "msd_" + name + "_addition.txt";
        File f = new File(adder_file_name);
        if (f.exists() && !f.isDirectory()) {
            System.out.println("Warning: number system " + name + "was previously defined and is being overwritten.");
        }
        AutomatonWriter.write(adder, adder_file_name);
        System.out.println("Ostrowski adder automaton created and written to file " + adder_file_name);
    }

    private Automaton initAutomaton(int inputs) {
        resetAutomaton();
        Automaton automaton = new Automaton();

        // Declare the alphabet.
        automaton.setAlphabetSize(1);
        IntList list = new IntArrayList(dMax +1);
        for (int i = 0; i <= dMax; i++) {
            list.add(i);
        }

        // 3 inputs to the adder, all have the same alphabet and the null NumberSystem.
        int alphabetSize = 1;
        for(int i=0;i<inputs;i++) {
            automaton.getA().add(list);
            automaton.getNS().add(null);
            alphabetSize *= (dMax + 1);
        }
        automaton.setD(new ArrayList<>());
        automaton.setAlphabetSize(alphabetSize);
        automaton.setQ(0);
        return automaton;
    }

    private static void handleZeroState(Automaton adder) {
        boolean zeroStateNeeded =
            adder.getD().stream().anyMatch(
                tm -> tm.int2ObjectEntrySet().stream().anyMatch(
                    es -> es.getValue().getInt(0) == 0));

        if (!zeroStateNeeded) {
            adder.getD().remove(0);
            adder.getO().removeInt(0);
            adder.setQ(adder.getQ() - 1);
            adder.getD().forEach(tm -> {
                tm.forEach((k, v) -> {
                    int dest = v.getInt(0) - 1;
                    v.set(0, dest);
                });
            });
        }
    }

    public String toString() {
        return "name: " + this.name + ", alpha: " + this.alpha + ", period index: " + this.periodIndex;
    }

    private void assertValues(IntList list) {
        if (list.isEmpty()) {
            throw new RuntimeException("The period cannot be empty.");
        }
        for (int d : list) {
            if (d <= 0) {
                throw new RuntimeException(
                        "Error: All digits of the continued fraction must be positive integers.");
            }
        }
    }

    private int alphaI(int i) {
        int index = i < sz_alpha ? i : this.periodIndex + ((i - sz_alpha) % (sz_alpha - this.periodIndex));
        return alpha.get(index);
    }

    /** The transition matrix defines the behavior of the "4-input adder" automaton, which is used
     * in constructing the Ostrowski addition automaton. Each transition is parameterized by two
     * integers (r, s).
     * Conceptually:
     *   The automaton states (0 through 6) represent different "carry" or "configuration" conditions
     *   in the addition process under Ostrowski numeration.
     *   The transitions encode how to update the automaton's state given certain inputs and the
     *   Ostrowski system's arithmetic rules.
     */
     private void initTransitions() {
        transition = new int[NUM_STATES][NUM_STATES][2];
        for (int i = 0; i < NUM_STATES; i++) {
            for (int j = 0; j < NUM_STATES; j++) {
                transition[i][j][0] = NONE;
                transition[i][j][1] = NONE;
            }
        }

        transition[0][0][0] = 0;
        transition[0][0][1] = 0;
        transition[0][1][0] = 0;
        transition[0][1][1] = 1;

        transition[1][2][0] = -1;
        transition[1][2][1] = 0;
        transition[1][3][0] = -1;
        transition[1][3][1] = 1;
        transition[1][4][0] = -1;
        transition[1][4][1] = -1;

        transition[2][0][0] = 0;
        transition[2][0][1] = -1;
        transition[2][1][0] = 0;
        transition[2][1][1] = 0;

        transition[3][2][0] = -1;
        transition[3][2][1] = -1;
        transition[3][3][0] = -1;
        transition[3][3][1] = 0;
        transition[3][4][0] = -1;
        transition[3][4][1] = -2;

        transition[4][5][0] = 1;
        transition[4][5][1] = 0;
        transition[4][6][0] = 1;
        transition[4][6][1] = -1;

        transition[5][2][0] = -1;
        transition[5][2][1] = 1;
        transition[5][3][0] = -1;
        transition[5][3][1] = 2;
        transition[5][4][0] = -1;
        transition[5][4][1] = 0;

        transition[6][0][0] = 0;
        transition[6][0][1] = 1;
        transition[6][1][0] = 0;
        transition[6][1][1] = 2;
    }

    private void performAdderBfs() {
        // In a node, the indices mean the following.
        // 0: The state in the 4-input automaton.
        // 1: The C.F. index at which the input started.
        // 2: The C.F. index that is currently active in the input.

        // This is the start state.
        NodeState start_node = new NodeState(0, 0, 0);
        this.nodeToIndex.put(start_node, 0);
        this.indexToNode.put(0, start_node);
        ++this.totalNodes;

        Queue<Integer> queue = new LinkedList<>();
        this.stateTransitions = new TreeMap<>();

        // These are the "0" states.
        this.stateTransitions.put(0, new Int2ObjectRBTreeMap<>());
        for (int i = 1; i < this.sz_alpha; i++) {
            addNodeWithNewTransitions(new NodeState(0, i, i), 0, queue, 0);
        }

        int r, s;
        while (!queue.isEmpty()) {
            int cur_node_idx = queue.remove();
            NodeState cur_node = indexToNode.get(cur_node_idx);
            int state = cur_node.getState();
            int start_index = cur_node.getStartIndex();
            int seen_index = cur_node.getSeenIndex();

            if (seen_index == 1 && this.sz_alpha > 2 && this.periodIndex > 1) {
                // The input ends here.
                continue;
            }

            for (int st = 0; st < NUM_STATES; st++) {
                r = this.transition[state][st][0];
                s = this.transition[state][st][1];

                if (r == NONE || s == NONE) {
                    continue;
                }

                if (seen_index > 1) {
                    addTransitionsAndNode(new NodeState(st, start_index, seen_index - 1),
                        cur_node_idx, alphaI(seen_index - 1), r, s, queue);
                }

                if (seen_index == this.periodIndex) {
                    // There is another possibility.
                    // Next index could also be sz_alpha - 1.
                    addTransitionsAndNode(new NodeState(st, start_index, sz_alpha - 1),
                        cur_node_idx, alphaI(sz_alpha - 1), r, s, queue);
                }
            }
        }
    }

    private void addTransitionsAndNode(NodeState node, int cur_node_idx, int a, int r, int s, Queue<Integer> queue) {
        this.stateTransitions.putIfAbsent(cur_node_idx, new Int2ObjectRBTreeMap<>());
        if (nodeToIndex.containsKey(node)) {
            // This node already exists, don't create a new NodeState.
            addTransitions(
                this.stateTransitions.get(cur_node_idx),
                a * r + s,
                nodeToIndex.get(node),
                dMax);
        } else {
            addNodeWithNewTransitions(node, cur_node_idx, queue, a * r + s);
        }
    }


    private void performReprBfs() {
        // In a node, the indices mean the following.
        // 0: The state in the 2-input automaton.
        // 1: The C.F. index at which the input started.
        // 2: The C.F. index that is currently active in the input.

        // This is the start state.
        NodeState start_node = new NodeState(0, 0, 0);
        this.nodeToIndex.put(start_node, 0);
        this.indexToNode.put(0, start_node);
        ++this.totalNodes;

        Queue<Integer> queue = new LinkedList<>();
        this.stateTransitions = new TreeMap<>();
        int a;

        // These are the "0" states.
        this.stateTransitions.put(0, new Int2ObjectRBTreeMap<>());
        for (int i = 1; i < this.sz_alpha; ++i) {
            a = alphaI(i);
            addNodeIndices(new NodeState(0, i, i));
            for (int inp = 0; inp < a; ++inp) {
                putStateTransition(0, inp, this.totalNodes);
            }
            queue.add(this.totalNodes++);

            addNode(new NodeState(1, i, i), 0, queue, a);
        }

        while (!queue.isEmpty()) {
            int cur_node_idx = queue.remove();

            NodeState cur_node = indexToNode.get(cur_node_idx);
            int state = cur_node.getState();
            int start_index = cur_node.getStartIndex();
            int seen_index = cur_node.getSeenIndex();

            if (seen_index == 1 && this.sz_alpha > 2 && this.periodIndex > 1) {
                // The input ends here.
                continue;
            }

            if (seen_index > 1) {
                a = alphaI(seen_index - 1);
                if (state == 1) {
                    // Can only take a "0" transition from a "1" state.
                    a = 1;
                }

                this.stateTransitions.putIfAbsent(cur_node_idx, new Int2ObjectRBTreeMap<>());

                // Will go to state 0 for all transitions < a.
                pointToNode(a, new NodeState(0, start_index, seen_index - 1), cur_node_idx, queue);

                // Go to state 1 from this state 0 for transition = a (only if seen_index > 2).
                if (state == 0 && seen_index > 2) {
                    pointSymbolToNode(
                        new NodeState(1, start_index, seen_index - 1), cur_node_idx, queue, a);
                }
            }

            if (seen_index == this.periodIndex) {
                // There is another possibility.
                // Next index could also be sz_alpha - 1.
                a = alphaI(sz_alpha - 1);
                if (state == 1) {
                    // Can only take a "0" transition from a "1" state.
                    a = 1;
                }

                // Create the map if does not exist.
                this.stateTransitions.putIfAbsent(cur_node_idx, new Int2ObjectRBTreeMap<>());
                NodeState node = new NodeState(0, start_index, sz_alpha - 1);
                pointToNode(a, node, cur_node_idx, queue);

                // Go to state 1 from this state 0 for transition = a.
                if (state == 0) {
                    pointSymbolToNode(
                        new NodeState(1, start_index, sz_alpha - 1), cur_node_idx, queue, a);
                }
            }
        }
    }

    private void addNodeIndices(NodeState node) {
        nodeToIndex.put(node, this.totalNodes);
        indexToNode.put(this.totalNodes, node);
    }

    private void pointToNode(int a, NodeState node, int cur_node_idx, Queue<Integer> queue) {
        for (int inp = 0; inp < a; ++inp) {
            pointSymbolToNode(node, cur_node_idx, queue, inp);
        }
    }

    private void pointSymbolToNode(NodeState node, int cur_node_idx, Queue<Integer> queue, int inp) {
        if (nodeToIndex.containsKey(node)) {
            putStateTransition(cur_node_idx, inp, nodeToIndex.get(node));
        } else {
            addNode(node, cur_node_idx, queue, inp);
        }
    }

    // Create a new NodeState.
    private void addNode(NodeState node, int cur_node_idx, Queue<Integer> queue, int inp) {
        addNodeIndices(node);
        putStateTransition(cur_node_idx, inp, this.totalNodes);
        queue.add(this.totalNodes++);
    }

    // Create a new NodeState.
    private void addNodeWithNewTransitions(NodeState node, int cur_node_idx, Queue<Integer> queue, int inp) {
        addNodeIndices(node);
        addTransitions(this.stateTransitions.get(cur_node_idx), inp, this.totalNodes, dMax);
        queue.add(this.totalNodes++);
    }

    private void putStateTransition(int cur_node_idx, int inp, int value) {
        this.stateTransitions
            .get(cur_node_idx)
            .putIfAbsent(inp, new IntArrayList());
        this.stateTransitions
            .get(cur_node_idx)
            .get(inp)
            .add(value);
    }

    private static void addTransitions(
            Int2ObjectRBTreeMap<IntList> current_state_transitions,
            int diff,
            int encodedDestination,
            int d_max) {
        for (int x = 0; x <= d_max; ++x) {
            for (int y = 0; y <= d_max; ++y) {
                for (int z = 0; z <= d_max; ++z) {
                    if (z - x - y == diff) {
                        int input = inputEncode(d_max+1, x, y, z);
                        current_state_transitions.putIfAbsent(input, new IntArrayList());
                        current_state_transitions.get(input).add(encodedDestination);
                    }
                }
            }
        }
    }

    private static int inputEncode(int base, int x, int y, int z) {
        return x + base * y + base * base * z;
    }

    private boolean isAdderFinal(int node_index) {
        NodeState node = indexToNode.get(node_index);
        return (
                ((node.getState() == 0 || node.getState() == 2 || node.getState() == 6) &&
                        node.getSeenIndex() == 1));
    }

    private void resetAutomaton() {
        this.nodeToIndex.clear();
        this.indexToNode.clear();
        this.stateTransitions = new TreeMap<>();
        this.totalNodes = 0;
    }
}
