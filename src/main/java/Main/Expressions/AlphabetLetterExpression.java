package Main.Expressions;

import Main.Expression;

public class AlphabetLetterExpression extends Expression {
  public AlphabetLetterExpression(String expressionInString, int value) {
    this.expressionInString = expressionInString;
    this.constant = value;
  }
}
