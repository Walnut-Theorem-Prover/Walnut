package Main.Commands;

import Automata.Automaton;
import Main.Prover;
import Main.Session;
import Main.TestCase;

public class Star {
  public static TestCase star(String s, String oldName, String newName) {
    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(oldName + Prover.TXT_EXTENSION));

    Automaton C = M.star();

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), newName, false);
    return new TestCase(C);
  }
}
