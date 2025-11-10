/*	 Copyright 2016 Hamoon Mousavi
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

import Automata.Automaton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static Automata.Writer.AutomatonMatrixWriter.EMPTY_MATRIX_TEST_CASES;

public class TestCase {
    private final String error;
    private final String details;
    private final List<String> matrixAddresses;
    private final String gvAddress;
    private final List<AutomatonFilenamePair> automatonPairs;

    public static final String DEFAULT_TESTFILE = "automaton";
    public static final String OST_REPR_TESTFILE = DEFAULT_TESTFILE + "_repr";
    public static final String ERROR_FILE = "error";
    public static final String DETAILS_FILE = "details";

    public TestCase(
        String error, List<String> matrixAddresses, String gvAddress,
        String details, List<AutomatonFilenamePair> automatonPairs) {
        this.error = error;
        this.matrixAddresses = matrixAddresses;
        this.gvAddress = gvAddress;
        this.details = details;
        this.automatonPairs = automatonPairs;
    }
    public TestCase(Automaton result) {
        this("", EMPTY_MATRIX_TEST_CASES, "", "",
            List.of(new AutomatonFilenamePair(result, DEFAULT_TESTFILE)));
    }
    public TestCase(List<AutomatonFilenamePair> automatonPairs) {
        this("", EMPTY_MATRIX_TEST_CASES, "", "", automatonPairs);
    }

    public List<AutomatonFilenamePair> getAutomatonPairs() {
        return automatonPairs;
    }

    public List<String> getMatrixOutput() throws IOException {
        if (matrixAddresses == null || matrixAddresses.isEmpty()) {
            return EMPTY_MATRIX_TEST_CASES;
        }
        List<String> output = new ArrayList<>();
        for (String address: matrixAddresses) {
            output.add(UtilityMethods.readFromFile(address));
        }
        return output;
    }

    public String getGraphViz() throws IOException {
        if (gvAddress == null || gvAddress.isEmpty()) {
            return "";
        }
        return UtilityMethods.readFromFile(gvAddress);
    }

    public String getDetails() {
        return details;
    }

    public String getError() {
        return error;
    }

    public record AutomatonFilenamePair(Automaton automaton, String filename) { }

}
