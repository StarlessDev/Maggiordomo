package gg.discord.dorado.logging;

import gg.discord.dorado.Bot;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

@UtilityClass
public class References {

    public String user(String id) {
        if(!Bot.getInstance().isReady()) return null;

        User user = Bot.getInstance().getJda().getUserById(id);
        return user != null ? user.getAsTag() : null;
    }

    public String guild(String id) {
        if(!Bot.getInstance().isReady()) return null;

        Guild guild = Bot.getInstance().getJda().getGuildById(id);
        return guild != null ? guild.getName() : null;
    }
}
