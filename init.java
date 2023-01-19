import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class init {

    public static void configFile() {

        try {

            // Check if JSON file exists
            JSONParser parser = new JSONParser();
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

        } catch (FileNotFoundException e) {

            // If file does not exist, create a new file
            JSONObject obj = new JSONObject();
            obj.put("apiKey", "");
            obj.put("mods", new JSONArray());
            obj.put("serverVersion", "");
            try {

                FileWriter file = new FileWriter("mcmodmanager.json");
                file.write(obj.toJSONString());
                file.close();

                System.out.println("Config file not found. A new one has been generated.");

            } catch (Exception ex) {

                e.printStackTrace();
                System.out.println("Config file not found. Error creating a new one.");

            }

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

    }

    /**
     * modFolder method checks if a folder called "mods" exists in the current
     * directory.
     * If the folder exists, the method does nothing.
     * If the folder does not exist, the method will notify the user that the folder
     * is missing and exit the program.
     */
    public static void modFolder() {
        File modFolder = new File("./mods");
        if (!modFolder.exists()) {
            System.out.println(
                    "Can not find the mods folder. Make sure you are running this program in the same directory as the mods folder for your server.");
            System.exit(0);
        }
    }

    public static String apiKey() {

        String apiKey = "";

        try {

            // Open config file
            JSONParser parser = new JSONParser();
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

            // Check if current api entry is blank
            apiKey = (String) jsonData.get("apiKey");
            if (apiKey.equals("")) {

                System.out.println(
                        "No API key found.\nSet an API key by using the -k flag.\nSee usage (-h) for more information.");
                System.exit(0);

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        return apiKey;

    }

    public static void fileCheck() {

        JSONParser parser = new JSONParser();
        try {

            // Open JSON file
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

            // Get mods array
            JSONArray modsArray = (JSONArray) jsonData.get("mods");

            // For each mod...
            for (int i = 0; i < modsArray.size(); i++) {

                // Get mod info
                JSONObject mod = (JSONObject) modsArray.get(i);
                String name = (String) mod.get("name");
                String fileName = (String) mod.get("fileName");
                String modID = (String) mod.get("modID");
                String downloadLink = (String) mod.get("downloadLink");

                // Check if the mod file exists in the mod folder
                File modFile = new File("./mods/" + fileName);
                if (!modFile.exists()) {

                    // If the mod file doesnt exist, ask the user how they want to proceed
                    System.out.println("ERROR: The " + name + " mod file could not be found in the mods folder.");
                    Scanner scanner = new Scanner(System.in);
                    System.out.println(
                            "Do you want to redownload the mod file or remove it from the mod list? Type 'add' or 'remove':");
                    String response = scanner.nextLine();

                    if (response.equals("add")) {

                        // Re download the mod
                        ModManager.downloadFile(downloadLink, "./mods/" + fileName);
                        System.out.println("Mod has been added");

                    } else if (response.equals("remove")) {

                        // Remove the mod from the mod list
                        ModManager.removeMod(modID);
                        System.out.println("Mod has been removed");

                    } else {

                        System.out.println("Invalid input");
                        System.exit(0);

                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
