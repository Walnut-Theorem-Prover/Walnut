package Main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

  public static final String CONVERTED = "Converted";
  public static final String CONVERTING = "Converting";

  public static final String DETERMINIZED = "Determinized";
  public static final String DETERMINIZING = "Determinizing";

  public static final String MINIMIZED = "Minimized";
  public static final String MINIMIZING = "Minimizing";

  private static final String CONSOLE_LOGGER_NAME = "Walnut.Console";
  private static final String COMMAND_LOGGER_NAME = "Walnut.CommandLog";
  private static final String DETAILED_LOGGER_NAME = "Walnut.DetailedLog";

  public static final String GLOBAL_LOG_FILENAME = "global_log.txt";

  private static final Logger consoleLogger = LoggerFactory.getLogger(CONSOLE_LOGGER_NAME);
  private static final Logger commandLogger = LoggerFactory.getLogger(COMMAND_LOGGER_NAME);
  private static final Logger detailedLogger = LoggerFactory.getLogger(DETAILED_LOGGER_NAME);

  private static BufferedWriter globalLogWriter;
  private static boolean globalLogHasContent = false;

  private static boolean printSteps = false;
  private static boolean printDetails = false;
  private static boolean evalLogFilesActive = false;
  private static StringBuilder commandLog = new StringBuilder();
  private static StringBuilder detailedLog = new StringBuilder();
  private static int indentCount = 0;
  private static boolean printEnabled = true;

  public static void initializeGlobalLog(String filename) {
    closeGlobalLogWriter();
    try {
      Path path = Path.of(filename);
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      globalLogWriter = Files.newBufferedWriter(
          path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
      globalLogHasContent = false;
    } catch (IOException e) {
      System.out.println("Could not create global log file " + filename);
    }
  }

  private static void closeGlobalLogWriter() {
    if (globalLogWriter == null) {
      return;
    }
    try {
      globalLogWriter.close();
    } catch (IOException ignored) {
      // There is nowhere useful to report this during shutdown.
    } finally {
      globalLogWriter = null;
    }
  }

  public static void logCommand(String command) {
    if (command == null) {
      command = "";
    }
    if (globalLogWriter == null) {
      return;
    }
    try {
      if (globalLogHasContent) {
        globalLogWriter.newLine();
      }
      globalLogWriter.write(">>> " + command);
      globalLogWriter.newLine();
      globalLogWriter.flush();
      globalLogHasContent = true;
    } catch (IOException ignored) {
      // Do not let logging failures interfere with Walnut commands.
    }
  }

  public static void configureForCommand(boolean shouldPrintSteps, boolean shouldPrintDetails) {
    printSteps = shouldPrintSteps;
    printDetails = shouldPrintDetails;
    evalLogFilesActive = false;
    commandLog = new StringBuilder();
    detailedLog = new StringBuilder();
  }

  public static boolean shouldPrintDetails() {
    return printEnabled && printDetails;
  }

  public static boolean shouldPrintStepsOrDetails() {
    return printEnabled && (printSteps || printDetails);
  }

  public static String getCommandLog() {
    return commandLog.toString();
  }

  public static String getDetailedLog() {
    return printDetails ? detailedLog.toString() : "";
  }

  public static CommandLogContext writeEvalLogsTo(String resultName) {
    return new CommandLogContext(
        addFileAppender(COMMAND_LOGGER_NAME, resultName + "_log.txt"),
        printDetails ? addFileAppender(DETAILED_LOGGER_NAME, resultName + "_detailed_log.txt") : null,
        evalLogFilesActive);
  }

  public static void indent() {
    indentCount++;
  }
  public static void dedent() {
    indentCount--;
  }
  public static void resetIndent() { indentCount = 0;} // useful for integration tests

  // temporarily disable print for helper calls
  public static void disablePrint() { printEnabled = false; }
  public static void enablePrint() { printEnabled = true; }

  public static void logMessage(String msg) {
    logMessage(printDetails, msg);
  }

  public static void logMessage(boolean print, String msg) {
    if (printEnabled && print) {
      logDetail(msg, true);
    }
  }

  public static void logAndPrint(String msg) {
    logAndPrint(printDetails, msg);
  }

  public static void logAndPrint(boolean print, String msg) {
    logDetail(msg, print);
  }

  public static void logEvaluationStep(String msg, boolean finalLine) {
    String msgWithIndent = " ".repeat(indentCount) + msg;
    append(commandLog, msgWithIndent, finalLine);
    commandLogger.info(msgWithIndent);
    writeGlobalLogLine(msgWithIndent);

    if (printDetails) {
      append(detailedLog, msgWithIndent, finalLine);
      detailedLogger.info(msgWithIndent);
    }

    if (shouldPrintStepsOrDetails()) {
      consoleLogger.info(msgWithIndent);
    }
  }

  private static void logDetail(String msg, boolean print) {
    String msgWithIndent = " ".repeat(indentCount) + msg;
    writeGlobalLogLine(msgWithIndent);

    if (printDetails) {
      appendLine(detailedLog, msgWithIndent);
      detailedLogger.info(msgWithIndent);
    }

    if (!evalLogFilesActive) {
      appendLine(commandLog, msgWithIndent);
      commandLogger.info(msgWithIndent);
    }

    if (printEnabled && print) {
      consoleLogger.info(msgWithIndent);
    }
  }

  private static void writeGlobalLogLine(String msg) {
    if (msg == null) {
      msg = "";
    }
    if (globalLogWriter == null) {
      return;
    }
    try {
      globalLogWriter.write(msg);
      globalLogWriter.newLine();
      globalLogWriter.flush();
      globalLogHasContent = true;
    } catch (IOException ignored) {
      // Do not let logging failures interfere with Walnut commands.
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
      evalLogFilesActive = true;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closeAppender(detailedAppender);
      closeAppender(commandAppender);
      evalLogFilesActive = previousEvalLogFilesActive;
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
      writeGlobalLogLine(e.getMessage());
      // handled Walnut exception; only print message
    } else {
      // Create a truncated stack trace
      StackTraceElement[] fullStack = e.getStackTrace();
      e.setStackTrace(Arrays.copyOf(fullStack, Math.min(fullStack.length, length)));
      StringWriter stackTrace = new StringWriter();
      e.printStackTrace(new PrintWriter(stackTrace));
      writeGlobalLogLine(stackTrace.toString().stripTrailing());
      e.printStackTrace();
    }
  }
}
