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
      new MetaCommands().parseMetaCommands("[strategy OTF 5 x]", true);
    });

    Assertions.assertEquals("", mc.parseMetaCommands("[strategy OTF-CCLS 5]", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_CCLS, mc.getStrategy(5));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy OTF-CCLS 5][strategy OTF-BRZ-CCLS 10]blah", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_CCLS, mc.getStrategy(5));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_BRZ_CCLS, mc.getStrategy(10));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy OTF-CCLS *]blah", true));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_CCLS, mc.getStrategy(0));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_CCLS, mc.getStrategy(1));
  }

  @Test
  void testParseExport() {
    MetaCommands mc = new MetaCommands();
    Assertions.assertEquals("", mc.parseMetaCommands("[export 5]", true));
    Assertions.assertNull(mc.getExportBAName(0));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[export *]blah", true));

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
