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

    public static String apiKey() {

        String apiKey = "";

        try {

            JSONParser parser = new JSONParser();
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

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
