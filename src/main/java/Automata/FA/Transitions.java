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
import java.util.List;
import java.util.Set;

public class Transitions {
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
  /**
   * TODO: finish splitting DFA and NFA transitions.
   */
  private List<Int2ObjectRBTreeMap<IntList>> nfaD; // transitions when this is an NFA -- null if this is a known DFA

  private List<Int2IntMap> dfaD; // memory-efficient transitions when this is a known DFA -- usually null

  Transitions() {
    nfaD = new ArrayList<>();
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
  public void clearNfaD() {
    this.nfaD.clear();
  }

  public List<Int2IntMap> getDfaD() {
    return dfaD;
  }

  public void setDfaD(List<Int2IntMap> dfaD) {
    this.dfaD = dfaD;
  }
  public Int2IntMap addMapToDfaD() {
    Int2IntMap iMap = new Int2IntOpenHashMap();
    this.dfaD.add(iMap);
    return iMap;
  }

  /**
   * Reduce memory by trimming all maps.
   */
  public void reduceMemory() {
    if (this.dfaD != null) {
      for (Int2IntMap int2IntMap : this.dfaD) {
        ((Int2IntOpenHashMap) int2IntMap).trim();
      }
    }
    if (this.nfaD != null) {
      for (Int2ObjectRBTreeMap<IntList> iMap : nfaD) {
        for(IntList iList: iMap.values()) {
          ((IntArrayList)iList).trim();
        }
      }
    }
  }

  public long determineTransitionCount() {
    long numTransitionsLong = 0;
    if (nfaD == null) {
      for (Int2IntMap int2IntMap : dfaD) {
        numTransitionsLong += int2IntMap.keySet().size();
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

  public boolean isDeterministic() {
    if (nfaD == null && dfaD == null) {
      throw new WalnutException("Unexpected null transitions.");
    }
    if (dfaD != null) {
      return true; // trivially, if we're using DFA transitions, we're in a DFA
    }
    for (int q = 0; q < nfaD.size(); q++) {
      for (Int2ObjectMap.Entry<IntList> entry : this.getEntriesNfaD(q)) {
        if (entry.getValue().size() > 1) {
          return false;
        }
      }
    }
    return true;
  }

}
