package Automata.Writer;

import java.io.Writer;
import java.util.function.Function;

public interface MatrixEmitter extends AutoCloseable {
  record EmitterSpec(String intro, String str, String extension, Function<Writer, MatrixEmitter> ctor) {}

  void begin();

  void emitInitialRowVector(String name, int Q, int q0);

  /** Called before the Q rows are streamed via emitRow */
  void beginMatrix(String name, int Q);

  /** Called exactly Q times per matrix */
  void emitRow(int[] row);

  void endMatrix();

  void emitFinalColumnVector(String name, boolean[] isAccepting);

  void emitFixup(String vName, String mName);

  @Override default void close() {}
}
