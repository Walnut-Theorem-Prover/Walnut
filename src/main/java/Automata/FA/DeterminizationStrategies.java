package Automata.FA;

import MRC.Index.AntichainForestIndex;
import MRC.Index.OTFIndex;
import MRC.Model.DeterminizeRecord;
import MRC.Model.MyDFA;
import MRC.Model.MyNFA;
import MRC.Model.Threshold;
import MRC.NFATrim;
import MRC.Simulation.ParallelSimulation;
import Main.UtilityMethods;
import MRC.OnTheFlyDeterminization;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.ts.AcceptorPowersetViewTS;

import java.util.*;

/**
 * Determinization logic, with support for alternative strategies of:
 * Brzozowski, OTF, OTF-no-simulation, OTF-Brzozowski, and OTF-Brzozowski-no-simulation
 * Automata are numbered as they are (non-silently) determinized.
 * This is useful for meta-commands like [export] and [strategy]
 */
public class DeterminizationStrategies {
  private static final Int2ObjectMap<Strategy> strategyMap = new Int2ObjectOpenHashMap<>();

  public static Int2ObjectMap<Strategy> getStrategyMap() {
    return strategyMap;
  }

  public enum Strategy {
    SC("SC", List.of("SC")), BRZ("Brzozowski", List.of("Brz")),
    OTF("OTF", List.of("OTF")), OTF_BRZ("OTF-Brzozowski", List.of("OTF_BRZ", "OTF-BRZ")),
    OTF_NOSIM("OTF-no-simulation", List.of("OTF_NOSIM", "OTF-NOSIM")),
    OTF_BRZ_NOSIM("OTF-Brzozowski-no-simulation", List.of("OTF_BRZ_NOSIM", "OTF-BRZ-NOSIM"));
    private final String name;
    private final List<String> aliases;

    Strategy(String name, List<String> aliases) {
      this.name = name;
      this.aliases = new ArrayList<>(aliases);
      this.aliases.add(name);
    }

    public static Strategy fromString(String name) {
      for (Strategy strategy : Strategy.values()) {
        for (String alias : strategy.aliases) {
          if (name.equalsIgnoreCase(alias)) {
            return strategy;
          }
        }
      }
      throw new IllegalArgumentException("No strategy found for: " + name);
    }

    String outputName(int currentIdx) {
      return "[#" + currentIdx + ", strategy: " + this.name + "]";
    }

    Strategy removeBrzozowski() {
      return switch(this) {
        case BRZ -> SC;
        case OTF_BRZ -> OTF;
        case OTF_BRZ_NOSIM -> OTF_NOSIM;
        default -> throw new RuntimeException("Unexpected strategy:" + this.name);
      };
    }
  }


    /**
     * Subset (or powerset) Construction.
     */
    public static void determinize(
            FA fa, List<Int2IntMap> newMemD, IntSet initialState, boolean print, String prefix, StringBuilder log) {
      if (newMemD != null) {
        fa.setNfaD(null);
      }
      if (fa.getDfaD() != null) {
        fa.setNfaD(null);
      }

      // Convert to MyNFA representation
      // then trim
      // then bisim


      long timeBefore = System.currentTimeMillis();

      Strategy strategy = Strategy.SC;
      if (print) {
        // Increment our automata count for use in strategy calculations.
        // Note this is only done when print is true.
        // That's because there are several silent automata creations for NS, Ostrowski, and other caches.
        int automataIdx = FA.IncrementIndex();
        strategy = strategyMap.getOrDefault(automataIdx, Strategy.SC);
        UtilityMethods.logMessage(print, prefix +
            "Determinizing " + strategy.outputName(automataIdx) + ": " + fa.getQ() + " states", log);
      }

      switch (strategy) {
        case SC -> SC(fa, initialState, print, prefix, log);
        case BRZ -> Brz(fa, initialState, strategy, true, print, prefix, log);
        case OTF_BRZ -> Brz(fa, initialState, strategy, true, print, prefix, log);
        case OTF_BRZ_NOSIM -> Brz(fa, initialState, strategy, false, print, prefix, log);
        case OTF -> OTF(fa, initialState, true, print, prefix, log);
        case OTF_NOSIM -> OTF(fa, initialState, false, print, prefix, log);
      }

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(
          print, prefix + "Determinized: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

  /**
   * Brzozowski's strategy for SC (or OTF).
   * @param fa - finite automaton
   * @param initialStates - original initial states
   * @param strategy - BRZ or OTF_BRZ
   */
  private static void Brz(
      FA fa, IntSet initialStates, Strategy strategy, boolean doSimulation, boolean print, String prefix, StringBuilder log) {
    strategy = strategy.removeBrzozowski();

    // Reverse
    brzStep(fa, initialStates, strategy, doSimulation, print, prefix, log, "Reverse");

    fa.justMinimize(print, prefix, log); // also switches back to NFA representation

    // Reverse again. Note that initial state of SC is q0
    brzStep(fa, IntSet.of(fa.getQ0()), Strategy.SC, doSimulation, print, prefix, log, "Reverse of reverse");
  }

  private static void brzStep(FA fa, IntSet initialStates, Strategy strategy, boolean doSimulation, boolean print, String prefix, StringBuilder log, String message) {
    IntSet newInitialStates;
    long timeAfter;
    long timeBefore;
    timeBefore = System.currentTimeMillis();
    newInitialStates = fa.reverseToNFAInternal(initialStates);
    UtilityMethods.logMessage(print, prefix + message + " -- Determinizing with strategy:" + strategy.name + ".", log);
    if (strategy.equals(Strategy.SC)) {
      SC(fa, newInitialStates, print, prefix, log);
    } else {
      OTF(fa, newInitialStates, print, doSimulation, prefix, log);
    }
    timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(
        print, prefix + message + ": " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  private static void SC(
      FA fa, IntSet initialState, boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();

    int stateCount = 0, currentState = 0;
    Object2IntMap<IntSet> metastateToId = new Object2IntOpenHashMap<>();
    List<IntSet> metastateList = new ArrayList<>();
    metastateList.add(initialState);
    metastateToId.put(initialState, 0);
    stateCount++;

    List<Int2IntMap> dfaD = new ArrayList<>();

    while (currentState < stateCount) {

      if (print) {
        int statesSoFar = currentState + 1;
        long timeAfter = System.currentTimeMillis();
        UtilityMethods.logMessage(statesSoFar == 1e2 || statesSoFar == 1e3 || statesSoFar % 1e4 == 0,
            prefix + "  Progress: Added " + statesSoFar + " states - "
                + (stateCount - statesSoFar) + " states left in queue - "
                + stateCount + " reachable states - " + (timeAfter - timeBefore) + "ms", log);
      }

      IntSet state = metastateList.get(currentState);
      dfaD.add(new Int2IntOpenHashMap());
      Int2IntMap currentStateMap = dfaD.get(currentState);
      for (int in = 0; in != fa.getAlphabetSize(); ++in) {
        IntOpenHashSet metastate = determineMetastate(fa, state, in);
        if (!metastate.isEmpty()) {
          int new_dValue;
          int key = metastateToId.getOrDefault(metastate, -1);
          if (key != -1) {
            new_dValue = key;
          } else {
            // TODO: BitSet may be a better choice, but it's not clear when NFA size is, say, >> 20000.
            metastate.trim(); // reduce memory footprint of set before storing
            metastateList.add(metastate);
            metastateToId.put(metastate, stateCount);
            new_dValue = stateCount;
            stateCount++;
          }
          currentStateMap.put(in, new_dValue);
        }
      }
      currentState++;
    }
    fa.setQ(stateCount);
    fa.setQ0(0);
    fa.setO(calculateNewStateOutput(fa.getO(), metastateList));
    fa.setNfaD(null);
    fa.setDfaD(dfaD);
  }

  private static void OTF(
      FA fa, IntSet initialState, boolean doSimulation, boolean print, String prefix, StringBuilder log) {
    long timeBefore = System.currentTimeMillis();

    MyNFA<Integer> myNFA = fa.FAtoMyNFA(initialState);
    MyNFA<Integer> reduced = NFATrim.bisim(myNFA);
    if (reduced.size() < fa.getQ()) {
      UtilityMethods.logMessage(
          print, prefix + "Bisimulation reduced to " + reduced.size() + " states", log);
    }
    ArrayList<BitSet> simRels = new ArrayList<>();
    if (doSimulation) {
      int prevSize = reduced.size();
      reduced = ParallelSimulation.fullyComputeRels(reduced, simRels);
      if (reduced.size() != prevSize) {
        UtilityMethods.logMessage(
            print, prefix + "Simulation altered to " + reduced.size() + " states", log);
      }
      if (!simRels.isEmpty()) {
        int simCount = 0;
        for (BitSet b : simRels) {
          if (b != null) {
            simCount += b.cardinality();
          }
        }
        UtilityMethods.logMessage(
            print, prefix + "Found " + simCount + " simulation relations", log);
      }
    }
    final Threshold threshold = Threshold.adaptiveSteps(4000);
    OTFIndex index = new AntichainForestIndex<>(reduced, simRels.toArray(new BitSet[0]));
    Alphabet<Integer> inputs = reduced.getInputAlphabet();
    AcceptorPowersetViewTS<BitSet, Integer, Integer> nfa = reduced.powersetView();
    Deque<DeterminizeRecord<BitSet>> stack = new ArrayDeque<>();

    BitSet init = nfa.getInitialState();
    boolean initAcc = nfa.isAccepting(init);
    MyDFA<Integer> out = new MyDFA<>(inputs);
    int initOut = out.addInitialState(initAcc);

    index.put(init, initOut);

    stack.push(new DeterminizeRecord<>(init, initOut));
    BitSet finishedStates = new BitSet();
    Deque<Integer> stateBuffer = new ArrayDeque<>();

    int numInputs = fa.getAlphabetSize();
    int totalPrints = 0;

    while (!stack.isEmpty()) {
      if (print) {
        int statesSoFar = out.size();
        if (statesSoFar / 1000 > totalPrints) {
          totalPrints++;
          int queueSize = stack.size();
          long timeAfter = System.currentTimeMillis();
          UtilityMethods.logMessage(true,
              prefix + "  Progress: Added " + statesSoFar + " states - "
                  + queueSize + " states left in queue - " + (timeAfter - timeBefore) + "ms", log);
        }
      }
      DeterminizeRecord<BitSet> curr = stack.pop();
      BitSet inState = curr.inputState;
      int outState = curr.outputAddress;
      boolean complete = true;
      for (int i = 0; i < numInputs; ++i) {
        final BitSet prune = index.prune(nfa.getSuccessor(inState, i));
        int outSucc = index.get(prune);
        if (outSucc == OTFIndex.MISSING_ELEMENT) {
          complete = false;
          final boolean succAcc = nfa.isAccepting(prune);
          // add new state to DFA and to stack
          if (stateBuffer.isEmpty()) {
            outSucc = out.addState(succAcc);
          } else {
            outSucc = stateBuffer.pop();
            out.setAccepting(outSucc, succAcc);
          }
          index.put(prune, outSucc);
          stack.push(new DeterminizeRecord<>(prune, outSucc));
        }
        out.setTransition(outState, inputs.getSymbolIndex(i), outSucc);
      }

      finishedStates.set(outState);

      if (complete && threshold.test(out)) {
        OnTheFlyDeterminization.minimizeReuse(inputs, out, finishedStates, stateBuffer, index);
        threshold.update(out.size() - stateBuffer.size());
      }
    }
    fa.setFromMyDFA(out);
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
        IntList newO = new IntArrayList(metastates.size());
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
