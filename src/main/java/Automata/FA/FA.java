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
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

/**
 * Abstraction of NFA/DFA code from Automaton.
 */
public class FA implements Cloneable {
  private int q0;
  private int Q;
  private int alphabetSize;
  private IntList O;
  private List<Int2ObjectRBTreeMap<IntList>> d;

  private static final int MAX_BRICS_CHARACTER = (1 << Character.SIZE) - 1;

  public FA() {
    O = new IntArrayList();
    d = new ArrayList<>();
  }

  public boolean isTotalized() {
      boolean totalized = true;
      for(int q = 0; q < Q; q++){
          for(int x = 0; x < alphabetSize; x++){
              if(!d.get(q).containsKey(x)) {
                  totalized = false;
              }
              else if (d.get(q).get(x).size() > 1) {
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
      d = buildTransitionsFromMorphism(prevMorphism);
  }

  public void clear() {
    O.clear();
    d.clear();
  }

  /**
   * Merges all transitions from `sourceMap` into `destMap`.
   * Each key's list gets appended to or created if missing.
   */
  private static void mergeTransitions(Int2ObjectRBTreeMap<IntList> destMap,
                                       Int2ObjectRBTreeMap<IntList> sourceMap) {
    for (Int2ObjectMap.Entry<IntList> entry : sourceMap.int2ObjectEntrySet()) {
      int symbol = entry.getIntKey();
      IntList newTransitions = entry.getValue();
      destMap.computeIfAbsent(symbol, k -> new IntArrayList()).addAll(newTransitions);
    }
  }


  public static void starStates(FA automaton, FA N) {
    // We clone the current automaton and add a new state which will be our new initial state.
    // We will then canonize the resulting automaton after.
    int newState = N.Q;
    N.O.add(1); // The newly added state is a final state.
    N.d.add(new Int2ObjectRBTreeMap<>());

    for (int q = 0; q < N.getQ(); q++) {
      if (N.O.getInt(q) == 0) continue;  // only handle final states
      // Merge transitions from automaton's initial state's transitions
      mergeTransitions(N.d.get(q), automaton.d.get(automaton.getQ0()));
    }

    // For the new initial state, copy transitions from automaton's original q0
    mergeTransitions(N.d.get(newState), automaton.d.get(automaton.getQ0()));

    N.setQ(N.getQ() + 1);
    N.setQ0(newState);
  }

  public static void concatStates(FA other, FA N, int originalQ) {
      // to access the other's states, just do q. To access the other's states in N, do originalQ + q.
      for (int q = 0; q < other.getQ(); q++) {
        N.O.add(other.O.getInt(q)); // add the output
        N.d.add(new Int2ObjectRBTreeMap<>());
        for (Int2ObjectMap.Entry<IntList> entry : other.d.get(q).int2ObjectEntrySet()) {
          int symbol = entry.getIntKey();
          IntList oldDestinations = entry.getValue();
          IntArrayList newTransitionMap = new IntArrayList(oldDestinations.size());
          for (int i = 0; i < oldDestinations.size(); i++) {
            newTransitionMap.add(oldDestinations.getInt(i) + originalQ);
          }
          N.d.get(originalQ + q).put(symbol, newTransitionMap);
        }
      }

      // now iterate through all of self's states. If they are final, add a transition to wherever the other's
      // initial state goes.
      for (int q = 0; q < originalQ; q++) {
        if (N.O.getInt(q) == 0) { // if it is NOT a final state
          continue;
        }

        // otherwise, it is a final state, and we add our transitions.
        for (Int2ObjectMap.Entry<IntList> entry : N.d.get(originalQ).int2ObjectEntrySet()) {
          N.d.get(q).computeIfAbsent(entry.getIntKey(), s -> new IntArrayList()).addAll(entry.getValue());
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
      M.getFa().d = newD;
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
      Queue<Integer> state_queue = new LinkedList<>();
      state_queue.add(q0);

      /**map holds the permutation we need to apply to Q. In other words if map = {(0,3),(1,10),...} then
       *we have got to send Q[0] to Q[3] and Q[1] to Q[10]*/
      Int2IntMap map = new Int2IntOpenHashMap();
      map.put(q0, 0);
      int i = 1;
      while (!state_queue.isEmpty()) {
        int q = state_queue.poll();
        for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
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
              newD.set(map.get(q), d.get(q));
          }
      }

      Q = newQ;
      O = newO;
      d = newD;

      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
          IntList newDestination = new IntArrayList();
          for (int p : entry.getValue()) {
            if (map.containsKey(p)) {
              newDestination.add(map.get(p));
            }
          }

          if (!newDestination.isEmpty()) {
            d.get(q).put(entry.getIntKey(), newDestination);
          } else {
            d.get(q).remove(entry.getIntKey());
          }
        }
      }
  }

  public void addOffsetToInputs(int offset) {
      List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>(Q);
      for (int q = 0; q < Q; q++) new_d.add(new Int2ObjectRBTreeMap<>());
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
          new_d.get(q).put(entry.getIntKey() + offset, entry.getValue());
        }
      }
      d = new_d;
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
      d.add(currentStatesTransitions);
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
    d = new ArrayList<>();
    for (int q = 0; q < Q; q++) {
      State state = setOfStates.get(q);
      O.add(state.isAccept() ? 1 : 0);
      Int2ObjectRBTreeMap<IntList> currentStatesTransitions = new Int2ObjectRBTreeMap<>();
      d.add(currentStatesTransitions);
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
              if (!d.get(q).containsKey(x)) {
                // point every missing transition to new state
                IntList nullState = new IntArrayList();
                nullState.add(Q);
                d.get(q).put(x, nullState);
                totalized = false;
              }
          }
      }
      if (!totalized) {
        // Add new state, point new state to itself
        int newState = Q;
        O.add(0);
        Q++;
        d.add(new Int2ObjectRBTreeMap<>());
        for (int x = 0; x < alphabetSize; x++) {
          IntList nullState = new IntArrayList();
          nullState.add(newState);
          d.get(newState).put(x, nullState);
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

  /* Minimization algorithm */
  public void minimizeValmari(List<Int2IntMap> newMemD, boolean print, String prefix, StringBuilder log) {
      IntSet qqq = new IntOpenHashSet();
      qqq.add(q0);
      newMemD = subsetConstruction(newMemD, qqq, print, prefix, log);

      ValmariDFA v = new ValmariDFA(newMemD, Q);
      v.minValmari(O);
      Q = v.blocks.z;
      q0 = v.blocks.S[q0];
      O = v.determineO();
      d = v.determineD();
  }

  int obtainMinimumOutput() {
    int min = determineMinOutput();
    O.add(min - 1);
    Q++;
    d.add(new Int2ObjectRBTreeMap<>());
    for (int x = 0; x < alphabetSize; x++) {
      IntList nullState = new IntArrayList();
      nullState.add(Q - 1);
      d.get(Q - 1).put(x, nullState);
    }
    return min;
  }

  public IntSet reverseDFAtoNFAInternal() {
      // We change the direction of transitions first.
      List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>();
      for (int q = 0; q < Q; q++) new_d.add(new Int2ObjectRBTreeMap<>());
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
          int symbol = entry.getIntKey();
          IntList dests = entry.getValue();
          for (int dest : dests) {
            if (new_d.get(dest).containsKey(symbol))
              new_d.get(dest).get(symbol).add(q);
            else {
              IntList destinationSet = new IntArrayList();
              destinationSet.add(q);
              new_d.get(dest).put(symbol, destinationSet);
            }
          }
        }
      }
      d = new_d;
      IntSet setOfFinalStates = new IntOpenHashSet();
      // final states become non-final
      for (int q = 0; q < Q; q++) {
          if (O.getInt(q) != 0) {
              setOfFinalStates.add(q);
              O.set(q, 0);
          }
      }
      O.set(q0, 1); // initial state becomes the final state.
      return setOfFinalStates;
  }

  boolean totalizeIfNecessary() {
      //we first check if the automaton is totalized
      boolean totalized = true;
      for (int q = 0; q < Q; q++) {
          for (int x = 0; x < alphabetSize; x++) {
              if (!d.get(q).containsKey(x)) {
                  IntList nullState = new IntArrayList();
                  nullState.add(Q);
                  d.get(q).put(x, nullState);
                  totalized = false;
              }
          }
      }
      return totalized;
  }

  /**
   * Build up a new subset of states in the subset construction algorithm.
   *
   * @param newMemD - memory-efficient transition function
   * @param state   -
   * @param in      - index into alphabet
   * @return Subset of states used in Subset Construction
   */
  private IntOpenHashSet determineMetastate(List<Int2IntMap> newMemD, IntSet state, int in) {
      IntOpenHashSet dest = new IntOpenHashSet();
      for (int q : state) {
          if (newMemD == null) {
              IntList values = this.d.get(q).get(in);
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

  public List<Int2ObjectRBTreeMap<IntList>> getD(){
    return d;
  }

  public void setD(List<Int2ObjectRBTreeMap<IntList>> d) {
    this.d = d;
  }

  public FA clone() {
    FA fa = new FA();
    fa.Q = this.Q;
    fa.q0 = this.q0;
    fa.alphabetSize = this.alphabetSize;
    fa.O = new IntArrayList(this.O);
    fa.d = new ArrayList<>();
    for (int q = 0; q < fa.Q; q++) {
      fa.d.add(new Int2ObjectRBTreeMap<>());
      for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
        fa.d.get(q).put(entry.getIntKey(), new IntArrayList(entry.getValue()));
      }
    }
    return fa;
  }

  public void setOutput(int output) {
    for (int j = 0; j < O.size(); j++) {
      O.set(j, O.getInt(j) == output ? 1 : 0);
    }
  }

  /**
   * Subset Construction (Determinizing).
   *
   * @param initial_state
   * @param print
   * @param prefix
   * @param log
   * @return A memory-efficient representation of a determinized transition function
   */
  public List<Int2IntMap> subsetConstruction(
      List<Int2IntMap> newMemD, IntSet initial_state, boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    UtilityMethods.logMessage(print, prefix + "Determinizing: " + getQ() + " states", log);

    int number_of_states = 0, current_state = 0;
    Object2IntMap<IntSet> statesHash = new Object2IntOpenHashMap<>();
    List<IntSet> statesList = new ArrayList<>();
    statesList.add(initial_state);
    statesHash.put(initial_state, 0);
    number_of_states++;

    List<Int2IntMap> new_d = new ArrayList<>();

    while (current_state < number_of_states) {

      if (print) {
        int statesSoFar = current_state + 1;
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0, prefix + "  Progress: Added " + statesSoFar + " states - "
            + (number_of_states - statesSoFar) + " states left in queue - "
            + number_of_states + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
      }

      IntSet state = statesList.get(current_state);
      new_d.add(new Int2IntOpenHashMap());
      Int2IntMap currentStateMap = new_d.get(current_state);
      for (int in = 0; in != alphabetSize; ++in) {
        IntOpenHashSet stateSubset = this.determineMetastate(newMemD, state, in);
        if (!stateSubset.isEmpty()) {
          int new_dValue;
          int key = statesHash.getOrDefault(stateSubset, -1);
          if (key != -1) {
            new_dValue = key;
          } else {
            stateSubset.trim(); // reduce memory footprint of set before storing
            statesList.add(stateSubset);
            statesHash.put(stateSubset, number_of_states);
            new_dValue = number_of_states;
            number_of_states++;
          }
          currentStateMap.put(in, new_dValue);
        }
      }
      current_state++;
    }
    setD(null);
    // NOTE: d is now null! This is to save peak memory
    // It's recomputed in minimize_valmari via the memory-efficient newMemD
    Q = number_of_states;
    q0 = 0;
    O = calculateNewStateOutput(O, statesList);

    long timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(print, prefix + "Determinized: " + getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    return new_d;
  }

  /**
   * Calculate new state output (O), from previous O and statesList.
   *
   * @param O          - previous O
   * @param statesList
   * @return new O
   */
  private static IntList calculateNewStateOutput(IntList O, List<IntSet> statesList) {
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

  public void permuteD(int[] encoded_input_permutation) {
    for (int q = 0; q < Q; q++) {
      Int2ObjectRBTreeMap<IntList> permuted_d = new Int2ObjectRBTreeMap<>();
      for (Int2ObjectMap.Entry<IntList> entry : getD().get(q).int2ObjectEntrySet()) {
        permuted_d.put(encoded_input_permutation[entry.getIntKey()], entry.getValue());
      }
      d.set(q, permuted_d);
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
      for (Int2ObjectMap.Entry<IntList> entry : d.get(q).int2ObjectEntrySet()) {
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
    IntList dQ0 = d.get(q0).get(zero);
    if (dQ0 == null) {
      dQ0 = new IntArrayList();
      d.get(q0).put(zero, dQ0);
    }
    if (!dQ0.contains(q0)) {
      dQ0.add(q0);
    }
    IntSet result = new IntOpenHashSet();
    Queue<Integer> queue = new LinkedList<>();
    queue.add(q0);
    while (!queue.isEmpty()) {
      int q = queue.poll();
      result.add(q);
      dQ0 = d.get(q).get(zero);
      if (dQ0 != null) {
        for (int p: dQ0) {
          if (!result.contains(p))
            queue.add(p);
        }
      }
    }
    return result;
  }

  /**
   * So for example if f is a final state and f is reachable from q by reading 0*
   * then q will be in the resulting set of this method.
   */
  public void setStatesReachableToFinalStatesByZeros(int zero) {
    Set<Integer> result = new HashSet<>();
    Queue<Integer> queue = new LinkedList<>();
    //this is the adjacency matrix of the reverse of the transition graph of this automaton on 0
    List<List<Integer>> adjacencyList = new ArrayList<>(Q);
    for (int q = 0; q < Q; q++) adjacencyList.add(new ArrayList<>());
    for (int q = 0; q < Q; q++) {
      if (d.get(q).containsKey(zero)) {
        List<Integer> destination = d.get(q).get(zero);
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
    for (int q : result) {
      O.set(q, 1);
    }
  }

  public void setFieldsFromFile(int newQ, int newQ0, Map<Integer, Integer> state_output,
                                Map<Integer, Int2ObjectRBTreeMap<IntList>> state_transition) {
    Q = newQ;
    q0 = newQ0;
    for (int q = 0; q < newQ; q++) {
      O.add((int) state_output.get(q));
      d.add(state_transition.get(q));
    }
  }

  /**
   * Check if automaton is deterministic (amd total): each state must have exactly alphabetSize transitions
   */
  public boolean isDeterministic() {
    for (int q = 0; q < Q; q++) {
      if (d.get(q).keySet().size() != alphabetSize) {
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
        row.add(d.get(q).get(di).getInt(0));
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
        for (int di : d.get(j).keySet()) {
          int nextState = d
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
      d = newD;
  }

  public void addTransition(int src, int dest, int inp) {
      IntList destStates = new IntArrayList();
      destStates.add(dest);
      d.get(src).put(inp, destStates);
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
}
