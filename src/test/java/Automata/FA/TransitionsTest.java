package Automata.FA;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransitionsTest {
  @Test
  void testTransitionsDFA() {
    // trivial tests. Should be more interesting later.
    TransitionsDFA tDFA = new TransitionsDFA();
    Assertions.assertTrue(tDFA.isDeterministic());
    Assertions.assertEquals(0, tDFA.determineTransitionCount());
    tDFA.reduceMemory();
    Assertions.assertEquals("dfaD:[]", tDFA.toString());
    tDFA.setDfaD(tDFA.getDfaD());
    Int2IntMap iMap = tDFA.addMapToDfaD();
    Assertions.assertNotNull(iMap);
  }
}
