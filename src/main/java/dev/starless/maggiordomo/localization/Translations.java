package dev.starless.maggiordomo.localization;

import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.utils.ConfigUtils;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.IOUtil;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@UtilityClass
public class Translations {

    private final Map<String, CommentedConfigurationNode> roots = new ConcurrentHashMap<>();

    private final String defaultString = "Missing language string. Contact an admin!";

    public void init() {
        File languagesDir = new File("languages");
        File guidesDir = new File("guides");
        if (!languagesDir.isDirectory() && !languagesDir.mkdirs()) {
            BotLogger.error("Could not create the languages directory");
            return;
        }
        if (!guidesDir.isDirectory() && !guidesDir.mkdirs()) {
            BotLogger.error("Could not create the guides directory");
            return;
        }

        // Load default languages and respective guides
        for (DefaultLanguages lang : DefaultLanguages.values()) {
            String langFileName = "messages_" + lang.getCode() + ".yml";
            File langFile = new File(languagesDir, langFileName);
            if (!langFile.exists()) {
                copyFromClasspath(langFile, "languages/" + langFileName);
            }

            String guideFileName = "guide_" + lang.getCode() + ".png";
            File guideFile = new File(guidesDir, guideFileName);
            if (!guideFile.exists()) {
                copyFromClasspath(guideFile, "guides/" + guideFileName);
            }
        }

        try (Stream<Path> paths = Files.walk(Path.of(languagesDir.toURI()))) {
            paths.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("messages_") && name.endsWith(".yml");
                    })
                    .forEach(path -> {
                        String string = path.getFileName().toString();
                        int start = string.indexOf('_') + 1;
                        int end = string.lastIndexOf(".");

                        String code = string.substring(start, end);
                        if (roots.containsKey(code)) return;

                        try {
                            roots.put(code, ConfigUtils.readFromFile(path.toFile()));
                        } catch (ConfigurateException e) {
                            BotLogger.info("Cannot read the language file: " + path.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            BotLogger.info("Cannot read the files from the languages directory.");
        }

        BotLogger.info("Loaded %d languages", roots.size());
    }

    public Set<String> getLanguageCodes() {
        return roots.keySet();
    }

    public String string(Messages message, String lang) {
        CommentedConfigurationNode root = roots.getOrDefault(lang, null);
        return root != null ? ConfigUtils.nodeFromPath(root, message.getPath()).getString() : defaultString;
    }

    public String string(Messages message, String lang, Object... args) {
        try {
            return String.format(string(message, lang), args);
        } catch (IllegalFormatException ex) {
            return "Invalid formatting in the string: " + message.getPath();
        }
    }

    public String stringFormatted(Messages message, String lang, Object... args) {
        // The Object... args should represent a map like Map<String, Object>
        // where string is used to compute the replaced value and the object the new value
        if (args.length % 2 == 0) {
            try {
                String str = string(message, lang);
                for (int i = 0; i < args.length; i += 2) {
                    String replaced = ("{" + args[i] + "}").toUpperCase();
                    String value = String.valueOf(args[i + 1]);
                    str = str.replaceAll(replaced, value);
                }
            } catch (Exception ignored) {
                // Just return the error as the string to warn the user about the config error
            }
        }

        return "Invalid formatting in the string: " + message.getPath();
    }

    public FileUpload guide(String lang) {
        return FileUpload.fromData(new File("guides", "guide_" + lang + ".png"));
    }

    private void copyFromClasspath(File destination, String name) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new IOException("File not found!");

            Files.copy(is, Path.of(destination.toURI()));
        } catch (IOException e) {
            BotLogger.info("Could not copy %s from the classpath", name);
        }
    }
}
