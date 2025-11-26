package Main;

import java.util.Arrays;

public class Logging {

  public static final String APPLIED = "applied";
  public static final String APPLYING = "applying";

  public static final String COMPARED = "compared";
  public static final String COMPARING = "comparing";

  public static final String COMPUTED = "computed";
  public static final String COMPUTING = "computing";

  public static final String FIXED = "fixed";
  public static final String FIXING = "fixing";

  public static final String QUANTIFIED = "quantified";
  public static final String QUANTIFYING = "quantifying";

  public static final String REMOVED = "removed";
  public static final String REMOVING = "removing";

  public static final String REVERSED = "reversed";
  public static final String REVERSING = "reversing";

  public static final String TOTALIZED = "totalized";
  public static final String TOTALIZING = "totalizing";

  // note caps. This is historical, and just to avoid changing lots of code.

  public static final String DETERMINIZED = "Determinized";
  public static final String DETERMINIZING = "Determinizing";

  public static final String MINIMIZED = "Minimized";
  public static final String MINIMIZING = "Minimizing";

  public static void logMessage(boolean print, String msg, StringBuilder log) {
    if (print) {
      if (log != null) { log.append(msg).append(System.lineSeparator()); }
      System.out.println(msg);
    }
  }

  public static void logAndPrint(boolean print, String msg, StringBuilder log) {
    if (log != null) { log.append(msg).append(System.lineSeparator()); }
    if (print) {
      System.out.println(msg);
    }
  }

  /**
   * Create a truncated stack trace so users don't see a full screen stack dump
   */
  public static void printTruncatedStackTrace(Exception e) {
    printTruncatedStackTrace(e, 1); // vaguely friendly stack length
  }

  public static void printTruncatedStackTrace(Exception e, int length) {
    if (e instanceof WalnutException) {
      System.out.println(e.getMessage());
      // handled Walnut exception; only print message
    } else {
      // Create a truncated stack trace
      StackTraceElement[] fullStack = e.getStackTrace();
      e.setStackTrace(Arrays.copyOf(fullStack, Math.min(fullStack.length, length)));
      e.printStackTrace();
    }
  }
}
