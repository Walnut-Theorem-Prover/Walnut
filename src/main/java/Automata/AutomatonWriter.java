package Automata;

import Main.GraphViz;
import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class AutomatonWriter {
    /**
     * Writes down matrices for this automaton to a .mpl file given by the address.
     *
     * @param automaton
     * @param address
     */
    public static String write_matrices(Automaton automaton, String address, List<String> free_variables) throws Exception {
        if (automaton.TRUE_FALSE_AUTOMATON) {
            throw new Exception("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
        }
        automaton.canonize();
        StringBuilder s = new StringBuilder();
        s.append("with(ArrayTools):" + System.lineSeparator());
        write_initial_state_vector(automaton, s);
        s.append(System.lineSeparator() + "# In what follows, the M_i_x, for a free variable i and a value x, denotes" + System.lineSeparator());
        s.append("# an incidence matrix of the underlying graph of (the automaton of)" + System.lineSeparator());
        s.append("# the predicate in the query." + System.lineSeparator());
        s.append("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of" + System.lineSeparator());
        s.append("# transitions with i=x from p to q." + System.lineSeparator());
        for (String variable : free_variables) {
            if (!automaton.label.contains(variable)) {
                throw new Exception("incidence matrices for the variable " + variable + " cannot be calculated, because " + variable + " is not a free variable.");
            }
        }
        List<Integer> indices = free_variables.stream().map(variable -> automaton.label.indexOf(variable)).collect(Collectors.toList());
        List<List<Integer>> indexValueLists = indices.stream().map(index -> automaton.A.get(index)).collect(Collectors.toList());
        List<List<Integer>> valueLists = AutomatonLogicalOps.cartesianProduct(indexValueLists);
        for (List<Integer> valueList : valueLists) {
            write_matrix_for_a_variable_list_value_pair(automaton, free_variables, valueList, indices, s);
        }
        write_final_states_vector(automaton, s);
        s.append(System.lineSeparator() + "for i from 1 to Size(v)[2] do v := v.M_");
        s.append(String.join("_", free_variables) + "_");
        s.append(String.join("_", Collections.nCopies(free_variables.size(), "0")));
        s.append("; od; #fix up v by multiplying");

        String res = s.toString();

        try {
            PrintWriter out = new PrintWriter(address, "UTF-8");
            out.write(res);
            out.close();
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
        return res;
    }

    private static void write_matrix_for_a_variable_list_value_pair(Automaton automaton, List<String> variables, List<Integer> valueList, List<Integer> indices, StringBuilder s) {
        s.append(System.lineSeparator() + "M_" + String.join("_", variables) + "_");
        s.append(valueList.stream().map(String::valueOf).collect(Collectors.joining("_")));
        s.append(" := Matrix([");
        Set<Integer> encoded_values = new HashSet<>();
        for (int x = 0; x != automaton.alphabetSize; ++x) {
            List<Integer> decoding = Automaton.decode(automaton, x);
            List<Integer> compareList = indices.stream().map(index -> decoding.get(index)).collect(Collectors.toList());
            if (compareList.equals(valueList)) {
                encoded_values.add(x);
            }
        }
        int[][] M = new int[automaton.Q][automaton.Q];
        for (int p = 0; p < automaton.Q; ++p) {
            Int2ObjectRBTreeMap<IntList> transitions_p = automaton.d.get(p);
            for (int v : encoded_values) {
                if (transitions_p.containsKey(v)) {
                    List<Integer> dest = transitions_p.get(v);
                    for (int q : dest) {
                        M[p][q]++;
                    }
                }
            }

            s.append("[");
            for (int q = 0; q < automaton.Q; ++q) {
                s.append(M[p][q]);
                if (q < (automaton.Q - 1)) {
                    s.append(",");
                }
            }
            s.append("]");
            if (p < (automaton.Q - 1)) {
                s.append("," + System.lineSeparator());
            }
        }
        s.append("]);" + System.lineSeparator());
    }

    private static void write_initial_state_vector(Automaton automaton, StringBuilder s) {
        s.append("# The row vector v denotes the indicator vector of the (singleton)" + System.lineSeparator());
        s.append("# set of initial states." + System.lineSeparator());
        s.append("v := Vector[row]([");
        for (int q = 0; q != automaton.Q; ++q) {
            if (q == automaton.q0) {
                s.append("1");
            } else {
                s.append("0");
            }
            if (q < (automaton.Q - 1)) {
                s.append(",");
            }
        }
        s.append("]);" + System.lineSeparator());
    }

    private static void write_final_states_vector(Automaton automaton, StringBuilder s) {
        s.append(System.lineSeparator() + "# The column vector w denotes the indicator vector of the" + System.lineSeparator());
        s.append("# set of final states." + System.lineSeparator());
        s.append("w := Vector[column]([");
        for (int q = 0; q != automaton.Q; ++q) {
            if (automaton.O.getInt(q) != 0) {
                s.append("1");
            } else {
                s.append("0");
            }
            if (q < (automaton.Q - 1)) {
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
        try {
            PrintWriter out = new PrintWriter(address, "UTF-8");
            if (automaton.TRUE_FALSE_AUTOMATON) {
                if (automaton.TRUE_AUTOMATON)
                    out.write("true");
                else
                    out.write("false");
            } else {
                automaton.canonize();
                writeAlphabet(automaton, out);
                for (int q = 0; q < automaton.Q; q++) {
                    writeState(automaton, out, q);
                }
            }
            out.close();
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
    }

    private static void writeAlphabet(Automaton automaton, PrintWriter out) {
        for (int i = 0; i < automaton.A.size(); i++) {
            List<Integer> l = automaton.A.get(i);
            if (automaton.NS.get(i) == null) {
                out.write("{");
                out.write(UtilityMethods.genericListString(l, ", "));
                out.write("} ");
            } else {
                if (i == 0)
                    out.write(automaton.NS.get(i).toString());
                else
                    out.write(" " + automaton.NS.get(i).toString());
            }
        }
        out.write(System.lineSeparator());
    }

    private static void writeState(Automaton automaton, PrintWriter out, int q) {
        out.write(
                System.lineSeparator() + q + " " +
                        automaton.O.getInt(q) + System.lineSeparator());
        for (int n : automaton.d.get(q).keySet()) {
            List<Integer> l = Automaton.decode(automaton, n);
            for (int j = 0; j < l.size(); j++)
                out.write(l.get(j) + " ");
            out.write("->");
            for (int dest : automaton.d.get(q).get(n))
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
        GraphViz gv = new GraphViz();
        if (automaton.TRUE_FALSE_AUTOMATON) {
            gv.addln(gv.start_graph());
            gv.addln("label = \"(): " + predicate + "\";");
            gv.addln("rankdir = LR;");
            if (automaton.TRUE_AUTOMATON)
                gv.addln("node [shape = doublecircle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
            else
                gv.addln("node [shape = circle, label=\"" + 0 + "\", fontsize=12]" + 0 + ";");
            gv.addln("node [shape = point ]; qi");
            gv.addln("qi ->" + 0 + ";");
            if (automaton.TRUE_AUTOMATON)
                gv.addln(0 + " -> " + 0 + "[ label = \"*\"];");
            gv.addln(gv.end_graph());
        } else {
            automaton.canonize();
            gv.addln(gv.start_graph());
            gv.addln("label = \"" + UtilityMethods.toTuple(automaton.label) + ": " + predicate + "\";");
            gv.addln("rankdir = LR;");
            for (int q = 0; q < automaton.Q; q++) {
                if (isDFAO)
                    gv.addln("node [shape = circle, label=\"" + q + "/" + automaton.O.getInt(q) + "\", fontsize=12]" + q + ";");
                else if (automaton.O.getInt(q) != 0)
                    gv.addln("node [shape = doublecircle, label=\"" + q + "\", fontsize=12]" + q + ";");
                else
                    gv.addln("node [shape = circle, label=\"" + q + "\", fontsize=12]" + q + ";");
            }

            gv.addln("node [shape = point ]; qi");
            gv.addln("qi -> " + automaton.q0 + ";");

            TreeMap<Integer, TreeMap<Integer, List<String>>> transitions =
                    new TreeMap<>();
            for (int q = 0; q < automaton.Q; q++) {
                transitions.put(q, new TreeMap<>());
                for (int x : automaton.d.get(q).keySet()) {
                    for (int dest : automaton.d.get(q).get(x)) {
                        transitions.get(q).putIfAbsent(dest, new ArrayList<>());
                        transitions.get(q).get(dest).add(
                                UtilityMethods.toTransitionLabel(Automaton.decode(automaton, x)));
                    }
                }
            }

            for (int q = 0; q < automaton.Q; q++) {
                for (int dest : transitions.get(q).keySet()) {
                    String transition_label = String.join(", ", transitions.get(q).get(dest));
                    gv.addln(q + " -> " + dest + "[ label = \"" + transition_label + "\"];");
                }
            }

            gv.addln(gv.end_graph());
        }
        try {
            PrintWriter out = new PrintWriter(address, "UTF-8");
            out.write(gv.getDotSource());
            out.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
    }
}
