package Automata.FA;

import Main.UtilityMethods;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class DeterminizationStrategies {

    public enum Strategy {
        SC("SC"), BRZ("Brzozowski"), OTF("OTF"), OTF_BRZ("OTF-Brzozowski");
        private final String name;
        Strategy(String name) {
            this.name = name;
        }
    }

    private static String getStrategy() {
        return "[#" + FA.IncrementIndex() +", strategy: " + Strategy.SC.name + "]";
    }

    /**
     * Subset (or powerset) Construction.
     *
     * @return A memory-efficient representation of a determinized transition function
     */
    public static List<Int2IntMap> determinize(
            FA fa, List<Int2IntMap> newMemD, IntSet initialState, boolean print, String prefix, StringBuilder log, Strategy strategy) {
      if (newMemD != null) {
          fa.setDfaD(newMemD);
      }
      if (fa.getDfaD() != null) {
        fa.setNfaD(null);
      }
      long timeBefore = System.currentTimeMillis();
      UtilityMethods.logMessage(print, prefix +
              "Determinizing " + getStrategy() +": " + fa.getQ() + " states", log);

      int number_of_states = 0, current_state = 0;
      Object2IntMap<IntSet> statesHash = new Object2IntOpenHashMap<>();
      List<IntSet> statesList = new ArrayList<>();
      statesList.add(initialState);
      statesHash.put(initialState, 0);
      number_of_states++;

      List<Int2IntMap> newD = new ArrayList<>();

      while (current_state < number_of_states) {

        if (print) {
          int statesSoFar = current_state + 1;
          long timeAfter = System.currentTimeMillis();
          UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
                  prefix + "  Progress: Added " + statesSoFar + " states - "
              + (number_of_states - statesSoFar) + " states left in queue - "
              + number_of_states + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
        }

        IntSet state = statesList.get(current_state);
        newD.add(new Int2IntOpenHashMap());
        Int2IntMap currentStateMap = newD.get(current_state);
        for (int in = 0; in != fa.getAlphabetSize(); ++in) {
          IntOpenHashSet stateSubset = determineMetastate(fa, state, in);
          if (!stateSubset.isEmpty()) {
            int new_dValue;
            int key = statesHash.getOrDefault(stateSubset, -1);
            if (key != -1) {
              new_dValue = key;
            } else {
              stateSubset.trim(); // reduce memory footprint of set before storing
              statesList.add(stateSubset);
              statesHash.put(stateSubset, number_of_states);
              new_dValue = number_of_states;
              number_of_states++;
            }
            currentStateMap.put(in, new_dValue);
          }
        }
        current_state++;
      }
      // NOTE: d is now null! This is to save peak memory
      // It's recomputed in minimize_valmari via the memory-efficient newMemD
      fa.setQ(number_of_states);
      fa.setQ0(0);
      fa.setO(calculateNewStateOutput(fa.getO(), statesList));

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(
              print, prefix + "Determinized: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
      fa.setNfaD(null);
      fa.setDfaD(newD);
      return newD;
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
