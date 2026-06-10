package Automata;

import Automata.FA.ProductStrategies;
import Main.EvalComputations.Token.ArithmeticOperator;
import Main.EvalComputations.Token.RelationalOperator;
import Main.Logging;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.*;

import static Main.Logging.*;

/**
 * This class handles various DFAO operations.
 * Eventually this should be strong-typed.
 */
public class WordAutomaton {
  /**
   * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
   * For example if operator = "<" then this method changes the word A
   * to a DFA that accepts x iff this[x] < o lexicographically.
   * To be used only when A is a DFAO (word).
   */
  public static void compareWordAutomaton(Automaton wordA, int o, RelationalOperator.Ops operator) {
      String opStr = operator.getSymbol();
      long timeBefore = System.currentTimeMillis();
      logMessage(COMPARING + " (" + opStr + ") against " + o + ":" + wordA.fa.getQ() + " states");
      Logging.indent();
      IntList wordAOutput = wordA.getFa().getO();
      for (int p = 0; p < wordA.fa.getQ(); p++) {
          wordA.fa.setOutputIfEqual(p, RelationalOperator.compare(operator, wordAOutput.getInt(p), o));
      }
      // As of now, this is *not* a word automaton
      wordA.determinizeAndMinimize();
      long timeAfter = System.currentTimeMillis();
      Logging.dedent();
      logMessage(COMPARED + " (" + opStr + ") against " + o + ":" + wordA.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
  }

    /**
     * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
     * For example if operator = "<" then this method returns
     * a DFA that accepts x iff this[x] < W[x] lexicographically.
     * To be used only when A and B are DFAOs (words).
     */
    public static Automaton compareWordAutomata(Automaton wordA, Automaton wordB, String operator) {
        long timeBefore = System.currentTimeMillis();
        logMessage(COMPARING + " (" + operator + "):"
                + wordA.fa.getQ() + " states - "+ wordB.fa.getQ() + " states");
        Logging.indent();
        Automaton M = ProductStrategies.crossProductAndMinimize(wordA, wordB, operator);
        Logging.dedent();
        long timeAfter = System.currentTimeMillis();
        logMessage(COMPARED + " (" + operator + "):"
                + M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        return M;
    }

    /**
     * The operator can be one of "+" "-" "/" "*".
     * For example if operator = "+" then this method returns
     * a DFAO that outputs o+this[x] (or this[x]+p) on input x.
     * To be used only when this automaton and M are DFAOs (words).
     */
    public static void applyWordArithOperator(Automaton wordA, int o, ArithmeticOperator.Ops op, boolean reverse) {
        String opStr = op.getSymbol();
        long timeBefore = System.currentTimeMillis();
        logMessage(APPLYING + " operator (" + opStr + "):" + wordA.fa.getQ() + " states");
        Logging.indent();
        for (int p = 0; p < wordA.fa.getQ(); p++) {
            IntList thisO = wordA.fa.getO();
            int thisP = thisO.getInt(p);
            thisO.set(p,
                reverse ? ArithmeticOperator.arith(op, thisP, o) : ArithmeticOperator.arith(op, o, thisP));
        }
        minimizeSelfWithOutput(wordA);
        long timeAfter = System.currentTimeMillis();
        Logging.dedent();
        logMessage(APPLIED + " operator (" + opStr + "):" + wordA.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
    }

    /**
     * The operator can be one of "+" "-" "*" "/".
     * For example if operator = "+" then this method returns a DFAO that outputs this[x] + B[x] on input x.
     * To be used only when this A and M are DFAOs (words).
     */
    public static Automaton applyWordOperator(Automaton wordA, Automaton wordB, String operator) {
        long timeBefore = System.currentTimeMillis();
        logMessage(APPLYING + " operator (" + operator + "):"
            + wordA.fa.getQ() + " states - " + wordB.fa.getQ() + " states");
        Logging.indent();
        Automaton N = ProductStrategies.crossProduct(wordA, wordB, operator);
        N = minimizeWithOutput(N);
        long timeAfter = System.currentTimeMillis();
        Logging.dedent();
        logMessage(APPLIED + " operator (" + operator + "):"
            + N.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
        return N;
    }

  /**
   * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
   * The returned object is deterministic.
   */
  public static void reverseWithOutput(Automaton wordA, boolean reverseMsd) {
      if (wordA.fa.isTRUE_FALSE_AUTOMATON()) {
          return;
      }

      long timeBefore = System.currentTimeMillis();
      logMessage(REVERSING + ": " + wordA.fa.getQ() + " states");
      Logging.indent();
      boolean addedDeadState = wordA.fa.addDistinguishedDeadState();

      int minOutput = 0;
      if (addedDeadState) {
          // get state with smallest output. all states with this output will be removed.
          // after transducing, all states with this minimum output will be removed.
          minOutput = wordA.fa.determineMinOutput();
      }

      // need to define states, an initial state, transitions, and outputs.
      Map<Integer, Integer> newInitState = new HashMap<>();
      for (int i = 0; i < wordA.fa.getQ(); i++) {
          newInitState.put(i, wordA.fa.getO().getInt(i));
      }

      IntList newO = new IntArrayList();
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>();

      List<Map<Integer, Integer>> newStates = new ArrayList<>();
      newStates.add(newInitState);

      Map<Map<Integer, Integer>, Integer> newStatesHash = new HashMap<>();
      newStatesHash.put(newInitState, newStates.size() - 1);

      Queue<Map<Integer, Integer>> newStatesQueue = new LinkedList<>();
      newStatesQueue.add(newInitState);

      while (!newStatesQueue.isEmpty()) {
          Map<Integer, Integer> currState = newStatesQueue.remove();

          // set up the output of this state to be g(q0), where g = currState.
          newO.add((int) currState.get(wordA.fa.getQ0()));

          newD.add(new Int2ObjectRBTreeMap<>());

          if (wordA.fa.getT().getNfaStateKeySet(wordA.fa.getQ0()).size() != wordA.getAlphabetSize()) {
              throw new WalnutException("Automaton should be deterministic!");
          }
          for (int l : wordA.fa.getT().getNfaStateKeySet(wordA.fa.getQ0())) {
              Map<Integer, Integer> toState = new HashMap<>();

              for (int i = 0; i < wordA.fa.getQ(); i++) {
                  toState.put(i, currState.get(wordA.fa.getT().getNfaStateDests(i, l).getInt(0)));
              }

              if (!newStatesHash.containsKey(toState)) {
                  newStates.add(toState);
                  newStatesQueue.add(toState);
                  newStatesHash.put(toState, newStates.size() - 1);
              }

              // set up the transition.
              IntList newList = new IntArrayList();
              newList.add((int) newStatesHash.get(toState));
              newD.get(newD.size() - 1).put(l, newList);
          }
      }

      wordA.fa.setFields(newStates.size(), newO, newD);

      if (reverseMsd) {
          NumberSystem.flipNS(wordA.getNS());
      }

      minimizeSelfWithOutput(wordA);

      if (addedDeadState) {
          // note: wordA is deterministic
          AutomatonLogicalOps.removeStatesWithOutputRebuild(wordA.fa, minOutput);
          wordA.forceCanonize();
      }

      long timeAfter = System.currentTimeMillis();
      Logging.dedent();
      logMessage(REVERSED + ": " + wordA.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
  }

  /**
   * @param outputs A list of integers, indicating which uncombined automata and in what order to return.
   * @return A list of non-word automata, each corresponding to the list of outputs.
   * For the sake of an example, suppose that outputs is [0,1,2], then we return the list of automaton without output
   * which accepts if the output in our automaton is 0,1 or 2 respectively.
   */
  public static List<Automaton> uncombine(Automaton wordA, List<Integer> outputs) {
      List<Automaton> automata = new ArrayList<>(outputs.size());
      for (Integer output : outputs) {
          Automaton M = wordA.clone();
          M.fa.setOutputIfEqual(output);
          // M is *not* a word automaton
          automata.add(M);
      }
      return automata;
  }

  /**
   * @return A minimized DFA with output recognizing the same language as the current DFA (possibly also with output).
   * We minimize a DFA with output by first uncombining into automata without output, minimizing the uncombined automata, and
   * then recombining. It follows that if the uncombined automata are minimal, then the combined automata is also minimal
   */
  public static Automaton minimizeWithOutput(Automaton wordA) {
      IntList outputs = new IntArrayList(wordA.fa.getO());
      UtilityMethods.removeDuplicates(outputs);
      List<Automaton> subautomata = uncombine(wordA, outputs);
      for (Automaton subautomaton : subautomata) {
          // These are *not* word automata
          subautomaton.determinizeAndMinimize();
      }
      Automaton N = subautomata.remove(0);
      List<String> label = new ArrayList<>(N.getLabel()); // We keep the old labels, since they are replaced in the combine
      N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs);
      N.setLabel(label);
      return N;
  }

  public static void minimizeSelfWithOutput(Automaton wordA) {
      Automaton N = minimizeWithOutput(wordA);
      wordA.copy(N);
  }
}
