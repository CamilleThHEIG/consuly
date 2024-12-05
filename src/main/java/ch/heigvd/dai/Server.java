package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.sql.SQLOutput;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.*;

import org.json.JSONArray;
import picocli.CommandLine;
import ch.heigvd.dai.util.JSON;
import ch.heigvd.dai.util.Group;
import ch.heigvd.dai.consulyProtocolEnums.*;
import java.util.concurrent.Callable;

import org.json.JSONObject;


@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {
    private final int NUMBER_OF_THREADS;
    private static final LinkedList<Integer> ACTIVE_PORTS; // Optional, for hosting multiple servers if needed
    private static final String MsgPrf = "[Server] : ";
    private static int nextClientId = 1;
    private static int nextGroupId = 1;

    private static final String END_OF_LINE = "\n";
    private static LinkedList<Group> groups;
    private static LinkedList<Integer> membersReady;
    private boolean isReady;

    static {
        ACTIVE_PORTS = new LinkedList();
        membersReady = new LinkedList<>();
    }

    {
        NUMBER_OF_THREADS = 5;
        groups = new LinkedList<>();
        isReady = false;
    }

    private static int lastClientIdUsed = 2;

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
        private final int clientId;
        private boolean clientInGroup = false;
        private String clientGroupName = "";    // "" meaning no group name
        private final Socket socket;
        private String serverOut, clientIn;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        public String getClientGroupName() {
            if (clientGroupName.isEmpty()){
                System.out.println("WARNING EMPTY NAME");
            }
            return clientGroupName;
        }

        public String getLocalPrefix(){
            return MsgPrf + " to " + clientId + " : ";
        }

        private boolean removeGroupWithAdminId(int adminId){
            Iterator<Group> it = groups.iterator();
            while (it.hasNext()) {
                Group group = it.next();
                if (group.isOwner(adminId)) {
                    it.remove();
                    return true;
                }
            }
            return false;
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

        private Group getGroupWithAdminId(int adminId) {
            for (Group group : groups) {
                if (group.isOwner(adminId)) {
                    return group;
                }
            }
            System.out.println("Did not find group with adminId " + adminId);
            return null;
        }

        private Group getGroupByName(String groupName) {
            for (Group group : groups) {
                if (group.name().equals(groupName)) {
                    return group;
                }
            }
            return null;
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
        private boolean handleGroupDeletion(BufferedReader in, BufferedWriter out, int clientId) throws IOException {
            // only an admin should be allowed to call this

            Group myClientGroup = getGroupWithAdminId(clientId);

            if (myClientGroup == null) {
                out.write(ServAns.ERROR + END_OF_LINE);
                out.flush();
                return false;
            }

            // sendForceQuit to everyone

            getGroupWithAdminId(clientId).setToBeDeleted(true);

            // waiting for everyone to quit
            while(!getGroupWithAdminId(clientId).everyoneButAdminLeft()) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    System.out.println("CATCHED");
                }
            }
            // now we are alon in the group

            //groupToDelete.getMembersIdList().clear(); // Par simplicité on vide la liste des membres du groupe

            // effectively create the group server side
            if(!removeGroupWithAdminId(clientId)) {
                out.write(ServAns.FAILURE_DELETION + END_OF_LINE);
                out.flush();
                return false;
            }

            out.write(ServAns.SUCCESS_DELETION + END_OF_LINE);
            out.flush();
            clientGroupName = "";
            return true;
        }

        /**
         * Fonction qui gère le processus de création d'un groupe
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, String groupname) throws IOException {
            String password = null;

            // Ask the client for the password of the group
            out.write(ServAns.PASSWORD_FOR + END_OF_LINE);
            out.flush();

            // Receive the password
            clientIn = in.readLine();
            password = clientIn.split(" ")[1];

            if (password == null || password.isEmpty()) {
                out.write(ServAns.INVALID_PASSWORD + END_OF_LINE);
                out.flush();
                return false;
            }

            // effectively create the group server side
            Group newGroup = new Group(clientId, groupname, password);
            groups.add(newGroup);
            clientGroupName = groupname;
            clientInGroup = true;
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

        public void handleJoinGroup(BufferedReader in, BufferedWriter out, String groupName) throws IOException {
            Group group = getGroupByName(groupName);
            if(group == null) {
                out.write(ServAns.INVALID_GROUP + END_OF_LINE);
                out.flush();
                return;
            }

            String password = group.password(), triedPassword;
            out.write(ServAns.VERIFY_PASSWD + END_OF_LINE);
            out.flush();
            int count = 3;

            while (true){
                clientIn = in.readLine();
                switch (decodeClientMessage(clientIn.split(" ")[0])) {
                    case TRY_PASSWD:
                        count -= 1;
                        triedPassword = clientIn.split(" ")[1];
                        System.out.println(MsgPrf + "Client " + clientId + "tried password : " + triedPassword);
                        if (triedPassword.equals(password)) {
                            try{
                                getGroupByName(groupName).addMember(clientId);
                                clientInGroup = true;
                                System.out.println(MsgPrf + "Client " + clientId + " successfully joined group " + groupName);
                                clientGroupName = groupName;
                                out.write(ServAns.PASSWD_SUCCESS + END_OF_LINE);
                                out.flush();

                            } catch (NullPointerException e) {
                                System.out.println("Group dispeared D:");
                                out.write(ServAns.ERROR + END_OF_LINE);
                                out.flush();
                            }
                            return;
                        } else if (count != 0){
                            out.write(ServAns.RETRY_PASSWD + END_OF_LINE);
                            out.flush();
                        } else {
                            out.write(ServAns.NO_MORE_TRIES + END_OF_LINE);
                            out.flush();
                        }
                        break;
                    case INVALID:
                        System.out.println("Weird message" + clientIn);
                }
            }
        }

        public void handleReady(BufferedReader in, BufferedWriter out, int clientId) throws IOException {

            // on attend 10 secondes de voir si on demande quelque chose de nous
            int count = 0;
            while (count < 10) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    System.out.println("INTERRUPTED");
                }
                boolean makeFinal = getGroupByName(clientGroupName).getOnGoingMakeFinal();
                if (getGroupByName(getClientGroupName()).getToBeDeleted()){
                    System.out.println(MsgPrf + "Sending FORCE_QUIT ready to  ");
                    out.write(ServAns.FORCE_QUIT + END_OF_LINE);
                    out.flush();
                    return;
                } else if (makeFinal){
                    System.out.println(getLocalPrefix() + "Sending SEND_PREF_LIST ready");
                    out.write(ServAns.SEND_PREF_LIST + END_OF_LINE);
                    out.flush();
                    String clientResponse = in.readLine();
                    System.out.println(getLocalPrefix() + "client response: " + clientResponse);
                    if (decodeClientMessage(clientResponse).equals(ClientMessages.READY_SEND)){
                        receiveList(in, out);
                    } else {
                        System.out.println(MsgPrf + "WEIRD message received from client " + clientId + " : " + clientResponse);
                    }
                    return;
                }
                count +=1;
            }

            System.out.println(MsgPrf + "Sending release ready to  ");
            out.write(ServAns.RELEASE_READY  + END_OF_LINE);
            out.flush();

            // membersReady.add(clientId);
        }

        public void handleMake(BufferedReader in, BufferedWriter out) throws IOException {
            System.out.println(MsgPrf + "In handleMake");
            // assuming client is Admin
            getGroupWithAdminId(clientId).setOnGoingMakeFinal(true);

            // envoyer demander l'envoi de la liste de l'admin
            // ....

            System.out.println(getLocalPrefix() + "Sending SEND_PREF_LIST to client (admin) " + clientId);
            out.write(ServAns.SEND_PREF_LIST + END_OF_LINE);
            out.flush();

            String clientResponse = in.readLine();

            if (decodeClientMessage(clientResponse).equals(ClientMessages.READY_SEND)){
                receiveList(in, out);
            } else{
                System.out.println(MsgPrf + "WEIRD message received from client " + clientId + " : " + clientResponse);
            }

            // attendre que tout le monde ait envoyé sa liste
            while (!getGroupWithAdminId(clientId).everyoneSentList()){
                // attendre
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    System.out.println("INTERRUPTED");
                }
            }
            // maintenant que tout le monde a envoyé, faire la finale


            makeFinalList(getGroupWithAdminId(clientId), in, out);

            System.out.println(MsgPrf + "Exit handleMake");
        }

        /**
         * Handles client wanting to quit it's group
         * @param out
         * @throws IOException
         */
        private void handleQuitGroup(BufferedWriter out) throws IOException {
            for (Group group : groups) {
                if (group.hasMember(clientId)) {
                    group.removeMember(clientId);
                    out.write(ServAns.ACK_QUIT + END_OF_LINE);
                    out.flush();
                    clientGroupName = "";
                    return;
                }
            }
            // TODO should we signify client that it's not in a group ?;
            System.out.println("ERROR : client was not in a group D:");

            out.write(ServAns.ERROR_13 + END_OF_LINE);
            out.flush();
        }

        private ClientMessages decodeClientMessage(String message) {
            try{
                String command = message.split(" ")[0];
                return ClientMessages.valueOf(command);
            } catch (IllegalArgumentException e) {
                return ClientMessages.INVALID;
            }
        }

        public void handleBaseMenu(BufferedReader in, BufferedWriter out, String userMessage) throws IOException {
            String groupName;
            switch (decodeClientMessage(userMessage.split(" ")[0])) {
                case CREATE_GROUP :
                    groupName = userMessage.split(" ")[1];
                    System.out.println(MsgPrf + "Creating group with name " + groupName);
                    handleGroupCreation(in, out, groupName);

                    break;
                case LIST_GROUPS:
                    System.out.println(MsgPrf + "Listing groups to " + clientId);
                    handleGroupListing(out);
                    break;

                case JOIN:
                    groupName = userMessage.split(" ")[1];
                    handleJoinGroup(in, out, groupName);
                    break;

                case INVALID:
                    System.out.println(MsgPrf + "Invalid command received: " + userMessage);
                    break;

                case QUIT:
                    handleQuitGroup(out);
                    clientInGroup = false;
                    break;
            }
        }

        public void handleGroupMenu(BufferedReader in, BufferedWriter out, String userMessage) throws IOException {
            int clientId;
            switch (decodeClientMessage(userMessage)) {
                case MAKE:
                    System.out.println(MsgPrf + "Make not available yet ...");
                    handleMake(in, out);
                    break;
                case READY:
                    System.out.println(MsgPrf + "Client " + this.clientId + " is ready ...");
                    handleReady(in, out, this.clientId);
                    break;
                case DELETE_GROUP:
                    System.out.println(MsgPrf + "Deleting group ... ");
                    clientId = Integer.parseInt(clientIn.split(" ")[1]);
                    handleGroupDeletion(in, out, clientId);
                    clientInGroup = false;
                    break;
                case QUIT:
                    handleQuitGroup(out);
                    clientInGroup = false;
                    socket.close();
                    break;
            }
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)))
            {
                // Handle first contact
                clientIn = in.readLine();
                switch (decodeClientMessage(clientIn)) {
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
                String groupname;
                System.out.println(MsgPrf + "Waiting for client command ...");
                while (!socket.isClosed()) {
                    clientIn = in.readLine();
                    if (clientInGroup) {
                        handleGroupMenu(in, out, clientIn);
                    } else {
                        handleBaseMenu(in, out, clientIn);
                    }
                }
            } catch (IOException e) {
                System.out.println("Server exception: " + e.getMessage());
            }
        }

        /**
         * What to do to receive a list from a user

         * @param in
         * @param out
         * @throws IOException
         */
        public boolean receiveList(BufferedReader in, BufferedWriter out) throws IOException {
            System.out.println(MsgPrf + "In receiveList");
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

            // write the result
            JSON json = new JSON(clientId);

            json.writeFileWithLists(like, dislike, noopinion);

            System.out.println(MsgPrf + "Before with groupName : " + clientGroupName + " and client id " + clientId);
            getGroupByName(clientGroupName).memberSentList(clientId);

            System.out.println(MsgPrf + "End of receiveList");

            return true;
        }

        /**
         * When group admin calls this function, server will generate a group preference list, based on the members of the group
         * @param group : group to create the list from
         * @param in
         * @param out
         * @throws IOException
         */
        public void makeFinalList(Group group, BufferedReader in, BufferedWriter out) throws IOException {
            System.out.println(MsgPrf + "In make final");
            LinkedList<Integer> membersList = group.getMembersIdList();
            System.out.println("Client id " + clientId);
            JSON jsonInteractor = new JSON(clientId);

            LinkedList<JSONArray> allDislikes = new LinkedList<>();
            LinkedList<String> finalList = new LinkedList<>();

            // loop a first time to load every json content
            for (Integer memberId : membersList) {
                // ask their list

                //TODO WHEN TESTING OVER, REMOVE THIS LINE WITH COMMENTED ONE
                //String filePath = "serverfiles/test" + memberId + ".json";
                 String filePath = "serverfiles/user" + memberId + ".json";

                JSONObject x =  jsonInteractor.loadJsonFile(filePath);

                Object likes = x.get("like");
                Object dislikes = x.get("dislike");

                // likes or dislikes is not a list, this is weird
                if (!(likes instanceof JSONArray jslikes) || !(dislikes instanceof JSONArray JSdislikes)){
                    System.out.println("WEIRD : likes and dislikes should be an array");
                    continue;
                }
                boolean alreadyIn;
                for (int i = 0; i < jslikes.length(); i++) {    // for each style in "likes", if it's not already in, we add it
                    alreadyIn = false;
                    // TODO this is probably not ASD friendly
                    String newStyle = jslikes.get(i).toString();
                    for (String s : finalList) {
                        if (newStyle.equals(s)) {
                            alreadyIn = true;
                            break;
                        }
                    }
                    // add only if not already in
                    if (!alreadyIn) {
                        finalList.add(newStyle);
                    }
                }
                allDislikes.add(JSdislikes);
            }

            System.out.println("At this point, we have");
            System.out.println(finalList);
            // add the likes in the final list

            // after every likes has been added, we removes the styles that we find in both the finallist and the dislikes
            for (JSONArray dislikes : allDislikes) {
                for (int i = 0; i < dislikes.length(); i++) {
                    for (int j = 0 ; j < finalList.size(); j++) {
                        if (dislikes.get(i).toString().equals(finalList.get(j))){
                            finalList.remove(j);
                            j = j - 1;
                        }
                    }
                }
            }
            System.out.println("Final result :");
            for (String style : finalList) {
                System.out.println(style);
            }
        }
    }
}