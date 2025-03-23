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

import Main.EvalComputations.Expressions.Expression;
import Main.WalnutException;

import java.util.List;
import java.util.Stack;


public abstract class Operator extends Token {
    public static final String REVERSE = "`";
    public static final String EXISTS = "E";
    public static final String FORALL = "A";
    public static final String INFINITE = "I";
    public static final String NEGATE = "~";
    protected boolean leftParenthesis = false;
    private int priority;
    protected String op;

    public boolean isOperator() {
        return true;
    }

    public void put(List<Token> postOrder, Stack<Operator> S) {
        if (op.equals(LeftParenthesis.LEFT_PAREN) || op.equals(EXISTS) || op.equals(FORALL) || op.equals(INFINITE)) {
            S.push(this);
            return;
        }
        while (!S.isEmpty()) {
            if (S.peek().getPriority() <= this.getPriority()) {
                if (rightAssociativity() && S.peek().getPriority() == this.getPriority()) {
                    break;
                }
                Operator op = S.pop();
                postOrder.add(op);
            } else {
                break;
            }
        }
        S.push(this);
    }

    public String toString() {
        return op;
    }

    public boolean isLeftParenthesis() {
        return leftParenthesis;
    }

    public boolean rightAssociativity() {
        return op.equals(REVERSE) || this.isNegation(op);
    }

    public void setPriority() {
        if (RelationalOperator.RELATIONAL_OPERATORS.containsKey(op)) {
            priority = 40;
            return;
        }
        if (ArithmeticOperator.ARITHMETIC_OPERATORS.containsKey(op)) {
            switch (ArithmeticOperator.Ops.fromSymbol(op)) {
                case UNARY_NEGATIVE:
                    priority = 5;
                    break;
                case MULT, DIV:
                    priority = 10;
                    break;
                case PLUS, MINUS:
                    priority = 20;
                    break;
            }
            return;
        }
        switch (op) {
            case NEGATE, REVERSE:
                priority = 80;
                break;
            case LogicalOperator.AND, LogicalOperator.OR, LogicalOperator.XOR:
                priority = 90;
                break;
            case LogicalOperator.IMPLY:
                priority = 100;
                break;
            case LogicalOperator.IFF:
                priority = 110;
                break;
            case EXISTS, FORALL, INFINITE:
                priority = 150;
                break;
            case LeftParenthesis.LEFT_PAREN:
                priority = 200;
                break;
            default:
                if (this.isNegation(op)) {
                    priority = 80;
                } else {
                    priority = Integer.MAX_VALUE;
                }
        }
    }

    public int getPriority() {
        return priority;
    }

    /*
    To allow for multiple kinds of tildes (~, ˜,  ̃), this function needs to be run instead of directly comparing the
    character with the usual ~ tilde.
    This function allows for the \u02dc tilde and \u0303 tilde.
     */
    public boolean isNegation(String op) {
        boolean specialNegation = false;

        if (op.length() == 1) {
            String hexString = Integer.toHexString(op.charAt(0));
            // check if the string has unicode code 2dc or 303. different types of tildes.
            specialNegation = hexString.equals("2dc") || hexString.equals("303");
        }

        return specialNegation || op.equals(NEGATE);
    }

    protected void validateArity(Stack<Expression> S) {
        if (S.size() < getArity()) throw new WalnutException("operator " + op + " requires " + getArity() + " operands");
    }
}
