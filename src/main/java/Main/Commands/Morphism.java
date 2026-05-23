package Main.Commands;

import Main.Prover;
import Main.Session;

import java.io.IOException;

public class Morphism {
  public static void morphismCommand(String morphismDefinition, String name) throws IOException {
    Automata.Morphism M = new Automata.Morphism(morphismDefinition);
    System.out.print("Defined with domain ");
    System.out.print(M.mapping.keySet());
    System.out.print(" and range ");
    System.out.print(M.range);
    M.write(Session.getAddressForResult() + name + Prover.TXT_EXTENSION);
    M.write(Session.getWriteAddressForMorphismLibrary() + name + Prover.TXT_EXTENSION);
  }
}
