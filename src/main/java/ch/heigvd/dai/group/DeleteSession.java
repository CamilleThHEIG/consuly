package ch.heigvd.dai.group;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "delete", description = "Deletes the session with the specified port.")
public class DeleteSession implements Runnable {

    @Option(
            names = {"-p", "--port"},
            description = "The port number for the session (default: ${DEFAULT-VALUE}).",
            required = true,
            defaultValue = "4446")
    private static int port;

    @Override
    public void run() {
        try {
            System.out.println("Deleting session on port: " + port);
            // Add your session deletion logic here
        } catch (Exception e) {
            System.out.println("Error deleting session: " + e.getMessage());
        }
    }
}
