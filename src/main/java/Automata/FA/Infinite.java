package Automata.FA;

import Automata.RichAlphabet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;
import java.util.function.Predicate;

public class Infinite {
  // Determines whether an automaton accepts infinitely many values. If it does, a regex of infinitely many accepted values (not all)
  // is given. This is true iff there exists a cycle in a minimized version of the automaton, which previously had leading or
  // trailing zeroes removed according to whether it was msd or lsd
  public static String infinite(FA fa, RichAlphabet r) {
      for (int i = 0; i < fa.getQ(); i++) {
          IntSet visited = new IntOpenHashSet(); // states we have visited
          String cycle = infiniteHelper(fa, r, visited, i, i, "");
          // once a cycle is detected, compute a prefix leading to state i and a suffix from state i to an accepting state
          if (!cycle.isEmpty()) {
              final int finalI = i;
              String prefix = findPath(fa, fa.getQ0(), y -> y == finalI, r);
              String suffix = findPath(fa, finalI, fa::isAccepting, r);
              return prefix + "(" + cycle + ")*" + suffix;
          }
      }
      return ""; // an empty string signals that we have failed to find a cycle
  }

  // helper function for our DFS to facilitate recursion
  private static String infiniteHelper(FA fa, RichAlphabet r, IntSet visited, int started, int state, String result) {
    if (visited.contains(state)) {
      if (state == started) {
        return result;
      }
      return "";
    }
    visited.add(state);
    for (Int2ObjectMap.Entry<IntList> entry : fa.getT().getEntriesNfaD(state)) {
      for (int y: entry.getValue()) {
        // this adds brackets even when inputs have arity 1 - this is fine, since we just want a usable infinite regex
        String cycle = infiniteHelper(fa, r, visited, started, y, result + r.decode(entry.getIntKey()));
        if (!cycle.isEmpty()) {
          return cycle;
        }
      }
    }

    visited.remove(state);
    return "";
  }

  // Core pathfinding logic
  private static String findPath(FA fa, int startState, Predicate<Integer> isFoundCondition, RichAlphabet r) {
    // Early exit if the start state meets the condition
    if (isFoundCondition.test(startState)) {
      return "";
    }
    List<Integer> distance = new ArrayList<>(Collections.nCopies(fa.getQ(), -1));
    List<Integer> prev = new ArrayList<>(Collections.nCopies(fa.getQ(), -1));
    List<Integer> input = new ArrayList<>(Collections.nCopies(fa.getQ(), -1));
    distance.set(startState, 0);

    Queue<Integer> queue = new LinkedList<>();
    queue.add(startState);

    boolean found = false;
    int endState = -1;

    // BFS to find the path
    while (!queue.isEmpty() && !found) {
      int current = queue.poll();

      for (Int2ObjectMap.Entry<IntList> entry : fa.getT().getEntriesNfaD(current)) {
        int x = entry.getIntKey();
        IntList transitions = entry.getValue();

        for (int y : transitions) {
          if (isFoundCondition.test(y)) {
            found = true;
            endState = y;
          }
          if (distance.get(y) == -1) { // Unvisited state
            distance.set(y, distance.get(current) + 1);
            prev.set(y, current);
            input.set(y, x);
            queue.add(y);
          }
        }
      }
    }

    // Reconstruct the path
    List<Integer> path = new ArrayList<>();
    int current = found ? endState : startState;
    while (current != startState) {
      path.add(input.get(current));
      current = prev.get(current);
    }
    Collections.reverse(path);

    // Convert the path to a string
    StringBuilder result = new StringBuilder();
    for (Integer node : path) {
      result.append(r.decode(node));
    }
    return result.toString();
  }
}
