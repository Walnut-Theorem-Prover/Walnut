package Main;

import Automata.FA.DeterminizationStrategies;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class MetaCommands {
  private static int AutomataIndex = 0; // Indicates the index of the automata in a particular run

  public static final Int2ObjectMap<DeterminizationStrategies.Strategy> strategyMap = new Int2ObjectOpenHashMap<>();

  // for now we only support exporting BA. Could be generalized to MPL, TXT, etc.
  public static final IntSet exportBAMap = new IntOpenHashSet();

  /**
   * Used when we're done with a particular run.
   */
  public static void resetAutomataIndex() {
    AutomataIndex = 0;
    strategyMap.clear();
    exportBAMap.clear();
  }

  public static int incrementAutomataIndex() {
    return AutomataIndex++;
  }

  public static void addStrategy(int index, DeterminizationStrategies.Strategy strategy) {
    strategyMap.put(index, strategy);
  }

  public static void addExportBA(int index) {
    exportBAMap.add(index);
  }
}
