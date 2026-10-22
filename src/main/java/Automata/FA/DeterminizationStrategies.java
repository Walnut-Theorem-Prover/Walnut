package Automata.FA;

import Automata.Automaton;
import Main.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

import static Main.Logging.DETERMINIZED;
import static Main.Logging.DETERMINIZING;
import static Main.UtilityMethods.MISSING_ELT;

/**
 * Determinization logic, with support for alternative strategies of:
 * Brzozowski, OTF, OTF-no-simulation, OTF-Brzozowski, and OTF-Brzozowski-no-simulation
 * Command automata are numbered for detailed logging or wildcard metacommands.
 * This is useful for meta-commands like [export] and [strategy]
 */
public class DeterminizationStrategies {

  public enum Strategy {
    SC("SC", false, List.of("SC")),
    BRZ("Brzozowski", false, List.of("Brz")),
    CCLS("CCLS", true, List.of("CCLS")),
    BRZ_CCLS("Brzozowski-CCLS", true, List.of("BRZCCLS")),
    CCL("CCL", false, List.of("CCL")),
    BRZ_CCL("Brzozowski-CCL", false, List.of( "BRZCCL"));
    private final String name;
    private final boolean doSimulation;
    private final List<String> aliases;

    Strategy(String name, boolean doSimulation, List<String> aliases) {
      this.name = name;
      this.doSimulation = doSimulation;
      this.aliases = new ArrayList<>(aliases);
      this.aliases.add(name);
    }

    public static Strategy fromString(String name) {
      // ignore underscores and dashes
      String tempName = name.replace("_", "-").replace("-","");
      for (Strategy strategy : Strategy.values()) {
        for (String alias : strategy.aliases) {
          if (tempName.equalsIgnoreCase(alias)) {
            return strategy;
          }
        }
      }
      throw new IllegalArgumentException("No strategy found for: " + name);
    }

    public boolean isOTFStrategy() {
      return !this.equals(SC) && !this.equals(BRZ);
    }

    String outputName(int currentIdx) {
      return "[#" + currentIdx + ", strategy: " + this.name + "]";
    }

    Strategy removeBrzozowski() {
      return switch(this) {
        case BRZ -> SC;
        case BRZ_CCLS -> CCLS;
        case BRZ_CCL -> CCL;
        default -> throw new WalnutException("Unexpected strategy:" + this.name);
      };
    }
  }

    /**
     * Determinization strategies:
     *   Subset Construction
     *   Brzozowski double-reversal
     *   OTF-CCL, OTF-CCLS
     *   Brzozowski + (OTF-CCL, OTF-CCLS)
     */
    public static void determinize(Automaton A, IntSet initialState) {
      FA fa = A.getFa();
      long timeBefore = System.currentTimeMillis();

      Strategy strategy = Strategy.SC;
      MetaCommands mc = Prover.mainProver.metaCommands;
      // A command log identifies command work even when console output is disabled.
      // Truly internal work (number-system/Ostrowski caches) passes false and null.
      if (Logging.shouldPrintDetails() || (Logging.shouldPrintStepsOrDetails() && mc.hasWildcardCommands())) {
        // Increment our automata count for use in strategy calculations.
        // Note this is only done when print is true.
        // That's because there are several silent automata creations for NS, Ostrowski, and other caches.
        int automataIdx = mc.incrementAutomataIndex();
        strategy = mc.getStrategy(automataIdx);

        // Write exported file if specified
        String exportName = mc.getExportName(automataIdx);
        if (exportName != null) {
          String exportFormat = mc.getExportFormat(automataIdx);
          // Writers canonize; do not let an export renumber the live states or tracks.
          ProverHelper.exportAutomata(Prover.currentEvalName, exportName + "_" + automataIdx + "_pre",
              exportFormat, A, fa.isFAO());
        }

        Logging.logMessage(DETERMINIZING +
            " " + strategy.outputName(automataIdx) + ": " + fa.getQ() + " states");
      }

      if (strategy != Strategy.SC) {
        if (fa.isFAO()) {
          throw new WalnutException("DFAOs are not supported for non-SC strategies.");
        }
      }

      switch (strategy) {
        case SC -> SC(fa, initialState);
        case BRZ, BRZ_CCL, BRZ_CCLS -> Brz(fa, initialState, strategy);
        case CCL, CCLS -> OtfDeterminizer.determinize(fa, initialState, strategy.doSimulation);
      }

      long timeAfter = System.currentTimeMillis();

      Logging.logMessage(
          DETERMINIZED + ": " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
    }

  /**
   * Brzozowski's strategy for SC (or OTF).
   *
   * @param fa            - finite automaton
   * @param initialStates - original initial states
   * @param strategy      - BRZ or OTF_BRZ
   */
  private static void Brz(FA fa, IntSet initialStates, Strategy strategy) {
    strategy = strategy.removeBrzozowski();

    // Reverse, determinize, minimize
    brzStep(fa, initialStates, strategy, "Reverse");
    fa.justMinimize();

    // Reverse and determinize again. Note that initial state is now q0
    brzStep(fa, IntSet.of(fa.getQ0()), Strategy.SC, "Reverse of reverse");
  }

  private static void brzStep(FA fa, IntSet initialStates, Strategy strategy,
                              String message) {
    long timeBefore = System.currentTimeMillis();
    IntSet newInitialStates = fa.reverseToNFAInternal(initialStates);
    Logging.logMessage(message + " -- " + DETERMINIZING + " with strategy:" + strategy.name + ".");
    if (strategy.equals(Strategy.SC)) {
      SC(fa, newInitialStates);
    } else {
      OtfDeterminizer.determinize(fa, newInitialStates, strategy.doSimulation);
    }
    long timeAfter = System.currentTimeMillis();
    Logging.logMessage(message + ": " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms");
  }

  private static void SC(FA fa, IntSet initialState) {
    long timeBefore = System.currentTimeMillis();

    int stateCount = 0, currentState = 0;
    Object2IntMap<IntSet> metastateToId = new Object2IntOpenHashMap<>();
    metastateToId.defaultReturnValue(MISSING_ELT);
    List<IntSet> metastateList = new ArrayList<>();
    metastateList.add(initialState);
    metastateToId.put(initialState, 0);
    stateCount++;

    // precompute for efficiency
    int alphabetSize = fa.getAlphabetSize();
    List<Int2ObjectRBTreeMap<IntList>> nfaD = fa.getT().getNfaD();

    List<Int2IntMap> dfaD = new ArrayList<>(nfaD.size());

    while (currentState < stateCount) {

      if (Logging.shouldPrintDetails()) {
        int statesSoFar = currentState + 1;
        long timeAfter = System.currentTimeMillis();
        Logging.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
            "  Progress: Added " + statesSoFar + " states - "
                + (stateCount - statesSoFar) + " states left in queue - "
                + stateCount + " reachable states - " + (timeAfter - timeBefore) + "ms");
      }

      IntSet state = metastateList.get(currentState);
      Int2IntMap currentStateMap = new Int2IntOpenHashMap();
      dfaD.add(currentStateMap);
      for (int in = 0; in != alphabetSize; ++in) {
        IntOpenHashSet metastate = new IntOpenHashSet();
        for (int q : state) {
          IntList values = nfaD.get(q).get(in);
          if (values != null) {
            metastate.addAll(values);
          }
        }
        if (metastate.isEmpty()) {
          continue;
        }
        int new_dValue;
        int key = metastateToId.getInt(metastate);
        if (key != MISSING_ELT) {
          new_dValue = key;
        } else {
          // TODO: BitSet may be a better choice, but it's not clear when NFA size is, say, >> 20000.
          metastate.trim(); // reduce memory footprint of set before storing
          metastateList.add(metastate);
          metastateToId.put(metastate, stateCount);
          new_dValue = stateCount++;
        }
        currentStateMap.put(in, new_dValue);
      }
      currentState++;
    }
    fa.setQ(stateCount);
    fa.setQ0(0);
    fa.calculateNewStateOutput(metastateList);
    fa.setDfaTransitions(dfaD);
  }
}
