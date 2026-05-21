package Main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final String CONSOLE_LOGGER_NAME = "Walnut.Console";
  private static final String COMMAND_LOGGER_NAME = "Walnut.CommandLog";
  private static final String DETAILED_LOGGER_NAME = "Walnut.DetailedLog";

  private static final Logger consoleLogger = LoggerFactory.getLogger(CONSOLE_LOGGER_NAME);
  private static final Logger commandLogger = LoggerFactory.getLogger(COMMAND_LOGGER_NAME);
  private static final Logger detailedLogger = LoggerFactory.getLogger(DETAILED_LOGGER_NAME);

  private static final ThreadLocal<Boolean> printSteps = ThreadLocal.withInitial(() -> false);
  private static final ThreadLocal<Boolean> printDetails = ThreadLocal.withInitial(() -> false);
  private static final ThreadLocal<Boolean> evalLogFilesActive = ThreadLocal.withInitial(() -> false);
  private static final ThreadLocal<StringBuilder> commandLog = ThreadLocal.withInitial(StringBuilder::new);
  private static final ThreadLocal<StringBuilder> detailedLog = ThreadLocal.withInitial(StringBuilder::new);

  public static void configureForCommand(boolean shouldPrintSteps, boolean shouldPrintDetails) {
    printSteps.set(shouldPrintSteps);
    printDetails.set(shouldPrintDetails);
    evalLogFilesActive.set(false);
    commandLog.set(new StringBuilder());
    detailedLog.set(new StringBuilder());
  }

  public static boolean shouldPrintDetails() {
    return printDetails.get();
  }

  public static boolean shouldPrintStepsOrDetails() {
    return printSteps.get() || printDetails.get();
  }

  public static String getCommandLog() {
    return commandLog.get().toString();
  }

  public static String getDetailedLog() {
    return printDetails.get() ? detailedLog.get().toString() : "";
  }

  public static CommandLogContext writeEvalLogsTo(String resultName) {
    return new CommandLogContext(
        addFileAppender(COMMAND_LOGGER_NAME, resultName + "_log.txt"),
        printDetails.get() ? addFileAppender(DETAILED_LOGGER_NAME, resultName + "_detailed_log.txt") : null,
        evalLogFilesActive.get());
  }

  public static void logMessage(String msg) {
    logMessage(printDetails.get(), msg);
  }

  public static void logMessage(boolean print, String msg) {
    if (print) {
      logDetail(msg, true);
    }
  }

  /**
   * Legacy overload. The StringBuilder is intentionally ignored; logging state now lives here.
   */
  public static void logMessage(boolean print, String msg, StringBuilder ignoredLog) {
    logMessage(print, msg);
  }

  public static void logAndPrint(String msg) {
    logAndPrint(printDetails.get(), msg);
  }

  public static void logAndPrint(boolean print, String msg) {
    logDetail(msg, print);
  }

  /**
   * Legacy overload. The StringBuilder is intentionally ignored; logging state now lives here.
   */
  public static void logAndPrint(boolean print, String msg, StringBuilder ignoredLog) {
    logAndPrint(print, msg);
  }

  public static void logEvaluationStep(String msg) {
    logEvaluationStep(msg, false);
  }

  public static void logFinalEvaluationStep(String msg) {
    logEvaluationStep(msg, true);
  }

  private static void logEvaluationStep(String msg, boolean finalLine) {
    append(commandLog.get(), msg, finalLine);
    commandLogger.info(msg);

    if (printDetails.get()) {
      append(detailedLog.get(), msg, finalLine);
      detailedLogger.info(msg);
    }

    if (shouldPrintStepsOrDetails()) {
      consoleLogger.info(msg);
    }
  }

  private static void logDetail(String msg, boolean print) {
    if (printDetails.get()) {
      appendLine(detailedLog.get(), msg);
      detailedLogger.info(msg);
    }

    if (!evalLogFilesActive.get()) {
      appendLine(commandLog.get(), msg);
      commandLogger.info(msg);
    }

    if (print) {
      consoleLogger.info(msg);
    }
  }

  private static void appendLine(StringBuilder log, String msg) {
    append(log, msg, false);
  }

  private static void append(StringBuilder log, String msg, boolean finalLine) {
    log.append(msg);
    if (!finalLine) {
      log.append(System.lineSeparator());
    }
  }

  private static FileLogAppender addFileAppender(String loggerName, String filename) {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (!(loggerFactory instanceof LoggerContext loggerContext)) {
      return null;
    }

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%msg%n");
    encoder.start();

    FileAppender<ILoggingEvent> appender = new FileAppender<>();
    appender.setContext(loggerContext);
    appender.setName(loggerName + "." + Integer.toHexString(filename.hashCode()) + "." + System.nanoTime());
    appender.setFile(filename);
    appender.setAppend(false);
    appender.setImmediateFlush(true);
    appender.setEncoder(encoder);
    appender.start();

    ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
    logger.setAdditive(false);
    logger.setLevel(Level.INFO);
    logger.addAppender(appender);

    return new FileLogAppender(logger, appender, encoder);
  }

  public static final class CommandLogContext implements AutoCloseable {
    private final FileLogAppender commandAppender;
    private final FileLogAppender detailedAppender;
    private final boolean previousEvalLogFilesActive;
    private boolean closed;

    private CommandLogContext(
        FileLogAppender commandAppender,
        FileLogAppender detailedAppender,
        boolean previousEvalLogFilesActive) {
      this.commandAppender = commandAppender;
      this.detailedAppender = detailedAppender;
      this.previousEvalLogFilesActive = previousEvalLogFilesActive;
      evalLogFilesActive.set(true);
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closeAppender(detailedAppender);
      closeAppender(commandAppender);
      evalLogFilesActive.set(previousEvalLogFilesActive);
      closed = true;
    }

    private void closeAppender(FileLogAppender logAppender) {
      if (logAppender == null) {
        return;
      }
      logAppender.logger.detachAppender(logAppender.appender);
      logAppender.appender.stop();
      logAppender.encoder.stop();
    }
  }

  private record FileLogAppender(
      ch.qos.logback.classic.Logger logger,
      FileAppender<ILoggingEvent> appender,
      PatternLayoutEncoder encoder) {}

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
