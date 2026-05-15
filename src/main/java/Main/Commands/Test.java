package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Main.Prover;
import Main.ProverHelper;
import Main.TestCase;
import Main.WalnutException;

import java.util.ArrayList;
import java.util.List;

public class Test {
  /**
   * We find the first (non-empty) n inputs accepted by our automaton in shortlex order.
   * If fewer than n inputs are accepted, we output all that are.
   * @param testName - automaton to test
   * @param needed - inputs needed
   * @return - whether the automaton accepts at least needed inputs
   */
  public static boolean testCommand(String testName, int needed) {
    Automaton M = Automaton.readAutomatonFromFile(testName);
    List<String> accepted = findAccepted(M, testName, needed);
    if (accepted.size() < needed) {
      System.out.println(testName + " only accepts " + accepted.size() + " inputs, which are as follows: ");
    }
    for (String input : accepted) {
      System.out.println(input);
    }
    return accepted.size() >= needed;
  }

  public static List<String> findAccepted(Automaton M, String testName, int needed) {
    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);

    boolean infinite = ProverHelper.infFromAutomaton(testName, M);

    // TODO - Call reg command directly, rather than through fake command
    StringBuilder incLengthReg = createFakeRegCommand(testName, M.richAlphabet.getA());

    StringBuilder dotReg = new StringBuilder();
    int searchLength = 0;
    List<String> accepted = new ArrayList<>();
    while (true) {
      searchLength++;
      dotReg.append(".");
      TestCase retrieval = Prover.regCommand(incLengthReg + "\"" + dotReg + "\";");
      if (retrieval.getAutomatonPairs().size() != 1) {
        throw new WalnutException("Unexpected retrieval output");
      }
      Automaton R = retrieval.getAutomatonPairs().get(0).automaton().clone();

      // and-ing automata uses the cross product routine, which requires labeled automata
      R.setLabel(M.getLabel());
      Automaton N = AutomatonLogicalOps.and(M, R, false, null, null);
      accepted.addAll(N.findAccepted(searchLength, needed - accepted.size()));
      if (accepted.size() >= needed) {
        break;
      }

      // If our automaton accepts finitely many inputs, it does not have a non-redundant cycle, and so the highest length input that could be
      // accepted is equal to the number of states in the automaton
      if (!(infinite) && (searchLength >= M.fa.getQ())) {
        break;
      }
    }
    return accepted;
  }

  private static StringBuilder createFakeRegCommand(String testName, List<List<Integer>> A) {
    StringBuilder incLengthReg = new StringBuilder();
    incLengthReg.append("reg ").append(testName).append("_len ");
    for (List<Integer> integers : A) {
      String alphaString = integers.toString();
      alphaString = alphaString.substring(1, alphaString.length() - 1);
      alphaString = "{" + alphaString + "} ";
      incLengthReg.append(alphaString);
    }
    return incLengthReg;
  }
}
