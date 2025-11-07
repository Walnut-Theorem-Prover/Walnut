package Automata.Writer;

import java.io.PrintWriter;
import java.io.Writer;

public final class SageEmitter implements MatrixEmitter {
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
    AutomatonMatrixDump.writeInitialRowVectorComment(out, name);
    out.print(name + " = matrix(ZZ, 1, " + Q + ", [");
    for (int q = 0; q < Q; ++q) {
      out.print(q == q0 ? "1" : "0");
      if (q < (Q - 1)) out.print(",");
    }
    out.println("])");
    out.println();
    AutomatonMatrixDump.writeIncidenceMatricesComment(out);
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
    AutomatonMatrixDump.writeFinalColumnVectorComment(out, name);
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