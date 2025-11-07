package Automata.Writer;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * MATLAB / Octave emitter.
 */
public final class MatlabEmitter implements MatrixEmitter {
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

