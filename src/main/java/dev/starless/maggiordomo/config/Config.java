package dev.starless.maggiordomo.config;

import com.vdurmont.semver4j.Semver;
import dev.starless.maggiordomo.Main;
import dev.starless.maggiordomo.utils.BotLogger;
import dev.starless.maggiordomo.utils.ConfigUtils;
import lombok.Getter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {

    @Getter private boolean initialized = false;

    private YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public boolean init() {
        if (initialized) return true;

        File configDirectory = new File("configuration");
        if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
            return false;
        }

        File configFile = new File(configDirectory, "maggiordomo.yml");

        boolean configExisting = configFile.exists(); // Controlliamo se esiste il file della configurazione
        boolean configUpdating = false; // Verrà impostata su true se la config deve essere aggiornata
        if (!configExisting) {
            // Se non esiste, cerchiamo di crearla
            try {
                if (configFile.createNewFile()) {
                    BotLogger.info("The config has been created. Stop the bot to edit it.");
                } else {
                    BotLogger.error("Could not create the config file");
                }
            } catch (IOException e) {
                BotLogger.error("An error occurred while creating the config directory: " + e.getMessage());
            }
        }

        loader = ConfigUtils.loaderFromFile(configFile);
        try {
            root = loader.load();
        } catch (IOException e) {
            BotLogger.error("Could not load the config: " + e.getMessage());
            return false;
        }

        Map<String, Object> oldValues = new HashMap<>();
        if (configExisting) {
            // Controlliamo se la versione della config è minore della versione del plugin
            // usando la libreria semver4j
            configUpdating = new Semver(getString(ConfigEntry.CONFIG_VERSION)).isLowerThan(Main.getVersion());
            if (configUpdating) {
                BotLogger.info(String.format("Updating the configuration to v%s.", Main.getVersion()));

                // Get and delete the old nodes
                getNodeValues(oldValues, root);

                // Insert the new config version
                set(ConfigEntry.CONFIG_VERSION, Main.getVersion(), true); // Imposta la nuova versione della config
            } else {
                BotLogger.info("The configuration is up to date.");
            }
        } else {
            set(ConfigEntry.CONFIG_VERSION, Main.getVersion(), false);
        }

        for (ConfigEntry entry : ConfigEntry.values()) {
            if (entry.equals(ConfigEntry.CONFIG_VERSION)) continue;

            if (configUpdating) {
                Object value = oldValues.getOrDefault(entry.getPath(), entry.getDefaultValue());
                set(entry, value, true);
            } else {
                set(entry, entry.getDefaultValue(), false);
            }
        }

        save();
        initialized = true;

        return configExisting;
    }

    public void save() {
        try {
            loader.save(root);
        } catch (IOException e) {
            BotLogger.error("An error occurred while saving the config: " + e.getMessage());
        }
    }

    public void set(ConfigEntry entry, Object value, boolean replace) {
        try {
            CommentedConfigurationNode node = nodeFromPath(entry.getPath());
            if (!replace && !node.empty()) return;

            node.set(value);
        } catch (SerializationException e) {
            BotLogger.error("An error occurred while editing the config: " + e.getMessage());
        }
    }

    private void getNodeValues(Map<String, Object> map, CommentedConfigurationNode node) {
        node.childrenMap().forEach((obj, subNode) -> {
            // if the subnode has a children, run the function recursively
            if (!subNode.childrenMap().isEmpty()) {
                getNodeValues(map, subNode);
            } else {
                // Build the node string path
                StringBuilder sb = new StringBuilder();
                Object[] pathParts = subNode.path().array();
                for (Object part : pathParts) {
                    sb.append(part).append(".");
                }

                try {
                    // leave the last character out of the key
                    map.put(sb.substring(0, sb.length() - 1), subNode.get(Object.class));
                } catch (SerializationException e) {
                    BotLogger.error("Something could not be serialized as object? " + e.getMessage());
                }
            }

            node.removeChild(obj);
        });
    }

    private CommentedConfigurationNode nodeFromPath(String path) {
        String[] spl = path.split("\\.");
        CommentedConfigurationNode node = null;
        for (String s : spl) {
            if (node == null) node = root.node(s);
            else node = node.node(s);
        }

        return node;
    }

    public String getString(ConfigEntry entry) {
        return nodeFromPath(entry.getPath()).getString();
    }

    public int getInt(ConfigEntry entry) {
        return nodeFromPath(entry.getPath()).getInt();
    }

    public long getLong(ConfigEntry entry) {
        return nodeFromPath(entry.getPath()).getLong();
    }

    public boolean getBoolean(ConfigEntry entry) {
        return nodeFromPath(entry.getPath()).getBoolean();
    }
}
