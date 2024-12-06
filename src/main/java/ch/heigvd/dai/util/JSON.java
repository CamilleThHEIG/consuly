package ch.heigvd.dai.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Scanner;

public class JSON {
    private final int id;
    private final String[] styles = {"Pop", "Rock", "Metal", "Classical"};
    private String directory ="userfiles/", filePath;

    public enum Taste {
        like, dislike, noopinion;
    };

    private final JSONObject json;

    public JSON(int id){
        this.id = id;
        filePath = directory + "user0.json";
        json = new JSONObject();
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

    /**
     * Crée un fichier de préférence en demandant les préférences via la ligne de commande
     */
    public void createByAsking() throws IOException {
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
            switch (userInput) {
                case "like" : like_list.put(style); break;
                case "dislike": dislike_list.put(style); break;
                case "noopinion": noopinion_list.put(style); break;
            }
        }
        json.put(Taste.like.name(), like_list);
        json.put(Taste.dislike.name(), dislike_list);
        json.put(Taste.noopinion.name(), noopinion_list);

        Path userfilesPath = Paths.get(directory);
        if (!Files.exists(userfilesPath)) {
            Files.createDirectories(userfilesPath);
        }

        try (FileWriter fileOut = new FileWriter(filePath)) {
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

    /**
     * Pour créer une liste de préférence personnelle à partir d'un fichier
     * @param lList la liste des liked
     * @param dList la liste des disliked
     * @param nList la liste des neutral
     */
    public void writeFileWithLists(JSONArray lList, JSONArray dList, JSONArray nList){
        json.put(Taste.like.name(), lList);
        json.put(Taste.dislike.name(), dList);
        json.put(Taste.noopinion.name(), nList);
        try (FileWriter fileOut = new FileWriter(filePath)) {
            fileOut.write(json.toString(4) + "\n"); // Indentation de 4 espaces pour rendre le fichier lisible
            fileOut.flush();
            System.out.println("Flushed");
            System.out.println("Fichier JSON pour id " + id + " sauvegardé avec succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
        System.out.println("End of writeFileWithLists.");
    }

    /**
     return le Taste d'un style donné dans les tableaux spécifiés
     */
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
        String list;
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

    /**
     * What does this do ?
     */
    public void showTastes() {
        for(int i = 0; i < Taste.values().length; ++i)
            System.out.print((Taste.values()[i])+ " ");
    }


    /**
     * Loads a json file, and return the content as a JSONObject
     * @param path path to read the file from
     * @return
     */
    public JSONObject loadJsonFile(String path){
        try (FileReader reader = new FileReader(path)) {
            // Lire le contenu du fichier dans une chaîne
            StringBuilder content = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                content.append((char) i);
            }
            // Convertir la chaîne en objet JSON
            return new JSONObject(content.toString());

        } catch (FileNotFoundException fnf){
            System.out.println("Couldn't file suitable file at " + path);
        } catch (IOException io){
            System.out.println("IOException");
        }
        return null;
    }
}