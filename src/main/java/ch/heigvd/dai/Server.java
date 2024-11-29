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

import java.util.concurrent.Callable;

import org.json.JSONArray;

import picocli.CommandLine;
import ch.heigvd.dai.util.JSON;
import ch.heigvd.dai.util.Group;


@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {
    private final int NUMBER_OF_THREADS;
    private static final LinkedList<Integer> ACTIVE_PORTS;
    private static LinkedList<Group> groups;
    private static final String EOT;
    private static int lastClientIdUsed = 2;

    static {
        ACTIVE_PORTS = new LinkedList();
        EOT = "\u0004\n";
    }

    {
        NUMBER_OF_THREADS = 5;
        groups = new LinkedList<>();
    }


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
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                while ((userIn = in.readLine()) != null) {
                    System.out.println("[Client] " + userIn);

                    // Check if a session is active on the port
                    if (userIn.equals("CHECK_SESSION")) {
                        out.write("What is the port your try to join: \n");
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
                        out.write("Choose an option ?\n");
                        out.write("[1] CREATE a new group\n");
                        out.write("[2] DELETE a group\n");
                        out.write("[3] JOIN a group\n");
                        out.write("[4] SHOW all groups\n");
                        out.write("[5] QUIT\n");
                        out.write(EOT);
                        out.flush();
                    } else { // Action of the client
                        switch (userIn) {
                            case "1" -> {
                                createGroup(out, in);
                            }
                            case "2" -> {
                                deleteGroup(out, in);
                            }
                            case "3" -> {
                                joinGroup(out, in);
                            }
                            case "4" -> {
                                showGroups(out);
                            }
                            case "5" -> {
                                out.write("Bye.");
                                out.write(EOT);
                                out.flush();
                            }
                            default -> {
                                out.write("Invalid option.\n");
                                out.write(EOT);
                                out.flush();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Client handling exception: " + e.getMessage());
            }
        }


        private static void createGroup(BufferedWriter out, BufferedReader in) throws IOException {
            out.write("Enter the name of the group: "); out.write(EOT); out.flush();
            String name = in.readLine();

            groups.add(new Group(name, 1, new int[]{1}));
            out.write("Group created.\n"); out.write(EOT); out.flush();
        }

        private void showGroups(BufferedWriter out) throws IOException {
            if(groups.isEmpty()) {
                out.write("No group created yet.");
                out.write(EOT);
                out.flush();
                return;
            }

            for (Group g : groups) {
                out.write(g.toString());
            }
            out.write(EOT);
            out.flush();
        }

        private void joinGroup(BufferedWriter out, BufferedReader in) throws IOException {
            out.write("Enter the name of the group: ");
            out.flush();
            String name = in.readLine();
            Group group = groups.stream().filter(g -> g.name().equals(name)).findFirst().orElse(null);
            if (group == null) {
                out.write("Group not found.\n");
                out.write(EOT);
                out.flush();
                return;
            }
            group.joinGroup();
            out.write("Joined group.\n");
            out.write(EOT);
            out.flush();
        }

        private void deleteGroup(BufferedWriter out, BufferedReader in) throws IOException {
            out.write("Enter the name of the group: ");
            out.write(EOT);
            out.flush();
            String name = in.readLine();
            groups.removeIf(group -> group.name().equals(name));
            out.write("Group deleted.\n");
            out.write(EOT);
            out.flush();
        }

        private boolean isSessionActive(int port) {
            for (int activePort : ACTIVE_PORTS) {
                if (activePort == port) {
                    return true;
                }
            }
            return false;
        }

        /**
         * What to do to receive a list from a user
         * @param in
         * @param out
         * @throws IOException
         */

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
                if (listToAdd != null && userIn.contains("STYLE")){
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

            // write the result
            JSON json = new JSON(lastClientIdUsed);
            json.writeFileWithLists(like, dislike, noopinion);
        }
    }

}
