package Main.Commands;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.Morphism;
import Automata.NumberSystem;
import Main.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Image {
  public static TestCase image(String s, String morphismFileName, String imageOldName, String imageNewName, boolean printFlag) throws IOException {
    String morphismAddress =
        Session.getReadFileForMorphismLibrary(morphismFileName + Prover.TXT_EXTENSION);
    Automata.Morphism h = new Morphism(UtilityMethods.readFromFile(morphismAddress));
    h.requirePositiveUniformLength();

    Automaton oldWord = new Automaton(Session.getReadFileForWordsLibrary(imageOldName + Prover.TXT_EXTENSION));
    String numSysName = determineImageNumberSystemPrefix(oldWord, imageOldName);

    List<Integer> range = new ArrayList<>(h.range);
    range.sort(Integer::compareTo);

    Automaton first = null;
    LinkedList<Automaton> subautomata = new LinkedList<>();
    IntList outputs = new IntArrayList();
    for (int value : range) {
      Automaton valueAutomaton =
          EvalDef.getImageEval(h.makeInterPredicate(value, imageOldName, numSysName), printFlag);
      if (first == null) {
        first = valueAutomaton;
      } else {
        subautomata.add(valueAutomaton);
      }
      outputs.add(value);
    }

    Automaton image = AutomatonLogicalOps.combine(first, subautomata, outputs);
    image.writeAutomata(s, Session.getWriteAddressForWordsLibrary(), imageNewName, true);
    return new TestCase(image);
  }

  private static String determineImageNumberSystemPrefix(Automaton word, String wordName) {
    if (word.getArity() != 1 || word.getNS().size() != 1) {
      throw new WalnutException("Image requires a unary word automaton: " + wordName);
    }
    NumberSystem ns = word.getNS().get(0);
    if (ns == null || ns.toString().isEmpty()) {
      return "";
    }
    return "?" + ns;
  }
}
