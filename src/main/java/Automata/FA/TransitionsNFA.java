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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public class TransitionsNFA implements Transitions {
  private List<Int2ObjectRBTreeMap<IntList>> nfaD;

  public TransitionsNFA() {
    this(new ArrayList<>());
  }

  public TransitionsNFA(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    if (nfaD == null) {
      throw new WalnutException("NFA transitions cannot be null.");
    }
    this.nfaD = nfaD;
  }

  public List<Int2ObjectRBTreeMap<IntList>> getNfaD(){
    return nfaD;
  }

  public Int2ObjectRBTreeMap<IntList> getNfaState(int q){
    return nfaD.get(q);
  }
  public IntSortedSet getNfaStateKeySet(int q){
    return nfaD.get(q).keySet();
  }
  public IntList getNfaStateDests(int q, int in){
    return nfaD.get(q).get(in);
  }

  public Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state) {
    return nfaD.get(state).int2ObjectEntrySet();
  }

  public void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    if (nfaD == null) {
      throw new WalnutException("NFA transitions cannot be null.");
    }
    this.nfaD = nfaD;
  }
  public void addToNfaD(Int2ObjectRBTreeMap<IntList> entry) {
    nfaD.add(entry);
  }
  public Int2ObjectRBTreeMap<IntList> addMapToNfaD() {
    Int2ObjectRBTreeMap<IntList> entry = new Int2ObjectRBTreeMap<>();
    nfaD.add(entry);
    return entry;
  }
  public void setNfaDTransition(int src, int inp, IntList destStates) {
    nfaD.get(src).put(inp, destStates);
  }

  public boolean hasDfaTransitions() {
    return false;
  }

  public int getDfaStateCount() {
    throw new WalnutException("DFA transition storage is not available.");
  }

  public IntSortedSet getDfaStateKeySet(int q) {
    throw new WalnutException("DFA transition storage is not available.");
  }

  public int getDfaStateDest(int q, int in) {
    throw new WalnutException("DFA transition storage is not available.");
  }

  public boolean hasDfaDTransition(int q, int in) {
    throw new WalnutException("DFA transition storage is not available.");
  }

  public void setDfaDTransition(int q, int in, int dest) {
    throw new WalnutException("DFA transition storage is not available.");
  }

  public void setDfaD(List<Int2IntMap> dfaD) {
    throw new WalnutException("Cannot install DFA transitions on TransitionsNFA; use FA.setDfaTransitions instead.");
  }

  public void addDfaState() {
    throw new WalnutException("Cannot add DFA transitions to TransitionsNFA; use FA.setDfaTransitions first.");
  }

  @Override
  public Int2IntMap canonize(int initialState) {
    Int2IntMap permutationMap = determinePermutationMap(initialState);
    int newQ = permutationMap.size();

    List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(newQ);
    for (int q = 0; q < newQ; q++) {
      newD.add(null);
    }
    for (int q = 0; q < nfaD.size(); q++) {
      if (permutationMap.containsKey(q)) {
        newD.set(permutationMap.get(q), nfaD.get(q));
      }
    }
    nfaD = newD;

    for (Int2ObjectRBTreeMap<IntList> row : nfaD) {
      Iterator<Int2ObjectMap.Entry<IntList>> entries = row.int2ObjectEntrySet().iterator();
      while (entries.hasNext()) {
        Int2ObjectMap.Entry<IntList> entry = entries.next();
        IntList oldDestination = entry.getValue();
        IntList newDestination = new IntArrayList(oldDestination.size());
        for (int p : oldDestination) {
          if (permutationMap.containsKey(p)) {
            newDestination.add(permutationMap.get(p));
          }
        }
        if (newDestination.isEmpty()) {
          entries.remove();
        } else {
          row.put(entry.getIntKey(), newDestination);
        }
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
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
        for (int p : entry.getValue()) {
          if (!permutationMap.containsKey(p)) {
            permutationMap.put(p, nextState++);
            stateQueue.enqueue(p);
          }
        }
      }
    }
    return permutationMap;
  }

  @Override
  public void permuteInputs(IntUnaryOperator encodedInputPermutation) {
    for (int q = 0; q < nfaD.size(); q++) {
      Int2ObjectRBTreeMap<IntList> permutedRow = new Int2ObjectRBTreeMap<>();
      for (Int2ObjectMap.Entry<IntList> entry : nfaD.get(q).int2ObjectEntrySet()) {
        permutedRow.put(encodedInputPermutation.applyAsInt(entry.getIntKey()), entry.getValue());
      }
      nfaD.set(q, permutedRow);
    }
  }

  /**
   * Reduce memory by trimming all maps.
   */
  public void reduceMemory() {
    for (Int2ObjectRBTreeMap<IntList> iMap : nfaD) {
      for(IntList iList: iMap.values()) {
        if (iList instanceof IntArrayList intArrayList) {
          intArrayList.trim();
        }
      }
    }
  }

  public long determineTransitionCount() {
    long numTransitionsLong = 0;
    for (int q = 0; q < nfaD.size(); q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        numTransitionsLong += entry.getValue().size();
      }
    }
    return numTransitionsLong;
  }

  public boolean isDeterministic() {
    for (int q = 0; q < nfaD.size(); q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        if (entry.getValue().size() > 1) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "nfaD:" + nfaD;
  }
}
