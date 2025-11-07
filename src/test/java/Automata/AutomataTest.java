package Automata;

import Automata.Writer.AutomatonWriter;
import Main.Session;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static Automata.Automaton.*;

public class AutomataTest {

  private static final String automataTestDir = Session.getAddressForUnitTestResources() + "/automataTest/";
  private static final String shift = automataTestDir + "shift.txt";
  private static final String shiftFlipAccept = automataTestDir + "shift_flip_accept.txt";
  private static final String shiftFibSmall = automataTestDir + "shift_fib_small.txt";

  @Test
  void testPermute() {
    // See notes in UtilityMethods. This was *not* the original expected behavior, but that may be okay.
    List<String> L = List.of("a","b","c");

    //original expected behavior
    //Assertions.assertEquals(List.of("b","c","a"), UtilityMethods.permute(L, new int[]{1,2,0}));

    //actual behavior
    Assertions.assertEquals(List.of("c","a","b"), permute(L, new int[]{1,2,0}));
  }

  @Test
  void testLabelPermutation() {
    /*
     * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
     * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
     * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
     */
    List<String> label = List.of("z","a","c");
    List<String> sorted_label = new ArrayList<>(label);
    Collections.sort(sorted_label);
    int[] labelPermutation = getLabelPermutation(label, sorted_label);
    Assertions.assertArrayEquals(new int[]{2,0,1}, labelPermutation);

    List<List<Integer>> A = List.of(List.of(-1,2), List.of(0,1), List.of(1,2,3));
    List<List<Integer>> permutedA = permute(A, labelPermutation);
    List<List<Integer>> expectedPermutedA =
        List.of(List.of(0,1), List.of(1,2,3), List.of(-1,2));
    Assertions.assertEquals(expectedPermutedA, permutedA);

    RichAlphabet r = new RichAlphabet();
    r.setA(permutedA);
    r.setupEncoder();
    Assertions.assertEquals(List.of(1,2,6), r.getEncoder());
  }

  @Test
  void testCombineSimple() {
    /* shift:
    State 0 accepts, [0,0]->0, [0,1]->1
    State 1 rejects, [1,0]->0, [1,1]->1
    If totalized, other inputs go to state 2, which would reject and all inputs go to state 2.
    */
    Automaton A = new Automaton(shift);
    Assertions.assertEquals(2, A.fa.getQ());
    Assertions.assertEquals(0, A.fa.getQ0());
    Assertions.assertEquals(IntList.of(1,0), A.fa.getO());

    Queue<Automaton> q = new LinkedList<>();
    q.add(A.clone());
    Automaton c = AutomatonLogicalOps.combine(A, q, IntList.of(1,2), false, "", null);

    /*
    Combined:
    State 0 outputs a 2 (since both automata accept). [0,0]->0, [0,1]->2
    State 1 is universal rejector.
    State 2 outputs a 0 (since both automata reject). [1,0]->0, [1,1]->2
     */

    Assertions.assertEquals(A.richAlphabet.getA(), c.richAlphabet.getA()); // languages are same
    Assertions.assertEquals(3, c.fa.getQ());
    Assertions.assertEquals(0, c.fa.getQ0());
    Assertions.assertEquals(IntList.of(2,0,0), c.fa.getO());

    String expectedAutomatonStr = getExpectedAutomatonString(automataTestDir + "shift_c.txt");
    Assertions.assertEquals(expectedAutomatonStr, getActualAutomatonString(c));
  }

  @Test
  void testCombineAsymmetric() {
    /* shift: see above
       shift_flip_accept: flip acceptance. State 0 rejects, State 1 accepts.
    */
    Automaton A = new Automaton(shift);
    Assertions.assertEquals(2, A.fa.getQ());
    Assertions.assertEquals(0, A.fa.getQ0());
    Assertions.assertEquals(IntList.of(1,0), A.fa.getO());

    Automaton B = new Automaton(shiftFlipAccept);
    Assertions.assertEquals(2, B.fa.getQ());
    Assertions.assertEquals(0, B.fa.getQ0());
    Assertions.assertEquals(IntList.of(0,1), B.fa.getO());

    Queue<Automaton> q = new LinkedList<>();
    q.add(B.clone());
    Automaton c = AutomatonLogicalOps.combine(A, q, IntList.of(1,2), false, "", null);

    /*
    Combined:
    State 0 outputs a 1 (since A accepts but B rejects). [0,0]->0, [0,1]->2
    State 1 is universal rejector.
    State 2 outputs a 2 (since B accepts but A accepts). [1,0]->0, [1,1]->2
     */

    Assertions.assertEquals(A.richAlphabet.getA(), c.richAlphabet.getA()); // languages are same
    Assertions.assertEquals(3, c.fa.getQ());
    Assertions.assertEquals(0, c.fa.getQ0());
    Assertions.assertEquals(IntList.of(1,0,2), c.fa.getO());

    String expectedAutomatonStr = getExpectedAutomatonString(automataTestDir + "shift_asym_c1.txt");
    Assertions.assertEquals(expectedAutomatonStr, getActualAutomatonString(c));

    q.clear();
    q.add(A.clone());
    c = AutomatonLogicalOps.combine(B, q, IntList.of(1,2), false, "", null);

     /*
    Combined:
    State 0 outputs a 2 (since B accepts but A rejects). [0,0]->0, [0,1]->2
    State 1 is universal rejector.
    State 2 outputs a 1 (since B rejects but A accepts). [1,0]->0, [1,1]->2
     */

    Assertions.assertEquals(A.richAlphabet.getA(), c.richAlphabet.getA()); // languages are same
    Assertions.assertEquals(3, c.fa.getQ());
    Assertions.assertEquals(0, c.fa.getQ0());
    Assertions.assertEquals(IntList.of(2,0,1), c.fa.getO());

    expectedAutomatonStr = getExpectedAutomatonString(automataTestDir + "shift_asym_c2.txt");
    Assertions.assertEquals(expectedAutomatonStr, getActualAutomatonString(c));
  }

  // Activate applyAllRepresentationsWithOutput() logic
  @Test
  void testCombineAllReps() {
    Automaton A = new Automaton(shiftFibSmall);
    /*
    Very simple automata with [msd_pisot4 msd_pisot4] number system.
    State 0 ([0,0]->1) rejects, state 1 ([1,0]->1) accepts.
     */
    Queue<Automaton> q = new LinkedList<>();
    q.add(A.clone());
    Automaton c = AutomatonLogicalOps.combine(A, q, IntList.of(1,2), false, "", null);

    Assertions.assertEquals(A.richAlphabet.getA(), c.richAlphabet.getA()); // languages are same
    Assertions.assertEquals(0, c.fa.getQ0());

    // Usually this isn't minimized, so we don't assert how many states this has in case it's ever minimized.
    // What we care about is behavior. We want the input "[0,0]([1,0])*" to yield an output of "02*".

    Assertions.assertEquals(2, c.richAlphabet.getA().size());
    Assertions.assertEquals(List.of(0,0), c.richAlphabet.decode(0));
    Assertions.assertEquals(List.of(1,0), c.richAlphabet.decode(1));

    Assertions.assertEquals(0, c.fa.getO().getInt(0)); // first state outputs "0"
    int nextState = c.fa.t.getNfaState(0).get(0).getInt(0);

    // follow the [1,0]* route. This might be an overcount, but it's guaranteed to terminate.
    int repCount = 0;
    while (repCount < c.fa.getQ()) {
      Assertions.assertEquals(2, c.fa.getO().getInt(nextState)); // other states output "2"
      nextState = c.fa.t.getNfaState(nextState).get(1).getInt(0);
      repCount++;
    }
  }

  private static String getActualAutomatonString(Automaton c) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    AutomatonWriter.writeTxtFormatToStream(c, printWriter);
    return stringWriter.toString();
  }

  private static String getExpectedAutomatonString(String expectedFileLoc) {
    String expectedAutomaton = null;
    try {
      expectedAutomaton = Files.readString(Path.of(expectedFileLoc));
    } catch (IOException ex) {
      Assertions.fail("Unexpected: " + ex);
    }
    return expectedAutomaton;
  }
}
