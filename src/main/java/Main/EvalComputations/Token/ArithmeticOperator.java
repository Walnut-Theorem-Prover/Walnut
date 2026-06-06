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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import Automata.*;
import Main.EvalComputations.Expressions.*;
import Main.Logging;
import Main.WalnutException;
import Main.EvalComputations.Expressions.Expression;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;


public class ArithmeticOperator extends Operator {
    private final NumberSystem ns;

    private final ArithmeticOperator.Ops opp;
    public static final Map<String, ArithmeticOperator.Ops> ARITHMETIC_OPERATORS = new HashMap<>();
    static {
        for (ArithmeticOperator.Ops op : ArithmeticOperator.Ops.values()) {
            ARITHMETIC_OPERATORS.put(op.getSymbol(), op);
        }
    }
    public enum Ops {
        PLUS("+"),
        MINUS("-"),
        DIV("/"),
        MULT("*"),
        UNARY_NEGATIVE("_");

        private final String symbol;

        Ops(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static ArithmeticOperator.Ops fromSymbol(String symbol) {
            for (ArithmeticOperator.Ops op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown arithmetic operator: " + symbol);
        }
    }

    @SuppressWarnings("this-escape")
    public ArithmeticOperator(int position, String op, NumberSystem ns) {
        this.op = op;
        this.opp = Ops.fromSymbol(op);
        setPriority();
        arity = opp.equals(Ops.UNARY_NEGATIVE) ? 1 : 2;
        this.positionInPredicate = position;
        this.ns = ns;
    }

    public String toString() {
        return op + "_" + ns;
    }

    public void act(Stack<Expression> S) {
        super.validateArity(S);
        Expression b = S.pop();
        if (!isValidArithmeticOperator(b))
            throw WalnutException.invalidOperator(op, b);
        if (opp.equals(Ops.UNARY_NEGATIVE)) {
            processUnaryOperator(b, S);
        } else {
            processBinaryOperator(b, S);
        }
    }

    private void processUnaryOperator(Expression b, Stack<Expression> S) {
        if (b instanceof NumberLiteralExpression ne) {
            BigInteger value = ne.value().negate();
            S.push(new NumberLiteralExpression(value.toString(), value, ns));
            return;
        }
        if (b instanceof AlphabetLetterExpression) {
            S.push(new AlphabetLetterExpression("@" + (-b.constant), -b.constant));
            return;
        }
        if (b instanceof WordExpression) {
            WordAutomaton.applyWordArithOperator(b.wordAutomaton, 0, Ops.MINUS, false);
            S.push(b);
            return;
        }

        String c = getUniqueString();
        // b + c = 0
        Automaton M = ns.arithmetic(b.identifier, c, 0, Ops.PLUS);
        Logging.logAndPrint( COMPUTING + " " + op + b);
        M = andThenQuantifyIfArithmetic(b, M);
        S.push(new ArithmeticExpression("(" + op + b + ")", M, c));
        Logging.logAndPrint( COMPUTED + " " + op + b);
    }

    private void processBinaryOperator(Expression b, Stack<Expression> S) {
        Expression a = S.pop();
        if (!isValidArithmeticOperator(a))
            throw WalnutException.invalidOperator(op, a);

        if (a instanceof WordExpression && b instanceof WordExpression) {
            a.wordAutomaton = WordAutomaton.applyWordOperator(a.wordAutomaton, b.wordAutomaton, op);
            Logging.indent();
            a.M = AutomatonLogicalOps.and(a.M, b.M);
            Logging.dedent();
            ((WordExpression)a).identifiersToQuantify.addAll(((WordExpression)b).identifiersToQuantify);
            S.push(a);
            return;
        }
        if (a instanceof WordExpression && (b instanceof AlphabetLetterExpression || b instanceof NumberLiteralExpression)) {
            WordAutomaton.applyWordArithOperator(a.wordAutomaton, getIntConstantForWord(b), opp, true);
            S.push(a);
            return;
        }
        if ((a instanceof AlphabetLetterExpression || a instanceof NumberLiteralExpression) && b instanceof WordExpression) {
            WordAutomaton.applyWordArithOperator(b.wordAutomaton, getIntConstantForWord(a), opp, false);
            S.push(b);
            return;
        }

        if (isConstantExpression(a) && isConstantExpression(b)) {
            BigInteger value = arith(opp, getConstantValue(a), getConstantValue(b));
            S.push(new NumberLiteralExpression(value.toString(), value, ns));
            return;
        }
        String c = getUniqueString();
        Automaton M;
        Logging.logAndPrint( COMPUTING + " " + a + op + b);
        if (a instanceof WordExpression || (a instanceof ArithmeticExpression || a instanceof VariableExpression) && b instanceof WordExpression) {
            /* We rewrite T[a] * 5 = z as
             * (T[a] = @0 => 0 * 5 = z) & (T[a] = @1 => 1 * 5 = z)
             * With more statements of the form (T[a] = @i => i * 5 = z) for each output i.
             */
            WordExpression word;
            Expression arithmetic;
            boolean reverse;
            if (a instanceof WordExpression) {
                word = (WordExpression) a;
                arithmetic = b;
                reverse = false;
            } else {
                word = (WordExpression) b;
                arithmetic = a;
                reverse = true;
            }

            Logging.indent();

            M = new Automaton(true);
            for (int o : word.wordAutomaton.fa.getO()) {
                Automaton N = word.wordAutomaton.clone();
                WordAutomaton.compareWordAutomaton(N, o, RelationalOperator.Ops.EQUAL);
                Automaton C;
                if (o == 0 && opp.equals(Ops.MULT)) {
                    C = ns.getConstant(0);
                    C.bind(List.of(c));
                } else if (reverse) {
                    C = ns.arithmetic(arithmetic.identifier, o, c, opp);
                } else {
                    C = ns.arithmetic(o, arithmetic.identifier, c, opp);
                }
                N = AutomatonLogicalOps.imply(N, C, LogicalOperator.IMPLY);
                M = AutomatonLogicalOps.and(M, N);
            }
            M = AutomatonLogicalOps.and(M, word.M);
            AutomatonQuantification.quantify(M, word.identifiersToQuantify);
            M = andThenQuantifyIfArithmetic(arithmetic, M);

            Logging.dedent();

        } else {
            if (a instanceof NumberLiteralExpression ne) {
                if (ne.isZero() && opp.equals(Ops.MULT)) {
                    S.push(new NumberLiteralExpression("0", BigInteger.ZERO, ns));
                    return;
                }
                M = ns.arithmetic(ne.value(), b.identifier, c, opp);
            } else if (a instanceof AlphabetLetterExpression) {
                if (a.constant == 0 && opp.equals(Ops.MULT)) {
                    S.push(new NumberLiteralExpression("0", BigInteger.ZERO, ns));
                    return;
                }
                M = ns.arithmetic(a.constant, b.identifier, c, opp);
            } else if (b instanceof NumberLiteralExpression ne) {
                if (ne.isZero() && opp.equals(Ops.MULT)) {
                    S.push(new NumberLiteralExpression("0", BigInteger.ZERO, ns));
                    return;
                }
                M = ns.arithmetic(a.identifier, ne.value(), c, opp);
            } else if (b instanceof AlphabetLetterExpression) {
                if (b.constant == 0 && opp.equals(Ops.MULT)) {
                    S.push(new NumberLiteralExpression("0", BigInteger.ZERO, ns));
                    return;
                }
                M = ns.arithmetic(a.identifier, b.constant, c, opp);
            } else {
                M = ns.arithmetic(a.identifier, b.identifier, c, opp);
            }

            M = andThenQuantifyIfArithmetic(a, M);
            M = andThenQuantifyIfArithmetic(b, M);
        }
        S.push(new ArithmeticExpression("(" + a + op + b + ")", M, c));
        Logging.logAndPrint( COMPUTED + " " + a + op + b);
    }

    public static int arith(ArithmeticOperator.Ops op, int a, int b) {
        return arith(op, BigInteger.valueOf(a), BigInteger.valueOf(b)).intValueExact();
    }

    public static BigInteger arith(ArithmeticOperator.Ops op, BigInteger a, BigInteger b) {
        switch (op) {
            case PLUS -> {
                return a.add(b);
            }
            case MINUS -> {
                return a.subtract(b);
            }
            case DIV -> {
                if (b.signum() == 0) throw WalnutException.divisionByZero();
                BigInteger[] qr = a.divideAndRemainder(b);
                return qr[1].signum() != 0 && a.signum() != b.signum() ? qr[0].subtract(BigInteger.ONE) : qr[0];
            }
            case MULT -> {
                return a.multiply(b);
            }
            default -> throw WalnutException.unexpectedOperator(op.getSymbol());
        }
    }

    private static int getIntConstantForWord(Expression e) {
        if (e instanceof NumberLiteralExpression ne) {
            return ne.intValueExact("number literal " + ne + " used in word automaton output arithmetic");
        }
        return e.constant;
    }

    private static boolean isConstantExpression(Expression e) {
        return e instanceof NumberLiteralExpression || e instanceof AlphabetLetterExpression;
    }

    private static BigInteger getConstantValue(Expression e) {
        if (e instanceof NumberLiteralExpression ne) {
            return ne.value();
        }
        return BigInteger.valueOf(e.constant);
    }

    private static boolean isValidArithmeticOperator(Expression a) {
        return (a instanceof AlphabetLetterExpression || a instanceof WordExpression || a instanceof ArithmeticExpression || a instanceof VariableExpression || a instanceof NumberLiteralExpression);
    }
}
