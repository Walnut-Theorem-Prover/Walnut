package Automata.FA;

import it.unimi.dsi.fastutil.ints.*;

import java.util.*;

public class Trimmer {
    private static final int INVALID_VALUE = -1;

    /**
     * Forward- and backward- trim unconnected states from the finite automaton
     */
    public static void trimAutomaton(FA a) {
        if (a.isTRUE_FALSE_AUTOMATON() || a.getQ() <= 1) {
            return;
        }
        IntSet initialStates = new IntOpenHashSet();
        initialStates.add(a.getQ0());
        IntSet trimmed = rightTrim(a.getAlphabetSize(), a.getNfaD(), initialStates);
        IntSet trimmed2 = leftTrim(a);
        trimmed.retainAll(trimmed2);
        quotient(a, trimmed);
    }

    /**
     * Quotient out dead states.
     */
    static void quotient(FA a, IntSet statesToKeep) {
        if (statesToKeep.isEmpty()) {
            // special case to make Walnut happy
            a.getO().clear();
            a.getO().add(0);
            a.getNfaD().clear();
            a.getNfaD().add(new Int2ObjectRBTreeMap<>());
            a.setQ0(0);
            a.setQ(1);
            return;
        }
        int oldQ = a.getQ();
        int newQ = statesToKeep.size();

        // determine mapping of old states to new ones
        int[] oldToNewMap = new int[oldQ];
        Arrays.fill(oldToNewMap, INVALID_VALUE);
        IntList oldO = a.getO();
        IntList newO = new IntArrayList(newQ);
        int oldq0 = a.getQ0();
        List<Int2ObjectRBTreeMap<IntList>> oldD = a.getNfaD();
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(newQ);
        // Add new states -- initial, accepting properties
        for (int i = 0; i < oldQ; i++) {
            if (statesToKeep.contains(i)) {
                newO.add(oldO.getInt(i));
                newD.add(new Int2ObjectRBTreeMap<>());
                int newState = newO.size() - 1;
                oldToNewMap[i] = newState;
                if (i == oldq0) {
                    a.setQ0(newState);
                }
            }
        }
        // Add transitions
        for (int i = 0; i < oldQ; i++) {
            if (oldToNewMap[i] == INVALID_VALUE) {
                continue;
            }
            Int2ObjectRBTreeMap<IntList> iMap = oldD.get(i);
            Int2ObjectRBTreeMap<IntList> newMap = newD.get(oldToNewMap[i]);
            for (int in = 0; in < a.getAlphabetSize(); in++) {
                IntList iList = iMap.get(in);
                if (iList == null) {
                    continue;
                }
                IntList newList = newMap.computeIfAbsent(in, (x -> new IntArrayList()));
                for (int k : iList) {
                    if (oldToNewMap[k] != INVALID_VALUE) {
                        newList.add(oldToNewMap[k]);
                    }
                }
            }
        }
        // q0 is already set
        a.setFields(newQ, newO, newD);
    }

    /**
     * Return the states that can reach final states.
     */
    public static IntSet leftTrim(FA a) {
        IntSet initialStates = a.getFinalStates(); // reversed -- final are now initial
        return rightTrim(a.getAlphabetSize(), flipTransitions(a.getNfaD()), initialStates);
    }

    /**
     * Return the states that are reachable from initial states.
     */
    public static IntSet rightTrim(
            int alphabetSize, List<Int2ObjectRBTreeMap<IntList>> d, IntSet initialStates) {
        IntSet found = new IntOpenHashSet();
        Deque<Integer> stack = new ArrayDeque<>();

        for (int init : initialStates) {
            stack.push(init);
            found.add(init);
        }

        while (!stack.isEmpty()) {
            Integer curr = stack.pop();
            Int2ObjectRBTreeMap<IntList> iMap = d.get(curr);
            for (int in = 0; in < alphabetSize; in++) {
                IntList iList = iMap.get(in);
                if (iList != null) {
                    for (int succState : iList) {
                        if (found.add(succState)) {
                            stack.push(succState);
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Flip (reverse) transitions.
     * @param d - original transitions
     * @return reversed transitions
     */
    private static List<Int2ObjectRBTreeMap<IntList>> flipTransitions(List<Int2ObjectRBTreeMap<IntList>> d) {
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(d.size());
        for (int q = 0; q < d.size(); q++) {
            newD.add(new Int2ObjectRBTreeMap<>());
        }
        for(int q = 0; q < d.size(); q++){
            Int2ObjectRBTreeMap<IntList> iList = d.get(q);
            for(Int2ObjectMap.Entry<IntList> transEntry : iList.int2ObjectEntrySet()) {
                for (int dest: transEntry.getValue()) {
                    newD.get(dest).computeIfAbsent(transEntry.getIntKey(), (x -> new IntArrayList())).add(q);
                }
            }
        }
        return newD;
    }
}

