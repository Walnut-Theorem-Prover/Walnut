package Main.Commands;

import Automata.Automaton;
import Automata.NumberSystem;
import Automata.Numeration.Ostrowski;
import Main.Prover;
import Main.TestCase;

import java.util.List;

import static Main.TestCase.DEFAULT_TESTFILE;

public class Ost {
  public static TestCase ostCommand(String name, String preperiod, String period) {
    Ostrowski ostr = new Ostrowski(name, preperiod, period);
    Automaton repr = ostr.createRepresentationAutomaton();
    String msdName = NumberSystem.MSD_UNDERSCORE + name;
    Ostrowski.writeAutomaton(name, msdName + Prover.TXT_EXTENSION, repr);
    Automaton adder = ostr.createAdderAutomaton();
    Ostrowski.writeAutomaton(name, msdName + NumberSystem.UNDERSCORE_ADDITION_AUTOMATON, adder);
    return new TestCase(
        List.of(new TestCase.AutomatonFilenamePair(adder, DEFAULT_TESTFILE),
            new TestCase.AutomatonFilenamePair(repr, TestCase.OST_REPR_TESTFILE)));
  }
}
