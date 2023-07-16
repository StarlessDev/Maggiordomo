package dev.starless.maggiordomo.config;

import dev.starless.maggiordomo.Main;

public enum ConfigEntry {

    CONFIG_VERSION("config_version", Main.getVersion()),
    TOKEN("token", "discord-bot-token"),
    MONGO("mongo", "mongodb://"),
    UPTIME_ENABLED("uptimerobot.enabled", false),
    UPTIME_PORT("uptimerobot.port", 8080),
    UPTIME_ENDPOINT("uptimerobot.endpoint", "/endpoint");

    private final String path;
    private final Object defaultValue;

    ConfigEntry(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}

