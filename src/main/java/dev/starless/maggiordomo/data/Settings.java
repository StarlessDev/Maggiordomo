package dev.starless.maggiordomo.data;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.mongo.annotations.MongoKey;
import dev.starless.mongo.annotations.MongoObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    private final Map<FilterType, Set<String>> filterStrings;
    private List<String> categories;

    private String channelID;
    private String voiceID;
    private String menuID;
    private String publicRole;
    private String language;
    private long maxInactivity;

    private String title;
    private String descriptionRaw;

    public Settings(String guild, String everyone) {
        this.guild = guild;

        this.premiumRoles = new HashSet<>();
        this.bannedRoles = new HashSet<>();
        this.filterStrings = new HashMap<>();
        // In genere CopyOnWriteArrayList è molto pesante quando si tratta di
        // operazioni di scrittura, ma dato che abbiamo pochi elementi e
        // la maggior parte delle volte leggiamo da questa lista
        // dovrebbe essere accettabile come soluzione.
        this.categories = new CopyOnWriteArrayList<>();

        this.channelID = "-1";
        this.voiceID = "-1";
        this.menuID = "-1";
        this.publicRole = everyone;
        this.language = "en";
        this.maxInactivity = -1L;

        this.title = Translations.string(Messages.SETTINGS_INTERFACE_TITLE, language);
        this.descriptionRaw = Translations.string(Messages.SETTINGS_INTERFACE_DESCRIPTION, language);
    }

    public Settings(Guild guild) {
        this(guild.getId(), guild.getPublicRole().getId());
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

        if (!toBeDeleted.isEmpty()) {
            toBeDeleted.forEach(id -> categories.remove(id));
        }

        return output != null ? output : createCategory(guild);
    }

    public @Nullable Category createCategory(Guild guild) {
        int movement = 0;

        if (hasCategory()) {
            Category prevCategory = guild.getCategoryById(categories.get(categories.size() - 1));
            if (prevCategory != null) {
                List<Category> categoryList = guild.getCategories();
                movement = categoryList.size() - categoryList.indexOf(prevCategory) - 1;
            }
        }

        Category newCategory;
        try { // Try to create the category
            String categoryName = "rooms";
            if (!categories.isEmpty()) {
                categoryName += " (" + (categories.size() + 1) + ")";
            }

            newCategory = guild.createCategory(categoryName)
                    // Bot permissions (not actually needed if bot has admin, which needs to be removed in the future)
                    .addMemberPermissionOverride(Bot.getInstance().getJda().getSelfUser().getIdLong(),
                            List.of(Permission.MANAGE_CHANNEL,
                                    Permission.MESSAGE_MANAGE,
                                    Permission.MANAGE_ROLES,
                                    Permission.MANAGE_PERMISSIONS,
                                    Permission.MESSAGE_SEND,
                                    Permission.MESSAGE_ATTACH_FILES),
                            Collections.emptyList())
                    .addRolePermissionOverride(Long.parseLong(publicRole),
                            Collections.singletonList(Permission.VIEW_CHANNEL),
                            List.of(Permission.CREATE_PUBLIC_THREADS,
                                    Permission.CREATE_PRIVATE_THREADS,
                                    Permission.MESSAGE_SEND_IN_THREADS))
                    .complete();
        } catch (RuntimeException ex) { // Catch a possible exception
            newCategory = null;
        }

        if (newCategory != null) {
            // Add category to the list
            categories.add(newCategory.getId());

            if (movement != 0) {
                guild.modifyCategoryPositions()
                        .selectPosition(newCategory)
                        .moveUp(movement)
                        .complete();
            }

            // Update database async
            CompletableFuture.runAsync(() -> Bot.getInstance().getCore().getSettingsMapper().update(this));
        }

        return newCategory;
    }

    public Set<String> getFilterWords(FilterType type) {
        return filterStrings.getOrDefault(type, new HashSet<>());
    }

    public synchronized void modifyFilters(FilterType type, Consumer<Set<String>> action) {
        Set<String> strings = filterStrings.compute(type, (key, list) -> {
            if (list == null) {
                list = new HashSet<>();
            }

            return list;
        });

        action.accept(strings);
        filterStrings.put(type, strings);
    }

    public boolean isBanned(Member member) {
        return member.getRoles().stream()
                .map(Role::getId)
                .anyMatch(bannedRoles::contains);
    }

    public boolean hasNoAccess(Member member) {
        Role everyone = member.getGuild().getPublicRole();
        if (everyone.getId().equals(publicRole)) return false;

        return member.getRoles().stream()
                .map(Role::getId)
                .noneMatch(id -> id.equals(publicRole));
    }

    public boolean hasCategory() {
        return !categories.isEmpty();
    }

    public Category getMainCategory(Guild guild) {
        return !categories.isEmpty() ? guild.getCategoryById(categories.get(0)) : null;
    }

    public boolean isMainCategory(String categoryID) {
        return !categories.isEmpty() && categories.get(0).equals(categoryID);
    }

    public String getDescription() {
        return descriptionRaw.replace("{CHANNEL}", voiceID.equals("-1") ? "`???`" : "<#" + voiceID + ">");
    }
}
