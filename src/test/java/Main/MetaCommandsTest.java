package Main;

import Automata.FA.DeterminizationStrategies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetaCommandsTest {
  @Test
  void testParseStrategy() {
    MetaCommands mc = new MetaCommands();
    Assertions.assertEquals("", mc.parseMetaCommands(""));

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[strategy OTF 5 x]");
    });

    Assertions.assertEquals("", mc.parseMetaCommands("[strategy OTF 5]"));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF, mc.getStrategy(5));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy OTF 5][strategy OTF-BRZ 10]blah"));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF, mc.getStrategy(5));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF_BRZ, mc.getStrategy(10));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[strategy OTF *]blah"));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF, mc.getStrategy(0));
    Assertions.assertEquals(DeterminizationStrategies.Strategy.OTF, mc.getStrategy(1));
  }

  @Test
  void testParseExport() {
    MetaCommands mc = new MetaCommands();
    Assertions.assertEquals("", mc.parseMetaCommands("[export 5]"));
    Assertions.assertNull(mc.getExportBAName(0));
    Assertions.assertEquals(MetaCommands.DEFAULT_EXPORT_NAME, mc.getExportBAName(5));

    mc = new MetaCommands();
    Assertions.assertEquals("blah", mc.parseMetaCommands("[export *]blah"));
    Assertions.assertEquals(MetaCommands.DEFAULT_EXPORT_NAME, mc.getExportBAName(0));
    Assertions.assertEquals(MetaCommands.DEFAULT_EXPORT_NAME, mc.getExportBAName(1));

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[export 5 x]");
    });
  }

  @Test
  void testParseBogusCommand() {
    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[strategy 5 OTF");
    });

    Assertions.assertThrows(RuntimeException.class, () -> {
      new MetaCommands().parseMetaCommands("[bogusCommand 5 x]");
    });
  }
}
