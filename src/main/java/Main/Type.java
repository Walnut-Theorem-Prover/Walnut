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

public enum Type {
    automaton, word, arithmetic, alphabetLetter, variable, numberLiteral;

    public String toString() {
        return switch (this) {
            case automaton -> "automaton";
            case word -> "word";
            case arithmetic -> "arithmetic";
            case alphabetLetter -> "alphabet letter";
            case variable -> "variable";
            case numberLiteral -> "number literal";
        };
    }
}
