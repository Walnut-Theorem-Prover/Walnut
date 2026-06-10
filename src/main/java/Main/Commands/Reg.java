package Main.Commands;

import Automata.AutomatonDFA;
import Automata.FA.BricsConverter;
import Automata.NumberSystem;
import Automata.RichAlphabet;
import Main.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reg {
  private static final String RE_FOR_AN_ALPHABET_VECTOR = "(\\[(\\s*(\\+|\\-)?\\s*\\d+)(\\s*,\\s*(\\+|\\-)?\\s*\\d+)*\\s*\\])|(\\d)";
  private static final Pattern PAT_FOR_AN_ALPHABET_VECTOR = Pattern.compile(RE_FOR_AN_ALPHABET_VECTOR);

  public static TestCase reg(String listOfAlphabets, String baseexp, String regName) {
    List<List<Integer>> alphabets = new ArrayList<>();
    List<NumberSystem> NS = new ArrayList<>();
    Alphabet.determineAlphabetsAndNS(listOfAlphabets, NS, alphabets);
    // To support regular expressions with multiple arity (eg. "[1,0][0,1][0,0]*"), we must translate each of these vectors to an
    // encoding, which will then be turned into a unicode character that dk.brics can work with when constructing an automaton
    // from a regular expression. Since the encoding method is within the Automaton class, we create a dummy instance and load it
    // with our sequence of number systems in order to access it. After the regex automaton is created, we set its alphabet to be the
    // one requested, instead of the unicode alphabet that dk.brics uses.
    AutomatonDFA M = new AutomatonDFA();
    M.richAlphabet.setA(alphabets);
    M.determineAlphabetSize();

    String regex = determineEncodedRegex(baseexp, M.richAlphabet);

    AutomatonDFA R = new AutomatonDFA(regex, M.getAlphabetSize());
    R.richAlphabet.setA(M.richAlphabet.getA());
    R.determineAlphabetSize();
    R.setNS(NS);

    R.writeAutomata(regex, Session.getWriteAddressForAutomataLibrary(), regName, false);
    return new TestCase(R);
  }

  public static String determineEncodedRegex(String baseexp, RichAlphabet r) {
    int inputLength = r.getA().size();
    Matcher m2 = PAT_FOR_AN_ALPHABET_VECTOR.matcher(baseexp);
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
}
