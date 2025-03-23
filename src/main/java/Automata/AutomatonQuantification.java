package Automata;

import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutomatonQuantification {
  public static void quantify(Automaton A, String labelToQuantify, boolean print, String prefix, StringBuilder log) {
      quantify(A, Set.of(labelToQuantify), print, prefix, log);
  }

  public static void quantify(Automaton A, List<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
      quantify(A, new HashSet<>(labelsToQuantify), print, prefix, log);
  }

  /**
       * This method computes the existential quantification of this A.
       * Takes a list of labels and performs the existential quantifier over
       * the inputs with labels in listOfLabelsToQuantify. It simply eliminates inputs in listOfLabelsToQuantify.
       * After the quantification is done, we address the issue of
       * leadingZeros or trailingZeros (depending on the value of leadingZeros), if all of the inputs
       * of the resulting A are of type arithmetic.
       * This is why we mandate that an input of type arithmetic must have 0 in its alphabet, also that
       * every number system must use 0 to denote its additive identity.
       *
       * @param labelsToQuantify must contain at least one element, and must be a subset of this.label.
       */
  public static void quantify(Automaton A, Set<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
      quantifyHelper(A, labelsToQuantify, print, prefix, log);
      if (A.fa.isTRUE_FALSE_AUTOMATON()) return;

      Boolean isMsd = NumberSystem.determineMsd(A.getNS());
      if (isMsd == null) return;
      if (isMsd)
          AutomatonLogicalOps.fixLeadingZerosProblem(A, print, prefix, log);
      else
          AutomatonLogicalOps.fixTrailingZerosProblem(A, print, prefix, log);
  }

  /**
   * This method is very similar to public void quantify(Set<String> listOfLabelsToQuantify,boolean leadingZeros)
   * with the exception that, this method does not deal with leading/trailing zeros problem.
   */
  private static void quantifyHelper(
      Automaton A, Set<String> labelsToQuantify, boolean print, String prefix, StringBuilder log) {
      if (labelsToQuantify.isEmpty() || A.getLabel() == null) {
          return;
      }

      for (String s : labelsToQuantify) {
          if (!A.getLabel().contains(s)) {
              throw new RuntimeException(
                      "Variable " + s + " in the list of quantified variables is not a free variable.");
          }
      }
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "quantifying:" + A.getQ() + " states", log);

      //If this is the case, then the quantified automaton is either the true or false automaton.
      //It is true if the language is not empty.
      if (labelsToQuantify.size() == A.getA().size()) {
          A.fa.setTRUE_AUTOMATON(!A.isEmpty());
          A.fa.setTRUE_FALSE_AUTOMATON(true);
          A.clear();
          return;
      }

      List<Integer> listOfInputsToQuantify = new ArrayList<>(labelsToQuantify.size());
      //extract the list of indices of inputs we would like to quantify
      for (String l : labelsToQuantify)
          listOfInputsToQuantify.add(A.getLabel().indexOf(l));
      List<List<Integer>> allInputs = new ArrayList<>(A.getAlphabetSize());
      for (int i = 0; i < A.getAlphabetSize(); i++)
          allInputs.add(A.richAlphabet.decode(i));
      //now we remove those indices in listOfInputsToQuantify from A,T,label, and allInputs
      UtilityMethods.removeIndices(A.richAlphabet.getA(), listOfInputsToQuantify);
      A.richAlphabet.setEncoder(null);
      A.determineAlphabetSize();
      UtilityMethods.removeIndices(A.getNS(), listOfInputsToQuantify);
      UtilityMethods.removeIndices(A.getLabel(), listOfInputsToQuantify);
      for (List<Integer> i : allInputs)
          UtilityMethods.removeIndices(i, listOfInputsToQuantify);
      //example: permutation[1] = 7 means that encoded old input 1 becomes encoded new input 7
      List<Integer> permutation = new ArrayList<>(allInputs.size());
      for (List<Integer> i : allInputs)
          permutation.add(A.richAlphabet.encode(i));

      int Q = A.getFa().getQ();
      List<Int2ObjectRBTreeMap<IntList>> newD = new ArrayList<>(Q);
      for (int q = 0; q < Q; q++) {
          Int2ObjectRBTreeMap<IntList> newMemDTransitionFunction = new Int2ObjectRBTreeMap<>();
          newD.add(newMemDTransitionFunction);
          for (Int2ObjectMap.Entry<IntList> transition : A.getFa().getEntriesNfaD(q)) {
              int mappedKey = permutation.get(transition.getIntKey());
              IntList existingTransitions = newMemDTransitionFunction.get(mappedKey);
              if (existingTransitions != null) {
                  addAllWithoutRepetition(existingTransitions, transition.getValue());
              } else {
                  newMemDTransitionFunction.put(mappedKey, new IntArrayList(transition.getValue()));
              }
          }
      }
      A.setD(newD);
      A.fa.determinizeAndMinimize(print, prefix + " ", log);
      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix + "quantified:" + A.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  /**
   * add elements of R that do not exist in L to L.
   * Also: keep order of previous elements of L and new elements (w.r.t. R).
   */
  private static <T> void addAllWithoutRepetition(List<T> L, List<T> R) {
    if (R == null || R.isEmpty()) return;
    R.stream().filter(x -> !L.contains(x)).forEach(L::add);
  }
}
