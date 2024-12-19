/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
 *
 * 	 This file is part of Walnut.
 *
 *   Walnut is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Walnut is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Walnut.  If not, see <http://www.gnu.org/licenses/>.
 */

package Main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.NumberSystem;
import Automata.Automaton;
import Token.AlphabetLetter;
import Token.ArithmeticOperator;
import Token.Function;
import Token.LeftParenthesis;
import Token.LogicalOperator;
import Token.NumberLiteral;
import Token.Operator;
import Token.RelationalOperator;
import Token.RightParenthesis;
import Token.Token;
import Token.Variable;
import Token.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static Automata.ParseMethods.PATTERN_WHITESPACE;

public class Predicate {
    private static final Logger LOGGER = LogManager.getLogger(Predicate.class);

    String predicate;
    List<Token> postOrder;
    Stack<Operator> operatorStack;
    int realStartingPosition;
    String defaultNumberSystem;
    Matcher MATCHER_FOR_LOGICAL_OPERATORS;
    Matcher MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES;
    Matcher MATCHER_FOR_RELATIONAL_OPERATORS;
    Matcher MATCHER_FOR_ARITHMETIC_OPERATORS;
    Matcher MATCHER_FOR_NUMBER_SYSTEM;
    Matcher MATCHER_FOR_WORD;
    Matcher MATCHER_FOR_WORD_WITH_DELIMITER;
    Matcher MATCHER_FOR_FUNCTION;
    Matcher MATCHER_FOR_MACRO;
    Matcher MATCHER_FOR_VARIABLE;
    Matcher MATCHER_FOR_NUMBER_LITERAL;
    Matcher MATCHER_FOR_ALPHABET_LETTER;
    Matcher MATCHER_FOR_LEFT_PARENTHESIS;
    Matcher MATCHER_FOR_RIGHT_PARENTHESIS;
    Matcher MATCHER_FOR_WHITESPACE;

    static HashMap<String, NumberSystem> numberSystemHash = new HashMap<>();

    public static HashMap<String, NumberSystem> get_number_system_Hash() {
        return numberSystemHash;
    }

    // allow automata names that start with A, E, I, for stuff like .AUTOMATON[..] or .EVEN[..], etc.
    static Pattern PATTERN_FOR_LOGICAL_OPERATORS =
        Pattern.compile("\\G\\s*(?<!\\.)(`|\\^|\\&|\\~|\\||=>|<=>|E|A|I|\\u02DC|\\u0303)");
    static Pattern PATTERN_FOR_LIST_OF_QUANTIFIED_VARIABLES =
        Pattern.compile("\\G\\s*((\\s*([a-zA-Z&&[^AEI]]\\w*)\\s*)(\\s*,\\s*([a-zA-Z&&[^AEI]]\\w*)\\s*)*)");
    static Pattern PATTERN_FOR_RELATIONAL_OPERATORS = Pattern.compile("\\G\\s*(>=|<=|<|>|=|!=)");
    static Pattern PATTERN_FOR_ARITHMETIC_OPERATORS = Pattern.compile("\\G\\s*(_|/|\\*|\\+|\\-)");
    static Pattern PATTERN_FOR_NUMBER_SYSTEM =
        Pattern.compile("\\G\\s*\\?(((msd|lsd)_(\\d+|\\w+))|((msd|lsd)(\\d+|\\w+))|(msd|lsd)|(\\d+|\\w+))");
    static Pattern PATTERN_FOR_WORD = Pattern.compile("\\G\\s*([a-zA-Z&&[^AEI]]\\w*)\\s*\\[");
    static Pattern PATTERN_FOR_WORD_WITH_DELIMITER = Pattern.compile("\\G\\s*\\.([a-zA-Z]\\w*)\\s*\\[");
    static Pattern PATTERN_FOR_FUNCTION = Pattern.compile("\\G\\s*\\$([a-zA-Z&&[^AEI]]\\w*)\\s*\\(");
    static Pattern PATTERN_FOR_MACRO = Pattern.compile("\\G(\\s*)\\#([a-zA-Z&&[^AEI]]\\w*)\\s*\\(");
    static Pattern PATTERN_FOR_VARIABLE = Pattern.compile("\\G\\s*([a-zA-Z&&[^AEI]]\\w*)");
    static Pattern PATTERN_FOR_NUMBER_LITERAL = Pattern.compile("\\G\\s*(\\d+)");
    static Pattern PATTERN_FOR_ALPHABET_LETTER = Pattern.compile("\\G\\s*@(\\s*(\\+|\\-)?\\s*\\d+)");
    static Pattern PATTERN_FOR_LEFT_PARENTHESIS = Pattern.compile("\\G\\s*\\(");
    static Pattern PATTERN_FOR_RIGHT_PARENTHESIS = Pattern.compile("\\G\\s*\\)");
    static Pattern PATTERN_FOR_WHITESPACE = Pattern.compile("\\G\\s+");

    public Predicate(String predicate) {
        this("msd_2", predicate, 0);
    }

    private void initializeMatchers() {
        MATCHER_FOR_LOGICAL_OPERATORS = PATTERN_FOR_LOGICAL_OPERATORS.matcher(predicate);
        MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES = PATTERN_FOR_LIST_OF_QUANTIFIED_VARIABLES.matcher(predicate);
        MATCHER_FOR_RELATIONAL_OPERATORS = PATTERN_FOR_RELATIONAL_OPERATORS.matcher(predicate);
        MATCHER_FOR_ARITHMETIC_OPERATORS = PATTERN_FOR_ARITHMETIC_OPERATORS.matcher(predicate);
        MATCHER_FOR_NUMBER_SYSTEM = PATTERN_FOR_NUMBER_SYSTEM.matcher(predicate);
        MATCHER_FOR_WORD = PATTERN_FOR_WORD.matcher(predicate);
        MATCHER_FOR_WORD_WITH_DELIMITER = PATTERN_FOR_WORD_WITH_DELIMITER.matcher(predicate);
        MATCHER_FOR_FUNCTION = PATTERN_FOR_FUNCTION.matcher(predicate);
        MATCHER_FOR_MACRO = PATTERN_FOR_MACRO.matcher(predicate);
        MATCHER_FOR_VARIABLE = PATTERN_FOR_VARIABLE.matcher(predicate);
        MATCHER_FOR_NUMBER_LITERAL = PATTERN_FOR_NUMBER_LITERAL.matcher(predicate);
        MATCHER_FOR_ALPHABET_LETTER = PATTERN_FOR_ALPHABET_LETTER.matcher(predicate);
        MATCHER_FOR_LEFT_PARENTHESIS = PATTERN_FOR_LEFT_PARENTHESIS.matcher(predicate);
        MATCHER_FOR_RIGHT_PARENTHESIS = PATTERN_FOR_RIGHT_PARENTHESIS.matcher(predicate);
        MATCHER_FOR_WHITESPACE = PATTERN_FOR_WHITESPACE.matcher(predicate);
    }

    public Predicate(
            String defaultNumberSystem,
            String predicate,
            int realStartingPosition) {
        operatorStack = new Stack<>();
        postOrder = new ArrayList<>();
        this.realStartingPosition = realStartingPosition;
        this.predicate = predicate;
        this.defaultNumberSystem = defaultNumberSystem;
        if (PATTERN_WHITESPACE.matcher(predicate).matches()) return;
        initializeMatchers();
        tokenizeAndComputePostOrder();
    }

    private void tokenizeAndComputePostOrder() {
        Stack<String> numberSystems = new Stack<>();
        numberSystems.push(defaultNumberSystem);
        String currentNumberSystem = defaultNumberSystem;
        int index = 0;
        Token t;
        Operator op;
        boolean lastTokenWasOperator = true;
        while (index < predicate.length()) {
            if (MATCHER_FOR_LOGICAL_OPERATORS.find(index)) {
                lastTokenWasOperator = true;
                Matcher matcher = MATCHER_FOR_LOGICAL_OPERATORS;
                if (matcher.group(1).equals("E") || matcher.group(1).equals("A") || matcher.group(1).equals("I")) {
                    if (!MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES.find(matcher.end())) {
                        throw new RuntimeException(
                                "Operator " + matcher.group(1) +
                                        " requires a list of variables: char at " +
                                        (realStartingPosition + index));
                    }

                    index = handleQuantifier();
                } else {
                    op = new LogicalOperator(
                            realStartingPosition + matcher.start(1), matcher.group(1));
                    op.put(postOrder, operatorStack);
                    index = matcher.end();
                }
            } else if (MATCHER_FOR_RELATIONAL_OPERATORS.find(index)) {
                lastTokenWasOperator = true;
                Matcher matcher = MATCHER_FOR_RELATIONAL_OPERATORS;
                if (!numberSystemHash.containsKey(currentNumberSystem)) {
                    numberSystemHash.put(currentNumberSystem, new NumberSystem(currentNumberSystem));
                }
                op = new RelationalOperator(realStartingPosition + matcher.start(1), matcher.group(1), numberSystemHash.get(currentNumberSystem));
                op.put(postOrder, operatorStack);
                index = matcher.end();
            } else if (MATCHER_FOR_ARITHMETIC_OPERATORS.find(index)) {
                lastTokenWasOperator = true;
                Matcher matcher = MATCHER_FOR_ARITHMETIC_OPERATORS;
                if (!numberSystemHash.containsKey(currentNumberSystem))
                    numberSystemHash.put(currentNumberSystem, new NumberSystem(currentNumberSystem));
                op = new ArithmeticOperator(realStartingPosition + matcher.start(1), matcher.group(1), numberSystemHash.get(currentNumberSystem));
                op.put(postOrder, operatorStack);
                index = matcher.end();
            } else if (MATCHER_FOR_WORD.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                lastTokenWasOperator = false;
                index = putWord(currentNumberSystem, false);
            } else if (MATCHER_FOR_WORD_WITH_DELIMITER.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                lastTokenWasOperator = false;
                index = putWord(currentNumberSystem, true);
            } else if (MATCHER_FOR_FUNCTION.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                lastTokenWasOperator = false;
                index = putFunction(currentNumberSystem);
            } else if (MATCHER_FOR_MACRO.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                index = putMacro();
            } else if (MATCHER_FOR_VARIABLE.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                lastTokenWasOperator = false;
                t = new Variable(realStartingPosition + MATCHER_FOR_VARIABLE.start(1), MATCHER_FOR_VARIABLE.group(1));
                t.put(postOrder);
                index = MATCHER_FOR_VARIABLE.end();
            } else if (MATCHER_FOR_NUMBER_LITERAL.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(realStartingPosition + index);
                lastTokenWasOperator = false;
                if (!numberSystemHash.containsKey(currentNumberSystem))
                    numberSystemHash.put(currentNumberSystem, new NumberSystem(currentNumberSystem));
                t = new NumberLiteral(realStartingPosition + MATCHER_FOR_NUMBER_LITERAL.start(1), UtilityMethods.parseInt(MATCHER_FOR_NUMBER_LITERAL.group(1)), numberSystemHash.get(currentNumberSystem));
                t.put(postOrder);
                index = MATCHER_FOR_NUMBER_LITERAL.end();
            } else if (MATCHER_FOR_ALPHABET_LETTER.find(index)) {
                if (!lastTokenWasOperator) throw ExceptionHelper.operatorMissing(index);
                lastTokenWasOperator = false;
                t = new AlphabetLetter(realStartingPosition + MATCHER_FOR_ALPHABET_LETTER.start(1), UtilityMethods.parseInt(MATCHER_FOR_ALPHABET_LETTER.group(1)));
                t.put(postOrder);
                index = MATCHER_FOR_ALPHABET_LETTER.end();
            } else if (MATCHER_FOR_NUMBER_SYSTEM.find(index)) {
                String tmp = deriveNumberSystem();
                numberSystems.push(tmp);
                currentNumberSystem = tmp;
                index = MATCHER_FOR_NUMBER_SYSTEM.end();
            } else if (MATCHER_FOR_LEFT_PARENTHESIS.find(index)) {
                op = new LeftParenthesis(realStartingPosition + index);
                op.put(postOrder, operatorStack);
                numberSystems.push("(");
                index = MATCHER_FOR_LEFT_PARENTHESIS.end();
            } else if (MATCHER_FOR_RIGHT_PARENTHESIS.find(index)) {
                op = new RightParenthesis(realStartingPosition + index);
                op.put(postOrder, operatorStack);
                currentNumberSystem = findCurrentNumberSystem(numberSystems);
                index = MATCHER_FOR_RIGHT_PARENTHESIS.end();
            } else if (MATCHER_FOR_WHITESPACE.find(index)) {
                index = MATCHER_FOR_WHITESPACE.end();
            } else {
                throw ExceptionHelper.undefinedToken(realStartingPosition + index);
            }
        }

        while (!operatorStack.isEmpty()) {
            op = operatorStack.pop();
            if (op.isLeftParenthesis()) {
                throw ExceptionHelper.unbalancedParen(op.getPositionInPredicate());
            } else {
                postOrder.add(op);
            }
        }
    }

    private String findCurrentNumberSystem(Stack<String> numberSystems) {
        String currentNumberSystem = defaultNumberSystem;
        while (!numberSystems.isEmpty()) {
            if (numberSystems.pop().equals("(")) {
                Stack<String> tmp = new Stack<>();
                while (!numberSystems.isEmpty()) {
                    tmp.push(numberSystems.pop());
                    if (!tmp.peek().equals("(")) {
                        currentNumberSystem = tmp.peek();
                        break;
                    }
                }
                while (!tmp.isEmpty()) {
                    numberSystems.push(tmp.pop());
                }
                break;
            }
        }
        return currentNumberSystem;
    }

    private int handleQuantifier() {
        String[] listOfVars = MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES.group(1).split("(\\s|,)+");
        Operator op = new LogicalOperator(MATCHER_FOR_LOGICAL_OPERATORS.start(), MATCHER_FOR_LOGICAL_OPERATORS.group(1), listOfVars.length);
        op.put(postOrder, operatorStack);
        for (String var : listOfVars) {
            Token t = new Variable(MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES.start(), var);
            t.put(postOrder);
        }
        return MATCHER_FOR_LIST_OF_QUANTIFIED_VARIABLES.end();
    }

    private String deriveNumberSystem() {
        if (MATCHER_FOR_NUMBER_SYSTEM.group(2) != null) return MATCHER_FOR_NUMBER_SYSTEM.group(2);
        if (MATCHER_FOR_NUMBER_SYSTEM.group(5) != null) return "msd_" + MATCHER_FOR_NUMBER_SYSTEM.group(5);
        if (MATCHER_FOR_NUMBER_SYSTEM.group(8) != null) return MATCHER_FOR_NUMBER_SYSTEM.group(8) + "_2";
        if (MATCHER_FOR_NUMBER_SYSTEM.group(9) != null) return "msd_" + MATCHER_FOR_NUMBER_SYSTEM.group(9);
        return "msd_2";
    }

    private int putWord(String defaultNumberSystem, boolean withDelimiter) {
        Matcher matcher = MATCHER_FOR_WORD;
        if (withDelimiter) {
            matcher = MATCHER_FOR_WORD_WITH_DELIMITER;
        }

        String r_leftBracket = "\\G\\s*\\[";
        Pattern p_leftBracket = Pattern.compile(r_leftBracket);
        Matcher m_leftBracket = p_leftBracket.matcher(predicate);

        Automaton A = new Automaton(UtilityMethods.get_address_for_words_library() + matcher.group(1) + ".txt");

        Stack<Character> bracketStack = new Stack<>();
        bracketStack.push('[');
        int i = matcher.end();
        List<Predicate> indices = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int startingPosition = i;
        while (i < predicate.length()) {
            char ch = predicate.charAt(i);
            if (ch == ']') {
                if (bracketStack.isEmpty())
                    throw new RuntimeException("unbalanced bracket: chat at " + (realStartingPosition + i));
                bracketStack.pop();
                if (bracketStack.isEmpty()) {
                    indices.add(new Predicate(defaultNumberSystem, buf.toString(), realStartingPosition + startingPosition));
                    buf = new StringBuilder();
                    if (m_leftBracket.find(i + 1)) {
                        bracketStack.push('[');
                        i = m_leftBracket.end();
                        startingPosition = i;
                        continue;
                    } else {
                        break;
                    }
                } else
                    buf.append(']');
            } else {
                buf.append(ch);
                if (ch == '[') bracketStack.push('[');
            }
            i++;
        }
        for (Predicate p : indices) {
            List<Token> tmp = p.getPostOrder();
            if (tmp.isEmpty())
                throw new RuntimeException("index " + (indices.indexOf(p) + 1) + " of the word " + matcher.group(1) + " cannot be empty: char at " + matcher.start(1));
            postOrder.addAll(tmp);
        }
        Word w = new Word(realStartingPosition + matcher.start(1), matcher.group(1), A, indices.size());
        w.put(postOrder);
        return i + 1;
    }

    private int putFunction(String defaultNumberSystem) {
        Matcher matcher = MATCHER_FOR_FUNCTION;
        Automaton A = new Automaton(UtilityMethods.get_address_for_automata_library() + matcher.group(1) + ".txt");

        Stack<Character> parenthesisStack = new Stack<>();
        parenthesisStack.push('(');
        int i = matcher.end();
        List<Predicate> arguments = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int startingPosition = i;
        while (i < predicate.length()) {
            char ch = predicate.charAt(i);
            if (ch == '#' || ch == '$') {
                throw ExceptionHelper.internalMacro(realStartingPosition + i);
            }
            if (ch == ')') {
                if (parenthesisStack.isEmpty())
                    throw ExceptionHelper.unbalancedParen(realStartingPosition + i);
                parenthesisStack.pop();
                if (parenthesisStack.isEmpty()) {
                    arguments.add(new Predicate(defaultNumberSystem, buf.toString(), realStartingPosition + startingPosition));
                    break;
                }
                buf.append(')');
            } else if (ch == ',') {
                if (parenthesisStack.size() != 1)
                    throw ExceptionHelper.unbalancedParen(realStartingPosition + i);
                arguments.add(new Predicate(defaultNumberSystem, buf.toString(), realStartingPosition + startingPosition));
                buf = new StringBuilder();
                startingPosition = i + 1;
            } else {
                buf.append(ch);
                if (ch == '(') {
                    parenthesisStack.push('(');
                }
            }
            i++;
        }
        if (arguments.size() == 1 && arguments.get(0).getPostOrder().isEmpty()) {
            arguments.remove(0);
        }
        for (Predicate p : arguments) {
            List<Token> tmp = p.getPostOrder();
            if (tmp.isEmpty() && arguments.size() > 1)
                throw new RuntimeException("argument " + (arguments.indexOf(p) + 1) + " of the function " + matcher.group(1) + " cannot be empty: char at " + matcher.start(1));
            postOrder.addAll(tmp);
        }
        Function f = new Function(defaultNumberSystem, realStartingPosition + matcher.start(1), matcher.group(1), A, arguments.size());
        f.put(postOrder);
        return i + 1;
    }

    private int putMacro() {
        Matcher matcher = MATCHER_FOR_MACRO;

        StringBuilder macro = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    UtilityMethods.get_address_for_macro_library() + matcher.group(2) + ".txt"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                macro.append(line);
            }
        } catch (IOException e) {
            LOGGER.catching(e);
            throw new RuntimeException("Macro does not exist: " + matcher.group(2), e);
        }
        Stack<Character> parenthesisStack = new Stack<>();
        parenthesisStack.push('(');
        int i = matcher.end();
        List<String> arguments = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        while (i < predicate.length()) {
            char ch = predicate.charAt(i);
            if (ch == '#' || ch == '$') {
                throw ExceptionHelper.internalMacro(realStartingPosition + i);
            }
            if (ch == ')') {
                if (parenthesisStack.isEmpty())
                    throw ExceptionHelper.unbalancedParen(realStartingPosition + i);
                parenthesisStack.pop();
                if (parenthesisStack.isEmpty()) {
                    arguments.add(buf.toString());
                    break;
                }
                buf.append(')');
            } else if (ch == ',') {
                if (parenthesisStack.size() != 1)
                    throw ExceptionHelper.unbalancedParen(realStartingPosition + i);
                arguments.add(buf.toString());
                buf = new StringBuilder();
            } else {
                buf.append(ch);
                if (ch == '(') {
                    parenthesisStack.push('(');
                }
            }
            i++;
        }
        for (int arg = arguments.size() - 1; arg >= 0; arg--) {
            macro = new StringBuilder(macro.toString().replaceAll("%" + arg, arguments.get(arg)));
        }
        predicate = predicate.substring(0, matcher.start()) + matcher.group(1) + macro + predicate.substring(i + 1);
        initializeMatchers();
        return matcher.start();
    }

    public List<Token> getPostOrder() {
        return postOrder;
    }

    public String toString() {
        return UtilityMethods.genericListString(postOrder, ":");
    }
}
