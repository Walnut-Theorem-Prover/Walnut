package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Session;
import Main.TestCase;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Main.Prover.RE_EQ_INT_OPTIONAL;
import static Main.Prover.RE_WORD_OF_CMD_NO_SPC;

public class Combine {
  private static final String RE_FOR_AN_AUTOMATON_IN_combine_CMD =
      RE_WORD_OF_CMD_NO_SPC + "(" + RE_EQ_INT_OPTIONAL + ")";
  private static final Pattern PAT_FOR_AN_AUTOMATON_IN_combine_CMD =
      Pattern.compile(RE_FOR_AN_AUTOMATON_IN_combine_CMD);

  public static TestCase combineCommand(String s, String combineAutomata, String combineName) {
    List<String> automataNames = new ArrayList<>();
    IntList outputs = new IntArrayList();
    int argumentCounter = 0;

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_combine_CMD.matcher(combineAutomata);
    while (m1.find()) {
      argumentCounter++;
      String t = m1.group(1);
      String u = m1.group(2);
      // if no output is specified for a subautomaton, the default output is the index of the subautomaton in the argument list
      if (u.isEmpty()) {
        outputs.add(argumentCounter);
      } else {
        u = u.substring(1);
        // remove colon then convert string to integer
        outputs.add(Integer.parseInt(u));
      }
      automataNames.add(t);
    }

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
