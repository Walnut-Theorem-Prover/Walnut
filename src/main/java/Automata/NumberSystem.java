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

package Automata;

import java.io.File;
import java.util.*;

import Automata.FA.FA;
import Main.ExceptionHelper;
import Main.Session;
import Main.UtilityMethods;
import Token.RelationalOperator;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * The class NumberSystem represents a number system.<br>
 * A number system consists of the following four: <br>
 * - a rule to represent non-negative numbers, and hence an alphabet <br>
 * - a rule for addition <br>
 * - a rule for comparison, and by comparison we mean equality and less than testing.<br>
 * - a flag that determines whether numbers are represented in most significant digit order or least significant digit.<br>
 * For example msd_3 is a number system. It is the number system in which <br>
 * - numbers are represented in base 3 in most significant digit first. Hence the alphabet is {0,1,2}.<br>
 * - the rule for addition, is a simple two states automaton, that gets 3-tuples (a,b,c) in base three and
 * accepts iff c=a+b.<br>
 * - the rule for comparison is a simple two state automaton, that gets 2-tuples (a,b) in base three and accepts
 * iff a < b. <br>
 * - we already mentioned that numbers are in most significant digit first (msd) in this number system. <br>
 * <p>
 * We mandate that 0 and 1 belong to the alphabet of every number systems. In addition, we require that
 * 0* represent the additive identity in all number systems. We also mandate that either
 * 0*1 or 10* (depending on msd/lsd) represent multiplicative
 * identity in all number systems.<br>
 * <p>
 * From here on by rule we mean a finite automaton.
 * If the users want to create a new number system, they, at least, have to provide the rule for addition
 * (in the Custom Bases directory).
 * They can further provide the rule for less than. If less than rule is not provided, we use the lexicographic ordering
 * on integers to create an automaton for less than testing. So for example if the alphabet is {-2,0,7} then in lexicographic order, we have -270 < 0-270-2.<br>
 * Rule for equality is always the rule for word equalities, i.e., two numbers are equal if the words representing
 * them are equal.<br>
 * Rules for base n already exist in the system for every n>1. However the user can override them. <br><br>
 * <p>
 * <p>
 * VERY IMPORTANT: ALL PRIVATE METHODS WHICH RETURN AUTOMATON MUST BE USED WITH CAUTION. THEIR RETURNED AUTOMATON
 * SHOULD NOT BE ALTERED. IF YOU WANT TO ALTER THEIR RETURNED VALUE, THEN YOU HAVE GOT TO MAKE A CLONE AND DO THE
 * MODIFICATION ON THE CLONE.
 */
public class NumberSystem {
    /**
     * Examples: msd_2, lsd_3, lsd_fib, ...
     */
    private final String name;

    /**
     * is_msd is used to determine which of first or last digit is the most significant digit. It'll be used when we
     * call quantify method, and also in many other places.
     */
    private final boolean isMsd;

    /**
     * is_neg is used to determine whether the base is negative.
     */
    private boolean is_neg;

    /**
     * Automata for addition, lessThan, and equal<br>
     * -addition has three inputs, and it accepts
     * iff the third is the sum of the first two. So the input is ordered!<br>
     * -lessThan has two inputs, and it accepts iff the first
     * one is less than the second one. So the input is ordered!<br>
     * -equal has two inputs, and it accepts iff they are equal.
     * -baseChange is defined if the number system has a corresponding comparable negative number system. * Moreover,
     * baseChange must be initialized manually. comparison_neg accepts inputs x,y if and only if x represents in the
     * positive base the same non-negative integer as y does in the negative base.
     */
    private Automaton addition;
    private Automaton lessThan;
    public Automaton equality;
    public Automaton baseChange;
    private Automaton allRepresentations;

    /**
     * Used to compute constant(n),multiplication(n),division(n) with dynamic programming.
     * Because these three methods are time-consuming, we would like to cache their results in three HashMaps.
     * For example:<br>
     * constantsDynamicTable.get(4) is the automaton that has a single input, and accepts if that input equals 4.<br>
     * multiplicationsDynamicTable(3) is the automaton that gets two inputs, and accepts if the second is 3 times the first. So the input is ordered!<br>
     * divisionsDynamicTable(5) is the automaton that gets two inputs, and accepts if the second is one-third of the first. So the input is ordered!<br>
     */
    private final Map<Integer, Automaton> constantsDynamicTable;
    private final Map<Integer, Automaton> multiplicationsDynamicTable;
    private final Map<Integer, Automaton> divisionsDynamicTable;

    private boolean flagUseAllRepresentations = true;

    // flip the number system from msd to lsd and vice versa.
    static void flipNS(List<NumberSystem> numberSystems) {
        for (int i = 0; i < numberSystems.size(); i++) {
            NumberSystem NS = numberSystems.get(i);
            if (NS == null) {
                continue;
            }
            int indexOfUnderscore = NS.getName().indexOf("_");
            String msd_or_lsd = NS.getName().substring(0, indexOfUnderscore);
            String suffix = NS.getName().substring(indexOfUnderscore);
            String newName = (msd_or_lsd.equals("msd") ? "lsd" : "msd") + suffix;
            numberSystems.set(i, new NumberSystem(newName));
        }
    }

    static boolean isNSDiffering(
        List<NumberSystem> NNS, List<NumberSystem> firstNS, List<List<Integer>> A1, List<List<Integer>> A2) {
        if (NNS.size() != firstNS.size()) {
            return true;
        }
        for (int j = 0; j < NNS.size(); j++) {
            NumberSystem Nj = NNS.get(j);
            NumberSystem firstJ = firstNS.get(j);
            if ((Nj == null && firstJ != null) || (Nj != null && firstJ == null) ||
                (Nj != null && firstJ != null &&
                    !NNS.get(j).getName().equals(firstJ.getName())) || !A1.equals(A2)) {
                return true;
            }
        }
        return false;
    }

  static Boolean determineMsd(List<NumberSystem> numberSystems) {
        boolean isMsd = true;
        boolean flag = false;
        for (NumberSystem ns : numberSystems) {
            if (ns == null)
                return null;
            if (flag && (ns.isMsd() != isMsd))
                return null;
            isMsd = ns.isMsd();
            flag = true;
        }
        return isMsd;
    }

    NumberSystem determineNegativeNS() {
        NumberSystem negativeNumberSystem;
        if (is_neg) {
            negativeNumberSystem = this;
        } else {
            String msd_or_lsd = name.substring(0, name.indexOf("_"));
            String base = name.substring(name.indexOf("_") + 1);
            negativeNumberSystem = new NumberSystem(msd_or_lsd + "_neg_" + base);
        }
        negativeNumberSystem.setBaseChangeAutomaton();
        return negativeNumberSystem;
    }

    public boolean isMsd() {
        return isMsd;
    }

    public String getName() {
        return name;
    }

    public boolean useAllRepresentations() {
        return flagUseAllRepresentations;
    }

    public List<Integer> getAlphabet() {
        return addition.getA().get(0);
    }

    public Automaton getAllRepresentations() {
        return allRepresentations;
    }

    public NumberSystem(String name) {
        this.name = name;
        String msd_or_lsd = name.substring(0, name.indexOf("_"));
        isMsd = msd_or_lsd.equals("msd");
        is_neg = name.contains("neg");
        String base = name.substring(name.indexOf("_") + 1);

        setAdditionAutomaton(name, base);
        setLessThanAutomaton(name, base);
        setEqualityAutomaton(getAlphabet());
        setAllRepAutomaton(name, base);

        constantsDynamicTable = new HashMap<>();
        multiplicationsDynamicTable = new HashMap<>();
        divisionsDynamicTable = new HashMap<>();
    }


    /**
     * Tries to create an Automaton from the main file path.
     * If it does not exist, tries the complement file path and reverses.
     * Otherwise, returns null
     */
    private Automaton loadAutomatonOrNull(String name, String extension, String base) {
        // When the number system does not exist, we try to see whether its complement exists or not.
        // For example lsd_2 is the complement of msd_2.
        String mainName = Session.getReadAddressForCustomBases(name + extension);
        String complementName = Session.getReadAddressForCustomBases((isMsd ? "lsd" : "msd") + "_" + base + extension);
        File fMain = new File(mainName);
        File fComplement = new File(complementName);
        if (fMain.isFile()) {
            return new Automaton(mainName);
        } else if (fComplement.isFile()) {
            Automaton A = new Automaton(complementName);
            AutomatonLogicalOps.reverse(A, false, null, null, false);
            return A;
        }
        return null;
    }


    private void setAdditionAutomaton(
        String name, String base) {
        addition = loadAutomatonOrNull(name, "_addition.txt", base);
        if (addition == null) {
            if (UtilityMethods.isNumber(base) && Integer.parseInt(base) > 1) {
                addition = baseNadditionAutomaton(Integer.parseInt(base));
            } else if (UtilityMethods.parseNegNumber(base) > 1) {
                addition = baseNegNAddition(UtilityMethods.parseNegNumber(base));
            } else {
                throw new RuntimeException("Number system " + name + " is not defined.");
            }
            if (!isMsd) {
                AutomatonLogicalOps.reverse(addition, false, null, null, false);
            }
        }

        /**
         * The alphabet of all inputs of addition automaton must be equal. It must contain 0 and 1.
         * The addition automata must have 3 inputs.
         * All 3 inputs must be of type arithmetic.
         */
        if (addition.getA() == null || addition.getA().size() != 3) {
            throw new RuntimeException(
                    "The addition automaton must have exactly 3 inputs: base " + name);
        }

        if (!getAlphabet().contains(0)) {
            throw new RuntimeException(
                    "The input alphabet of addition automaton must contain 0: base " + name);
        }

        if (!getAlphabet().contains(1)) {
            throw new RuntimeException(
                    "The input alphabet of addition automaton must contain 1: base " + name);
        }

        for (int i = 1; i < addition.getA().size(); i++) {
            if (!UtilityMethods.areEqual(addition.getA().get(i), getAlphabet())) {
                throw new RuntimeException(
                        "All 3 inputs of the addition automaton " +
                                "must have the same alphabet: base " + name);
            }
        }

        for (int i = 0; i < addition.getA().size(); i++) {
            addition.getNS().set(i, this);
        }
    }

    private void setLessThanAutomaton(
        String name, String base) {
        lessThan = loadAutomatonOrNull(name, "_less_than.txt", base);
        if (lessThan == null) {
            if (UtilityMethods.parseNegNumber(base) > 1) {
                lessThan = baseNegNLessThan(UtilityMethods.parseNegNumber(base));
            } else {
                lessThan = lexicographicLessThan(getAlphabet());
            }
            if (!isMsd) {
                AutomatonLogicalOps.reverse(lessThan, false, null, null, false);
            }
        }
        /**
         * The lessThan automata must have 2 inputs.
         * All 2 inputs must be of type arithmetic.
         * Inputs must have the same alphabet as the addition automaton.
         */
        if (lessThan.getA() == null || lessThan.getA().size() != 2) {
            throw new RuntimeException(
                "The less_than automaton must have exactly 2 inputs: base " + name);
        }

        for (int i = 0; i < lessThan.getA().size(); i++) {
            if (!UtilityMethods.areEqual(lessThan.getA().get(i), getAlphabet())) {
                throw new RuntimeException(
                    "Inputs of the less_than automaton must have the same alphabet " +
                        "as the alphabet of inputs of addition automaton: base " + name);
            }

            lessThan.getNS().set(i, this);
        }
    }

    private void setAllRepAutomaton(
        String name, String base) {
        //the set of all representations
        allRepresentations = loadAutomatonOrNull(name, ".txt", base);
        if (allRepresentations == null) {
            flagUseAllRepresentations = false;
        } else {
            for (int i = 0; i < allRepresentations.getNS().size(); i++) {
                allRepresentations.getNS().set(i, this);
            }
            applyAllRepresentations();
        }
    }

    /**
     * Initializes equality. equality has two inputs, and accepts iff the two inputs are equal.
     *
     * @param alphabet
     */
    private void setEqualityAutomaton(List<Integer> alphabet) {
        equality = initBasicAutomaton(IntList.of(1), 2, alphabet);
        FA equalityFA = equality.getFa();
        for (int i = 0; i < alphabet.size(); i++) {
            equalityFA.addNewTransition(0, 0, i * alphabet.size() + i);
        }
    }

    /**
     * Initializes lessThan to lexicographic lessThan. lessThan has two inputs, and it accepts iff the first
     * one is less than the second one. So the input is ordered!
     *
     * @param alphabet
     */
    private Automaton lexicographicLessThan(List<Integer> alphabet) {
        alphabet = new ArrayList<>(alphabet);
        Collections.sort(alphabet);
        Automaton lessThan = initBasicAutomaton(IntList.of(0,1), 2, alphabet);
        FA lessThanFA = lessThan.getFa();
        for (int i = 0; i < alphabet.size(); i++) {
            for (int j = 0; j < alphabet.size(); j++) {
                if (i == j) {
                    lessThanFA.addNewTransition(0, 0, j * alphabet.size() + i);
                }
                if (i < j) {
                    lessThanFA.addNewTransition(0, 1, j * alphabet.size() + i);
                }
                lessThanFA.addNewTransition(1, 1, i * alphabet.size() + j);
            }
        }
        return lessThan;
    }

    /**
     * Initializes equality of the positive base and negative base if not already set. Equality has two inputs (a,b),
     * and it accepts iff a in the positive base equals b in the negative base. The current number system can be either
     * the postive or negative one. This is not initialized for all number systems by default. You should call this
     * function to initialize as required. If no base_change file is found in the custom bases, we leave the baseChange
     * automaton unset.
     *
     */
    private void setBaseChangeAutomaton() {
        if (baseChange != null) return;
        String base = name.substring(name.indexOf("_") + 1);
        baseChange = loadAutomatonOrNull(
            is_neg ? name : name.substring(0, name.indexOf("_")), "_base_change.txt", base);
        if (baseChange == null) {
            if (UtilityMethods.parseNegNumber(base) > 1) {
                baseChange = baseNBaseChange(UtilityMethods.parseNegNumber(base));
                if (isMsd) {
                    AutomatonLogicalOps.reverse(baseChange, false, null, null, false);
                }
            }
            if (baseChange == null) {
                throw new RuntimeException("Number system cannot be compared.");
            }
        }
        baseChange.applyAllRepresentations();
    }

    private void applyAllRepresentations() {
        addition.applyAllRepresentations();
        lessThan.applyAllRepresentations();
        equality.applyAllRepresentations();
    }

    /**
     * Initializes addition to base n addition. addition has three inputs, and it accepts
     * iff the third is the sum of the first two. So the input is ordered!
     *
     * @param n
     */
    private Automaton baseNadditionAutomaton(int n) {
        List<Integer> alphabet = buildAlphabet(n);
        Automaton addition = initBasicAutomaton(IntList.of(1,0), 3, alphabet);
        FA additionFA = addition.getFa();
        int l = 0;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    if (i + j == k) {
                        additionFA.addNewTransition(0, 0, l);
                    }
                    if (i + j + 1 == k) {
                        additionFA.addNewTransition(0, 1, l);
                    }
                    if (i + j + 1 == k + n) {
                        additionFA.addNewTransition(1, 1, l);
                    }
                    if (i + j == k + n) {
                        additionFA.addNewTransition(1, 0, l);
                    }
                    l++;
                }
            }
        }
        return addition;
    }

    private static List<Integer> buildAlphabet(int n) {
        List<Integer> alphabet = new ArrayList<>(n);
        for (int i = 0; i < n; i++) alphabet.add(i);
        return alphabet;
    }

    /**
     * Initializes addition to base negative n addition. addition has three inputs, and it accepts
     * iff the third is the sum of the first two. So the input is ordered!
     *
     * @param n
     */
    private Automaton baseNegNAddition(int n) {
        List<Integer> alphabet = buildAlphabet(n);
        Automaton addition =  initBasicAutomaton(IntList.of(1,0,0), 3, alphabet);
        FA additionFA = addition.getFa();
        int l = 0;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    if (i + j == k) {
                        additionFA.addNewTransition(0, 0, l);
                    }
                    if (i + j + 1 == k) {
                        additionFA.addNewTransition(0, 1, l);
                    }
                    if (i + j - 1 == k) {
                        additionFA.addNewTransition(0, 2, l);
                    }
                    if (i + j == k + n) {
                        additionFA.addNewTransition(2, 0, l);
                    }
                    if (i + j + 1 == k + n) {
                        additionFA.addNewTransition(2, 1, l);
                    }
                    if (i + j - 1 == k + n) {
                        additionFA.addNewTransition(2, 2, l);
                    }
                    if (i == 0 && j == 0 && k == n - 1) {
                        additionFA.addNewTransition(1, 2, l);
                    }
                    l++;
                }
            }
        }
        return addition;
    }

    /**
     * Initializes lessThan to base negative n lessThan. less_than has two inputs, and it accepts
     * iff the first is less than the second. So the input is ordered!
     *
     * @param n
     */
    private Automaton baseNegNLessThan(int n) {
        List<Integer> alphabet = buildAlphabet(n);
        Automaton lessThan = initBasicAutomaton(IntList.of(0,1,0), 2, alphabet);
        FA lessThanFA = lessThan.getFa();
        int l = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (i == j) {
                    lessThanFA.addNewTransition(0, 0, l);
                }
                if (i < j) {
                    lessThanFA.addNewTransition(0, 1, l);
                }
                if (j < i) {
                    lessThanFA.addNewTransition(0, 2, l);
                }
                lessThanFA.addNewTransition(1, 2, l);
                lessThanFA.addNewTransition(2, 1, l);
                l++;
            }
        }
        return lessThan;
    }

    /**
     * Initializes equality of base n and base -n. Equality has two inputs (a,b), and it accepts
     * iff [a]_n = [b]_-n (a is a base n representation and b is a base -n representation of
     * the same integer).
     *
     * @param n
     */
    private Automaton baseNBaseChange(int n) {
        List<Integer> alphabet = buildAlphabet(n);
        Automaton baseChange = initBasicAutomaton(IntList.of(1,1,0,0));
        if (isMsd) {
            baseChange.getNS().add(new NumberSystem("msd_" + n));
            baseChange.getNS().add(new NumberSystem("msd_neg_" + n));
        } else {
            baseChange.getNS().add(new NumberSystem("lsd_" + n));
            baseChange.getNS().add(new NumberSystem("lsd_neg_" + n));
        }
        baseChange.getA().add(new ArrayList<>(alphabet));
        baseChange.getA().add(alphabet);
        baseChange.determineAlphabetSizeFromA();
        FA baseChangeFA = baseChange.getFa();
        int l = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (i == 0 && j == 0) {
                    baseChangeFA.addNewTransition(1, 0, l);
                }
                if (i == j) {
                    baseChangeFA.addNewTransition(0, 1, l);
                }
                if (i + 1 == j) {
                    baseChangeFA.addNewTransition(2, 1, l);
                }
                if (i + j == n) {
                    baseChangeFA.addNewTransition(1, 2, l);
                }
                if (i + j == n - 1) {
                    baseChangeFA.addNewTransition(3, 2, l);
                }
                if (i == n - 1 && j == 0) {
                    baseChangeFA.addNewTransition(2, 3, l);
                }
                l++;
            }
        }
        return baseChange;
    }

    private static Automaton initBasicAutomaton(IntList O) {
        Automaton a = new Automaton();
        a.getFa().initBasicFA(O);
        return a;
    }
    private Automaton initBasicAutomaton(IntList O, int inputSize, List<Integer> alphabet) {
        Automaton a = initBasicAutomaton(O);
        for(int i=0;i<inputSize;i++) {
            a.getNS().add(this);
            a.getA().add(new ArrayList<>(alphabet));
        }
        a.determineAlphabetSizeFromA();
        return a;
    }

    /**
     * @param n
     * @return an automaton that accepts only n.
     * If n < 0 and the current number system does not contain negative numbers, then we always return
     * the false automata. So BE CAREFUL when calling on n < 0.
     */
    public Automaton get(int n) {
        return constant(n).clone();
    }

    public Automaton getDivision(int n) {
        return division(n).clone();
    }

    public Automaton getMultiplication(int n) {
        return multiplication(n).clone();
    }

    public String toString() {
        return name;
    }

    private Automaton applyComparison(Automaton base, String a, String b, boolean reverse, boolean negate) {
        Automaton result = base.clone();
        result.bind(reverse ? List.of(b,a) : List.of(a,b));
        if (negate) AutomatonLogicalOps.not(result, false, null, null);
        return result;
    }

    /**
     * @param a
     * @param b
     * @param comparisonOperator can be any of "<",">","<=",">=","=","!="
     * @return an Automaton with two inputs, with labels a and b. It accepts iff a comparisonOperator b.
     * Note that the order of inputs, in the resulting automaton, is not guaranteed to be either (a,b) or (b,a).
     * So the input is not ordered!
     */
    public Automaton comparison(String a, String b, String comparisonOperator) {
      return switch (comparisonOperator) {
            case "<" -> applyComparison(lessThan, a, b, false, false);
            case ">" -> applyComparison(lessThan, a, b, true, false);
            case "=" -> applyComparison(equality, a, b, false, false);
            case "!=" -> applyComparison(equality, a, b, false, true);
            case ">=" -> applyComparison(lessThan, a, b, false, true);
            case "<=" -> applyComparison(lessThan, a, b, true, true);
            default -> throw new RuntimeException("undefined comparison operator:" + comparisonOperator);
        };
    }

    /**
     * @param a
     * @param b                  a non negative integer
     * @param comparisonOperator can be any of "<",">","<=",">=","=","!="
     * @return an Automaton with single input, with label = [a]. It accepts iff a comparisonOperator b.
     */
    public Automaton comparison(String a, int b, String comparisonOperator) {
        validateNeg(b);
        String B = "new " + a;//this way, we make sure B != a.
        Automaton N, M;
        if (b < 0) {
            M = arithmetic(a, -b, B, "+");
            N = comparison(B, 0, comparisonOperator);
        } else { // b >= 0
            N = get(b);
            if (comparisonOperator.equals("=")) {
                N.bind(List.of(a));
                return N;
            } else if (comparisonOperator.equals("!=")) {
                N.bind(List.of(a));
                AutomatonLogicalOps.not(N, false, null, null);
                return N;
            }
            N.bind(List.of(B));
            M = comparison(a, B, comparisonOperator);
        }
        M = AutomatonLogicalOps.and(M, N, false, null, null);
        AutomatonLogicalOps.quantify(M, B, false, null, null);
        return M;
    }

    /**
     * @param a                  a non negative integer
     * @param b
     * @param comparisonOperator can be any of "<",">","<=",">=","=","!="
     * @return an Automaton with single input, with label = [b]. It accepts iff a comparisonOperator b.
     */
    public Automaton comparison(int a, String b, String comparisonOperator) {
        validateNeg(a);
        return comparison(b, a, RelationalOperator.reverseOperator(comparisonOperator));
    }

    /**
     * @param a
     * @param b
     * @param c
     * @param arithmeticOperator can be any of "+", "-","*","/"
     * @return an Automaton with three inputs with labels a,b,c. It accepts iff c = a arithmeticOperator b.
     * Note that the order of inputs, in the resulting
     * automaton, is not guaranteed to be in any fixed order like (a,b,c) or (c,b,a) ...
     * So the input is not ordered!
     */
    public Automaton arithmetic(
            String a,
            String b,
            String c,
            String arithmeticOperator) {
        Automaton M = addition.clone();
        switch (arithmeticOperator) {
            case "+":
                M.bind(List.of(a, b, c));
                break;
            case "-":
                M.bind(List.of(b, c, a));
                break;
            case "*", "/":
                throw ExceptionHelper.operatorTwoVariables(arithmeticOperator);
            default:
                throw new RuntimeException("undefined arithmetic operator:" + arithmeticOperator);
        }
        return M;
    }

    /**
     * @param a
     * @param b                  an integer
     * @param c
     * @param arithmeticOperator can be any of "+","-","*","/"
     * @return an Automaton with two inputs, with labels a and c. It accepts iff c = a arithmeticOperator b.
     * Note that the order of inputs, in the resulting
     * automaton, is not guaranteed to be in any fixed order like [a,c] or [c,a].
     * So the input is not ordered!
     */
    public Automaton arithmetic(
            String a,
            int b,
            String c,
            String arithmeticOperator) {
        validateNeg(b);
        Automaton N;
        if (arithmeticOperator.equals("*")) {
            //note that the case of b = 0 is handled in Computer class
            N = getMultiplication(b);
            N.bind(List.of(a, c));
            return N;
        }
        if (arithmeticOperator.equals("/")) {
            if (b == 0) throw ExceptionHelper.divisionByZero();
            N = getDivision(b);
            N.bind(List.of(a, c));
            return N;
        }

        Automaton M;
        String B = a + c; //this way we make sure that B is not equal to a or c
        if (b < 0) { // We rewrite "a-b=c" as "a+(-b)=c" and "a+b=c" as "a-(-b)=c"
            N = get(-b);
            N.bind(List.of(B));
            M = arithmetic(a, B, c, arithmeticOperator.equals("+") ? "-" : "+");
        } else { // b >= 0
            N = get(b);
            N.bind(List.of(B));
            M = arithmetic(a, B, c, arithmeticOperator);
        }
        M = AutomatonLogicalOps.and(M, N, false, null, null);
        AutomatonLogicalOps.quantify(M, B, false, null, null);
        return M;
    }

    /**
     * @param a                  an integer
     * @param b
     * @param c
     * @param arithmeticOperator can be any of "+","-","*","/"
     * @return an Automaton with two inputs, with labels b and c. It accepts iff c = a arithmeticOperator b.
     * Note that the order of inputs, in the resulting
     * automaton, is not guaranteed to be in any fixed order like [b,c] or [c,b].
     * So the input is not ordered!
     */
    public Automaton arithmetic(
            int a,
            String b,
            String c,
            String arithmeticOperator) {
        validateNeg(a);
        Automaton N;
        if (arithmeticOperator.equals("*")) {
            N = getMultiplication(a);
            N.bind(List.of(b, c));
            return N;
        }
        if (arithmeticOperator.equals("/"))
            throw new RuntimeException("constants cannot be divided by variables");

        Automaton M;
        String A = b + c; //this way we make sure that A is not equal to b or c
        if (a < 0 && arithmeticOperator.equals("+")) { // We rewrite "a+b=c" and "c+(-a)=b"
            N = get(-a);
            N.bind(List.of(A));
            M = arithmetic(c, A, b, arithmeticOperator);
        } else {
            // Notice "a-b=c" is false unless we are in a negative base
            // So we may call get(a) where a < 0
            N = get(a);
            N.bind(List.of(A));
            M = arithmetic(A, b, c, arithmeticOperator);
        }
        M = AutomatonLogicalOps.and(M, N, false, null, null);
        AutomatonLogicalOps.quantify(M, A, false, null, null);
        return M;
    }

    /**
     * @param a
     * @param b
     * @param c                  an integer
     * @param arithmeticOperator can be any of "+","-","*","/"
     * @return an Automaton with two inputs, with labels b and c. It accepts iff c = a arithmeticOperator b.
     * Note that the order of inputs, in the resulting
     * automaton, is not guaranteed to be in any fixed order like [a,b] or [b,a].
     * So the input is not ordered!
     */
    public Automaton arithmetic(
            String a,
            String b,
            int c,
            String arithmeticOperator) {
        validateNeg(c);
        if (arithmeticOperator.equals("*") || arithmeticOperator.equals("/")) {
            throw ExceptionHelper.operatorTwoVariables(arithmeticOperator);
        }

        Automaton N;
        Automaton M;
        String C = a + b; //this way we make sure that A is not equal to a or b
        if (c < 0 && arithmeticOperator.equals("-")) { // We rewrite "a-b=c" and "a+(-c)=b"
            N = get(-c);
            N.bind(List.of(C));
            M = arithmetic(a, C, b, arithmeticOperator);
        } else {
            // Notice "a+b=c" is false unless we are in a negative base
            // So we may call get(c) where c < 0
            N = get(c);
            N.bind(List.of(C));
            M = arithmetic(a, b, C, arithmeticOperator);
        }
        M = AutomatonLogicalOps.and(M, N, false, null, null);
        AutomatonLogicalOps.quantify(M, C, false, null, null);
        return M;
    }

    /**
     * @param n
     * @return an Automaton with one input. It accepts when the input equals n.
     */
    private Automaton constant(int n) {
        validateNeg(n);
        if (constantsDynamicTable.containsKey(n)) {
            return constantsDynamicTable.get(n);
        }

        Automaton P;
        String a = "a", b = "b", c = "c";
        if (n == 0) {
            P = makeZero();
        } else if (n == 1) {
            P = makeOne();
        } else if (n < 0) {
            // b = -n
            Automaton M = get(-n);
            M.bind(List.of(b));
            // Eb, a + b = 0 & b = -n
            P = arithmetic(a, b, 0, "+");
            P = AutomatonLogicalOps.and(P, M, false, null, null);
            AutomatonLogicalOps.quantify(P, b, false, null, null);
        } else { // n > 0
            // a = floor(n/2)
            Automaton M = get(n / 2);
            M.bind(List.of(a));
            // b = ceil(n/2)
            Automaton N = get(n / 2 + (n % 2 == 0 ? 0 : 1));
            N.bind(List.of(b));
            // Ea,Eb, a + b = c & a = floor(n/2) & b = ceil(n/2)
            P = arithmetic(a, b, c, "+");
            P = AutomatonLogicalOps.and(P, M, false, null, null);
            P = AutomatonLogicalOps.and(P, N, false, null, null);
            AutomatonLogicalOps.quantify(P, List.of(a, b), false, null, null);
        }
        constantsDynamicTable.put(n, P);
        return P;
    }

    /**
     * The returned automaton has two inputs, and it accepts iff the second is n times the first. So the input is ordered!
     *
     * @param n
     * @return
     */
    private Automaton multiplication(int n) {
        validateNeg(n);
        if (n == 0) throw new RuntimeException("multiplication(0)");
        if (multiplicationsDynamicTable.containsKey(n)) return multiplicationsDynamicTable.get(n);
        //note that the case of n==0 is handled in Computer class
        Automaton P;
        String a = "a", b = "b", c = "c", d = "d";
        if (n == 1) {
            P = equality;
        } else if (n < 0) {
            // c = (-n)*a
            Automaton M = getMultiplication(-n);
            M.bind(List.of(a, c));
            // Ec b + c = 0 & c = (-n)*a
            P = arithmetic(b, c, 0, "+");
            P = AutomatonLogicalOps.and(P, M, false, null, null);
            AutomatonLogicalOps.quantify(P, c, false, null, null);
            P.sortLabel();
        } else if (n == 2) {
            P = arithmetic(a, a, d, "+");
            P.sortLabel();
        } else { // n > 2
            // doubler
            Automaton D = getMultiplication(2);

            //b = k*a
            Automaton M = getMultiplication(n / 2);
            M.bind(List.of(a, b));

            if (n % 2 == 0) { // suppose n = 2k
                D.bind(List.of(b, d));
                P = AutomatonLogicalOps.and(M, D, false, null, null);
                AutomatonLogicalOps.quantify(P, b, false, null, null);
            } else { // n = 2k+1
                D.bind(List.of(b, c));
                P = arithmetic(c, a, d, "+");
                P = AutomatonLogicalOps.and(P, M, false, null, null);
                P = AutomatonLogicalOps.and(P, D, false, null, null);
                AutomatonLogicalOps.quantify(P, List.of(b, c), false, null, null);
            }

            P.sortLabel();
        }
        multiplicationsDynamicTable.put(n, P);
        return P;
    }

    private void validateNeg(int n) {
        if (!is_neg && n < 0) throw ExceptionHelper.negativeConstant(n);
    }

    /**
     * The returned automaton has two inputs, and it accepts iff the second is one nth of the first. So the input is ordered!
     *
     * @param n
     * @return
     */
    // a / n = b <=> Er,q a = q + r & q = n*b & n < r <= 0 if n < 0
    private Automaton division(int n) {
        validateNeg(n);
        if (n == 0) throw ExceptionHelper.divisionByZero();
        if (divisionsDynamicTable.containsKey(n)) return divisionsDynamicTable.get(n);
        String a = "a", b = "b", r = "r", q = "q";
        // We want to construct the following expressions
        // a / n = b <=> Er,q a = q + r & q = n*b & n < r <= 0 if n < 0
        // a / n = b <=> Er,q a = q + r & q = n*b & 0 <= r < n if n > 0
        Automaton M = arithmetic(q, r, a, "+");
        Automaton N = arithmetic(n, b, q, "*");

        // n < 0: n < r <= 0, n > 0: 0 <= r < n
        Automaton P1 = comparison(r, 0, n < 0 ? "<=" : ">=");
        Automaton P2 = comparison(r, n, n < 0 ? ">" : "<");

        Automaton P = AutomatonLogicalOps.and(P1, P2, false, null, null);
        Automaton R = AutomatonLogicalOps.and(M, N, false, null, null);
        R = AutomatonLogicalOps.and(R, P, false, null, null);
        AutomatonLogicalOps.quantify(R, List.of(q, r), false, null, null);
        R.sortLabel();
        divisionsDynamicTable.put(n, R);
        return R;
    }

    private Automaton makeZero() {
        return makeConstant("0*", 0);
    }

    private Automaton makeOne() {
        return makeConstant(isMsd ? "0*1" : "10*", 1);
    }

    private Automaton makeConstant(String regex, int constant) {
        List<Integer> alph = buildAlphabet(2);
        Automaton M = new Automaton(regex, alph, this);
        M.setA(new ArrayList<>());
        M.getA().add(new ArrayList<>(getAlphabet()));
        M.determineAlphabetSizeFromA();
        M.setEncoder(new ArrayList<>());
        M.getEncoder().add(1);
        M.canonize();
        constantsDynamicTable.put(constant, M);
        return M;
    }

    /**
     * Parse the base (k) from an Automaton's number system name, e.g. "msd_10" => 10.
     * Throws if base is invalid.
     */
    public int parseBase() {
        String baseStr = name.substring(name.indexOf("_") + 1);
        if (!UtilityMethods.isNumber(baseStr) || Integer.parseInt(baseStr) <= 1) {
            throw new RuntimeException("Base of automaton's number system must be > 1 and int, found: " + baseStr);
        }
        return Integer.parseInt(baseStr);
    }
}
