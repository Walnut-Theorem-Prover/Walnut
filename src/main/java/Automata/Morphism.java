/*   Copyright 2021 Laindon Burnett, 2025 John Nicol
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

package Automata;

import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The class Morphism represents a morphism from a finite alphabet to the integers,
 * defined by the integer word it sends each member of the alphabet to.
 * <p>
 * For example, with an alphabet of {0, 1, 2}, we define the mappings
 * 0 -> [-3]0102[11], 1 -> 2113, 2 -> 314
 * <p>
 * Square brackets are used to specify a number not in the range 0-9.
 * As of Walnut 8, square brackets are allowed for alphabet as well as image.
 */
public class Morphism {
    // The uniform length of the image of a letter, when applicable
    public final int length;

    // The mapping between each letter of the alphabet and its image under the morphism
    public final Map<Integer, IntList> mapping;

    // The set of values in the image of the morphism
    public final IntSet range;

    /**
     * Create morphism from file.
     */
    public Morphism(String mapString) {
        this.mapping = ParseMethods.parseMorphism(mapString);
        this.range = new IntOpenHashSet();
        for(Map.Entry<Integer, IntList> entry: mapping.entrySet()) {
            range.addAll(entry.getValue());
        }
        length = determineUniformLength(mapping);
    }

    public void write(String address) throws IOException {
        try (PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8)) {
            for (Map.Entry<Integer, IntList> entry : mapping.entrySet()) {
                out.write(escapedInt(entry.getKey())+ " -> ");
                for (Integer y : entry.getValue()) {
                    out.write(escapedInt(y));
                }
                out.write(System.lineSeparator());
            }
        }
    }

    private static String escapedInt(Integer y) {
        return ((0 <= y) && (9 >= y)) ? y.toString() : "[" + y + "]";
    }

    public Automaton toWordAutomaton() {
        Automaton promotion = new Automaton();

        final int maxImageLength = determineMaxImageLength(mapping);
        promotion.richAlphabet.getA().add(UtilityMethods.intRangeList(maxImageLength));
        final int maxEntry = determineMaxEntry(mapping);
        final List<Int2ObjectRBTreeMap<IntList>> newD = determineTransitions(mapping);
        promotion.getFa().setFields(
            maxEntry + 1, new IntArrayList(UtilityMethods.intRangeList(maxEntry + 1)), newD);
        // this word automaton is purely symbolic in input and we want it in the exact order given
        promotion.fa.setCanonized(true);
        // the base for the automata is the length of the longest image of any letter under the morphism
        promotion.getNS().add(new NumberSystem(NumberSystem.MSD_UNDERSCORE + maxImageLength));

        return promotion;
    }

    private static List<Int2ObjectRBTreeMap<IntList>> determineTransitions(Map<Integer, IntList> mapping) {
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(mapping.size());
        for(Map.Entry<Integer, IntList> entry: mapping.entrySet()) {
            Int2ObjectRBTreeMap<IntList> xmap = new Int2ObjectRBTreeMap<>();
            for (int i = 0; i < entry.getValue().size(); i++) {
                IntList newList = new IntArrayList();
                newList.add(entry.getValue().getInt(i));
                xmap.put(i, newList);
            }
            newD.add(xmap);
        }
        return newD;
    }

    private static int determineMaxEntry(Map<Integer, IntList> mapping) {
        int maxEntry = 0;
        for(Map.Entry<Integer, IntList> entry: mapping.entrySet()) {
            for (int y : entry.getValue()) {
                if (y < 0) {
                    throw WalnutException.morphismNegative();
                } else if (y > maxEntry) {
                    maxEntry = y;
                }
            }
        }
        return maxEntry;
    }

    private static int determineMaxImageLength(Map<Integer, IntList> mapping) {
        int maxImageLength = 0;
        for(Map.Entry<Integer, IntList> entry: mapping.entrySet()) {
            int length = entry.getValue().size();
            if (length > maxImageLength) {
                maxImageLength = length;
            }
        }
        return maxImageLength;
    }

    /**
     * Determines whether the morphism is uniform.
     * A morphism is uniform if the image of every input symbol in the alphabet
     * has the same length. For example:
     * - If mapping = {a -> "01", b -> "10", c -> "11"}, the morphism is uniform because all outputs have a length of 2.
     * - If mapping = {a -> "01", b -> "10", c -> "1"}, the morphism is not uniform because the output for 'c' has a different length.
     *
     * @return image length if uniform, -1 otherwise.
     */
    private static int determineUniformLength(Map<Integer, IntList> mapping) {
        boolean firstElement = true;
        int imageLength = 0;
        for(Map.Entry<Integer, IntList> entry: mapping.entrySet()) {
            if (firstElement) {
                imageLength = entry.getValue().size();
                firstElement = false;
            } else if (entry.getValue().size() != imageLength) {
                return -1;
            }
        }
        return imageLength;
    }

    // Generates the predicate for an intermediary automaton that accepts n iff value i appears at position n.
    // These predicates can be combined efficiently because their domains are disjoint when the base word's
    // output alphabet is covered by this morphism's domain.
    public String makeInterPredicate(int i, String baseAutomatonName, String numSys) {
        StringBuilder predicate = new StringBuilder(numSys);
        predicate.append(" E q, r (n=").append(length).append("*q+r & r>=0 & r<").append(length);
        for (Map.Entry<Integer, IntList> entry : mapping.entrySet()) {
            boolean exists = false;
            StringBuilder clause = new StringBuilder(" & (" + baseAutomatonName + "[q]");
            IntList symbolImage = entry.getValue();
            for (int j = 0; j < symbolImage.size(); j++) {
                if (symbolImage.getInt(j) == i) {
                    if (!exists) {
                        clause.append("= @").append(entry.getKey()).append(" => (r=").append(j);
                        exists = true;
                    } else {
                        clause.append("|r=").append(j);
                    }
                }
            }
            if (exists) {
                clause.append("))");
            } else {
                clause.append("!= @").append(entry.getKey()).append(")");
            }
            predicate.append(clause);
        }
        predicate.append(")");
        return predicate.toString();
    }

    public void requirePositiveUniformLength() {
        if (length < 0) {
            throw WalnutException.morphismNotUniform();
        }
        if (length == 0) {
            throw new WalnutException("A morphism applied to a word automaton must have positive uniform length.");
        }
    }
}
