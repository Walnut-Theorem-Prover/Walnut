package Main.Commands;

import Automata.Automaton;
import Automata.FA.ProductStrategies;
import Automata.WordAutomaton;
import Main.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;
import static Main.Prover.RE_WORD_OF_CMD_NO_SPC;

public class Join {
  static final String RE_FOR_AN_AUTOMATON_IN_join_CMD = RE_WORD_OF_CMD_NO_SPC + "((\\s*\\[\\s*[a-zA-Z&&[^AE]]\\w*\\s*])+)";
  static final Pattern PAT_FOR_AN_AUTOMATON_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_IN_join_CMD);
  static final int GROUP_JOIN_AUTOMATON_NAME = 1, GROUP_JOIN_AUTOMATON_INPUT = 2;
  static final String RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = "\\[\\s*([a-zA-Z&&[^AE]]\\w*)\\s*]";
  static final Pattern PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD = Pattern.compile(RE_FOR_AN_AUTOMATON_INPUT_IN_join_CMD);

  public static TestCase joinCommand(String s, String joinAutomata, String joinName) {
    Matcher m1 = PAT_FOR_AN_AUTOMATON_IN_join_CMD.matcher(joinAutomata);
    List<Automaton> subautomata = new ArrayList<>();
    boolean isDFAO = false;
    while (m1.find()) {
      String automatonName = m1.group(GROUP_JOIN_AUTOMATON_NAME);
      String addressForWordAutomaton
          = Session.getReadFileForWordsLibrary(automatonName + Prover.TXT_EXTENSION);
      Automaton M;
      if ((new File(addressForWordAutomaton)).isFile()) {
        M = new Automaton(addressForWordAutomaton);
        isDFAO = true;
      } else {
        String addressForAutomaton
            = Session.getReadFileForAutomataLibrary(automatonName + Prover.TXT_EXTENSION);
        M = new Automaton(addressForAutomaton);
      }

      String automatonInputs = m1.group(GROUP_JOIN_AUTOMATON_INPUT);
      Matcher m2 = PAT_FOR_AN_AUTOMATON_INPUT_IN_join_CMD.matcher(automatonInputs);
      List<String> label = new ArrayList<>();
      while (m2.find()) {
        String t = m2.group(1);
        label.add(t);
      }
      if (label.size() != M.richAlphabet.getA().size()) {
        throw new WalnutException("Number of inputs of word automata " + automatonName + " does not match number of inputs specified.");
      }
      M.setLabel(label);
      subautomata.add(M);
    }
    Automaton N = subautomata.remove(0);
    N = join(N, new LinkedList<>(subautomata));

    N.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), joinName, isDFAO);
    return new TestCase(N);
  }

  /**
   * @param automaton
   * @param subautomata A queue of automaton which we will "join" with the current automaton.
   * @return The cross product of the current automaton and automaton in subautomata, using the operation "first" on the outputs.
   * For sake of example, the current Automaton is M1, and subautomata consists of M2 and M3.
   * Then on input x, returned automaton should output the first non-zero value of [ M1(x), M2(x), M3(x) ].
   */
  public static Automaton join(Automaton automaton, Queue<Automaton> subautomata) {
      Automaton first = automaton.clone();

      while (!subautomata.isEmpty()) {
          Automaton next = subautomata.remove();
          long timeBefore = System.currentTimeMillis();
          Logging.logMessage(COMPUTING + " =>:" + first.fa.getQ() + " states - " + next.fa.getQ() + " states");
          Logging.indent();

          // crossProduct requires both automata to be totalized, otherwise it has no idea which cartesian states to transition to
          first.fa.totalize();
          next.fa.totalize();
          first = ProductStrategies.crossProduct(first, next, Prover.FIRST_OP);
          first = WordAutomaton.minimizeWithOutput(first);

          Logging.dedent();
          long timeAfter = System.currentTimeMillis();
          Logging.logMessage(COMPUTED + " =>:" + first.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
      }
      return first;
  }
}
