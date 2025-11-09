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

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains a number of useful static methods.
 */
public class UtilityMethods {
    static String ADDRESS_FOR_UNIT_TEST_INTEGRATION_TEST_RESULTS = "src/test/resources/integrationTests/";
    private static final Pattern PATTERN_NUMBER = Pattern.compile("^\\d+$");
    private static final Pattern PATTERN_NEG_NUMBER = Pattern.compile("^neg_\\d+$");
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");

    /**
     * checks if a string is \\d+
     */
    public static boolean isNumber(String s) {
        return PATTERN_NUMBER.matcher(s).matches();
    }

    /**
     * Checks if a string is neg_\\d+, then return the number but negative.
     * Otherwise, returns 0.
     */
    public static int parseNegNumber(String s) {
        if (!PATTERN_NEG_NUMBER.matcher(s).matches()) {
            return 0;
        }
        return parseInt(s.substring(4));
    }

    /**
     * For example when L = [1,2,3] then the result is the string "(1,2,3)"
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
     */
    public static <T> void removeDuplicates(List<T> L) {
        if (L == null || L.size() <= 1) return;
        Set<T> set = new LinkedHashSet<>(L);
        L.clear();
        L.addAll(set);
    }

    /**
     * Checks if the set of L and R are equal. L and R do not have duplicates.
     */
    public static <T> boolean areEqual(List<T> L, List<T> R) {
        if (L == null && R == null) return true;
        if (L == null || R == null) return false;
        return new HashSet<>(L).equals(new HashSet<>(R));
    }

    /**
     * For example when indices = [1,3] and L = [X,Y,Z,W] then the result is [X,Z]
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
     * Parse integer from String. The string may have spaces, which are removed.
     */
    public static int parseInt(String s) {
        return Integer.parseInt(PATTERN_WHITESPACE.matcher(s).replaceAll(""));
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
        return (b % a == 0) ? commonRoot(a, b / a) : -1;
    }

    /**
     * Many objects are stringified as: a_0 , a_1, a_2, ..., a_n
     * Where a_i can be represented as a string, and "," is an arbitrary separator.
     */
    public static String genericListString(List<?> objects, String separator) {
        return objects.stream().map(Object::toString).collect(Collectors.joining(separator));
    }

    public static boolean isSorted(List<String> label) {
        for (int i = 0; i < label.size() - 1; i++) {
            if (label.get(i).compareTo(label.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    public static File validateFile(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file: " + path);
        }
        return file;
    }

    public static String readFromFile(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.isFile()) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            String temp;
            boolean flag = false;
            while ((temp = reader.readLine()) != null) {
                output.append(flag ? System.lineSeparator() : "").append(temp);
                flag = true;
            }
        }
        return output.toString();
    }
}
