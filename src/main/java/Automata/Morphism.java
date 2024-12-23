/*   Copyright 2021 Laindon Burnett
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

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.TreeMap;

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
    public HashSet<Integer> range;

    // The syntax for declaring a morphism in the command line is identical to that
    // of a saved morphism file, so we reuse this constructor
    public Morphism(String name, String mapString) {
      // The name of the morphism
      this.mapping = ParseMethods.parseMorphism(mapString);
        this.range = new HashSet<>();
        for (Integer key : mapping.keySet()) {
            range.addAll(mapping.get(key));
        }
    }

    // Reads the entirety of a file and passes this into the more general constructor
    public Morphism(String address) throws IOException {
        this("", Files.readString(Paths.get(address)));
    }

    public void write(String address) throws IOException {
        PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8);
        for (Integer x : mapping.keySet()) {
            out.write(x.toString() + " -> ");
            for (Integer y : mapping.get(x)) {
                if ((0 <= y) && (9 >= y)) {
                    out.write(y.toString());
                } else {
                    out.write("[" + y + "]");
                }
            }
            out.write(System.lineSeparator());
        }
        out.close();
    }

    public Automaton toWordAutomaton() {
        int maxImageLength = 0;
        for (int x : mapping.keySet()) {
            int length = mapping.get(x).size();
            if (length > maxImageLength) {
                maxImageLength = length;
            }
        }
        Automaton promotion = new Automaton();
        List<Integer> alphabet = IntStream.rangeClosed(0, maxImageLength - 1).boxed().collect(Collectors.toList());
        promotion.getA().add(alphabet);
        int maxEntry = 0;
        for (int x : mapping.keySet()) {
            for (int y : mapping.get(x)) {
                if (y < 0) {
                    throw new RuntimeException("Cannot promote a morphism with negative values.");
                } else if (y > maxEntry) {
                    maxEntry = y;
                }
            }
        }
        promotion.setQ(maxEntry + 1);
        promotion.setO(IntArrayList.toList(IntStream.rangeClosed(0, promotion.getQ() - 1)));
        for (int x : mapping.keySet()) {
            Int2ObjectRBTreeMap<IntList> xmap = new Int2ObjectRBTreeMap<>();
            for (int i = 0; i < mapping.get(x).size(); i++) {
                IntList newList = new IntArrayList();
                newList.add((int) mapping.get(x).get(i));
                xmap.put(i, newList);
            }
            promotion.getD().add(xmap);
        }
        // this word automaton is purely symbolic in input and we want it in the exact order given
        promotion.setCanonized(true);
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
        for (int x : mapping.keySet()) {
            if (firstElement) {
                imageLength = mapping.get(x).size();
                firstElement = false;
            } else if (mapping.get(x).size() != imageLength) {
                return false;
            }
        }
        this.length = imageLength;
        return true;
    }

    // Generates a command to define an intermediary word automaton given an integer i that accepts iff an i appears in position n of a word
    // These can then be combined efficiently with a combine command as they have disjoint domains
    public String makeInterCommand(int i, String baseAutomatonName, String numSys) {
        if (numSys != "") {
            numSys = "?" + numSys;
        }
        String interCommand = "def " + baseAutomatonName + "_" + i;
        interCommand += " \"" + numSys + " E q, r (n=" + length.toString() + "*q+r & r>=0 & r<" + length;
        for (Integer key : this.mapping.keySet()) {
            boolean exists = false;
            String clause = " & (" + baseAutomatonName + "[q]";
            List<Integer> symbolImage = this.mapping.get(key);
            for (int j = 0; j < symbolImage.size(); j++) {
                if (symbolImage.get(j) == i) {
                    if (!exists) {
                        clause += "= @" + key.toString() + " => (r=" + j;
                        exists = true;
                    } else {
                        clause += "|r=" + j;
                    }
                }
            }
            if (exists) {
                clause += "))";
            } else {
                clause += "!= @" + key.toString() + ")";
            }
            interCommand += clause;
        }
        interCommand += ")\":";
        return interCommand;
    }
}
