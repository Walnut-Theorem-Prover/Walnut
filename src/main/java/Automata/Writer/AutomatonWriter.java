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
package Automata.Writer;

import Automata.Automaton;
import Automata.FA.FA;
import Automata.NumberSystem;
import Main.Logging;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.*;
import java.util.*;

import net.automatalib.automaton.fsa.impl.CompactNFA;
import net.automatalib.serialization.ba.BAWriter;

public class AutomatonWriter {
    /**
     * Writes automaton to a file given by the address.
     * This automaton can be NFA, NFAO, DFA, or DFAO. However, it cannot have epsilon transition.
     */
    public static void writeToTxtFormat(Automaton automaton, String address) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter((address))))) {
            writeTxtFormatToStream(automaton, out);
        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
        }
    }

    public static void writeTxtFormatToStream(Automaton automaton, PrintWriter out) {
        if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
            out.write(automaton.fa.trueFalseString());
        } else {
            automaton.canonize();
            writeAlphabet(automaton, out);
            for (int q = 0; q < automaton.fa.getQ(); q++) {
                writeState(automaton, out, q);
            }
        }
    }

    private static void writeAlphabet(Automaton automaton, PrintWriter out) {
        for (int i = 0; i < automaton.richAlphabet.getA().size(); i++) {
            NumberSystem numberSystem = automaton.getNS().get(i);
            if (numberSystem == null) {
                List<Integer> l = automaton.richAlphabet.getA().get(i);
                out.write("{");
                out.write(UtilityMethods.genericListString(l, ", "));
                out.write("} ");
            } else {
                if (i > 0) {
                    out.write(" ");
                }
                out.write(numberSystem.toString());
            }
        }
        out.write(System.lineSeparator());
    }

    private static void writeState(Automaton automaton, PrintWriter out, int q) {
        out.write(
                System.lineSeparator() + q + " " +
                        automaton.fa.getO().getInt(q) + System.lineSeparator());
        for (Int2ObjectMap.Entry<IntList> entry : automaton.getFa().getT().getEntriesNfaD(q)) {
            List<Integer> l = automaton.richAlphabet.decode(entry.getIntKey());
            for (Integer integer : l) {
                out.write(integer + " ");
            }
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
     */
    public static void writeToGV(Automaton automaton, String address, String predicate, boolean isDFAO) {
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

                TreeMap<Integer, TreeMap<Integer, List<String>>> transitions = new TreeMap<>();
                for (int q = 0; q < Q; q++) {
                    TreeMap<Integer, List<String>> treeMap = new TreeMap<>();
                    transitions.put(q, treeMap);
                    for (Int2ObjectMap.Entry<IntList> entry : automaton.fa.getT().getEntriesNfaD(q)) {
                        for (int dest : entry.getValue()) {
                            treeMap.putIfAbsent(dest, new ArrayList<>());
                            treeMap.get(dest).add(
                                UtilityMethods.toTransitionLabel(automaton.richAlphabet.decode(entry.getIntKey())));
                        }
                    }
                }

                for (int q = 0; q < Q; q++) {
                    for (Map.Entry<Integer, List<String>> entry : transitions.get(q).entrySet()) {
                        String transitionLabel = String.join(", ", entry.getValue());
                        out.println( q + " -> " + entry.getKey() + "[ label = \"" + transitionLabel + "\"];");
                    }
                }

                out.println("}");
            }
        } catch (IOException e) {
            Logging.printTruncatedStackTrace(e);
        }
    }

    public static void exportToBA(FA a, String address, boolean isDFAO) {
        if (isDFAO) {
            throw new WalnutException("Can't export DFAO to BA format");
        }
        System.out.println("Exporting to:" + address);
        CompactNFA<Integer> compactNFA = a.FAtoCompactNFA();
        BAWriter<Integer> baWriter = new BAWriter<>();
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(address))) {
            baWriter.writeModel(os, compactNFA, compactNFA.getInputAlphabet());
        } catch (IOException e) {
          Logging.printTruncatedStackTrace(e);
        }
    }

}
