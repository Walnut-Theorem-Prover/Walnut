package Main.Commands;

import Automata.Automaton;
import Automata.NumberSystem;
import Automata.RichAlphabet;
import Automata.FA.ProductStrategies;
import Automata.Search.ProductBFS;
import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.Expression;
import Main.EvalComputations.Expressions.WordExpression;
import Main.WalnutException;
import net.automatalib.word.Word;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static Main.Prover.RE_EVAL_BINDING;

/** Finite value output for eval/def. */
final class EvalOutput {
  private EvalOutput() {}

  /** Parses finite assignments such as {@code n=1..10}. */
  static List<Binding> parseBindings(String input) {
    List<Binding> bindings = new ArrayList<>();
    Set<String> names = new LinkedHashSet<>();
    for (String token : input.strip().split("\\s+")) {
      if (!token.matches(RE_EVAL_BINDING)) throw new WalnutException("Invalid eval/def binding: " + token);
      String[] assignment = token.split("=", 2);
      if (!names.add(assignment[0])) throw new WalnutException("Duplicate eval/def binding: " + assignment[0]);
      String[] range = assignment[1].split("\\.\\.", 2);
      BigInteger start = new BigInteger(range[0]);
      BigInteger end = range.length == 1 ? start : new BigInteger(range[1]);
      bindings.add(new Binding(assignment[0], start, end));
    }
    return bindings;
  }

  /** Evaluates and formats an expression over finite bindings. */
  static String output(Expression result, List<Binding> bindings) {
    if (!(result instanceof AutomatonExpression) && !(result instanceof WordExpression)) {
      throw new WalnutException("Finite eval/def output requires an automaton or word result.");
    }

    List<String> boundNames = bindings.stream().map(Binding::name).toList();
    LinkedHashSet<String> unbound = new LinkedHashSet<>();
    if (result instanceof WordExpression word) {
      if (!word.wordAutomaton.fa.isTRUE_FALSE_AUTOMATON()) unbound.addAll(word.wordAutomaton.getLabel());
      if (!word.M.fa.isTRUE_FALSE_AUTOMATON()) unbound.addAll(word.M.getLabel());
      unbound.removeAll(word.identifiersToQuantify);
    } else if (!result.M.fa.isTRUE_FALSE_AUTOMATON()) {
      unbound.addAll(result.M.getLabel());
    }
    for (String name : boundNames) {
      if (!unbound.remove(name)) {
        throw new WalnutException("Eval/def binding " + name + " is not a free variable of the expression.");
      }
    }
    if (!unbound.isEmpty()) {
      throw new WalnutException("Missing eval/def binding for free variable " + unbound.iterator().next() + ".");
    }

    List<List<BigInteger>> coordinates = bindings.stream().map(Binding::values).toList();
    int cellCount = 1;
    for (List<BigInteger> values : coordinates) cellCount = Math.multiplyExact(cellCount, values.size());

    int[] values = new int[cellCount];
    int[] indices = new int[bindings.size()];
    for (int cell = 0; cell < cellCount; cell++) {
      decodeIndex(cell, coordinates, bindings.size(), indices);
      values[cell] = evaluate(result, bindings, coordinates, indices);
    }
    return values.length == 1 ? Integer.toString(values[0]) : format(bindings, coordinates, values);
  }

  record Binding(String name, BigInteger start, BigInteger end) {
    /** Enumerates this inclusive range. */
    List<BigInteger> values() {
      BigInteger count = end.subtract(start).abs().add(BigInteger.ONE);
      final int size;
      try {
        size = count.intValueExact();
      } catch (ArithmeticException e) {
        throw new WalnutException("Eval/def range " + name + "=" + start + ".." + end + " is too large.", e);
      }
      BigInteger step = end.compareTo(start) >= 0 ? BigInteger.ONE : BigInteger.ONE.negate();
      List<BigInteger> values = new ArrayList<>(size);
      for (BigInteger value = start; values.size() < size; value = value.add(step)) values.add(value);
      return values;
    }
  }

  /** Evaluates one coordinate of a Boolean or word result. */
  private static int evaluate(
      Expression result, List<Binding> bindings, List<List<BigInteger>> coordinates, int[] indices) {
    List<Automaton> constraints = new ArrayList<>();
    boolean possible = addConstraint(constraints, result.M);

    for (int i = 0; possible && i < bindings.size(); i++) {
      Binding binding = bindings.get(i);
      NumberSystem ns = numberSystemFor(result, binding.name());
      if (ns == null) throw new WalnutException("No number system for eval/def binding " + binding.name() + ".");
      Automaton constant = ns.getConstant(coordinates.get(i).get(indices[i]));
      if (!constant.fa.isTRUE_FALSE_AUTOMATON()) constant.bind(List.of(binding.name()));
      possible = addConstraint(constraints, constant);
    }

    if (!possible) {
      if (result instanceof WordExpression) throw new WalnutException("No word output is defined at this coordinate.");
      return 0;
    }

    Automaton outputAutomaton = result instanceof WordExpression word ? word.wordAutomaton : null;
    Witness witness = witness(constraints, outputAutomaton);
    if (witness.word() == null) {
      if (outputAutomaton != null) throw new WalnutException("No word output is defined at this coordinate.");
      return 0;
    }
    return outputAutomaton == null ? 1 : readOutput(outputAutomaton, witness);
  }

  /** Adds a nontrivial Boolean constraint and reports whether it is possible. */
  private static boolean addConstraint(List<Automaton> constraints, Automaton automaton) {
    if (automaton.fa.isTRUE_FALSE_AUTOMATON()) return automaton.fa.isTRUE_AUTOMATON();
    constraints.add(automaton);
    return true;
  }

  /** Finds the number system used by one free variable. */
  private static NumberSystem numberSystemFor(Expression result, String variable) {
    NumberSystem found = numberSystemFor(result.M, variable);
    if (result instanceof WordExpression word) {
      NumberSystem other = numberSystemFor(word.wordAutomaton, variable);
      if (found != null && other != null && !found.getName().equals(other.getName())) {
        throw new WalnutException("Variable " + variable + " uses incompatible number systems.");
      }
      if (found == null) found = other;
    }
    return found;
  }

  /** Finds a variable number system in one automaton. */
  private static NumberSystem numberSystemFor(Automaton automaton, String variable) {
    if (automaton.fa.isTRUE_FALSE_AUTOMATON()) return null;
    int index = automaton.getLabel().indexOf(variable);
    return index < 0 ? null : automaton.getNS().get(index);
  }

  /** Finds a common input representation satisfying all coordinate constraints. */
  private static Witness witness(List<Automaton> constraints, Automaton outputAutomaton) {
    List<Automaton> components = new ArrayList<>(constraints);
    if (outputAutomaton != null) components.add(outputAutomaton);
    LabeledAlphabet alphabet = unionAlphabet(components);
    int[][] symbolMaps = new int[components.size()][];
    int[] start = new int[components.size()];
    for (int i = 0; i < components.size(); i++) {
      Automaton component = components.get(i);
      start[i] = component.fa.getQ0();
      symbolMaps[i] = ProductStrategies.projectAlphabet(
          alphabet.labels(), alphabet.alphabet(), component.getLabel(), component.richAlphabet);
    }

    Word<Integer> word = ProductBFS.shortestWitnessWordInt(
        start, alphabet.alphabet().determineAlphabetSize(),
        (state, symbol, next) -> {
          for (int i = 0; i < components.size(); i++) {
            var destinations = components.get(i).fa.getT().getNfaStateDests(state[i], symbolMaps[i][symbol]);
            if (destinations == null || destinations.isEmpty()) return false;
            next[i] = destinations.getInt(0);
          }
          return true;
        },
        state -> {
          for (int i = 0; i < constraints.size(); i++) {
            if (!constraints.get(i).fa.isAccepting(state[i])) return false;
          }
          return true;
        });
    return new Witness(word, alphabet);
  }

  /** Runs a witness through a word automaton and returns its state output. */
  private static int readOutput(Automaton word, Witness witness) {
    int[] symbolMap = ProductStrategies.projectAlphabet(
        witness.alphabet().labels(), witness.alphabet().alphabet(), word.getLabel(), word.richAlphabet);
    int state = word.fa.getQ0();
    for (int symbol : witness.word()) {
      var destinations = word.fa.getT().getNfaStateDests(state, symbolMap[symbol]);
      if (destinations == null || destinations.isEmpty()) {
        throw new WalnutException("Word automaton has no transition for an eval/def witness.");
      }
      state = destinations.getInt(0);
    }
    return word.fa.getO().getInt(state);
  }

  /** Builds the labeled union alphabet used by the product search. */
  private static LabeledAlphabet unionAlphabet(List<Automaton> automata) {
    Map<String, List<Integer>> alphabetByLabel = new LinkedHashMap<>();
    for (Automaton automaton : automata) {
      if (automaton.fa.isTRUE_FALSE_AUTOMATON()) continue;
      for (int i = 0; i < automaton.getLabel().size(); i++) {
        String label = automaton.getLabel().get(i);
        List<Integer> alphabet = automaton.richAlphabet.getA().get(i);
        List<Integer> previous = alphabetByLabel.putIfAbsent(label, alphabet);
        if (previous != null && !previous.equals(alphabet)) {
          throw new WalnutException("Variable " + label + " has incompatible alphabets in eval/def output.");
        }
      }
    }
    RichAlphabet alphabet = new RichAlphabet();
    alphabet.setA(new ArrayList<>(alphabetByLabel.values()));
    alphabet.setupEncoder();
    return new LabeledAlphabet(new ArrayList<>(alphabetByLabel.keySet()), alphabet);
  }

  private record LabeledAlphabet(List<String> labels, RichAlphabet alphabet) {}
  private record Witness(Word<Integer> word, LabeledAlphabet alphabet) {}

  /** Formats one-dimensional rows or recursively indented higher-dimensional slices. */
  private static String format(List<Binding> bindings, List<List<BigInteger>> coordinates, int[] values) {
    if (bindings.size() == 1) return formatRow(bindings.get(0).name(), coordinates.get(0), values, 0, "");
    StringBuilder out = new StringBuilder();
    appendSlices(out, bindings, coordinates, values, 0, 0, "");
    return out.toString();
  }

  /** Appends recursively indented slices ending in one-dimensional rows. */
  private static void appendSlices(
      StringBuilder out, List<Binding> bindings, List<List<BigInteger>> coordinates,
      int[] values, int dimension, int offset, String indent) {
    int last = bindings.size() - 1;
    if (dimension == last) {
      if (!out.isEmpty()) out.append(System.lineSeparator());
      out.append(formatRow(bindings.get(last).name(), coordinates.get(last), values, offset, indent));
      return;
    }

    int stride = 1;
    for (int i = dimension + 1; i < coordinates.size(); i++) stride *= coordinates.get(i).size();
    for (int i = 0; i < coordinates.get(dimension).size(); i++) {
      if (!out.isEmpty()) out.append(System.lineSeparator());
      out.append(indent).append(bindings.get(dimension).name()).append('=')
          .append(coordinates.get(dimension).get(i)).append(':');
      appendSlices(out, bindings, coordinates, values, dimension + 1, offset + i * stride, indent + "  ");
    }
  }

  /** Formats one coordinate axis and its values. */
  private static String formatRow(
      String name, List<BigInteger> coordinates, int[] values, int offset, String indent) {
    String[] headers = coordinates.stream().map(BigInteger::toString).toArray(String[]::new);
    String[] cells = new String[headers.length];
    int[] widths = new int[headers.length];
    for (int i = 0; i < headers.length; i++) {
      cells[i] = Integer.toString(values[offset + i]);
      widths[i] = Math.max(headers[i].length(), cells[i].length());
    }
    String prefix = indent + name + ": ";
    return prefix + aligned(headers, widths) + System.lineSeparator()
        + " ".repeat(prefix.length()) + aligned(cells, widths);
  }

  /** Decodes a flat cell index into mixed-radix coordinate indices. */
  private static void decodeIndex(
      int flat, List<List<BigInteger>> coordinates, int dimensions, int[] indices) {
    for (int i = dimensions - 1; i >= 0; i--) {
      indices[i] = flat % coordinates.get(i).size();
      flat /= coordinates.get(i).size();
    }
  }

  /** Right-aligns a row to the supplied column widths. */
  private static String aligned(String[] values, int[] widths) {
    StringBuilder row = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) row.append(' ');
      row.append(" ".repeat(widths[i] - values[i].length())).append(values[i]);
    }
    return row.toString();
  }

}
