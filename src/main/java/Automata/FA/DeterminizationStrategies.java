package Automata.FA;

import MRC.Index.AntichainForestIndex;
import MRC.Index.OTFIndex;
import MRC.Model.DeterminizeRecord;
import MRC.Model.MyDFA;
import MRC.Model.MyNFA;
import MRC.Model.Threshold;
import MRC.NFATrim;
import Main.UtilityMethods;
import MRC.OnTheFlyDeterminization;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.automatalib.alphabet.Alphabet;
import simpleRABIT.algorithms.ParallelSimulation;

import java.util.*;

/**
 * Determinization logic, with support for alternative strategies of Brzozowski, OTF, and OTF-Brzozowski
 * Automata are numbered as they are determinized, which is useful for meta-commands like [export] and [strategy]
 */
public class DeterminizationStrategies {
  private static final Int2ObjectMap<Strategy> strategyMap = new Int2ObjectOpenHashMap<>();

  public static Int2ObjectMap<Strategy> getStrategyMap() {
    return strategyMap;
  }

  public enum Strategy {
    SC("SC", List.of("SC")), BRZ("Brzozowski", List.of("Brz")),
    OTF("OTF", List.of("OTF")), OTF_BRZ("OTF-Brzozowski", List.of("OTF_BRZ", "OTF-BRZ"));
    private final String name;
    private final List<String> aliases;

    Strategy(String name, List<String> aliases) {
      this.name = name;
      this.aliases = new ArrayList<>(aliases);
      this.aliases.add(name);
    }
    public static Strategy fromString(String name) {
      for (Strategy strategy : Strategy.values()) {
        for(String alias: strategy.aliases) {
          if (name.equalsIgnoreCase(alias)) {
            return strategy;
          }
        }
      }
      throw new IllegalArgumentException("No strategy found for: " + name);
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
        fa.setNfaD(null);
      }
      if (fa.getDfaD() != null) {
        fa.setNfaD(null);
      }
      long timeBefore = System.currentTimeMillis();
      int automataIdx = FA.IncrementIndex();

      strategy = (strategy != null) ? strategy : strategyMap.getOrDefault(automataIdx, Strategy.SC);
      UtilityMethods.logMessage(print, prefix +
              "Determinizing " + getStrategy(automataIdx, strategy) +": " + fa.getQ() + " states", log);

      switch (strategy) {
        case SC -> SC(fa, initialState, print, prefix, log, timeBefore);
        case BRZ, OTF_BRZ -> Brz(fa, initialState, strategy, print, prefix, log, timeBefore);
        case OTF -> OTF(fa, initialState, print, prefix, log, timeBefore);
      }

      long timeAfter = System.currentTimeMillis();
      UtilityMethods.logMessage(
          print, prefix + "Determinized: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    }

  /**
   * Brzozowski's strategy for SC (or OTF).
   * @param fa - finite automaton
   * @param origInitialStates - original initial states
   * @param strategy - BRZ or OTF_BRZ
   */
  private static void Brz(
      FA fa, IntSet origInitialStates, Strategy strategy, boolean print, String prefix, StringBuilder log, long timeBefore) {
      // Reverse
    IntSet newInitialStates = fa.reverseToNFAInternal(origInitialStates);
    UtilityMethods.logMessage(print, prefix + "Reversed -- Determinizing.", log);
    if (strategy.equals(Strategy.BRZ)) {
      SC(fa, newInitialStates, print, prefix, log, timeBefore);
    } else {
      OTF(fa, newInitialStates, print, prefix, log, timeBefore);
    }
    long timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(
        print, prefix + "Reversed: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
    fa.justMinimize(print, prefix + " ", log); // also switches back to NFA representation

    // Reverse again. Note that initial state of SC is q0
    newInitialStates = fa.reverseToNFAInternal(IntSet.of(fa.getQ0()));
    UtilityMethods.logMessage(print, prefix + "Reverse of reverse -- Determinizing.", log);
    timeBefore = System.currentTimeMillis();
    if (strategy.equals(Strategy.BRZ)) {
      SC(fa, newInitialStates, print, prefix, log, timeBefore);
    } else {
      OTF(fa, newInitialStates, print, prefix, log, timeBefore);
    }
    timeAfter = System.currentTimeMillis();
    UtilityMethods.logMessage(
        print, prefix + "Reverse of reverse: " + fa.getQ() + " states - " + (timeAfter - timeBefore) + "ms", log);
  }

  private static void SC(
      FA fa, IntSet initialState, boolean print, String prefix, StringBuilder log, long timeBefore) {
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
            // TODO: BitSet may be a better choice, but it's not clear when NFA size is, say, >> 20000.
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
  }

  private static void OTF(
      FA fa, IntSet initialState, boolean print, String prefix, StringBuilder log, long timeBefore) {
    MyNFA<Integer> myNFA = fa.FAtoMyNFA();
    // Replace initial states
    for(int i: myNFA.getInitialStates()) {
      myNFA.setInitial(i, false);
    }
    for(int i: initialState) {
      myNFA.setInitial(i, true);
    }
    MyNFA<Integer> reduced = NFATrim.bisim(myNFA);
    if (reduced.size() < fa.getQ()) {
      System.out.println("Bisimulation reduced to " + reduced.size() + " states");
    }
    int prevSize = reduced.size();
    ArrayList<BitSet> simRels = new ArrayList<>();
    reduced = ParallelSimulation.fullyComputeRels(reduced, simRels);
    if (reduced.size() != prevSize) {
      System.out.println("Sim altered to: " + reduced.size());
    }
    final Threshold threshold = Threshold.adaptiveSteps(4000);
    OTFIndex index = new AntichainForestIndex<>(reduced, simRels.toArray(new BitSet[0]));
    Alphabet<Integer> inputs = reduced.getInputAlphabet();
    MyNFA<Integer>.CompactPowersetView nfa = reduced.powersetView();
    Deque<DeterminizeRecord<BitSet>> stack = new ArrayDeque<>();
    BitSet init = nfa.getInitialState();
    boolean initAcc = nfa.isAccepting(init);
    MyDFA<Integer> out = new MyDFA<>(reduced.getInputAlphabet());
    int initOut = out.addInitialState(initAcc);
    index.put(init, initOut);
    stack.push(new DeterminizeRecord<>(init, initOut));
    BitSet finishedStates = new BitSet();
    Deque<Integer> stateBuffer = new ArrayDeque<>();
    int numInputs = inputs.size();

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
        BitSet prune = index.prune(nfa.getTransition(inState, i, numInputs));
        int outSucc = index.get(prune);
        if (outSucc == -1) {
          complete = false;
          boolean succAcc = nfa.isAccepting(prune);
          if (stateBuffer.isEmpty()) {
            outSucc = out.addState(succAcc);
          } else {
            outSucc = stateBuffer.pop();
            out.setAccepting(outSucc, succAcc);
          }
          index.put(prune, outSucc);
          stack.push(new DeterminizeRecord<>(prune, outSucc));
        }
        out.setTransition(outState, i, outSucc);
      }

      finishedStates.set(outState);
      if (complete && threshold.test(out)) {
        OnTheFlyDeterminization.minimizeReuse(inputs, out, finishedStates, stateBuffer, index);
        threshold.update(out.size() - stateBuffer.size());
      }
    }
    // Final minimization
    OnTheFlyDeterminization.minimizeReuse(inputs, out, finishedStates, stateBuffer, index);
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
