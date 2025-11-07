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

/**
 * MATLAB / Octave emitter.
 */
public final class MatlabEmitter implements MatrixEmitter {
  public static final String MATLAB_STRING = "m"; // note this conflicts with Mathematica
  public static final String EXTENSION = Prover.DOT + MATLAB_STRING;
  private final PrintWriter out;
  private boolean firstRow = true;

  public MatlabEmitter(Writer writer) {
    this.out = new PrintWriter(writer);
  }

  @Override
  public void begin() {
    out.println("% MATLAB/Octave output");
  }

  @Override
  public void emitInitialRowVector(String name, int Q, int q0) {
    AutomatonMatrixDump.writeInitialRowVectorComment(out, name);
    out.print(name + " = [");
    for (int q = 0; q < Q; ++q) {
      out.print(q == q0 ? "1" : "0");
      if (q < Q - 1) out.print(" ");
    }
    out.println("];");
    out.println();
    AutomatonMatrixDump.writeIncidenceMatricesComment(out);
  }

  @Override
  public void beginMatrix(String name, int Q) {
    out.println();
    out.print(name + " = [");
    firstRow = true;
  }

  @Override
  public void emitRow(int[] row) {
    if (!firstRow) out.print("; ");       // row separator
    firstRow = false;
    for (int i = 0; i < row.length; i++) {
      out.print(row[i]);
      if (i < row.length - 1) out.print(" ");
    }
  }

  @Override
  public void endMatrix() {
    out.println("];");
  }

  @Override
  public void emitFinalColumnVector(String name, boolean[] isAccepting) {
    out.println();
    AutomatonMatrixDump.writeFinalColumnVectorComment(out, name);
    out.print(name + " = [");
    for (int i = 0; i < isAccepting.length; ++i) {
      out.print(isAccepting[i] ? "1" : "0");
      if (i < isAccepting.length - 1) out.print("; ");
    }
    out.println("];");
  }

  @Override
  public void emitFixup(String vName, String mName) {
    out.println();
    out.println("% fix up " + vName + " by multiplying");
    // one-liner is Octave/MATLAB compatible
    out.println("for i = 1:size(" + vName + ", 2), " + vName + " = " + vName + " * " + mName + "; end");
  }

  @Override
  public void close() {
    out.flush();
  }
}

