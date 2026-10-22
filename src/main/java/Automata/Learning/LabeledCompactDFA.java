package Automata.Learning;

import Automata.Automaton;
import Automata.NumberSystem;
import Automata.RichAlphabet;
import Main.WalnutException;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.word.Word;

import java.math.BigInteger;
import java.util.*;

/** A compact DFA with Walnut track metadata; a null DFA denotes a hypothesis placeholder. */
public class LabeledCompactDFA {
  public CompactDFA<Integer> cDFA;
  public final List<String> labels;
  public RichAlphabet richAlphabet;
  public List<NumberSystem> NS;
  public int radix;
  public final boolean wantAccept; // Clause acceptance polarity
  public final Automaton originalAutomaton; // Preserves DFAO outputs

  /** Canonicalizes the source in place and preserves it for callers that need DFAO outputs. */
  public LabeledCompactDFA(Automaton A) {
    this(A, true);
  }

  /**
   * Canonicalizes and compacts the source, optionally retaining the Walnut representation.
   * Fixed-hypothesis verification does not need the original FA after conversion, while
   * DFAO-based membership oracles do need it for state outputs.
   */
  public LabeledCompactDFA(Automaton A, boolean retainOriginalAutomaton) {
    A.canonize(); // aside from other things, this sorts the labels alphabetically
    this.originalAutomaton = retainOriginalAutomaton ? A : null;
    this.cDFA = A.fa.FAtoCompactDFA();
    this.labels = A.getLabel();
    this.NS = A.getNS();
    this.richAlphabet = A.richAlphabet;
    if (A.richAlphabet.getEncoder() == null) {
      A.richAlphabet.setupEncoder();
    }
    this.radix = richAlphabet.getA().get(0).size();
    this.wantAccept = true; // We only allow custom negation of the hypothesis
  }

  /** Records a hypothesis occurrence, keeping its argument order and acceptance polarity. */
  public LabeledCompactDFA(List<String> labels, boolean wantAccept) {
    ensureDistinctLabels(labels);
    this.labels = labels;
    this.wantAccept = wantAccept;
    this.originalAutomaton = null;
  }

  /** Materializes a hypothesis DFA using the target alphabet and number-system schema. */
  public LabeledCompactDFA(
      CompactDFA<Integer> cDFA, List<String> labels, LabeledCompactDFA schema, boolean wantAccept) {
    this(labels, wantAccept);
    if (labels.size() != schema.richAlphabet.getA().size()) {
      throw new WalnutException(
          "Hypothesis arity " + labels.size() + " does not match target arity "
              + schema.richAlphabet.getA().size());
    }
    this.cDFA = cDFA;
    this.richAlphabet = schema.richAlphabet;
    this.NS = schema.NS;
    this.radix = richAlphabet.getA().get(0).size();
  }

  /** Rejects repeated arguments without reordering positional hypothesis labels. */
  private static void ensureDistinctLabels(List<String> labels) {
    Set<String> seen = new HashSet<>();
    for (String label : labels) {
      if (!seen.add(label)) {
        throw new WalnutException("Duplicate hypothesis label in " + labels);
      }
    }
  }

  /** Collects clause labels in their first-occurrence order. */
  public static List<String> unionLabels(List<LabeledCompactDFA> parts) {
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    for (LabeledCompactDFA part : parts) {
      labels.addAll(part.labels);
    }
    return new ArrayList<>(labels);
  }

  /**
   * Precomputes the projection from the witness alphabet to this DFA's alphabet.
   * Each witness symbol is restricted to this DFA's labels and re-encoded as a local symbol.
   *
   * @param witnessAlphabet combined alphabet of all witness labels
   * @param witnessLabels labels of the witness alphabet
   * @return map from each encoded witness symbol to the corresponding symbol for this DFA
   */
  public int[] compileSymbolMap(RichAlphabet witnessAlphabet, List<String> witnessLabels) {
    if (witnessAlphabet.getEncoder() == null) { witnessAlphabet.setupEncoder(); }
    if (richAlphabet.getEncoder() == null) { richAlphabet.setupEncoder(); }
    int[] witnessTrack = new int[labels.size()];
    int[][] digitIndices = new int[labels.size()][];
    for (int i = 0; i < labels.size(); i++) {
      witnessTrack[i] = witnessLabels.indexOf(labels.get(i));
      if (witnessTrack[i] < 0) {
        throw new WalnutException("Label " + labels.get(i) + " not found in witness labels " + witnessLabels);
      }
      List<Integer> witnessDigits = witnessAlphabet.getA().get(witnessTrack[i]);
      List<Integer> localDigits = richAlphabet.getA().get(i);
      digitIndices[i] = new int[witnessDigits.size()];
      for (int digit = 0; digit < witnessDigits.size(); digit++) {
        int localIndex = localDigits.indexOf(witnessDigits.get(digit));
        if (localIndex < 0) {
          throw new WalnutException(
              "Label " + labels.get(i) + " cannot project digit " + witnessDigits.get(digit)
                  + " into alphabet " + localDigits);
        }
        digitIndices[i][digit] = localIndex;
      }
    }

    int[] symbolMap = new int[witnessAlphabet.determineAlphabetSize()];
    for (int witnessSymbol = 0; witnessSymbol < symbolMap.length; witnessSymbol++) {
      int partSymbol = 0;
      for (int localTrack = 0; localTrack < labels.size(); localTrack++) {
        int track = witnessTrack[localTrack];
        int digitIndex = (witnessSymbol / witnessAlphabet.getEncoder().getInt(track))
            % witnessAlphabet.getA().get(track).size();
        partSymbol += digitIndices[localTrack][digitIndex] * richAlphabet.getEncoder().getInt(localTrack);
      }
      symbolMap[witnessSymbol] = partSymbol;
    }
    return symbolMap;
  }

  /** Shows positional values when possible, otherwise the actual digit words and number system. */
  public String formatDecodedWord(Word<Integer> input) {
    StringBuilder result = new StringBuilder();
    for (int track = 0; track < labels.size(); track++) {
      if (track > 0) result.append(' ');
      result.append(labels.get(track)).append('=');
      NumberSystem numberSystem = NS == null || track >= NS.size() ? null : NS.get(track);
      int base = 0;
      if (numberSystem != null) {
        try {
          base = numberSystem.parseBase();
        } catch (WalnutException ignored) {
          // Alphabet size is not a positional base for systems such as Fibonacci.
        }
      }

      BigInteger value = BigInteger.ZERO;
      StringJoiner digits = new StringJoiner(",", "[", "]");
      List<Integer> alphabet = richAlphabet.getA().get(track);
      int encoding = richAlphabet.getEncoder().getInt(track);
      for (int i = 0; i < input.length(); i++) {
        // Horner's rule reads LSD words backwards; raw digit words retain their input order.
        int position = base > 0 && !numberSystem.isMsd() ? input.length() - 1 - i : i;
        int digit = alphabet.get((input.getSymbol(position) / encoding) % alphabet.size());
        if (base > 0) {
          value = value.multiply(BigInteger.valueOf(base)).add(BigInteger.valueOf(digit));
        } else {
          digits.add(Integer.toString(digit));
        }
      }
      if (base > 0) {
        result.append(value);
      } else {
        result.append(digits);
        if (numberSystem != null) result.append(" (?").append(numberSystem.getName()).append(')');
      }
    }
    return result.toString();
  }
}
