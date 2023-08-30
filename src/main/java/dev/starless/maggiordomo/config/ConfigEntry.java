package dev.starless.maggiordomo.config;

import dev.starless.maggiordomo.Main;

public enum ConfigEntry {

    CONFIG_VERSION("config_version", Main.getVersion()),
    TOKEN("token", "discord-bot-token"),
    MONGO("mongo", "mongodb://"),
    SERVER_PORT("server.port", 8080),
    API_ENABLED("server.api.enabled", true),
    API_KEY("server.api.key", "defaultkey"),
    API_RATE_LIMIT("server.api.ratelimit", 10),
    UPTIME_ENABLED("server.uptimerobot.enabled", false),
    UPTIME_ENDPOINT("server.uptimerobot.endpoint", "/endpoint");

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

