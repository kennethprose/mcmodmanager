import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class ModManager {

    public static String apiKey;

    public static void printUsage() {

        String usage = """
                usage: java mcmodmanager.java [OPTIONS]

                OPTIONS:
                    -h,         --help                  print usage information
                    -c,         --check-updates         check for any available updates for all mods
                    -k API_KEY, --api-key API_KEY       Sets your CurseForge API key
                    -a ModID,   --add-mod ModID         Adds a new mod to mod list and installs it
                    """;
        System.out.println(usage);
        System.exit(0);

    }

    /**
     * This function is used to set the API key for curseforge.
     *
     * @param key The API key to be set.
     */
    public static void setAPIKey(String key) {

        JSONParser parser = new JSONParser();

        try {

            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

            // Add or overwrite the users key to the JSON data
            jsonData.put("apiKey", key);

            // Write the updated data back to the JSON file
            FileWriter file = new FileWriter("mcmodmanager.json");
            file.write(jsonData.toJSONString());
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This function makes an API call to the CurseForge API and returns the
     * response as a JSONObject.
     *
     * @param endpoint The endpoint of the API call. This should not include the
     *                 base URL or any query parameters.
     * @param params   The query parameters for the API call. This should not
     *                 include the endpoint or the base URL.
     * @return A JSONObject representing the API response.
     * @throws Exception if an error occurs during the API call or if the response
     *                   code is not 200.
     */
    public static JSONObject curseForgeAPICall(String endpoint, String params) throws Exception {

        // Create a new HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Create a new HttpRequest with the API endpoint URL and the x-api-key header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.curseforge.com" + endpoint + params))
                .header("x-api-key", apiKey)
                .GET()
                .build();

        // Send the request and retrieve the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check if the response code is not 200 and throw an exception if it's not
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unexpected response code: " + response.statusCode());
        }

        // Get the response body as a string
        String responseBody = response.body();

        // Create a new JSONParser
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = null;

        // Try to parse the response body as a JSON object
        try {
            jsonResponse = (JSONObject) parser.parse(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return the JSON object
        return jsonResponse;
    }

    /**
     * checkUpdates() function checks for updates of all mods in mcmodmanager.json
     * by comparing the current version of each mod with the newest version
     * available on CurseForge.
     *
     * @throws Exception if the mcmodmanager.json file is not found or data is
     *                   corrupted
     */
    public static void checkUpdates() {

        JSONParser parser = new JSONParser();

        try {

            // Parse mcmodmanager.json file and get the "mods" array
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));
            JSONArray modsArray = (JSONArray) jsonData.get("mods");

            // Iterate through the mods array
            for (int i = 0; i < modsArray.size(); i++) {

                // Get the mod information from the config file (name, curseforgeID,
                // currentVersion)
                JSONObject mod = (JSONObject) modsArray.get(i);
                String name = (String) mod.get("name");
                String curseforgeID = (String) mod.get("curseforgeID");
                String currentVersion = (String) mod.get("currentVersion");

                // Get the latest version of the mod from CurseForge API
                JSONObject getModFiles = curseForgeAPICall("/v1/mods/" + curseforgeID + "/files", "");
                JSONArray dataArray = (JSONArray) getModFiles.get("data");
                JSONObject firstDataObject = (JSONObject) dataArray.get(0);
                String newestGameVersion = (String) ((JSONArray) firstDataObject.get("gameVersions")).get(0);

                // Compare the current version of the mod with the newest version available
                if (newestGameVersion.compareTo(currentVersion) > 0) {

                    System.out.println("A new version of " + name + " is available: " + currentVersion + " -> "
                            + newestGameVersion);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Data file not found or data corrupted.");
        }

    }

    /**
     * Method to add a new entry in the "mods" array of the JSON file
     * 'mcmodmanager.json'.
     * 
     * @param modID the curseforge id of the mod to add.
     * 
     * @throws Exception if there is any problem with the file reading or parsing.
     */
    public static void addMod(String modID) {

        // Initialize JSON parser to parse JSON file
        JSONParser parser = new JSONParser();

        try {

            // Read JSON file and parse its contents into a JSONObject
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

            // Get the 'mods' array and 'currentServerVersion' from the JSON object
            JSONArray modsArray = (JSONArray) jsonData.get("mods");
            String currentServerVersion = (String) jsonData.get("currentServerVersion");

            // Create a new JSON object for the new mod
            JSONObject newMod = new JSONObject();
            newMod.put("curseforgeID", modID);
            newMod.put("name", "New Mod");
            newMod.put("currentVersion", currentServerVersion);

            // Add the new mod object to the 'mods' array
            modsArray.add(newMod);

            // Write the updated JSON object back to the file
            FileWriter file = new FileWriter("mcmodmanager.json");
            file.write(jsonData.toJSONString());
            file.close();

        } catch (Exception e) {

            // Handle any exceptions that may occur during the reading, parsing, or writing
            // process
            e.printStackTrace();
            System.out.println("ERROR: Data file not found or data corrupted.");

        }

        // TODO: Confirm with the user and download the file

    }

    public static void main(String[] args) {

        if (args.length > 0) {

            switch (args[0]) {

                case "-h", "--help":
                    printUsage();
                    break;

                case "-c", "--check-updates":
                    init.configFile();
                    init.apiKey();

                    checkUpdates();
                    break;

                case "-a", "--add-mod":
                    init.configFile();
                    init.apiKey();

                    addMod(args[1]);
                    break;

                case "-k", "--api-key":
                    init.configFile();

                    setAPIKey(args[1]);
                    break;

                default:
                    break;

            }

        }

    }
}