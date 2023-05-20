package gg.discord.dorado.data;

import it.ayyjava.storage.annotations.MongoKey;
import it.ayyjava.storage.annotations.MongoObject;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashSet;
import java.util.Set;

@Data
@MongoObject(database = "Maggiordomo", collection = "Settings")
public class Settings {

    @MongoKey private final String guild;
    private final Set<String> premiumRoles;
    private final Set<String> bannedRoles;
    private String categoryID;
    private String channelID;
    private String voiceID;
    private String publicRole;

    public Settings(String guild, String category, String channel, String voice, String role, Set<String> premiumRoles, Set<String> staffRoles, Set<String> bannedRoles) {
        this.guild = guild;
        this.categoryID = category;
        this.channelID = channel;
        this.voiceID = voice;
        this.publicRole = role;
        this.premiumRoles = premiumRoles;
        this.bannedRoles = bannedRoles;
    }

    public Settings(Guild guild) {
        this(guild.getId(), "-1", "-1", "-1", "-1", new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public void reset() {
        this.categoryID = "-1";
        this.voiceID = "-1";
        this.publicRole = "-1";
    }

    public boolean hasCategory() {
        return !categoryID.equals("-1");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Settings settings = (Settings) o;

        return guild.equals(settings.guild);
    }

    @Override
    public int hashCode() {
        return guild.hashCode();
    }
}
