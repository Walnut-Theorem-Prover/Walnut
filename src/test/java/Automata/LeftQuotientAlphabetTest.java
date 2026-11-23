package Automata;

import Main.EqualityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class LeftQuotientAlphabetTest {
    @Test
    void testOutOfAlphabetPrefixCannotMatch() {
        // {1} \\ {00} is empty: 1 is not a symbol of the first alphabet.
        // Encoding symbol 1 using alphabet {0} previously produced -1,
        // which could subsequently be decoded as symbol 0.
        Automaton language = regex("00", List.of(0));
        Automaton prefixes = regex("1", List.of(0, 1));

        Automaton quotient = AutomatonLogicalOps.leftQuotient(language, prefixes);
        Assertions.assertTrue(quotient.fa.isLanguageEmpty());
    }

    @Test
    void testValidPrefixesSurviveTheAlphabetFilter() {
        // {0,1} \\ {00} = {0}: ignore prefix 1 but retain prefix 0.
        Automaton language = regex("00", List.of(0));
        Automaton prefixes = regex("0|1", List.of(0, 1));
        Automaton expected = regex("0", List.of(0));

        Automaton quotient = AutomatonLogicalOps.leftQuotient(language, prefixes);
        Assertions.assertTrue(EqualityUtils.faEqual(expected.fa, quotient.fa));
    }

    private static Automaton regex(String expression, List<Integer> alphabet) {
        return new AutomatonDFA(expression, alphabet, null);
    }
}
