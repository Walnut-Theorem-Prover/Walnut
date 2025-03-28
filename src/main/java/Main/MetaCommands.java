package Main;

import Automata.FA.DeterminizationStrategies;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.regex.Matcher;

public class MetaCommands {
  private static final String WILDCARD = "*";
  static final String DEFAULT_EXPORT_NAME = "export";
  private int automataIndex = 0; // Indicates the index of the automata in a particular run

  private final Int2ObjectMap<DeterminizationStrategies.Strategy> strategyMap = new Int2ObjectOpenHashMap<>();
  DeterminizationStrategies.Strategy alwaysOnStrategy = null;

  // for now, we only support exporting BA. Could be generalized to MPL, TXT, etc.
  private final IntSet exportBASet = new IntOpenHashSet();
  private boolean alwaysOnExport = false;

  public int incrementAutomataIndex() {
    return automataIndex++;
  }

  public void addStrategy(String index, DeterminizationStrategies.Strategy strategy) {
    if (WILDCARD.equals(index)) {
      alwaysOnStrategy = strategy;
    } else {
      strategyMap.put(Integer.parseInt(index), strategy);
    }
  }
  public DeterminizationStrategies.Strategy getStrategy(int index) {
    if (alwaysOnStrategy != null) {
      return alwaysOnStrategy;
    }
    return strategyMap.getOrDefault(index, DeterminizationStrategies.Strategy.SC);
  }

  public void addExportBA(String index) {
    if (WILDCARD.equals(index)) {
      alwaysOnExport = true;
    } else {
      exportBASet.add(Integer.parseInt(index));
    }
  }
  public String getExportBAName(int index) {
    if (alwaysOnExport || exportBASet.contains(index)) {
      return Prover.currentEvalName == null ? DEFAULT_EXPORT_NAME : Prover.currentEvalName;
    }
    return null;
  }

  /**
   * Parse meta-commands and then return the remainder of the string.
   */
  String parseMetaCommands(String s, boolean printDetails) {
    // repeatedly match meta commands at the start of the string
    while (s.startsWith(Prover.LEFT_BRACKET)) {
      Matcher metaCmdMatcher = Prover.PAT_META_CMD.matcher(s);
      if (!metaCmdMatcher.find()) {
        throw WalnutException.invalidCommand(s);
      }
      // Get the current meta command block and process it
      String metaCommandString = metaCmdMatcher.group(Prover.GROUP_META_CMD).strip();
      if (!metaCommandString.isEmpty() && !printDetails) {
        throw new WalnutException("Metacommands are currently only supported for commands ending in ::");
      }
      for (String metaCommand : metaCommandString.split(",")) {
        metaCommand = metaCommand.strip();
        if (metaCommand.startsWith(Prover.STRATEGY)) {
          // example: strategy OTF 15
          String[] strategyAndIndex = metaCommand.split("\\s+");
          if (strategyAndIndex.length != 3) {
            throw WalnutException.invalidCommandUse(metaCommand);
          }
          String strategyName = strategyAndIndex[1];
          String strategyIndex = strategyAndIndex[2];
          addStrategy(strategyIndex, DeterminizationStrategies.Strategy.fromString(strategyName)
          );
        } else if (metaCommand.startsWith(Prover.EXPORT)) {
          // example: export 15, or export *
          String[] parts = metaCommand.split("\\s+");
          if (parts.length != 2) {
            throw WalnutException.invalidCommandUse(metaCommand);
          }
          addExportBA(parts[1]);
        } else {
          throw WalnutException.invalidCommand(metaCommand);
        }
      }
      // Remove the processed meta command block from s and trim any whitespace
      s = metaCmdMatcher.group(Prover.GROUP_FINAL_CMD).strip();
    }
    return s;
  }
}
