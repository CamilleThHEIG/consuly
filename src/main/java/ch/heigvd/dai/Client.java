package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.Callable;
import java.io.IOException;

import ch.heigvd.dai.util.JSON;
import picocli.CommandLine;
import org.json.*;

import java.util.Scanner;
import java.util.concurrent.Callable;


import org.json.JSONArray;
import org.json.JSONObject;

import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {
    private static final String EOT = "\u0004";

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

    protected String message = "Hello, server! I'm the client. 🤖";

    @Override
    public Integer call() throws FileNotFoundException, UnsupportedEncodingException {
        if (edit) {
            edit();
            return 0;
        } else {
            return connectToServer();
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
