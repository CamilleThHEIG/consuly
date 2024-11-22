package ch.heigvd.dai;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.Callable;
import java.util.Scanner;
import java.io.IOException;
import picocli.CommandLine;
import org.json.*;

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
            names = {"-e", "--e"},
            description = "Include this if you want to edit your preferences json file.",
            defaultValue = "False")
    protected boolean edit;

    protected String message = "Hello, server! I'm the client. 🤖";

    @Override
    public Integer call() throws FileNotFoundException, UnsupportedEncodingException {
        if (edit){
            edit();
            return 0;
        } else {
            return connect();
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
        for (String style : styles_list){
            System.out.println("How do you like : " + style);

            String userInput = myObj.nextLine();  // Read user input
            while (!userInput.equals("like")  && !userInput.equals("dislike") && !userInput.equals("noopinion")) {
                System.out.println("Try again");
                userInput = myObj.nextLine();
            }
            switch (userInput){
                case "like": like_list.put(style); break;
                case "dislike": dislike_list.put(style); break;
                case "noopinion": noopinion_list.put(style); break;
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

    private int connect(){
        System.out.println("Connecting to host " + host + " on port " + port);
        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); // BufferedReader to read input from the server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // PrintWriter to send output to the server
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)); // BufferedReader to read input from the standard input (console)
        ){
            System.out.println("Connected successfully!");

            String serverOut, userIn, welcomeMessage;

            // Server communication
            welcomeMessage = in.readLine();
            System.out.println(welcomeMessage);

            while (!socket.isClosed()) {
                // User input
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
        } catch (IOException e) {
            System.out.println("Unable to connect to host " + host + " on port " + port);
            e.printStackTrace();
        }
        return 0;
    }
}
