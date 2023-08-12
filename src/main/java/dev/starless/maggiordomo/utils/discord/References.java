package dev.starless.maggiordomo.utils.discord;

import dev.starless.maggiordomo.Bot;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

@UtilityClass
public class References {

    public String user(String id) {
        if (!Bot.getInstance().isReady()) return null;

        User user = Bot.getInstance().getJda().getUserById(id);
        return user(user);
    }

    public String user(User user) {
        if (user != null) {
            return user.getDiscriminator().equals("0000") ? "@" + user.getName() : user.getAsTag();
        } else {
            return null;
        }
    }

    public String guild(String id) {
        if (!Bot.getInstance().isReady()) return null;

        Guild guild = Bot.getInstance().getJda().getGuildById(id);
        return guild != null ? guild.getName() : null;
    }

    public String role(Guild guild, String id) {
        Role role = guild.getRoleById(id);
        return role != null ? role.getAsMention() : "<@&0>";
    }

    public String roleName(Guild guild, String id) {
        Role role = guild.getRoleById(id);
        if(role == null) return "@Unknown";

        String out = "";
        if(role.getIdLong() != guild.getPublicRole().getIdLong()) {
            out += "@";
        }

        return out + role.getName();
    }
}
