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
import Automata.NumberSystem;
import Main.EvalComputations.Token.Token;
import Main.Logging;
import Main.WalnutException;

import java.math.BigInteger;
import java.util.List;

public class NumberLiteralExpression extends Expression {
  private final BigInteger value;
  private final NumberSystem base;

  public NumberLiteralExpression(String expressionInString, BigInteger value, NumberSystem base) {
    this.expressionInString = expressionInString;
    this.value = value;
    this.base = base;
    try {
      this.constant = value.intValueExact();
    } catch (ArithmeticException e) {
      this.constant = 0;
    }
  }

  public BigInteger value() {
    return value;
  }

  public boolean isZero() {
    return value.signum() == 0;
  }

  public int intValueExact(String context) {
    try {
      return value.intValueExact();
    } catch (ArithmeticException e) {
      throw new WalnutException(context + " must fit in a Java int, found: " + value, e);
    }
  }

  public Automaton act(Token t, List<String> identifiers, List<String> quantify, Automaton M) {
    Automaton constant = this.base.getConstant(this.value);
    String id = t.getUniqueString();
    constant.bind(List.of(id));
    identifiers.add(id);
    quantify.add(id);
    Logging.indent();
    M = AutomatonLogicalOps.and(M, constant);
    Logging.dedent();
    return M;
  }
}
