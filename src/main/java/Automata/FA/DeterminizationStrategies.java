package Automata.FA;

import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class DeterminizationStrategies {
  private static final Int2ObjectMap<Strategy> strategyMap = new Int2ObjectOpenHashMap<>();

  public static Int2ObjectMap<Strategy> getStrategyMap() {
    return strategyMap;
  }

  public enum Strategy {
    SC("SC"), BRZ("Brzozowski"), OTF("OTF"), OTF_BRZ("OTF-Brzozowski");
    private final String name;

    Strategy(String name) {
      this.name = name;
    }
    public static Strategy fromString(String name) {
      for (Strategy strategy : Strategy.values()) {
        if (strategy.name.equalsIgnoreCase(name)) {
          return strategy;
        }
      }
      throw new IllegalArgumentException("No strategy found for name: " + name);
    }
  }

    private static String getStrategy(int currentIdx, Strategy strategy) {
        return "[#" + currentIdx +", strategy: " + strategy.name + "]";
    }

    /**
     * Subset (or powerset) Construction.
     */
    public static void determinize(
            FA fa, List<Int2IntMap> newMemD, IntSet initialState, boolean print, String prefix, StringBuilder log, Strategy strategy) {
      if (newMemD != null) {
          fa.setDfaD(newMemD);
      }
      if (fa.getDfaD() != null) {
        fa.setNfaD(null);
      }
      long timeBefore = System.currentTimeMillis();
      int automataIdx = FA.IncrementIndex();
      Strategy s = strategyMap.getOrDefault(automataIdx, Strategy.SC);

      UtilityMethods.logMessage(print, prefix +
              "Determinizing " + getStrategy(automataIdx, s) +": " + fa.getQ() + " states", log);

      switch (s) {
        case SC -> SC(fa, initialState, print, prefix, log, timeBefore);
        case BRZ -> Brz(fa, initialState, print, prefix, log, timeBefore);
        default -> throw new RuntimeException("Unexpected strategy: " + s.name());
      }

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(
          print, prefix + "Determinized: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

  private static void Brz(
      FA fa, IntSet initialState, boolean print, String prefix, StringBuilder log, long timeBefore) {
    // Reverse
    IntSet setOfFinalStates = fa.reverseToNFAInternal(initialState);
    // Run with SC
    SC(fa, setOfFinalStates, print, prefix, log, timeBefore);
    // Reverse again
    fa.reverseToNFAInternal(IntSet.of(fa.getQ0()));
  }

    private static void SC(FA fa, IntSet initialState, boolean print, String prefix, StringBuilder log, long timeBefore) {
        int number_of_states = 0, current_state = 0;
        Object2IntMap<IntSet> metastateToId = new Object2IntOpenHashMap<>();
        List<IntSet> metastateList = new ArrayList<>();
        metastateList.add(initialState);
        metastateToId.put(initialState, 0);
        number_of_states++;

        List<Int2IntMap> dfaD = new ArrayList<>();

        while (current_state < number_of_states) {

          if (print) {
            int statesSoFar = current_state + 1;
            long timeAfter = System.currentTimeMillis();
            UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
                    prefix + "  Progress: Added " + statesSoFar + " states - "
                + (number_of_states - statesSoFar) + " states left in queue - "
                + number_of_states + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
          }

          IntSet state = metastateList.get(current_state);
          dfaD.add(new Int2IntOpenHashMap());
          Int2IntMap currentStateMap = dfaD.get(current_state);
          for (int in = 0; in != fa.getAlphabetSize(); ++in) {
            IntOpenHashSet metastate = determineMetastate(fa, state, in);
            if (!metastate.isEmpty()) {
              int new_dValue;
              int key = metastateToId.getOrDefault(metastate, -1);
              if (key != -1) {
                new_dValue = key;
              } else {
                metastate.trim(); // reduce memory footprint of set before storing
                metastateList.add(metastate);
                metastateToId.put(metastate, number_of_states);
                new_dValue = number_of_states;
                number_of_states++;
              }
              currentStateMap.put(in, new_dValue);
            }
          }
          current_state++;
        }
        fa.setQ(number_of_states);
        fa.setQ0(0);
        fa.setO(calculateNewStateOutput(fa.getO(), metastateList));
        fa.setNfaD(null);
        fa.setDfaD(dfaD);
        // NOTE: nfaD is now null! This is to save peak memory, and indicates we are now in a DFA
    }

    /**
     * Build up a new subset of states in the subset construction algorithm.
     *
     * @param state   -
     * @param in      - index into alphabet
     * @return Subset of states used in Subset Construction
     */
    private static IntOpenHashSet determineMetastate(FA fa, IntSet state, int in) {
        IntOpenHashSet dest = new IntOpenHashSet();
        for (int q : state) {
            if (fa.getDfaD() == null) {
                IntList values = fa.getNfaD().get(q).get(in);
                if (values != null) {
                    dest.addAll(values);
                }
            } else {
                Int2IntMap iMap = fa.getDfaD().get(q);
                int key = iMap.getOrDefault(in, -1);
                if (key != -1) {
                    dest.add(key);
                }
            }
        }
        return dest;
    }

    /**
     * Calculate new state output (O), from previous O and metastates.
     * @param O          - previous O
     * @param metastates - list of metastates
     * @return new O
     */
    private static IntList calculateNewStateOutput(IntList O, List<IntSet> metastates) {
        IntList newO = new IntArrayList();
        for (IntSet metastate : metastates) {
            boolean flag = false;
            for (int q : metastate) {
                if (O.getInt(q) != 0) {
                    newO.add(1);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                newO.add(0);
            }
        }
        return newO;
    }
}
