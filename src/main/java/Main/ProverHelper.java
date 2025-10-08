package Main;

import Automata.*;
import Automata.FA.BricsConverter;
import Automata.FA.Infinite;
import Main.EvalComputations.Token.ArithmeticOperator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Main.TestCase.DEFAULT_TESTFILE;

/**
 * Helper class for Prover.
 */
public class ProverHelper {
  static NumberSystem getNumberSystem(String base, List<NumberSystem> numSys, int proverNumberSystem) {
    try {
      if (!Predicate.numberSystemHash.containsKey(base))
        Predicate.numberSystemHash.put(base, new NumberSystem(base));
      NumberSystem ns = Predicate.numberSystemHash.get(base);
      numSys.add(Predicate.numberSystemHash.get(base));
      return ns;
    } catch (RuntimeException e) {
      throw new WalnutException("number system " + base + " does not exist: char at " + proverNumberSystem + System.lineSeparator() + "\t:" + e.getMessage());
    }
  }

  /**
   * Export automata to any supported format.
   */
  public static void exportAutomata(String s, String filename, String exportType, Automaton M, boolean isDFAO) {
    String exportTypeLower = exportType.toLowerCase();
    String resultFile = Session.getAddressForResult() + filename;

    // currently only a few types are supported
    switch (exportTypeLower) {
      case Prover.BA_STRING -> AutomatonWriter.exportToBA(M.fa, resultFile + Prover.BA_EXTENSION, isDFAO);
      case Prover.GV_STRING -> {
        System.out.println("Writing to " + resultFile + Prover.GV_EXTENSION);
        AutomatonWriter.draw(M, resultFile + Prover.GV_EXTENSION, s, isDFAO);
      }
      case Prover.TXT_STRING ->
          throw new WalnutException("Exporting to " + Prover.TXT_EXTENSION + " is redundant; this is the input format");
      default -> throw WalnutException.unexpectedFormat(exportType);
    }
  }

  static Matcher matchOrFail(Pattern pattern, String input, String commandName) {
    Matcher m = pattern.matcher(input);
    if (!m.find()) {
      throw WalnutException.invalidCommandUse(commandName);
    }
    return m;
  }

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  static String determineEncodedRegex(String baseexp, int inputLength, RichAlphabet r) {
    Matcher m2 = Prover.PAT_FOR_AN_ALPHABET_VECTOR.matcher(baseexp);
    // if we haven't had to replace any input vectors with unicode, we use the legacy method of constructing the automaton
    StringBuilder sb = new StringBuilder();
    while (m2.find()) {
      String alphabetVector = m2.group();

      // needed to replace this string with the unicode mapping
      if (alphabetVector.charAt(0) == '[') {
        alphabetVector = alphabetVector.substring(1, alphabetVector.length() - 1); // truncate brackets [ ]
      }

      List<Integer> L = new ArrayList<>();
      Matcher m3 = Prover.PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(alphabetVector);
      while (m3.find()) {
        L.add(UtilityMethods.parseInt(m3.group()));
      }
      if (L.size() != inputLength) {
        throw new WalnutException("Mismatch between vector length in regex and specified number of inputs to automaton");
      }
      String replacementStr = BricsConverter.convertEncodingForBrics(r.encode(L));

      // replace exactly this match
      m2.appendReplacement(sb, Matcher.quoteReplacement(replacementStr));
    }
    m2.appendTail(sb);

    // We should always do this with replacement, since we may have regexes such as "...", which accepts any three characters
    // in a row, on an alphabet containing bracketed characters. We don't make any replacements here, but they are implicitly made
    // when we intersect with our alphabet(s).

    // remove all whitespace from regular expression.
    return sb.toString().replaceAll("\\s", "");
  }

  static TestCase describe(boolean isDFAO, String inFileName) {
    String inLibrary = determineInLibrary(isDFAO, inFileName);

    Automaton M = new Automaton(inLibrary);

    UtilityMethods.logMessage(true, "File location: " + inLibrary, Prover.log);
    String comments = AutomatonReader.readComments(inLibrary);
    UtilityMethods.logMessage(true, "Comments: " + comments, Prover.log);
    UtilityMethods.logMessage(true, "State count:" + M.fa.getQ(), Prover.log);
    UtilityMethods.logMessage(true, "Transition count:" + M.fa.t.determineTransitionCount(), Prover.log);
    UtilityMethods.logMessage(true, "Alphabet size:" + M.fa.getAlphabetSize(), Prover.log);
    UtilityMethods.logMessage(true, "Number systems:" + M.getNS(), Prover.log);

    return new TestCase(M, "", null, null, Prover.log.toString(),
        List.of(new TestCase.AutomatonFilenamePair(M, DEFAULT_TESTFILE)));
  }

  static void morphismCommand(String morphismDefinition, String name) throws IOException {
    Morphism M = new Morphism();
    M.parseMap(morphismDefinition);
    System.out.print("Defined with domain ");
    System.out.print(M.mapping.keySet());
    System.out.print(" and range ");
    System.out.print(M.range);
    M.write(Session.getAddressForResult() + name + Prover.TXT_EXTENSION);
    M.write(Session.getWriteAddressForMorphismLibrary() + name + Prover.TXT_EXTENSION);
  }

  static boolean infFromAddress(String address) {
    Automaton M = Automaton.readAutomatonFromFile(address);
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);
    String infReg = Infinite.infinite(M.fa, M.richAlphabet);
    System.out.println(!infReg.isEmpty() ? infReg : "Automaton " + address + " accepts finitely many values.");
    return !infReg.isEmpty();
  }

  static List<Integer> determineAlphabet(String s) {
    List<Integer> L = new ArrayList<>();
    s = s.substring(1, s.length() - 1); //truncation { and } from beginning and end
    Matcher m = Prover.PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(s);
    while (m.find()) {
      L.add(UtilityMethods.parseInt(m.group()));
    }
    UtilityMethods.removeDuplicates(L);

    return L;
  }

  static TestCase reverseCommand(String s, String inFileName, boolean isDFAO, String newName, boolean printFlag) {
    Automaton M = new Automaton(determineInLibrary(isDFAO, inFileName));
    if (isDFAO) {
      WordAutomaton.reverseWithOutput(M, true, printFlag, Prover.prefix, Prover.log);
    } else {
      AutomatonLogicalOps.reverse(M, printFlag, Prover.prefix, Prover.log, true);
    }
    M.writeAutomata(s, determineOutLibrary(isDFAO), newName, true);
    return new TestCase(M);
  }

  public static TestCase processSplitCommand(
      String s, boolean isReverse, String automatonName,
      String name, Matcher inputPattern, boolean printFlag) {

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

    subautomata.replaceAll(automaton -> automaton.processSplit(plusMinusInputs, isReverse, printFlag, Prover.prefix, Prover.log));

    Automaton N = subautomata.remove(0);
    N = AutomatonLogicalOps.combine(N, new LinkedList<>(subautomata), outputs, printFlag, Prover.prefix, Prover.log);

    N.writeAutomata(s, determineOutLibrary(isDFAO), name, isDFAO);
    return new TestCase(N);
  }

  static String determineInLibrary(boolean isDFAO, String inFileName) {
    return isDFAO ?
        Session.getReadFileForWordsLibrary(inFileName) : Session.getReadFileForAutomataLibrary(inFileName);
  }

  static String determineOutLibrary(boolean isDFAO) {
    return isDFAO ?
        Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary();
  }
}
