// package CLI;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.*;
import java.util.stream.Collectors;
import java.io.IOException;

public class Parser {
    String commandName;
    String args[];

    public boolean parse(String command) {
        // Check if the commnd is empty(null) or consists of white spaces only
        if (command == null) {
            return false;
        } else if (command.trim().isEmpty()) {
            return false;
        }
        // Make an array to store the whole command
        String[] commands = command.split(" ");
        commandName = commands[0];

        // We didn't specify size of the array so we will specify it now with number of
        // commands-1
        args = new String[commands.length - 1];

        for (int i = 1; i < commands.length; i++) {
            args[i - 1] = commands[i];
        }

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }
}

class Terminal {
    Parser parser = new Parser();
    private Path currentPath = Paths.get(System.getProperty("user.dir"));
    private final Path homePath = Paths.get(System.getProperty("user.home"));

    public String pwd() {
        return currentPath.toAbsolutePath().toString();
    }

    public String ls() {
        if (!Files.exists(currentPath)) {
            System.out.println("Error:directory does not exist");
            return "";
        }

        if (!Files.isDirectory(currentPath)) {
            System.out.println("Error:path is not a directory");
            return "";
        }

        try (var paths = Files.list(currentPath)) {
            return paths.map(p -> p.getFileName().toString()).sorted().collect(Collectors.joining("\n"));
        } catch (IOException error) {
            System.out.println("Error reading directory: " + error.getMessage());
            return "";
        }
    }

    

    public void cd(String[] args) {
        if (args.length == 1 || args.length == 0) {

        Path newPath;
        // if No arguments: go to home directory
        if (args.length == 0) {
            newPath = homePath;
        } else {
            String arg = args[0];
            // Go to parent directory
            if ("..".equals(arg)) {
                Path parentPath = currentPath.getParent();
                if (parentPath == null) {
                    return;
                }else{
                    currentPath = parentPath;
                    return;
                }
            }

            // Navigate to a specific directory
            newPath = currentPath.resolve(arg);
            newPath = newPath.normalize().toAbsolutePath();

            if (Files.exists(newPath) && Files.isDirectory(newPath)) {
                currentPath = newPath;
                return;
            } else {
                System.out.println("Error: Directory does not exist");
                return;
            }
        }

        // Apply home path change if no args
        currentPath = newPath;

        } else {
         System.out.println("Error: too many arguments");
            return;
        }
    }



    private String runCommand(String cmd, String[] args) {
        switch (cmd) {
            case "pwd":
                return pwd();

            case ("ls"):
                return ls();
            case ("cd"):
                cd(args);
                return "";

            default:
                System.out.print("Command Not Found");
                return "";
        }

    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(terminal.currentPath + "> ");
            // Read input
            String command = scanner.nextLine();

            // Parse commnd into command and arguments
            terminal.parser.parse(command);
            String cmd = terminal.parser.getCommandName();
            String[] arg = terminal.parser.getArgs();

            // Check if the command equals to exit to break the loop
            if (cmd.equals("exit")) {
                break;
            }
            // run command
            System.out.println(terminal.runCommand(cmd, arg));

        }

        scanner.close();
    }
}
