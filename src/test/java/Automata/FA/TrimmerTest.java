package Automata.FA;

import it.unimi.dsi.fastutil.ints.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TrimmerTest {
    @Test
    void testNFAExampleNoTrim() {
        FA a = new FA();
        a.setAlphabetSize(2);
        // 4 states: 0: a -> 1, a,b -> 2. 1: a -> 3. 2: a,b -> 3. 3: accept.
        a.addOutput(false);
        a.addOutput(false);
        a.addOutput(false);
        a.addOutput(true); // accepting
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = a.t.addMapToNfaD();
        s0.put(0, new IntArrayList(List.of(1, 2))); // 0: a -> 1, a -> 2
        s0.put(1, new IntArrayList(List.of(2))); // 0: b -> 2

        Int2ObjectRBTreeMap<IntList> s1 = a.t.addMapToNfaD();
        IntList s1ListA = new IntArrayList(List.of(3));
        s1.put(0, s1ListA); // 1: a -> 3

        Int2ObjectRBTreeMap<IntList> s2 = a.t.addMapToNfaD();
        s2.put(0, new IntArrayList(List.of(3))); // 2: a -> 3
        s2.put(1, new IntArrayList(List.of(3))); // 2: b -> 3

        a.t.addMapToNfaD(); // s3

        a.setQ(a.getO().size());
        Assertions.assertEquals(a.getQ(), a.t.getNfaD().size());

        a.canonizeInternal();
        int oldQ = a.getQ();
        int oldQ0 = a.getQ0();

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(oldQ, a.getQ());
        a.canonizeInternal();
        Assertions.assertEquals(oldQ, a.getQ());
        Assertions.assertEquals(oldQ0, a.getQ0());
    }

    @Test
    void testNFATrim() {
        FA a = new FA();
        a.setAlphabetSize(1);
        // 0: a->{1,2}. 3 accepting but nothing reaches it.
        a.addOutput(true);
        a.addOutput(true);
        a.addOutput(true);
        a.addOutput(true);
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = a.t.addMapToNfaD();
        s0.put(0, new IntArrayList(List.of(1, 2))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s2 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s3 = a.t.addMapToNfaD();

        IntSet initialStates = new IntOpenHashSet(IntSet.of(0));
        IntSet rTrim = Trimmer.rightTrim(1, a.t.getNfaD(), initialStates);
        Assertions.assertEquals(IntSet.of(0,1,2), rTrim);

        IntSet lTrim = Trimmer.leftTrim(a);
        Assertions.assertEquals(IntSet.of(0,1,2,3), lTrim);

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(3, a.getQ());
        Assertions.assertEquals(0, a.getQ0());
        Assertions.assertEquals(IntList.of(1,2),a.t.getNfaState(0).get(0));
    }

    @Test
    void testNFATrimWithRenumber() {
        FA a = new FA();
        a.setAlphabetSize(1);
        // 0: a->{2,3}. 1 accepting but nothing reaches it.
        a.addOutput(true);
        a.addOutput(true);
        a.addOutput(true);
        a.addOutput(true);
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = a.t.addMapToNfaD();
        s0.put(0, new IntArrayList(List.of(2, 3))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s2 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s3 = a.t.addMapToNfaD();

        IntSet initialStates = new IntOpenHashSet(IntSet.of(0));
        IntSet rTrim = Trimmer.rightTrim(1, a.t.getNfaD(), initialStates);
        Assertions.assertEquals(IntSet.of(0,2,3), rTrim);

        IntSet lTrim = Trimmer.leftTrim(a);
        Assertions.assertEquals(IntSet.of(0,1,2,3), lTrim);

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(3, a.getQ());
        Assertions.assertEquals(0, a.getQ0());
        Assertions.assertEquals(IntList.of(1,2),a.t.getNfaState(0).get(0)); // renumbered
    }

    @Test
    void testTrimInvalidDest() {
        FA a = new FA();
        a.setAlphabetSize(1);
        a.addOutput(true);
        a.addOutput(false);
        a.addOutput(false);
        a.addOutput(false);
        a.setQ(4);
        Int2ObjectRBTreeMap<IntList> s0 = a.t.addMapToNfaD();
        s0.put(0, new IntArrayList(List.of(2, 3))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s2 = a.t.addMapToNfaD();
        Int2ObjectRBTreeMap<IntList> s3 = a.t.addMapToNfaD();

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(1, a.getQ());
        Assertions.assertTrue(a.t.getNfaState(0).get(0).isEmpty());
    }

    @Test
    void testTrimNull() {
        new Trimmer(); // just for coverage
        Trimmer.trimAutomaton(new FA());
    }

    @Test
    void testTrimAll() {
        FA a = new FA();
        a.setAlphabetSize(1);
        a.addOutput(false);
        a.addOutput(false);
        a.setQ(2);
        a.t.addMapToNfaD();
        a.t.addMapToNfaD();
        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(1, a.getQ());
    }
}