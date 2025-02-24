package Main;

/*
Location for basic operation transformations.
Consider making this strongly-typed.
 */
public class BasicOp {

    public static final String EQUAL = "=";
    public static final String NOT_EQUAL = "!=";
    public static final String LESS_THAN = "<";
    public static final String GREATER_THAN = ">";
    public static final String LESS_EQ_THAN = "<=";
    public static final String GREATER_EQ_THAN = ">=";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String DIV = "/";
    public static final String MULT = "*";

    public static int arith(String op, int a, int b) {
      switch (op) {
          case PLUS -> {
              return a + b;
          }
          case MINUS -> {
              return a - b;
          }
          case DIV -> {
              if (b == 0) throw ExceptionHelper.divisionByZero();
              return Math.floorDiv(a, b);
          }
          case MULT -> {
              return a * b;
          }
          default -> throw ExceptionHelper.unexpectedOperator(op);
      }
  }

  public static boolean compare(String op, int a, int b) {
      return switch (op) {
          case EQUAL -> a == b;
          case NOT_EQUAL -> a != b;
          case LESS_THAN -> a < b;
          case GREATER_THAN -> a > b;
          case LESS_EQ_THAN -> a <= b;
          case GREATER_EQ_THAN -> a >= b;
          default -> throw ExceptionHelper.unexpectedOperator(op);
      };
  }

  public static String reverseOperator(String op) {
      return switch (op) {
          case EQUAL -> EQUAL;
          case NOT_EQUAL -> NOT_EQUAL;
          case LESS_THAN -> GREATER_THAN;
          case GREATER_THAN -> LESS_THAN;
          case LESS_EQ_THAN -> GREATER_EQ_THAN;
          case GREATER_EQ_THAN -> LESS_EQ_THAN;
          default -> throw ExceptionHelper.unexpectedOperator(op);
      };
  }
}
