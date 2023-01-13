import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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

}
