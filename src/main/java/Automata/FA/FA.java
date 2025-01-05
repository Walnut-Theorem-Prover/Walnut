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
import Main.ExceptionHelper;
import Main.UtilityMethods;
import Token.RelationalOperator;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.*;
import net.automatalib.alphabet.Alphabets;
import net.automatalib.automaton.fsa.CompactNFA;

import java.util.*;

/**
 * Abstraction of NFA/DFA/DFAO code from Automaton.
 */
public class FA implements Cloneable {
  private static int AutomataIndex = 0; // Indicates the index of the automata in a particular run
  private int q0;
  private int Q;
  private int alphabetSize;
  private IntList O;
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

  /**
   * Used when we're done with a particular run.
   */
  public static void resetIndex() {
      AutomataIndex = 0;
  }
  public static int IncrementIndex() {
    return AutomataIndex++;
  }

  public boolean equals(FA M) {
    if (isTRUE_FALSE_AUTOMATON() != M.isTRUE_FALSE_AUTOMATON()) return false;
    if (isTRUE_FALSE_AUTOMATON() && M.isTRUE_FALSE_AUTOMATON()) {
      return isTRUE_AUTOMATON() == M.isTRUE_AUTOMATON();
    }
    dk.brics.automaton.Automaton Y = M.to_dk_brics_automaton();
    dk.brics.automaton.Automaton X = this.to_dk_brics_automaton();
    return X.equals(Y);
  }

  @Override
  public String toString() {
    return "T/F:(" + TRUE_FALSE_AUTOMATON + "," + TRUE_AUTOMATON + ")" +
            "Q:" + Q + ", q0:" + q0 + ", canon: " + canonized + ", O:" + O + ", dfaD:" + dfaD + ", nfaD:" + nfaD;
  }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method changes the word A
     * to a DFA that accepts x iff this[x] < o lexicographically.
     * To be used only when this A is a DFAO (word).
     *
     * @param operator
     */
    public void compare(
            int o, String operator, boolean print, String prefix, StringBuilder log) {
        long timeBefore = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "comparing (" + operator + ") against " + o + ":" + Q + " states", log);
        for (int p = 0; p < getQ(); p++) {
            O.set(p, RelationalOperator.compare(operator, O.getInt(p), o) ? 1 : 0);
        }
        determinizeAndMinimize(print, prefix + " ", log);
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(print, prefix + "compared (" + operator + ") against " + o + ":" + Q + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

    public boolean isTotalized() {
      boolean totalized = true;
      for(int q = 0; q < Q; q++){
          for(int x = 0; x < alphabetSize; x++){
              if(!nfaD.get(q).containsKey(x)) {
                  totalized = false;
              }
              else if (nfaD.get(q).get(x).size() > 1) {
                  throw new RuntimeException("Automaton must have at most one transition per input per state.");
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
              list.add(row.get(di));
              transitionMap.put(di, list);
          }
          newD.add(transitionMap);
      }
      return newD;
  }

  public void updateTransitionsFromMorphism(int exponent) {
      List<List<Integer>> prevMorphism = buildInitialMorphism();
      // Repeatedly extend the morphism exponent-1 more times
      for (int i = 2; i <= exponent; i++) {
          prevMorphism = extendMorphism(prevMorphism);
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
    int newState = N.Q;
    N.O.add(1); // The newly added state is a final state.
    N.nfaD.add(new Int2ObjectRBTreeMap<>());
    N.setQ0(newState);
    N.setQ(N.getQ() + 1);
    Int2ObjectRBTreeMap<IntList> sourceMap = automaton.nfaD.get(automaton.getQ0());
    for (int q = 0; q < N.getQ(); q++) {
      if (N.O.getInt(q) == 0) continue;  // only handle final states
      // Merge transitions from automaton's initial state's transitions
      Int2ObjectRBTreeMap<IntList> destMap = N.nfaD.get(q);
      for (Int2ObjectMap.Entry<IntList> entry : sourceMap.int2ObjectEntrySet()) {
        destMap.computeIfAbsent(entry.getIntKey(), k -> new IntArrayList()).addAll(entry.getValue());
      }
    }
  }

  public static void concatStates(FA other, FA N, int originalQ) {
      // to access the other's states, just do q. To access the other's states in N, do originalQ + q.
      for (int q = 0; q < other.getQ(); q++) {
        N.O.add(other.O.getInt(q)); // add the output
        N.nfaD.add(new Int2ObjectRBTreeMap<>());
        for (Int2ObjectMap.Entry<IntList> entry : other.nfaD.get(q).int2ObjectEntrySet()) {
          int symbol = entry.getIntKey();
          IntList oldDestinations = entry.getValue();
          IntArrayList newTransitionMap = new IntArrayList(oldDestinations.size());
          for (int i = 0; i < oldDestinations.size(); i++) {
            newTransitionMap.add(oldDestinations.getInt(i) + originalQ);
          }
          N.nfaD.get(originalQ + q).put(symbol, newTransitionMap);
        }
      }

      // now iterate through all of self's states. If they are final, add a transition to wherever the other's
      // initial state goes.
      for (int q = 0; q < originalQ; q++) {
        if (N.O.getInt(q) == 0) { // if it is NOT a final state
          continue;
        }

        // otherwise, it is a final state, and we add our transitions.
        for (Int2ObjectMap.Entry<IntList> entry : N.nfaD.get(originalQ).int2ObjectEntrySet()) {
          N.nfaD.get(q).computeIfAbsent(entry.getIntKey(), s -> new IntArrayList()).addAll(entry.getValue());
        }
      }
      N.setQ(originalQ + other.getQ());
  }

  public static void alphabetStates(Automaton automaton, List<List<Integer>> alphabet, Automaton M) {
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
      for (int q = 0; q < M.getQ(); q++) {
          Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
          for (Int2ObjectMap.Entry<IntList> entry: automaton.getD().get(q).int2ObjectEntrySet()) {
            List<Integer> decoded = Automaton.decode(automaton.getA(), entry.getIntKey());
            if (isInNewAlphabet(alphabet, decoded)) {
              newMap.put(M.encode(decoded), entry.getValue());
            }
          }
          newD.add(newMap);
      }
      M.fa.nfaD = newD;
  }

  private static boolean isInNewAlphabet(List<List<Integer>> alphabet, List<Integer> decoded) {
    for (int i = 0; i < decoded.size(); i++) {
      if (!alphabet.get(i).contains(decoded.get(i))) {
        return false;
      }
    }
    return true;
  }

  public void canonizeInternal() {
    if (this.canonized || this.isTRUE_FALSE_AUTOMATON()) return;
    Queue<Integer> state_queue = new LinkedList<>();
    state_queue.add(q0);

    /**map holds the permutation we need to apply to Q. In other words if map = {(0,3),(1,10),...} then
     *we have got to send Q[0] to Q[3] and Q[1] to Q[10]*/
    Int2IntMap map = new Int2IntOpenHashMap();
    map.put(q0, 0);
    int i = 1;
    while (!state_queue.isEmpty()) {
      int q = state_queue.poll();
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
        for (int p : entry.getValue()) {
          if (!map.containsKey(p)) {
            map.put(p, i++);
            state_queue.add(p);
          }
        }
      }
    }

    q0 = map.get(q0);
    int newQ = map.size();
    IntList newO = new IntArrayList(newQ);
    for (int q = 0; q < newQ; q++) {
      newO.add(0);
    }
    for (int q = 0; q < Q; q++) {
      if (map.containsKey(q)) {
        newO.set(map.get(q), O.getInt(q));
      }
    }

    List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(newQ);
    for (int q = 0; q < newQ; q++) {
      newD.add(null);
    }

    for (int q = 0; q < Q; q++) {
      if (map.containsKey(q)) {
        newD.set(map.get(q), nfaD.get(q));
      }
    }

    Q = newQ;
    O = newO;
    nfaD = newD;

    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
        IntList newDestination = new IntArrayList();
        for (int p : entry.getValue()) {
          if (map.containsKey(p)) {
            newDestination.add(map.get(p));
          }
        }

        if (!newDestination.isEmpty()) {
          nfaD.get(q).put(entry.getIntKey(), newDestination);
        } else {
          nfaD.get(q).remove(entry.getIntKey());
        }
      }
    }
    this.canonized = true;
  }

  public void addOffsetToInputs(int offset) {
      List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>(Q);
      for (int q = 0; q < Q; q++) new_d.add(new Int2ObjectRBTreeMap<>());
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
          new_d.get(q).put(entry.getIntKey() + offset, entry.getValue());
        }
      }
      nfaD = new_d;
  }

  public void convertBrics(List<Integer> alphabet, dk.brics.automaton.Automaton M) {
    alphabetSize = alphabet.size();
    List<State> setOfStates = new ArrayList<>(M.getStates());
    Q = setOfStates.size();
    q0 = setOfStates.indexOf(M.getInitialState());
    for (int q = 0; q < Q; q++) {
      State state = setOfStates.get(q);
      O.add(state.isAccept() ? 1 : 0);
      Int2ObjectRBTreeMap<IntList> currentStatesTransitions = new Int2ObjectRBTreeMap<>();
      nfaD.add(currentStatesTransitions);
      for (Transition t : state.getTransitions()) {
        for (char a = UtilityMethods.max(t.getMin(), '0'); a <= UtilityMethods.min(t.getMax(), '9'); a++) {
          if (alphabet.contains(a - '0')) {
            IntList dest = new IntArrayList();
            dest.add(setOfStates.indexOf(t.getDest()));
            currentStatesTransitions.put(alphabet.indexOf(a - '0'), dest);
          }
        }
      }
    }
  }

  public void setFromBricsAutomaton(dk.brics.automaton.Automaton M, List<State> setOfStates) {
    Q = setOfStates.size();
    q0 = setOfStates.indexOf(M.getInitialState());
    O = new IntArrayList();
    nfaD = new ArrayList<>();
    for (int q = 0; q < Q; q++) {
      State state = setOfStates.get(q);
      O.add(state.isAccept() ? 1 : 0);
      Int2ObjectRBTreeMap<IntList> currentStatesTransitions = new Int2ObjectRBTreeMap<>();
      nfaD.add(currentStatesTransitions);
      for (Transition t : state.getTransitions()) {
        for (char a = t.getMin(); a <= t.getMax(); a++) {
          IntList dest = new IntArrayList();
          dest.add(setOfStates.indexOf(t.getDest()));
          currentStatesTransitions.put(a, dest);
        }
      }
    }
  }

  public void totalize() {
      //we first check if the automaton is totalized
      boolean totalized = true;
      for (int q = 0; q < Q; q++) {
          for (int x = 0; x < alphabetSize; x++) {
              if (!nfaD.get(q).containsKey(x)) {
                // point every missing transition to new state
                IntList nullState = new IntArrayList();
                nullState.add(Q);
                nfaD.get(q).put(x, nullState);
                totalized = false;
              }
          }
      }
      if (!totalized) {
        // Add new state, point new state to itself
        int newState = Q;
        O.add(0);
        Q++;
        nfaD.add(new Int2ObjectRBTreeMap<>());
        for (int x = 0; x < alphabetSize; x++) {
          IntList nullState = new IntArrayList();
          nullState.add(newState);
          nfaD.get(newState).put(x, nullState);
        }
      }
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
      log.append(msg + System.lineSeparator());
      System.out.println(msg);
    }
    return !totalized;
  }

  int obtainMinimumOutput() {
    int min = determineMinOutput();
    O.add(min - 1);
    Q++;
    nfaD.add(new Int2ObjectRBTreeMap<>());
    for (int x = 0; x < alphabetSize; x++) {
      IntList nullState = new IntArrayList();
      nullState.add(Q - 1);
      nfaD.get(Q - 1).put(x, nullState);
    }
    return min;
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
      if (nfaD != null) {
        // reverse NFA transitions
        for (int q = 0; q < Q; q++) {
          for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
            for (int dest : entry.getValue()) {
              addTransition(newNfaD, dest, entry.getIntKey(), q);
            }
          }
        }
      } else {
        // reverse DFA transitions
        for (int q = 0; q < Q; q++) {
          for (Int2IntMap.Entry entry : dfaD.get(q).int2IntEntrySet()) {
            addTransition(newNfaD, entry.getIntValue(), entry.getIntKey(), q);
          }
        }
      }
      nfaD = newNfaD;
      IntSet newInitialStates = new IntOpenHashSet();
      // final states become initial states
      for (int q = 0; q < Q; q++) {
          if (O.getInt(q) != 0) {
              newInitialStates.add(q);
              O.set(q, 0);
          }
      }
      for(int initState: oldInitialStates) {
        O.set(initState, 1); // initial states become final.
      }
      return newInitialStates;
  }

  private static void addTransition(List<Int2ObjectRBTreeMap<IntList>> newNfaD, int dest, int symbol, int q) {
    if (newNfaD.get(dest).containsKey(symbol))
      newNfaD.get(dest).get(symbol).add(q);
    else {
      IntList destinationSet = new IntArrayList();
      destinationSet.add(q);
      newNfaD.get(dest).put(symbol, destinationSet);
    }
  }

  boolean totalizeIfNecessary() {
      //we first check if the automaton is totalized
      boolean totalized = true;
      for (int q = 0; q < Q; q++) {
          for (int x = 0; x < alphabetSize; x++) {
              if (!nfaD.get(q).containsKey(x)) {
                  IntList nullState = new IntArrayList();
                  nullState.add(Q);
                  nfaD.get(q).put(x, nullState);
                  totalized = false;
              }
          }
      }
      return totalized;
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

  public void setO(IntList o) {
    O = o;
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

  public void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    this.nfaD = nfaD;
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
      fa.nfaD.add(new Int2ObjectRBTreeMap<>());
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
        fa.nfaD.get(q).put(entry.getIntKey(), new IntArrayList(entry.getValue()));
      }
    }
    return fa;
  }

  public void setOutput(int output) {
    for (int j = 0; j < O.size(); j++) {
      O.set(j, O.getInt(j) == output ? 1 : 0);
    }
  }



  public void permuteD(int[] encoded_input_permutation) {
    for (int q = 0; q < Q; q++) {
      Int2ObjectRBTreeMap<IntList> permuted_d = new Int2ObjectRBTreeMap<>();
      for (Int2ObjectMap.Entry<IntList> entry : getNfaD().get(q).int2ObjectEntrySet()) {
        permuted_d.put(encoded_input_permutation[entry.getIntKey()], entry.getValue());
      }
      nfaD.set(q, permuted_d);
    }
  }
  /**
   * Transform this automaton from Automaton to dk.brics.automaton.Automaton. This automaton can be
   * any automaton (deterministic/non-deterministic and with output/without output).
   *
   * @return
   */
  public dk.brics.automaton.Automaton to_dk_brics_automaton() {
    /**
     * Since the dk.brics.automaton uses char as its input alphabet for an automaton, then in order to transform
     * Automata.Automaton to dk.brics.automaton.Automata we've got to make sure, the input alphabet is less than
     * size of char which 2^16 - 1
     */
    if (getAlphabetSize() > MAX_BRICS_CHARACTER) {
      throw ExceptionHelper.alphabetExceedsSize(MAX_BRICS_CHARACTER);
    }
    boolean deterministic = true;
    List<dk.brics.automaton.State> setOfStates = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) {
      setOfStates.add(new dk.brics.automaton.State());
      if (O.getInt(q) != 0) setOfStates.get(q).setAccept(true);
    }
    dk.brics.automaton.State initialState = setOfStates.get(getQ0());
    for (int q = 0; q < Q; q++) {
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
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
      if (O.getInt(q) != 0) queue.add(q);
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
      O.set(q, 1);
    }
    return altered;
  }

  public void setFieldsFromFile(int newQ, int newQ0, Map<Integer, Integer> state_output,
                                Map<Integer, Int2ObjectRBTreeMap<IntList>> state_transition) {
    Q = newQ;
    q0 = newQ0;
    for (int q = 0; q < newQ; q++) {
      O.add((int) state_output.get(q));
      nfaD.add(state_transition.get(q));
    }
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

  /**
   * Extend morphism by applying the automaton transitions again.
   * (Used in convertMsdBaseToExponent)
   */
  private List<List<Integer>> extendMorphism(List<List<Integer>> prev) {
    List<List<Integer>> newMorphism = new ArrayList<>();
    for (int j = 0; j < Q; j++) {
      List<Integer> extendedRow = new ArrayList<>();
      for (int k = 0; k < prev.get(j).size(); k++) {
        // For each digit di in state j:
        for (int di : nfaD.get(j).keySet()) {
          int nextState = nfaD
              .get(prev.get(j).get(k))
              .get(di)
              .getInt(0);
          extendedRow.add(nextState);
        }
      }
      newMorphism.add(extendedRow);
    }
    return newMorphism;
  }

  public int determineMinOutput() {
    if (O.isEmpty()) {
      throw ExceptionHelper.alphabetIsEmpty();
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

  public void addTransition(int src, int dest, int inp) {
      IntList destStates = new IntArrayList();
      destStates.add(dest);
      nfaD.get(src).put(inp, destStates);
  }

  public IntSet getFinalStates() {
      IntSet finalStates = new IntOpenHashSet();
      for (int q = 0; q < O.size(); q++) {
          if (O.getInt(q) > 0) {
              finalStates.add(q);
          }
      }
      return finalStates;
  }

  public void determinizeAndMinimize(boolean print, String prefix, StringBuilder log) {
    // Working with NFA. Let's trim.
    int oldQ = this.Q;
    Trimmer.trimAutomaton(this);
    if (oldQ != this.Q) {
      UtilityMethods.logMessage(print, prefix + "Trimmed to: " + getQ() + " states.", log);
    }
    IntSet qqq = new IntOpenHashSet();
    qqq.add(q0);
    determinizeAndMinimize(null, qqq, print, prefix, log);
  }

  /**
   * Determinize and minimize. Technically, the logging is backwards.
   */
  public void determinizeAndMinimize(List<Int2IntMap> dfaD, IntSet qqq, boolean print, String prefix, StringBuilder log) {
    DeterminizationStrategies.determinize(
            this, dfaD, qqq, print, prefix + " ", log, DeterminizationStrategies.Strategy.SC);
    justMinimize(print, prefix + " ", log);
  }

  /**
   * We don't need to determinize here; just minimize.
   */
  public void justMinimize(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    UtilityMethods.logMessage(
            print, prefix + "Minimizing: " + Q + " states.", log);

    minimize();

    long timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(
            print, prefix + "Minimized:" + Q + " states - " + (timeAfter - timeBefore) + "ms.", log);
  }

  public void minimize() {
    ValmariDFA v = new ValmariDFA(this.dfaD, Q);
    v.minValmari(O);
    Q = v.blocks.z;
    q0 = v.blocks.S[q0];
    O = v.determineO();
    nfaD = v.determineD();
    this.dfaD = null; // TODO: we're using NFA representation, even though we know this is a DFA
    this.canonized = false;
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

  public CompactNFA<Integer> convertToMyNFA() {
      CompactNFA<Integer> myNFA = new CompactNFA<>(Alphabets.integers(0, this.alphabetSize - 1));
      for (int i = 0; i < this.Q; i++) {
          myNFA.addState(this.O.getInt(i) != 0);
      }
      myNFA.setInitial(this.q0, true);
      for (int i = 0; i < this.Q; i++) {
          Int2ObjectRBTreeMap<IntList> iMap = nfaD.get(i);
          for (int in = 0; in < this.alphabetSize; in++) {
              IntList iList = iMap.get(in);
              if (iList != null) {
                  myNFA.addTransitions(i, in, iList);
              }
          }
      }
      return myNFA;
  }

    public List<Int2IntMap> getDfaD() {
        return dfaD;
    }

    public void setDfaD(List<Int2IntMap> dfaD) {
        this.dfaD = dfaD;
    }
}
