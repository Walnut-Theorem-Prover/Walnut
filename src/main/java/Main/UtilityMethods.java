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

package Main;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains a number of useful static methods.
 *
 * @author Hamoon
 */
public class UtilityMethods {
    static String dir = "";
    static String ADDRESS_FOR_COMMAND_FILES = "Command Files/";
    static String ADDRESS_FOR_MACRO_LIBRARY = "Macro Library/";
    static String ADDRESS_FOR_AUTOMATA_LIBRARY = "Automata Library/";
    static String ADDRESS_FOR_WORDS_LIBRARY = "Word Automata Library/";
    static String ADDRESS_FOR_MORPHISM_LIBRARY = "Morphism Library/";
    static String ADDRESS_FOR_TRANSDUCER_LIBRARY = "Transducer Library/";
    static String ADDRESS_FOR_RESULT = "Result/";
    static String ADDRESS_FOR_CUSTOM_BASES = "Custom Bases/";
    static String ADDRESS_FOR_HELP_COMMANDS = "Help Documentation/Commands/";
    static String ADDRESS_FOR_INTEGRATION_TEST_RESULTS = "Test Results/Integration Tests/";
    static String ADDRESS_FOR_UNIT_TEST_INTEGRATION_TEST_RESULTS = "src/test/resources/integrationTests/";

    static String PROMPT = "\n[Walnut]$ ";

    public static void setPaths() {
        String path = System.getProperty("user.dir");
        if (path.endsWith("bin"))
            dir = "../";
    }

    public static String get_address_for_command_files() {
        return dir + ADDRESS_FOR_COMMAND_FILES;
    }

    public static String get_address_for_automata_library() {
        return dir + ADDRESS_FOR_AUTOMATA_LIBRARY;
    }

    public static String get_address_for_macro_library() {
        return dir + ADDRESS_FOR_MACRO_LIBRARY;
    }

    public static String get_address_for_result() {
        return dir + ADDRESS_FOR_RESULT;
    }

    public static String get_address_for_custom_bases() {
        return dir + ADDRESS_FOR_CUSTOM_BASES;
    }

    public static String get_address_for_words_library() {
        return dir + ADDRESS_FOR_WORDS_LIBRARY;
    }

    public static String get_address_for_morphism_library() {
        return dir + ADDRESS_FOR_MORPHISM_LIBRARY;
    }

    public static String get_address_for_transducer_library() {
        return dir + ADDRESS_FOR_TRANSDUCER_LIBRARY;
    }

    public static String get_address_for_integration_test_results() {
        return dir + ADDRESS_FOR_INTEGRATION_TEST_RESULTS;
    }

    public static String get_address_for_help_commands() {
        return dir + ADDRESS_FOR_HELP_COMMANDS;
    }


    public static char min(char a, char b) {
        return a < b ? a : b;
    }

    public static char max(char a, char b) {
        return a < b ? b : a;
    }

    /**
     * checks if a string is \\d+
     *
     * @param s
     * @return
     */
    public static boolean isNumber(String s) {
        return s.matches("^\\d+$");
    }

    /**
     * Checks if a string is neg_\\d+, then return the number but negative.
     * Otherwise, returns 0.
     *
     * @param s
     * @return
     */
    public static int parseNegNumber(String s) {
        if (!s.matches("^neg_\\d+$")) {
            return 0;
        }
        return parseInt(s.substring(4));
    }

    /**
     * Permutes L with regard to permutation.
     * @jn1z notes: However, behavior is *not* what was designed:
     * Expected: "if permutation = [1,2,0] then the return value is [L[1],L[2],L[0]]"
     * Actual:   "if permutation = [1,2,0] then the return value is [L[2],L[0],L[1]]", i.e. the inverse
     * Changing this causes other issues, so we're leaving it.
     * (I suspect as this is the inverse, it ends up not being an issue down the line.)
     * Also: behavior is undefined is permutation size != L.size
     *
     * @param L
     * @param permutation
     * @return
     */
    public static <T> List<T> permute(List<T> L, int[] permutation) {
        List<T> R = new ArrayList<>(L);
        for (int i = 0; i < L.size(); i++) {
            R.set(permutation[i], L.get(i));
        }
        return R;
    }

    /**
     * For example when L = [1,2,3] then the result is the string "(1,2,3)"
     *
     * @param l
     * @return
     */
    public static <T> String toTuple(List<T> l) {
        return "(" + UtilityMethods.genericListString(l, ",") + ")";
    }

    public static <T> String toTransitionLabel(List<T> l) {
        String s = "";
        if (l.size() == 1) {
            s += l.get(0);
            return s;
        }
        return "[" + UtilityMethods.genericListString(l, ",") + "]";
    }

    /**
     * For example when L = [1,3,2,1,3] the result is [1,3,2]
     *
     * @param L
     */
    public static <T> void removeDuplicates(List<T> L) {
        if (L == null || L.size() <= 1) return;
        List<T> R = new ArrayList<>();
        for (int i = 0; i < L.size(); i++) {
            boolean flag = true;
            for (int j = 0; j < i; j++) {
                if (L.get(i).equals(L.get(j))) {
                    flag = false;
                    break;
                }
            }
            if (flag) R.add(L.get(i));
        }
        L.clear();
        L.addAll(R);
    }

    /**
     * Checks if the set of L and R are equal. L and R does not have duplicates.
     *
     * @param L
     * @param R
     * @return
     */
    public static <T> boolean areEqual(List<T> L, List<T> R) {
        if (L == null && R == null) return true;
        if (L == null || R == null) return false;
        if (L.size() != R.size()) return false;
        for (T x : L)
            if (!R.contains(x)) return false;
        return true;
    }

    /**
     * add elements of R that do not exist in L to L.
     * Also: keep order of previous elements of L and new elements (w.r.t. R).
     *
     * @param L
     * @param R
     */
    public static <T> void addAllWithoutRepetition(List<T> L, List<T> R) {
        if (R == null || R.isEmpty()) return;
        for (T x : R) {
            boolean flag = true;
            for (T y : L) {
                if (y.equals(x)) {
                    flag = false;
                    break;
                }
            }
            if (flag)
                L.add(x);
        }
    }

    /**
     * For example when indices = [1,3] and L = [X,Y,Z,W] then the result is [X,Z]
     *
     * @param L
     * @param indices
     */
    public static <T> void removeIndices(List<T> L, List<Integer> indices) {
        List<T> R = new ArrayList<>();
        for (int i = 0; i < L.size(); i++) {
            if (!indices.contains(i))
                R.add(L.get(i));
        }
        L.clear();
        L.addAll(R);
    }

    /**
     * @param s
     * @return
     */
    public static int parseInt(String s) {
        String[] part = s.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String x : part) {
            b.append(x);
        }
        BigInteger val = new BigInteger(b.toString());
        BigInteger min = BigInteger.valueOf(Integer.MIN_VALUE);
        BigInteger max = BigInteger.valueOf(Integer.MAX_VALUE);
        if (val.compareTo(min) < 0 || val.compareTo(max) > 0) {
            throw new RuntimeException("Trying to parse the number " + b + ", which is outside of the integer limit [" + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "].");
        }
        return Integer.parseInt(b.toString());
    }

    /**
     * Return the common root of two numbers a, b. If no common root exists, return -1.
     * https://stackoverflow.com/a/72369344/
     */
    public static int commonRoot(int a, int b) {
        if (a == 1 || b == 1) {
            return -1;
        }
        if (a == b) {
            return a;
        }
        if (a > b) {
            return commonRoot(b, a);
        }
        return (b % a == 0) ? commonRoot(a, b/a) : -1;
    }

    /**
     * Many objects are stringified as: a_0 , a_1, a_2, ..., a_n
     * Where a_i can be represented as a string, and "," is an arbitrary separator.
     */
    public static String genericListString(List<?> objects, String separator) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<objects.size();i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(objects.get(i));
        }
        return sb.toString();
    }

    public static boolean isSorted(List<String> label) {
        for (int i = 0; i < label.size() - 1; i++) {
            if (label.get(i).compareTo(label.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
     * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
     * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
     */
    public static int[] getLabelPermutation(List<String> label, List<String> sorted_label) {
        int[] label_permutation = new int[label.size()];
        for (int i = 0; i < label.size(); i++) {
            label_permutation[i] = sorted_label.indexOf(label.get(i));
        }
        return label_permutation;
    }

    public static List<Integer> getPermutedEncoder(List<List<Integer>> A, List<List<Integer>> permuted_A) {
        List<Integer> permuted_encoder = new ArrayList<>();
        permuted_encoder.add(1);
        for (int i = 0; i < A.size() - 1; i++) {
            permuted_encoder.add(permuted_encoder.get(i) * permuted_A.get(i).size());
        }
        return permuted_encoder;
    }
}
