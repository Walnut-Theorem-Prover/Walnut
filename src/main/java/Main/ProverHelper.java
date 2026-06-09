package Main;

import Automata.*;
import Automata.FA.Infinite;
import Automata.Writer.AutomatonWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for Prover.
 */
public class ProverHelper {
  /**
   * Export automata to any supported format.
   */
  public static void exportAutomata(String s, String filename, String exportType, Automaton M, boolean isDFAO) {
    String exportTypeLower = exportType.toLowerCase();
    String predicate = s == null ? "" : s;
    String resultFile = Session.getAddressForResult() + filename;

    // currently only a few types are supported
    switch (exportTypeLower) {
      case Prover.BA_STRING -> AutomatonWriter.exportToBA(M.fa, resultFile + Prover.BA_EXTENSION, isDFAO);
      case Prover.GV_STRING -> {
        System.out.println("Writing to " + resultFile + Prover.GV_EXTENSION);
        AutomatonWriter.writeToGV(M, resultFile + Prover.GV_EXTENSION, predicate, isDFAO);
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

  static boolean infFromAddress(String address) {
    Automaton M = Automaton.readAutomatonFromFile(address);
    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeros(M, M.getLabel());
    return infFromAutomaton(address, M);
  }

  public static boolean infFromAutomaton(String automatonName, Automaton M) {
    String infReg = Infinite.infinite(M.fa, M.richAlphabet);
    System.out.println(!infReg.isEmpty() ?
        ("Automaton accepts infinite values, including regex:" + infReg) :
        "Automaton " + automatonName + " accepts finitely many values.");
    return !infReg.isEmpty();
  }

  public static String determineInLibrary(boolean isDFAO, String inFileName) {
    return isDFAO ?
        Session.getReadFileForWordsLibrary(inFileName) : Session.getReadFileForAutomataLibrary(inFileName);
  }

  public static String determineOutLibrary(boolean isDFAO) {
    return isDFAO ?
        Session.getWriteAddressForWordsLibrary() : Session.getWriteAddressForAutomataLibrary();
  }

}
