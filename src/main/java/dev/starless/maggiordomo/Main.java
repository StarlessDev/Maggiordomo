package dev.starless.maggiordomo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.logging.BotLogger;
import lombok.Getter;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Main {

    @Getter private static final String version = "2.1.0";

    public static void main(String[] args) {
        BotLogger.info("Running Maggiordomo v%s by Starless", version);
        checkUpdate();

        try {
            Bot.getInstance().start();
        } catch (InvalidTokenException | IllegalArgumentException ex) {
            BotLogger.error("An error occurred while starting the bot: " + ex.getMessage());
            System.exit(0);
        }
    }

    // Controlla se ci sono degli aggiornamenti su github
    private static void checkUpdate() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI("https://api.github.com/repos/StarlessDev/Maggiordomo/releases/latest")).build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String latestTag = json.get("tag_name").getAsString();

                Semver currentVersion = new Semver(version);
                Semver latestVersion = new Semver(latestTag);
                if (latestVersion.isGreaterThan(currentVersion)) {
                    BotLogger.info("There is a new version (%s > %s) available on github!", latestTag, version);
                } else {
                    BotLogger.info("Maggiordomo is up to date.");
                }
            } else {
                BotLogger.error("Could not check for updates! Invalid HTTP Code: " + code);
            }
        } catch (URISyntaxException ex) {
            BotLogger.error("Could not check for updates! (URISyntaxException)");
        } catch (IOException | InterruptedException e) {
            BotLogger.error("Could not check for updates! (%s)", e.getMessage());
        }
    }
}