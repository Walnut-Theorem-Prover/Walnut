package Main;

/**
 * Exceptions are centralized for easier maintenance.
 * @author jn1z
 */
public class ExceptionHelper {
    public static RuntimeException alphabetExceedsSize(int size) {
        return new RuntimeException("size of input alphabet exceeds the limit of " + size);
    }
    public static RuntimeException alphabetIsEmpty() {
        return new RuntimeException("Output alphabet is empty");
    }

    public static RuntimeException bricsNFA() {
        return new RuntimeException("cannot set an automaton of type Automaton to a non-deterministic automaton of type dk.bricks.automaton.Automaton");
    }

    public static RuntimeException divisionByZero() {
        return new RuntimeException("division by zero");
    }

    public static RuntimeException internalMacro(int index) {
        return new RuntimeException("a function/macro cannot be called from inside another function/macro's argument list: char at " + index);
    }

    public static RuntimeException invalidBind() {
        return new RuntimeException("invalid use of method bind");
    }

    public static RuntimeException invalidCommand() {
        return new RuntimeException("Invalid command.");
    }

    public static RuntimeException invalidCommand(String command) {
        return new RuntimeException("Invalid command: " + command);
    }

    public static RuntimeException invalidCommandUse(String command) {
        return new RuntimeException("Invalid use of the " + command + " command.");
    }

    public static RuntimeException negativeConstant(int a) {
        return new RuntimeException("negative constant " + a);
    }

    public static RuntimeException noSuchCommand() {
        return new RuntimeException("No such command exists.");
    }

    public static RuntimeException operatorMissing(int index) {
        return new RuntimeException("An operator is missing: char at " + index);
    }

    public static RuntimeException unbalancedParen(int index) {
        return new RuntimeException("unbalanced parenthesis: char at " + index);
    }

    public static RuntimeException undefinedToken(int position) {
        return new RuntimeException("Undefined token: char at " + position);
    }

    public static RuntimeException undefinedStatement(int line, String address) {
        return new RuntimeException("Undefined statement: line "+ line + " of file " + address);
    }
}
