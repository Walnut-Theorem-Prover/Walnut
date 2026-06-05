package Main.Commands;

import Automata.Automaton;
import Main.Prover;
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

    C = C.concat(automataNames);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), concatName, true);
    return new TestCase(C);
  }
}
