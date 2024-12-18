package Main.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.NumberSystem;
import Main.Expression;
import Token.Token;

import java.util.List;

public class VariableExpression extends Expression {
  public VariableExpression(String identifier) {
    this.identifier = identifier;
    this.expressionInString = identifier;
  }

  public Automaton act(boolean print, String prefix, StringBuilder log, Token t,
                       NumberSystem ns, List<String> identifiers, Automaton M, List<String> quantify) {
    if (!identifiers.contains(this.identifier)) {
      identifiers.add(this.identifier);
    } else {
      String new_identifier = this.identifier + t.getUniqueString();
      Automaton eq = ns.equality.clone();
      eq.bind(this.identifier, new_identifier);
      quantify.add(new_identifier);
      identifiers.add(new_identifier);
      M = AutomatonLogicalOps.and(M, eq, print, prefix + " ", log);
    }
    return M;
  }
}
