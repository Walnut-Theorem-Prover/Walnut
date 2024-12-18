/*	 Copyright 2025 John Nicol
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
package Main.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Expression;

import java.util.List;

public class ArithmeticExpression extends Expression {
  public ArithmeticExpression(String expressionInString, Automaton M, String identifier) {
    this.expressionInString = expressionInString;
    this.M = M;
    this.identifier = identifier;
  }

  public Automaton act(boolean print, String prefix, StringBuilder log, List<String> identifiers, Automaton M, List<String> quantify) {
    identifiers.add(this.identifier);
    M = AutomatonLogicalOps.and(M, this.M, print, prefix + " ", log);
    quantify.add(this.identifier);
    return M;
  }
}
