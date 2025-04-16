import java.io.*;
import java.util.Scanner;

public class CommandLineInterpreter {

    private static File currentDirectory = new File(System.getProperty("user.dir"));

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;

        System.out.println("Welcome to CLI! Type 'help' for a list of commands.");

        while (true) {
            System.out.print("> ");
            input = scanner.nextLine().trim();

            // Handling pipes and redirection
            if (input.contains("|")) {
                // Handling pipe (|)
                String[] pipeParts = input.split("\\|");
                String command1 = pipeParts[0].trim();
                String command2 = pipeParts[1].trim();

                String command1Output = processCommand(command1, null);
                String finalOutput = pipeCommand(command1Output, command2);
                System.out.println(finalOutput);
            } else if (input.contains(">") || input.contains(">>")) {
                // Handling redirection (>) or (>>)
                boolean append = input.contains(">>");
                String[] redirectionParts = input.split(">");
                String command = redirectionParts[0].trim();
                String fileName = redirectionParts[1].trim();

                String commandOutput = processCommand(command, null);
                redirectToFile(commandOutput, fileName, append);
            } else {
                // Process other commands normally
                String result = processCommand(input, null);
                System.out.print(result); // Removed the newline to avoid double newlines in outputs.
            }
        }
    }

    // Processing and executing a command
    public static String processCommand(String input, String inputData) {
        String[] splitInput = input.split("\\s+");  // Splitting the command and its arguments
        String command = splitInput[0];
        String[] arguments = new String[splitInput.length - 1];
        System.arraycopy(splitInput, 1, arguments, 0, arguments.length);

        StringBuilder output = new StringBuilder();

        switch (command) {
            case "pwd":
                output.append(pwdCommand());
                break;
            case "cd":
                if (arguments.length > 0) {
                    output.append(cdCommand(arguments[0]));
                } else {
                    output.append("cd: Missing argument.\n");
                }
                break;
            case "ls":
                output.append(lsCommand(arguments));  // Pass flags (if any) to the ls command
                break;
            case "touch":
                if (arguments.length > 0) {
                    output.append(touchCommand(arguments)); // Pass all arguments to the touchCommand
                } else {
                    output.append("touch: Missing file name.\n");
                }
                break;
            case "mkdir":
                if (arguments.length > 0) {
                    output.append(mkdirCommand(arguments[0]));
                } else {
                    output.append("mkdir: Missing directory name.\n");
                }
                break;
            case "rmdir":
                if (arguments.length > 0) {
                    output.append(rmdirCommand(arguments[0]));
                } else {
                    output.append("rmdir: Missing directory name.\n");
                }
                break;
            case "mv":
                if (arguments.length > 1) {
                    output.append(mvCommand(arguments[0], arguments[1]));
                } else {
                    output.append("mv: Missing source or destination.\n");
                }
                break;
            case "rm":
                if (arguments.length > 0) {
                    output.append(rmCommand(arguments));
                } else {
                    output.append("rm: Missing file or directory name.\n");
                }
                break;
            case "cat":
                // Check if we are piping data into the 'cat' command
                if (inputData != null) {
                    output.append(inputData).append("\n");
                } else if (arguments.length > 0) {
                    output.append(catCommand(arguments[0]));
                } else {
                    output.append("cat: Missing file name.\n");
                }
                break;
            case "help":
                output.append(helpCommand());
                break;
            case "exit":
                System.exit(0);
            default:
                output.append("Invalid command: ").append(command).append("\n");
        }

        return output.toString();
    }

    // Command functions
    public static String pwdCommand() {
        return currentDirectory.getAbsolutePath() + "\n";
    }

    public static String cdCommand(String path) {
        File newDir = new File(currentDirectory, path);

        // If the path is absolute, use it directly
        if (new File(path).isAbsolute()) {
            newDir = new File(path);
        }

        // Handle navigating to the parent directory
        if (path.equals("..")) {
            newDir = currentDirectory.getParentFile();
            if (newDir != null) {
                currentDirectory = newDir;
                return "";
            } else {
                return "cd: No such file or directory\n";
            }
        }

        // Handle normal path changes
        if (newDir.exists() && newDir.isDirectory()) {
            currentDirectory = newDir.getAbsoluteFile();
            return "";
        } else {
            return "cd: No such file or directory\n";
        }
    }

    public static String lsCommand(String[] flags) {
        File[] files = currentDirectory.listFiles();

        if (files == null) {
            return "ls: Unable to read directory\n";
        }

        StringBuilder output = new StringBuilder();
        boolean showAll = false;
        boolean recursive = false;

        for (String flag : flags) {
            if (flag.equals("-a")) showAll = true;
            if (flag.equals("-r")) recursive = true;
        }

        for (File file : files) {
            if (!showAll && file.getName().startsWith(".")) {
                continue;
            }
            output.append(file.getName()).append("\n");

            if (recursive && file.isDirectory()) {
                output.append(lsRecursive(file));
            }
        }

        return output.toString();
    }

    public static String lsRecursive(File dir) {
        StringBuilder output = new StringBuilder();
        File[] files = dir.listFiles();
        if (files == null) return "";

        for (File file : files) {
            if (file.isDirectory()) {
                output.append(file.getName()).append("/\n");
                output.append(lsRecursive(file));
            } else {
                output.append(file.getName()).append("\n");
            }
        }

        return output.toString();
    }

    public static String touchCommand(String... fileNames) {
        StringBuilder output = new StringBuilder();

        for (String fileName : fileNames) {
            File file = new File(currentDirectory, fileName);
            try {
                if (file.createNewFile()) {
                    output.append("File created: ").append(fileName).append("\n");
                } else {
                    output.append("File already exists: ").append(fileName).append("\n");
                }
            } catch (IOException e) {
                output.append("Error creating file: ").append(fileName).append("\n");
            }
        }

        return output.toString();
    }

    public static String mkdirCommand(String dirName) {
        File dir = new File(currentDirectory, dirName);
        if (dir.mkdir()) {
            return "Directory created: " + dirName + "\n";
        } else {
            return "Unable to create directory: " + dirName + "\n";
        }
    }

    public static String rmdirCommand(String dirName) {
        File dir = new File(currentDirectory, dirName);
        if (dir.exists() && dir.isDirectory() && dir.delete()) {
            return "Directory removed: " + dirName + "\n";
        } else {
            return "Unable to remove directory: " + dirName + "\n";
        }
    }

    public static String mvCommand(String source, String destination) {
        File sourceFile = new File(currentDirectory, source);
        File destFile = new File(currentDirectory, destination);

        if (sourceFile.renameTo(destFile)) {
            return "Moved " + source + " to " + destination + "\n";
        } else {
            return "Unable to move file or directory\n";
        }
    }

    public static String rmCommand(String... args) {
        // Check if the -r flag is present
        boolean recursive = false;
        String target = "";

        // Parsing the arguments to check for -r
        for (String arg : args) {
            if (arg.equals("-r")) {
                recursive = true;
            } else {
                target = arg;  // The file/directory name
            }
        }

        File file = new File(currentDirectory, target);
        if (!file.exists()) {
            return "rm: File or directory not found: " + target + "\n";
        }

        if (file.isDirectory()) {
            if (recursive) {
                if (deleteDirectoryRecursively(file)) {
                    return "Directory and its contents removed: " + target + "\n";
                } else {
                    return "rm: Unable to remove directory: " + target + "\n";
                }
            } else {
                return "rm: Cannot remove '" + target + "': Is a directory\n";
            }
        } else {
            if (file.delete()) {
                return "File removed: " + target + "\n";
            } else {
                return "rm: Unable to remove file: " + target + "\n";
            }
        }
    }

    public static boolean deleteDirectoryRecursively(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (!deleteDirectoryRecursively(file)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static String catCommand(String fileName) {
        File file = new File(currentDirectory, fileName);
        if (!file.exists()) {
            return "File not found: " + fileName + "\n";
        } else {

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            } catch (IOException e) {
                return "Error reading file: " + fileName + "\n";
            }
        }
    }

    public static String helpCommand() {
        return """
                Available commands:
                pwd                Print working directory
                cd <dir>           Change directory
                ls [-a] [-r]       List directory contents, -a to show hidden files, -r for recursive listing
                touch <file>       Create a new file
                mkdir <dir>        Create a new directory
                rmdir <dir>        Remove an empty directory
                rm [-r] <target>   Remove a file or directory (use -r for recursive directory deletion)
                mv <src> <dest>    Move or rename a file or directory
                cat <file>         Display file contents
                exit               Exit the CLI
                help               Show this help message
                """;
    }

    public static String pipeCommand(String input, String command) {
        return processCommand(command, input);
    }

    public static void redirectToFile(String content, String fileName, boolean append) {
        File file = new File(currentDirectory, fileName);
        try (FileWriter writer = new FileWriter(file, append)) {
            writer.write(content + "\n"); // Ensure content ends with a newline
        } catch (IOException e) {
            System.out.println("Error writing to file: " + fileName);
        }
    }
}
