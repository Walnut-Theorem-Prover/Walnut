package Automata.FA;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransitionsTest {
  @Test
  void testTransitionsDFA() {
    TransitionsDFA tDFA = new TransitionsDFA();
    Assertions.assertTrue(tDFA.isDeterministic());
    Assertions.assertEquals(0, tDFA.determineTransitionCount());
    tDFA.reduceMemory();
    Assertions.assertEquals("dfaD:[]", tDFA.toString());
  }
}
