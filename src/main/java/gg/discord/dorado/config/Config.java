package gg.discord.dorado.config;

import com.vdurmont.semver4j.Semver;
import gg.discord.dorado.Main;
import gg.discord.dorado.logging.BotLogger;
import lombok.Getter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
        boolean configUpdating; // Verrà impostata su true se la config deve essere aggiornata
        if (!configExisting) {
            // Se non esiste, cerchiamo di crearla
            try {
                if (!configFile.createNewFile()) {
                    BotLogger.error("Impossibile creare la configurazione");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loader = YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .path(Paths.get(configFile.toURI()))
                .build();
        try {
            root = loader.load();
        } catch (IOException e) {
            BotLogger.error("Impossibile caricare la configurazione: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return false;
        }

        Map<Object, CommentedConfigurationNode> oldValues = new HashMap<>();
        if (configExisting) {
            // Controlliamo se la versione della config è minore della versione del plugin
            // usando la libreria semver4j
            configUpdating = new Semver(getString(ConfigEntry.CONFIG_VERSION)).isLowerThan(Main.getVersion());
            if (configUpdating) {
                BotLogger.info(String.format("Aggiornamento della config alla versione %s.", Main.getVersion()));

                // Prendiamo i valori della vecchia configurazione e li mettiamo nella cache
                getAllNodes(oldValues, root);
                // Rimuoviamoli dalla configurazione
                oldValues.forEach((obj, node) -> root.removeChild(obj));

                set(ConfigEntry.CONFIG_VERSION, Main.getVersion(), true); // Imposta la nuova versione della config
            } else {
                BotLogger.info("La config è già aggiornata.");
            }
        } else {
            set(ConfigEntry.CONFIG_VERSION, Main.getVersion(), false);
        }

        for (ConfigEntry entry : ConfigEntry.values()) {
            if (entry.equals(ConfigEntry.CONFIG_VERSION)) continue;

            // Se la config si deve aggiornare, vai nella cache a cercare il valore della key (si usa il valore default se non esiste)
            // Altrimenti, aggiunge un valore default alla config per assicurarsi che sia presente
            set(entry, entry.getDefaultValue(), false);
        }

        save();
        initialized = true;

        return true;
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

    private void getAllNodes(Map<Object, CommentedConfigurationNode> map, CommentedConfigurationNode node) {
        node.childrenMap().forEach((obj, subNode) -> {
            if (node.childrenMap().size() != 0) {
                getAllNodes(map, subNode);
            } else {
                map.put(obj, subNode);
            }
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
