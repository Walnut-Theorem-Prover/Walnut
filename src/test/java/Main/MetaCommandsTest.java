package Main;

import Automata.FA.DeterminizationStrategies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetaCommandsTest {
  @Test
  void testParseStrategy() {
    MetaCommands mc = new MetaCommands();
    Assertions.assertEquals("", mc.parseMetaCommands("", true));

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[strategy 5 CCL x]", true);
    });

    Assertions.assertEquals("", mc.parseMetaCommands("[strategy 5 CCLS]", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.CCLS, mc.getStrategy(5));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy 5 CCLS][strategy 10 BRZ-CCLS]blah", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.CCLS, mc.getStrategy(5));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.BRZ_CCLS, mc.getStrategy(10));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy * CCLS]blah", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.CCLS, mc.getStrategy(0));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.CCLS, mc.getStrategy(1));
  }

  @Test
  void testParseExport() {
    MetaCommands mc = new MetaCommands();
    Assertions.assertEquals("", mc.parseMetaCommands("[export 5 BA]", true));
    Assertions.assertNull(mc.getExportName(0));

    Assertions.assertThrows(WalnutException.class, () -> {
      new MetaCommands().parseMetaCommands("[export 5]", true);
    });
    Assertions.assertThrows(WalnutException.class, () -> {
      new MetaCommands().parseMetaCommands("[export 5 BLAH]", true);
    });

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[export * TXT]blah", true));
    Assertions.assertEquals("txt", mc.getExportFormat(0));

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[export 5 x]", true);
    });
  }

  @Test
  void testParseBogusCommand() {
    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[strategy 5 OTF", true);
    });

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[bogusCommand 5 x]", true);
    });

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[strategy OTF 5]", false);
    });
  }
}
