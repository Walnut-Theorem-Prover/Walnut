package Main.Commands;

import Automata.Automaton;
import Automata.FA.FA;
import Automata.NumberSystem;
import Main.Logging;
import Main.Session;
import Main.TestCase;
import Main.WalnutException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Main.Prover.RE_WORD_OF_CMD_NO_SPC;

public class Concat {
  private static final String RE_FOR_AN_AUTOMATON_IN_concat_CMD = RE_WORD_OF_CMD_NO_SPC;
  private static final Pattern PAT_FOR_AN_AUTOMATON_IN_concat_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_concat_CMD);

  public static TestCase concat(String s, String concatAutomata, String concatName) {
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_concat_CMD.matcher(concatAutomata);
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.size() < 2) {
      throw new WalnutException("Concatenation requires at least two automata as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = concat(C, automataNames);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), concatName, true);
    return new TestCase(C);
  }

  // concatenate
  public static Automaton concat(Automaton automaton, List<String> automataNames) {
      Automaton first = automaton.clone();

      for (String automataName : automataNames) {
          long timeBefore = System.currentTimeMillis();
          Automaton N = Automaton.readAutomatonFromFile(automataName);

          first = concat(first, N);

          long timeAfter = System.currentTimeMillis();
          Logging.logMessage("concatenated =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
      }
      return first;
  }

  private static Automaton concat(Automaton automaton, Automaton other) {
      long timeBefore = System.currentTimeMillis();
      Logging.logMessage("concat: " + automaton.fa.getQ() + " state automaton with " + other.fa.getQ() + " state automaton");

      // ensure that N has the same number system as first.
      if (NumberSystem.isNSDiffering(other.getNS(), automaton.getNS(), automaton.richAlphabet.getA(), other.richAlphabet.getA())) {
          throw new WalnutException("Automata to be concatenated must have the same number system(s).");
      }

      Automaton N = automaton.clone();

      int originalQ = automaton.fa.getQ();

      FA.concatStates(other.fa, N.fa, originalQ); // NOTE: this may be an NFA

      N.normalizeNumberSystems();

      N.determinizeAndMinimize();
      N.applyAllRepresentations();

      long timeAfter = System.currentTimeMillis();
      Logging.logMessage("concat complete: " + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");

      return N;
  }
}
