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
    private static final LinkedList<Integer> ACTIVE_PORTS; // Optional, for hosting multiple servers
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
        private String userIn;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        private void removeGroup(String name) {
            Iterator<Group> it = groups.iterator();
            while (it.hasNext()) {
                Group group = it.next();
                if (group.name().equalsIgnoreCase(name)) {
                    it.remove();
                    System.out.println("Groupe supprimé : " + name);
                    return;
                }
            }
            System.out.println("Groupe introuvable : " + name);
        }

        /**
         * Notifie un client spécifique qu'il doit quitter un groupe en envoyant un message personnalisé.
         *
         * @param clientId L'identifiant du client à notifier.
         */
        private void notifyClientToQuit(int clientId, BufferedWriter out) {
//            out.write(ServAns.FORCE_QUIT + END_OF_LINE);
//            out.flush();
//            System.out.println(MsgPrf + "Sent FORCE_QUIT to client " + clientId);
        }


        /**
         * Fonction qui gère le processus de suppression d'un groupe
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private boolean handleGroupDeletion(BufferedReader in, BufferedWriter out, String groupName) throws IOException {
            Group groupToDelete = null;

            // Rechercher le groupe à supprimer
            for (Group grp : groups) {
                if (grp.name().equalsIgnoreCase(groupName)) {
                    groupToDelete = grp;
                    break;
                }
            }

            if (groupToDelete != null) {
                // Notifier les membres du groupe
                for (Integer memberId : groupToDelete.membersIdList()) {
                    notifyClientToQuit(memberId, out);
                }

                // Supprimer le groupe
                removeGroup(groupName);

                // Confirmer la suppression au client admin
                out.write(ServAns.SUCCESS_DELETION + END_OF_LINE);
                out.flush();
                System.out.println(MsgPrf + "Group '" + groupName + "' deleted successfully.");
                return true;
            } else {
                // Groupe introuvable
                out.write(ServAns.INVALID_GROUP + END_OF_LINE);
                out.flush();
                System.out.println(MsgPrf + "Group '" + groupName + "' not found.");
                return false;
            }
        }

        /**
         * Fonction qui gère le processus de création d'un groupe
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, String groupname) throws IOException {
            out.write(ServAns.PASSWORD_FOR + END_OF_LINE);
            out.flush();

            String clientMessage = in.readLine();
            String password = clientMessage.split(" ")[1];

            System.out.println(MsgPrf + "Client password: " + password);

            // TODO verify password validity

            out.write(ServAns.VALID_PASSWORD + END_OF_LINE);
            out.flush();

            // effectively create the group server side

            clientMessage = in.readLine();
            if (!clientMessage.equals(ClientMessages.ACK_VALID.toString())) {
                // abort group creation
                System.out.println("Aborting (1)");
                return false;
            }
            out.write(ServAns.GRANT_ADMIN + " " + nextGroupId + END_OF_LINE);
            out.flush();

            // peut-être que le client devrait encore valider qu'il a reçu

            int placeHolderId = 4;
            Group newGroup = new Group(nextGroupId, placeHolderId);
            groups.add(newGroup);

            System.out.println(MsgPrf + "Created group " + nextGroupId);

            nextGroupId++;
            return true;
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
                if (userFirstMessage.equals(ClientMessages.CONNECT_SRV.toString())) {
                    out.write(ServAns.ACCEPT_CONNECT + END_OF_LINE);
                    out.flush();
                } else {
                    out.write(ServAns.REFUSED_CONNECT + END_OF_LINE);
                    out.flush();
                    socket.close();
                }

                String userMessage, groupname;
                ClientMessages clientMessage;
                while (!socket.isClosed()) {
                    System.out.println(MsgPrf + "Waiting for client command ...");
                    userMessage = in.readLine();
                    clientMessage = decodeClientMessage(userMessage);

                    switch (clientMessage) {
                        case CREATE_GROUP :
                            groupname = userMessage.substring(ClientMessages.CREATE_GROUP.toString().length());
                            System.out.println(MsgPrf +  "group creation command received");
                            boolean groupCreated = handleGroupCreation(in, out, groupname); break;
                        case DELETE_GROUP:
                            groupname = userMessage.substring(ClientMessages.DELETE_GROUP.toString().length());
                            boolean groupDeleted = handleGroupDeletion(in, out, groupname); break;
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
