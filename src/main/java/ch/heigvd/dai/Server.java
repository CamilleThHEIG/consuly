package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {

    private static final int NUMBER_OF_THREADS = 5;
    private static final LinkedList<Integer> ACTIVE_PORTS = new LinkedList();

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    @CommandLine.Option(
            names = {"-c", "--create"},
            description = "Create a new session.",
            defaultValue = "true",
            required = true)
    protected boolean create;

    @Override
    public Integer call() {
        try (
                ServerSocket serverSocket = new ServerSocket(port); ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
            System.out.println("Server is listening on port " + port);
            while (!serverSocket.isClosed()) {
                // Create a new session
                if (this.create && !ACTIVE_PORTS.contains(port)) {
                    ACTIVE_PORTS.add(port);
                }

                Socket socket = serverSocket.accept(); // Accept a client connection
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
        private String userIn;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                while ((userIn = in.readLine()) != null) {
                    System.out.println("[Client] " + userIn);

                    // Check if a session is active on the port
                    if (userIn.equals("CHECK_SESSION")) {
                        out.write("What is the port your try to join:\n");
                        out.flush();

                        userIn = in.readLine();
                        if (isSessionActive(Integer.parseInt(userIn))) {
                            out.write("SESSION_ACTIVE\n");
                            out.flush();
                        } else {
                            out.write("CONNECTION_REFUSED\n");
                            out.flush();
                            continue;
                        }
                    }

                    if (userIn.equals("CONNECTED")) {
                        out.write("Choose an option ?\n1. Create a group\n2. Delete a group\n3. Join a group\n");
                        out.flush();
                    }

                    // Action of the client
                    switch (userIn) {
                        case "1" -> System.out.println("In case 1"); //createGroup();
                        case "2" -> System.out.println("In case 2"); //deleteGroup();
                        case "3" -> System.out.println("In case 3"); //joinGroup();
                    }
                }
            } catch (IOException e) {
                System.out.println("Client handling exception: " + e.getMessage());
            }
        }

        private boolean isSessionActive(int port) {
            for (int activePort : ACTIVE_PORTS) {
                if (activePort == port) {
                    return true;
                }
            }
            return false;
        }

        public void receiveList(BufferedReader in, BufferedWriter out) throws IOException {
            System.out.println("In receiveList");
            out.write("READY_RECEIVE\n");
            out.flush();

            // These lists will then be written in local server files
            JSONArray like = new JSONArray();
            JSONArray dislike = new JSONArray();
            JSONArray noopinion = new JSONArray();
            JSONArray listToAdd = null;

            boolean keepLoop = true;
            while (keepLoop) {
                String userIn = in.readLine();
                if (listToAdd != null && userIn.contains("STYLE")) {
                    String res = userIn.substring(5).replace("<", "").replace(">", "");
                    listToAdd.put(res);
                    out.write("ACK\n");
                    out.flush();
                }
                switch (userIn) {
                    case "SENDING_LIKES":
                        out.write("ACK\n");
                        out.flush();
                        listToAdd = like;
                        break;
                    case "SENDING_DISLIKES":
                        out.write("ACK\n");
                        out.flush();
                        listToAdd = dislike;
                        break;
                    case "SENDING_NEUTRAL":
                        out.write("ACK\n");
                        out.flush();
                        listToAdd = noopinion;
                        break;
                    case "FINISHED":
                        out.write("ACK\n");
                        out.flush();
                        keepLoop = false;
                        break;
                }
            }
            System.out.println("Like");
            for (int j = 0; j < like.length(); ++j) {
                System.out.println(like.getString(j));
            }

            System.out.println("\nDisike");
            for (int j = 0; j < dislike.length(); ++j) {
                System.out.println(dislike.getString(j));
            }

            System.out.println("\nNeutral");
            for (int j = 0; j < noopinion.length(); ++j) {
                System.out.println(noopinion.getString(j));
            }
            System.out.println("\nEnd of receiveList");
        }
    }
}
