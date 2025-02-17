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
import MRC.Model.MyDFA;
import MRC.Model.MyNFA;
import Main.ExceptionHelper;
import Main.UtilityMethods;
import Token.RelationalOperator;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.*;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactNFA;

import java.util.*;
import java.util.function.Predicate;

/**
 * Abstraction of NFA/DFA/DFAO code from Automaton.
 * TODO: fully abstract transitions such that this is easily an NFA, DFA, or anywhere in between.
 */
public class FA implements Cloneable {
  private static int AutomataIndex = 0; // Indicates the index of the automata in a particular run

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

  /**
   * Used when we're done with a particular run.
   */
  public static void resetIndex() {
    AutomataIndex = 0;
    DeterminizationStrategies.getStrategyMap().clear();
  }
  public static int incrementIndex() {
    return AutomataIndex++;
  }

  public void alphabetStates(List<List<Integer>> newAlphabet, List<List<Integer>> oldAlphabet, Automaton M) {
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(M.getQ());
      for (int q = 0; q < M.getQ(); q++) {
          Int2ObjectRBTreeMap<IntList> newMap = new Int2ObjectRBTreeMap<>();
          for (Int2ObjectMap.Entry<IntList> entry: getEntriesNfaD(q)) {
            List<Integer> decoded = RichAlphabet.decode(oldAlphabet, entry.getIntKey());
            if (isInNewAlphabet(newAlphabet, decoded)) {
              newMap.put(M.richAlphabet.encode(decoded), entry.getValue());
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

  public void initBasicFA(IntList O) {
    this.O = new IntArrayList(O);
    Q = O.size();
    for(int i = 0; i < Q; i++) {
      nfaD.add(new Int2ObjectRBTreeMap<>());
    }
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
              list.add((int)row.get(di));
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
    N.Q = N.Q + 1;
    Set<Int2ObjectMap.Entry<IntList>> sourceEntrySet = automaton.getEntriesNfaD(automaton.getQ0());
    for (int q = 0; q < N.getQ(); q++) {
      if (N.O.getInt(q) == 0) continue;  // only handle final states
      // Merge transitions from automaton's initial state's transitions
      Int2ObjectRBTreeMap<IntList> destMap = N.nfaD.get(q);
      for (Int2ObjectMap.Entry<IntList> entry : sourceEntrySet) {
        destMap.computeIfAbsent(entry.getIntKey(), k -> new IntArrayList()).addAll(entry.getValue());
      }
    }
  }

  public static void concatStates(FA other, FA N, int originalQ) {
      // to access the other's states, just do q. To access the other's states in N, do originalQ + q.
      for (int q = 0; q < other.getQ(); q++) {
        N.O.add(other.O.getInt(q)); // add the output
        N.nfaD.add(new Int2ObjectRBTreeMap<>());
        for (Int2ObjectMap.Entry<IntList> entry : other.getEntriesNfaD(q)) {
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
        for (Int2ObjectMap.Entry<IntList> entry : N.getEntriesNfaD(originalQ)) {
          N.nfaD.get(q).computeIfAbsent(entry.getIntKey(), s -> new IntArrayList()).addAll(entry.getValue());
        }
      }
      N.setQ(originalQ + other.getQ());
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
          nfaD.get(q).put(entry.getIntKey(), newDestination);
        } else {
          nfaD.get(q).remove(entry.getIntKey());
        }
      }
    }
    this.canonized = true;
  }

  private void addOffsetToInputs(int offset) {
      List<Int2ObjectRBTreeMap<IntList>> new_d = new ArrayList<>(Q);
      for (int q = 0; q < Q; q++) new_d.add(new Int2ObjectRBTreeMap<>());
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
          new_d.get(q).put(entry.getIntKey() + offset, entry.getValue());
        }
      }
      nfaD = new_d;
  }

  public void convertBrics(List<Integer> alphabet, String regularExpression) {
    long timeBefore = System.currentTimeMillis();
    // For example if alphabet = {2,4,1} then intersectingRegExp = [241]*
    StringBuilder intersectingRegExp = new StringBuilder("[");
    for (int x : alphabet) {
      if (x < 0 || x > 9) {
        throw new RuntimeException("the input alphabet of an automaton generated from a regular expression must be a subset of {0,1,...,9}");
      }
      intersectingRegExp.append(x);
    }
    intersectingRegExp.append("]*");
    regularExpression = "(" + regularExpression + ")&" + intersectingRegExp;
    dk.brics.automaton.RegExp RE = new RegExp(regularExpression);
    dk.brics.automaton.Automaton M = RE.toAutomaton();
    M.minimize();

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
        for (char a = (char)Math.max(t.getMin(), '0'); a <= Math.min(t.getMax(), '9'); a++) {
          if (alphabet.contains(a - '0')) {
            IntList dest = new IntArrayList();
            dest.add(setOfStates.indexOf(t.getDest()));
            currentStatesTransitions.put(alphabet.indexOf(a - '0'), dest);
          }
        }
      }
    }
    long timeAfter = System.currentTimeMillis();
    String msg = "computed ~:" + Q + " states - " + (timeAfter - timeBefore) + "ms";
    System.out.println(msg);
  }

  public void setFromBricsAutomaton(int alphabetSize, String regularExpression) {
    if (alphabetSize > ((1 << Character.SIZE) - 1)) {
      throw new RuntimeException("size of input alphabet exceeds the limit of " + ((1 << Character.SIZE) - 1));
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
      throw ExceptionHelper.bricsNFA();
    List<State> setOfStates = new ArrayList<>(M.getStates());

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
    // We added 128 to the encoding of every input vector before to avoid reserved characters, now we subtract it again
    // to get back the standard encoding
    this.addOffsetToInputs(-128);
    long timeAfter = System.currentTimeMillis();
    String msg = "computed ~:" + getQ() + " states - " + (timeAfter - timeBefore) + "ms";
    System.out.println(msg);
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

  private void totalize() {
    //we first check if the automaton is totalized
    boolean totalized = true;
    int deadState = Q; // may or may not be created

    for (int q = 0; q < Q; q++) {
      for (int x = 0; x < alphabetSize; x++) {
        if (!nfaD.get(q).containsKey(x)) {
          // point missing transitions to new state
          IntList nullState = new IntArrayList();
          nullState.add(deadState);
          nfaD.get(q).put(x, nullState);
          totalized = false;
        }
      }
    }
    if (!totalized) {
      // Add new non-accepting state that points to itself
      O.add(0);
      Q++;
      nfaD.add(new Int2ObjectRBTreeMap<>());
      for (int x = 0; x < alphabetSize; x++) {
        IntList nullState = new IntArrayList();
        nullState.add(deadState);
        nfaD.get(deadState).put(x, nullState);
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
      UtilityMethods.logMessage(true, msg, log);
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

      // reverse NFA transitions
      for (int q = 0; q < Q; q++) {
        for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
          for (int dest : entry.getValue()) {
            addNewTransition(newNfaD, dest, entry.getIntKey(), q);
          }
        }
      }
      FA.reduceNfaDMemory(newNfaD);

      nfaD = newNfaD;
      dfaD = null; // this is explicitly an NFA now
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

  private static void addNewTransition(List<Int2ObjectRBTreeMap<IntList>> newNfaD, int dest, int symbol, int q) {
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

  public Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state) {
    return nfaD.get(state).int2ObjectEntrySet();
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
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
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
      for (Int2ObjectMap.Entry<IntList> entry : getEntriesNfaD(q)) {
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

  public void setFieldsFromFile(int newQ, int newQ0, Map<Integer, Integer> stateOutput,
                                Map<Integer, Int2ObjectRBTreeMap<IntList>> stateTransition) {
    Q = newQ;
    q0 = newQ0;
    for (int q = 0; q < newQ; q++) {
      O.add((int) stateOutput.get(q));
      nfaD.add(stateTransition.get(q));
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

  /**
   * Add new transition to nfaD. Note that this will overwrite previous transitions if it exists.
   */
  public void addNewTransition(int src, int dest, int inp) {
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
    determinizeAndMinimize(qqq, print, prefix, log);
  }

  /**
   * Determinize and minimize. Technically, the logging is backwards.
   */
  public void determinizeAndMinimize(IntSet qqq, boolean print, String prefix, StringBuilder log) {
    DeterminizationStrategies.determinize(this, qqq, print, prefix + " ", log);
    justMinimize(print, prefix + " ", log);
  }

  /**
   * We don't need to determinize here; just minimize.
   */
  public void justMinimize(boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();
    UtilityMethods.logMessage(
            print, prefix + "Minimizing: " + Q + " states.", log);

    this.convertNFAtoDFA();
    ValmariDFA v = new ValmariDFA(this.dfaD, Q);
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

  public CompactNFA<Integer> FAtoCompactNFA() {
      CompactNFA<Integer> nfa = new CompactNFA<>(Alphabets.integers(0, this.alphabetSize - 1), this.Q);
      for (int i = 0; i < this.Q; i++) {
          nfa.addState(this.O.getInt(i) != 0);
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

  /*
  Convert FA to MyNFA representation, allowing additional initialState
   */
  public MyNFA<Integer> FAtoMyNFA(IntSet initialState) {
    MyNFA<Integer> nfa = this.FAtoMyNFA();
    // Replace initial states
    for(int i: nfa.getInitialStates()) {
      nfa.setInitial(i, false);
    }
    for(int i: initialState) {
      nfa.setInitial(i, true);
    }
    return nfa;
  }

  /*
  Convert FA to MyNFA representation
  */
  public MyNFA<Integer> FAtoMyNFA() {
    MyNFA<Integer> nfa = new MyNFA<>(Alphabets.integers(0, this.alphabetSize - 1), this.Q);
    for (int i = 0; i < this.Q; i++) {
      nfa.addState(this.O.getInt(i) != 0);
    }
    nfa.setInitial(this.q0, true);
    if (nfaD != null) {
      for (int i = 0; i < this.Q; i++) {
        for(Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(i)) {
          nfa.addTransitions(i, entry.getIntKey(), entry.getValue());
        }
      }
    } else {
      for (int i = 0; i < this.Q; i++) {
        for (Int2IntMap.Entry entry : dfaD.get(i).int2IntEntrySet()) {
          nfa.addTransition(i, entry.getIntKey(), entry.getIntValue());
        }
      }
    }
    return nfa;
  }

  public static FA compactNFAToFA(CompactNFA<Integer> cNFA) {
    FA fa = new FA();
    fa.Q = cNFA.size();
    Set<Integer> initialStates = cNFA.getInitialStates();
    if (initialStates.size() > 1) {
      throw new RuntimeException("Unexpected initial states from CompactNFA:" + initialStates);
    }
    fa.setQ0(initialStates.iterator().next());
    for(int i=0;i<fa.Q;i++) {
      fa.O.add(cNFA.isAccepting(i) ? 1 : 0);
    }
    fa.alphabetSize = cNFA.getInputAlphabet().size();
    for(int i=0;i<fa.Q;i++) {
      Int2ObjectRBTreeMap<IntList> iMap = new Int2ObjectRBTreeMap<>();
      fa.nfaD.add(iMap);
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

  public void setFromMyDFA(MyDFA<Integer> myDFA) {
    Q = myDFA.size();
    q0 = myDFA.getInitialState();
    O.clear();
    for(int i=0;i<Q;i++) {
      O.add(myDFA.isAccepting(i) ? 1 : 0);
    }
    alphabetSize = myDFA.getInputAlphabet().size();
    nfaD = null;
    dfaD = new ArrayList<>();
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
          throw new RuntimeException("Unexpected NFA instead of DFA.");
        }
        iMap.put(entry.getIntKey(), entry.getValue().iterator().nextInt());
      }
    }
    nfaD = null;
  }

  // helper function for our DFS to facilitate recursion
  public String infiniteHelper(List<List<Integer>> A, IntSet visited, int started, int state, String result) {
      if (visited.contains(state)) {
          if (state == started) {
              return result;
          }
          return "";
      }
      visited.add(state);
      for (Int2ObjectMap.Entry<IntList> entry : getEntriesNfaD(state)) {
          for (int y: entry.getValue()) {
              // this adds brackets even when inputs have arity 1 - this is fine, since we just want a usable infinite regex
              String cycle = infiniteHelper(A, visited, started, y, result + RichAlphabet.decode(A, entry.getIntKey()));
              if (!cycle.isEmpty()) {
                  return cycle;
              }
          }
      }

      visited.remove(state);
      return "";
  }

  // Determines whether an automaton accepts infinitely many values. If it does, a regex of infinitely many accepted values (not all)
  // is given. This is true iff there exists a cycle in a minimized version of the automaton, which previously had leading or
  // trailing zeroes removed according to whether it was msd or lsd
  public String infinite(List<List<Integer>> A) {
      for (int i = 0; i < Q; i++) {
          IntSet visited = new IntOpenHashSet(); // states we have visited
          String cycle = infiniteHelper(A, visited, i, i, "");
          // once a cycle is detected, compute a prefix leading to state i and a suffix from state i to an accepting state
          if (!cycle.isEmpty()) {
              final int finalI = i;
              String prefix = findPath(this, getQ0(), y -> y == finalI, A);
              String suffix = findPath(this, finalI, y -> O.getInt(y) != 0, A);
              return prefix + "(" + cycle + ")*" + suffix;
          }
      }
      return ""; // an empty string signals that we have failed to find a cycle
  }
  // Core pathfinding logic
  private static String findPath(
      FA automaton,
      int startState,
      Predicate<Integer> isFoundCondition,
      List<List<Integer>> A) {
    // Early exit if the start state meets the condition
    if (isFoundCondition.test(startState)) {
      return "";
    }
    List<Integer> distance = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
    List<Integer> prev = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
    List<Integer> input = new ArrayList<>(Collections.nCopies(automaton.getQ(), -1));
    distance.set(startState, 0);

    Queue<Integer> queue = new LinkedList<>();
    queue.add(startState);

    boolean found = false;
    int endState = -1;

    // BFS to find the path
    while (!queue.isEmpty() && !found) {
      int current = queue.poll();

      for (Int2ObjectMap.Entry<IntList> entry : automaton.getEntriesNfaD(current)) {
        int x = entry.getIntKey();
        IntList transitions = entry.getValue();

        for (int y : transitions) {
          if (isFoundCondition.test(y)) {
            found = true;
            endState = y;
          }
          if (distance.get(y) == -1) { // Unvisited state
            distance.set(y, distance.get(current) + 1);
            prev.set(y, current);
            input.set(y, x);
            queue.add(y);
          }
        }
      }
    }

    // Reconstruct the path
    List<Integer> path = new ArrayList<>();
    int current = found ? endState : startState;
    while (current != startState) {
      path.add(input.get(current));
      current = prev.get(current);
    }
    Collections.reverse(path);

    // Convert the path to a string
    StringBuilder result = new StringBuilder();
    for (Integer node : path) {
      result.append(RichAlphabet.decode(A, node));
    }
    return result.toString();
  }
}
