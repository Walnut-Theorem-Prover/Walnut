package Automata;

import Automata.FA.BricsConverter;
import Main.UtilityMethods;
import Main.WalnutException;

import java.util.ArrayList;
import java.util.List;

/**
 * Typesafe extension that requires determinism. DFA and DFAO are allowed.
 */
public class AutomatonDFA extends Automaton {
  public AutomatonDFA() {
    super();
  }

  public AutomatonDFA(boolean truthValue) {
    super();
    fa.setTRUE_FALSE_AUTOMATON(true);
    this.fa.setTRUE_AUTOMATON(truthValue);
  }

  /**
   * Takes an address and constructs the DFA/DFAO represented by the file referred to by the address.
   * NFA input is determinized when Walnut can determinize it.
   */
  public AutomatonDFA(String address) {
    super();
    AutomatonReader.readAutomaton(this, address);
    requireDfaStorage();
  }

  /**
   * Takes a regular expression and the alphabet for that regular expression and constructs the corresponding automaton.
   * For example if the regularExpression = "01*" and alphabet = [0,1,2], then the resulting automaton accepts
   * words of the form 01* over the alphabet {0,1,2}.
   * We actually compute the automaton for regularExpression intersected with alphabet*.
   * So for example if regularExpression = [^4]* and alphabet is [1,2,4], then the resulting
   * automaton accepts (1|2)*
   * An important thing to note here is that the automaton being constructed
   * with this constructor, has only one input, and it is of type AlphabetLetter.
   */
  @SuppressWarnings("this-escape")
  public AutomatonDFA(String regularExpression, List<Integer> alphabet, NumberSystem numSys) {
    super();
    if (alphabet == null || alphabet.isEmpty()) throw new WalnutException("empty alphabet is not accepted");
    alphabet = new ArrayList<>(alphabet);
    //The alphabet is a set and does not allow repeated elements. However, the user might enter the command
    //reg myreg {1,1,0,0,0} "10*"; and therefore alphabet = [1,1,0,0,0]. So remove duplicates.
    UtilityMethods.removeDuplicates(alphabet);
    this.richAlphabet.getA().add(alphabet);

    BricsConverter.convertFromBrics(this.fa, alphabet, regularExpression);
    getNS().add(numSys);
    requireDfaStorage();
  }

  public static AutomatonDFA readFromFile(String address) {
    return new AutomatonDFA(address);
  }

  public static AutomatonDFA from(Automaton automaton) {
    if (automaton instanceof AutomatonDFA dfa) {
      dfa.requireDfaStorage();
      return dfa;
    }
    if (automaton.fa.isTRUE_FALSE_AUTOMATON()) {
      return new AutomatonDFA(automaton.fa.isTRUE_AUTOMATON());
    }
    AutomatonDFA dfa = (AutomatonDFA) automaton.cloneFields(new AutomatonDFA());
    dfa.requireDfaStorage();
    return dfa;
  }

  private void requireDfaStorage() {
    if (this.fa.isTRUE_FALSE_AUTOMATON()) {
      return;
    }
    if (!this.getFa().getT().isDeterministic()) {
      if (this.getFa().isFAO()) {
        throw WalnutException.nonDeterministicO();
      }
      determinizeAndMinimize();
    }
    this.getFa().convertNFAtoDFA();
  }

  // This handles the generalised case of vectors such as "[0,1]*[0,0][0,1]"
  // TODO - maybe exactly the same as above
  @SuppressWarnings("this-escape")
  public AutomatonDFA(String regularExpression, Integer alphabetSize) {
    super();
    BricsConverter.setFromBricsAutomaton(this.fa, alphabetSize, regularExpression);
    requireDfaStorage();
  }

  @Override
  public AutomatonDFA clone() {
    if (fa.isTRUE_FALSE_AUTOMATON()) {
      return new AutomatonDFA(fa.isTRUE_AUTOMATON());
    }
    requireDfaStorage();
    return (AutomatonDFA) cloneFields(new AutomatonDFA());
  }
}
