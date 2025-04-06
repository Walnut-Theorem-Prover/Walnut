package Automata;

import Main.EvalComputations.Token.ArithmeticOperator;
import Main.EvalComputations.Token.RelationalOperator;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.*;

/**
 * This class handles various NFAO and DFAO operations.
 * Eventually this should be strong-typed.
 */
public class WordAutomaton {
  /**
   * The operator can be one of "<" ">" "=" "!=" "<=" ">=".
   * For example if operator = "<" then this method changes the word A
   * to a DFA that accepts x iff this[x] < o lexicographically.
   * To be used only when this A is a DFAO (word).
   */
  public static void compareWordAutomaton(
      Automaton A, int o, RelationalOperator.Ops operator, boolean print, String prefix, StringBuilder log) {
      String opStr = operator.getSymbol();
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "comparing (" + opStr + ") against " + o + ":" + A.fa.getQ() + " states", log);
      for (int p = 0; p < A.fa.getQ(); p++) {
          A.fa.setOutputIfEqual(p, RelationalOperator.compare(operator, A.fa.getO().getInt(p), o));
      }
      // As of now, A is *not* a word automaton
      A.determinizeAndMinimize(print, prefix + " ", log);
      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "compared (" + opStr + ") against " + o + ":" + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  /**
   * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
   */
  public static void reverseWithOutput(Automaton A, boolean reverseMsd, boolean print, String prefix, StringBuilder log) {
      if (A.fa.isTRUE_FALSE_AUTOMATON()) {
          return;
      }

      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "reversing: " + A.fa.getQ() + " states", log);

      boolean addedDeadState = A.fa.addDistinguishedDeadState(print, prefix, log);

      int minOutput = 0;
      if (addedDeadState) {
          // get state with smallest output. all states with this output will be removed.
          // after transducing, all states with this minimum output will be removed.
          minOutput = A.fa.determineMinOutput();
      }

      // need to define states, an initial state, transitions, and outputs.
      Map<Integer, Integer> newInitState = new HashMap<>();
      for (int i = 0; i < A.fa.getQ(); i++) {
          newInitState.put(i, A.fa.getO().getInt(i));
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
          newO.add((int) currState.get(A.fa.getQ0()));

          newD.add(new Int2ObjectRBTreeMap<>());

          if (A.fa.getNfaD().get(A.fa.getQ0()).keySet().size() != A.getAlphabetSize()) {
              throw new WalnutException("Automaton should be deterministic!");
          }
          for (int l : A.fa.getNfaD().get(A.fa.getQ0()).keySet()) {
              Map<Integer, Integer> toState = new HashMap<>();

              for (int i = 0; i < A.fa.getQ(); i++) {
                  toState.put(i, currState.get(A.fa.getNfaD().get(i).get(l).getInt(0)));
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

      A.fa.setFields(newStates.size(), newO, newD);

      if (reverseMsd) {
          NumberSystem.flipNS(A.getNS());
      }

      minimizeSelfWithOutput(A, print, prefix + " ", log);

      if (addedDeadState) {
          AutomatonLogicalOps.removeStatesWithMinOutput(A, minOutput);
      }

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "reversed: " + A.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  /**
   * @param B
   * @param outputs A list of integers, indicating which uncombined automata and in what order to return.
   * @return A list of automata, each corresponding to the list of outputs.
   * For the sake of an example, suppose that outputs is [0,1,2], then we return the list of automaton without output
   * which accepts if the output in our automaton is 0,1 or 2 respectively.
   */
  public static List<Automaton> uncombine(Automaton B, List<Integer> outputs) {
      List<Automaton> automata = new ArrayList<>(outputs.size());
      for (Integer output : outputs) {
          Automaton M = B.clone();
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
  public static Automaton minimizeWithOutput(Automaton B, boolean print, String prefix, StringBuilder log) {
      IntList outputs = new IntArrayList(B.fa.getO());
      UtilityMethods.removeDuplicates(outputs);
      List<Automaton> subautomata = uncombine(B, outputs);
      for (Automaton subautomaton : subautomata) {
          // These are *not* word automata
          subautomaton.determinizeAndMinimize(print, prefix, log);
      }
      Automaton N = subautomata.remove(0);
      List<String> label = new ArrayList<>(N.getLabel()); // We keep the old labels, since they are replaced in the combine
      N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, print, prefix, log);
      N.setLabel(label);
      return N;
  }

  public static void minimizeSelfWithOutput(Automaton B, boolean print, String prefix, StringBuilder log) {
      Automaton N = minimizeWithOutput(B, print, prefix, log);
      B.copy(N);
  }

  /**
   * The operator can be one of "+" "-" "/" "*".
   * For example if operator = "+" then this method returns
   * a DFAO that outputs o+this[x] (or this[x]+p) on input x.
   * To be used only when this automaton and M are DFAOs (words).
   */
  public static void applyWordArithOperator(Automaton B, int o, ArithmeticOperator.Ops op, boolean reverse, boolean print, String prefix, StringBuilder log) {
      String opStr = op.getSymbol();
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "applying operator (" + opStr + "):" + B.fa.getQ() + " states", log);
      for (int p = 0; p < B.fa.getQ(); p++) {
          IntList thisO = B.fa.getO();
          int thisP = thisO.getInt(p);
          thisO.set(p,
              reverse ? ArithmeticOperator.arith(op, thisP, o) : ArithmeticOperator.arith(op, o, thisP));
      }
      minimizeSelfWithOutput(B, print, prefix + " ", log);
      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "applied operator (" + opStr + "):" + B.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }
}
