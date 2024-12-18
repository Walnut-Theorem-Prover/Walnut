package Main.Expressions;

import Automata.Automaton;
import Main.Expression;

import java.util.List;

public class WordExpression extends Expression {
  public List<String> list_of_identifiers_to_quantify;

  public WordExpression(
      String expressionInString, Automata.Automaton W, Automaton M, List<String> quantify) {
    this.expressionInString = expressionInString;
    this.W = W;
    this.M = M;
    list_of_identifiers_to_quantify = quantify;
  }
}
