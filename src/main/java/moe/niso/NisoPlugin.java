package moe.niso;

import moe.niso.commands.HomeCommand;
import moe.niso.commands.WarpCommand;
import moe.niso.listeners.ChatListener;
import moe.niso.listeners.JoinListener;
import moe.niso.listeners.LeaveListener;
import moe.niso.listeners.MotdListener;
import moe.niso.managers.ConfigManager;
import moe.niso.managers.DatabaseManager;
import moe.niso.managers.TablistManager;
import moe.niso.managers.VersionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class NisoPlugin extends JavaPlugin {
    private static NisoPlugin instance;
    public String logPrefixDatabase = "Database -> ";
    public String logPrefixManager = "Manager -> ";
    public String logPrefixUpdater = "Updater -> ";
    private DatabaseManager databaseManager;
    private TablistManager tablistManager;

    /**
     * Get the plugin instance.
     *
     * @return Plugin instance
     */
    public static NisoPlugin getInstance() {
        return instance;
    }

    /**
     * Called when the plugin is loading.
     */
    @Override
    public void onLoad() {
        getLogger().info(logPrefixManager + "Plugin is loading...");
    }

    /**
     * Called when the plugin is enabled / the server is starting up.
     */
    @Override
    public void onEnable() {
        // Set the instance variable to this instance
        instance = this;

        // Create default configuration file if it doesn't exist and check for missing/invalid values
        ConfigManager.checkDefaultConfig();

        // Without these the plugin will not work properly
        initialiseDatabase();
        initialisePlaceholderAPI();

        // Checking for a new version
        updateCheck();

        // Register event listeners and commands
        registerEvents();
        registerCommands();

        // Start the tablist manager
        tablistManager = new TablistManager();

        // Notify that the plugin is enabled
        getLogger().info(logPrefixManager + "Plugin is enabled!");
    }

    /**
     * Called when the plugin is disabled / the server is shutting down.
     */
    @Override
    public void onDisable() {
        // Close the database connection pool
        if (databaseManager != null) {
            databaseManager.closeDatabase();
        }

        // Stop the tablist manager
        if (tablistManager != null) {
            tablistManager.stop();
        }

        getLogger().info(logPrefixManager + "Plugin is disabled!");
    }

    /**
     * Register all commands.
     */
    private void registerCommands() {
        getCmd("home").setExecutor(new HomeCommand());
        getCmd("warp").setExecutor(new WarpCommand());

        getLogger().info(logPrefixManager + "Commands registered!");
    }

    /**
     * Register all event listeners.
     */
    private void registerEvents() {
        PluginManager manager = getServer().getPluginManager();

        manager.registerEvents(new JoinListener(), this);
        manager.registerEvents(new LeaveListener(), this);
        manager.registerEvents(new ChatListener(), this);
        manager.registerEvents(new MotdListener(), this);

        getLogger().info(logPrefixManager + "Events registered!");
    }

    /**
     * Initialise the database connection and create tables if they don't exist.
     */
    private void initialiseDatabase() {
        // Check if the PostgreSQL driver is loaded and disable the plugin if it's not found
        try {
            Class.forName("org.postgresql.Driver");
            getLogger().info(logPrefixDatabase + "PostgreSQL Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            getLogger().severe(logPrefixDatabase + "PostgreSQL Driver not found: " + e.getMessage());
            getLogger().severe(logPrefixDatabase + "Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize and set up database manager
        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();

        // Create tables if they don't exist
        databaseManager.createHomesTable();
        databaseManager.createWarpsTable();
    }

    /**
     * Initialise PlaceholderAPI and disable the plugin if it's not found.
     */
    private void initialisePlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info(logPrefixManager + "PlaceholderAPI found!");
        } else {
            getLogger().severe(logPrefixManager + "PlaceholderAPI is not installed!");
            getLogger().severe(logPrefixManager + "Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Check for updates and download the newest version if auto-update is enabled.
     */
    private void updateCheck() {
        final String currentVersion = VersionManager.getCurrentVersion();
        final String newestVersion = VersionManager.getNewestVersion();
        final boolean updateAvailable = VersionManager.isNewerVersion(currentVersion, newestVersion);

        if (updateAvailable) {
            getLogger().info(logPrefixUpdater + "New version available! (Latest: " + newestVersion + ", Current: " + currentVersion + ")");

            final boolean autoUpdate = getConfig().getBoolean("auto-update");

            if (autoUpdate) {
                getLogger().info(logPrefixUpdater + "Starting automatic download...");

                if (VersionManager.downloadUpdate()) {
                    getLogger().info(logPrefixUpdater + "Download successful! Restart the server to apply changes.");
                } else {
                    getLogger().warning(logPrefixUpdater + "Download failed! Check the console for errors.");
                }

            } else {
                // Update is available but auto-update is disabled
                getLogger().info(logPrefixUpdater + "Download the update at: " + VersionManager.getDownloadURL());
            }

        } else {
            // No update available
            getLogger().info(logPrefixUpdater + "Plugin is up to date! (Latest: " + newestVersion + ", Current: " + currentVersion + ")");
        }
    }

    /**
     * Get a command by name and throw an exception if it's not found.
     * Utility method so I don't have to write Object.requireNonNull() every time.
     *
     * @param name Command name
     * @return PluginCommand instance
     */
    private PluginCommand getCmd(String name) {
        return Objects.requireNonNull(getCommand(name), "Command '" + name + "' is not registered in plugin.yml");
    }

    /**
     * Get the database manager instance.
     *
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Prefix a message with the message prefix.
     *
     * @param component Component to prefix
     * @return Prefixed component
     */
    public Component prefixMessage(Component component) {
        return MiniMessage.miniMessage().deserialize(Objects.requireNonNull(getConfig().getString("message-prefix"))).append(component);
    }
}