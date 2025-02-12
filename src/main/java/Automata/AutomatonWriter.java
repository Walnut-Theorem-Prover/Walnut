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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
    public static String writeMatrices(Automaton automaton, String address, List<String> free_variables) {
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            throw new RuntimeException("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
        }
        automaton.canonize();
        StringBuilder s = new StringBuilder();
        s.append("with(ArrayTools):" + System.lineSeparator());
        writeInitialStateVector(automaton.getFa(), s);
        s.append(System.lineSeparator() + "# In what follows, the M_i_x, for a free variable i and a value x, denotes" + System.lineSeparator());
        s.append("# an incidence matrix of the underlying graph of (the automaton of)" + System.lineSeparator());
        s.append("# the predicate in the query." + System.lineSeparator());
        s.append("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of" + System.lineSeparator());
        s.append("# transitions with i=x from p to q." + System.lineSeparator());
        for (String variable : free_variables) {
            if (!automaton.getLabel().contains(variable)) {
                throw new RuntimeException("incidence matrices for the variable " + variable + " cannot be calculated, because " + variable + " is not a free variable.");
            }
        }
        List<Integer> indices = free_variables.stream().map(variable -> automaton.getLabel().indexOf(variable)).collect(Collectors.toList());
        List<List<Integer>> indexValueLists = indices.stream().map(index -> automaton.getA().get(index)).collect(Collectors.toList());
        List<List<Integer>> valueLists = cartesianProduct(indexValueLists);
        for (List<Integer> valueList : valueLists) {
            writeMatrixForAVariableListValuePair(automaton, free_variables, valueList, indices, s);
        }
        writeFinalStatesVector(automaton.getFa(), s);
        s.append(System.lineSeparator() + "for i from 1 to Size(v)[2] do v := v.M_");
        s.append(String.join("_", free_variables) + "_");
        s.append(String.join("_", Collections.nCopies(free_variables.size(), "0")));
        s.append("; od; #fix up v by multiplying");

        String res = s.toString();

        try (PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8)) {
            out.write(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
      return res;
    }

    private static void writeMatrixForAVariableListValuePair(Automaton automaton, List<String> variables, List<Integer> valueList, List<Integer> indices, StringBuilder s) {
        s.append(System.lineSeparator() + "M_" + String.join("_", variables) + "_");
        s.append(valueList.stream().map(String::valueOf).collect(Collectors.joining("_")));
        s.append(" := Matrix([");
        Set<Integer> encoded_values = new HashSet<>();
        for (int x = 0; x != automaton.getAlphabetSize(); ++x) {
            List<Integer> decoding = automaton.richAlphabet.decode(x);
            List<Integer> compareList = indices.stream().map(decoding::get).toList();
            if (compareList.equals(valueList)) {
                encoded_values.add(x);
            }
        }
        int[][] M = new int[automaton.getQ()][automaton.getQ()];
        for (int p = 0; p < automaton.getQ(); ++p) {
            Int2ObjectRBTreeMap<IntList> transitions_p = automaton.getD().get(p);
            for (int v : encoded_values) {
                if (transitions_p.containsKey(v)) {
                    List<Integer> dest = transitions_p.get(v);
                    for (int q : dest) {
                        M[p][q]++;
                    }
                }
            }

            s.append("[");
            for (int q = 0; q < automaton.getQ(); ++q) {
                s.append(M[p][q]);
                if (q < (automaton.getQ() - 1)) {
                    s.append(",");
                }
            }
            s.append("]");
            if (p < (automaton.getQ() - 1)) {
                s.append("," + System.lineSeparator());
            }
        }
        s.append("]);" + System.lineSeparator());
    }

    private static void writeInitialStateVector(FA automaton, StringBuilder s) {
        s.append("# The row vector v denotes the indicator vector of the (singleton)" + System.lineSeparator());
        s.append("# set of initial states." + System.lineSeparator());
        s.append("v := Vector[row]([");
        for (int q = 0; q != automaton.getQ(); ++q) {
            if (q == automaton.getQ0()) {
                s.append("1");
            } else {
                s.append("0");
            }
            if (q < (automaton.getQ() - 1)) {
                s.append(",");
            }
        }
        s.append("]);" + System.lineSeparator());
    }

    private static void writeFinalStatesVector(FA automaton, StringBuilder s) {
        s.append(System.lineSeparator() + "# The column vector w denotes the indicator vector of the" + System.lineSeparator());
        s.append("# set of final states." + System.lineSeparator());
        s.append("w := Vector[column]([");
        for (int q = 0; q != automaton.getQ(); ++q) {
            if (automaton.getO().getInt(q) != 0) {
                s.append("1");
            } else {
                s.append("0");
            }
            if (q < (automaton.getQ() - 1)) {
                s.append(",");
            }
        }
        s.append("]);" + System.lineSeparator());
    }

    /**
     * Writes this automaton to a file given by the address.
     * This automaton can be non deterministic. It can also be a DFAO. However it cannot have epsilon transition.
     *
     * @param automaton
     * @param address
     * @throws
     */
    public static void write(Automaton automaton, String address) {
        try (PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8)) {
            writeToStream(automaton, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToStream(Automaton automaton, PrintWriter out) {
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            out.write(automaton.fa.isTRUE_AUTOMATON() ? "true" : "false");
        } else {
            automaton.canonize();
            writeAlphabet(automaton, out);
            for (int q = 0; q < automaton.getQ(); q++) {
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
                        automaton.getO().getInt(q) + System.lineSeparator());
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
        StringBuilder gv = new StringBuilder();
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            addln(gv,"digraph G {");
            addln(gv,"label = \"(): " + predicate + "\";");
            addln(gv,"rankdir = LR;");
            if (automaton.fa.isTRUE_AUTOMATON())
                addln(gv,"node [shape = doublecircle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
            else
                addln(gv,"node [shape = circle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
            addln(gv,"node [shape = point ]; qi");
            addln(gv,"qi ->" + 0 + ";");
            if (automaton.fa.isTRUE_AUTOMATON())
                addln(gv,0 + " -> " + 0 + "[ label = \"*\"];");
            addln(gv,"}");
        } else {
            automaton.canonize();
            addln(gv,"digraph G {");
            addln(gv,"label = \"" + UtilityMethods.toTuple(automaton.getLabel()) + ": " + predicate + "\";");
            addln(gv,"rankdir = LR;");
            for (int q = 0; q < automaton.getQ(); q++) {
                if (isDFAO)
                    addln(gv,"node [shape = circle, label=\"" + q + "/" + automaton.getO().getInt(q) + "\", fontsize=12]" + q + ";");
                else if (automaton.getO().getInt(q) != 0)
                    addln(gv,"node [shape = doublecircle, label=\"" + q + "\", fontsize=12]" + q + ";");
                else
                    addln(gv,"node [shape = circle, label=\"" + q + "\", fontsize=12]" + q + ";");
            }

            addln(gv,"node [shape = point ]; qi");
            addln(gv,"qi -> " + automaton.getQ0() + ";");

            TreeMap<Integer, TreeMap<Integer, List<String>>> transitions =
                    new TreeMap<>();
            for (int q = 0; q < automaton.getQ(); q++) {
                transitions.put(q, new TreeMap<>());
                for (Int2ObjectMap.Entry<IntList> entry : automaton.getFa().getEntriesNfaD(q)) {
                    for (int dest : entry.getValue()) {
                        transitions.get(q).putIfAbsent(dest, new ArrayList<>());
                        transitions.get(q).get(dest).add(
                            UtilityMethods.toTransitionLabel(automaton.richAlphabet.decode(entry.getIntKey())));
                    }
                }
            }

            for (int q = 0; q < automaton.getQ(); q++) {
                for (Map.Entry<Integer, List<String>> entry : transitions.get(q).entrySet()) {
                    String transition_label = String.join(", ", entry.getValue());
                    addln(gv, q + " -> " + entry.getKey() + "[ label = \"" + transition_label + "\"];");
                }
            }

            addln(gv,"}");
        }
        try (PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8)) {
            out.write(gv.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void addln(StringBuilder gv, String line) {
        gv.append(line).append(System.lineSeparator());
    }

    private static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> resultLists = new ArrayList<>();
        if (lists.isEmpty()) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (T condition : firstList) {
                for (List<T> remainingList : remainingLists) {
                    ArrayList<T> resultList = new ArrayList<>();
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
          e.printStackTrace();
        }
    }
}
