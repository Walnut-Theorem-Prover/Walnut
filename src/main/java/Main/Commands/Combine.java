package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Prover;
import Main.Session;
import Main.TestCase;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Combine {
  public static TestCase combineCommand(String s, List<String> automataNames, IntList outputs, String combineName) {
    if (automataNames.isEmpty()) {
      throw new WalnutException("Combine requires at least one automaton as input.");
    }
    Automaton first = Automaton.readAutomatonFromFile(automataNames.get(0));
    automataNames.remove(0);

    Queue<Automaton> subautomata = new LinkedList<>();

    for (String name : automataNames) {
      Automaton M = Automaton.readAutomatonFromFile(name);
      subautomata.add(M);
    }

    Automaton C = AutomatonLogicalOps.combine(first, subautomata, outputs);

    C.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), combineName, true);
    return new TestCase(C);
  }
}
