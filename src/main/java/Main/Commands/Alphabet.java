package Main.Commands;

import Automata.Automaton;
import Automata.NumberSystem;
import Main.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Alphabet {
  static final String RE_FOR_AN_ALPHABET = "((((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))|(\\{(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\}))\\s+";
  static final Pattern PAT_FOR_AN_ALPHABET = Pattern.compile(RE_FOR_AN_ALPHABET);

  public static TestCase alphabetCommand(String s, String listOfAlphabets, int nsStart, boolean isDFAO, String inFileName, String newName) {
    List<NumberSystem> NS = new ArrayList<>();
    List<List<Integer>> alphabets = new ArrayList<>();
    determineAlphabetsAndNS(listOfAlphabets, nsStart, NS, alphabets);

    Automaton M = new Automaton(ProverHelper.determineInLibrary(isDFAO, inFileName));

    // here, call the function to set the number system.
    M.setAlphabet(isDFAO, NS, alphabets);

    M.writeAutomata(s, ProverHelper.determineOutLibrary(isDFAO), newName, false);
    return new TestCase(M);
  }

  public static void determineAlphabetsAndNS(
      String listOfAlphabets, int nsStart, List<NumberSystem> NS, List<List<Integer>> alphabets) {
    Matcher m1 = PAT_FOR_AN_ALPHABET.matcher(listOfAlphabets);
    int counter = 1;
    while (m1.find()) {
      if ((m1.group(Prover.R_NUMBER_SYSTEM) != null)) {
        String base = Prover.determineBase(m1);
        NumberSystem ns = NumberSystem.getNumberSystem(base, NS, nsStart);
        alphabets.add(ns.getAlphabet());
      } else if (m1.group(Prover.R_SET) != null) {
        alphabets.add(determineAlphabet(m1.group(Prover.R_SET)));
        NS.add(null);
      } else {
        throw new WalnutException("Alphabet at position " + counter + " not recognized in alphabet command");
      }
      counter += 1;
    }
  }

  private static List<Integer> determineAlphabet(String s) {
    List<Integer> L = new ArrayList<>();
    s = s.substring(1, s.length() - 1); //truncation { and } from beginning and end
    Matcher m = Prover.PAT_FOR_A_SINGLE_ELEMENT_OF_A_SET.matcher(s);
    while (m.find()) {
      L.add(UtilityMethods.parseInt(m.group()));
    }
    UtilityMethods.removeDuplicates(L);

    return L;
  }
}
