package dev.starless.maggiordomo.localization;

import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.utils.ConfigUtils;
import lombok.experimental.UtilityClass;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@UtilityClass
public class MessageProvider {

    private final Map<String, CommentedConfigurationNode> roots = new ConcurrentHashMap<>();

    private final String defaultString = "Missing language string. Contact an admin about this!";

    public void init() {
        File baseDir = new File("languages");
        if (!baseDir.isDirectory() && !baseDir.mkdirs()) {
            BotLogger.error("Could not create the languages directory");
            return;
        }

        // Load default languages
        for (DefaultLanguages lang : DefaultLanguages.values()) {
            String fileName = "messages_" + lang.getCode() + ".yml";
            File defaultLangFile = new File(baseDir, fileName);
            if (!defaultLangFile.exists()) {
                try (InputStream is = MessageProvider.class.getClassLoader().getResourceAsStream(fileName)) {
                    if(is == null) throw new IOException("File not found!");

                    Files.copy(is, Path.of(defaultLangFile.toURI()));
                } catch (IOException e) {
                    BotLogger.info("Cannot load from classpath default lang: " + lang.getCode());
                }
            }
        }

        try (Stream<Path> paths = Files.walk(Path.of(baseDir.toURI()))) {
            paths.map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith("messages_") && name.endsWith(".yml"))
                    .forEach(string -> {
                        int start = string.indexOf('_') + 1;
                        int end = string.lastIndexOf(".");

                        String code = string.substring(start, end);
                        if (roots.containsKey(code)) return;

                        File customLangFile = new File(string);
                        try {
                            roots.put(code, ConfigUtils.readFromFile(customLangFile));
                        } catch (ConfigurateException e) {
                            BotLogger.info("Cannot read the language file: " + customLangFile.getName());
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

    public String getMessage(Messages message, String lang) {
        CommentedConfigurationNode root = roots.getOrDefault(lang, null);
        return root != null ? ConfigUtils.nodeFromPath(root, message.getPath()).getString(defaultString) : defaultString;
    }

    public String getMessage(Messages message, String lang, Object... args) {
        try {
            return String.format(getMessage(message, lang), args);
        } catch (IllegalFormatException ex) {
            return "Invalid formatting in the string: " + message.getPath();
        }
    }

    public String getMessageFormatted(Messages message, String lang, Object... args) {
        // The Object... args should represent a map like Map<String, Object>
        // where string is used to compute the replaced value and the object the new value
        if (args.length % 2 == 0) {
            try {
                String str = getMessage(message, lang);
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
}
