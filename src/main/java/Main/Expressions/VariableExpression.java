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
import Automata.NumberSystem;
import Main.Expression;
import Token.Token;

import java.util.List;

public class VariableExpression extends Expression {
  public VariableExpression(String identifier) {
    this.identifier = identifier;
    this.expressionInString = identifier;
  }

  public Automaton act(boolean print, String prefix, StringBuilder log, Token t,
                       NumberSystem ns, List<String> identifiers, Automaton M, List<String> quantify) {
    if (!identifiers.contains(this.identifier)) {
      identifiers.add(this.identifier);
    } else {
      String new_identifier = this.identifier + t.getUniqueString();
      Automaton eq = ns.equality.clone();
      eq.bind(List.of(this.identifier, new_identifier));
      quantify.add(new_identifier);
      identifiers.add(new_identifier);
      M = AutomatonLogicalOps.and(M, eq, print, prefix + " ", log);
    }
    return M;
  }
}
