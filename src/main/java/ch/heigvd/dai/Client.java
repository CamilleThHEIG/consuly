package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.Callable;
import ch.heigvd.dai.util.Group;
import java.io.IOException;
import ch.heigvd.dai.util.JSON;
import picocli.CommandLine;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.heigvd.dai.consulyProtocolEnums.*;


@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {
    private static final String END_OF_LINE = "\n";
    private static final String MsgPrf = "[Client] : ";
    private static boolean connectedToServer, inAGroup = false;
    private boolean isAdmin = false;
    private ClientMessages clientMessage;
    private ServAns servAnswer;
    private String serverOut, clientIn;
    private int id;
    private Group group = null;

    @CommandLine.Option(
            names = {"-h", "--host"},
            description = "Host to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "localhost")
    protected String host;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    @CommandLine.Option(
            names = {"-e", "--e"},
            description = "Include this if you want to edit your preferences json file.",
            defaultValue = "False")
    protected boolean edit;

    @Override
    public Integer call() throws FileNotFoundException, UnsupportedEncodingException {
        if (edit) {
            edit();
            return 0;
        } else {
            return connectToServer2();
        }
    }

    private void edit(){
        JSON json = new JSON();
        json.createByAsking();
    }


    private void showGroupMenu(){
        System.out.print("Currently in group " + group.name() + ". Choose an option: ");
        if (isAdmin){
            System.out.println("(you are admin)");
            System.out.println("MAKE : make the final playlist for the group");
            System.out.println("DELETE : delete the group");
        } else {
            System.out.println("\nREADY : signify the server that you are ready for final playlist (or be kicked)");
        }
        System.out.println("QUIT : quit the group");
        //System.out.println("SHOW_MENU : show this menu again (éventuellement)");
        System.out.print("->");
    }

    private void showMenu(){
        System.out.println("Choose an option ?");
        System.out.println("CREATE : create a new group");
        //System.out.println("DELETE : delete a group");
        System.out.println("JOIN : join a group");
        System.out.println("LIST : Display all existing groups");
        System.out.println("QUIT");
    }

    private GroupMenuCmd decodeGroupMenuInput(String input){
        try {
            return GroupMenuCmd.valueOf(input);
        } catch (IllegalArgumentException e) {
            return GroupMenuCmd.INVALID;
        }
    }

    private BaseMenuCmd decodeBaseMenuInput(String input){
        try {
            return BaseMenuCmd.valueOf(input);
        } catch (IllegalArgumentException e) {
            return BaseMenuCmd.INVALID;
        }
    }

    private ServAns decodeServerAnswer(String response) {
        try{
            return ServAns.valueOf(response);
        } catch (IllegalArgumentException e) {
            return ServAns.WEIRD_ANSWER;
        }
    }


    // handleReady(.....){
    // rep = in.readline()
    // en fonction de rep (qui peut être SEND_LIST, OU FORCEQUIT, ou REALEASE_READY) faire une action

    private boolean handleGroupDeletion(BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        System.out.println("Group deletion");

        // DELETE_GROUP <groupname>
        out.write(ClientMessages.DELETE_GROUP + " " + this.id + " " + this.group.name() + END_OF_LINE); // Send the client id for verification and the group name
        out.flush();

        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.INVALID_GROUP:
                    System.out.println(MsgPrf + "The group does not exist.");
                    return false;
                case ServAns.INVALID_ID:
                    System.out.println(MsgPrf + "You are not the owner of the group.");
                    return false;
                case ServAns.FAILURE_DELETION:
                    System.out.println(MsgPrf + "Failed to delete the group.");
                    return false;
                case ServAns.WAITING_USER_TO_QUIT:
                    System.out.println(MsgPrf + "Waiting for users to quit the group.");
                    continue;
                case ServAns.SUCCESS_DELETION:
                    System.out.println(MsgPrf + "Group deleted successfully.");
                    return true;
            }
        }
    }

    private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, BufferedReader stdIn, String groupname) throws IOException {
        String password = null;

        // CREATE_GROUP <groupname>
        out.write(ClientMessages.CREATE_GROUP + " " + groupname + END_OF_LINE);
        out.flush();

        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut.split(" ")[0])) {
                case PASSWORD_FOR:
                    System.out.print("Enter a password for the group : ");
                    password = stdIn.readLine();
                    out.write(ClientMessages.PASSWORD + " " + password + END_OF_LINE);
                    out.flush();
                    break;
                case ServAns.VALID_PASSWORD:
                    System.out.println(MsgPrf + "Group created successfully.");
                    group = new Group(groupname, this.id, password);
                    isAdmin = inAGroup = true;
                    return true;
                case ServAns.INVALID_PASSWORD:
                    System.out.println(MsgPrf + "Invalid password");
                    return false;
            }
        }
    }

    private void handleGroupJoin(BufferedReader in, BufferedWriter out, BufferedReader stdIn) {
//         out.write(ClientMessages.JOIN + " " + groupname + END_OF_LINE);
//         out.flush();
//         String serverResponse = in.readLine();
//         switch (decodeServerAnswer(serverResponse)) {
//             case ServAns.INVALID_GROUP:
//                 System.out.println(MsgPrf + "The group does not exist.");
//                 return false;
//             case ServAns.INVALID_ID:
//                 System.out.println(MsgPrf + "You are not the owner of the group.");
//                 return false;
//             case ServAns.FAILURE_DELETION:
//                 System.out.println(MsgPrf + "Failed to delete the group.");
//                 return false;
//             case ServAns.WAITING_USER_TO_QUIT:
//                 System.out.println(MsgPrf + "Waiting for users to quit the group.");
//                 continue;
//             case ServAns.SUCCESS_DELETION:
//                 System.out.println(MsgPrf + "Group deleted successfully.");
//                 return true;
//         }
    }

    private void handleGroupList(BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        out.write(ClientMessages.LIST_GROUPS + END_OF_LINE);
        out.flush();

        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.END_OF_LIST:
                    System.out.println(MsgPrf + "End of list.");
                    return;
                case ServAns.NO_GROUP:
                    System.out.println(MsgPrf + "No group available.");
                    return;
                default:
                    System.out.println(serverOut);
            }
        }
    }

    private void groupMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        // maybe ask the group info to the server
        showGroupMenu();
        GroupMenuCmd input = GroupMenuCmd.INVALID;
        while (input == GroupMenuCmd.INVALID) {
            System.out.print(">");
            clientIn = stdIn.readLine();

            switch (decodeGroupMenuInput(clientIn)) {
                case MAKE:
                    System.out.println("Making the final playlist");
                    handleMake(in, out, stdIn);
                    break;
                case DELETE:
                    handleGroupDeletion(in, out, stdIn);
                    break;
                case READY:
                    System.out.println("Signifying the server that you are ready for final playlist");
                    handleReady(in, out, stdIn);
                    break;
                case INVALID:
                    System.out.println("Invalid input. Try again");
                    break;
            }
        }
    }

    private void handleMake(BufferedReader in, BufferedWriter out, BufferedReader stdIn) {

    }

    private void handleReady(BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        out.write(ClientMessages.READY + END_OF_LINE);
        out.flush();

        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.SUCCESS_DELETION:
                    System.out.println(MsgPrf + "Group deleted successfully.");
                    return;
                case ServAns.WAITING_USER_TO_QUIT:
                    System.out.println(MsgPrf + "Waiting for users to quit the group.");
                    continue;
                case ServAns.FORCE_QUIT:
                    System.out.println(MsgPrf + "You have been kicked from the group.");
                    return;
            }
        }
    }

    private void baseServerMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        showMenu();
        BaseMenuCmd input = BaseMenuCmd.INVALID;
        while (input == BaseMenuCmd.INVALID) {
            System.out.print(">");
            clientIn = stdIn.readLine();
            input = decodeBaseMenuInput(clientIn);

            switch (input) {
                case CREATE:
                    System.out.println("Group creation");
                    System.out.print("What's the name of your awesome group ?");
                    String groupname = stdIn.readLine();
                    handleGroupCreation(in, out, stdIn, groupname); break;
//                case DELETE:
//                    handleGroupDeletion(in, out, stdIn);
//                    break;
                case JOIN:
                    handleGroupList(in, out, stdIn);
                    System.out.println("Which group do you want to join ?");
                    handleGroupJoin(in, out, stdIn);
                    break;
                case LIST:
                    handleGroupList(in, out, stdIn);
                    break;
                case QUIT:
                    System.out.println("Quit the server. Not available yet.\n\n");
                    socket.close();
                    break;
                case INVALID:
                    System.out.println("Invalid input. Try again\n\n");
                    break;
            }
        }
    }

    private int connectToServer2() {
        System.out.println(MsgPrf + "Connecting to host " + host + " on port " + port);
        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); // BufferedReader to read input from the server
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)); // BufferedReader to read input from the standard input (console)
        ){
            out.write(ClientMessages.CONNECT_SRV + END_OF_LINE); // first contact with server
            out.flush();

            serverOut = in.readLine();
            String command = serverOut.split(" ")[0]; // Extract the command part of the response

            switch(decodeServerAnswer(command)) {
                case ACCEPT_CONNECT:
                    this.id = Integer.parseInt(serverOut.split(" ")[1]);; // get the id given by the server
                    connectedToServer = true;
                    System.out.println("Connected to server with id " + this.id);
                    break;
                case REFUSED_CONNECT, WEIRD_ANSWER:
                    socket.close();
                    return 1;
            }

            // user input loop
            while (!socket.isClosed()) {
                if (inAGroup){
                    groupMenu(socket, in, out, stdIn);
                } else {
                    baseServerMenu(socket, in, out, stdIn);
                }
            }

            System.out.println("End of transmission with server");

        } catch (IOException e) {
            System.out.println("Unable to connect to host " + host + " on port " + port);
        }
        return 0;
    }

    /**
     * send a message that expects ACK from the server
     * @param out
     * @param in
     * @param message
     * @throws IOException
     */
    private void sendAckExpectedMessage(BufferedWriter out, BufferedReader in, String message) throws IOException {
        out.write(message + "\n");
        out.flush();
        serverOut = in.readLine();
        if (!serverOut.equals("ACK")) {
            System.out.println("WEIRD RESPONSE " + serverOut + " TO " + message);
        } else {
            //System.out.println("Received : " + serverResponse);
        }
    }

    /**
     * send a sublist (the like list for example) to server
     * @param out
     * @param in
     * @param list_to_send
     * @throws IOException
     */
    private void sendList(BufferedWriter out, BufferedReader in, JSONArray list_to_send) throws IOException {
        for (int j = 0; j < list_to_send.length(); ++j) {
            clientIn = "STYLE<" + list_to_send.getString(j) + ">";
            System.out.println("Sending : " + clientIn);
            out.write(clientIn + "\n");
            out.flush();

            serverOut = in.readLine();
            if (!serverOut.equals("ACK")) {
                System.out.println("WEIRD RESPONSE " + serverOut + " TO " + clientIn);
            }
        }
    }

    /**
     * send the local preference list to the server
     * @param out out buffer to user
     * @param in buffer to receive message from server
     * @throws IOException
     */
    private void sendPreferences(BufferedWriter out, BufferedReader in) throws IOException {
        // this section will later be replaced with cleaner interaction with json
        try (FileReader reader = new FileReader("userfiles/user0.json")) {
            System.out.println("Sending READY_SEND");
            out.write("READY_SEND\n");
            out.flush();
            serverOut = in.readLine();
            if (!serverOut.equals("READY_RECEIVE")) {
                System.out.println("WEIRD RESPONSE " + serverOut + " TO READY_SEND");
            } else {
                System.out.println("Received : " + serverOut);
            }

            // Lire le contenu du fichier dans une chaîne
            StringBuilder content = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                content.append((char) i);
            }
            // Convertir la chaîne en objet JSON
            JSONObject jsonObject = new JSONObject(content.toString());
            // Lire les données
            JSONArray like = jsonObject.getJSONArray("like");
            JSONArray dislike = jsonObject.getJSONArray("dislike");
            JSONArray noopinion = jsonObject.getJSONArray("noopinion");

            sendAckExpectedMessage(out, in, "SENDING_LIKES");
            sendList(out, in, like);
            sendAckExpectedMessage(out, in, "SENDING_DISLIKES");
            sendList(out, in, dislike);
            sendAckExpectedMessage(out, in, "SENDING_NEUTRAL");
            sendList(out, in, noopinion);
            sendAckExpectedMessage(out, in, "FINISHED");

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier JSON : " + e.getMessage());
        }
    }
}
