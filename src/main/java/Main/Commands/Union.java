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

public class Union {
  private static final String RE_FOR_AN_AUTOMATON_IN_union_CMD = RE_WORD_OF_CMD_NO_SPC;
  private static final Pattern PAT_FOR_AN_AUTOMATON_IN_union_CMD =
      Pattern.compile(RE_FOR_AN_AUTOMATON_IN_union_CMD);

  public static TestCase union(String s, String unionAutomata, String unionName) {
    List<String> automataNames = new ArrayList<>();

    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_union_CMD.matcher(unionAutomata);
    while (m1.find()) {
      automataNames.add(m1.group(1));
    }

    if (automataNames.isEmpty()) {
      throw new WalnutException("Union requires at least one automaton as input.");
    }
    Automaton C = Automaton.readAutomatonFromFile(automataNames.get(0));

    automataNames.remove(0);

    C = C.unionOrIntersect(automataNames, Prover.UNION);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), unionName, true);
    return new TestCase(C);
  }
}
