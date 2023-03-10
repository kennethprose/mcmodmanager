import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
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
                    -a ModID,   --add-mod ModID         Adds a new mod to mod list and installs it
                    -c VERSION, --check-updates VERSION check for any available updates for all mods
                    -h,         --help                  print usage information
                    -k API_KEY, --api-key API_KEY       Sets your CurseForge API key
                    -r ModID,   --remove-mod ModID      Removes a mod from the mod list and uninstalls it
                    -s VERSION, --set-version           Sets which server version you are running
                    -u,         --update                Updates all mods
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
     * This function is used to set the server version that you are running
     *
     * @param key The server verison to be set.
     */
    public static void setServerVersion(String version) {

        JSONParser parser = new JSONParser();

        try {

            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));

            // Add or overwrite the version in the JSON data
            jsonData.put("serverVersion", version);

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

    public static void saveUpdateInfo(String modID, String fileName, String downloadLink, String newVersion,
            long fileID) {

        JSONParser parser = new JSONParser();

        try {

            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));
            JSONArray modsArray = (JSONArray) jsonData.get("mods");

            for (int i = 0; i < modsArray.size(); i++) {

                JSONObject mod = (JSONObject) modsArray.get(i);
                String currModID = (String) mod.get("modID");

                if (currModID.equals(modID)) {

                    // Create a new JSON object for the update info
                    JSONObject updateInfo = new JSONObject();
                    updateInfo.put("newFileName", fileName);
                    updateInfo.put("newFileID", fileID);
                    updateInfo.put("newDownloadLink", downloadLink);
                    updateInfo.put("newVersion", newVersion);

                    // Add new update info object to mod info
                    mod.put("update", updateInfo);

                    // Write the updated JSON object back to the file
                    FileWriter file = new FileWriter("mcmodmanager.json");
                    file.write(jsonData.toJSONString());
                    file.close();

                    return;

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * checkUpdates() function checks for updates of all mods in mcmodmanager.json
     *
     * @throws Exception if the mcmodmanager.json file is not found or data is
     *                   corrupted
     */
    public static void checkUpdates(String desiredVersion) {

        JSONParser parser = new JSONParser();

        try {

            // Parse mcmodmanager.json file and get the "mods" array + server version
            JSONObject jsonData = (JSONObject) parser.parse(new FileReader("mcmodmanager.json"));
            JSONArray modsArray = (JSONArray) jsonData.get("mods");
            String serverVersion = (String) jsonData.get("serverVersion");

            if (serverVersion.equals(desiredVersion)) {

                for (int i = 0; i < modsArray.size(); i++) {

                    // Get the mod information from the config file (name, modID, fileID)
                    JSONObject mod = (JSONObject) modsArray.get(i);
                    String name = (String) mod.get("name");
                    String modID = (String) mod.get("modID");
                    long fileID = (long) mod.get("fileID");

                    // Get the latest version of the mod from CurseForge API
                    JSONObject getModFiles = curseForgeAPICall("/v1/mods/" + modID + "/files",
                            "?gameVersion=" + serverVersion);
                    JSONObject pagination = (JSONObject) getModFiles.get("pagination");

                    // If there are no results in response JSON, we know we cannot update
                    if ((long) pagination.get("resultCount") == 0) {
                        System.out.println("There are no versions of " + name + " for " + serverVersion);
                        continue;
                    }

                    // Get the ID of the newest file
                    JSONArray dataArray = (JSONArray) getModFiles.get("data");
                    JSONObject firstMod = (JSONObject) dataArray.get(0);
                    long newfileID = (long) firstMod.get("id");

                    // Compare file IDs to see if a newer file is present
                    if (newfileID > fileID) {

                        System.out.println(name + " - Updates available");

                        // Get rest of new file info
                        String newFileName = (String) firstMod.get("fileName");
                        String newDownloadLink = (String) firstMod.get("downloadUrl");

                        // Save update info
                        saveUpdateInfo(modID, newFileName, newDownloadLink, serverVersion, newfileID);

                    } else {
                        System.out.println(name + " - No updates available");
                    }

                }

            } else {

                for (int i = 0; i < modsArray.size(); i++) {

                    // Get the mod information from the config file (name, modID, fileID)
                    JSONObject mod = (JSONObject) modsArray.get(i);
                    String name = (String) mod.get("name");
                    String modID = (String) mod.get("modID");

                    // Get the latest version of the mod from CurseForge API
                    JSONObject getModFiles = curseForgeAPICall("/v1/mods/" + modID + "/files",
                            "?gameVersion=" + desiredVersion);
                    JSONObject pagination = (JSONObject) getModFiles.get("pagination");

                    // If there are no results in response JSON, we know we cannot update
                    if ((long) pagination.get("resultCount") == 0) {
                        System.out.println("There are no versions of " + name + " for " + desiredVersion);
                        continue;
                    }

                    // If there were results, then we know we can update
                    System.out.println(name + " is ready to update to " + desiredVersion);

                    // Get new file info
                    JSONArray dataArray = (JSONArray) getModFiles.get("data");
                    JSONObject firstMod = (JSONObject) dataArray.get(0);
                    long newfileID = (long) firstMod.get("id");
                    String newFileName = (String) firstMod.get("fileName");
                    String newDownloadLink = (String) firstMod.get("downloadUrl");

                    // Save update info
                    saveUpdateInfo(modID, newFileName, newDownloadLink, desiredVersion, newfileID);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
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
            newMod.put("downloadLink", downloadLink);

            // Add the new mod object to the 'mods' array
            modsArray.add(newMod);

            // Write the updated JSON object back to the file
            FileWriter file = new FileWriter("mcmodmanager.json");
            file.write(jsonData.toJSONString());
            file.close();

            // Download the file to the mods folder
            downloadFile(downloadLink, "./mods/" + fileName);

        } catch (Exception e) {

            // Handle any exceptions that may occur during the reading, parsing, or writing
            // process
            e.printStackTrace();

        }

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

                // Get the file name and delete the file from the mods folder
                String fileName = (String) currentMod.get("fileName");
                File modFile = new File("./mods/" + fileName);
                modFile.delete();

                // remove it from config file
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
                    init.modFolder();
                    init.fileCheck();

                    checkUpdates(args[1]);
                    break;

                case "-a", "--add-mod":
                    init.configFile();
                    apiKey = init.apiKey();
                    init.modFolder();
                    init.fileCheck();

                    addMod(args[1]);
                    break;

                case "-r", "--remove-mod":
                    init.configFile();
                    apiKey = init.apiKey();
                    init.modFolder();
                    init.fileCheck();

                    removeMod(args[1]);
                    break;

                case "-k", "--api-key":
                    init.configFile();

                    setAPIKey(args[1]);
                    break;

                case "-s", "--set-version":
                    init.configFile();

                    setServerVersion(args[1]);
                    break;

                case "-u", "--update":
                    init.configFile();
                    apiKey = init.apiKey();
                    init.modFolder();
                    init.fileCheck();

                    break;

                default:
                    break;

            }

        }

    }
}