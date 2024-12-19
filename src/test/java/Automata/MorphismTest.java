package Automata;

import Main.UtilityMethods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MorphismTest {
    @Test
    void testGamMorphism() throws IOException {
        Morphism h = new Morphism(UtilityMethods.get_address_for_morphism_library()+"gam.txt");
        Automaton P = h.toWordAutomaton();
        Assertions.assertEquals("[{0=>[0], 1=>[1]}, {0=>[2], 1=>[1]}, {0=>[0], 1=>[3]}, {0=>[2], 1=>[3]}]", P.d.toString());
    }

    @Test
    void testUniform() {
        TreeMap<Integer, List<Integer>> mapping = new TreeMap<>();

        Morphism morphism;

        morphism = new Morphism("blah", "0->0010 1->1122 2->0200 3->1212");
        Assertions.assertTrue(morphism.isUniform());

        morphism.mapping = mapping;
        Assertions.assertTrue(morphism.isUniform(), "Empty mapping should be uniform");

        mapping.put(0, List.of(0, 1));
        mapping.put(1, List.of(1, 0));
        mapping.put(2, List.of(1, 1));
        Assertions.assertTrue(morphism.isUniform(), "Mapping with all outputs of same length should be uniform");

        mapping.clear();
        mapping.put(0, List.of(0, 1));
        mapping.put(1, List.of(0, 1));
        mapping.put(2, List.of(1));
        Assertions.assertFalse(morphism.isUniform(), "Mapping with different output lengths should not be uniform");

        mapping.clear();
        mapping.put(0, List.of(0, 1));
        Assertions.assertTrue(morphism.isUniform(), "Mapping with a single key should be uniform");

        mapping.clear();
        mapping.put(0, List.of(0, 1));
        mapping.put(1, List.of());
        mapping.put(2, List.of(1));
        Assertions.assertFalse(morphism.isUniform(), "Mapping with mixed lengths should not be uniform");

        mapping.clear();
        mapping.put(0, List.of(0, 1, 0, 1));
        mapping.put(1, List.of(1, 1, 0, 1));
        mapping.put(2, List.of(1, 1, 1, 1));
        Assertions.assertTrue(morphism.isUniform(), "Large mapping with consistent lengths should be uniform");
    }
}
