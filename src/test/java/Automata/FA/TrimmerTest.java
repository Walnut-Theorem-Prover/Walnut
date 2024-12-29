package Automata.FA;

import it.unimi.dsi.fastutil.ints.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TrimmerTest {
    @Test
    void testNFAExampleNoTrim() {
        FA a = new FA();
        a.setAlphabetSize(2);
        // 4 states: 0: a -> 1, a,b -> 2. 1: a -> 3. 2: a,b -> 3. 3: accept.
        a.getO().add(0);
        a.getO().add(0);
        a.getO().add(0);
        a.getO().add(1); // accepting
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = new Int2ObjectRBTreeMap<>();
        s0.put(0, new IntArrayList(List.of(1, 2))); // 0: a -> 1, a -> 2
        s0.put(1, new IntArrayList(List.of(2))); // 0: b -> 2

        Int2ObjectRBTreeMap<IntList> s1 = new Int2ObjectRBTreeMap<>();
        IntList s1ListA = new IntArrayList(List.of(3));
        s1.put(0, s1ListA); // 1: a -> 3

        Int2ObjectRBTreeMap<IntList> s2 = new Int2ObjectRBTreeMap<>();
        s2.put(0, new IntArrayList(List.of(3))); // 2: a -> 3
        s2.put(1, new IntArrayList(List.of(3))); // 2: b -> 3

        a.getD().add(s0);
        a.getD().add(s1);
        a.getD().add(s2);
        a.getD().add(new Int2ObjectRBTreeMap<>()); // s3

        a.setQ(a.getO().size());
        Assertions.assertEquals(a.getQ(), a.getD().size());

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
        a.getO().add(1);
        a.getO().add(1);
        a.getO().add(1);
        a.getO().add(1);
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = new Int2ObjectRBTreeMap<>();
        s0.put(0, new IntArrayList(List.of(1, 2))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s2 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s3 = new Int2ObjectRBTreeMap<>();

        a.getD().add(s0);
        a.getD().add(s1);
        a.getD().add(s2);
        a.getD().add(s3);

        IntSet initialStates = new IntOpenHashSet(IntSet.of(0));
        IntSet rTrim = Trimmer.rightTrim(1, a.getD(), initialStates);
        Assertions.assertEquals(IntSet.of(0,1,2), rTrim);

        IntSet lTrim = Trimmer.leftTrim(a);
        Assertions.assertEquals(IntSet.of(0,1,2,3), lTrim);

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(3, a.getQ());
        Assertions.assertEquals(0, a.getQ0());
        Assertions.assertEquals(IntList.of(1,2),a.getD().get(0).get(0));
    }

    @Test
    void testNFATrimWithRenumber() {
        FA a = new FA();
        a.setAlphabetSize(1);
        // 0: a->{2,3}. 1 accepting but nothing reaches it.
        a.getO().add(1);
        a.getO().add(1);
        a.getO().add(1);
        a.getO().add(1);
        a.setQ(4);

        Int2ObjectRBTreeMap<IntList> s0 = new Int2ObjectRBTreeMap<>();
        s0.put(0, new IntArrayList(List.of(2, 3))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s2 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s3 = new Int2ObjectRBTreeMap<>();

        a.getD().add(s0);
        a.getD().add(s1);
        a.getD().add(s2);
        a.getD().add(s3);

        IntSet initialStates = new IntOpenHashSet(IntSet.of(0));
        IntSet rTrim = Trimmer.rightTrim(1, a.getD(), initialStates);
        Assertions.assertEquals(IntSet.of(0,2,3), rTrim);

        IntSet lTrim = Trimmer.leftTrim(a);
        Assertions.assertEquals(IntSet.of(0,1,2,3), lTrim);

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(3, a.getQ());
        Assertions.assertEquals(0, a.getQ0());
        Assertions.assertEquals(IntList.of(1,2),a.getD().get(0).get(0)); // renumbered
    }

    @Test
    void testTrimInvalidDest() {
        FA a = new FA();
        a.setAlphabetSize(1);
        a.getO().add(1);
        a.getO().add(0);
        a.getO().add(0);
        a.getO().add(0);
        a.setQ(4);
        Int2ObjectRBTreeMap<IntList> s0 = new Int2ObjectRBTreeMap<>();
        s0.put(0, new IntArrayList(List.of(2, 3))); // 0: a -> {1,2}

        Int2ObjectRBTreeMap<IntList> s1 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s2 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s3 = new Int2ObjectRBTreeMap<>();

        a.getD().add(s0);
        a.getD().add(s1);
        a.getD().add(s2);
        a.getD().add(s3);
        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(1, a.getQ());
        Assertions.assertTrue(a.getD().get(0).get(0).isEmpty());
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
        a.getO().add(0);
        a.getO().add(0);
        a.setQ(2);
        Int2ObjectRBTreeMap<IntList> s0 = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<IntList> s1 = new Int2ObjectRBTreeMap<>();
        a.getD().add(s0);
        a.getD().add(s1);

        Trimmer.trimAutomaton(a);
        Assertions.assertEquals(1, a.getQ());
    }
}