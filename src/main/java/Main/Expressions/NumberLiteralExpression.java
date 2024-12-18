package Main.Expressions;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.NumberSystem;
import Main.Expression;
import Token.Token;

import java.util.List;

public class NumberLiteralExpression extends Expression {
  public NumberLiteralExpression(String expressionInString, int value, NumberSystem base) {
    this.expressionInString = expressionInString;
    this.constant = value;
    this.base = base;
  }
  public Automaton act(boolean print, String prefix, StringBuilder log, Token t,
                       List<String> identifiers, List<String> quantify, Automaton M) {
    Automaton constant = this.base.get(this.constant);
    String id = t.getUniqueString();
    constant.bind(id);
    identifiers.add(id);
    quantify.add(id);
    M = AutomatonLogicalOps.and(M, constant, print, prefix + " ", log);
    return M;
  }
}
