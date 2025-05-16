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
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The class Morphism represents a morphism from a finite alphabet to the integers,
 * defined by the integer word it sends each member of the alphabet to.
 * <p>
 * For example, with an alphabet of {0, 1, 2}, we define the mappings
 * 0 -> [-3]0102[11],
 * 1 -> 2113,
 * 2 -> 314
 * <p>
 * Here square brackets are used to specify a number not in the range 0-9
 */
public class Morphism {
    // The uniform length of the image of a letter, when applicable
    private Integer length;

    // The mapping between each letter of the alphabet and its image under the morphism
    public TreeMap<Integer, List<Integer>> mapping;

    // The set of values in the image of the morphism
    public Set<Integer> range;

    public Morphism() {}

    /**
     * Create morphism from file.
     */
    @SuppressWarnings("this-escape")
    public Morphism(String address) throws IOException {
        File f = UtilityMethods.validateFile(address);
        String mapString = Files.readString(Paths.get(f.toURI()));
        parseMap(mapString);
    }

    public void parseMap(String mapString) {
        this.mapping = ParseMethods.parseMorphism(mapString);
        this.range = new HashSet<>();
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            range.addAll(entry.getValue());
        }
    }

    public void write(String address) throws IOException {
        try (PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8)) {
            for (Map.Entry<Integer, List<Integer>> entry : mapping.entrySet()) {
                out.write(entry.getKey().toString() + " -> ");
                for (Integer y : entry.getValue()) {
                    if ((0 <= y) && (9 >= y)) {
                        out.write(y.toString());
                    } else {
                        out.write("[" + y + "]");
                    }
                }
                out.write(System.lineSeparator());
            }
        }
    }

    public Automaton toWordAutomaton() {
        int maxImageLength = 0;
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            int length = entry.getValue().size();
            if (length > maxImageLength) {
                maxImageLength = length;
            }
        }
        Automaton promotion = new Automaton();
        List<Integer> alphabet = IntStream.rangeClosed(0, maxImageLength - 1).boxed().collect(Collectors.toList());
        promotion.richAlphabet.getA().add(alphabet);
        int maxEntry = 0;
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            for (int y : entry.getValue()) {
                if (y < 0) {
                    throw new WalnutException("Cannot promote a morphism with negative values.");
                } else if (y > maxEntry) {
                    maxEntry = y;
                }
            }
        }
        List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            Int2ObjectRBTreeMap<IntList> xmap = new Int2ObjectRBTreeMap<>();
            for (int i = 0; i < entry.getValue().size(); i++) {
                IntList newList = new IntArrayList();
                newList.add((int) entry.getValue().get(i));
                xmap.put(i, newList);
            }
            newD.add(xmap);
        }
        promotion.getFa().setFields(
            maxEntry + 1, IntArrayList.toList(IntStream.rangeClosed(0, maxEntry)), newD);

        // this word automaton is purely symbolic in input and we want it in the exact order given
        promotion.fa.setCanonized(true);
        // the base for the automata is the length of the longest image of any letter under the morphism
        promotion.getNS().add(new NumberSystem("msd_" + maxImageLength));

        return promotion;
    }

    /**
     * Determines whether the morphism is uniform.
     * A morphism is uniform if the image of every input symbol in the alphabet
     * has the same length. For example:
     * - If mapping = {a -> "01", b -> "10", c -> "11"}, the morphism is uniform because all outputs have a length of 2.
     * - If mapping = {a -> "01", b -> "10", c -> "1"}, the morphism is not uniform because the output for 'c' has a different length.
     *
     * @return true if the morphism is uniform; false otherwise.
     */
    public boolean isUniform() {
        boolean firstElement = true;
        int imageLength = 0;
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            if (firstElement) {
                imageLength = entry.getValue().size();
                firstElement = false;
            } else if (entry.getValue().size() != imageLength) {
                return false;
            }
        }
        this.length = imageLength;
        return true;
    }

    // Generates a command to define an intermediary word automaton given an integer i that accepts iff an i appears in position n of a word
    // These can then be combined efficiently with a combine command as they have disjoint domains
    public String makeInterCommand(int i, String baseAutomatonName, String numSys) {
        if (!numSys.isEmpty()) {
            numSys = "?" + numSys;
        }
        StringBuilder interCommand = new StringBuilder("def " + baseAutomatonName + "_" + i);
        interCommand.append(" \"").append(numSys).append(" E q, r (n=").append(length.toString()).append("*q+r & r>=0 & r<").append(length);
        for(Map.Entry<Integer, List<Integer>> entry: mapping.entrySet()) {
            boolean exists = false;
            StringBuilder clause = new StringBuilder(" & (" + baseAutomatonName + "[q]");
            List<Integer> symbolImage = entry.getValue();
            for (int j = 0; j < symbolImage.size(); j++) {
                if (symbolImage.get(j) == i) {
                    if (!exists) {
                        clause.append("= @").append(entry.getKey().toString()).append(" => (r=").append(j);
                        exists = true;
                    } else {
                        clause.append("|r=").append(j);
                    }
                }
            }
            if (exists) {
                clause.append("))");
            } else {
                clause.append("!= @").append(entry.getKey().toString()).append(")");
            }
            interCommand.append(clause);
        }
        interCommand.append(")\":");
        return interCommand.toString();
    }
}
