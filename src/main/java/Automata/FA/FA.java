/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
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
package Automata.FA;

import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.*;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.fsa.impl.CompactNFA;

import java.util.*;

import static Main.Logging.*;

/**
 * Abstraction of NFA/DFA/DFAO code from Automaton.
 * TODO: fully abstract transitions such that this is explicitly an NFA or a DFA.
 */
public class FA implements Cloneable {

  // q0 is the initial state. Multiple initial states aren't supported.
  private int q0;

  // Q stores the number of states. For example when Q = 3, the set of states is {0,1,2}.
  private int Q;
  private int alphabetSize;

  // O stores the output of a state. In the case of DFA/NFA, a nonzero value means a final state,
  // and a value of zero means a non-final state.
  private IntList O;

  private TransitionsNFA t;

  private boolean canonized; // When true, states are sorted in breadth-first order

  private boolean TRUE_FALSE_AUTOMATON;
  private boolean TRUE_AUTOMATON = false;

  public FA() {
    O = new IntArrayList();
    t = new TransitionsNFA();
  }

  public boolean isAccepting(int state) {
    return O.getInt(state) != 0;
  }

  // Check if this is an FAO.
  public boolean isFAO() {
    for (int i=0;i<Q;i++) {
      if (O.getInt(i) > 1) {
        return true;
      }
    }
    return false;
  }

  public void initBasicFA(IntList O) {
    this.O = new IntArrayList(O);
    Q = O.size();
    for(int i = 0; i < Q; i++) {
      this.t.addMapToNfaD();
    }
  }

  @Override
  public String toString() {
    return "T/F:(" + TRUE_FALSE_AUTOMATON + "," + TRUE_AUTOMATON + ")" +
            "Q:" + Q + ", q0:" + q0 + ", canon: " + canonized + ", O:" + O +
            ", " + t;
  }

  public void clear() {
    O.clear();
    t.clearNfaD();
    canonized = false;
  }

  // NOTE: This will often create an NFA
  public static void starStates(FA automaton, FA N) {
    // N is a clone of automaton.
    // We add a new state which will be our new initial state.
    N.q0 = N.Q++;
    N.addOutput(true);  // The newly added state is a final state.
    N.t.addMapToNfaD();
    N.mergeInTransitions(N.Q, automaton.t.getEntriesNfaD(automaton.q0));
  }

  // NOTE: This will often create an NFA
  public static void concatStates(FA other, FA N, int originalQ) {
      // to access the other's states, just do q. To access the other's states in N, do originalQ + q.
      for (int q = 0; q < other.Q; q++) {
        N.O.add(other.O.getInt(q)); // add the output
        N.t.addMapToNfaD();
        for (Int2ObjectMap.Entry<IntList> entry : other.t.getEntriesNfaD(q)) {
          IntArrayList newTransitionMap = new IntArrayList(entry.getValue().size());
          for(int i: entry.getValue()) {
            newTransitionMap.add(originalQ + i);
          }
          N.t.setNfaDTransition(originalQ + q, entry.getIntKey(), newTransitionMap);
        }
      }

    N.mergeInTransitions(originalQ, N.t.getEntriesNfaD(originalQ));

    N.Q = originalQ + other.Q;
  }

  /**
   * Iterate through all of self's states. If they are final, add a transition to wherever the other's initial state goes.
   * NOTE: this can create an NFA.
   */
  private void mergeInTransitions(int originalQ, Set<Int2ObjectMap.Entry<IntList>> sourceEntrySet) {
    for (int q = 0; q < originalQ; q++) {
      if (!isAccepting(q)) {
        continue;
      }
      // otherwise, it is a final state, and we add our transitions.
      Int2ObjectRBTreeMap<IntList> destMap = t.getNfaState(q);
      for (Int2ObjectMap.Entry<IntList> entry : sourceEntrySet) {
        destMap.computeIfAbsent(entry.getIntKey(), s -> new IntArrayList()).addAll(entry.getValue());
      }
    }
  }

  /**
   * Sorts states based on their breadth-first order.
   * The method also removes states that are not reachable from the initial state.
   */
  public void canonizeInternal() {
    if (this.canonized || this.isTRUE_FALSE_AUTOMATON()) return;
    Int2IntMap permutationMap = determinePermutationMap();

    q0 = permutationMap.get(q0);
    int newQ = permutationMap.size();
    IntList newO = new IntArrayList(newQ);
    for (int q = 0; q < newQ; q++) {
      newO.add(0);
    }
    List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(newQ);
    for (int q = 0; q < newQ; q++) {
      newD.add(null);
    }

    for (int q = 0; q < Q; q++) {
      if (permutationMap.containsKey(q)) {
        newO.set(permutationMap.get(q), O.getInt(q));
        newD.set(permutationMap.get(q), t.getNfaState(q));
      }
    }

    Q = newQ;
    O = newO;
    t.setNfaD(newD);

    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.t.getEntriesNfaD(q)) {
        IntList newDestination = new IntArrayList();
        for (int p : entry.getValue()) {
          if (permutationMap.containsKey(p)) {
            newDestination.add(permutationMap.get(p));
          }
        }

        if (!newDestination.isEmpty()) {
          this.t.setNfaDTransition(q, entry.getIntKey(), newDestination);
        } else {
          t.getNfaState(q).remove(entry.getIntKey());
        }
      }
    }
    this.canonized = true;
  }

  //permutationMap holds the permutation we need to apply to Q. In other words if it = {(0,3),(1,10),...} then
  // we send Q[0] to Q[3] and Q[1] to Q[10]
  // Note this has a side effect of trimming the new states
  private Int2IntMap determinePermutationMap() {
    Queue<Integer> stateQueue = new LinkedList<>();
    stateQueue.add(q0);
    Int2IntMap permutationMap = new Int2IntOpenHashMap();
    permutationMap.put(q0, 0);
    int i = 1;
    while (!stateQueue.isEmpty()) {
      int q = stateQueue.poll();
      for (Int2ObjectMap.Entry<IntList> entry : this.t.getEntriesNfaD(q)) {
        for (int p : entry.getValue()) {
          if (!permutationMap.containsKey(p)) {
            permutationMap.put(p, i++);
            stateQueue.add(p);
          }
        }
      }
    }
    return permutationMap;
  }

  /**
   * This method adds a dead state to totalize the transition function
   */
  public void totalize(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    logMessage(print, prefix + TOTALIZING + ":" + Q + " states", log);
    //we first check if the automaton is totalized
    int sinkState = Q; // potential new dead state
    if (!totalizeStates(sinkState)) {
      addSinkState(0, sinkState);
    }
    long timeAfter = System.currentTimeMillis();
    logMessage(print, prefix + TOTALIZED + ":" + Q + " states - " + (timeAfter - timeBefore) + "ms", log);
  }


  /**
   * This method adds a dead state with an output one less than the minimum output number of the word automaton.
   * <p>
   * Return whether a dead state was even added.
   */
  public boolean addDistinguishedDeadState(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    logMessage(print, prefix + "Adding distinguished dead state: " + getQ() + " states", log);
    boolean totalized = this.totalizeStates(this.Q);
    int min;
    if (totalized) {
      min = 0;
    } else {
      min = determineMinOutput();
      addSinkState(min - 1, Q);
    }

    long timeAfter = System.currentTimeMillis();
    if (print) {
      String msg = prefix + "Already totalized, no distinguished state added: " + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
      if (!totalized) {
        msg = prefix + "Added distinguished dead state with output of " + (min - 1) + ": " + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
      }
      logMessage(true, msg, log);
    }
    return !totalized;
  }


  /**
   * Reverse NFA (or DFA), replacing with NFA.
   * Note that this returns initial state(s), since Walnut can't handle multiple initial states.
   * @return new initial state(s).
   */
  public IntSet reverseToNFAInternal(IntSet oldInitialStates) {
      // We change the direction of transitions first.
      List<Int2ObjectRBTreeMap<IntList>> newNfaD = new ArrayList<>(Q);
      for (int q = 0; q < Q; q++) newNfaD.add(new Int2ObjectRBTreeMap<>());

      // reverse NFA transitions
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : this.t.getEntriesNfaD(q)) {
          for (int dest : entry.getValue()) {
            addTransition(newNfaD, dest, entry.getIntKey(), q);
          }
        }
      }

      t.setNfaD(newNfaD);
      t.setDfaD(null); // this is explicitly an NFA now
      t.reduceMemory();
      
      IntSet newInitialStates = new IntOpenHashSet();
      // final states become initial states
      for (int q = 0; q < Q; q++) {
          if (isAccepting(q)) {
              newInitialStates.add(q);
              this.setOutputIfEqual(q, false);
          }
      }
      for(int initState: oldInitialStates) {
        this.setOutputIfEqual(initState, true); // initial states become final.
      }
      return newInitialStates;
  }

  static void addTransition(List<Int2ObjectRBTreeMap<IntList>> transitions,
                                    int state, int symbol, int destination) {
    IntList destList = transitions.get(state).get(symbol);
    if (destList == null) {
      destList = new IntArrayList();
      transitions.get(state).put(symbol, destList);
    }
    destList.add(destination);
  }

  private void addSinkState(int i, int sinkState) {
    // Add new non-accepting state that points to itself
    O.add(i);
    Q++;
    t.addMapToNfaD();
    addMissingTransitionsForState(t.getNfaState(sinkState), sinkState);
  }

  /**
   * Totalize states.
   * @param sinkState
   * @return whether previously total
   */
  private boolean totalizeStates(int sinkState) {
    boolean totalized = true;
    for (int q = 0; q < Q; q++) {
      if (addMissingTransitionsForState(t.getNfaState(q), sinkState)) {
        totalized = false;
      }
    }
    return totalized;
  }

  private boolean addMissingTransitionsForState(Int2ObjectRBTreeMap<IntList> iMap, int sinkState) {
    boolean added = false;
    for (int x = 0; x < alphabetSize; x++) {
      if (!iMap.containsKey(x)) {
        IntList pointToSink = new IntArrayList(1); // saves peak memory; often in fact, this is a DFA
        pointToSink.add(sinkState);
        iMap.put(x, pointToSink);
        added = true;
      }
    }
    return added;
  }

  public int getQ0() {
    return q0;
  }

  public void setQ0(int q0) {
    this.q0 = q0;
  }

  public int getQ() {
    return Q;
  }

  public void setQ(int q) {
    Q = q;
  }

  public IntList getO() {
    return O;
  }

  public void initO(int size) {
    this.O = new IntArrayList(size);
  }

  /**
   * Strong-type for NFA/DFA.
   * @param output
   */
  public void addOutput(boolean output) {
    O.add(output ? 1 : 0);
  }
  public void setOutputIfEqual(int idx, boolean output) {
    O.set(idx, output ? 1 : 0);
  }
  public void setOutputIfEqual(int output) {
    for (int j = 0; j < O.size(); j++) {
      this.setOutputIfEqual(j, O.getInt(j) == output);
    }
  }

  /**
   * Flip output.
   */
  public void flipOutput() {
    for (int q = 0; q < Q; q++)
      setOutputIfEqual(q, !isAccepting(q));
  }

  public int getAlphabetSize() {
    return alphabetSize;
  }

  public void setAlphabetSize(int alphabetSize) {
    this.alphabetSize = alphabetSize;
  }

  public FA clone() {
    FA fa = new FA();
    fa.Q = this.Q;
    fa.q0 = this.q0;
    fa.alphabetSize = this.alphabetSize;
    fa.O = new IntArrayList(this.O);
    fa.t = new TransitionsNFA();
    fa.canonized = this.canonized;
    for (int q = 0; q < fa.Q; q++) {
      fa.t.addMapToNfaD();
      for (Int2ObjectMap.Entry<IntList> entry : this.t.getEntriesNfaD(q)) {
        fa.t.setNfaDTransition(q, entry.getIntKey(), new IntArrayList(entry.getValue()));
      }
    }
    return fa;
  }

  /**
   * Permute entries of nfaD.
   * @param encodedInputPermutation
   */
  public void permuteNfaD(int[] encodedInputPermutation) {
    for (int q = 0; q < Q; q++) {
      Int2ObjectRBTreeMap<IntList> permutedNfaD = new Int2ObjectRBTreeMap<>();
      for (Int2ObjectMap.Entry<IntList> entry : t.getEntriesNfaD(q)) {
        permutedNfaD.put(encodedInputPermutation[entry.getIntKey()], entry.getValue());
      }
      t.getNfaD().set(q, permutedNfaD);
    }
  }

  /**
   * So for example if f is a final state and f is reachable from q by reading 0*
   * then q will be in the resulting set of this method.
   * Side effect: this may alter O.
   * @return true if this altered O.
   */
  public boolean setStatesReachableToFinalStatesByZeros(int zero) {
    Set<Integer> result = new HashSet<>();
    Queue<Integer> queue = new LinkedList<>();
    //this is the adjacency matrix of the reverse of the transition graph of this automaton on 0
    List<List<Integer>> adjacencyList = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) adjacencyList.add(new ArrayList<>());
    for (int q = 0; q < Q; q++) {
      IntList destination = t.getNfaStateDests(q, zero);
      if (destination != null) {
        for (int p : destination) {
          adjacencyList.get(p).add(q);
        }
      }
      if (isAccepting(q)) queue.add(q);
    }
    while (!queue.isEmpty()) {
      int q = queue.poll();
      result.add(q);
      for (int p : adjacencyList.get(q))
        if (!result.contains(p))
          queue.add(p);
    }
    boolean altered = false;
    for (int q : result) {
      altered = altered || (O.getInt(q) != 1);
      this.setOutputIfEqual(q, true);
    }
    return altered;
  }

  public void setFieldsFromFile(int newQ, int newQ0, Map<Integer, Integer> stateOutput,
                                Map<Integer, Int2ObjectRBTreeMap<IntList>> stateTransition) {
    Q = newQ;
    q0 = newQ0;
    for (int q = 0; q < newQ; q++) {
      O.add((int) stateOutput.get(q));
      this.t.addToNfaD(stateTransition.get(q));
    }
    t.reduceMemory();
  }

  /**
   * Check if automaton is deterministic (and total): each state must have exactly alphabetSize transitions
   */
  public boolean isDeterministicAndTotal() {
    for (int q = 0; q < Q; q++) {
      if (t.getNfaStateKeySet(q).size() != alphabetSize) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine minimum output in FA.
   * @return minimum output
   */
  public int determineMinOutput() {
    if (O.isEmpty()) {
      throw WalnutException.alphabetIsEmpty();
    }
    int minOutput = Integer.MAX_VALUE;
    for (int i = 0; i < O.size(); i++) {
      if (O.getInt(i) < minOutput) {
        minOutput = O.getInt(i);
      }
    }
    return minOutput;
  }

  public void setFields(int newStates, IntList newO, List<Int2ObjectRBTreeMap<IntList>> newD) {
      Q = newStates;
      O = newO;
      t.setNfaD(newD);
  }

  /**
   * Add new transition to nfaD. Note that this will overwrite previous transitions if it exists.
   */
  public void addNewTransition(int src, int dest, int inp) {
      IntList destStates = new IntArrayList();
      destStates.add(dest);
      t.setNfaDTransition(src, inp, destStates);
  }

  public IntSet getFinalStates() {
      IntSet finalStates = new IntOpenHashSet();
      for (int q = 0; q < O.size(); q++) {
          if (isAccepting(q)) {
              finalStates.add(q);
          }
      }
      return finalStates;
  }

  /**
   * We don't need to determinize here; just minimize.
   */
  public void justMinimize(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    logMessage(print, prefix + MINIMIZING + ": " + Q + " states.", log);

    this.convertNFAtoDFA();
    ValmariDFA v = new ValmariDFA(this, Q);
    this.t.setDfaD(null); // save memory
    v.minValmari(O);
    v.replaceFields(this); // TODO: we're using NFA representation, even though we know this is a DFA
    this.canonized = false;

    long timeAfter = System.currentTimeMillis();
    logMessage(
            print, prefix + MINIMIZED + ":" + Q + " states - " + (timeAfter - timeBefore) + "ms.", log);
  }

  public void setCanonized(boolean canonized) {
      this.canonized = canonized;
  }
  /**
   * When TRUE_FALSE_AUTOMATON = false, it means that this automaton is
   * an actual automaton and not one of the special automata: true or false
   * When TRUE_FALSE_AUTOMATON = true and TRUE_AUTOMATON = false then this is a false automaton.
   * When TRUE_FALSE_AUTOMATON = true and TRUE_AUTOMATON = true then this is a true automaton.
   */
  public boolean isTRUE_FALSE_AUTOMATON() {
    return TRUE_FALSE_AUTOMATON;
  }

  public void setTRUE_FALSE_AUTOMATON(boolean TRUE_FALSE_AUTOMATON) {
    this.TRUE_FALSE_AUTOMATON = TRUE_FALSE_AUTOMATON;
  }

  public boolean isTRUE_AUTOMATON() {
    return TRUE_AUTOMATON;
  }

  public String trueFalseString() {
    return Boolean.toString(TRUE_AUTOMATON);
  }

  public void setTRUE_AUTOMATON(boolean TRUE_AUTOMATON) {
    this.TRUE_AUTOMATON = TRUE_AUTOMATON;
  }

  /**
   Convert FA to CompactNFA representation, allowing additional initialState
   */
  public CompactNFA<Integer> FAtoCompactNFA(IntSet initialState) {
    CompactNFA<Integer> nfa = this.FAtoCompactNFA();
    // Replace initial states
    for(int i: nfa.getInitialStates()) {
      nfa.setInitial(i, false);
    }
    for(int i: initialState) {
      nfa.setInitial(i, true);
    }
    return nfa;
  }

  /**
   Convert FA to CompactNFA representation
   */
  public CompactNFA<Integer> FAtoCompactNFA() {
      CompactNFA<Integer> nfa = new CompactNFA<>(Alphabets.integers(0, this.alphabetSize - 1), this.Q);
      for (int i = 0; i < this.Q; i++) {
          nfa.addState(isAccepting(i));
      }
      nfa.setInitial(this.q0, true);
      for (int i = 0; i < this.Q; i++) {
        for (Int2ObjectMap.Entry<IntList> entry : t.getEntriesNfaD(i)) {
          nfa.addTransitions(i, entry.getIntKey(), entry.getValue());
        }
      }
      return nfa;
  }

  public static FA compactNFAToFA(CompactNFA<Integer> cNFA) {
    Set<Integer> initialStates = cNFA.getInitialStates();
    if (initialStates.size() > 1) {
      throw new WalnutException("Unexpected initial states from CompactNFA:" + initialStates);
    }

    FA fa = new FA();
    fa.Q = cNFA.size();
    fa.q0 = initialStates.iterator().next();
    for(int i=0;i<fa.Q;i++) {
      fa.addOutput(cNFA.isAccepting(i));
    }
    fa.alphabetSize = cNFA.getInputAlphabet().size();
    for(int i=0;i<fa.Q;i++) {
      Int2ObjectRBTreeMap<IntList> iMap = fa.t.addMapToNfaD();
      for(int in=0;in<fa.alphabetSize;in++) {
        Set<Integer> transDest = cNFA.getTransitions(i, in);
        if (transDest != null && !transDest.isEmpty()) {
          IntList iList = new IntArrayList(transDest);
          iMap.put(in, iList);
        }
      }
    }
    return fa;
  }

  public void setFromCompactDFA(CompactDFA<Integer> myDFA) {
    Q = myDFA.size();
    q0 = myDFA.getInitialState();
    O.clear();
    for(int i=0;i<Q;i++) {
      this.addOutput(myDFA.isAccepting(i));
    }
    alphabetSize = myDFA.getInputAlphabet().size();
    t.setNfaD(null);
    t.setDfaD(new ArrayList<>(Q));
    for(int i=0;i<Q;i++) {
      Int2IntMap iMap = t.addMapToDfaD();
      for(int in=0;in<alphabetSize;in++) {
        Integer dest = myDFA.getTransition(i, in);
        if (dest != null) {
          iMap.put(in, (int)dest);
        }
      }
    }
  }

  /**
   * Use DFA representation internally. Fails if not a DFA.
   */
  public void convertNFAtoDFA() {
    if (t.getNfaD() == null) {
      return; // nothing to do
    }
    if (!t.isDeterministic()) {
      throw new WalnutException("Unexpected NFA instead of DFA.");
    }
    t.setDfaD(new ArrayList<>(Q));
    for(int i=0;i<Q;i++) {
      Set<Int2ObjectMap.Entry<IntList>> sourceEntrySet = t.getEntriesNfaD(i);
      Int2IntMap iMap = t.addMapToDfaD();
      for(Int2ObjectMap.Entry<IntList> entry : sourceEntrySet) {
        iMap.put(entry.getIntKey(), entry.getValue().iterator().nextInt());
      }
    }
    t.setNfaD(null);
  }

  /**
   * Calculate new state output from metastates.
   */
  void calculateNewStateOutput(List<IntSet> metastates) {
    IntList oldO = new IntArrayList(O);
    this.initO(metastates.size());
    for (IntSet metastate : metastates) {
      // determine if metastate accepts
      boolean flag = false;
      for (int q : metastate) {
        if (oldO.getInt(q) != 0) {
          flag = true;
          break;
        }
      }
      this.addOutput(flag);
    }
  }

  /**
   * Determine if this FA accepts only the empty language.
   * We assume that this is an NFA.
   */
  public boolean isLanguageEmpty() {
    // No states at all => empty language
    if (getQ() <= 0) return true;

    // Accepts ε ?
    if (isAccepting(getQ0())) return false;

    // BFS from q0; stop as soon as we see an accepting state
    IntSet seen = new IntOpenHashSet();
    Deque<Integer> q = new ArrayDeque<>();
    seen.add(getQ0());
    q.add(getQ0());

    if (t.getNfaD() != null) {
      List<Int2ObjectRBTreeMap<IntList>> nfa = t.getNfaD();
      while (!q.isEmpty()) {
        int s = q.pop();
        Int2ObjectRBTreeMap<IntList> row = nfa.get(s);
        for (Int2ObjectMap.Entry<IntList> e : row.int2ObjectEntrySet()) {
          for (int t : e.getValue()) {
            if (seen.add(t)) {
              if (isAccepting(t)) return false;
              q.add(t);
            }
          }
        }
      }
    }
    return true;    // Never reached an accepting state
  }

  public TransitionsNFA getT() {
    return t;
  }

  public void setT(TransitionsNFA t) {
    this.t = t;
  }
}