package Main.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Expression;

import java.util.List;

public class ArithmeticExpression extends Expression {
  public ArithmeticExpression(String expressionInString, Automaton M, String identifier) {
    this.expressionInString = expressionInString;
    this.M = M;
    this.identifier = identifier;
  }

  public Automaton act(boolean print, String prefix, StringBuilder log, List<String> identifiers, Automaton M, List<String> quantify) {
    identifiers.add(this.identifier);
    M = AutomatonLogicalOps.and(M, this.M, print, prefix + " ", log);
    quantify.add(this.identifier);
    return M;
  }
}
