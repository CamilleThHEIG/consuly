package ch.heigvd.dai.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class JSON {
    private final String username;

    private final String[] styles = {"Pop", "Rock", "Metal", "Classical"};

    public enum Taste {
        like, dislike, noopinion;
    };

    private JSONObject json;

    public JSON() {
        this("unknown");
    }

    public JSON(String username) {
        this.username = username;
        json = new JSONObject();
        if (!new java.io.File("user" + username + ".json").exists()) {
            create();
        } else {
            try (FileReader fileIn = new FileReader("user" + username + ".json")) {
                json = new JSONObject(new Scanner(fileIn).useDelimiter("\\A").next());
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du fichier JSON : " + e.getMessage());
            }
        }
    }

    public LinkedList<String> getTaste(Taste taste) {
        LinkedList<String> likes = new LinkedList<>();
        JSONArray jsonArr = new JSONArray();
        switch(taste) {
            case like -> {
                if (json.has("like")) {
                    jsonArr = json.getJSONArray("like");
                }
            }
            case dislike -> {
                if (json.has("dislike")) {
                    jsonArr = json.getJSONArray("dislike");
                }
            }
            case noopinion -> {
                if (json.has("noopinion")) {
                    jsonArr = json.getJSONArray("noopinion");
                }
            }
        }

        for (int i = 0; i < jsonArr.length(); ++i) {
            likes.add(jsonArr.getString(i) + " ");
        }
        return likes;
    }

    public void create() {
        JSONArray dislike_list = new JSONArray(), noopinion_list = new JSONArray(), like_list = new JSONArray();
        Scanner stdIn = new Scanner(System.in);
        System.out.println("Please indicate how much you like these styles (with 'like', 'dislike', 'noopinion').");
        for (String style : styles){
            System.out.println("How do you like : " + style);

            String userInput = stdIn.nextLine();  // Read user input
            while (!userInput.equals(Taste.like.name())  && !userInput.equals(Taste.dislike.name()) && !userInput.equals(Taste.noopinion.name())) {
                System.out.print("Accepted opinions are : ");
                showTastes();
                System.out.println();

                userInput = stdIn.nextLine();
            }
            switch (userInput){
                case "like" : like_list.put(style); break;
                case "dislike": dislike_list.put(style); break;
                case "noopinion": noopinion_list.put(style); break;
            }
        }
        json.put(Taste.like.name(), like_list);
        json.put(Taste.dislike.name(), dislike_list);
        json.put(Taste.noopinion.name(), noopinion_list);

        try (FileWriter fileOut = new FileWriter("user.json")) {
            fileOut.write(json.toString(4)); // Indentation de 4 espaces pour rendre le fichier lisible
            fileOut.flush();
            System.out.println("Fichier JSON sauvegardé avec succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
    }

    public void edit() {
        Scanner stdIn = new Scanner(System.in);
        String userInput;
        JSONArray dislike_list = json.getJSONArray("dislike"), noopinion_list = json.getJSONArray("noopinion"), like_list = json.getJSONArray("like");

        System.out.println("Wich one do you want to change ?");
        System.out.println("\tchoose the selection below (between 0 and " + (styles.length - 1) + ')');
        for(int i = 0; i < styles.length; ++i) {
            System.out.println("[" + i + "] " + styles[i] + "\t" + styleTaste(dislike_list, like_list, noopinion_list, styles[i])); // + like | dislike | noopinion
        }

        // User Intput
        userInput = stdIn.nextLine();  // Read user input
        System.out.println("Please indicate how much you like this styles (with 'like', 'dislike', 'noopinion').");
        userInput = stdIn.nextLine();  // Read user input

        System.out.println("Your liking has been changed.");
    }

    private Taste styleTaste(JSONArray dList, JSONArray lList, JSONArray nList, String style) {
        Taste taste = Taste.like;
        for (int i = 0; i < dList.length(); ++i) {
            if (dList.getString(i).equals(style)) {
                taste = Taste.dislike;
                break;
            }
        }

        if (taste == Taste.like) {
            for (int i = 0; i < lList.length(); ++i) {
                if (lList.getString(i).equals(style)) {
                    taste = Taste.like;
                    break;
                }
            }
        }

        if (taste == Taste.like) {
            for (int i = 0; i < nList.length(); ++i) {
                if (nList.getString(i).equals(style)) {
                    taste = Taste.noopinion;
                    break;
                }
            }
        }
        return taste;
    }

    @Override
    public String toString() {
        String list = null;
        LinkedList<String> arrTaste = getTaste(Taste.like);
        list = "Like : ";
        for (int i = 0; i < arrTaste.size(); ++i) {
            list += arrTaste.get(i) + " ";
        }

        arrTaste = getTaste(Taste.dislike);
        list += "\nDislike : ";
        for (int i = 0; i < arrTaste.size(); ++i) {
            list += arrTaste.get(i) + " ";
        }

        arrTaste = getTaste(Taste.noopinion);
        list += "\nNo opinion : ";
        for (int i = 0; i < arrTaste.size(); ++i) {
            list += arrTaste.get(i) + " ";
        }
        return list;
    }

    public void showTastes() {
        for(int i = 0; i < Taste.values().length; ++i)
            System.out.print((Taste.values()[i])+ " ");
    }
}