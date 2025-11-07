package Automata.Writer;

import Automata.Automaton;
import Automata.FA.FA;
import Main.UtilityMethods;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public final class AutomatonMatrixDump {
  /**
   * Walks the automaton and streams v, all M_<vars>_<values>, and w to the given emitter.
   * The emitter controls the target syntax (Maple, Sage, MATLAB, etc).
   * It is assumed that freeVariables is non-null and non-empty.
   */
  public static void writeAll(Automaton automaton,
                              List<String> freeVariables,
                              MatrixEmitter emitter) {
    final FA fa = automaton.getFa();
    if (fa.isTRUE_FALSE_AUTOMATON()) {
      throw new WalnutException("incidence matrices cannot be calculated, because the automaton does not have a free variable.");
    }

    // Keep behavior consistent with your current writer
    automaton.canonize();

    // Build variable -> index using the automaton's label (List<String>)
    final List<String> labelVars = automaton.getLabel();
    Map<String, Integer> varIndex = new HashMap<>(Math.max(16, labelVars.size() * 2));
    for (int i = 0; i < labelVars.size(); i++) {
      String v = labelVars.get(i);
      if (varIndex.put(v, i) != null) {
        throw new WalnutException("Duplicate variable in automaton label: " + v);
      }
    }

    // Validate free vars exist and are unique
    Set<String> seen = new HashSet<>();
    for (String v : freeVariables) {
      if (!varIndex.containsKey(v)) {
        throw new WalnutException("incidence matrices for the variable " + v +
            " cannot be calculated, because " + v + " is not a free variable.");
      }
      if (!seen.add(v)) {
        throw new WalnutException("Duplicate free variable: " + v);
      }
    }

    // Indices & domains
    List<Integer> indices = freeVariables.stream().map(varIndex::get).toList();
    List<List<Integer>> domains = indices.stream()
        .map(i -> automaton.richAlphabet.getA().get(i))
        .collect(Collectors.toList());
    for (int i = 0; i < domains.size(); i++) {
      List<Integer> dom = domains.get(i);
      if (dom == null || dom.isEmpty()) {
        throw new WalnutException("Empty value domain for free variable: " + freeVariables.get(i));
      }
    }

    // Representative for fix-up:
    // Prefer 0 (to match original .mpl), else fall back to first value in each domain.
    List<Integer> rep = new ArrayList<>(domains.size());
    for (List<Integer> dom : domains) {
      if (dom.contains(0)) rep.add(0);
      else rep.add(dom.get(0));
    }

    final int Q  = fa.getQ();
    final int q0 = fa.getQ0();

    emitter.begin();
    emitter.emitInitialRowVector("v", Q, q0);

    // Generate all assignments and matrices
    for (List<Integer> assignment : cartesian(domains)) {
      final String mName = "M_" + String.join("_", freeVariables) + "_"
          + assignment.stream().map(String::valueOf).collect(Collectors.joining("_"));
      emitter.beginMatrix(mName, Q);

      // Gather matching encoded symbols for this assignment (mirrors your current logic)
      Set<Integer> encodedValues = new HashSet<>();
      for (int x = 0; x < automaton.getAlphabetSize(); ++x) {
        List<Integer> decoding = automaton.richAlphabet.decode(x);
        // pick out decoding at the chosen indices and compare to assignment
        boolean match = true;
        for (int i = 0; i < indices.size(); i++) {
          if (!Objects.equals(decoding.get(indices.get(i)), assignment.get(i))) {
            match = false; break;
          }
        }
        if (match) encodedValues.add(x);
      }

      int[] row = new int[Q];
      for (int p = 0; p < Q; ++p) {
        java.util.Arrays.fill(row, 0);
        Set<Int2ObjectMap.Entry<IntList>> entrySet = fa.t.getEntriesNfaD(p);
        for (Int2ObjectMap.Entry<IntList> e : entrySet) {
          int sym = e.getIntKey();
          if (!encodedValues.contains(sym)) continue;
          IntList targets = e.getValue();
          for (int k = 0, n = targets.size(); k < n; k++) {
            row[targets.getInt(k)]++;
          }
        }
        emitter.emitRow(row);
      }

      emitter.endMatrix();
    }

    // Final states vector
    boolean[] isAcc = new boolean[Q];
    for (int q = 0; q < Q; q++) isAcc[q] = fa.isAccepting(q);
    emitter.emitFinalColumnVector("w", isAcc);

    // Fix-up loop
    final String repName = "M_" + String.join("_", freeVariables) + "_"
        + rep.stream().map(String::valueOf).collect(Collectors.joining("_"));
    emitter.emitFixup("v", repName);
  }

  // Simple cartesian product: List<List<Integer>> domains -> List<List<Integer>> assignments
  private static <T> List<List<T>> cartesian(List<List<T>> lists) {
    List<List<T>> result = new ArrayList<>();
    if (lists.isEmpty()) {
      result.add(java.util.Collections.emptyList());
      return result;
    }
    List<T> first = lists.get(0);
    List<List<T>> rest = cartesian(lists.subList(1, lists.size()));
    for (T x : first) {
      for (List<T> r : rest) {
        List<T> newList = new ArrayList<>(1 + r.size());
        newList.add(x);
        newList.addAll(r);
        result.add(newList);
      }
    }
    return result;
  }

  public static void writeInitialRowVectorComment(PrintWriter out, String vName) {
    out.println("# The row vector " + vName + " denotes the indicator vector of the (singleton)");
    out.println("# set of initial states.");
  }

  public static void writeIncidenceMatricesComment(PrintWriter out) {
    out.println("# In what follows, the M_i_x, for a free variable i and a value x, denotes");
    out.println("# an incidence matrix of the underlying graph of (the automaton of)");
    out.println("# the predicate in the query.");
    out.println("# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of");
    out.println("# transitions with i=x from p to q.");
  }

  public static void writeFinalColumnVectorComment(PrintWriter out, String wName) {
    out.println("# The column vector " + wName + " denotes the indicator vector of the");
    out.println("# set of final states.");
  }

  /**
   * Writes down matrix for this automaton to file in specified format.
   * @return filename where matrix is written.
   */
  public static String writeMatrix(Automaton automaton, String address, String extension, List<String> freeVariables) {
    String filename = address + extension;
    try (Writer w = new BufferedWriter(new FileWriter(filename))) {
      MatrixEmitter emitter = new MapleEmitter(w);
      writeAll(automaton, freeVariables, emitter);
    } catch (IOException e) {
      UtilityMethods.printTruncatedStackTrace(e);
    }
    return filename;
  }
}
