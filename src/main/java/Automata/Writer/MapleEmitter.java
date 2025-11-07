package Automata.Writer;

import java.io.PrintWriter;
import java.io.Writer;

public final class MapleEmitter implements MatrixEmitter {
  private final PrintWriter out;
  private boolean firstRowOpen = false;

  public MapleEmitter(Writer writer) {
    this.out = new PrintWriter(writer);
  }

  @Override
  public void begin() {
    out.println("with(ArrayTools):");
  }

  @Override
  public void emitInitialRowVector(String name, int Q, int q0) {
    out.println("# The row vector " + name + " denotes the indicator vector of the (singleton)");
    out.println("# set of initial states.");
    out.print(name + " := Vector[row]([");
    for (int q = 0; q < Q; ++q) {
      out.print(q == q0 ? "1" : "0");
      if (q < (Q - 1)) out.print(",");
    }
    out.println("]);");
    out.println();
    out.println("# In what follows, the M_i_x, for a free variable i and a value x, denotes");
    out.println("# an incidence matrix of the underlying graph of (the automaton of)");
    out.println("# the predicate in the query.");
    out.println("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of");
    out.println("# transitions with i=x from p to q.");
  }

  @Override
  public void beginMatrix(String name, int Q) {
    out.println();
    out.print(name + " := Matrix([");
    firstRowOpen = false; // we open per-row explicitly
  }

  @Override
  public void emitRow(int[] row) {
    if (firstRowOpen) {
      out.println(",");
    } else {
      firstRowOpen = true;
    }
    out.print("[");
    for (int i = 0; i < row.length; i++) {
      out.print(row[i]);
      if (i < row.length - 1) out.print(",");
    }
    out.print("]");
  }

  @Override
  public void endMatrix() {
    out.println("]);");
  }

  @Override
  public void emitFinalColumnVector(String name, boolean[] isAccepting) {
    out.println();
    out.println("# The column vector " + name + " denotes the indicator vector of the");
    out.println("# set of final states.");
    out.print(name + " := Vector[column]([");
    for (int i = 0; i < isAccepting.length; ++i) {
      out.print(isAccepting[i] ? "1" : "0");
      if (i < isAccepting.length - 1) out.print(",");
    }
    out.println("]);");
  }

  @Override
  public void emitFixup(String vName, String mName) {
    out.println();
    out.print("for i from 1 to Size(" + vName + ")[2] do " + vName + " := " + vName + "." + mName + "; od; #fix up v by multiplying");
  }

  @Override
  public void close() {
    out.flush();
  }
}

