package dev.starless.maggiordomo.interfaces;

import net.dv8tion.jda.api.JDA;

public interface Module {

    void onEnable(JDA jda);

    void onDisable(JDA jda);
}
