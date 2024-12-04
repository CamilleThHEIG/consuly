package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.sql.SQLOutput;
import java.util.concurrent.Callable;

import ch.heigvd.dai.util.Group;
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
    private static ServAns responseServ;
    private static int id;
    private static Group group = null;

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
            System.out.println("QUIT : quit the group");
        }
        System.out.println("SHOW_MENU : show this menu again (éventuellement)");
        System.out.print("->");
    }

    private void showMenu(){
        System.out.println("Choose an option ?");
        System.out.println("CREATE : create a new group");
        System.out.println("DELETE : delete a group");
        System.out.println("JOIN : join a group");
        System.out.println("SHOW : show all existing groups");
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
        String serverResponse;
        ServAns responseServ;

        System.out.println("Group deletion");

        // DELETE_GROUP <groupname>
        out.write(ClientMessages.DELETE_GROUP + " " + this.id + " " + this.group.name() + END_OF_LINE); // Send the client id for verification and the group name
        out.flush();

        while(true) {
            serverResponse = in.readLine();
            switch (decodeServerAnswer(serverResponse)) {
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
        System.out.println("Group creation");
        String serverResponse, password = null;
        ServAns responseServ;

        // CREATE_GROUP <groupname>
        out.write(ClientMessages.CREATE_GROUP + " " + groupname + END_OF_LINE);
        out.flush();

        while(true) {
            serverResponse = in.readLine();
            switch (decodeServerAnswer(serverResponse)) {
                case PASSWORD_FOR:
                    System.out.print("Enter a password for the group : ");
                    password = stdIn.readLine();
                    out.write(ClientMessages.PASSWORD + " " + password + END_OF_LINE);
                    out.flush();
                    break;
                case ServAns.VALID_PASSWORD:
                    System.out.println(MsgPrf + "Group created successfully.");
                    group = new Group(groupname, this.id, password);
                    inAGroup = true;
                    return true;
                case ServAns.INVALID_PASSWORD:
                    System.out.println(MsgPrf + "Invalid password");
                    return false;
            }
        }
    }

    private void groupMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        // maybe ask the group info to the server
        showGroupMenu();
        String userInput;
        GroupMenuCmd input = GroupMenuCmd.INVALID;
        while (input == GroupMenuCmd.INVALID) {
            System.out.print(">");
            userInput = stdIn.readLine();

            input = decodeGroupMenuInput(userInput);
            switch (input){
                case DELETE: handleGroupDeletion(in, out, stdIn); break;

                // case READY : handleReady(.....)

                case INVALID:
                    System.out.println("Invalid input. Try again");
                    break;
            }
        }

    }

    private void baseServerMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        showMenu();
        String userInput;
        BaseMenuCmd input = BaseMenuCmd.INVALID;
        while (input == BaseMenuCmd.INVALID) {
            System.out.print(">");
            userInput = stdIn.readLine();
            input = decodeBaseMenuInput(userInput);

            switch (input) {
                case CREATE:
                    System.out.print("What's the name of your awesome group ?");
                    String groupname = stdIn.readLine();
                    handleGroupCreation(in, out, stdIn, groupname); break;
                case DELETE:
                    handleGroupDeletion(in, out, stdIn);
                    break;
                case LIST:
                    System.out.println("List the groups. Not available yet.\n\n");
                    break;
                case JOIN:
                    System.out.println("Join the group. Not available yet.\n\n");
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

            String response;
            response = in.readLine();
            String command = response.split(" ")[0]; // Extract the command part of the response

            switch(decodeServerAnswer(command)) {
                case ACCEPT_CONNECT:
                    this.id = Integer.parseInt(response.split(" ")[1]);; // get the id given by the server
                    connectedToServer = true;
                    System.out.println("Connected to server with id " + this.id);
                    break;
                case REFUSED_CONNECT, WEIRD_ANSWER:
                    socket.close();
                    return 1;
            }

            // user input loop
            String userInput;
            BaseMenuCmd input;

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
        String serverResponse;
        out.write(message + "\n");
        out.flush();
        serverResponse = in.readLine();
        if (!serverResponse.equals("ACK")) {
            System.out.println("WEIRD RESPONSE " + serverResponse + " TO " + message);
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
        String serverResponse;
        String message;
        for (int j = 0; j < list_to_send.length(); ++j) {
            message = "STYLE<" + list_to_send.getString(j) + ">";
            System.out.println("Sending : " + message);
            out.write(message + "\n");
            out.flush();

            serverResponse = in.readLine();
            if (!serverResponse.equals("ACK")) {
                System.out.println("WEIRD RESPONSE " + serverResponse + " TO " + message);
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
            String serverResponse = in.readLine();
            if (!serverResponse.equals("READY_RECEIVE")) {
                System.out.println("WEIRD RESPONSE " + serverResponse + " TO READY_SEND");
            } else {
                System.out.println("Received : " + serverResponse);
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
