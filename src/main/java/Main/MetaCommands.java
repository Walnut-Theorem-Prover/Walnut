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

  public MetaCommands() {
    Prover.usingOTF = false;
  }
  
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

  /**
   * Add "export 15 BA" for example. Currently only BA is supported.
   */
  public void addExport(String index, String format) {
    String formatLower = format.toLowerCase();
    if (!formatLower.equals("ba")) {
      throw WalnutException.unexpectedFormat(format);
    }
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
   * Parse meta-commands if they exist.
   * @param command - full command, e.g., [strategy 1 CCLS][export 3 BA][strategy 2 BRZ]"command"
   * @param printDetails - whether we're printing details, i.e., original command ended with ::
   * @return stripped string, e.g., "command"
   */
  String parseMetaCommands(String command, boolean printDetails) {
    // repeatedly match meta commands at the start of the string
    while (command.startsWith(Prover.LEFT_BRACKET)) {
      Matcher metaCmdMatcher = ProverHelper.matchOrFail(Prover.PAT_META_CMD, command, command);
      // Get the current meta command block and process it
      String metaCommandString = metaCmdMatcher.group(Prover.GROUP_META_CMD).strip();
      if (!metaCommandString.isEmpty() && !printDetails) {
        throw new WalnutException("Metacommands are currently only supported for commands ending in ::");
      }

      command = metaCmdMatcher.group(Prover.GROUP_FINAL_CMD).strip(); // update to be the remainder

      String[] parts = metaCommandString.split("\\s+");
      if (parts.length != 3) {
        throw WalnutException.invalidCommandUse(metaCommandString);
      }

      switch (parts[0]) {
        case Prover.STRATEGY:
          // example: strategy 15 CCL
          DeterminizationStrategies.Strategy strategy = DeterminizationStrategies.Strategy.fromString(parts[2]);
          if (strategy.isOTFStrategy()) {
            Prover.usingOTF = true;
          }
          addStrategy(parts[1], strategy);
          break;
        case Prover.EXPORT:
          // example: export 15 BA, or export * BA
          addExport(parts[1], parts[2]);
          break;
        default:
          throw WalnutException.invalidCommand(command);
      }
    }
    return command;
  }
}
