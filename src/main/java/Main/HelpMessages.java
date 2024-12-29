package Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class HelpMessages {
  /**
   * Main entry point for the "help" command.
   * <p>
   * Usage examples:
   * 1) help;
   * -> Lists all groups (folders) and all commands (.txt files) in each group
   * 2) help core;
   * -> Lists all commands in the "core" group only
   * 3) help run;
   * -> Searches all groups for "run.txt" (we assume only one match) and shows its contents
   * 4) help core run;
   * -> Shows the contents of "run.txt" in the "core" group
   */
  public static void helpCommand(String fullCommandLine) {
    try {
      String[] tokens = parseHelpArguments(fullCommandLine);
      String helpRoot = Session.getAddressForHelpCommands();

      if (tokens.length == 0) {
        // No arguments -> list all groups + subcommands
        listAllGroupsDetailed(helpRoot);
        return;
      }

      if (tokens.length == 1) {
        // Could either be a group or a single command (search in all groups)

        String maybeGroupOrCommand = tokens[0];

        if (isGroup(helpRoot, maybeGroupOrCommand)) {
          // If it's a known group, list all commands in that group
          listCommandsInGroup(helpRoot, maybeGroupOrCommand);
        } else {
          // Otherwise, assume it's a command name. We search all subdirectories
          // for maybeGroupOrCommand + ".txt"
          showCommandHelpAcrossAllGroups(helpRoot, maybeGroupOrCommand);
        }
        return;
      }

      if (tokens.length == 2) {
        // "help <group> <command>" -> only look in that group
        String group = tokens[0];
        String command = tokens[1];
        String groupPath = helpRoot + File.separator + group + File.separator;
        showCommandHelp(groupPath, command);
        return;
      }

      // If you wanted to support deeper nesting, you'd extend this further.
      System.out.println("Too many arguments. Usage: help [group] [command];");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Error using the help command");
    }
  }

  /**
   * parseHelpArguments()
   * --------------------
   * Removes:
   * - the leading "help" (assuming your parser or regex ensures it exists)
   * - optional trailing semicolon
   * Then splits on whitespace to produce arguments.
   */
  private static String[] parseHelpArguments(String fullCommandLine) {
    // Trim whitespace
    String s = fullCommandLine.trim();

    // Remove trailing semicolon if present
    if (s.endsWith(";")) {
      s = s.substring(0, s.length() - 1).trim();
    }

    // Remove the leading "help " (case-insensitive).
    // This uses a regex that matches "help" at the start, plus any trailing space.
    s = s.replaceFirst("(?i)^help\\s*", "").trim();

    // If nothing left, return empty array
    if (s.isEmpty()) {
      return new String[0];
    }

    // Split by whitespace
    return s.split("\\s+");
  }

  /**
   * isGroup()
   * ---------
   * Returns true if "groupName" is a valid subdirectory in "helpRoot"
   */
  private static boolean isGroup(String helpRoot, String groupName) {
    File groupDir = new File(helpRoot, groupName);
    return groupDir.exists() && groupDir.isDirectory();
  }

  /**
   * listAllGroupsDetailed()
   * -----------------------
   * When the user just types "help;", show:
   * - each group name
   * - the commands (".txt" files) in that group
   */
  private static void listAllGroupsDetailed(String helpRoot) {
    File root = new File(helpRoot);
    File[] directories = root.listFiles(File::isDirectory);

    System.out.println("Available help groups and commands:");
    if (directories == null || directories.length == 0) {
      return;
    }
    Arrays.sort(directories, (d1, d2) -> d1.getName().compareToIgnoreCase(d2.getName()));
    for (File dir : directories) {
      System.out.println("Group: " + dir.getName());
      File[] txtFiles = dir.listFiles((d, name) -> name.endsWith(".txt"));
      if (txtFiles != null && txtFiles.length > 0) {
        Arrays.sort(txtFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        System.out.println("  Commands:");
        outputCommands(txtFiles, "   - ");
      }
      System.out.println();
    }
  }

  /**
   * listCommandsInGroup()
   * ---------------------
   * Lists all "*.txt" files in the given groupName folder.
   */
  private static void listCommandsInGroup(String helpRoot, String groupName) {
    File groupDir = new File(helpRoot, groupName);
    File[] txtFiles = groupDir.listFiles((dir, name) -> name.endsWith(".txt"));

    System.out.println("Commands in group \"" + groupName + "\":");
    if (txtFiles == null || txtFiles.length == 0) {
      return;
    }
    Arrays.sort(txtFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
    outputCommands(txtFiles, " - ");
  }

  private static void outputCommands(File[] txtFiles, String x) {
    for (File txt : txtFiles) {
      String command = txt.getName().replaceFirst("\\.txt$", "");
      System.out.println(x + command);
    }
  }

  /**
   * showCommandHelpAcrossAllGroups()
   * --------------------------------
   * Searches *all* subdirectories for "commandName.txt".
   * We assume only one matching file. If found, we display it.
   * Otherwise, we print an error message.
   */
  private static void showCommandHelpAcrossAllGroups(String helpRoot, String commandName) throws IOException {
    File rootDir = new File(helpRoot);
    if (!rootDir.exists() || !rootDir.isDirectory()) {
      return;
    }

    // Look for "commandName.txt" in each subgroup
    File[] groups = rootDir.listFiles(File::isDirectory);
    if (groups == null || groups.length == 0) {
      return;
    }

    File foundFile = null;
    for (File groupDir : groups) {
      File candidate = new File(groupDir, commandName + ".txt");
      if (candidate.exists() && candidate.isFile()) {
        foundFile = candidate;
        break;
      }
    }

    if (foundFile == null) {
      System.out.println("No documentation found for command \"" + commandName + "\".");
    } else {
      printHelpFile(foundFile, commandName);
    }
  }

  /**
   * showCommandHelp()
   * -----------------
   * Looks for "<command>.txt" in the given path and prints its content.
   * This is used when we already know which group we're in.
   */
  private static void showCommandHelp(String groupPath, String command) throws IOException {
    File file = new File(groupPath + command + ".txt");
    if (!file.exists()) {
      System.out.println("No documentation found for command \"" + command + "\" in this group.");
      return;
    }
    printHelpFile(file, command);
  }

  private static void printHelpFile(File file, String commandName) throws IOException {
    System.out.println("=== Help: " + commandName + " ===");
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        System.out.println(line);
      }
    }
    System.out.println("=============================");
  }
}
