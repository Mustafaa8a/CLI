package CLI;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.*;
import java.io.FileOutputStream;


public class Parser {
    String commandName;
    String args[];

    // Holds the file name that the output should be redirected to (if any).
   public String redirectFile = null;

   // Determines whether the output should be appended to an existing file (>>) 
   // or overwrite the file content (>).
   public boolean appendMode = false;

    
 public boolean parse(String command) {
    //  Check if the command is null or empty
    if (command == null ) {
        return false;
    }
    else if(command.trim().isEmpty()){
        return false;
    }

    //  Split by spaces
    String[] tokens = command.trim().split("\\s+");
    commandName = tokens[0];

    // Initial values in case there's no redirection
    redirectFile = null;
    appendMode = false;

    //  Count args without redirection
    int argCount = 0;
    for (int i = 1; i < tokens.length; i++) {
        if (tokens[i].equals(">") || tokens[i].equals(">>")) break;
        argCount++;
    }

    //  Create args array
    args = new String[argCount];
    for (int i = 1; i <= argCount; i++) {
        args[i - 1] = tokens[i];
    }

    //  Check for redirection (if exists)
    for (int i = 1; i < tokens.length; i++) {
        if (tokens[i].equals(">") || tokens[i].equals(">>")) {
            appendMode = tokens[i].equals(">>");
            if (i + 1 < tokens.length) {
                redirectFile = tokens[i + 1];
            } else {
                System.out.println("Error: Missing file name after redirection.");
                return false;
            }
            break;
        }
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
        if (args.length > 1) {
            System.out.println("Error: too many arguments");
            return;
        }

        // if No arguments: go to home directory
        if (args.length == 0) {
            currentPath = homePath;
            return;
        } 
        Path newPath;
    
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

        
        try {

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
            
        } catch (Exception e) {
            System.out.println("Error: "+ e.getMessage());
        }
        
    }

    public String ls()
    {
        // Check if the current directory path exists
        if (!Files.exists(currentPath))
        {
            System.out.println("Error:directory does not exist");
            return "";
        }

        // Ensure that the current path is actually a directory
        if (!Files.isDirectory(currentPath))
        {
            System.out.println("Error:path is not a directory");
            return "";
        }

        try (var paths = Files.list(currentPath))
        {
            // Convert each Path to its file name, sort them alphabetically, and join them into one string with new lines
            return paths.map(p -> p.getFileName().toString()).sorted().collect(Collectors.joining("\n"));
        } 
        catch (IOException error)
        {
            System.out.println("Error reading directory: " + error.getMessage());
            return "";
        }
    }

public void cp(String[] args) 
{
    // Check if user passed at least 2 arguments (source and destination)
    if (args.length < 2) 
    {
        System.out.println("Error: cp requires exactly two arguments");
        return;
    }

    boolean recursive = false;
    int startIndex = 0;

    // Check if the first argument is "-r" (recursive copy)
    if (args[0].equals("-r")) 
    {
        recursive = true;
        startIndex = 1;

        // After "-r", there must be exactly 2 arguments (source and destination)
        if (args.length - startIndex != 2) 
        {
            System.out.println("Error: cp -r requires exactly two arguments");
            return;
        }
    } 
    else if (args.length != 2) 
    {
        System.out.println("Error: cp requires exactly two arguments");
        return;
    }

    // Resolve source and destination paths relative to currentPath
    Path source = currentPath.resolve(args[startIndex]).normalize().toAbsolutePath();
    Path destination = currentPath.resolve(args[startIndex + 1]).normalize().toAbsolutePath();

    // Check if source exists
    if (!Files.exists(source)) 
    {
        System.out.println("Error: Source not found");
        return;
    }

    // Perform copy operation
    try {
        if (recursive) {
            copyDirectoryRecursively(source, destination);
        } else {
            copyFile(source, destination);
        }
    } catch (IOException error) {
        System.out.println("Error: " + error.getMessage());
    }
}


private void copyDirectoryRecursively(Path source, Path destination) throws IOException 
{
    // If destination directory does not exist, create it
    if (!Files.exists(destination)) 
    {
        Files.createDirectories(destination);
    }

    // Walk through all files and subdirectories inside the source folder
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() 
    {

        // Called before visiting each subdirectory
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException 
        {
            // Create the corresponding directory in the destination
            Path targetDir = destination.resolve(source.relativize(dir));
            Files.createDirectories(targetDir);
            return FileVisitResult.CONTINUE;
        }

        // Called for each file found in the source folder
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException 
        {
            // Copy each file to the destination, keeping the relative structure
            Path targetFile = destination.resolve(source.relativize(file));
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }
    });
}


private void copyFile(Path source, Path destination) throws IOException
{
    // If the parent directory of destination does not exist, create it
    Path parent = destination.getParent();
    if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
    }

    // Check if the source is a file (not a directory)
    if (Files.isDirectory(source)) {
        System.out.println("Error: For cp, both must be files");
        return;
    }

    // Copy the file, replacing if it already exists
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
}


    public void rmdir(String[] args) {
        /*
         removes all empty directories
         path => remove the given directory is it's empty
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
            }catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            return;
        }
        else{

            // Get the path of the directory we want to delete
            Path dir = currentPath.resolve(args[0]).normalize().toAbsolutePath();

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
                }catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
                return;
            } 
            else {
                System.out.print("Error: Directory does not exist");
                return;
            }
            
        }

    } 

    public void touch(String[] args) {
        if (args.length == 1) {
            Path newFile = Paths.get(args[0]);
            if (!newFile.isAbsolute()) {
                // Get the given path
                newFile = currentPath.resolve(newFile).normalize();
            }
            try {
                // Create the file
                Files.createFile(newFile);
            } catch (IOException e) {
                System.out.println("Error creating file: " + e.getMessage());
            }
        } else {
            System.out.println("Error: invalid number of arguments");
            return;
        }
    }

    public void cat(String[] args) {
        if (args.length == 1 || args.length == 2) {
            for (String arg : args) {
                Path filePath = Paths.get(arg);
                if (!filePath.isAbsolute()) {
                    filePath = currentPath.resolve(filePath).normalize();
                }
                try {
                    List<String> lines = Files.readAllLines(filePath);
                    for (String line : lines) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                }
                if (!Files.exists(filePath)) {
                System.out.println("Error: file does not exist: " + filePath);
                continue;
                }
            }

        } else {
            System.out.println("Error: invalid number of arguments");
            return;
        }
    }

    public void zip(String[] args) {
        int firstIndex = 0;
        boolean includeSubdirs = false;
        // Check for -r option FIRST
        if (args.length > 0 && "-r".equals(args[0])) {
            includeSubdirs = true;
            firstIndex = 1;
        }
        // check for minimum required arguments
        int remainingArgs = args.length - firstIndex;
        if (remainingArgs < 2) {
            System.out.println("Error: Requires archive name and source files");
            return;
        }
        // Get archive name and source paths
        Path archPath = currentPath.resolve(args[firstIndex]).normalize();
        Path[] srcPaths = new Path[remainingArgs - 1];
        int i = 0;
        while (i < srcPaths.length) {
            srcPaths[i] = currentPath.resolve(args[firstIndex + 1 + i]).normalize();
            i++;
        }
        // Create zip archive and add all source paths
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archPath.toFile()))) {
            for (Path srcPath : srcPaths) {
                if (Files.notExists(srcPath)) {
                    System.out.println("Error: path does not exist: " + srcPath);
                    continue;
                }
                if (Files.isDirectory(srcPath)) {
                    if (includeSubdirs) {
                        Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                ZipEntry zipEntry = new ZipEntry(srcPath.relativize(file).toString());
                                zos.putNextEntry(zipEntry);
                                Files.copy(file, zos);
                                zos.closeEntry();
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(srcPath)) {
                            for (Path entry : stream) {
                                if (Files.isRegularFile(entry)) {
                                    ZipEntry zipEntry = new ZipEntry(srcPath.relativize(entry).toString());
                                    zos.putNextEntry(zipEntry);
                                    Files.copy(entry, zos);
                                    zos.closeEntry();
                                }
                            }
                        }
                    }
                } else {
                    ZipEntry zipEntry = new ZipEntry(srcPath.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(srcPath, zos);
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            System.out.println("Error creating zip archive: " + e.getMessage());
        }
    }

    public void unzip(String[] args)
    {
      if(args.length!=1 && !(args[1].equals("-d") && args.length==3))
      {
        System.out.println("Error: Invalid number of arguments");
        return;
      }
      Path zipFilePath = currentPath.resolve(args[0]).normalize();
     //Destination directory: if "-d" provided use its value, otherwise use currentPath
      Path destinationDir = (args.length == 3) ? resolvePath(args[2]) : currentPath;
      
      //Ensure the zip file exists before attempting to open it
      if(!Files.exists(zipFilePath))
      {
        System.out.println("Error: Zip file not found");
        return;
      }

      //Create destination directory using your existing mkdir() if the desired directory does not exist
    try 
    {
     if (!Files.exists(destinationDir))
     {
        Files.createDirectories(destinationDir); 
     } 
    } 
    catch (IOException e) 
    { 
        System.out.println("Error: Cannot create destination directory"); return;
   }
    //Open the zip file as a stream and iterate over entries
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) 
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) 
            {
            Path newFilePath = destinationDir.resolve(entry.getName()).normalize();
            if (entry.isDirectory()) 
            {
                Files.createDirectories(newFilePath);
            } 
            else 
            {
                //For files ensure parent directory exists
                Files.createDirectories(newFilePath.getParent());
                //Copy entry data from the zip input stream to the output file
                Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            zis.closeEntry();
            }
        } 
        catch (IOException error) 
        {
            System.out.println("Error extracting zip file: " + error.getMessage());
        }
    }

    public void rm(String[] args){

        // Path of the file 
        Path file = currentPath.resolve(args[0]);

        // Check if the file exists and is a file
        try {

            if (Files.isRegularFile(file)) {
                Files.delete(file);
            }
            else{
                System.out.print(file.getFileName() + " does not exist or is not a file");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
   
    //  mkdir command implementation
   public void mkdir(String[] args) {
        // Check if user passed at least one argument 
        if (args.length == 0) {
            System.out.println("mkdir: missing operand");
            return;
        }

        // Loop over each argument (each dir name/path separately)
        for (String dirArg : args) {
            
            // Build the path for this directory
            Path dirPath = currentPath.resolve(dirArg).normalize().toAbsolutePath();
            
            // Check if directory exists
            if (Files.exists(dirPath)) {
                System.out.println("mkdir: cannot create directory '" + dirArg + "': File exists");
                continue;
            }

            // Try to create directory (including parent folders)
            try {
                // createDirectories -> supports nested like xxx/xxxx
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                System.out.println("mkdir: failed to create '" + dirArg + "': " + e.getMessage());
            }
        }
    }

    public void wc(String[] args){
        if (args.length != 1) {
            System.out.println("Error: wc takes 1 argument");
        }
        Path file = currentPath.resolve(args[0]);
        try {
            // Path fileName = file.getFileName(); // Get file Name 
            String fileContent = new String(Files.readAllBytes(file)); // read file content
            
            long numLines = fileContent.lines().count();
            
            // Split between words by spaces (one space or more)
            long numWords = Arrays.stream(fileContent.split("\\s+")).filter(w -> !w.isEmpty()).count();
            
            // Count number of chars
            long charCount = fileContent.length();

            System.out.println(numLines + " " + numWords + " " + charCount + " " + file.getFileName());
            
        } catch (Exception e) {
            System.out.println("Error: Can not resolve file " + e.getMessage());
        }
        
    }

    private Path resolvePath(String pathStr) {
    Path path = Paths.get(pathStr);
    if (!path.isAbsolute()) {
        path = currentPath.resolve(path).normalize().toAbsolutePath();
    } else {
        path = path.normalize().toAbsolutePath();
    }
    return path;
}

    private String runCommand(String cmd,String[] args){
        
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

            case("cp"):
                cp(args);
                return "";
            
            case "touch":
                touch(args);
                return "";

            case "cat":
                cat(args);
                return "";
            
            case "wc":
                wc(args);
                return "";

            case "zip":
                zip(args);
                return "";  

            case "unzip":
                unzip(args);
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
        
        //  Read user input
        String command = scanner.nextLine();

        //  Parse the input command
        terminal.parser.parse(command);
        String cmd = terminal.parser.getCommandName();
        String[] arg = terminal.parser.getArgs();

        //  Exit condition
        if (cmd.equals("exit")) {
            break;
        }

        //  Execute the command and capture output
        String output = terminal.runCommand(cmd, arg);

        //  Check if output redirection is used ( ">" or ">>" )
          if (terminal.parser.redirectFile != null) {
             Path filePath = terminal.currentPath.resolve(terminal.parser.redirectFile);

            try {
               if (terminal.parser.appendMode) {
                // >> Append mode
                   Files.writeString(filePath, output + System.lineSeparator(), 
                   StandardOpenOption.CREATE, StandardOpenOption.APPEND);
               } else {
                // > Overwrite mode
                   Files.writeString(filePath, output + System.lineSeparator(),
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (IOException e) {
                   System.out.println("Error writing to file: " + e.getMessage());
            }
        } else {
             System.out.println(output);
           }

    }

    scanner.close();
  }

}