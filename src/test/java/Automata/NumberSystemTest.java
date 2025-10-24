package Automata;

import Main.EvalComputations.Token.ArithmeticOperator;
import Main.Session;
import Main.WalnutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class NumberSystemTest {
  @BeforeEach
  void setUp() {
    Session.setPathsAndNamesIntegrationTests();
    Session.cleanPathsAndNamesIntegrationTest();
  }

  @Test
  void testBogusNS() {
    // There's no guard for this; it throws a real exception -- however, we should never get here anyway
    Assertions.assertThrows(StringIndexOutOfBoundsException.class, () -> new NumberSystem(""));
  }

  @Test
  void testLessBogusNS() {
    Assertions.assertThrows(WalnutException.class, () -> new NumberSystem("msd_BOGUSNS"));
  }

  @Test
  void testMSD7() {
    NumberSystem ns = new NumberSystem("msd_7");
    Assertions.assertTrue(ns.isMsd());
    Assertions.assertFalse(ns.useAllRepresentations());
    Assertions.assertEquals("msd_7", ns.getName());

    Assertions.assertEquals(7, ns.parseBase());

    List<NumberSystem> numberSystemList = new ArrayList<>();

    // empty list is probably not by design
    Assertions.assertTrue(NumberSystem.determineMsd(numberSystemList));

    numberSystemList.add(ns);
    Assertions.assertTrue(NumberSystem.determineMsd(numberSystemList));

    numberSystemList.add(null);
    Assertions.assertNull(NumberSystem.determineMsd(numberSystemList));
  }

  @Test
  void testMSDFlip() {
    NumberSystem ns = new NumberSystem("msd_5");
    Assertions.assertTrue(ns.isMsd());
    Assertions.assertEquals("msd_5", ns.getName());

    List<NumberSystem> numberSystemList = new ArrayList<>();
    numberSystemList.add(ns);
    numberSystemList.add(null);

    NumberSystem.flipNS(numberSystemList);

    // number system doesn't change
    Assertions.assertTrue(ns.isMsd());
    Assertions.assertEquals("msd_5", ns.getName());

    // but the one in the list does
    NumberSystem firstNS = numberSystemList.get(0);
    Assertions.assertFalse(firstNS.isMsd());
    Assertions.assertEquals("lsd_5", firstNS.getName());
    Assertions.assertEquals(NumberSystem.LSD_UNDERSCORE, firstNS.determineBaseNameUnderscore());

    NumberSystem.flipNS(numberSystemList);
    firstNS = numberSystemList.get(0);
    Assertions.assertTrue(firstNS.isMsd());
    Assertions.assertEquals("msd_5", firstNS.getName());
  }

  @Test
  void testMSDFib() {
    NumberSystem ns = new NumberSystem("msd_fib");
    Assertions.assertTrue(ns.isMsd());
    Assertions.assertTrue(ns.useAllRepresentations());
    Assertions.assertEquals("msd_fib", ns.getName());

    Assertions.assertThrows(WalnutException.class, ns::parseBase);
  }

  @Test
  void testMakeNeg() {
    NumberSystem ns = new NumberSystem("msd_3");
    NumberSystem negNS = ns.determineNegativeNS();
    Assertions.assertEquals("msd_neg_3", negNS.getName());
    Assertions.assertEquals(NumberSystem.MSD_UNDERSCORE, negNS.determineBaseNameUnderscore());

    // double negative... remains negative. By design. ?
    NumberSystem doubleNegNS = negNS.determineNegativeNS();
    Assertions.assertEquals(negNS, doubleNegNS);
  }

  @Test
  void testArithmetic() {
    NumberSystem ns = new NumberSystem("msd_3");
    // Can't divide two variables
    Assertions.assertThrows(WalnutException.class,
        () -> ns.arithmetic("a","b","c", ArithmeticOperator.Ops.DIV));
    Assertions.assertThrows(WalnutException.class,
        () -> ns.arithmetic("a","b",0, ArithmeticOperator.Ops.DIV));

    // unexpected operation, probably can't even do this
    Assertions.assertThrows(WalnutException.class,
        () -> ns.arithmetic("a","b","c", ArithmeticOperator.Ops.UNARY_NEGATIVE));

    // division by zero
    Assertions.assertThrows(WalnutException.class,
        () -> ns.arithmetic("a",0,"c", ArithmeticOperator.Ops.DIV));
  }

  @Test
  void testNegArithmeticOrdering() {
    NumberSystem ns = new NumberSystem("msd_neg_3");
    Automaton A = ns.arithmetic(-1, "a", "b", ArithmeticOperator.Ops.PLUS);
    A.canonize();
    Automaton B = ns.arithmetic("b", 1, "a", ArithmeticOperator.Ops.PLUS);
    B.canonize();
    Assertions.assertEquals(A.toString(), B.toString()); // basically the same...

    // very similar case
    A = ns.arithmetic("a", "b", -1, ArithmeticOperator.Ops.MINUS);
    A.canonize();
    B = ns.arithmetic("a", 1, "b", ArithmeticOperator.Ops.MINUS);
    B.canonize();
    Assertions.assertEquals(A.toString(), B.toString()); // basically the same...
  }
}
