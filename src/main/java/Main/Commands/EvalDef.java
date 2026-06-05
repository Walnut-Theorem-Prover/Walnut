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

package Main.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Automata.Automaton;
import Automata.Writer.AutomatonMatrixWriter;
import Automata.Writer.MatrixEmitter;
import Main.*;
import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.Expression;
import Main.EvalComputations.Token.Token;

import static Main.Prover.RE_IDENTIFIER;
import static Main.TestCase.DEFAULT_TESTFILE;

/**
 * This is used in eval/def commands to compute the predicate.
 */
public class EvalDef {
  static final String REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = RE_IDENTIFIER;
  static final Pattern PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS = Pattern.compile(REXEXP_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS);

  public Expression result;
  public EvalDef(boolean printSteps, boolean printDetails) {
    Logging.configureForCommand(printSteps, printDetails);
  }

  public static TestCase evalDefCommand(
      boolean printFlag, boolean printDetails, String predicateStr, String evalName, String freeVarStr) {
    boolean headless = evalName == null || evalName.isBlank();

    // compute result based on predicate
    // if we wanted an "execution plan", it would be hooked in here
    EvalDef c = new EvalDef(printFlag, printDetails);
    if (headless) {
      return computeHeadless(c, predicateStr);
    }

    String resultName = Session.getAddressForResult() + evalName;
    try (Logging.CommandLogContext ignored = Logging.writeEvalLogsTo(resultName)) {
      Predicate predicate = new Predicate(predicateStr); // parse the predicates into an object
      c.compute(predicate);
      Automaton M = c.result.M;

      M.writeAutomata(predicateStr, Session.getWriteAddressForAutomataLibrary(), evalName, false);
      if (M.fa.isTRUE_FALSE_AUTOMATON()) {
        System.out.println("____\n" + M.fa.trueFalseString().toUpperCase());
      }

      List<String> matrixAddresses = writeMatrices(M, freeVarStr, resultName);
      return new TestCase(
          "", matrixAddresses, resultName + Prover.GV_EXTENSION, Logging.getDetailedLog(),
          List.of(new TestCase.AutomatonFilenamePair(M, DEFAULT_TESTFILE)));
    }
  }

  private static TestCase computeHeadless(EvalDef c, String predicateStr) {
    Predicate predicate = new Predicate(predicateStr); // parse the predicates into an object
    c.compute(predicate);
    Automaton M = c.result.M;

    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      System.out.println("____\n" + M.fa.trueFalseString().toUpperCase());
    }

    return new TestCase(
        "", List.of(), "", Logging.getDetailedLog(),
        List.of(new TestCase.AutomatonFilenamePair(M, DEFAULT_TESTFILE)));
  }

  public static Automaton getImageEval(String predicateStr, boolean printFlag) {
    Predicate p = new Predicate(predicateStr);
    // image is a final-result operation; do not preserve per-intermediate def logging.
    EvalDef c = new EvalDef(printFlag, false);
    c.compute(p);
    return c.result.M;
  }

  public String toString() {
    return result.toString();
  }

  private void compute(Predicate predicate) {
    Stack<Expression> expressions = new Stack<>();
    List<Token> postOrder = predicate.getPostOrder();
    long timeBeginning = System.currentTimeMillis();
    String step;

    for (Token t : postOrder) {
      try {
        long timeBefore = System.currentTimeMillis();
        t.act(expressions);
        long timeAfter = System.currentTimeMillis();
        Expression nextExpression = expressions.peek();
        if (t.isOperator() && nextExpression instanceof AutomatonExpression) {
          step = nextExpression + ":" +
              nextExpression.M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
          Logging.logEvaluationStep(step, false);
          Logging.indent();
        }
      } catch (RuntimeException e) {
        Logging.printTruncatedStackTrace(e);
        String message = e.getMessage();
        message += System.lineSeparator() + "\t: char at " + t.getPositionInPredicate();
        throw new WalnutException(message);
      }
    }

    long timeEnd = System.currentTimeMillis();
    Logging.resetIndent();
    Logging.logEvaluationStep("Total computation time: " + (timeEnd - timeBeginning) + "ms.", true);

    if (expressions.size() > 1) {
      StringBuilder message =
          new StringBuilder("Cannot evaluate the following into a single automaton:" + System.lineSeparator());
      Stack<Expression> tmp = new Stack<>();

      while (!expressions.isEmpty()) {
        tmp.push(expressions.pop());
      }

      while (!tmp.isEmpty()) {
        message.append(tmp.pop()).append(System.lineSeparator());
      }

      message.append("Probably some operators are missing.");
      throw new WalnutException(message.toString());
    } else if (expressions.isEmpty()) {
      throw new WalnutException("Evaluation ended in no result.");
    } else {
      result = expressions.pop();
      if (!(result instanceof AutomatonExpression)) {
        throw new WalnutException("The final result of the evaluation is not of type automaton");
      }
    }
  }

  private static List<String> writeMatrices(Automaton M, String freeVarStr, String resultName) {
    List<String> matrixAddresses = new ArrayList<>();
    List<String> freeVariables = determineFreeVariables(freeVarStr);
    if (!freeVariables.isEmpty()) {
      AutomatonMatrixWriter.writeAll(M, resultName, freeVariables);
      System.out.println("Matrix files:");
      for (MatrixEmitter.EmitterSpec emitterSpec : AutomatonMatrixWriter.EMITTERS) {
        System.out.println("  " + emitterSpec.intro() + ": " + resultName + emitterSpec.extension());
        matrixAddresses.add(resultName + emitterSpec.extension());
      }
    }
    return matrixAddresses;
  }

  private static List<String> determineFreeVariables(String freeVariablesStr) {
    List<String> freeVariables = new ArrayList<>();
    if (freeVariablesStr != null) {
      Matcher m1 = PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS.matcher(freeVariablesStr);
      while (m1.find()) {
        String t = m1.group();
        freeVariables.add(t);
      }
    }
    return freeVariables;
  }
}
