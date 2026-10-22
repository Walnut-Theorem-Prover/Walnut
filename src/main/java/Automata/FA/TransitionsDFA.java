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

import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public class TransitionsDFA implements Transitions {
  private List<Int2IntMap> dfaD;

  public TransitionsDFA() {
    this(new ArrayList<>());
  }

  public TransitionsDFA(List<Int2IntMap> dfaD) {
    if (dfaD == null) {
      throw new WalnutException("DFA transitions cannot be null.");
    }
    this.dfaD = dfaD;
  }

  @Override
  public TransitionsDFA clone() {
    List<Int2IntMap> copy = new ArrayList<>(dfaD.size());
    for (Int2IntMap row : dfaD) {
      Int2IntMap rowCopy = new Int2IntOpenHashMap(row);
      rowCopy.defaultReturnValue(row.defaultReturnValue());
      copy.add(rowCopy);
    }
    return new TransitionsDFA(copy);
  }

  /**
   * Return this DFA as an NFA transition table.  The result is a copy, because mutating
   * an NFA view cannot safely update DFA storage.
   */
  public List<Int2ObjectRBTreeMap<IntList>> getNfaD(){
    List<Int2ObjectRBTreeMap<IntList>> nfaD = new ArrayList<>(dfaD.size());
    for (Int2IntMap row : dfaD) {
      nfaD.add(convertRowToNfa(row));
    }
    return nfaD;
  }

  public Int2ObjectRBTreeMap<IntList> getNfaState(int q){
    return convertRowToNfa(dfaD.get(q));
  }
  public IntSortedSet getNfaStateKeySet(int q){
    return getDfaStateKeySet(q);
  }
  public IntList getNfaStateDests(int q, int in){
    Int2IntMap row = dfaD.get(q);
    if (!row.containsKey(in)) {
      return null;
    }
    IntList result = new IntArrayList(1);
    result.add(row.get(in));
    return result;
  }

  public Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state) {
    return getNfaState(state).int2ObjectEntrySet();
  }

  private static Int2ObjectRBTreeMap<IntList> convertRowToNfa(Int2IntMap row) {
    Int2ObjectRBTreeMap<IntList> nfaRow = new Int2ObjectRBTreeMap<>();
    for (Int2IntMap.Entry entry : row.int2IntEntrySet()) {
      IntList dest = new IntArrayList(1);
      dest.add(entry.getIntValue());
      nfaRow.put(entry.getIntKey(), dest);
    }
    return nfaRow;
  }

  public void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    throw new WalnutException("Cannot install NFA transitions on TransitionsDFA; use FA.setNfaTransitions instead.");
  }
  public void addToNfaD(Int2ObjectRBTreeMap<IntList> entry) {
    throw new WalnutException("Cannot add NFA transitions to TransitionsDFA; use FA.ensureNfaTransitions first.");
  }
  public Int2ObjectRBTreeMap<IntList> addMapToNfaD() {
    throw new WalnutException("Cannot add NFA transitions to TransitionsDFA; use FA.ensureNfaTransitions first.");
  }
  public void setNfaDTransition(int src, int inp, IntList destStates) {
    throw new WalnutException("Cannot mutate the NFA view of TransitionsDFA; use FA.ensureNfaTransitions first.");
  }

  public boolean hasDfaTransitions() {
    return true;
  }

  public int getDfaStateCount() {
    return dfaD.size();
  }

  public IntSortedSet getDfaStateKeySet(int q) {
    return new IntRBTreeSet(dfaD.get(q).keySet());
  }

  public int getDfaStateDest(int q, int in) {
    Int2IntMap row = dfaD.get(q);
    if (!row.containsKey(in)) {
      throw new WalnutException("No DFA transition from state " + q + " on input " + in + ".");
    }
    return row.get(in);
  }

  public boolean hasDfaDTransition(int q, int in) {
    return dfaD.get(q).containsKey(in);
  }

  public void setDfaDTransition(int q, int in, int dest) {
    dfaD.get(q).put(in, dest);
  }

  public void setDfaD(List<Int2IntMap> dfaD) {
    if (dfaD == null) {
      throw new WalnutException("DFA transitions cannot be null.");
    }
    this.dfaD = dfaD;
  }

  public void addDfaState() {
    this.dfaD.add(new Int2IntOpenHashMap());
  }

  @Override
  public Int2IntMap canonize(int initialState) {
    Int2IntMap permutationMap = determinePermutationMap(initialState);
    int newQ = permutationMap.size();

    List<Int2IntMap> newD = new ArrayList<>(newQ);
    for (int q = 0; q < newQ; q++) {
      newD.add(null);
    }
    for (int q = 0; q < dfaD.size(); q++) {
      if (permutationMap.containsKey(q)) {
        newD.set(permutationMap.get(q), dfaD.get(q));
      }
    }
    dfaD = newD;

    for (Int2IntMap row : dfaD) {
      for (Int2IntMap.Entry entry : row.int2IntEntrySet()) {
        row.put(entry.getIntKey(), permutationMap.get(entry.getIntValue()));
      }
    }
    return permutationMap;
  }

  private Int2IntMap determinePermutationMap(int initialState) {
    IntArrayFIFOQueue stateQueue = new IntArrayFIFOQueue();
    stateQueue.enqueue(initialState);
    Int2IntMap permutationMap = new Int2IntOpenHashMap();
    permutationMap.put(initialState, 0);
    int nextState = 1;
    while (!stateQueue.isEmpty()) {
      int q = stateQueue.dequeueInt();
      Int2IntMap row = dfaD.get(q);
      int[] inputs = row.keySet().toIntArray();
      Arrays.sort(inputs);
      for (int input : inputs) {
        int p = row.get(input);
        if (!permutationMap.containsKey(p)) {
          permutationMap.put(p, nextState++);
          stateQueue.enqueue(p);
        }
      }
    }
    return permutationMap;
  }

  @Override
  public void permuteInputs(IntUnaryOperator encodedInputPermutation) {
    List<Int2IntMap> permutedDfaD = new ArrayList<>(dfaD.size());
    for (Int2IntMap row : dfaD) {
      Int2IntMap permutedRow = new Int2IntOpenHashMap(row.size());
      for (Int2IntMap.Entry entry : row.int2IntEntrySet()) {
        permutedRow.put(encodedInputPermutation.applyAsInt(entry.getIntKey()), entry.getIntValue());
      }
      permutedDfaD.add(permutedRow);
    }
    dfaD = permutedDfaD;
  }

  /**
   * Reduce memory by trimming all maps.
   */
  public void reduceMemory() {
    for (Int2IntMap int2IntMap : this.dfaD) {
      if (int2IntMap instanceof Int2IntOpenHashMap openHashMap) {
        openHashMap.trim();
      }
    }
  }

  public long determineTransitionCount() {
    long numTransitionsLong = 0;
    for (Int2IntMap int2IntMap : dfaD) {
      numTransitionsLong += int2IntMap.keySet().size();
    }
    return numTransitionsLong;
  }

  public boolean isDeterministic() {
    return true;
  }

  private List<Int2IntRBTreeMap> getSortedDfaD() {
    List<Int2IntRBTreeMap> result = new ArrayList<>(dfaD.size());
    for (Int2IntMap row : dfaD) {
      result.add(new Int2IntRBTreeMap(row));
    }
    return result;
  }

  @Override
  public String toString() {
    return "dfaD:" + getSortedDfaD();
  }
}
