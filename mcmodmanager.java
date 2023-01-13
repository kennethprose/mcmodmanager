import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
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
                    -c VERSION, --check-updates VERSION check for any available updates for all mods
                    -k API_KEY, --api-key API_KEY       Sets your CurseForge API key
                    -a ModID,   --add-mod ModID         Adds a new mod to mod list and installs it
                    -r ModID,   --remove-mod ModID      Removes a mod from the mod list and uninstalls it
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
     * Function to download a file from a given URL and save it to a specified
     * location
     *
     * @param urlStr the URL of the file to be downloaded
     * @param file   the location to save the downloaded file
     * @throws IOException
     */
    public static void downloadFile(String urlStr, String file) throws IOException {

        // Create a URL object from the given URL string
        URL url = new URL(urlStr);

        // Open an InputStream to read data from the URL
        InputStream is = url.openStream();

        // Create a FileOutputStream to save the file to the specified location
        OutputStream os = new FileOutputStream(file);

        // Create a byte array to store read data
        byte[] b = new byte[2048];
        int length;

        // Read data from the InputStream and write it to the OutputStream
        // Repeat until all data has been read
        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        // Close the InputStream and OutputStream
        is.close();
        os.close();
    }

    /**
     * checkUpdates() function checks for updates of all mods in mcmodmanager.json
     * by comparing the current version of each mod with the newest version
     * available on CurseForge.
     *
     * @throws Exception if the mcmodmanager.json file is not found or data is
     *                   corrupted
     */
    public static void checkUpdates(String desiredVersion) {

        JSONParser parser = new JSONParser();

        try {

            // Parse mcmodmanager.json file and get the "mods" array
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));
            JSONArray modsArray = (JSONArray) jsonData.get("mods");

            // Iterate through the mods array
            for (int i = 0; i < modsArray.size(); i++) {

                // Get the mod information from the config file (name, modID,
                // currentVersion)
                JSONObject mod = (JSONObject) modsArray.get(i);
                String name = (String) mod.get("name");
                String modID = (String) mod.get("modID");
                String currentVersion = (String) mod.get("currentVersion");

                // Get the latest version of the mod from CurseForge API
                JSONObject getModFiles = curseForgeAPICall("/v1/mods/" + modID + "/files", "");
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

            // Get the 'mods' array and 'serverVersion' from the JSON object
            JSONArray modsArray = (JSONArray) jsonData.get("mods");
            String serverVersion = (String) jsonData.get("serverVersion");

            // Get mod info from curseforge
            JSONObject modInfo = (JSONObject) curseForgeAPICall("/v1/mods/" + modID, "").get("data");
            String modName = (String) modInfo.get("name");

            // Get most recent file for current server version of this mod
            JSONObject modFiles = (JSONObject) curseForgeAPICall("/v1/mods/" + modID + "/files",
                    "?gameVersion=" + serverVersion);
            JSONObject resultInfo = (JSONObject) modFiles.get("pagination");

            // If no mods are found for the current server version, notify user and exit
            if ((long) resultInfo.get("resultCount") == 0) {
                System.out.println("A version of " + modName + " could not be found for Minecraft " + serverVersion);
                return;
            }

            // Now that we know there is at least one released file for this mod on this
            // version, we get its info
            JSONArray fileArray = (JSONArray) modFiles.get("data");
            JSONObject mostRecentVersion = (JSONObject) fileArray.get(0);
            long fileID = (long) mostRecentVersion.get("id");
            String fileName = (String) mostRecentVersion.get("fileName");
            String downloadLink = (String) mostRecentVersion.get("downloadUrl");

            // Create a new JSON object for the new mod
            JSONObject newMod = new JSONObject();
            newMod.put("modID", modID);
            newMod.put("name", modName);
            newMod.put("currentVersion", serverVersion);
            newMod.put("fileName", fileName);
            newMod.put("fileID", fileID);

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

        }

        // TODO: Confirm with the user and download the file

    }

    public static void removeMod(String modID) {

        // Initialize JSON parser to parse JSON file
        JSONParser parser = new JSONParser();

        JSONObject jsonData = null;

        try {

            // Read JSON file and parse its contents into a JSONObject
            jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Get the 'mods' array and 'serverVersion' from the JSON object
        JSONArray modsArray = (JSONArray) jsonData.get("mods");

        // For each mod...
        for (int i = 0; i < modsArray.size(); i++) {

            // Grab its mod ID
            JSONObject currentMod = (JSONObject) modsArray.get(i);
            String currentModID = (String) currentMod.get("modID");

            // And if it matches the one we are looking for...
            if (currentModID.equals(modID)) {

                // remove it
                modsArray.remove(i);

                // Write the updated JSON object back to the file
                FileWriter file;
                try {

                    file = new FileWriter("mcmodmanager.json");
                    file.write(jsonData.toJSONString());
                    file.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Update user
                System.out.println("The mod has successfully been removed");
                return;

            }

        }

        // If all mods were searched without a match
        System.out.println("The specified mod could not be found");

    }

    public static void main(String[] args) {

        if (args.length > 0) {

            switch (args[0]) {

                case "-h", "--help":
                    printUsage();
                    break;

                case "-c", "--check-updates":
                    init.configFile();
                    apiKey = init.apiKey();

                    checkUpdates(args[1]);
                    break;

                case "-a", "--add-mod":
                    init.configFile();
                    apiKey = init.apiKey();

                    addMod(args[1]);
                    break;

                case "-r", "--remove-mod":
                    init.configFile();

                    removeMod(args[1]);
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