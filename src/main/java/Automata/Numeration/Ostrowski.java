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

import java.util.*;
import java.io.File;
import java.util.function.Predicate;

import Automata.*;
import Automata.FA.FA;
import Automata.Writer.AutomatonWriter;
import Main.Session;
import Main.WalnutException;
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
 * For mor details, see:
 * Baranwal, Aseem. Decision algorithms for Ostrowski-automatic sequences.
 *   MS thesis. University of Waterloo, 2020.
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

    // Maximum value in the continued fraction
    private int dMax;

    // Index where the period begins in alpha.
    private final int periodIndex;

    // Transitions in the 4-input adder. transition[p][q] = {r, s}.
    // This means state p transitions to state q on input r*d + s, where d is the current digit in
    // the continued fraction expansion of alpha.
    private final int[][][] adderTransitions;

    // Maps to keep track of states and transitions.
    private final Map<NodeState, Integer> nodeToIndex;
    private final Map<Integer, NodeState> indexToNode;
    private Map<Integer, Int2ObjectRBTreeMap<IntList>> stateTransitions;

    int totalNodes;

    public Ostrowski(String name, String preperiod, String period) {
        this.name = name;
        this.preperiod = new IntArrayList();
        this.period = new IntArrayList();
        ParseMethods.parseList(preperiod, this.preperiod);
        ParseMethods.parseList(period, this.period);

        // Remove leading 0's in the preperiod.
        removeLeadingZeroes(this.preperiod);

        if (this.preperiod.isEmpty()) {
            // Easier implementation.
            this.preperiod.addAll(this.period);
        }

        assertValues(this.preperiod);
        assertValues(this.period);

        if (this.preperiod.getInt(0) == 1) {
            // We want to restrict alpha < 1/2 because otherwise the first two place values in
            // the number system will be 1, which is troublesome.
            if (this.preperiod.size() > 1) {
                this.preperiod.set(0, this.preperiod.getInt(1) + 1);
                this.preperiod.removeInt(1);
            } else {
                this.preperiod.set(0, this.period.getInt(0) + 1);
                this.period.add(this.period.getInt(0));
                this.period.removeInt(0);
            }
        }

        this.alpha = new IntArrayList();
        this.alpha.add(0);
        this.alpha.addAll(this.preperiod);
        this.alpha.addAll(this.period);
        this.periodIndex = this.preperiod.size() + 1;

        dMax = alpha.getInt(1) - 1;
        for (int i = 2; i < alpha.size(); ++i) {
            dMax = Math.max(alpha.getInt(i), dMax);
        }

        this.adderTransitions = initAdderTransitions();
        this.nodeToIndex = new TreeMap<>();
        this.indexToNode = new TreeMap<>();
        this.totalNodes = 0;
    }

    private void removeLeadingZeroes(IntList iList) {
        Iterator<Integer> it = iList.iterator();
        int firstNonZero = 0;
        while (it.hasNext() && it.next() == 0) ++firstNonZero;
        iList.subList(0, firstNonZero).clear();
    }

    public Automaton createRepresentationAutomaton() {
        Automaton repr = initAutomaton(1);
        performReprBfs();
        populateAutomaton(repr, this::isReprFinal);
        return repr;
    }

    public Automaton createAdderAutomaton() {
        Automaton adder = initAutomaton(3);
        performAdderBfs();
        populateAutomaton(adder, this::isAdderFinal);
        return adder;
    }

    private boolean isReprFinal(NodeState node ) {
        return node != null && node.state() == 0 && node.seenIndex() == 1;
    }

    private boolean isAdderFinal(NodeState node) {
        return (node.state() == 0 || node.state() == 2 || node.state() == 6) && node.seenIndex() == 1;
    }

    private Automaton initAutomaton(int inputs) {
        // reset global fields.
        this.nodeToIndex.clear();
        this.indexToNode.clear();
        this.stateTransitions = new TreeMap<>();
        this.totalNodes = 0;

        // Declare the alphabet.
        IntList list = new IntArrayList(dMax + 1);
        for (int i = 0; i <= dMax; i++) {
            list.add(i);
        }
        // Inputs all have the same alphabet and the null NumberSystem.
        List<List<Integer>> A = new ArrayList<>(inputs);
        List<NumberSystem> ns = new ArrayList<>(inputs);
        for (int i = 0; i < inputs; i++) {
            A.add(list);
            ns.add(null);
        }

        Automaton automaton = new Automaton();
        automaton.richAlphabet.setA(A);
        automaton.setNS(ns);
        automaton.determineAlphabetSize();
        return automaton;
    }

    private void populateAutomaton(Automaton automaton, Predicate<NodeState> isStateFinal) {
        automaton.fa.setQ(this.totalNodes);
        for (int q = 0; q < automaton.fa.getQ(); ++q) {
            automaton.fa.addOutput(isStateFinal.test(indexToNode.get(q)));
            this.stateTransitions.putIfAbsent(q, new Int2ObjectRBTreeMap<>());
            automaton.fa.getT().addToNfaD(this.stateTransitions.get(q));
        }

        automaton.determinizeAndMinimize(false, "", null);

        // We need to canonize and remove the first state.
        // The automaton will work with this state as well, but it is useless. This happens
        // because the Automaton class does not support an epsilon transition for NFAs.
        automaton.canonize();

        handleZeroState(automaton.fa);
    }

    // Note: this is a minimized and canonized DFA.
    private static void handleZeroState(FA fa) {
        boolean zeroStateNeeded =
            fa.getT().getNfaD().stream().anyMatch(
                tm -> tm.int2ObjectEntrySet().stream().anyMatch(
                    es -> es.getValue().getInt(0) == 0));
        if (!zeroStateNeeded) {
            // remove 0th state
            fa.getT().getNfaD().remove(0);
            fa.getO().removeInt(0);
            fa.setQ(fa.getQ()-1);
            fa.getT().getNfaD().forEach(tm -> {
                tm.forEach((k, v) -> {
                    int dest = v.getInt(0) - 1;
                    v.set(0, dest);
                });
            });
        }
    }

    public static void writeAutomaton(String name, String fullName, Automaton a) {
        String automatonFileName = Session.getWriteAddressForCustomBases() + fullName;
        System.out.println("Writing to: " + automatonFileName);
        File f = new File(automatonFileName);
        if (f.exists()) {
            throw new WalnutException("Error: number system " + name + " already exists.");
        }
        AutomatonWriter.writeToTxtFormat(a, automatonFileName);
    }

    private void assertValues(IntList list) {
        if (list.isEmpty()) {
            throw new WalnutException("The period cannot be empty.");
        }
        for (int d : list) {
            if (d <= 0) {
                throw new WalnutException("Error: All digits of the continued fraction must be positive integers.");
            }
        }
    }

    private int alphaI(int i) {
        int alphaSize = alpha.size();
        int index = i < alphaSize ? i : this.periodIndex + ((i - alphaSize) % (alphaSize - this.periodIndex));
        return alpha.getInt(index);
    }

    private void performAdderBfs() {
        // In a node, the indices mean the following.
        // 0: The state in the 4-input automaton.
        // 1: The C.F. index at which the input started.
        // 2: The C.F. index that is currently active in the input.

        Queue<Integer> queue = initializeBfs();
        int alphaSize = this.alpha.size();
        for (int i = 1; i < alphaSize; i++) {
            addNodeWithNewTransitions(new NodeState(0, i, i), 0, queue, 0);
        }

        while (!queue.isEmpty()) {
            int curNodeIdx = queue.remove();
            NodeState curNode = indexToNode.get(curNodeIdx);
            int seenIndex = curNode.seenIndex();

            if (seenIndex == 1 && alphaSize > 2 && this.periodIndex > 1) {
                // The input ends here.
                continue;
            }

            int state = curNode.state();
            int startIndex = curNode.startIndex();

            for (int st = 0; st < NUM_STATES; st++) {
                int r = this.adderTransitions[state][st][0];
                int s = this.adderTransitions[state][st][1];

                if (r == NONE || s == NONE) {
                    continue;
                }

                if (seenIndex > 1) {
                    addTransitionsAndNode(new NodeState(st, startIndex, seenIndex - 1),
                        curNodeIdx, alphaI(seenIndex - 1), r, s, queue);
                }
                if (seenIndex == this.periodIndex) {
                    addTransitionsAndNode(new NodeState(st, startIndex, alphaSize - 1),
                        curNodeIdx, alphaI(alphaSize - 1), r, s, queue);
                }
            }
        }
    }

    private void performReprBfs() {
        // In a node, the indices mean the following.
        // 0: The state in the 2-input automaton.
        // 1: The C.F. index at which the input started.
        // 2: The C.F. index that is currently active in the input.

        Queue<Integer> queue = initializeBfs();
        int alphaSize = this.alpha.size();
        for (int i = 1; i < alphaSize; ++i) {
            int a = alphaI(i);
            addNodeIndices(new NodeState(0, i, i));
            for (int inp = 0; inp < a; ++inp) {
                putStateTransition(0, inp, this.totalNodes);
            }
            queue.add(this.totalNodes++);

            addNode(new NodeState(1, i, i), 0, queue, a);
        }

        while (!queue.isEmpty()) {
            int curNodeIdx = queue.remove();
            NodeState curNode = indexToNode.get(curNodeIdx);
            int seenIndex = curNode.seenIndex();

            if (seenIndex == 1 && alphaSize > 2 && this.periodIndex > 1) {
                // The input ends here.
                continue;
            }

            if (seenIndex > 1) {
                handleTransition(seenIndex, seenIndex - 1, curNode.state(),
                    curNodeIdx, curNode.startIndex(), seenIndex - 1, queue);
            }
            if (seenIndex == this.periodIndex) {
                handleTransition(seenIndex, alphaSize - 1, curNode.state(),
                    curNodeIdx, curNode.startIndex(), alphaSize - 1, queue);
            }
        }
    }

    private Queue<Integer> initializeBfs() {
        NodeState startNode = new NodeState(0, 0, 0); // This is the start state.
        this.nodeToIndex.put(startNode, 0);
        this.indexToNode.put(0, startNode);
        ++this.totalNodes;
        this.stateTransitions = new TreeMap<>();
        this.stateTransitions.put(0, new Int2ObjectRBTreeMap<>()); // These are the "0" states.
        return new LinkedList<>();
    }

    private void handleTransition(int seenIndex, int targetSeenIndex, int state, int curNodeIdx,
                                  int startIndex, int alphaIndex, Queue<Integer> queue) {
        this.stateTransitions.putIfAbsent(curNodeIdx, new Int2ObjectRBTreeMap<>());

        // Compute 'a' based on the state
        int a = state == 1 ? 1 : alphaI(alphaIndex);

        // Transition to state 0 for all values < a
        NodeState node = new NodeState(0, startIndex, targetSeenIndex);
        for (int inp = 0; inp < a; ++inp) {
            pointSymbolToNode(node, curNodeIdx, queue, inp);
        }

        // Transition to state 1 if needed
        if (state == 0 && (seenIndex > 2 || seenIndex == this.periodIndex)) {
            pointSymbolToNode(new NodeState(1, startIndex, targetSeenIndex), curNodeIdx, queue, a);
        }
    }

    private void addTransitionsAndNode(NodeState node, int curNodeIdx, int a, int r, int s, Queue<Integer> queue) {
        this.stateTransitions.putIfAbsent(curNodeIdx, new Int2ObjectRBTreeMap<>());
        if (nodeToIndex.containsKey(node)) {
            // This node already exists, don't create a new NodeState.
            addTransitions(
                this.stateTransitions.get(curNodeIdx),
                a * r + s,
                nodeToIndex.get(node),
                dMax);
        } else {
            addNodeWithNewTransitions(node, curNodeIdx, queue, a * r + s);
        }
    }

    private void addNodeIndices(NodeState node) {
        nodeToIndex.put(node, this.totalNodes);
        indexToNode.put(this.totalNodes, node);
    }

    private void pointSymbolToNode(NodeState node, int curNodeIdx, Queue<Integer> queue, int inp) {
        if (nodeToIndex.containsKey(node)) {
            putStateTransition(curNodeIdx, inp, nodeToIndex.get(node));
        } else {
            addNode(node, curNodeIdx, queue, inp);
        }
    }

    // Create a new NodeState.
    private void addNode(NodeState node, int curNodeIdx, Queue<Integer> queue, int inp) {
        addNodeIndices(node);
        putStateTransition(curNodeIdx, inp, this.totalNodes);
        queue.add(this.totalNodes++);
    }

    // Create a new NodeState.
    private void addNodeWithNewTransitions(NodeState node, int curNodeIdx, Queue<Integer> queue, int inp) {
        addNodeIndices(node);
        addTransitions(this.stateTransitions.get(curNodeIdx), inp, this.totalNodes, dMax);
        queue.add(this.totalNodes++);
    }

    private void putStateTransition(int curNodeIdx, int inp, int value) {
        this.stateTransitions.get(curNodeIdx).putIfAbsent(inp, new IntArrayList());
        this.stateTransitions.get(curNodeIdx).get(inp).add(value);
    }

    private static void addTransitions(
        Int2ObjectRBTreeMap<IntList> currentStateTransitions, int diff, int encodedDestination, int dMax) {
        int base = dMax + 1;
        for (int x = 0; x <= dMax; ++x) {
            for (int y = 0; y <= dMax; ++y) {
                int z = diff + x + y; // Only one possible value for z
                if (z >= 0 && z <= dMax) {
                    int inputEncode = x + base * y + base * base * z;
                    currentStateTransitions.computeIfAbsent(
                        inputEncode, k -> new IntArrayList()).add(encodedDestination);
                }
            }
        }
    }

    /**
     * The transition matrix defines the behavior of the "4-input adder" automaton, which is used
     * in constructing the Ostrowski addition automaton. Each transition is parameterized by two
     * integers (r, s).
     * Conceptually:
     * The automaton states (0 through 6) represent different "carry" or "configuration" conditions
     * in the addition process under Ostrowski numeration.
     * The transitions encode how to update the automaton's state given certain inputs and the
     * Ostrowski system's arithmetic rules.
     */
    private static int[][][] initAdderTransitions() {
        int[][][] adderTransitions = new int[NUM_STATES][NUM_STATES][2];
        for (int i = 0; i < NUM_STATES; i++) {
            for (int j = 0; j < NUM_STATES; j++) {
                adderTransitions[i][j][0] = adderTransitions[i][j][1] = NONE;
            }
        }

        adderTransitions[0][0][0] = 0;
        adderTransitions[0][0][1] = 0;
        adderTransitions[0][1][0] = 0;
        adderTransitions[0][1][1] = 1;

        adderTransitions[1][2][0] = -1;
        adderTransitions[1][2][1] = 0;
        adderTransitions[1][3][0] = -1;
        adderTransitions[1][3][1] = 1;
        adderTransitions[1][4][0] = -1;
        adderTransitions[1][4][1] = -1;

        adderTransitions[2][0][0] = 0;
        adderTransitions[2][0][1] = -1;
        adderTransitions[2][1][0] = 0;
        adderTransitions[2][1][1] = 0;

        adderTransitions[3][2][0] = -1;
        adderTransitions[3][2][1] = -1;
        adderTransitions[3][3][0] = -1;
        adderTransitions[3][3][1] = 0;
        adderTransitions[3][4][0] = -1;
        adderTransitions[3][4][1] = -2;

        adderTransitions[4][5][0] = 1;
        adderTransitions[4][5][1] = 0;
        adderTransitions[4][6][0] = 1;
        adderTransitions[4][6][1] = -1;

        adderTransitions[5][2][0] = -1;
        adderTransitions[5][2][1] = 1;
        adderTransitions[5][3][0] = -1;
        adderTransitions[5][3][1] = 2;
        adderTransitions[5][4][0] = -1;
        adderTransitions[5][4][1] = 0;

        adderTransitions[6][0][0] = 0;
        adderTransitions[6][0][1] = 1;
        adderTransitions[6][1][0] = 0;
        adderTransitions[6][1][1] = 2;
        return adderTransitions;
    }

    public String toString() {
        return "name: " + this.name + ", alpha: " + this.alpha + ", period index: " + this.periodIndex;
    }
}