package dev.starless.maggiordomo.data;

import dev.starless.maggiordomo.Bot;
import it.ayyjava.storage.annotations.MongoKey;
import it.ayyjava.storage.annotations.MongoObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MongoObject(database = "Maggiordomo", collection = "Settings")
public class Settings {

    @MongoKey
    @EqualsAndHashCode.Include
    private final String guild;

    private final Set<String> premiumRoles;
    private final Set<String> bannedRoles;
    private List<String> categories;
    private String channelID;
    private String voiceID;
    private String publicRole;

    public Settings(String guild, String category, String channel, String voice, String role, Set<String> premiumRoles, Set<String> staffRoles, Set<String> bannedRoles) {
        this.guild = guild;

        // In genere CopyOnWriteArrayList è molto pesante quando si tratta di
        // operazioni di scrittura, ma dato che abbiamo pochi elementi e
        // la maggior parte delle volte leggiamo da questa lista
        // dovrebbe essere accettabile come soluzione.
        this.categories = new CopyOnWriteArrayList<>();
        this.categories.add(category);

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
        categories.clear();
        voiceID = "-1";
        publicRole = "-1";
    }

    public void forEachCategory(Guild guild, Consumer<Category> action) {
        categories.forEach(id -> {
            Category category = guild.getCategoryById(id);
            if (category == null) return;

            action.accept(category);
        });
    }

    public synchronized Category getAvailableCategory(Guild guild) {
        List<String> toBeDeleted = new ArrayList<>();
        Category output = null;

        for (String id : categories) {
            Category category = guild.getCategoryById(id);
            if (category == null) {
                toBeDeleted.add(id);
                continue;
            }

            if (category.getChannels().size() < 50) {
                output = category;
                break;
            }
        }

        if(!toBeDeleted.isEmpty()) {
            toBeDeleted.forEach(id -> categories.remove(id));
        }

        return output != null ? output : createCategory(guild);
    }

    public Category createCategory(Guild guild) {
        int movement = 0;

        if (hasCategory()) {
            Category prevCategory = guild.getCategoryById(categories.get(categories.size() - 1));
            if (prevCategory != null) {
                List<Category> categoryList = guild.getCategories();
                movement = categoryList.size() - categoryList.indexOf(prevCategory) - 1;
            }
        }

        Category newCategory = guild.createCategory("private #" + categories.size()).complete();
        categories.add(newCategory.getId());
        if(movement != 0) {
            guild.modifyCategoryPositions()
                    .selectPosition(newCategory)
                    .moveUp(movement)
                    .complete();
        }

        // Update database async
        CompletableFuture.runAsync(() -> Bot.getInstance().getCore().getSettingsMapper().update(this));

        return newCategory;
    }

    public boolean hasCategory() {
        return !categories.isEmpty();
    }

    public boolean isMainCategory(String categoryID) {
        return categories.size() > 0 && categories.get(0).equals(categoryID);
    }
}
