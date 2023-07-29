package dev.starless.maggiordomo.utils;

import lombok.experimental.UtilityClass;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Paths;

@UtilityClass
public class ConfigUtils {

    public CommentedConfigurationNode nodeFromPath(CommentedConfigurationNode root, String path) {
        String[] spl = path.split("\\.");
        CommentedConfigurationNode node = null;
        for (String s : spl) {
            if (node == null) node = root.node(s);
            else node = node.node(s);
        }

        return node;
    }

    public YamlConfigurationLoader loaderFromFile(File langFile) {
        return YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .path(Paths.get(langFile.toURI()))
                .build();
    }

    public CommentedConfigurationNode readFromFile(File langFile) throws ConfigurateException {
        return loaderFromFile(langFile).load();
    }
}
