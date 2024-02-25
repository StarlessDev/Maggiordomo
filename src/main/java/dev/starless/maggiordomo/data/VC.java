package dev.starless.maggiordomo.data;

import dev.starless.maggiordomo.data.enums.UserState;
import dev.starless.maggiordomo.data.enums.VCState;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.mongo.api.annotations.MongoKey;
import dev.starless.mongo.api.annotations.MongoObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MongoObject(database = "Maggiordomo", collection = "VCs")
public class VC {

    @MongoKey
    @EqualsAndHashCode.Include
    private final String guild;
    @MongoKey
    @EqualsAndHashCode.Include
    private final String user;

    @Setter private String channel;
    @Setter private String category;
    @Setter private Instant lastJoin;

    private final Set<String> trusted;
    private final Set<String> banned;

    private String title;
    private int size;
    @Setter private VCState state;
    @Setter private boolean pinned;

    public VC(String guild, String user, String channel, String category, String name, int limit, VCState state, boolean pinned) {
        this.guild = guild;
        this.user = user;
        this.channel = channel;
        this.category = category;
        this.lastJoin = Instant.now();

        this.trusted = new HashSet<>();
        this.banned = new HashSet<>();

        this.title = name.length() > 99 ? name.substring(0, 100) : name;
        this.size = limit;
        this.state = state;
        this.pinned = pinned;
    }

    public VC(String guild, String user, String channel, String name) {
        this(guild, user, channel, "-1", name, 2, VCState.LOCKED, false);
    }

    public VC(Member member, String language) {
        this(member.getGuild().getId(),
                member.getId(),
                "-1",
                Translations.string(Messages.VC_NAME, language, member.getEffectiveName()));
    }

    public void addPlayerRecord(UserState type, String id) {
        consumeSet(type, set -> set.add(id));
    }

    public void removePlayerRecord(UserState type, String id) {
        consumeSet(type, set -> set.remove(id));
    }

    public boolean hasPlayerRecord(UserState type, String id) {
        AtomicBoolean check = new AtomicBoolean(false);
        consumeSet(type, set -> check.set(set.contains(id)));
        return check.get();
    }

    public void consumeSet(UserState type, Consumer<Set<String>> consumer) {
        consumer.accept(switch (type) {
            case BAN -> banned;
            case TRUST -> trusted;
        });
    }

    public Set<UserRecord<UserState>> getRoleRecords() {
        return getRecordsByStates(UserState.TRUST, UserState.BAN);
    }

    public Set<UserRecord<UserState>> getRecordsByStates(UserState... states) {
        Set<UserRecord<UserState>> total = new HashSet<>();

        for (UserState state : states) {
            consumeSet(state, set -> total.addAll(set.stream()
                    .map(id -> new UserRecord<>(state, id))
                    .toList()));
        }

        return total;
    }

    public boolean hasChannel() {
        return !channel.equals("-1");
    }

    public void setTitle(String title) {
        if (title.length() > 100) return;

        this.title = title;
    }

    public void setSize(int size) {
        if (size < -1 || size > 99) return;

        this.size = size;
    }
}
