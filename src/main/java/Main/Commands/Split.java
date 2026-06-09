package Main.Commands;

import Automata.*;
import Main.*;
import Main.EvalComputations.Token.ArithmeticOperator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

public class Split {
  public static TestCase processSplitCommand(
      String s, boolean isReverse, String automatonName, String name, Matcher inputPattern) {

    String addressForWordAutomaton =
        Session.getReadFileForWordsLibrary(automatonName + Prover.TXT_EXTENSION);

    Automaton M;
    boolean isDFAO;
    if ((new File(addressForWordAutomaton)).isFile()) {
      M = new Automaton(addressForWordAutomaton);
      isDFAO = true;
    } else {
      String addressForAutomaton =
          Session.getReadFileForAutomataLibrary(automatonName + Prover.TXT_EXTENSION);
      if ((new File(addressForAutomaton)).isFile()) {
        M = new Automaton(addressForAutomaton);
        isDFAO = false;
      } else {
        throw new WalnutException("Automaton " + automatonName + " does not exist.");
      }
    }

    List<ArithmeticOperator.Ops> plusMinusInputs = new ArrayList<>();
    boolean hasInput = false;
    while (inputPattern.find()) {
      String t = inputPattern.group(1);
      ArithmeticOperator.Ops tOp = t.isEmpty() ? null : ArithmeticOperator.Ops.fromSymbol(t);
      if (tOp != null && tOp != ArithmeticOperator.Ops.PLUS && tOp != ArithmeticOperator.Ops.MINUS) {
        throw WalnutException.invalidCommand(t);
      }
      hasInput = hasInput || (tOp != null);
      plusMinusInputs.add(tOp);
    }
    if (!hasInput || plusMinusInputs.isEmpty()) {
      throw new WalnutException("Cannot split without inputs.");
    }

    IntList outputs = new IntArrayList(M.fa.getO());
    UtilityMethods.removeDuplicates(outputs);
    List<Automaton> subautomata = WordAutomaton.uncombine(M, outputs);

    subautomata.replaceAll(automaton -> processSplit(automaton, plusMinusInputs, isReverse));

    Automaton N = subautomata.remove(0);
    N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs);

    N.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), name, isDFAO);
    return new TestCase(N);
  }

  /**
   * Generalized method to handle split and reverse split operations on the automaton.
   *
   * @param automaton
   * @param inputs  A list of "+", "-" or null. Indicating how our input will be interpreted in the output automata.
   * @param reverse Whether to perform the reverse split operation.
   * @return The modified automaton after the split/reverse split operation.
   */
  public static Automaton processSplit(Automaton automaton, List<ArithmeticOperator.Ops> inputs, boolean reverse) {
      if (automaton.getAlphabetSize() == 0) {
          throw new WalnutException("Cannot process split automaton with no inputs.");
      }
      if (inputs.size() != automaton.richAlphabet.getA().size()) {
          throw new WalnutException("Split automaton has incorrect number of inputs.");
      }

      Automaton M = automaton.clone();
      Set<String> quantifiers = new HashSet<>();
      // Label M with [b0, b1, ..., b(A.size() - 1)]
      List<String> names = new ArrayList<>(automaton.richAlphabet.getA().size());
      for (int i = 0; i < automaton.richAlphabet.getA().size(); i++) {
          names.add("b" + i);
      }
      M.setLabel(names);

      for (int i = 0; i < inputs.size(); i++) {
          // input is "", "+", or "-"
          ArithmeticOperator.Ops input = inputs.get(i);
          if (input == null) {
              continue;
          }
          NumberSystem ns = automaton.getNS().get(i);
          if (ns == null)
              throw new WalnutException("Number system for input " + i + " must be defined.");
          NumberSystem negativeNumberSystem = ns.determineNegativeNS();

          Automaton baseChange = negativeNumberSystem.baseChange.clone();
          String a = "a" + i, b = "b" + i, c = "c" + i;

          if (input.equals(ArithmeticOperator.Ops.PLUS)) {
              baseChange.bind(reverse ? List.of(b, a) : List.of(a, b)); // Use ternary for binding logic
              M = AutomatonLogicalOps.and(M, baseChange);
              quantifiers.add(b);
          } else { // inputs.get(i).equals(BasicOp.MINUS)
              baseChange.bind(List.of(reverse ? b : a, c)); // Use ternary for binding logic
              M = AutomatonLogicalOps.and(M, baseChange);
              M = AutomatonLogicalOps.and(
                  M,
                  negativeNumberSystem.arithmetic(reverse ? a : b, c, 0, ArithmeticOperator.Ops.PLUS) // Use ternary for arithmetic logic
              );
              quantifiers.add(b);
              quantifiers.add(c);
          }
      }
      AutomatonQuantification.quantify(M, quantifiers);
      M.sortLabel();
      M.randomLabel();
      return M;
  }
}
