package Automata.FA;

import Main.WalnutException;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Historical Brics conversions, now used only for regexes and test classes.
 *
 */
public class BricsConverter {
  public static void convertFromBrics(FA fa, List<Integer> alphabet, String regularExpression) {
    // For example if alphabet = {2,4,1} then intersectingRegExp = [241]*
    StringBuilder internalRegEx = new StringBuilder();
    for (int x : alphabet) {
      if (x < 0 || x > 9) {
        throw new WalnutException("the input alphabet of an automaton generated from a regular expression must be a subset of {0,1,...,9}");
      }
      internalRegEx.append(x);
    }

    long timeBefore = System.currentTimeMillis();

    dk.brics.automaton.Automaton M = buildBricsAutomaton(regularExpression, internalRegEx);

    fa.setAlphabetSize(alphabet.size());

    convertBricsAutomatonToInternalRepresentation(fa, M, (t, a) -> {
      // We only care about characters '0' through '9'
      int digit = a - '0';
      return alphabet.contains(digit) ? alphabet.indexOf(digit) : -1;
    });

    long timeAfter = System.currentTimeMillis();
    System.out.println("Converted from brics:" + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
  }

  private static dk.brics.automaton.Automaton buildBricsAutomaton(String regularExpression, StringBuilder internalRegEx) {
    regularExpression = "(" + regularExpression + ")&" + "[" + internalRegEx + "]*";
    RegExp RE = new RegExp(regularExpression);
    dk.brics.automaton.Automaton M = RE.toAutomaton();
    M.minimize();
    return M;
  }

  public static void setFromBricsAutomaton(FA fa, int alphabetSize, String regularExpression) {
    validateBricsAlphabetSize(alphabetSize);

    StringBuilder internalRegEx = new StringBuilder();
    for (int x = 0; x < alphabetSize; x++) {
      char nextChar = (char) (128 + x);
      internalRegEx.append(nextChar);
    }

    long timeBefore = System.currentTimeMillis();

    dk.brics.automaton.Automaton M = buildBricsAutomaton(regularExpression, internalRegEx);

    // We use packagedk.brics.automaton for automata minimization.
    if (!M.isDeterministic())
      throw WalnutException.bricsNFA();

    // Here, each character 'a' is used directly as the key.
    convertBricsAutomatonToInternalRepresentation(fa, M, (t, a) -> (int) a);

    // We added 128 to the encoding of every input vector before to avoid reserved characters, now we subtract it again
    // to get back the standard encoding
    fa.getT().setNfaD(addOffsetToInputs(fa, -128));

    long timeAfter = System.currentTimeMillis();
    System.out.println("Set from brics:" + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
  }

  private static List<Int2ObjectRBTreeMap<IntList>> addOffsetToInputs(FA fa, int offset) {
    List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(fa.getQ());
    for (int q = 0; q < fa.getQ(); q++) {
      Int2ObjectRBTreeMap<IntList> iMap = new Int2ObjectRBTreeMap<>();
      newD.add(iMap);
      for (Int2ObjectMap.Entry<IntList> entry : fa.getT().getEntriesNfaD(q)) {
        iMap.put(entry.getIntKey() + offset, entry.getValue());
      }
    }
    return newD;
  }

  private static void convertBricsAutomatonToInternalRepresentation(
      FA fa, dk.brics.automaton.Automaton M, BiFunction<Transition, Character, Integer> keyMapper) {
    List<State> setOfStates = new ArrayList<>(M.getStates());
    int Q = setOfStates.size();
    fa.setQ(Q);
    fa.setQ0(setOfStates.indexOf(M.getInitialState()));
    fa.initO(Q);
    fa.getT().setNfaD(new ArrayList<>(Q));
    for (int q = 0; q < Q; q++) {
      State state = setOfStates.get(q);
      fa.addOutput(state.isAccept());
      fa.getT().addMapToNfaD();
      for (Transition t : state.getTransitions()) {
        for (char a = t.getMin(); a <= t.getMax(); a++) {
          int key = keyMapper.apply(t, a);
          if (key != -1) {
            FA.addTransition(fa.getT().getNfaD(), q, key, setOfStates.indexOf(t.getDest()));
          }
        }
      }
    }
  }

  /**
   * Transform this automaton from Automaton to dk.brics.automaton.Automaton. This automaton can be
   * any automaton (deterministic/non-deterministic and with output/without output).
   */
  public static dk.brics.automaton.Automaton toDkBricsAutomaton(FA fa) {
    validateBricsAlphabetSize(fa.getAlphabetSize());
    boolean deterministic = true;
    List<State> setOfStates = new ArrayList<>(fa.getQ());
    for (int q = 0; q < fa.getQ(); q++) {
      setOfStates.add(new State());
      if (fa.isAccepting(q)) setOfStates.get(q).setAccept(true);
    }
    State initialState = setOfStates.get(fa.getQ0());
    for (int q = 0; q < fa.getQ(); q++) {
      for (Int2ObjectMap.Entry<IntList> entry : fa.getT().getEntriesNfaD(q)) {
        for (int dest : entry.getValue()) {
          setOfStates.get(q).addTransition(new Transition((char) entry.getIntKey(), setOfStates.get(dest)));
        }
        if (entry.getValue().size() > 1) deterministic = false;
      }
    }
    dk.brics.automaton.Automaton M = new dk.brics.automaton.Automaton();
    M.setInitialState(initialState);
    M.restoreInvariant();
    M.setDeterministic(deterministic);
    return M;
  }

  /**
   * Since the dk.brics.automaton uses char as its input alphabet for an automaton, then in order to transform
   * Automata.Automaton to dk.brics.automaton.Automata we ensure the input alphabet is no more than
   * the size of char, which is 2^16 - 1.
   * TODO: apparently there is some way to use UTF-16 in brics, but it's not obvious.
   */
  private static void validateBricsAlphabetSize(int alphabetSize) {
    final int MAX_BRICS_CHARACTER = (1 << Character.SIZE) - 1;
    if (alphabetSize > MAX_BRICS_CHARACTER) {
      throw WalnutException.alphabetExceedsSize(MAX_BRICS_CHARACTER);
    }
  }

  public static String convertEncodingForBrics(int vectorEncoding) {
    // dk.brics regex has several reserved characters - we cannot use these or the method that generates the automaton will
    // not be able to parse the string properly. All of these reserved characters have UTF-16 values between 0 and 127, so offsetting
    // our encoding by 128 will be enough to ensure that we have no conflicts
    vectorEncoding += 128;
    char replacement = (char) vectorEncoding;
    return Character.toString(replacement);
  }
}
