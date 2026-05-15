/*	 2026 John Nicol
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
package Automata.Search;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Product BFS specialized to product-states represented as {@code int[]}.
 * This is the main performance path in the hypothesis checks.
 * - Each product state is an int[] of fixed length K.
 * - The step function writes successors into a reusable output array to avoid per-edge allocations.
 * - Visited states are memoized by content (int[] equality) using a fastutil open-addressing map.
 */
public final class ProductBFS {

  @FunctionalInterface
  public interface IntStep {
    /**
     * Writes the successor of (state, sym) into out.
     *
     * @return true if this successor should be explored, false if it should be skipped/pruned
     */
    boolean step(int[] state, int sym, int[] out);
  }

  @FunctionalInterface
  public interface IntAccepting {
    boolean test(int[] state);
  }

  private static final Hash.Strategy<int[]> INT_ARRAY_STRATEGY = new Hash.Strategy<>() {
    @Override
    public int hashCode(int[] array) {
      return Arrays.hashCode(array);
    }

    @Override
    public boolean equals(int[] left, int[] right) {
      return Arrays.equals(left, right);
    }
  };

  private static final Object2IntMap<int[]> idOf = new Object2IntOpenCustomHashMap<>(INT_ARRAY_STRATEGY);

  /** state id -> immutable owned product-state tuple */
  private static final List<int[]> states = new ArrayList<>(1024);

  /** backpointer: previous state id */
  private static final IntList prevId = new IntArrayList(1024);

  /** backpointer: symbol used to reach this state */
  private static final IntList prevSym = new IntArrayList(1024);

  /** BFS queue over state ids */
  private static final IntPriorityQueue q = new IntArrayFIFOQueue();

  /**
   * Returns a shortest witness as a {@link Word}, or null if no accepting state is reachable.
   * Alphabet is [0..alphabetSize-1].
   */
  public static Word<Integer> shortestWitnessWordInt(
      int[] start, int alphabetSize, IntStep step, IntAccepting accepting) {

    if (accepting.test(start)) {
      return Word.epsilon();
    }

    final int tupleLength = start.length;

    // visited: product-state tuple -> state id
    idOf.clear();
    idOf.defaultReturnValue(-1);

    states.clear();
    prevId.clear();
    prevSym.clear();
    q.clear();

    // Insert start as state id 0.
    // Copy so callers may safely reuse/mutate their original start array.
    final int[] startKey = Arrays.copyOf(start, tupleLength);
    idOf.put(startKey, 0);
    states.add(startKey);
    prevId.add(-1);
    prevSym.add(-1);
    q.enqueue(0);

    // Reused scratch buffer for successors computed by step(...).
    final int[] nextStateBuffer = new int[tupleLength];

    while (!q.isEmpty()) {
      final int currentStateId = q.dequeueInt();
      final int[] currentState = states.get(currentStateId);

      for (int symbol = 0; symbol < alphabetSize; symbol++) {
        if (!step.step(currentState, symbol, nextStateBuffer)) {
          continue;
        }

        if (idOf.getInt(nextStateBuffer) != -1) {
          continue; // already visited
        }

        final int nextStateId = states.size();
        final int[] storedNextState = Arrays.copyOf(nextStateBuffer, tupleLength);

        idOf.put(storedNextState, nextStateId);
        states.add(storedNextState);
        prevId.add(currentStateId);
        prevSym.add(symbol);

        if (accepting.test(storedNextState)) {
          return reconstructWord(nextStateId);
        }

        q.enqueue(nextStateId);
      }
    }

    return null;
  }

  /**
   * Product of DFAs with per-component symbol projection and desired acceptance.
   * For each global symbol a, symbolMaps[a][i] is the symbol seen by component DFA i.
   * The target is a product state whose component-wise acceptance matches wantAccept[i] for every component i.
   * This includes a pruning precomputation:
   * for each component DFA and each local state, we precompute whether that local state
   * can still reach a state whose acceptance matches the desired value for that component.
   * If any component of a newly-generated product state is already "hopeless", the whole
   * product state can be discarded immediately.
   */
  static Word<Integer> shortestWitnessWordProduct(
      int[] start,
      int alphabetSize,
      CompactDFA<Integer>[] dfas,
      int[][] symbolMaps,
      boolean[] wantAccept) {

    final int componentCount = dfas.length;

    /*
     * For each component i, collect the distinct local symbols that can ever appear in
     * that component after projecting the global alphabet through symbolMaps.
     */
    final int[][] projectedSymbolsByComponent = projectedSymsByComponent(symbolMaps, componentCount);

    /*
     * liveStateCanReachWanted[i][q] means:
     *   Starting from live local state q in component i, there exists some continuation
     *   over that component's projected alphabet that reaches a local state whose
     *   acceptance matches wantAccept[i].
     *
     * Dead state -1 is handled directly at the call site:
     *   - if wantAccept[i] is false, dead is already good
     *   - if wantAccept[i] is true, dead can never recover
     */
    final boolean[][] liveStateCanReachWanted = new boolean[componentCount][];
    for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
      liveStateCanReachWanted[componentIndex] =
          computeLiveStateCanReachWanted(
              dfas[componentIndex],
              projectedSymbolsByComponent[componentIndex],
              wantAccept[componentIndex]
          );
    }

    // Early impossibility check at the start state.
    for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
      final int startLocalState = start[componentIndex];

      if (startLocalState < 0) {
        if (wantAccept[componentIndex]) {
          return null;
        }
      } else if (!liveStateCanReachWanted[componentIndex][startLocalState]) {
        return null;
      }
    }

    return shortestWitnessWordInt(
        start,
        alphabetSize,
        (productState, globalSym, succState) -> {
          final int[] projectedSymsForGlobalSym = symbolMaps[globalSym];

          for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
            final int currentLocalState = productState[componentIndex];
            final int projectedLocalSym = projectedSymsForGlobalSym[componentIndex];
            final int nextLocalState =
                (currentLocalState < 0)
                    ? -1
                    : dfas[componentIndex].getSuccessor(currentLocalState, projectedLocalSym);

            succState[componentIndex] = nextLocalState;

            /*
             * Prune: once a component enters a local state from which the desired local acceptance
             * is no longer reachable, this whole product successor can never lead to a witness.
             */
            if (nextLocalState < 0) {
              if (wantAccept[componentIndex]) {
                return false;
              }
            } else if (!liveStateCanReachWanted[componentIndex][nextLocalState]) {
              return false;
            }
          }

          return true;
        },
        productState -> {
          for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
            final boolean componentIsAccepting =
                productState[componentIndex] >= 0
                    && dfas[componentIndex].isAccepting(productState[componentIndex]);

            if (componentIsAccepting != wantAccept[componentIndex]) {
              return false;
            }
          }
          return true;
        }
    );
  }

  /**
   * For each component position i, collect the distinct local symbols that can appear there
   * when a global symbol is projected through symbolMaps.
   * Example:
   *   symbolMaps[a][i] = local symbol read by component i when global symbol a is taken.
   * The result is used by the pruning precomputation to know which local edges are relevant
   * for each component DFA.
   */
  private static int[][] projectedSymsByComponent(int[][] symbolMaps, int componentCount) {
    final IntArrayList[] projectedSymLists = new IntArrayList[componentCount];
    for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
      projectedSymLists[componentIndex] = new IntArrayList();
    }

    for (int[] projectedSymsForGlobalSym : symbolMaps) {
      for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
        final int projectedLocalSym = projectedSymsForGlobalSym[componentIndex];
        final IntArrayList symsSeenForComponent = projectedSymLists[componentIndex];

        boolean alreadyPresent = false;
        for (int symIndex = 0, symCount = symsSeenForComponent.size(); symIndex < symCount; symIndex++) {
          if (symsSeenForComponent.getInt(symIndex) == projectedLocalSym) {
            alreadyPresent = true;
            break;
          }
        }

        if (!alreadyPresent) {
          symsSeenForComponent.add(projectedLocalSym);
        }
      }
    }

    final int[][] result = new int[componentCount][];
    for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
      result[componentIndex] = projectedSymLists[componentIndex].toIntArray();
    }
    return result;
  }

  /**
   * For one component DFA, precompute which live local states can still reach a state whose
   * acceptance matches wantAccept.
   * This is a reverse reachability computation over the local DFA graph restricted to the
   * symbols that can actually appear in this component.
   * The returned array is indexed only by live DFA states:
   *   result[q] == true
   * means there exists some continuation from live local state q that reaches a local state
   * whose acceptance matches wantAccept.
   * Dead state -1 is not stored in the returned array. Callers handle it directly:
   *   - if wantAccept is false, dead is already good
   *   - if wantAccept is true, dead can never recover
   */
  private static boolean[] computeLiveStateCanReachWanted(
      CompactDFA<Integer> dfa, int[] allowedSyms, boolean wantAccept) {

    final int liveStateCount = dfa.size();

    /*
     * Build the reverse graph in two passes:
     * 1) Count how many predecessors each target live state has,
     *    and separately count how many transitions go directly to dead state -1.
     * 2) Allocate exact-sized predecessor arrays and fill them.
     */
    final int[] predCountByTargetState = new int[liveStateCount];
    int predCountIntoDeadState = 0;

    for (int sourceState = 0; sourceState < liveStateCount; sourceState++) {
      for (int allowedSym : allowedSyms) {
        final int successorState = dfa.getSuccessor(sourceState, allowedSym);
        if (successorState < 0) {
          predCountIntoDeadState++;
        } else {
          predCountByTargetState[successorState]++;
        }
      }
    }

    final int[][] predStatesByTargetState = new int[liveStateCount][];
    for (int targetState = 0; targetState < liveStateCount; targetState++) {
      predStatesByTargetState[targetState] = new int[predCountByTargetState[targetState]];
    }

    final int[] predStatesIntoDeadState = new int[predCountIntoDeadState];

    Arrays.fill(predCountByTargetState, 0);
    int deadWriteIndex = 0;

    for (int sourceState = 0; sourceState < liveStateCount; sourceState++) {
      for (int allowedSym : allowedSyms) {
        final int successorState = dfa.getSuccessor(sourceState, allowedSym);
        if (successorState < 0) {
          predStatesIntoDeadState[deadWriteIndex++] = sourceState;
        } else {
          predStatesByTargetState[successorState][predCountByTargetState[successorState]++] = sourceState;
        }
      }
    }

    final boolean[] liveStateCanReachWanted = new boolean[liveStateCount];
    final IntArrayFIFOQueue reverseBfsQueue = new IntArrayFIFOQueue();

    /*
     * Seed the reverse BFS with every live state that is already good:
     * - Any live state whose current acceptance already matches wantAccept.
     * - If wantAccept == false, then any live state with an allowed transition directly
     *   into dead state is also immediately good, because dead is non-accepting.
     */
    for (int state = 0; state < liveStateCount; state++) {
      if (dfa.isAccepting(state) == wantAccept) {
        liveStateCanReachWanted[state] = true;
        reverseBfsQueue.enqueue(state);
      }
    }

    if (!wantAccept) {
      for (int predecessorOfDeadState : predStatesIntoDeadState) {
        if (!liveStateCanReachWanted[predecessorOfDeadState]) {
          liveStateCanReachWanted[predecessorOfDeadState] = true;
          reverseBfsQueue.enqueue(predecessorOfDeadState);
        }
      }
    }

    // Reverse BFS: if a live state can reach an already-good live target in one step, then that state is good too.
    while (!reverseBfsQueue.isEmpty()) {
      final int knownGoodTargetState = reverseBfsQueue.dequeueInt();
      final int[] predStates = predStatesByTargetState[knownGoodTargetState];

      for (int predState : predStates) {
        if (!liveStateCanReachWanted[predState]) {
          liveStateCanReachWanted[predState] = true;
          reverseBfsQueue.enqueue(predState);
        }
      }
    }

    return liveStateCanReachWanted;
  }

  private static Word<Integer> reconstructWord(int acceptingStateId) {
    final IntList reversedSyms = new IntArrayList();
    for (int stateId = acceptingStateId; prevId.getInt(stateId) != -1; stateId = prevId.getInt(stateId)) {
      reversedSyms.add(prevSym.getInt(stateId));
    }

    final WordBuilder<Integer> wordBuilder = new WordBuilder<>(reversedSyms.size());
    for (int i = reversedSyms.size() - 1; i >= 0; i--) {
      wordBuilder.add(reversedSyms.getInt(i));
    }
    return wordBuilder.toWord();
  }
}