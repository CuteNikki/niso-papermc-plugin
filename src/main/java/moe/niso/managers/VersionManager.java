package moe.niso.managers;

import moe.niso.NisoPlugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class VersionManager {
    private static final NisoPlugin plugin = NisoPlugin.getInstance();
    private static final String repositoryOwner = "CuteNikki";

    /**
     * Checks if the current version is newer than the old version.
     *
     * @param oldVersion The old version
     * @param newVersion The new version
     * @return True if the new version is newer, false otherwise
     */
    public static boolean isNewerVersion(String oldVersion, String newVersion) {
        String[] oldParts = oldVersion.substring(1).split("\\.");
        String[] newParts = newVersion.substring(1).split("\\.");

        for (int i = 0; i < Math.max(oldParts.length, newParts.length); i++) {
            int oldPart = i < oldParts.length ? Integer.parseInt(oldParts[i]) : 0;
            int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
            if (newPart > oldPart) {
                return true;
            } else if (newPart < oldPart) {
                return false;
            }
        }
        return false;
    }

    /**
     * Gets the current version of the plugin.
     *
     * @return The current version
     */
    public static String getCurrentVersion() {
        return "v" + plugin.getPluginMeta().getVersion();
    }

    /**
     * Gets the newest version of the plugin from GitHub.
     *
     * @return The newest version
     */
    public static String getNewestVersion() {
        String version = null;
        try {
            final String urlString = "https://api.github.com/repos/" + repositoryOwner + "/" + plugin.getPluginMeta().getName() + "/releases/latest";
            final URI uri = new URI(urlString);
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (connection.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();

                JSONObject json = new JSONObject(content.toString());
                version = json.getString("tag_name");
            } else {
                plugin.getLogger().warning("Updater -> Failed to fetch latest version. Response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Updater -> Error fetching latest version: " + e.getMessage());
        }
        return version;
    }

    /**
     * Downloads the newest update from GitHub.
     *
     * @return True if the update was downloaded successfully, false otherwise
     */
    public static boolean downloadUpdate() {
        plugin.getLogger().info("Updater -> Downloading newest update...");
        final File file = new File("plugins/" + plugin.getPluginMeta().getName() + ".jar");

        if (!file.exists()) {
            plugin.getLogger().warning("Updater -> File not found! Trying to create...");
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().severe("Updater -> Failed to create file.");
                    plugin.getLogger().warning("Disabling plugin...");
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    return false;
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Updater -> Error creating file: " + e.getMessage());
                plugin.getLogger().warning("Disabling plugin...");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return false;
            }
        }

        final String urlString = "https://github.com/" + repositoryOwner + "/" + plugin.getPluginMeta().getName() + "/releases/latest/download/" + plugin.getPluginMeta().getName() + ".jar";

        try {
            final URI uri = new URI(urlString);
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

            connection.connect();

            final FileOutputStream outputStream = new FileOutputStream(file);
            final InputStream inputStream = connection.getInputStream();

            final byte[] buffer = new byte[1024];
            int readBytes;

            while ((readBytes = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readBytes);
            }

            outputStream.close();
            inputStream.close();
            connection.disconnect();
            plugin.getLogger().info("Updater -> Update downloaded successfully!");
            return true;
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().severe("Updater -> Error downloading file: " + e.getMessage());
            plugin.getLogger().warning("Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }
    }
}
