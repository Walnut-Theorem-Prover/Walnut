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
package Main.EvalComputations.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;

import java.util.List;

public class AutomatonExpression extends Expression {
  public AutomatonExpression(String expressionInString, Automata.Automaton M) {
    this.expressionInString = expressionInString;
    this.M = M;
  }
  public Automaton act(boolean print, String prefix, String name, StringBuilder log, int i, Automaton M, List<String> identifiers) {
    if (this.M.getArity() != 1) {
      throw new RuntimeException("argument " + (i + 1) + " of function " + name + " cannot be an automaton with != 1 inputs");
    }
    if (!this.M.isBound()) {
      throw new RuntimeException("argument " + (i + 1) + " of function " + name + " cannot be an automaton with unlabeled input");
    }
    identifiers.add(this.M.getLabel().get(0));
    M = AutomatonLogicalOps.and(M, this.M, print, prefix + " ", log);
    return M;
  }
}
