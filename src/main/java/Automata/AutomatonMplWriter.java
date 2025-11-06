/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
 *
 * 	 This file is part of Walnut.
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
package Automata;

import Automata.FA.FA;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class AutomatonMplWriter {
  /**
   * Writes down matrices for this automaton to a .mpl file given by the address.
   */
  public static void writeMatrices(Automaton automaton, String address, List<String> freeVariables) {
      if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
          throw new WalnutException("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
      }
      automaton.canonize();

      try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter((address))))) {
          out.println("with(ArrayTools):");
          writeInitialStateVector(automaton.getFa(), out);
          out.println();
          out.println("# In what follows, the M_i_x, for a free variable i and a value x, denotes");
          out.println("# an incidence matrix of the underlying graph of (the automaton of)");
          out.println("# the predicate in the query.");
          out.println("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of");
          out.println("# transitions with i=x from p to q.");
          for (String variable : freeVariables) {
              if (!automaton.getLabel().contains(variable)) {
                  throw new WalnutException("incidence matrices for the variable " + variable + " cannot be calculated, because " + variable + " is not a free variable.");
              }
          }
          List<Integer> indices = freeVariables.stream().map(variable -> automaton.getLabel().indexOf(variable)).collect(Collectors.toList());
          List<List<Integer>> indexValueLists = indices.stream().map(index -> automaton.richAlphabet.getA().get(index)).collect(Collectors.toList());
          List<List<Integer>> valueLists = cartesianProduct(indexValueLists);
          for (List<Integer> valueList : valueLists) {
              writeMatrixForAVariableListValuePair(automaton, freeVariables, valueList, indices, out);
          }
          writeFinalStatesVector(automaton.getFa(), out);
          out.println();
          out.print("for i from 1 to Size(v)[2] do v := v.M_");
          out.print(String.join("_", freeVariables) + "_");
          out.print(String.join("_", Collections.nCopies(freeVariables.size(), "0")));
          out.println("; od; #fix up v by multiplying");
      } catch (IOException e) {
          UtilityMethods.printTruncatedStackTrace(e);
      }
  }

  private static void writeMatrixForAVariableListValuePair(
      Automaton automaton, List<String> variables, List<Integer> valueList, List<Integer> indices, PrintWriter out) {
      out.println();
      out.print("M_" + String.join("_", variables) + "_");
      out.print(valueList.stream().map(String::valueOf).collect(Collectors.joining("_")));
      out.print(" := Matrix([");
      Set<Integer> encodedValues = new HashSet<>();
      for (int x = 0; x != automaton.getAlphabetSize(); ++x) {
          List<Integer> decoding = automaton.richAlphabet.decode(x);
          List<Integer> compareList = indices.stream().map(decoding::get).toList();
          if (compareList.equals(valueList)) {
              encodedValues.add(x);
          }
      }
      int Q = automaton.getFa().getQ();
      int[] Mp = new int[Q];
      for (int p = 0; p < Q; ++p) {
          Arrays.fill(Mp, 0); // re-use array
          Set<Int2ObjectMap.Entry<IntList>> entrySet = automaton.fa.t.getEntriesNfaD(p);
          for (Int2ObjectMap.Entry<IntList> entry : entrySet) {
              if (encodedValues.contains(entry.getIntKey())) {
                  IntList targets = entry.getValue();
                  for (int q : targets) {
                      Mp[q]++;
                  }
              }
          }

          out.print("[");
          for (int q = 0; q < Q; ++q) {
              out.print(Mp[q]);
              if (q < (Q - 1)) {
                  out.print(",");
              }
          }
          out.print("]");
          if (p < (Q - 1)) {
              out.println(",");
          }
      }
      out.println("]);");
  }

  private static void writeInitialStateVector(FA fa, PrintWriter out) {
      out.println("# The row vector v denotes the indicator vector of the (singleton)");
      out.println("# set of initial states.");
      out.print("v := Vector[row]([");
      for (int q = 0; q != fa.getQ(); ++q) {
          out.print(q == fa.getQ0() ? "1" : "0");
          if (q < (fa.getQ() - 1)) {
              out.print(",");
          }
      }
      out.println("]);");
  }

  private static void writeFinalStatesVector(FA fa, PrintWriter out) {
      out.println();
      out.println("# The column vector w denotes the indicator vector of the");
      out.println("# set of final states.");
      out.print("w := Vector[column]([");
      for (int q = 0; q != fa.getQ(); ++q) {
          out.print(fa.isAccepting(q) ? "1" : "0");
          if (q < (fa.getQ() - 1)) {
              out.print(",");
          }
      }
      out.println("]);");
  }

  private static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
    List<List<T>> resultLists = new ArrayList<>();
    if (lists.isEmpty()) {
      resultLists.add(Collections.emptyList());
      return resultLists;
    } else {
      List<T> firstList = lists.get(0);
      List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
      for (T condition : firstList) {
        for (List<T> remainingList : remainingLists) {
          List<T> resultList = new ArrayList<>(remainingList.size() + 1);
          resultList.add(condition);
          resultList.addAll(remainingList);
          resultLists.add(resultList);
        }
      }
    }
    return resultLists;
  }
}
