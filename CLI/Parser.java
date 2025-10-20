package CLI;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    String commandName;
    String args[];
    
    public boolean parse(String command){
        // Check if the commnd is empty(null) or consists of white spaces only
        if (command == null) {
            return false;
        }
        else if (command.trim().isEmpty()) {
            return false;
        }
        // Make an array to store the whole command 
        String[] commands = command.split(" ");
        commandName=commands[0];
        
        // We didn't specify size of the array so we will specify it now with number of commands-1 
        args = new String[commands.length-1];
        
        for (int i = 1; i < commands.length; i++) {
            args[i-1]=commands[i];
        }

        return true;
    }

    
    public String getCommandName(){
        return commandName;
    }
    
    public String[] getArgs(){
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

    public void cd(String[] args) {
        if (args.length == 1 || args.length == 0) {

            Path newPath;
            // if No arguments: go to home directory
            if (args.length == 0) {
                newPath = homePath;
            } 
            else {
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

                if (Files.isDirectory(newPath)) {
                    currentPath = newPath;
                    return;
                } else {
                    System.out.println("Error: Directory does not exist");
                    return;
                }
            }

            // Apply home path change if no args
            currentPath = newPath;

        } 
        else {
         System.out.println("Error: too many arguments");
            return;
        }
    }

    public String ls(){
        if (!Files.exists(currentPath))
        {
            System.out.println("Error:directory does not exist");
            return "";
        }

        if (!Files.isDirectory(currentPath))
        {
            System.out.println("Error:path is not a directory");
            return "";
        }

        try (var paths = Files.list(currentPath))
        {
            return paths.map(p -> p.getFileName().toString()).sorted().collect(Collectors.joining("\n"));
        } 
        catch (IOException error)
        {
            System.out.println("Error reading directory: " + error.getMessage());
            return "";
        }
    }

    public void rmdir(String[] args) throws IOException {
        /*
         1- * => removes all empty directories
         2- path => remove the given directory is it's empty
         3-  handle spaces in the name of the directory
        */
        if (args.length==0) {
            System.out.println("Error: rmdir takes one argument");
            return;
        }

        if ("*".equals(args[0])){
            // We need to loop over all the directories in the current path and check if they are ampty if so remove

            // Get all direcrotries in the current path
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(currentPath, Files::isDirectory)) {
            
                for (Path dir : dirStream) {

                    // We will go into each directory to see if it's empty or not 
                    try (DirectoryStream<Path> dir2 = Files.newDirectoryStream(dir)) {

                        if (!dir2.iterator().hasNext()) { 
                            Files.delete(dir);
                            
                        }
                    }
                }   
            }
            return;
        }
        else{
            // if the folder name has spaces
            String arg="";
            for (String string : args) {
                arg+=string+" ";
            }
            arg=arg.trim(); // To remove the last space

            // Get the path of the directory we want to delete
            Path dir = currentPath.resolve(arg).normalize().toAbsolutePath();

            // Check if the dir exists and is a directory not a file
            if (Files.isDirectory(dir)) {

                // check if the directory is empty
                try (DirectoryStream<Path> dir2 = Files.newDirectoryStream(dir)) {

                    if (!dir2.iterator().hasNext()) { 
                        Files.delete(dir);
                        return;
                    }
                    else {
                        System.out.print(dir.getFileName()+" is not empty!");
                    }
                }
                return;
            } 
            else {
                System.out.print("Error: Directory does not exist");
                return;
            }
            
        }

    } 
    public void rm(String[] args) throws IOException{
        // if the folder name has spaces
        String arg="";
        for (String string : args) {
            arg+=string+" ";
        }
        arg=arg.trim(); // To remove the last space

        // Path of the file 
        Path file = currentPath.resolve(arg);

        // Check if the file exists and is a file
        if (Files.isRegularFile(file)) {
            Files.delete(file);
        }
        else{
            System.out.print(file.getFileName() + " does not exist or is not a file");
        }
    }
   
    //===========================================
    //  mkdir command implementation
    //===========================================
   public void mkdir(String[] args) {
    // 1) Check if user passed at least one argument 
    if (args.length == 0) {
        System.out.println("mkdir: missing operand");
        return;
    }

    // 2) Loop over each argument (each dir name/path separately)
    for (String dirArg : args) {
        
        // Build the path for this directory
        Path dirPath = currentPath.resolve(dirArg).normalize().toAbsolutePath();
        
        // Check if directory exists
        if (Files.exists(dirPath)) {
            System.out.println("mkdir: cannot create directory '" + dirArg + "': File exists");
            continue; // move to next one
        }

        // Try to create directory (including parent folders)
        try {
            // createDirectories → supports nested like xxx/xxxx
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            System.out.println("mkdir: failed to create '" + dirArg + "': " + e.getMessage());
        }
    }
}


    private String runCommand(String cmd,String[] args) throws IOException{
        switch (cmd) {
            case "pwd":
                return pwd();
        
            case("ls"):
                return ls();
                
            case("cd"):
                cd(args);
                return ""; 
            
            case("rmdir"):
                rmdir(args);
                return ""; 
           
            case("rm"):
                rm(args);
                return ""; 

                
            case("mkdir"):
               mkdir(args);
               return "";


            default:
                System.out.print("Command Not Found");
                return "";
        }
        

    }


    public static void main(String[] args) throws IOException {
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
            System.out.println(terminal.runCommand(cmd,arg));
            
        }
        
        
        scanner.close();
    }
} 

