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

public class TestCase {
    private final String error;
    private final String details;
    private final String mplAddress;
    private final String gvAddress;
    private final Automaton result;

    public TestCase(Automaton result, String error, String mplAddress, String gvAddress, String details) {
        this.result = result;
        this.error = error;
        this.mplAddress = mplAddress;
        this.gvAddress = gvAddress;
        this.details = details;
    }
    public TestCase(Automaton result) {
        this(result, "", "", "", "");
    }

    public Automaton getResult() {
        return result;
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
}
