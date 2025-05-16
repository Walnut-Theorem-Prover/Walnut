/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
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

package Main.EvalComputations.Token;

import Main.WalnutException;

import java.util.List;
import java.util.Stack;


public class RightParenthesis extends Operator {
    public RightParenthesis(int position) {
        this.positionInPredicate = position;
    }

    public void put(List<Token> postOrder, Stack<Operator> S) {
        while (!S.isEmpty()) {
            if (!S.peek().isLeftParenthesis()) {
                postOrder.add(S.pop());
            } else {
                S.pop();
                return;
            }
        }
        throw WalnutException.unbalancedParen(positionInPredicate);
    }
}
