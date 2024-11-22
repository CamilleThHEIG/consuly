package ch.heigvd.dai;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.Scanner;

import java.io.FileWriter;
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
        System.out.println("Connecting to " + host + ":" + port + "...");
        try (DatagramSocket socket = new DatagramSocket()) {
            // Get the server address
            InetAddress serverAddress = InetAddress.getByName(host);

            // Transform the message into a byte array - always specify the encoding
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

            // Create a packet with the message, the server address and the port
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, port);

            // Send the packet
            socket.send(packet);

            System.out.println("[Client] Request sent: " + message);

            // Create a buffer for the incoming response
            byte[] responseBuffer = new byte[1024];

            // Create a packet for the incoming response
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            // Receive the packet - this is a blocking call
            socket.receive(responsePacket);

            // Transform the message into a string
            String response
                    = new String(
                    responsePacket.getData(),
                    responsePacket.getOffset(),
                    responsePacket.getLength(),
                    StandardCharsets.UTF_8);

            System.out.println("[Client] Received response: " + response);
        } catch (Exception e) {
            System.err.println("[Client] An error occurred: " + e.getMessage());
        }
        return 0;
    }
}
