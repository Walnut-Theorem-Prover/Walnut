package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.WordAutomaton;
import Main.ProverHelper;
import Main.TestCase;

public class Reverse {
  public static TestCase reverseCommand(String s, String inFileName, boolean isDFAO, String newName) {
    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));
    if (isDFAO) {
      WordAutomaton.reverseWithOutput(M, true);
    } else {
      AutomatonLogicalOps.reverse(M, true);
    }
    M.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), newName, true);
    return new TestCase(M);
  }
}
