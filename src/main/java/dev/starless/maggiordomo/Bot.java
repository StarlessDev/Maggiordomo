package dev.starless.maggiordomo;

import dev.starless.maggiordomo.config.Config;
import dev.starless.maggiordomo.config.ConfigEntry;
import dev.starless.maggiordomo.interfaces.Service;
import dev.starless.maggiordomo.logging.BotLogger;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.*;

@Getter
public class Bot implements Service {

    @Getter private static final Bot instance = new Bot();

    private Core core;
    private JDA jda;

    private boolean ready;

    @Override
    public void start() throws InvalidTokenException, IllegalArgumentException {
        ready = false;
        BotLogger.setup();

        // Inizializza le funzioni del bot
        Config config = new Config();
        if(!config.init()) {
            System.exit(0);
            return;
        }

        core = new Core(config);

        // Effettua il login
        jda = JDABuilder.createDefault(config.getString(ConfigEntry.TOKEN))
                .enableIntents(GUILD_MESSAGES,
                        GUILD_VOICE_STATES,
                        GUILD_MEMBERS)
                .disableIntents(DIRECT_MESSAGES,
                        GUILD_MODERATION,
                        GUILD_PRESENCES,
                        GUILD_EMOJIS_AND_STICKERS,
                        GUILD_WEBHOOKS,
                        GUILD_INVITES,
                        GUILD_MESSAGE_REACTIONS,
                        GUILD_MESSAGE_TYPING,
                        DIRECT_MESSAGE_REACTIONS,
                        DIRECT_MESSAGE_TYPING)
                .enableCache(VOICE_STATE, MEMBER_OVERRIDES)
                .disableCache(EMOJI,
                        STICKER,
                        ONLINE_STATUS,
                        ACTIVITY,
                        CLIENT_STATUS,
                        ROLE_TAGS,
                        FORUM_TAGS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setActivity(Activity.watching("VCs"))
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(this)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @SubscribeEvent
    public void onReady(@NotNull ReadyEvent event) {
        ready = true;

        // Avvia il bot vero e proprio
        core.onEnable(jda);

        // Avvia la console
        new Console().start();
    }

    @SubscribeEvent
    public void onShutdown(@NotNull ShutdownEvent event) {
        ready = false;
    }

    @Override
    public void stop() {
        if(jda == null) return;

        if(core != null) {
            core.onDisable(jda);
        }

        BotLogger.info("Unregistering listeners..."); // Non si sa mai
        jda.getEventManager().getRegisteredListeners().forEach(obj -> jda.removeEventListener(obj));

        BotLogger.info("Shutting down JDA...");
        jda.shutdown();

        // Might be an issue, just to be safe...
        BotLogger.info("Closing OkHttpClient...");
        OkHttpClient client = jda.getHttpClient();
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }
}
