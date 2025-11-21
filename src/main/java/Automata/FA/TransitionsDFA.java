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

public class TransitionsDFA implements Transitions {
  private List<Int2IntMap> dfaD; // memory-efficient transitions when this is a known DFA -- usually null

  TransitionsDFA() {
    dfaD = new ArrayList<>();
  }

  public List<Int2ObjectRBTreeMap<IntList>> getNfaD(){
    throw WalnutException.nonDeterministic();
  }

  public Int2ObjectRBTreeMap<IntList> getNfaState(int q){
    throw WalnutException.nonDeterministic();
  }
  public IntSortedSet getNfaStateKeySet(int q){
    throw WalnutException.nonDeterministic();
  }
  public IntList getNfaStateDests(int q, int in){
    throw WalnutException.nonDeterministic();
  }

  public Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state) {
    throw WalnutException.nonDeterministic();
  }

  public void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD) {
    throw WalnutException.nonDeterministic();
  }
  public void addToNfaD(Int2ObjectRBTreeMap<IntList> entry) {
    throw WalnutException.nonDeterministic();
  }
  public Int2ObjectRBTreeMap<IntList> addMapToNfaD() {
    throw WalnutException.nonDeterministic();
  }
  public void setNfaDTransition(int src, int inp, IntList destStates) {
    throw WalnutException.nonDeterministic();
  }
  public void clearNfaD() {
    throw WalnutException.nonDeterministic();
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
    for (Int2IntMap int2IntMap : this.dfaD) {
      ((Int2IntOpenHashMap) int2IntMap).trim();
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
    return true; // trivially, if we're using DFA transitions, we're in a DFA
  }

  @Override
  public String toString() {
    return "dfaD:" + getDfaD();
  }
}
