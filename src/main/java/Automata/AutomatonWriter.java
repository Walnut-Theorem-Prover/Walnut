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
import MRC.Model.MyNFA;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import net.automatalib.serialization.ba.BAWriter;

public class AutomatonWriter {
    /**
     * Writes down matrices for this automaton to a .mpl file given by the address.
     *
     * @param automaton
     * @param address
     */
    public static void writeMatrices(Automaton automaton, String address, List<String> free_variables) {
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            throw new RuntimeException("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
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
            for (String variable : free_variables) {
                if (!automaton.getLabel().contains(variable)) {
                    throw new RuntimeException("incidence matrices for the variable " + variable + " cannot be calculated, because " + variable + " is not a free variable.");
                }
            }
            List<Integer> indices = free_variables.stream().map(variable -> automaton.getLabel().indexOf(variable)).collect(Collectors.toList());
            List<List<Integer>> indexValueLists = indices.stream().map(index -> automaton.getA().get(index)).collect(Collectors.toList());
            List<List<Integer>> valueLists = cartesianProduct(indexValueLists);
            for (List<Integer> valueList : valueLists) {
                writeMatrixForAVariableListValuePair(automaton, free_variables, valueList, indices, out);
            }
            writeFinalStatesVector(automaton.getFa(), out);
            out.println();
            out.print("for i from 1 to Size(v)[2] do v := v.M_");
            out.print(String.join("_", free_variables) + "_");
            out.print(String.join("_", Collections.nCopies(free_variables.size(), "0")));
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
        Set<Integer> encoded_values = new HashSet<>();
        for (int x = 0; x != automaton.getAlphabetSize(); ++x) {
            List<Integer> decoding = automaton.richAlphabet.decode(x);
            List<Integer> compareList = indices.stream().map(decoding::get).toList();
            if (compareList.equals(valueList)) {
                encoded_values.add(x);
            }
        }
        int Q = automaton.getFa().getQ();
        int[][] M = new int[Q][Q];
        for (int p = 0; p < Q; ++p) {
            Int2ObjectRBTreeMap<IntList> transitions_p = automaton.fa.getNfaD().get(p);
            for (int v : encoded_values) {
                if (transitions_p.containsKey(v)) {
                    List<Integer> dest = transitions_p.get(v);
                    for (int q : dest) {
                        M[p][q]++;
                    }
                }
            }

            out.print("[");
            for (int q = 0; q < Q; ++q) {
                out.print(M[p][q]);
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

    /**
     * Writes automaton to a file given by the address.
     * This automaton can be NFA, NFAO, DFA, or DFAO. However, it cannot have epsilon transition.
     */
    public static void write(Automaton automaton, String address) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter((address))))) {
            writeToStream(automaton, out);
        } catch (IOException e) {
            UtilityMethods.printTruncatedStackTrace(e);
        }
    }

    public static void writeToStream(Automaton automaton, PrintWriter out) {
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            out.write(automaton.fa.isTRUE_AUTOMATON() ? "true" : "false");
        } else {
            automaton.canonize();
            writeAlphabet(automaton, out);
            for (int q = 0; q < automaton.fa.getQ(); q++) {
                writeState(automaton, out, q);
            }
        }
    }

    private static void writeAlphabet(Automaton automaton, PrintWriter out) {
        for (int i = 0; i < automaton.getA().size(); i++) {
            List<Integer> l = automaton.getA().get(i);
            if (automaton.getNS().get(i) == null) {
                out.write("{");
                out.write(UtilityMethods.genericListString(l, ", "));
                out.write("} ");
            } else {
                if (i == 0)
                    out.write(automaton.getNS().get(i).toString());
                else
                    out.write(" " + automaton.getNS().get(i).toString());
            }
        }
        out.write(System.lineSeparator());
    }

    private static void writeState(Automaton automaton, PrintWriter out, int q) {
        out.write(
                System.lineSeparator() + q + " " +
                        automaton.fa.getO().getInt(q) + System.lineSeparator());
        for (Int2ObjectMap.Entry<IntList> entry : automaton.getFa().getEntriesNfaD(q)) {
            List<Integer> l = automaton.richAlphabet.decode(entry.getIntKey());
            for (int j = 0; j < l.size(); j++)
                out.write(l.get(j) + " ");
            out.write("->");
            for (int dest : entry.getValue())
                out.write(" " + dest);
            out.write(System.lineSeparator());
        }
    }

    /**
     * Writes down this automaton to a .gv file given by the address. It uses the predicate that
     * caused this automaton as the label of this drawing.
     * Unlike prior versions of Walnut, this automaton can be a non deterministic automaton and also a DFAO.
     * In case of a DFAO the drawing contains state outputs with a slash (eg. "0/2" represents an output
     * of 2 from state 0)
     *
     * @param automaton
     * @param address
     */
    public static void draw(Automaton automaton, String address, String predicate, boolean isDFAO) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter((address))))) {
            if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
                out.println("digraph G {");
                out.println("label = \"(): " + predicate + "\";");
                out.println("rankdir = LR;");
                if (automaton.fa.isTRUE_AUTOMATON())
                    out.println("node [shape = doublecircle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
                else
                    out.println("node [shape = circle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
                out.println("node [shape = point ]; qi");
                out.println("qi ->" + 0 + ";");
                if (automaton.fa.isTRUE_AUTOMATON())
                    out.println(0 + " -> " + 0 + "[ label = \"*\"];");
                out.println("}");
            } else {
                automaton.canonize();
                out.println("digraph G {");
                out.println("label = \"" + UtilityMethods.toTuple(automaton.getLabel()) + ": " + predicate + "\";");
                out.println("rankdir = LR;");
                int Q = automaton.fa.getQ();
                for (int q = 0; q < Q; q++) {
                    if (isDFAO)
                        out.println("node [shape = circle, label=\"" + q + "/" + automaton.fa.getO().getInt(q) + "\", fontsize=12]" + q + ";");
                    else if (automaton.getFa().isAccepting(q))
                        out.println("node [shape = doublecircle, label=\"" + q + "\", fontsize=12]" + q + ";");
                    else
                        out.println("node [shape = circle, label=\"" + q + "\", fontsize=12]" + q + ";");
                }

                out.println("node [shape = point ]; qi");
                out.println("qi -> " + automaton.fa.getQ0() + ";");

                TreeMap<Integer, TreeMap<Integer, List<String>>> transitions =
                    new TreeMap<>();
                for (int q = 0; q < Q; q++) {
                    transitions.put(q, new TreeMap<>());
                    for (Int2ObjectMap.Entry<IntList> entry : automaton.fa.getEntriesNfaD(q)) {
                        for (int dest : entry.getValue()) {
                            transitions.get(q).putIfAbsent(dest, new ArrayList<>());
                            transitions.get(q).get(dest).add(
                                UtilityMethods.toTransitionLabel(automaton.richAlphabet.decode(entry.getIntKey())));
                        }
                    }
                }

                for (int q = 0; q < Q; q++) {
                    for (Map.Entry<Integer, List<String>> entry : transitions.get(q).entrySet()) {
                        String transition_label = String.join(", ", entry.getValue());
                        out.println( q + " -> " + entry.getKey() + "[ label = \"" + transition_label + "\"];");
                    }
                }

                out.println("}");
            }
        } catch (IOException e) {
            UtilityMethods.printTruncatedStackTrace(e);
        }
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

    public static void exportToBA(FA a, String address, boolean isDFAO) {
        if (isDFAO) {
            throw new RuntimeException("Can't export DFAO to BA format");
        }
        System.out.println("Exporting to:" + address);
        MyNFA<Integer> myNFA = a.FAtoMyNFA();
        BAWriter<Integer> baWriter = new BAWriter<>();
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(address))) {
            baWriter.writeModel(os, myNFA, myNFA.getInputAlphabet());
        } catch (IOException e) {
          UtilityMethods.printTruncatedStackTrace(e);
        }
    }
}
