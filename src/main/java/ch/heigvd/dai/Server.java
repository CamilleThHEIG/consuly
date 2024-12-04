package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import picocli.CommandLine;
import ch.heigvd.dai.util.JSON;
import ch.heigvd.dai.util.Group;

import ch.heigvd.dai.consulyProtocolEnums.*;

@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {
    private final int NUMBER_OF_THREADS;
    private static final LinkedList<Integer> ACTIVE_PORTS; // Optional, for hosting multiple servers if needed
    private static final String MsgPrf = "[Server] : ";
    private static int nextClientId = 1;
    private static final String END_OF_LINE = "\n";
    private static LinkedList<Group> groups;

    static {
        ACTIVE_PORTS = new LinkedList();
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
        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
             System.out.println("Server is listening on port " + port);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // Accept a client connection
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                executor.submit(new ClientHandler(socket, nextClientId++));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
        return 0;
    }

    static class ClientHandler implements Runnable {
        private int clientId;
        private final Socket socket;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        private boolean removeGroup(String name) {
            Iterator<Group> it = groups.iterator();
            while (it.hasNext()) {
                Group group = it.next();
                if (group.name().equalsIgnoreCase(name)) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }

        /**
         * Notifie un client spécifique qu'il doit quitter un groupe en envoyant un message personnalisé.
         *
         * @param clientId L'identifiant du client à notifier.
         */
//        private void notifyClientToQuit(int clientId, BufferedWriter out) {
//            try {
//                out.write(ServAns.WAITING_USER_TO_QUIT + END_OF_LINE);
//                out.flush();
//            } catch (IOException e) {
//                System.out.println("Error while notifying client to quit: " + e.getMessage());
//            }
//        }

        /**
         * Fonction qui gère le processus de suppression d'un groupe
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private boolean handleGroupDeletion(BufferedReader in, BufferedWriter out, String groupName, int clientId) throws IOException {
            String clientMessage;
            Group groupToDelete = getGroupByName(groupName);

            if (groupToDelete == null) {
                out.write(ServAns.INVALID_GROUP + END_OF_LINE);
                out.flush();
                return false;
            }

            // Check if the client is the owner of the group
            if (!groupToDelete.isOwner(clientId)) {
                out.write(ServAns.INVALID_ID + END_OF_LINE);
                out.flush();
                return false;
            }

            // Notify all members of the group to quit
//            for (Integer memberId : groupToDelete.membersIdList()) {
//                notifyClientToQuit(memberId, out);
//            }
            out.write(ServAns.WAITING_USER_TO_QUIT + END_OF_LINE);
            out.flush();
            groupToDelete.membersIdList().clear(); // Par simplicité on vide la liste des membres du groupe

            // effectively create the group server side
            if(!removeGroup(groupName)) {
                out.write(ServAns.FAILURE_DELETION + END_OF_LINE);
                out.flush();
                return false;
            }

            out.write(ServAns.SUCCESS_DELETION + END_OF_LINE);
            out.flush();
            return true;
        }

        private Group getGroupByName(String groupName) {
            for (Group group : groups) {
                if (group.name().equalsIgnoreCase(groupName)) {
                    return group;
                }
            }
            return null;
        }

        /**
         * Fonction qui gère le processus de création d'un groupe
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, String groupname) throws IOException {
            String clientMessage, password = null;

            // Ask the client for the password of the group
            out.write(ServAns.PASSWORD_FOR + END_OF_LINE);
            out.flush();

            // Receive the password
            clientMessage = in.readLine();
            password = clientMessage.split(" ")[1];

            if (password == null || password.isEmpty()) {
                out.write(ServAns.INVALID_PASSWORD + END_OF_LINE);
                out.flush();
                return false;
            }

            // effectively create the group server side
            Group newGroup = new Group(groupname, clientId, password);
            groups.add(newGroup);
            out.write(ServAns.VALID_PASSWORD + " " + newGroup.name() + END_OF_LINE);
            out.flush();
            return true;
        }

        private void handleGroupListing(BufferedWriter out) throws IOException {
            if(groups.isEmpty()) {
                out.write(ServAns.NO_GROUP + END_OF_LINE);
                out.flush();
                return;
            }

            for (Group group : groups) {
                out.write(group.name() + END_OF_LINE);
                out.flush();
            }
            out.write(ServAns.END_OF_LIST + END_OF_LINE);
            out.flush();
        }

        private ClientMessages decodeClientMessage(String message) {
            try{
                return ClientMessages.valueOf(message);
            } catch (IllegalArgumentException e) {
                return ClientMessages.INVALID;
            }
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)))
            {
                // Handle first contact
                String userFirstMessage = in.readLine();
                switch (decodeClientMessage(userFirstMessage)) {
                    case CONNECT_SRV:
                        out.write(ServAns.ACCEPT_CONNECT + " " + clientId + END_OF_LINE); // Send the client ID
                        out.flush();
                        break;
                    default:
                        out.write(ServAns.REFUSED_CONNECT + END_OF_LINE);
                        out.flush();
                        socket.close();
                        break;
                }

                String userMessage, groupname;
                System.out.println(MsgPrf + "Waiting for client command ...");
                while (!socket.isClosed()) {
                    userMessage = in.readLine();
                    switch (decodeClientMessage(userMessage.split(" ")[0])) {
                        case CREATE_GROUP :
                            groupname = userMessage.split(" ")[1];
                            System.out.println(MsgPrf + "Creating group with name " + groupname);
                            boolean groupCreated = handleGroupCreation(in, out, groupname); break;
                        case DELETE_GROUP:
                            groupname = userMessage.split(" ")[1];
                            System.out.println(MsgPrf + "Deleting group with name " + groupname);
                            int clientId = Integer.parseInt(userMessage.split(" ")[2]);
                            boolean groupDeleted = handleGroupDeletion(in, out, groupname, clientId); break;
                        case LIST_GROUPS:
                            handleGroupListing(out);
                            break;
                        case INVALID:
                            System.out.println(MsgPrf + "Invalid command received: " + userMessage);
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Server exception: " + e.getMessage());
            }
        }

        /**
         * Fonction qui gère la réception d'une liste de préférence
         * @param in
         * @param out
         * @throws IOException
         */
        public void receiveList(BufferedReader in, BufferedWriter out) throws IOException {
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
            JSON json = new JSON(nextClientId);
            json.writeFileWithLists(like, dislike, noopinion);
        }
    }
}
