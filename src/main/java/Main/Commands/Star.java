package Main.Commands;

import Automata.Automaton;
import Automata.FA.FA;
import Main.Logging;
import Main.Prover;
import Main.Session;
import Main.TestCase;

public class Star {
  public static TestCase star(String s, String oldName, String newName) {
    Automaton M = new Automaton(
        Session.getReadFileForAutomataLibrary(oldName + Prover.TXT_EXTENSION));

    Automaton C = star(M);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), newName, false);
    return new TestCase(C);
  }

  public static Automaton star(Automaton automaton) {
      long timeBefore = System.currentTimeMillis();
      Logging.logMessage("star: " + automaton.fa.getQ() + " state automaton");

      Automaton N = automaton.clone();
      FA.starStates(automaton.fa, N.fa); // NOTE: this may be an NFA
      N.normalizeNumberSystems();
      N.forceCanonize();
      N.determinizeAndMinimize();
      N.applyAllRepresentations();

      long timeAfter = System.currentTimeMillis();
      Logging.logMessage("star complete: " + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");

      return N;
  }
}
