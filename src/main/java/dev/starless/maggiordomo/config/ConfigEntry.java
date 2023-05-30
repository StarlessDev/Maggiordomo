package dev.starless.maggiordomo.config;

import dev.starless.maggiordomo.Main;

public enum ConfigEntry {

    CONFIG_VERSION("config_version", Main.getVersion()),
    TOKEN("token", "discord-bot-token"),
    MONGO("mongo", "mongodb://");

    private final String path;
    private final Object defaultValue;

    ConfigEntry(String s, Object obj) {
        path = s;
        defaultValue = obj;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}

