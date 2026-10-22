package Main.Aal;

import Automata.*;
import Automata.FA.FA;
import Automata.Learning.LabeledCompactDFA;
import Main.*;
import net.automatalib.automaton.fsa.DFA;

public class Aal {

  public static final String COMMAND = "aal";

  /** Parses and runs an AAL command, defaulting an omitted learner to LSTAR. */
  public static TestCase aalCommand(String command) {
    throw new WalnutException("The command " + command + " is not implemented");
  }

  /** Restores Walnut alphabet and number-system metadata on a learned DFA. */
  private static <S> Automaton automatonFromLearnedDfa(
      DFA<S, Integer> dfa, LabeledCompactDFA validRep) {
    Automaton result = new Automaton();
    result.fa = new FA();
    result.fa.setFromAutomataLibDFA(dfa, validRep.cDFA.getInputAlphabet());
    result.richAlphabet = validRep.richAlphabet;
    result.setNS(validRep.NS);
    return result;
  }
}
