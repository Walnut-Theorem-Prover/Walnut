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

package Token;

import java.util.List;
import java.util.Stack;

import Main.Expression;

public abstract class Token {
    private int arity;
    private int positionInPredicate;
    private static long uniqueCounter = 1000L;

    /**
     * Returns a unique string that contains only non-ASCII characters.
     * This method can be called indefinitely without repeating values.
     */
    public String getUniqueString() {
        uniqueCounter++;
        return convertToNonAscii(uniqueCounter);
    }

    /**
     * Converts a given number into a string representation using a custom base,
     * where the "digits" are non-ASCII characters (from 0x0100 to 0x017F).
     */
    private static String convertToNonAscii(long number) {
        final int base = 128; // Using 128 non-ASCII characters
        StringBuilder sb = new StringBuilder();
        do {
            int digit = (int)(number % base);
            // Map the digit to a non-ASCII character.
            // Since ASCII characters are in the range 0–127, we use code points starting at 0x0100.
            char nonAsciiChar = (char)(0x0100 + digit);
            sb.append(nonAsciiChar);
            number /= base;
        } while (number > 0);
        return sb.reverse().toString();
    }

    public void put(List<Token> postOrder) {
        postOrder.add(this);
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {}

    public boolean isOperator() {
        return false;
    }

    public int getPositionInPredicate() {
        return positionInPredicate;
    }

    public int getArity() {
        return arity;
    }

    public void setArity(int arity) {
        this.arity = arity;
    }

    public void setPositionInPredicate(int positionInPredicate) {
        this.positionInPredicate = positionInPredicate;
    }

    protected void validateArity(Stack<Expression> S, String name1, String name2) {
        if (S.size() < getArity()) throw new RuntimeException(name1 + this + " requires " + getArity() + name2);
    }

    public void validateArity(String name, int otherArity) {
        if (otherArity != getArity()) throw new RuntimeException(
                "function " + name + " requires " + otherArity + " arguments: char at " + getPositionInPredicate());
    }

    public Stack<Expression> reverseStack(Stack<Expression> S) {
        Stack<Expression> temp = new Stack<>();
        for (int i = 0; i < getArity(); i++) {
            temp.push(S.pop());
        }
        return temp;
    }
}