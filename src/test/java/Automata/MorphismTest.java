package Automata;

import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MorphismTest {
    @Test
    void testGamMorphism() throws IOException {
        Morphism h = new Morphism(Session.getReadFileForMorphismLibrary("gam.txt"));
        Automaton P = h.toWordAutomaton();
        Assertions.assertEquals("[{0=>[0], 1=>[1]}, {0=>[2], 1=>[1]}, {0=>[0], 1=>[3]}, {0=>[2], 1=>[3]}]", P.getD().toString());
    }
}
