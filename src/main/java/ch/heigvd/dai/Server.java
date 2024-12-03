package ch.heigvd.dai;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.Callable;

import org.json.JSONArray;
import picocli.CommandLine;
import ch.heigvd.dai.util.JSON;
import ch.heigvd.dai.util.Group;


@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {
    private static final int NUMBER_OF_THREADS = 5;

    private static int lastClientIdUsed = 2;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;


    @Override
    public Integer call() {
        System.out.println("Server is listening on port " + port);
        try (
                ServerSocket serverSocket = new ServerSocket(port); ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // Accept the client connection
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                executor.submit(new ClientHandler(socket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
        return 0;
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private static final int LOWERBOUND = 1, UPPERBOUND = 100;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }


        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
            ){
                // TODO REMOVE THIS WHEN TESTING IS OVER
                testMakeFinalList();
                /*
                String userIn;
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                while ((userIn = in.readLine()) != null) {
                    if (userIn.equals("READY_SEND")){
                        receiveList(in, out);
                    }
                }
                out.newLine();
                out.flush(); // Ensure the message is sent to the client

                 */

            } catch (IOException e) {
                System.out.println("Client handling exception: " + e.getMessage());
            }
        }

        /**
         * What to do to receive a list from a user
         * @param in
         * @param out
         * @throws IOException
         */
        public void receiveList(BufferedReader in, BufferedWriter out) throws IOException {
            System.out.println("In receiveList");
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
            /*
            System.out.println("Like");
            for (int j = 0 ; j < like.length() ; ++j){
                System.out.println(like.getString(j));
            }

            System.out.println("\nDisike");
            for (int j = 0 ; j < dislike.length() ; ++j){
                System.out.println(dislike.getString(j));
            }

            System.out.println("\nNeutral");
            for (int j = 0 ; j < noopinion.length() ; ++j){
                System.out.println(dislike.getString(j));
            }

            // write the result
            JSON json = new JSON(lastClientIdUsed);
            json.writeFileWithLists(like, dislike, noopinion);
             */
        }

        /**
         * When group admin calls this function, server will generate a group preference list, based on the members of the group
         * @param group : group to create the list from
         * @param in
         * @param out
         * @throws IOException
         */
        public void makeFinalList(Group group, BufferedReader in, BufferedWriter out) throws IOException {
            LinkedList<Integer> membersList = group.getMembersIdList();
            System.out.println("Here are the members");
            for (Integer memberId : membersList) {

                // ask their list
                // receiveList(null, null);

                System.out.println("Ping" + memberId);
            }
        }

        public void testMakeFinalList() throws IOException {
            Group testGroup = new Group(0, 17);
            testGroup.addMember(8);
            testGroup.addMember(9);
            testGroup.addMember(10);

            makeFinalList(testGroup, null, null);
        }
    }
}