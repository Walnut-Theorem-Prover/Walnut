/*	 2025 John Nicol
 *
 *   This file is part of Walnut.
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
package Automata.Writer;

import Main.Prover;

import java.io.PrintWriter;
import java.io.Writer;

public final class SageEmitter implements MatrixEmitter {
  public static final String STR = "sage";
  private static final String COMMENT_CHAR = "#";
  public static final String EXTENSION = Prover.DOT + STR;
  private final PrintWriter out;
  private boolean firstRowOpen = false;

  public SageEmitter(Writer writer) {
    this.out = new PrintWriter(writer);
  }

  @Override
  public void begin() {
    out.println("# SageMath output");
  }

  @Override
  public void emitInitialRowVector(String name, int Q, int q0) {
    AutomatonMatrixWriter.writeInitialRowVectorComment(out, name, COMMENT_CHAR);
    out.print(name + " = matrix(ZZ, 1, " + Q + ", [");
    for (int q = 0; q < Q; ++q) {
      out.print(q == q0 ? "1" : "0");
      if (q < (Q - 1)) out.print(",");
    }
    out.println("])");
    out.println();
    AutomatonMatrixWriter.writeIncidenceMatricesComment(out, COMMENT_CHAR);
  }

  @Override
  public void beginMatrix(String name, int Q) {
    out.println();
    out.print(name + " = matrix(ZZ, " + Q + ", " + Q + ", [");
    out.print("[");               // open first row
    firstRowOpen = true;          // we have one row bracket open
  }

  @Override
  public void emitRow(int[] row) {
    if (!firstRowOpen) {
      out.print(",[");            // start new row after a comma
    }
    for (int i = 0; i < row.length; i++) {
      out.print(row[i]);
      if (i < row.length - 1) out.print(",");
    }
    out.print("]");               // close this row
    firstRowOpen = false;         // subsequent rows will prepend ",["
  }

  @Override
  public void endMatrix() {
    out.println("])");
  }

  @Override
  public void emitFinalColumnVector(String name, boolean[] isAccepting) {
    out.println();
    AutomatonMatrixWriter.writeFinalColumnVectorComment(out, name, COMMENT_CHAR);
    out.print(name + " = matrix(ZZ, " + isAccepting.length + ", 1, [");
    for (int i = 0; i < isAccepting.length; ++i) {
      out.print(isAccepting[i] ? "1" : "0");
      if (i < isAccepting.length - 1) out.print(",");
    }
    out.println("])");
  }

  @Override
  public void emitFixup(String vName, String mName) {
    out.println();
    out.println("# fix up " + vName + " by multiplying");
    out.println("for _ in range(" + vName + ".ncols()):");
    out.println("    " + vName + " = " + vName + " * " + mName);
  }

  @Override
  public void close() {
    out.flush();
  }
}