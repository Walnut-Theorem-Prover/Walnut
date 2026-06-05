package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Session;
import Main.TestCase;

public class Quotient {
  public static TestCase rightQuotient(String s, String oldName1, String oldName2, String newName) {
    Automaton M1 = Automaton.readAutomatonFromFile(oldName1);
    Automaton M2 = Automaton.readAutomatonFromFile(oldName2);
    Automaton C = AutomatonLogicalOps.rightQuotient(M1, M2, false);
    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), newName, false);
    return new TestCase(C);
  }

  public static TestCase leftQuotient(String s, String oldName1, String oldName2, String newName) {
    Automaton M1 = Automaton.readAutomatonFromFile(oldName1);
    Automaton M2 = Automaton.readAutomatonFromFile(oldName2);
    Automaton C = AutomatonLogicalOps.leftQuotient(M1, M2);
    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), newName, false);
    return new TestCase(C);
  }
}
