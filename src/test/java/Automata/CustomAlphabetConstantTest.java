package Automata;

import Main.Commands.EvalDef;
import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class CustomAlphabetConstantTest {
    @BeforeEach
    void setUp() {
        Session.setPathsAndNamesIntegrationTests();
    }

    @Test
    void testConstantsUseDigitValuesRatherThanAlphabetIndices() {
        // The custom binary addition resource declares its alphabet as {1,0}.
        // The value 0 must still be represented by digit 0, and 1 by digit 1.
        NumberSystem ns = new NumberSystem("msd_auditorder");
        Assertions.assertEquals(List.of(1, 0), ns.getAlphabet());
        Automaton zero = ns.getConstant(0);
        Automaton one = ns.getConstant(1);

        Assertions.assertTrue(acceptsDigit(zero, 0));
        Assertions.assertFalse(acceptsDigit(zero, 1));
        Assertions.assertFalse(acceptsDigit(one, 0));
        Assertions.assertTrue(acceptsDigit(one, 1));
    }

    @Test
    void testZeroIsExactlyTheAdditiveIdentity() {
        // For integers, x+x=x iff x=0.
        Automaton result = EvalDef.evaluatePredicate("?msd_auditorder Ax ((x=0) <=> (x+x=x))");
        Assertions.assertTrue(result.fa.isTRUE_FALSE_AUTOMATON());
        Assertions.assertTrue(result.fa.isTRUE_AUTOMATON());
    }

    private static boolean acceptsDigit(Automaton automaton, int digit) {
        int input = automaton.richAlphabet.encode(List.of(digit));
        return automaton.fa.FAtoCompactNFA().accepts(List.of(input));
    }
}
