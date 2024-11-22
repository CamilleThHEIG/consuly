package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {
    private static final int NUMBER_OF_THREADS = 5;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    @Override
    public Integer call() {
        System.out.println("Server is listening on port " + port);
        try (
                ServerSocket serverSocket = new ServerSocket(port); ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // Accept the client connection
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                executor.submit(new ClientHandler(socket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
        return 0;
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private static final int LOWERBOUND = 1, UPPERBOUND = 100;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
            ){
                out.write("Welcome-average giga chad- you must have been waiting for so long to have a decent app ! What kind do you like ?");
                out.newLine();
                out.flush(); // Ensure the message is sent to the client

                String userIn;
                while ((userIn = in.readLine()) != null) {
                    try {
                            System.out.println("[Client] " + userIn);
                            out.write("Congratulations! You've guessed the number, Bye.");
                            out.flush();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                out.newLine();
                out.flush(); // Ensure the message is sent to the client

        } catch (IOException e) {
                System.out.println("Client handling exception: " + e.getMessage());
            }
        }
    }
}
