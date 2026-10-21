package Main;

import Automata.FA.DeterminizationStrategies;
import it.unimi.dsi.fastutil.ints.*;

import java.util.regex.Matcher;

public class MetaCommands {
  private static final String WILDCARD = "*";
  static final String DEFAULT_EXPORT_NAME = "export";
  private int automataIndex = 0; // Indicates the index of the automata in a particular run

  // strategy metacommand
  private final Int2ObjectMap<DeterminizationStrategies.Strategy> strategyMap = new Int2ObjectOpenHashMap<>();
  DeterminizationStrategies.Strategy alwaysOnStrategy = null;

  // export metacommand
  // txt, ba, or gv
  private final Int2ObjectMap<String> exportMap = new Int2ObjectOpenHashMap<>();
  private boolean alwaysOnExport = false;

  public MetaCommands() {
    Prover.usingOTF = false;
    Prover.earlyExistTermination = false;
  }
  
  public int incrementAutomataIndex() {
    return automataIndex++;
  }

  /**
   * Add strategy for given automata index.
   * Note that it's impossible to validate the automata index when invoked.
   */
  public void addStrategy(String automataIdx, DeterminizationStrategies.Strategy strategy) {
    if (WILDCARD.equals(automataIdx)) {
      alwaysOnStrategy = strategy;
    } else {
      strategyMap.put(Integer.parseInt(automataIdx), strategy);
    }
  }

  public DeterminizationStrategies.Strategy getStrategy(int automataIdx) {
    if (alwaysOnStrategy != null) {
      return alwaysOnStrategy;
    }
    return strategyMap.getOrDefault(automataIdx, DeterminizationStrategies.Strategy.SC);
  }

  /**
   * Add "export 15 BA" for example.
   */
  public void addExport(String automataIdx, String format) {
    String formatLower = format.toLowerCase();
    if (!formatLower.equals("ba") && !formatLower.equals("txt") && !formatLower.equals("gv")) {
      throw WalnutException.unexpectedFormat(format);
    }
    if (WILDCARD.equals(automataIdx)) {
      alwaysOnExport = true;
      exportMap.put(0, formatLower);
    } else {
      exportMap.put(Integer.parseInt(automataIdx), formatLower);
    }
  }

  public String getExportName(int index) {
    if (alwaysOnExport || exportMap.containsKey(index)) {
      return Prover.currentEvalName == null ? DEFAULT_EXPORT_NAME : Prover.currentEvalName;
    }
    return null;
  }
  public String getExportFormat(int index) {
    if (alwaysOnExport || exportMap.containsKey(index)) {
      return exportMap.get(alwaysOnExport ? 0 : index);
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
      if (parts.length != 3 && (parts.length != 1 || !parts[0].equals(Prover.EARLY_EXIST_TERMINATION))) {
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
          // example: export 15 BA, or export * TXT
          addExport(parts[1], parts[2]);
          break;
        case Prover.EARLY_EXIST_TERMINATION:
          Prover.earlyExistTermination = true;
          break;
        default:
          throw WalnutException.invalidCommand(command);
      }
    }
    return command;
  }
}
