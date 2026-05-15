package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.Search.ProductBFS;
import Main.ProverHelper;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.IntList;
import net.automatalib.word.Word;

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
    if (needed <= 0) {
      return new ArrayList<>();
    }

    // We do not want to count multiple representations of the same value as distinct accepted values.
    // This preserves the existing behavior that skips representations beginning with 0
    // (or [0,0], etc., for higher-arity numeric inputs).
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);

    // Keep the existing side effect: test also reports whether the language is finite or infinite.
    ProverHelper.infFromAutomaton(testName, M);

    List<String> accepted = new ArrayList<>(needed);
    Word<Integer> previous = null;

    while (accepted.size() < needed) {
      Word<Integer> nextWord = findNextAcceptedWord(M, previous);
      if (nextWord == null) {
        break;
      }

      accepted.add(formatAcceptedWord(M, nextWord));
      previous = nextWord;
    }

    return accepted;
  }

  /**
   * Finds the shortlex-smallest non-empty accepted encoded-symbol word strictly after previous.
   * Because previous is the last word emitted, every earlier emitted word is <= previous in
   * shortlex order. So we do not need to remember all emitted words; it is enough to reject
   * candidates that are not strictly after previous.
   * Product state layout:
   *   [0] state of the Walnut automaton M
   *   [1] min(length read so far, previous.length() + 1)
   *   [2] lexicographic comparison with previous while the current length is <= previous.length()
   */
  private static Word<Integer> findNextAcceptedWord(Automaton M, Word<Integer> previous) {
    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      if (M.fa.isTRUE_AUTOMATON()) {
        throw new WalnutException("Cannot enumerate accepted inputs of an unmaterialized true automaton.");
      }
      return null;
    }

    int[] start = { M.fa.getQ0(), 0, 0 };

    return ProductBFS.shortestWitnessWordInt(
        start,
        M.getAlphabetSize(),
        (state, symbol, out) -> {
          IntList destinations = M.fa.getT().getNfaStateDests(state[0], symbol);
          if (destinations == null || destinations.isEmpty()) {
            return false;
          }
          int oldLength = state[1];
          out[0] = destinations.getInt(0);
          out[1] = previous == null ? oldLength + 1 : Math.min(oldLength + 1, previous.length() + 1);
          out[2] = updateComparison(previous, oldLength, state[2], symbol);
          return true;
        },
        state -> state[1] != 0 && M.fa.isAccepting(state[0]) && isAfterPrevious(previous, state[1], state[2])
    );
  }

  private static int updateComparison(Word<Integer> previous, int position, int comparison, int symbol) {
    if (previous == null || comparison != 0 || position >= previous.length()) {
      return comparison;
    }
    return Integer.compare(symbol, previous.getSymbol(position));
  }

  private static boolean isAfterPrevious(Word<Integer> previous, int length, int comparison) {
    return previous == null || length > previous.length() || (length == previous.length() && comparison > 0);
  }

  /**
   * Keeps the same user-facing formatting as Automaton.findAcceptedHelper:
   * single-arity digits 0..9 are printed without brackets, while vector symbols remain bracketed.
   */
  private static String formatAcceptedWord(Automaton M, Word<Integer> word) {
    boolean singleArity = M.richAlphabet.getA().size() == 1;
    StringBuilder path = new StringBuilder();

    for (int i = 0; i < word.length(); i++) {
      List<Integer> decoded = M.richAlphabet.decode(word.getSymbol(i));
      String input = decoded.toString();

      if (singleArity && decoded.get(0) >= 0 && decoded.get(0) <= 9) {
        input = input.substring(1, input.length() - 1);
      }
      path.append(input);
    }
    return path.toString();
  }
}
