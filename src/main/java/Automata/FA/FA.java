/*   Copyright 2025 John Nicol
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

import Automata.Automaton;
import Automata.RichAlphabet;
import Main.WalnutException;
import Main.UtilityMethods;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.*;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.fsa.impl.CompactNFA;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Abstraction of NFA/DFA/DFAO code from Automaton.
 * TODO: fully abstract transitions such that this is easily an NFA, DFA, or anywhere in between.
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

  /**
   * Transition function. For example, when d[0] = [(0,[1]),(1,[2,3]),(2,[2]),(3,[4]),(4,[1]),(5,[0])]
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
   */
  private List<Int2ObjectRBTreeMap<IntList>> nfaD; // transitions when this is an NFA -- null if this is a known DFA

  private List<Int2IntMap> dfaD; // memory-efficient transitions when this is a known DFA -- usually null
  private boolean canonized; // When true, states are sorted in breadth-first order

  private boolean TRUE_FALSE_AUTOMATON;
  private boolean TRUE_AUTOMATON = false;

  private static final int MAX_BRICS_CHARACTER = (1 << Character.SIZE) - 1;

  public FA() {
    O = new IntArrayList();
    nfaD = new ArrayList<>();
  }
  public boolean isAccepting(int state) {
    return O.getInt(state) != 0;
  }

  // Check if this is a DFAO.
  public boolean isDFAO() {
    for (int i=0;i<Q;i++) {
      if (O.getInt(i) > 1) {
        return true;
      }
    }
    return false;
  }

  public void alphabetStates(RichAlphabet oldAlphabet, Automaton M) {
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(M.fa.getQ());
      for (int q = 0; q < M.fa.getQ(); q++) {
          Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
          for (Int2ObjectMap.Entry<IntList> entry: getEntriesNfaD(q)) {
            List<Integer> decoded = oldAlphabet.decode(entry.getIntKey());
            if (M.richAlphabet.isInNewAlphabet(decoded)) {
              newMap.put(M.richAlphabet.encode(decoded), entry.getValue());
            }
          }
          newD.add(newMap);
      }
      M.fa.nfaD = newD;
  }

  public void initBasicFA(IntList O) {
    this.O = new IntArrayList(O);
    Q = O.size();
    for(int i = 0; i < Q; i++) {
      this.addToNfaD(new Int2ObjectRBTreeMap<>());
    }
  }

  public boolean equals(FA M) {
    if (isTRUE_FALSE_AUTOMATON() != M.isTRUE_FALSE_AUTOMATON()) return false;
    if (isTRUE_FALSE_AUTOMATON() && M.isTRUE_FALSE_AUTOMATON()) {
      return isTRUE_AUTOMATON() == M.isTRUE_AUTOMATON();
    }
    dk.brics.automaton.Automaton Y = M.toDkBricsAutomaton();
    dk.brics.automaton.Automaton X = this.toDkBricsAutomaton();
    return X.equals(Y);
  }

  @Override
  public String toString() {
    return "T/F:(" + TRUE_FALSE_AUTOMATON + "," + TRUE_AUTOMATON + ")" +
            "Q:" + Q + ", q0:" + q0 + ", canon: " + canonized + ", O:" + O + ", dfaD:" + dfaD + ", nfaD:" + nfaD;
  }

  public boolean isTotalized() {
      boolean totalized = true;
      for(int q = 0; q < Q; q++){
          for(int x = 0; x < alphabetSize; x++){
              if(!nfaD.get(q).containsKey(x)) {
                  totalized = false;
              }
              else if (nfaD.get(q).get(x).size() > 1) {
                  throw new WalnutException("Automaton must have at most one transition per input per state.");
              }
          }
      }
      return totalized;
  }

  /**
   * Build transitions (newD) from the final morphism matrix.
   * (Used in convertMsdBaseToExponent)
   */
  private List<Int2ObjectRBTreeMap<IntList>> buildTransitionsFromMorphism(List<List<Integer>> morphism) {
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
      for (int q = 0; q < Q; q++) {
          Int2ObjectRBTreeMap<IntList> transitionMap = new Int2ObjectRBTreeMap<>();
          List<Integer> row = morphism.get(q);
          for (int di = 0; di < row.size(); di++) {
              IntList list = new IntArrayList();
              list.add((int)row.get(di));
              transitionMap.put(di, list);
          }
          newD.add(transitionMap);
      }
      return newD;
  }

  /**
   * Extend morphism by applying the automaton transitions again.
   */
  public void updateTransitionsFromMorphism(int exponent) {
      List<List<Integer>> prevMorphism = buildInitialMorphism();
      // Repeatedly extend the morphism exponent-1 more times
      for (int i = 2; i <= exponent; i++) {
        List<List<Integer>> newMorphism = new ArrayList<>(Q);
        for (int j = 0; j < Q; j++) {
          List<Integer> extendedRow = new ArrayList<>();
          for (int k = 0; k < prevMorphism.get(j).size(); k++) {
            // For each digit di in state j:
            for (int di : nfaD.get(j).keySet()) {
              int nextState = nfaD
                  .get(prevMorphism.get(j).get(k))
                  .get(di)
                  .getInt(0);
              extendedRow.add(nextState);
            }
          }
          newMorphism.add(extendedRow);
        }
        prevMorphism = newMorphism;
      }
      // Create new transitions from the final morphism
      nfaD = buildTransitionsFromMorphism(prevMorphism);
  }

  public void clear() {
    O.clear();
    nfaD.clear();
    canonized = false;
  }

  public static void starStates(FA automaton, FA N) {
    // N is a clone of automaton.
    // We add a new state which will be our new initial state.
    N.q0 = N.Q++;;
    N.addOutput(true);  // The newly added state is a final state.
    N.addToNfaD(new Int2ObjectRBTreeMap<>());
    N.mergeInTransitions(N.Q, automaton.getEntriesNfaD(automaton.q0));
  }

  public static void concatStates(FA other, FA N, int originalQ) {
      // to access the other's states, just do q. To access the other's states in N, do originalQ + q.
      for (int q = 0; q < other.Q; q++) {
        N.O.add(other.O.getInt(q)); // add the output
        N.addToNfaD(new Int2ObjectRBTreeMap<>());
        for (Int2ObjectMap.Entry<IntList> entry : other.getEntriesNfaD(q)) {
          IntArrayList newTransitionMap = new IntArrayList(entry.getValue().size());
          for(int i: entry.getValue()) {
            newTransitionMap.add(originalQ + i);
          }
          N.setTransition(originalQ + q, newTransitionMap, entry.getIntKey());
        }
      }

    N.mergeInTransitions(originalQ, N.getEntriesNfaD(originalQ));

    N.Q = originalQ + other.Q;
  }

  /**
   * Iterate through all of self's states. If they are final, add a transition to wherever the other's initial state goes.
   */
  private void mergeInTransitions(int originalQ, Set<Int2ObjectMap.Entry<IntList>> sourceEntrySet) {
    for (int q = 0; q < originalQ; q++) {
      if (!isAccepting(q)) {
        continue;
      }
      // otherwise, it is a final state, and we add our transitions.
      Int2ObjectRBTreeMap<IntList> destMap = nfaD.get(q);
      for (Int2ObjectMap.Entry<IntList> entry : sourceEntrySet) {
        destMap.computeIfAbsent(entry.getIntKey(), s -> new IntArrayList()).addAll(entry.getValue());
      }
    }
  }

  public void canonizeInternal() {
    if (this.canonized || this.isTRUE_FALSE_AUTOMATON()) return;
    Queue<Integer> state_queue = new LinkedList<>();
    state_queue.add(q0);

    //map holds the permutation we need to apply to Q. In other words if map = {(0,3),(1,10),...} then
    // we send Q[0] to Q[3] and Q[1] to Q[10]
    Int2IntMap permutationMap = new Int2IntOpenHashMap();
    permutationMap.put(q0, 0);
    int i = 1;
    while (!state_queue.isEmpty()) {
      int q = state_queue.poll();
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        for (int p : entry.getValue()) {
          if (!permutationMap.containsKey(p)) {
            permutationMap.put(p, i++);
            state_queue.add(p);
          }
        }
      }
    }

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
        newD.set(permutationMap.get(q), nfaD.get(q));
      }
    }

    Q = newQ;
    O = newO;
    nfaD = newD;

    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        IntList newDestination = new IntArrayList();
        for (int p : entry.getValue()) {
          if (permutationMap.containsKey(p)) {
            newDestination.add(permutationMap.get(p));
          }
        }

        if (!newDestination.isEmpty()) {
          this.setTransition(q, newDestination, entry.getIntKey());
        } else {
          nfaD.get(q).remove(entry.getIntKey());
        }
      }
    }
    this.canonized = true;
  }

  public void convertBrics(List<Integer> alphabet, String regularExpression) {
    long timeBefore = System.currentTimeMillis();
    // For example if alphabet = {2,4,1} then intersectingRegExp = [241]*
    StringBuilder intersectingRegExp = new StringBuilder("[");
    for (int x : alphabet) {
      if (x < 0 || x > 9) {
        throw new WalnutException("the input alphabet of an automaton generated from a regular expression must be a subset of {0,1,...,9}");
      }
      intersectingRegExp.append(x);
    }
    intersectingRegExp.append("]*");
    regularExpression = "(" + regularExpression + ")&" + intersectingRegExp;

    dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
    dk.brics.automaton.Automaton M = RE.toAutomaton();
    M.minimize();

    alphabetSize = alphabet.size();

    convertBricsAutomatonToInternalRepresentation(M, (t, a) -> {
      // We only care about characters '0' through '9'
      int digit = a - '0';
      return alphabet.contains(digit) ? alphabet.indexOf(digit) : -1;
    });

    long timeAfter = System.currentTimeMillis();
    String msg = "computed ~:" + Q + " states - " + (timeAfter - timeBefore) + "ms";
    System.out.println(msg);
  }

  public void setFromBricsAutomaton(int alphabetSize, String regularExpression) {
    if (alphabetSize > ((1 << Character.SIZE) - 1)) {
      throw new WalnutException("size of input alphabet exceeds the limit of " + ((1 << Character.SIZE) - 1));
    }
    long timeBefore = System.currentTimeMillis();
    StringBuilder intersectingRegExp = new StringBuilder("[");
    for (int x = 0; x < alphabetSize; x++) {
      char nextChar = (char) (128 + x);
      intersectingRegExp.append(nextChar);
    }
    intersectingRegExp.append("]*");
    regularExpression = "(" + regularExpression + ")&" + intersectingRegExp;

    dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
    dk.brics.automaton.Automaton M = RE.toAutomaton();
    M.minimize();

    // We use packagedk.brics.automaton for automata minimization.
    if (!M.isDeterministic())
      throw WalnutException.bricsNFA();

    // Here, each character 'a' is used directly as the key.
    convertBricsAutomatonToInternalRepresentation(M, (t, a) -> (int) a);

    // We added 128 to the encoding of every input vector before to avoid reserved characters, now we subtract it again
    // to get back the standard encoding
    nfaD = this.addOffsetToInputs(-128);

    long timeAfter = System.currentTimeMillis();
    String msg = "computed ~:" + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
    System.out.println(msg);
  }

  private List<Int2ObjectRBTreeMap<IntList>> addOffsetToInputs(int offset) {
    List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) newD.add(new Int2ObjectRBTreeMap<>());
    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        newD.get(q).put(entry.getIntKey() + offset, entry.getValue());
      }
    }
    return newD;
  }

  private void convertBricsAutomatonToInternalRepresentation(
      dk.brics.automaton.Automaton M, BiFunction<Transition, Character, Integer> keyMapper) {
    List<State> setOfStates = new ArrayList<>(M.getStates());
    Q = setOfStates.size();
    q0 = setOfStates.indexOf(M.getInitialState());
    this.initO(Q);
    nfaD = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) {
      State state = setOfStates.get(q);
      this.addOutput(state.isAccept());
      this.addToNfaD(new Int2ObjectRBTreeMap<>());
      for (Transition t : state.getTransitions()) {
        for (char a = t.getMin(); a <= t.getMax(); a++) {
          int key = keyMapper.apply(t, a);
          if (key != -1) {
            addTransition(nfaD, q, key, setOfStates.indexOf(t.getDest()));
          }
        }
      }
    }
  }


  /**
   * This method adds a dead state to totalize the transition function
   *
   */
  public void totalize(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    UtilityMethods.logMessage(print, prefix + "totalizing:" + Q + " states", log);
    totalize();
    long timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(print, prefix + "totalized:" + Q + " states - " + (timeAfter - timeBefore) + "ms", log);
  }


  /**
   * This method adds a dead state with an output one less than the minimum output number of the word automaton.
   * <p>
   * Return whether a dead state was even added.
   */
  public boolean addDistinguishedDeadState(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    UtilityMethods.logMessage(print, prefix + "Adding distinguished dead state: " + getQ() + " states", log);
    boolean totalized = this.totalizeIfNecessary();
    int min = totalized ? 0 : this.obtainMinimumOutput();

    long timeAfter = System.currentTimeMillis();
    if (print) {
      String msg = prefix + "Already totalized, no distinguished state added: " + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
      if (!totalized) {
        msg = prefix + "Added distinguished dead state with output of " + (min - 1) + ": " + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
      }
      UtilityMethods.logMessage(true, msg, log);
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
        for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
          for (int dest : entry.getValue()) {
            addTransition(newNfaD, dest, entry.getIntKey(), q);
          }
        }
      }
      FA.reduceNfaDMemory(newNfaD);

      nfaD = newNfaD;
      dfaD = null; // this is explicitly an NFA now
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

  private static void addTransition(List<Int2ObjectRBTreeMap<IntList>> transitions,
                                    int state, int symbol, int destination) {
    IntList destList = transitions.get(state).get(symbol);
    if (destList == null) {
      destList = new IntArrayList();
      transitions.get(state).put(symbol, destList);
    }
    destList.add(destination);
  }

  private void totalize() {
    //we first check if the automaton is totalized
    int sinkState = Q; // potential new dead state
    if (!totalizeStates(sinkState)) {
      addSinkState(0, sinkState);
    }
  }

  boolean totalizeIfNecessary() {
    return totalizeStates(Q);
  }

  int obtainMinimumOutput() {
    int min = determineMinOutput();
    addSinkState(min - 1, Q);
    return min;
  }

  private void addSinkState(int i, int sinkState) {
    // Add new non-accepting state that points to itself
    O.add(i);
    Q++;
    nfaD.add(new Int2ObjectRBTreeMap<>());
    addMissingTransitionsForState(nfaD.get(sinkState), sinkState);
  }

  private boolean totalizeStates(int sinkState) {
    boolean totalized = true;
    for (int q = 0; q < Q; q++) {
      if (addMissingTransitionsForState(nfaD.get(q), sinkState)) {
        totalized = false;
      }
    }
    return totalized;
  }

  private boolean addMissingTransitionsForState(Int2ObjectRBTreeMap<IntList> iMap, int sinkState) {
    boolean added = false;
    for (int x = 0; x < alphabetSize; x++) {
      if (!iMap.containsKey(x)) {
        IntList pointToSink = new IntArrayList();
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

  public List<Int2ObjectRBTreeMap<IntList>> getNfaD(){
    return nfaD;
  }

  public Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state) {
    return nfaD.get(state).int2ObjectEntrySet();
  }

  public void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    this.nfaD = nfaD;
  }
  public void addToNfaD(Int2ObjectRBTreeMap<IntList> entry) {
    nfaD.add(entry);
  }

  public FA clone() {
    FA fa = new FA();
    fa.Q = this.Q;
    fa.q0 = this.q0;
    fa.alphabetSize = this.alphabetSize;
    fa.O = new IntArrayList(this.O);
    fa.nfaD = new ArrayList<>();
    fa.canonized = this.canonized;
    for (int q = 0; q < fa.Q; q++) {
      fa.addToNfaD(new Int2ObjectRBTreeMap<>());
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        fa.setTransition(q, new IntArrayList(entry.getValue()), entry.getIntKey());
      }
    }
    return fa;
  }

  public void setOutputIfEqual(int output) {
    for (int j = 0; j < O.size(); j++) {
      this.setOutputIfEqual(j, O.getInt(j) == output);
    }
  }

  /**
   * Permute entries of nfaD.
   * @param encodedInputPermutation
   */
  public void permuteNfaD(int[] encodedInputPermutation) {
    for (int q = 0; q < Q; q++) {
      Int2ObjectRBTreeMap<IntList> permutedNfaD = new Int2ObjectRBTreeMap<>();
      for (Int2ObjectMap.Entry<IntList> entry : getEntriesNfaD(q)) {
        permutedNfaD.put(encodedInputPermutation[entry.getIntKey()], entry.getValue());
      }
      nfaD.set(q, permutedNfaD);
    }
  }
  /**
   * Transform this automaton from Automaton to dk.brics.automaton.Automaton. This automaton can be
   * any automaton (deterministic/non-deterministic and with output/without output).
   *
   * @return
   */
  public dk.brics.automaton.Automaton toDkBricsAutomaton() {
    /**
     * Since the dk.brics.automaton uses char as its input alphabet for an automaton, then in order to transform
     * Automata.Automaton to dk.brics.automaton.Automata we've got to make sure, the input alphabet is less than
     * size of char which 2^16 - 1
     */
    if (getAlphabetSize() > MAX_BRICS_CHARACTER) {
      throw WalnutException.alphabetExceedsSize(MAX_BRICS_CHARACTER);
    }
    boolean deterministic = true;
    List<dk.brics.automaton.State> setOfStates = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) {
      setOfStates.add(new dk.brics.automaton.State());
      if (isAccepting(q)) setOfStates.get(q).setAccept(true);
    }
    dk.brics.automaton.State initialState = setOfStates.get(getQ0());
    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        for (int dest : entry.getValue()) {
          setOfStates.get(q).addTransition(new dk.brics.automaton.Transition((char) entry.getIntKey(), setOfStates.get(dest)));
        }
        if (entry.getValue().size() > 1) deterministic = false;
      }
    }
    dk.brics.automaton.Automaton M = new dk.brics.automaton.Automaton();
    M.setInitialState(initialState);
    M.restoreInvariant();
    M.setDeterministic(deterministic);
    return M;
  }

  /**
   * Returns the set of states reachable from the initial state by reading 0*
   *
   */
  public IntSet zeroReachableStates(int zero) {
    // Ensure q0 is initialized in nfaD
    IntList dQ0 = nfaD.get(q0).computeIfAbsent(zero, k -> new IntArrayList());
    if (!dQ0.contains(q0)) {
      dQ0.add(q0);
    }

    // Perform BFS to find zero-reachable states
    IntSet result = new IntOpenHashSet();
    Queue<Integer> queue = new LinkedList<>();
    queue.add(q0);

    while (!queue.isEmpty()) {
      int q = queue.poll();
      if (result.add(q)) { // Add q to result; skip if already processed
        IntList transitions = nfaD.get(q).get(zero);
        if (transitions != null) {
          for (int p : transitions) {
            if (!result.contains(p)) {
              queue.add(p);
            }
          }
        }
      }
    }

    return result;
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
      List<Integer> destination = nfaD.get(q).get(zero);
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
      this.addToNfaD(stateTransition.get(q));
    }
    reduceNfaDMemory(nfaD);
  }

  /**
   * Check if automaton is deterministic (amd total): each state must have exactly alphabetSize transitions
   */
  public boolean isDeterministic() {
    for (int q = 0; q < Q; q++) {
      if (nfaD.get(q).keySet().size() != alphabetSize) {
        return false;
      }
    }
    return true;
  }

  /**
   * Build the initial morphism from the automaton transitions.
   * (Used in convertMsdBaseToExponent)
   */
  private List<List<Integer>> buildInitialMorphism() {
    List<List<Integer>> result = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) {
      List<Integer> row = new ArrayList<>(alphabetSize);
      for (int di = 0; di < alphabetSize; di++) {
        row.add(nfaD.get(q).get(di).getInt(0));
      }
      result.add(row);
    }
    return result;
  }

  public int determineMinOutput() {
    if (O.isEmpty()) {
      throw WalnutException.alphabetIsEmpty();
    }
    int minOutput = 0;
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
      nfaD = newD;
  }

  /**
   * Add new transition to nfaD. Note that this will overwrite previous transitions if it exists.
   */
  public void addNewTransition(int src, int dest, int inp) {
      IntList destStates = new IntArrayList();
      destStates.add(dest);
      setTransition(src, destStates, inp);
  }
  public void setTransition(int src, IntList destStates, int inp) {
    nfaD.get(src).put(inp, destStates);
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
    UtilityMethods.logMessage(
            print, prefix + "Minimizing: " + Q + " states.", log);

    this.convertNFAtoDFA();
    ValmariDFA v = new ValmariDFA(this, Q);
    this.setDfaD(null); // save memory
    v.minValmari(O);
    v.replaceFields(this); // TODO: we're using NFA representation, even though we know this is a DFA
    this.canonized = false;

    long timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(
            print, prefix + "Minimized:" + Q + " states - " + (timeAfter - timeBefore) + "ms.", log);
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
          Int2ObjectRBTreeMap<IntList> iMap = nfaD.get(i);
          for (int in = 0; in < this.alphabetSize; in++) {
              IntList iList = iMap.get(in);
              if (iList != null) {
                  nfa.addTransitions(i, in, iList);
              }
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
      Int2ObjectRBTreeMap<IntList> iMap = new Int2ObjectRBTreeMap<>();
      fa.addToNfaD(iMap);
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
    nfaD = null;
    dfaD = new ArrayList<>(Q);
    for(int i=0;i<Q;i++) {
      Int2IntMap iMap = new Int2IntOpenHashMap();
      dfaD.add(iMap);
      for(int in=0;in<alphabetSize;in++) {
        Integer dest = myDFA.getTransition(i, in);
        if (dest != null) {
          iMap.put(in, (int)dest);
        }
      }
    }
  }

  public List<Int2IntMap> getDfaD() {
      return dfaD;
  }

  public void setDfaD(List<Int2IntMap> dfaD) {
      this.dfaD = dfaD;
  }

  /**
   * Reduce memory in DfaD by trimming all maps.
   */
  public void reduceDfaDMemory() {
    for (Int2IntMap int2IntMap : this.dfaD) {
      ((Int2IntOpenHashMap) int2IntMap).trim();
    }
  }

  /**
   * Reduce memory in DfaD by trimming all maps.
   */
  public static void reduceNfaDMemory(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    for (Int2ObjectRBTreeMap<IntList> iMap : nfaD) {
      for(IntList iList: iMap.values()) {
        ((IntArrayList)iList).trim();
      }
    }
  }

  /**
   * Use DFA representation internally. Fails if not a DFA.
   */
  public void convertNFAtoDFA() {
    if (nfaD == null) {
      return; // nothing to do
    }
    dfaD = new ArrayList<>(Q);
    for(int i=0;i<Q;i++) {
      Set<Int2ObjectMap.Entry<IntList>> sourceEntrySet = this.getEntriesNfaD(i);
      Int2IntMap iMap = new Int2IntOpenHashMap();
      dfaD.add(iMap);
      for(Int2ObjectMap.Entry<IntList> entry : sourceEntrySet) {
        if (entry.getValue().size() > 1) {
          throw new WalnutException("Unexpected NFA instead of DFA.");
        }
        iMap.put(entry.getIntKey(), entry.getValue().iterator().nextInt());
      }
    }
    nfaD = null;
  }

  /**
   * Calculate new state output from previous O and metastates.
   */
  void calculateNewStateOutput(IntList oldO, List<IntSet> metastates) {
    this.initO(metastates.size());
    for (IntSet metastate : metastates) {
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

  public long determineTransitionCount() {
    long numTransitionsLong = 0;
    if (nfaD == null) {
      for(int q = 0; q < dfaD.size();q++){
        numTransitionsLong += dfaD.get(q).keySet().size();
      }
    } else {
      for (int q = 0; q < nfaD.size();q++) {
        for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
          numTransitionsLong += entry.getValue().size();
        }
      }
    }
    return numTransitionsLong;
  }
}
