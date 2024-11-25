package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import ch.heigvd.dai.group.CreateSession;
import ch.heigvd.dai.group.DeleteSession;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Launch the server side of the application.", subcommands = {CreateSession.class, DeleteSession.class})
public class Server implements Callable<Integer> {

    private static final int NUMBER_OF_THREADS = 5;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    @Override
    public Integer call() {
        try (
                ServerSocket serverSocket = new ServerSocket(port); ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
            System.out.println("Server is listening on port " + port);
            while (!serverSocket.isClosed()) {
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
                //System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                while ((userIn = in.readLine()) != null) {
                    if (userIn.equals("READY_SEND")) {
                        receiveList(in, out);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client handling exception: " + e.getMessage());
            }
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

    public void executeCreateSession(int port) {
        String[] args = {"create", "--port", String.valueOf(port)};
        new CommandLine(new CreateSession()).execute(args);
    }

    public void executeDeleteSession(int port) {
        String[] args = {"delete", "--port", String.valueOf(port)};
        new CommandLine(new DeleteSession()).execute(args);
    }
}
