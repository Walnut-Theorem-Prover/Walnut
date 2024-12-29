package Main;

import org.junit.jupiter.api.Test;

public class HelpMessagesTest {
  @Test
  void testHelpMessages() {
    new HelpMessages(); // coverage
    HelpMessages.helpCommand("help;");
    HelpMessages.helpCommand("help TestDir");
    HelpMessages.helpCommand("help testfile");
    HelpMessages.helpCommand("help TestDir testfile");
    HelpMessages.helpCommand("help TestDir nonexistent");
    HelpMessages.helpCommand("help nonexistent");
    HelpMessages.helpCommand("help a b c");
  }
}
