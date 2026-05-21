package Automata;

import Main.Session;
import Main.UtilityMethods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MorphismTest {
    @Test
    void testGamMorphism() throws IOException {
        String morphismAddress = Session.getReadFileForMorphismLibrary("gam.txt");
        String mapString =
            Files.readString(Paths.get(UtilityMethods.validateFile(morphismAddress).toURI()));
        Morphism h = new Morphism(mapString);
        Automaton P = h.toWordAutomaton();
        Assertions.assertEquals("[{0=>[0], 1=>[1]}, {0=>[2], 1=>[1]}, {0=>[0], 1=>[3]}, {0=>[2], 1=>[3]}]",
            P.getFa().getT().getNfaD().toString());
    }
}
