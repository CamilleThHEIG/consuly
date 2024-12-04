package ch.heigvd.dai;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.Callable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
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


        public void handleReady(){
            // called when user writes READY

            // if GROUP du user qui a call. makeFinalList = True
            // -> receiveList
            // else sleep and après 10 sec send RELEASE READY
            //

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
            JSON jsonInteractor = new JSON();

            LinkedList<JSONArray> allDislikes = new LinkedList<>();
            LinkedList<String> finalList = new LinkedList<>();

            // loop a first time to load every json content
            for (Integer memberId : membersList) {
                // ask their list

                //TODO WHEN TESTING OVER, REMOVE THIS LINE WITH COMMENTED ONE
                String filePath = "serverfiles/test" + memberId + ".json";
                // String filePath = "serverfiles/user" + memberId + ".json";

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

        public void testMakeFinalList() throws IOException {
            Group testGroup = new Group(0, 9);
            testGroup.addMember(10);
            testGroup.addMember(11);

            // testGroup.notifiyListReceived(10);

            makeFinalList(testGroup, null, null);
        }
    }
}