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

import it.unimi.dsi.fastutil.ints.*;

import java.util.List;
import java.util.Set;

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
public interface Transitions {
  List<Int2ObjectRBTreeMap<IntList>> getNfaD();

  Int2ObjectRBTreeMap<IntList> getNfaState(int q);
  IntSortedSet getNfaStateKeySet(int q);
  IntList getNfaStateDests(int q, int in);

  Set<Int2ObjectMap.Entry<IntList>> getEntriesNfaD(int state);

  void setNfaD(List<Int2ObjectRBTreeMap<IntList>> nfaD);
  void addToNfaD(Int2ObjectRBTreeMap<IntList> entry);
  Int2ObjectRBTreeMap<IntList> addMapToNfaD();
  void setNfaDTransition(int src, int inp, IntList destStates);
  void clearNfaD();

  void reduceMemory();

  long determineTransitionCount();
  boolean isDeterministic();
}
