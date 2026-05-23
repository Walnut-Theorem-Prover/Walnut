package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonReader;
import Main.Logging;
import Main.ProverHelper;
import Main.TestCase;

import java.util.List;

import static Main.TestCase.DEFAULT_TESTFILE;

public class Describe {
  public static TestCase describe(boolean isDFAO, String inFileName) {
    String inLibrary = ProverHelper.determineInLibrary(isDFAO, inFileName);

    Automaton M = new Automaton(inLibrary);

    Logging.logMessage(true, "File location: " + inLibrary);
    String comments = AutomatonReader.readComments(inLibrary);
    Logging.logMessage(true, "Comments: " + comments);
    Logging.logMessage(true, "State count:" + M.fa.getQ());
    Logging.logMessage(true, "Transition count:" + M.fa.getT().determineTransitionCount());
    Logging.logMessage(true, "Alphabet size:" + M.fa.getAlphabetSize());
    Logging.logMessage(true, "Number systems:" + M.getNS());

    return new TestCase("", null, null, Logging.getCommandLog(),
        List.of(new TestCase.AutomatonFilenamePair(M, DEFAULT_TESTFILE)));
  }
}
