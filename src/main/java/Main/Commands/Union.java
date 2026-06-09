package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.NumberSystem;
import Main.*;
import Main.EvalComputations.Token.LogicalOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Main.Logging.COMPUTED;
import static Main.Prover.*;

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

    C = unionOrIntersect(C, automataNames, Prover.UNION);

    C.writeAutomata(s, Session.getWriteAddressForAutomataLibrary(), unionName, true);
    return new TestCase(C);
  }

  /**
   * Either perform the union or intersection of a list of automata.
   *
   * @param automaton
   * @param automataNames - list of automata names, saved in Automata Library
   * @param op            - either "union" or "intersect"
   * @return The union/intersection of all automata in automataNames and this automaton
   */
  public static Automaton unionOrIntersect(Automaton automaton, List<String> automataNames, String op) {
      Automaton first = automaton.clone();
      for (String automataName : automataNames) {
          long timeBefore = System.currentTimeMillis();
          Automaton N = Automaton.readAutomatonFromFile(automataName);

          // ensure that N has the same number system as first.
          if (NumberSystem.isNSDiffering(N.getNS(), first.getNS(), N.richAlphabet.getA(), first.richAlphabet.getA())) {
              throw new WalnutException("Automata to be unioned must have the same number system(s).");
          }

          // crossProduct requires labeling; make an arbitrary labeling and use it for both: this is valid since
          // input alphabets and arities are assumed to be identical for the combine method
          first.randomLabel();
          N.setLabel(first.getLabel());

          if (op.equals(UNION)) {
              first = AutomatonLogicalOps.or(first, N, LogicalOperator.OR);
          } else if (op.equals(INTERSECT)) {
              first = AutomatonLogicalOps.and(first, N);
          } else {
              throw new WalnutException("Internal union/intersect error");
          }

          long timeAfter = System.currentTimeMillis();
          Logging.logMessage(COMPUTED + " =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
      }
      return first;
  }
}
