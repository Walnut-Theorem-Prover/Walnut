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
import Main.Expression;

import java.util.List;

public class WordExpression extends Expression {
  public List<String> list_of_identifiers_to_quantify;

  public WordExpression(
      String expressionInString, Automata.Automaton wordAutomaton, Automaton M, List<String> quantify) {
    this.expressionInString = expressionInString;
    this.wordAutomaton = wordAutomaton;
    this.M = M;
    list_of_identifiers_to_quantify = quantify;
  }
}
