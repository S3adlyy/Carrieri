package utils.grecru;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SimplePythonKernel {

    private Process pythonProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private BufferedReader errorReader;
    private boolean isRunning = false;

    private Consumer<String> onOutput;
    private Consumer<String> onError;
    private Consumer<String> onResult;

    private Thread outputThread;
    private Thread errorThread;
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    // Flag to track if we're currently collecting variables
    private boolean collectingVariables = false;
    private StringBuilder variableBuffer = new StringBuilder();

    public SimplePythonKernel() {
        this.onOutput = s -> {};
        this.onError = s -> {};
        this.onResult = s -> {};
    }

    public void setOnOutput(Consumer<String> listener) { this.onOutput = listener; }
    public void setOnError(Consumer<String> listener) { this.onError = listener; }
    public void setOnResult(Consumer<String> listener) { this.onResult = listener; }

    public void start() throws IOException {
        try {
            // Check Python version first
            Process checkProcess = Runtime.getRuntime().exec("python --version");
            int exitCode = checkProcess.waitFor();
            if (exitCode != 0) {
                throw new IOException("Python is not available. Please install Python and add it to PATH.");
            }

            BufferedReader versionReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String version = versionReader.readLine();
            System.out.println("✅ Python detected: " + version);

        } catch (InterruptedException e) {
            throw new IOException("Failed to check Python version", e);
        }

        // Improved Python script with better command handling
        String pythonScript =
                "import sys\n" +
                        "import code\n" +
                        "import io\n" +
                        "\n" +
                        "# Create interactive console\n" +
                        "console = code.InteractiveConsole()\n" +
                        "\n" +
                        "# Send ready signal\n" +
                        "print('KERNEL_READY')\n" +
                        "sys.stdout.flush()\n" +
                        "\n" +
                        "# Main loop\n" +
                        "while True:\n" +
                        "    try:\n" +
                        "        # Read a line (special command) or code block\n" +
                        "        first_line = sys.stdin.readline().rstrip('\\n')\n" +
                        "        if not first_line:\n" +
                        "            continue\n" +
                        "        \n" +
                        "        # Check for special commands\n" +
                        "        if first_line == 'GET_VARS':\n" +
                        "            # Get variables without printing debug info\n" +
                        "            vars_list = []\n" +
                        "            global_vars = dict(globals())\n" +
                        "            for name, value in global_vars.items():\n" +
                        "                if not name.startswith('_') and name not in ['console', 'sys', 'io', 'code', 'vars_list', 'name', 'value', 'first_line']:\n" +
                        "                    try:\n" +
                        "                        if not callable(value) and not name.startswith('__'):\n" +
                        "                            vars_list.append(f'{name} ({type(value).__name__})')\n" +
                        "                    except:\n" +
                        "                        pass\n" +
                        "            print('===VARS_START===')\n" +
                        "            for v in vars_list:\n" +
                        "                print(v)\n" +
                        "            print('===VARS_END===')\n" +
                        "            sys.stdout.flush()\n" +
                        "            continue\n" +
                        "        \n" +
                        "        # If not a special command, read the rest of the code block\n" +
                        "        code_lines = [first_line]\n" +
                        "        while True:\n" +
                        "            line = sys.stdin.readline().rstrip('\\n')\n" +
                        "            if line == '':\n" +
                        "                break\n" +
                        "            code_lines.append(line)\n" +
                        "        \n" +
                        "        code_str = '\\n'.join(code_lines)\n" +
                        "        \n" +
                        "        # Execute regular code - capture output\n" +
                        "        old_stdout = sys.stdout\n" +
                        "        captured_stdout = io.StringIO()\n" +
                        "        sys.stdout = captured_stdout\n" +
                        "        \n" +
                        "        try:\n" +
                        "            # Use push for multi-line code\n" +
                        "            console.push(code_str)\n" +
                        "        except Exception as e:\n" +
                        "            print(f'Error: {e}')\n" +
                        "        finally:\n" +
                        "            # Restore stdout and print captured output\n" +
                        "            sys.stdout = old_stdout\n" +
                        "            output = captured_stdout.getvalue()\n" +
                        "            if output:\n" +
                        "                print(output, end='')\n" +
                        "        \n" +
                        "        sys.stdout.flush()\n" +
                        "        \n" +
                        "    except Exception as e:\n" +
                        "        print(f'Kernel error: {e}', file=sys.stderr)\n" +
                        "        sys.stderr.flush()\n";

        // Start Python process
        ProcessBuilder pb = new ProcessBuilder("python", "-u", "-c", pythonScript);
        pb.redirectErrorStream(false);
        pythonProcess = pb.start();

        reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
        errorReader = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));

        isRunning = true;

        // Start output reader
        startOutputReader();

        // Start error reader
        startErrorReader();

        // Wait for ready signal with timeout
        try {
            boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
            if (!ready) {
                throw new IOException("Timeout waiting for kernel to be ready");
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for kernel", e);
        }
    }

    private void startOutputReader() {
        outputThread = new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    final String output = line;

                    if (output.equals("KERNEL_READY")) {
                        readyLatch.countDown();
                        continue;
                    }

                    if (output.equals("===VARS_START===")) {
                        collectingVariables = true;
                        variableBuffer = new StringBuilder();
                        continue;
                    } else if (output.equals("===VARS_END===")) {
                        collectingVariables = false;
                        // Send the complete variable list as one result
                        if (onResult != null) {
                            onResult.accept(variableBuffer.toString());
                        }
                        continue;
                    }

                    if (collectingVariables) {
                        // Collect variable lines without sending them individually
                        variableBuffer.append(output).append("\n");
                    } else {
                        // Regular output - send to listeners
                        if (output.startsWith("Error:")) {
                            if (onError != null) onError.accept(output.substring(6));
                        } else {
                            // Filter out any remaining debug lines
                            if (!output.matches("^[a-zA-Z_]+ \\([a-zA-Z_]+\\)$") &&
                                    !output.matches("^[a-zA-Z_]+$") &&
                                    !output.isEmpty() &&
                                    !output.contains("===VARS")) {
                                if (onResult != null) onResult.accept(output);
                                if (onOutput != null) onOutput.accept(output);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Output reader error: " + e.getMessage());
                }
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
    }

    private void startErrorReader() {
        errorThread = new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = errorReader.readLine()) != null) {
                    final String error = line;
                    if (onError != null) onError.accept(error);
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error reader error: " + e.getMessage());
                }
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();
    }

    public void executeCode(String code) throws IOException {
        if (!isRunning || pythonProcess == null) {
            throw new IOException("Kernel not running");
        }

        // Split code into lines and send each line
        String[] lines = code.split("\n");
        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }
        writer.newLine(); // Empty line to signal end of code block
        writer.flush();
    }

    public void getVariables() throws IOException {
        if (!isRunning || pythonProcess == null) {
            throw new IOException("Kernel not running");
        }

        // Send GET_VARS as a special command (not as Python code)
        writer.write("GET_VARS");
        writer.newLine();
        writer.newLine(); // Empty line to signal end of command
        writer.flush();
    }

    public void inspectVariable(String varName) throws IOException {
        String inspectCode =
                "import pprint\n" +
                        "print('\\n' + '─'*40)\n" +
                        "print(f'🔍 Inspecting: " + varName + "')\n" +
                        "print('─'*40)\n" +
                        "print(f'Type: {type(" + varName + ").__name__}')\n" +
                        "print('Value:')\n" +
                        "pprint.pprint(" + varName + ")\n";

        executeCode(inspectCode);
    }

    public boolean isRunning() {
        return isRunning && pythonProcess != null && pythonProcess.isAlive();
    }

    public void shutdown() {
        isRunning = false;

        if (outputThread != null) outputThread.interrupt();
        if (errorThread != null) errorThread.interrupt();

        try { if (writer != null) writer.close(); } catch (IOException e) {}
        try { if (reader != null) reader.close(); } catch (IOException e) {}
        try { if (errorReader != null) errorReader.close(); } catch (IOException e) {}

        if (pythonProcess != null) {
            pythonProcess.destroyForcibly();
        }
    }
}