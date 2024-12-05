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

import ch.heigvd.dai.util.JSON;
import picocli.CommandLine;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.heigvd.dai.consulyProtocolEnums.*;


@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {
    private static final String END_OF_LINE = "\n";
    private static final String MsgPrf = "[Client] ";
    private static boolean connectedToServer, inAGroup = false;
    private boolean isAdmin = false;
    private ClientMessages clientMessage;
    private String serverOut, clientIn;
    private int id;
    private String joinedGroupName = "";
    public static final String ANSI_RESET = "\u001B[0m", ANSI_GREEN = "\u001B[32m", BACKGROUND_RED = "\u001B[41m", ANSI_RED = "\u001B[31m", ANSI_YELLOW = "\u001B[33m";


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
    public Integer call() throws IOException {
        if (edit) {
            edit();
            return 0;
        } else {
            return connectToServer();
        }
    }

    private void edit() throws IOException {
        JSON json = new JSON(this.id);
        json.createByAsking();
    }

    private void showGroupMenu(){
        if (joinedGroupName == null) {
            System.out.println("Currently not in a group.");
            return;
        }

        System.out.print("Currently in group " + joinedGroupName + ". Choose an option: ");
        if (isAdmin){
            System.out.println("(you are admin)");
            System.out.println(ANSI_GREEN + "\tMAKE" + ANSI_RESET  + " make the final playlist for the group");
            System.out.println(ANSI_RED + "\tDELETE" + ANSI_RESET  + " delete the group" + ANSI_RESET);
        } else {
            System.out.println(ANSI_GREEN + "\nREADY" + ANSI_RESET + " signify the server that you are ready for final playlist (or be kicked)");
            System.out.println(BACKGROUND_RED + "\tQUIT" + ANSI_RESET + " quit the group");
        }
        System.out.print("->");
    }

    private void showMenu(){
        System.out.println("Choose an option ?");
        System.out.println(ANSI_GREEN + "\tCREATE " + ANSI_RESET + " create a group" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "\tJOIN " + ANSI_RESET + " join a group");
        System.out.println(ANSI_GREEN + "\tLIST " + ANSI_RESET + "  Display all existing groups");
        System.out.println(BACKGROUND_RED + "\tQUIT" + ANSI_RESET);
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
            String message = response.split(" ")[0];
            return ServAns.valueOf(message);
        } catch (IllegalArgumentException e) {
            return ServAns.WEIRD_ANSWER;
        } catch (NullPointerException e) {
            System.out.println(ANSI_RED + "Can not decode NULL" + ANSI_RESET);
            return null;
        }
    }

    private void handleGroupDeletion(BufferedReader in, BufferedWriter out) throws IOException {
        if (joinedGroupName == null) {
            System.out.println("WEIRD. No group joined");
            return;
        }
        out.write(ClientMessages.DELETE_GROUP + " " + this.id + END_OF_LINE); // Send the client id for verification
        out.flush();
        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.INVALID_GROUP:
                    System.out.println(ANSI_RED + MsgPrf + "The group does not exist." + ANSI_RESET);
                    return;
                case ServAns.INVALID_ID:
                    System.out.println(ANSI_RED + MsgPrf + "You are not the owner of the group." + ANSI_RESET);
                    return;
                case ServAns.FAILURE_DELETION:
                    System.out.println(ANSI_RED + MsgPrf + "Failed to delete the group." + ANSI_RESET);
                    return;
                case ServAns.SUCCESS_DELETION:
                    System.out.println(ANSI_GREEN + MsgPrf + "Group deleted successfully." + ANSI_RESET);
                    isAdmin = false;
                    inAGroup = false;
                    return;
                case null:
                    System.out.println(ANSI_RED + "Received a null response." + ANSI_RESET);
                    break;
                default:
                    System.out.println("WEIRD : received " + serverOut);
            }
        }
    }

    private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, BufferedReader stdIn, String groupName) throws IOException {
        String password;

        out.write(ClientMessages.CREATE_GROUP + " " + groupName + END_OF_LINE);
        out.flush();

        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut.split(" ")[0])) {
                case CHOOSE_PASSWORD:
                    System.out.print("Enter a password for the group : ");
                    password = stdIn.readLine();
                    System.out.println();
                    out.write(ClientMessages.PASSWORD + " " + password + END_OF_LINE);
                    out.flush();
                    break;
                case ServAns.VALID_PASSWORD:
                    System.out.println(ANSI_GREEN + MsgPrf + "Group created successfully." + ANSI_RESET);
                    isAdmin = inAGroup = true;
                    joinedGroupName = groupName;
                    return true;
                case ServAns.INVALID_PASSWORD:
                    System.out.println(ANSI_RED + MsgPrf + "Invalid password" + ANSI_RESET);
                    return false;
            }
        }
    }

    private boolean handleGroupJoin(BufferedReader in, BufferedWriter out, BufferedReader stdIn, String chosenGroupName) throws IOException {
        out.write(ClientMessages.JOIN + " " + chosenGroupName + END_OF_LINE);
        out.flush();

        String userPasswdGuess;
        while (true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut.split(" ")[0])) {
                case VERIFY_PASSWD:
                    System.out.print("Password for this group: ");
                    userPasswdGuess = stdIn.readLine();
                    out.write(ClientMessages.TRY_PASSWD + " " + userPasswdGuess + END_OF_LINE);
                    out.flush();
                    break;

                case RETRY_PASSWD:
                    System.out.println(ANSI_RED + "Incorrect password. Please try again." + ANSI_RESET);
                    System.out.print("Password for this group: ");
                    userPasswdGuess = stdIn.readLine();
                    out.write(ClientMessages.TRY_PASSWD + " " + userPasswdGuess + END_OF_LINE);
                    out.flush();
                    break;

                case PASSWD_SUCCESS:
                    System.out.println(ANSI_GREEN + "Successfully joined the group!" + ANSI_RESET);
                    inAGroup = true;
                    joinedGroupName = chosenGroupName;
                    return true;

                case NO_MORE_TRIES:
                    System.out.println(ANSI_RED + "Too many failed attempts. You cannot join this group, bye bye." + ANSI_RESET);
                    return false;
                case INVALID_GROUP:
                    System.out.println(ANSI_RED + "The group does not exist." + ANSI_RESET);
                    return false;
                case ERROR_8:
                    System.out.println(ANSI_RED + "Error happened during password verification" + ANSI_RESET);
                    return false;
                default:
                    System.out.println(ANSI_RED + "Unexpected response: " + serverOut + ANSI_RESET);
                    return false;
            }
        }
    }

    private void handleGroupQuit(BufferedWriter out) throws IOException {
        joinedGroupName = "";
        inAGroup = false;
        out.write(ClientMessages.QUIT + END_OF_LINE);
        out.flush();
    }

    private boolean handleGroupList(BufferedReader in, BufferedWriter out) throws IOException {
        out.write(ClientMessages.LIST_GROUPS + END_OF_LINE);
        out.flush();
        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.END_OF_LIST:
                    System.out.println(MsgPrf + "End of list.");
                    return true;
                case ServAns.NO_GROUP:
                    System.out.println(MsgPrf + "No group available.");
                    return false;
                case ServAns.AVAILABLE_GROUP:
                    // TODO maybe improve this display if possible ?
                    System.out.println("GROUP : " + serverOut.split(" ")[1]);
                    break;
                case ServAns.WEIRD_ANSWER:
                    System.out.println(ANSI_YELLOW + "WEIRD :" + serverOut + ANSI_RESET);
                    return false;
            }
        }
    }

    private void handleMake(BufferedReader in, BufferedWriter out) throws IOException {
        out.write(ClientMessages.MAKE + END_OF_LINE);
        out.flush();

        String servResponse = in.readLine();

        switch (decodeServerAnswer(servResponse)) {
            case SEND_PREF_LIST : sendPreferences(out, in);
                break;
            default:
                System.out.println("Weird response : " + servResponse);
        }
    }

    private void handleReady(BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        out.write(ClientMessages.READY + " " + this.id + END_OF_LINE);
        out.flush();

        System.out.println("You signified that you are ready ...");
        while(true) {
            serverOut = in.readLine();
            switch (decodeServerAnswer(serverOut)) {
                case ServAns.FORCE_QUIT:
                    System.out.println(ANSI_RED + MsgPrf + "You have been kicked from the group." + ANSI_RESET);
                    handleGroupQuit(out);
                    return;
                case ServAns.SEND_PREF_LIST:
                    // envoyer la liste de préférence
                    sendPreferences(out, in);
                    return;

                case ServAns.RELEASE_READY:
                    System.out.println(ANSI_GREEN + MsgPrf + "You can now act again." + ANSI_RESET);
                    return;
            }
        }
    }

    private void groupMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        // maybe ask the group info to the server
        showGroupMenu();
        while (true) {
            System.out.print(">");
            clientIn = stdIn.readLine();
            System.out.print(END_OF_LINE);
            // if not admin, have another dedicated switch
            switch (decodeGroupMenuInput(clientIn)) {
                case MAKE:
                    System.out.println("Making the final playlist");
                    handleMake(in, out);
                    return;
                case DELETE:
                    handleGroupDeletion(in, out);
                    return;
                case READY:
                    System.out.println("Signifying the server that you are ready for final playlist or to be kicked");
                    handleReady(in, out, stdIn);
                    return;
                case QUIT:
                    if (isAdmin) handleGroupDeletion(in, out); // Delete le group avant de quitter si admin
                    handleGroupQuit(out);
                    socket.close();
                    return;
                case INVALID:
                    System.out.println(ANSI_RED + "Invalid input. Try again" + ANSI_RESET);
                    break;
            }
        }
    }

    private void baseServerMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        showMenu();
        BaseMenuCmd input = BaseMenuCmd.INVALID;
        while (input == BaseMenuCmd.INVALID) {
            System.out.print(">");
            clientIn = stdIn.readLine();
            System.out.print(END_OF_LINE);

            input = decodeBaseMenuInput(clientIn);
            String groupName = null;
            switch (input) {
                case CREATE:
                    while(groupName == null) {
                        System.out.print("What's the name of your awesome group ? ");
                        groupName = stdIn.readLine();
                    }
                    if(!handleGroupCreation(in, out, stdIn, groupName)) {
                        System.out.println(ANSI_RED + "Failed to create the group." + ANSI_RESET);
                    }
                    break;
                case JOIN:
                    if (!handleGroupList(in, out)){
                        break;
                    }
                    System.out.print("Which group do you want to join ? ");
                    String chosenGroupName = stdIn.readLine();
                    if(!handleGroupJoin(in, out, stdIn, chosenGroupName)) {
                        System.out.println("RETURNING");
                        return;
                    }
                    break;
                case LIST:
                    handleGroupList(in, out);
                    break;
                case QUIT:
                    socket.close();
                    break;
                case INVALID:
                    System.out.println(ANSI_RED + "Invalid input. Try again\n\n" + ANSI_RESET);
                    break;
            }
        }
    }

    private int connectToServer() {
        System.out.println(MsgPrf + "Connecting to host " + host + " on port " + port);
        // Check if the user has a preferences file
        try(FileReader reader = new FileReader("userfile/user0.json")) {
            System.out.println(ANSI_GREEN + "Preferences file found." + ANSI_RESET);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "No preferences file found. Please create one by adding an \"-e\" argument." + ANSI_RESET);
            return 1;
        }

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
                    System.out.println("BASE SERVER MENU");
                    baseServerMenu(socket, in, out, stdIn);
                }
            }

            System.out.println("End of transmission with server");

        } catch (IOException e) {
            System.out.println(ANSI_RED + "Unable to connect to host " + host + " on port " + port + ANSI_RESET);
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
            System.out.println(ANSI_RED + "WEIRD RESPONSE " + serverOut + " TO " + message + ANSI_RESET);
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
            out.write(clientIn + "\n");
            out.flush();

            serverOut = in.readLine();
            if (!serverOut.equals("ACK")) {
                System.out.println(ANSI_RED + "WEIRD RESPONSE " + serverOut + " TO " + clientIn + ANSI_RESET);
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
            out.write("READY_SEND\n");
            out.flush();
            serverOut = in.readLine();
            if (!serverOut.equals("READY_RECEIVE")) {
                System.out.println(ANSI_RED + "WEIRD RESPONSE " + serverOut + " TO READY_SEND" + ANSI_RESET);
            } else {
                System.out.println(ANSI_GREEN + "Received : " + serverOut + ANSI_RESET);
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
            System.err.println(ANSI_RED + "Erreur lors de la lecture du fichier JSON : " + e.getMessage() + ANSI_RESET);
        }
    }
}
