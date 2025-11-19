package Automata;

import Main.WalnutException;

/**
 * Typesafe extension that requires determinism.
 */
// TODO - assert that this is deterministic
// TODO - use with deterministic transitions
// TODO - propagate throughout codebase
public class AutomatonDFA extends Automaton {
  @Override
  public AutomatonDFA clone() {
    if (!this.getFa().getT().isDeterministic()) {
      throw WalnutException.nonDeterministic();
    }
    return (AutomatonDFA) super.clone();
  }
}
