package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.json.JSONArray;
import org.json.JSONObject;

import picocli.CommandLine;

/*
    * This class is the entry point for the client side of the application. It is
    * responsible for connecting to the server and sending messages to it.
 */
@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {

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
            names = {"-e", "--edit"},
            description = "Edit the user preferences.",
            defaultValue = "false")
    protected boolean edit;

    protected String message, response, answer;

    @Override
    public Integer call() throws FileNotFoundException, UnsupportedEncodingException {
        // Check if the user doesn't have a list of preferences or wants to edit them
        if (!new File("user.json").exists() || edit) {
            edit();
        }

        try (Socket socket = new Socket(host, port); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            // Check if a session is active on the port
            out.println("CHECK_SESSION");
            response = in.readLine();
            out.println(port);
            response = in.readLine();
            if (response.equals("SESSION_ACTIVE")) {
                // System.out.println("An active session was found on this port.");
                // System.out.println("Do you want to join it? (y/n)");
                // Scanner scanner = new Scanner(System.in);
                // answer = scanner.nextLine();
                //if (answer.equals("y")) {
                    return connect(); // Join the session
                //}
            } else if (response.equals("CONNECTION_REFUSED")) {
                System.out.println("No active session found on this port.");
                return 1;
            }
            return 0;
        } catch (IOException e) {
            System.out.println("Unable to connect to host " + host + " on port " + port);
            return 1;
        }
    }

    private void edit() throws FileNotFoundException, UnsupportedEncodingException {
        // Took first ten from https://www.musicianwave.com/top-music-genres/
        String[] styles_list = {"Pop", "Rock", "Rap", "Jazz", "Blues", "Folk", "Metal", "Country", "Classical"};

        JSONArray dislike_list = new JSONArray();
        JSONArray noopinion_list = new JSONArray();
        JSONArray like_list = new JSONArray();

        Scanner myObj = new Scanner(System.in);  // Create a Scanner object

        System.out.println("Please indicate how much you like these styles (with 'like', 'dislike', 'noopinion'.");
        for (String style : styles_list) {
            System.out.println("How do you like : " + style);

            String userInput = myObj.nextLine();  // Read user input
            while (!userInput.equals("like") && !userInput.equals("dislike") && !userInput.equals("noopinion")) {
                System.out.println("Try again");
                userInput = myObj.nextLine();
            }
            switch (userInput) {
                case "like":
                    like_list.put(style);
                    break;
                case "dislike":
                    dislike_list.put(style);
                    break;
                case "noopinion":
                    noopinion_list.put(style);
                    break;
            }
        }
        JSONObject jo = new JSONObject();
        jo.put("like", like_list);
        jo.put("dislike", dislike_list);
        jo.put("noopinion", noopinion_list);

        try (FileWriter file = new FileWriter("user.json")) {
            file.write(jo.toString(4)); // Indentation de 4 espaces pour rendre le fichier lisible
            file.flush();
            System.out.println("Fichier JSON sauvegardé avec succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
    }

    private int connect() {
        System.out.println("Connecting to host " + host + " on port " + port);
        try (
                Socket socket = new Socket(host, port); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); // BufferedReader to read input from the server
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // PrintWriter to send output to the server
                 BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)); // BufferedReader to read input from the standard input (console)
                ) {
            System.out.println("Connected successfully! On session " + port);
            out.write("CONNECTED\n");
            out.flush();

            message = stdIn.readLine();
            out.println(message);
            response = in.readLine();
            System.out.println("[Server] " + response);
            response = in.readLine();
            System.out.println(response);
            response = in.readLine();
            System.out.println(response);
            response = in.readLine();
            System.out.println(response);

            /*
            while (!socket.isClosed()) {
                // User input
                System.out.println("Choose something to send the server");
                userIn = stdIn.readLine();
                System.out.println(userIn);
                out.write(userIn + "\n");
                out.flush(); // Ensure the message is sent to the server

                // Server response
                serverOut = in.readLine();
                System.out.println("[Server] " + serverOut);

                if (serverOut.contains("Congratulations! You've guessed the number, Bye.")) {
                    socket.close();
                    break;
                }
            }
             */
        } catch (IOException e) {
            System.out.println("Unable to connect to host " + host + " on port " + port);
            e.printStackTrace();
        }
        return 0;
    }

    private void sendAckExpectedMessage(BufferedWriter out, BufferedReader in, String message) throws IOException {
        String serverResponse;
        //System.out.println("Sending :" + message);
        out.write(message + "\n");
        out.flush();
        serverResponse = in.readLine();
        if (!serverResponse.equals("ACK")) {
            System.out.println("WEIRD RESPONSE " + serverResponse + " TO " + message);
        } else {
            //System.out.println("Received : " + serverResponse);
        }
    }

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

    private void sendPreferences(BufferedWriter out, BufferedReader in) throws IOException {
        // this section will later be replaced with cleaner interaction with json
        try (FileReader reader = new FileReader("user.json")) {
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
            String message = "";

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
