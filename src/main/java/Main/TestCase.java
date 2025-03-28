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
import java.util.List;

public class TestCase {
    private final String error;
    private final String details;
    private final String mplAddress;
    private final String gvAddress;
    private final Automaton result;
    private final List<AutomatonFilenamePair> automatonPairs;

    public static final String DEFAULT_TESTFILE = "automaton";
    public static final String ERROR_FILE = "error";
    public static final String DETAILS_FILE = "details";

    public TestCase(
        Automaton result, String error, String mplAddress, String gvAddress,
        String details, List<AutomatonFilenamePair> automatonPairs) {
        this.result = result;
        this.error = error;
        this.mplAddress = mplAddress;
        this.gvAddress = gvAddress;
        this.details = details;
        this.automatonPairs = automatonPairs;
    }
    public TestCase(Automaton result) {
        this(result, "", "", "", "",
            List.of(new AutomatonFilenamePair(result, DEFAULT_TESTFILE)));
    }
    public TestCase(Automaton result, List<AutomatonFilenamePair> automatonPairs) {
        this(result, "", "", "", "", automatonPairs);
    }

    public List<AutomatonFilenamePair> getAutomatonPairs() {
        return automatonPairs;
    }

    public String getMpl() throws IOException {
        if (mplAddress == null || mplAddress.isEmpty()) {
            return "";
        }
        return UtilityMethods.readFromFile(mplAddress);
    }

    public String getGraphView() throws IOException {
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
