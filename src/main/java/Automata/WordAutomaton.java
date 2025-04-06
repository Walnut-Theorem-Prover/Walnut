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
      Automaton wordAutomaton, int o, RelationalOperator.Ops operator, boolean print, String prefix, StringBuilder log) {
      String opStr = operator.getSymbol();
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "comparing (" + opStr + ") against " + o + ":" + wordAutomaton.fa.getQ() + " states", log);
      for (int p = 0; p < wordAutomaton.fa.getQ(); p++) {
          wordAutomaton.fa.setOutputIfEqual(p, RelationalOperator.compare(operator, wordAutomaton.fa.getO().getInt(p), o));
      }
      // As of now, this is *not* a word automaton
      wordAutomaton.determinizeAndMinimize(print, prefix + " ", log);
      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "compared (" + opStr + ") against " + o + ":" + wordAutomaton.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  /**
   * Reverse a DFAO. Use Theorem 4.3.3 from Allouche & Shallit.
   */
  public static void reverseWithOutput(Automaton wordAutomaton, boolean reverseMsd,
                                       boolean print, String prefix, StringBuilder log) {
      if (wordAutomaton.fa.isTRUE_FALSE_AUTOMATON()) {
          return;
      }

      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "reversing: " + wordAutomaton.fa.getQ() + " states", log);

      boolean addedDeadState = wordAutomaton.fa.addDistinguishedDeadState(print, prefix, log);

      int minOutput = 0;
      if (addedDeadState) {
          // get state with smallest output. all states with this output will be removed.
          // after transducing, all states with this minimum output will be removed.
          minOutput = wordAutomaton.fa.determineMinOutput();
      }

      // need to define states, an initial state, transitions, and outputs.
      Map<Integer, Integer> newInitState = new HashMap<>();
      for (int i = 0; i < wordAutomaton.fa.getQ(); i++) {
          newInitState.put(i, wordAutomaton.fa.getO().getInt(i));
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
          newO.add((int) currState.get(wordAutomaton.fa.getQ0()));

          newD.add(new Int2ObjectRBTreeMap<>());

          if (wordAutomaton.fa.getNfaD().get(wordAutomaton.fa.getQ0()).keySet().size() != wordAutomaton.getAlphabetSize()) {
              throw new WalnutException("Automaton should be deterministic!");
          }
          for (int l : wordAutomaton.fa.getNfaD().get(wordAutomaton.fa.getQ0()).keySet()) {
              Map<Integer, Integer> toState = new HashMap<>();

              for (int i = 0; i < wordAutomaton.fa.getQ(); i++) {
                  toState.put(i, currState.get(wordAutomaton.fa.getNfaD().get(i).get(l).getInt(0)));
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

      wordAutomaton.fa.setFields(newStates.size(), newO, newD);

      if (reverseMsd) {
          NumberSystem.flipNS(wordAutomaton.getNS());
      }

      minimizeSelfWithOutput(wordAutomaton, print, prefix + " ", log);

      if (addedDeadState) {
          AutomatonLogicalOps.removeStatesWithMinOutput(wordAutomaton, minOutput);
      }

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "reversed: " + wordAutomaton.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  /**
   * @param outputs A list of integers, indicating which uncombined automata and in what order to return.
   * @return A list of non-word automata, each corresponding to the list of outputs.
   * For the sake of an example, suppose that outputs is [0,1,2], then we return the list of automaton without output
   * which accepts if the output in our automaton is 0,1 or 2 respectively.
   */
  public static List<Automaton> uncombine(Automaton wordAutomaton, List<Integer> outputs) {
      List<Automaton> automata = new ArrayList<>(outputs.size());
      for (Integer output : outputs) {
          Automaton M = wordAutomaton.clone();
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
  public static Automaton minimizeWithOutput(Automaton wordAutomaton, boolean print, String prefix, StringBuilder log) {
      IntList outputs = new IntArrayList(wordAutomaton.fa.getO());
      UtilityMethods.removeDuplicates(outputs);
      List<Automaton> subautomata = uncombine(wordAutomaton, outputs);
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

  public static void minimizeSelfWithOutput(Automaton wordAutomaton, boolean print, String prefix, StringBuilder log) {
      Automaton N = minimizeWithOutput(wordAutomaton, print, prefix, log);
      wordAutomaton.copy(N);
  }

  /**
   * The operator can be one of "+" "-" "/" "*".
   * For example if operator = "+" then this method returns
   * a DFAO that outputs o+this[x] (or this[x]+p) on input x.
   * To be used only when this automaton and M are DFAOs (words).
   */
  public static void applyWordArithOperator(Automaton wordAutomaton, int o, ArithmeticOperator.Ops op, boolean reverse,
                                            boolean print, String prefix, StringBuilder log) {
      String opStr = op.getSymbol();
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "applying operator (" + opStr + "):" + wordAutomaton.fa.getQ() + " states", log);
      for (int p = 0; p < wordAutomaton.fa.getQ(); p++) {
          IntList thisO = wordAutomaton.fa.getO();
          int thisP = thisO.getInt(p);
          thisO.set(p,
              reverse ? ArithmeticOperator.arith(op, thisP, o) : ArithmeticOperator.arith(op, o, thisP));
      }
      minimizeSelfWithOutput(wordAutomaton, print, prefix + " ", log);
      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "applied operator (" + opStr + "):" + wordAutomaton.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }
}
