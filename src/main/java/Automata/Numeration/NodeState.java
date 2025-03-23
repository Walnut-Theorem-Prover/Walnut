/*   Copyright 2019 Aseem Baranwal, 2025 John Nicol
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

package Automata.Numeration;

public record NodeState(int state, int startIndex, int seenIndex) implements Comparable<NodeState> {

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof NodeState) &&
            ((NodeState) obj).state == state &&
            ((NodeState) obj).startIndex == startIndex &&
            ((NodeState) obj).seenIndex == seenIndex;
    }

    public int compareTo(NodeState obj) {
        if (this.equals(obj)) {
            return 0;
        } else if ((obj.state > state) ||
            (obj.state == state && obj.startIndex > startIndex) ||
            (obj.state == state && obj.startIndex == startIndex && obj.seenIndex > seenIndex)) {
            return -1;
        } else {
            return 1;
        }
    }

    public String toString() {
        return "[" + state + " " + startIndex + " " + seenIndex + "]";
    }
}
