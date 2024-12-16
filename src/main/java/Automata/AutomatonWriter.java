package Automata;

import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomatonWriter {

    /**
     * Writes down matrices for this automaton to a .mpl file given by the address.
     * @param automaton
     * @param address
     */
    public static String write_matrices(Automaton automaton, String address, List<String> free_variables)throws Exception{
        if(automaton.TRUE_FALSE_AUTOMATON){
            throw new Exception("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
        }
        automaton.canonize();
        StringBuilder s = new StringBuilder();
        s.append("with(ArrayTools):" + UtilityMethods.newLine());
        write_initial_state_vector(automaton, s);
        s.append(UtilityMethods.newLine() + "# In what follows, the M_i_x, for a free variable i and a value x, denotes" + UtilityMethods.newLine());
        s.append("# an incidence matrix of the underlying graph of (the automaton of)" + UtilityMethods.newLine());
        s.append("# the predicate in the query." + UtilityMethods.newLine());
        s.append("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of" + UtilityMethods.newLine());
        s.append("# transitions with i=x from p to q." + UtilityMethods.newLine());
        for(String variable : free_variables){
            if(!automaton.label.contains(variable)){
                throw new Exception("incidence matrices for the variable " + variable + " cannot be calculated, because " + variable +" is not a free variable.");
            }
        }
        List<Integer> indices = free_variables.stream().map(variable -> automaton.label.indexOf(variable)).collect(Collectors.toList());
        List<List<Integer>> indexValueLists = indices.stream().map(index -> automaton.A.get(index)).collect(Collectors.toList());
        List<List<Integer>> valueLists = automaton.cartesianProduct(indexValueLists);
        for (List<Integer> valueList: valueLists) {
            write_matrix_for_a_variable_list_value_pair(automaton, free_variables,valueList,indices,s);
        }
        write_final_states_vector(automaton, s);
        s.append(UtilityMethods.newLine() + "for i from 1 to Size(v)[2] do v := v.M_");
        s.append(String.join("_", free_variables)+"_");
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
        s.append(UtilityMethods.newLine() + "M_"+String.join("_", variables)+"_");
        s.append(valueList.stream().map(String::valueOf).collect(Collectors.joining("_")));
        s.append(" := Matrix([");
        Set<Integer> encoded_values = new HashSet<>();
        for(int x = 0; x != automaton.alphabetSize; ++x){
            List<Integer> decoding = automaton.decode(x);
            List<Integer> compareList = indices.stream().map(index -> decoding.get(index)).collect(Collectors.toList());
            if(compareList.equals(valueList)){
                encoded_values.add(x);
            }
        }
        int[][] M = new int[automaton.Q][automaton.Q];
        for(int p = 0; p < automaton.Q; ++p){
            Int2ObjectRBTreeMap<IntList> transitions_p = automaton.d.get(p);
            for(int v : encoded_values){
                if(transitions_p.containsKey(v)){
                    List<Integer> dest = transitions_p.get(v);
                    for(int q:dest){
                        M[p][q]++;
                    }
                }
            }

            s.append("[");
            for(int q = 0; q < automaton.Q; ++q){
                s.append(M[p][q]);
                if(q < (automaton.Q -1)){
                    s.append(",");
                }
            }
            s.append("]");
            if(p < (automaton.Q -1)){
                s.append("," + UtilityMethods.newLine());
            }
        }
        s.append("]);" + UtilityMethods.newLine());
    }

    private static void write_initial_state_vector(Automaton automaton, StringBuilder s){
        s.append("# The row vector v denotes the indicator vector of the (singleton)" + UtilityMethods.newLine());
        s.append("# set of initial states." + UtilityMethods.newLine());
        s.append("v := Vector[row]([");
        for(int q = 0; q != automaton.Q; ++q){
            if(q == automaton.q0){
                s.append("1");
            }
            else{
                s.append("0");
            }
            if(q < (automaton.Q -1)){
                s.append(",");
            }
        }
        s.append("]);" + UtilityMethods.newLine());
    }

    private static void write_final_states_vector(Automaton automaton, StringBuilder s){
        s.append(UtilityMethods.newLine()+"# The column vector w denotes the indicator vector of the" + UtilityMethods.newLine());
        s.append("# set of final states." + UtilityMethods.newLine());
        s.append("w := Vector[column]([");
        for(int q = 0; q != automaton.Q; ++q){
            if(automaton.O.getInt(q) != 0){
                s.append("1");
            }
            else{
                s.append("0");
            }
            if(q < (automaton.Q -1)){
                s.append(",");
            }
        }
        s.append("]);" + UtilityMethods.newLine());
    }

}
