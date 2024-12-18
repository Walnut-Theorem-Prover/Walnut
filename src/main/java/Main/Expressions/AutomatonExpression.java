package Main.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Expression;

import java.util.List;

public class AutomatonExpression extends Expression {
  public AutomatonExpression(String expressionInString, Automata.Automaton M) {
    this.expressionInString = expressionInString;
    this.M = M;
  }
  public Automaton act(boolean print, String prefix, String name, StringBuilder log, int i, Automaton M, List<String> identifiers) {
    if (this.M.getArity() != 1) {
      throw new RuntimeException("argument " + (i + 1) + " of function " + name + " cannot be an automaton with != 1 inputs");
    }
    if (!this.M.isBound()) {
      throw new RuntimeException("argument " + (i + 1) + " of function " + name + " cannot be an automaton with unlabeled input");
    }
    identifiers.add(this.M.getLabel().get(0));
    M = AutomatonLogicalOps.and(M, this.M, print, prefix + " ", log);
    return M;
  }
}
