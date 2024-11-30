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

import java.util.concurrent.Callable;

import ch.heigvd.dai.util.JSON;
import picocli.CommandLine;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.heigvd.dai.consulyProtocolEnums.*;


@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {
    private boolean connectedToServer = false;

    private static final String END_OF_LINE = "\n";
    private static final String EOT = "\u0004";
    private static final String MsgPrf = "[Client] : ";
    private static boolean inAGroup = false;

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
        System.out.println("In edit2");
        JSON json = new JSON();
        json.createByAsking();
    }

    private int connectToServer() {
        System.out.println("Connecting to host " + host + " on port " + port);
        try (
                Socket socket = new Socket(host, port); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); // BufferedReader to read input from the server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // PrintWriter to send output to the server
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)); // BufferedReader to read input from the standard input (console)
        ) {
            System.out.println("Connected successfully! On session " + port);
            out.write("CONNECTED\n");
            out.flush();

            String response;
            while(!socket.isClosed()) {

                // Client reads the message from the server until it finds EOT
                while ((response = in.readLine()) != null && !response.equals(EOT)) {
                    System.out.println("[Server] " + response);
                }

                // Client chooses an option
                System.out.print("Enter your message: ");
                String message = stdIn.readLine();
                out.write(message + "\n");
                out.flush();

                // Read server response after sending the message
                while ((response = in.readLine()) != null && !response.equals(EOT)) {
                    System.out.println("[Server] " + response);
                }

                if(message.equalsIgnoreCase("5")) {
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Unable to connect to host " + host + " on port " + port);
            e.printStackTrace();
        }
        return 0;
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
        System.out.println(MsgPrf + "want to delete group" + END_OF_LINE);
        out.write(ClientMessages.DELETE_GROUP + END_OF_LINE);
        out.flush();

        String response;
        response = in.readLine();
        if (!response.equals(ServAns.SUCCESS_DELETION.toString())) {
            System.out.println("[Server] " + response);
            return false;
        }

        inAGroup = false;
        System.out.println(MsgPrf + "Group deleted");
        return true;
    }

    private boolean handleGroupCreation(BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        System.out.println("Group creation");
        out.write(ClientMessages.CREATE_GROUP + END_OF_LINE);
        out.flush();

        String servResponse = in.readLine();
        if (! servResponse.equals(ServAns.PASSWORD_FOR.toString())){
            return false;
        }

        System.out.println("Choisir un mot de passe : ");
        String chosenPassword = stdIn.readLine();

        out.write(ClientMessages.PASSWORD + " " + chosenPassword + END_OF_LINE);
        out.flush();

        // On s'attend à VALIDPASSWD

        servResponse = in.readLine();
        if (! servResponse.equals(ServAns.VALID_PASSWORD.toString())){
            // return false pour le moment, mais peut-être qu'on voudrait redonner un essai au user ?
            System.out.println("Aborting (1)");
            return false;
        }
        System.out.println(MsgPrf + "received : " + servResponse);
        // inform the server

        out.write(ClientMessages.ACK_VALID + END_OF_LINE);
        out.flush();

        servResponse = in.readLine();

        if (! servResponse.contains(ServAns.GRANT_ADMIN.toString())){
            // return false pour le moment, mais peut-être qu'on voudrait redonner un essai au user ?
            System.out.println("Aborting (2)");
            return false;
        }
        String groupIdStr = servResponse.split(" ")[1];

        System.out.println(MsgPrf + "Joined group " + groupIdStr + " !");
        this.inAGroup = true;
        return true;
    }

    private void groupMenu(Socket socket, BufferedReader in, BufferedWriter out, BufferedReader stdIn) throws IOException {
        // maybe ask the group info to the server
        System.out.println("In group : <someGroupId");
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
                    handleGroupCreation(in, out, stdIn); break;
                case DELETE:
                    System.out.println("Are you sure it's ok to delete a group from outside the group ?");
                    break;
                case LIST:
                    System.out.println("List the groups");
                    break;
                case JOIN:
                    System.out.println("Join the group");
                    break;
                case QUIT:
                    System.out.println("Quit the server");
                    socket.close();
                    break;
                case INVALID:
                    System.out.println("Invalid input. Try again");
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
            System.out.println("Connected successfully! On session " + port);
            out.write("CONNECT_SRV\n"); out.flush();
            // decoder la réponse du serveur
            String response;
            response = in.readLine();
            if (response != null && !response.equals(EOT)) {
                System.out.println("[Server] " + response);
                if (response.equals(ServAns.REFUSED_CONNECT.toString())) {
                    socket.close();
                } else if (response.equals(ServAns.ACCEPT_CONNECT.toString())) {
                    connectedToServer = true;
                } else {
                    System.out.println("Weird server answer. Closing.");
                    socket.close();
                }
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
