import org.junit.jupiter.api.*;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineInterpreterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testPwdCommand() {
        String result = CommandLineInterpreter.pwdCommand();
        assertEquals(System.getProperty("user.dir") + "\n", result, "The pwd command should return the current directory.");
    }

    @Test
    public void testCdCommand() {
        String currentDir = System.getProperty("user.dir");
        String newDir = currentDir; // Stay in the same directory
        String result = CommandLineInterpreter.cdCommand(newDir);
        assertEquals("", result, "The cd command should change to the specified directory without errors.");
    }

    @Test
    public void testCdInvalidCommand() {
        String result = CommandLineInterpreter.cdCommand("non_existing_directory");
        assertEquals("cd: No such file or directory\n", result, "The cd command should return an error for non-existent directories.");
    }

    @Test
    public void testLsCommand() {
        String result = CommandLineInterpreter.lsCommand(new String[] {});
        assertFalse(result.isEmpty(), "The ls command should list files and directories.");
    }

    @Test
    public void testTouchCommand() throws IOException {
        String fileName = "testfile.txt";
        File file = new File(System.getProperty("user.dir"), fileName);

        try {
            String result = CommandLineInterpreter.touchCommand(fileName);
            assertEquals("File created: " + fileName + "\n", result, "The touch command should create a file.");
            assertTrue(file.exists(), "File should exist after the touch command.");
        } finally {
            file.delete();  // Clean up after test
        }
    }

    @Test
    public void testMkdirCommand() {
        String dirName = "testdir";
        File dir = new File(System.getProperty("user.dir"), dirName);

        try {
            String result = CommandLineInterpreter.mkdirCommand(dirName);
            assertEquals("Directory created: " + dirName + "\n", result, "The mkdir command should create a directory.");
            assertTrue(dir.exists(), "Directory should exist after the mkdir command.");
        } finally {
            dir.delete();  // Clean up after test
        }
    }

    @Test
    public void testRmdirCommand() {
        String dirName = "testdir";
        File dir = new File(System.getProperty("user.dir"), dirName);
        dir.mkdir();

        String result = CommandLineInterpreter.rmdirCommand(dirName);
        assertEquals("Directory removed: " + dirName + "\n", result, "The rmdir command should remove the directory.");
        assertFalse(dir.exists(), "Directory should not exist after the rmdir command.");
    }

    @Test
    public void testRmCommand() throws IOException {
        String fileName = "testfile.txt";
        File file = new File(System.getProperty("user.dir"), fileName);
        file.createNewFile();

        String result = CommandLineInterpreter.rmCommand(fileName);
        assertEquals("File removed: " + fileName + "\n", result, "The rm command should remove the file.");
        assertFalse(file.exists(), "File should not exist after the rm command.");
    }

    @Test
    public void testRmRecursiveCommand() throws IOException {
        // Create a test directory with a file inside it
        String dirName = "testdir";
        String fileName = "testfile.txt";
        File dir = new File(System.getProperty("user.dir"), dirName);
        dir.mkdir();
        File file = new File(dir, fileName);
        file.createNewFile();

        // Run the rm -r command
        String result = CommandLineInterpreter.rmCommand("-r", dirName);
        assertEquals("Directory and its contents removed: " + dirName + "\n", result, "The rm -r command should recursively remove the directory and its contents.");

        assertFalse(dir.exists(), "Directory should not exist after the rm -r command.");
        assertFalse(file.exists(), "File inside the directory should also be removed after the rm -r command.");
    }

    @Test
    public void testRmWithoutRecursiveForDirectory() {
        // Create a test directory
        String dirName = "testdir";
        File dir = new File(System.getProperty("user.dir"), dirName);
        dir.mkdir();

        // Run the rm command without the -r flag
        String result = CommandLineInterpreter.rmCommand(dirName);
        assertEquals("rm: Cannot remove '" + dirName + "': Is a directory\n", result, "The rm command without -r should not remove a directory.");

        assertTrue(dir.exists(), "Directory should still exist after the rm command without -r.");
        dir.delete(); // Clean up after test
    }

    @Test
    public void testRmNonExistentFile() {
        String result = CommandLineInterpreter.rmCommand("non_existing_file.txt");
        assertEquals("rm: File or directory not found: non_existing_file.txt\n", result, "The rm command should return an error for a non-existent file.");
    }

    @Test
    public void testCatCommand() throws IOException {
        String fileName = "testfile.txt";
        File file = new File(System.getProperty("user.dir"), fileName);
        String fileContent = "Hello World";

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fileContent);
        }

        String result = CommandLineInterpreter.catCommand(fileName);
        assertEquals(fileContent + "\n", result, "The cat command should display the file content.");

        file.delete();  // Clean up after test
    }

    @Test
    public void testHelpCommand() {
        String result = CommandLineInterpreter.helpCommand();
        assertTrue(result.contains("pwd"), "Help command should list pwd.");
        assertTrue(result.contains("ls"), "Help command should list ls.");
        assertTrue(result.contains("mkdir"), "Help command should list mkdir.");
    }

    @Test
    public void testInvalidCommand() {
        String result = CommandLineInterpreter.processCommand("invalidCommand", null);
        assertTrue(result.contains("Invalid command"), "Invalid command should return an error message.");
    }

    @Test
    public void testOutputRedirection() throws IOException {
        String fileName = "output.txt";
        String content = "Hello, CLI!";
        CommandLineInterpreter.redirectToFile(content, fileName, false);

        File file = new File(System.getProperty("user.dir"), fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String fileContent = reader.readLine();
            assertEquals(content, fileContent, "Output should be written to the file.");
        } finally {
            file.delete();  // Clean up after test
        }
    }

    @Test
    public void testAppendOutputRedirection() throws IOException {
        String fileName = "output.txt";
        String content = "Hello, CLI!";
        String newContent = "This is new content.";

        // Write initial content to the file
        CommandLineInterpreter.redirectToFile(content, fileName, false);

        // Append new content
        CommandLineInterpreter.redirectToFile(newContent, fileName, true);

        File file = new File(System.getProperty("user.dir"), fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String fileContent = reader.readLine();
            String appendedContent = reader.readLine();

            assertEquals(content, fileContent, "First line should be the initial content.");
            assertEquals(newContent, appendedContent, "Second line should be the appended content.");
        } finally {
            file.delete();  // Clean up after test
        }
    }

    @Test
    public void testPipeCommand() {
        String inputData = "Hello World";
        String command = "cat";
        String result = CommandLineInterpreter.pipeCommand(inputData, command);
        assertEquals(inputData + "\n", result, "Pipe command should pass input to the next command.");
    }
}
