package Main;

import Main.EvalComputations.Expressions.Expression;

import java.io.Serial;

/**
 * Exceptions are centralized for easier maintenance.
 * @author jn1z
 */
public class WalnutException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 12345L; // just to make compiler happy

    public WalnutException(String s) {
        super(s);
    }
    public WalnutException(String s, Exception e) {
        super(s, e);
    }

    public static WalnutException alphabetExceedsSize(int size) {
        return new WalnutException("size of input alphabet exceeds the limit of " + size);
    }
    public static WalnutException alphabetIsEmpty() {
        return new WalnutException("Output alphabet is empty");
    }

    public static WalnutException arrayOverflow(String v, long count) {
        return new WalnutException("Array overflow: " + v + " is of size " + count + " which can't be handled by Java arrays");
    }

    public static WalnutException bricsNFA() {
        return new WalnutException("cannot set an automaton of type Automaton to a non-deterministic automaton of type dk.bricks.automaton.Automaton");
    }

    public static WalnutException divisionByZero() {
        return new WalnutException("division by zero");
    }

    public static WalnutException errorCommand(String cmd) {
        return new WalnutException("Error using the " + cmd + " command.");
    }

    public static WalnutException fileDoesNotExist(String address) {
        return new WalnutException("File does not exist: " + address);
    }

    public static WalnutException internalMacro(int index) {
        return new WalnutException("a function/macro cannot be called from inside another function/macro's argument list: char at " + index);
    }

    public static WalnutException invalidBind() {
        return new WalnutException("invalid use of method bind");
    }

    public static WalnutException invalidCommand() {
        return new WalnutException("Invalid command.");
    }

    public static WalnutException invalidCommand(String command) {
        return new WalnutException("Invalid command: " + command);
    }

    public static WalnutException invalidCommandUse(String command) {
        return new WalnutException("Invalid use of the " + command + " command.");
    }

    public static WalnutException invalidOperator(String op, Expression a) {
        return new WalnutException("operator " + op + " cannot be applied to the operand " + a + " of type " + a.getClass().getName());
    }

    public static WalnutException invalidDualOperators(String op, Expression a, Expression b) {
        return new WalnutException("operator " + op + " cannot be applied to operands " + a + " and " + b + " of types " + a.getClass().getName() + " and " + b.getClass().getName() + " respectively");
    }

    public static WalnutException negativeConstant(int a) {
        return new WalnutException("negative constant " + a);
    }

    public static WalnutException noSuchCommand() {
        return new WalnutException("No such command exists.");
    }

    public static WalnutException operatorMissing(int index) {
        return new WalnutException("An operator is missing: char at " + index);
    }

    public static WalnutException operatorTwoVariables(String operator) {
        return new WalnutException("the operator " + operator + " cannot be applied to two variables");
    }

    public static WalnutException unbalancedParen(int index) {
        return new WalnutException("unbalanced parenthesis: char at " + index);
    }

    public static WalnutException undefinedStatement(long lineNumber, String address) {
        return new WalnutException("Undefined statement: line at " + lineNumber + " of file " + address);
    }

    public static WalnutException undefinedToken(int position) {
        return new WalnutException("Undefined token: char at " + position);
    }

    public static WalnutException unexpectedOperator(String op) {
        return new WalnutException("Unexpected operator:" + op);
    }
}
