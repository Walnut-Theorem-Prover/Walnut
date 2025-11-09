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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;

import Automata.Automaton;
import Automata.Writer.AutomatonMatrixWriter;
import Automata.Writer.MatrixEmitter;
import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.Expression;
import Main.EvalComputations.Token.LogicalOperator;
import Main.EvalComputations.Token.Operator;
import Main.EvalComputations.Token.Token;

/**
 * This is used in eval/def commands to compute the predicate.
 */
public class EvalComputer {
  Expression result;
  private final StringBuilder log;
  private final StringBuilder logDetails;
  private final boolean printStepsOrDetails;
  private final boolean printDetails;

  public EvalComputer(boolean printSteps, boolean printDetails) {
    this.log = new StringBuilder();
    this.logDetails = new StringBuilder();
    this.printStepsOrDetails = printSteps || printDetails;
    this.printDetails = printDetails;
  }

  public String toString() {
    return result.toString();
  }

  public String getLogDetails() { return printDetails ? logDetails.toString() : ""; }

  void compute(Predicate predicate) {
    Stack<Expression> expressions = new Stack<>();
    List<Token> postOrder = predicate.getPostOrder();
    String prefix = "";
    long timeBeginning = System.currentTimeMillis();
    String step;

    // TODO: make this strongly-typed.
    boolean earlyTermination =
        Prover.earlyExistTermination && postOrder.get(postOrder.size() - 1).toString().equals(Operator.EXISTS);

    for (int i = 0; i < postOrder.size(); i++) {
      Token t = postOrder.get(i);
      try {
        long timeBefore = System.currentTimeMillis();
        if (!earlyTermination || i < postOrder.size() - 1) {
          t.act(expressions, printDetails, prefix, logDetails);
        } else {
          // Special-case: the last operand is "E" and we have an earlyExistTermination metacommand
          // return early without determinizing
          if (!(t instanceof LogicalOperator)) {
            throw WalnutException.unexpectedOperator(t.toString());
          }
          ((LogicalOperator) t).actExistsSpecialCase(expressions, printDetails, prefix, logDetails);
          break;
        }
        long timeAfter = System.currentTimeMillis();
        Expression nextExpression = expressions.peek();
        if (t.isOperator() && nextExpression instanceof AutomatonExpression) {
          step = prefix + nextExpression + ":" +
              nextExpression.M.fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
          log.append(step).append(System.lineSeparator());
          logDetails.append(step).append(System.lineSeparator());
          if (printStepsOrDetails) {
            System.out.println(step);
          }
          prefix += " ";
        }
      } catch (RuntimeException e) {
        Logging.printTruncatedStackTrace(e);
        String message = e.getMessage();
        message += System.lineSeparator() + "\t: char at " + t.getPositionInPredicate();
        throw new WalnutException(message);
      }
    }

    long timeEnd = System.currentTimeMillis();
    step = "Total computation time: " + (timeEnd - timeBeginning) + "ms.";
    log.append(step);
    logDetails.append(step);
    if (printStepsOrDetails) {
      System.out.println(step);
    }

    if (expressions.size() > 1) {
      StringBuilder message =
          new StringBuilder("Cannot evaluate the following into a single automaton:" +
              System.lineSeparator());
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

  List<String> writeAutomata(
      String predicateStr, String evalName, String freeVarStr, String resultName)
      throws IOException {
    Automaton M = result.M;
    M.writeAutomata(predicateStr, Session.getWriteAddressForAutomataLibrary(), evalName, false);

    List<String> freeVariables = determineFreeVariables(freeVarStr);
    if (!freeVariables.isEmpty()) {
      AutomatonMatrixWriter.writeAll(M, resultName, freeVariables);
    }

    writeLogs(resultName);

    if (M.fa.isTRUE_FALSE_AUTOMATON()) {
      System.out.println("____\n" + M.fa.trueFalseString().toUpperCase());
    }

    List<String> matrixAddresses = new ArrayList<>();
    if (!freeVariables.isEmpty()) {
      System.out.println("Matrix files:");
      for (MatrixEmitter.EmitterSpec emitterSpec : AutomatonMatrixWriter.EMITTERS) {
        System.out.println("  " + emitterSpec.intro() + ": " + resultName + emitterSpec.extension());
        matrixAddresses.add(resultName + emitterSpec.extension());
      }
    }
    return matrixAddresses;
  }

  void writeLogs(String resultName) throws IOException {
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(resultName + "_log.txt")))) {
      out.write(log.toString());
    }
    if (printDetails) {
      try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(resultName + "_detailed_log.txt")))) {
        out.write(logDetails.toString());
      }
    }
  }

  static List<String> determineFreeVariables(String freeVariablesStr) {
    List<String> freeVariables = new ArrayList<>();
    if (freeVariablesStr != null) {
      Matcher m1 = Prover.PAT_FOR_A_FREE_VARIABLE_IN_eval_def_CMDS.matcher(freeVariablesStr);
      while (m1.find()) {
        String t = m1.group();
        freeVariables.add(t);
      }
    }
    return freeVariables;
  }
}
