package ch.heigvd.dai.group;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Creates a new session with the specified port.")
public class CreateSession implements Runnable {

    @Option(
            names = {"-p", "--port"},
            description = "The port number for the session (default: ${DEFAULT-VALUE}).",
            required = true,
            defaultValue = "4446")
    private static int port;

    @Override
    public void run() {
        try {
            System.out.println("Creating session on port: " + port);
            // Add your session creation logic here
        } catch (Exception e) {
            System.out.println("Error creating session: " + e.getMessage());
        }
    }
}
