package dev.starless.maggiordomo.data.user;

import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.enums.VCStatus;
import it.ayyjava.storage.annotations.MongoKey;
import it.ayyjava.storage.annotations.MongoObject;
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
    @Setter private Instant lastJoin;

    // Questi sono i permessi
    private final Set<UserRecord> trusted;
    private final Set<UserRecord> banned;

    private String title;
    private int size;
    @Setter private VCStatus status;
    @Setter private boolean pinned;

    public VC(String guild, String user, String channel, String name, int limit, VCStatus status, boolean pinned) {
        this.guild = guild;
        this.user = user;
        this.channel = channel;
        this.lastJoin = Instant.now();

        this.trusted = new HashSet<>();
        this.banned = new HashSet<>();

        this.title = name.length() > 99 ? name.substring(0, 100) : name;
        this.size = limit;
        this.status = status;
        this.pinned = pinned;
    }

    public VC(String guild, String user, String channel, String name) {
        this(guild, user, channel, name, 2, VCStatus.LOCKED, false);
    }

    public VC(Member member) {
        this(member.getGuild().getId(), member.getId(), "-1", "Stanza di " + member.getEffectiveName());
    }

    public void addRecordPlayer(RecordType type, String id) {
        UserRecord record = new UserRecord(type, guild, channel, id);
        consumeSet(type, set -> set.add(record));
    }

    public void removeRecordPlayer(RecordType type, String id) {
        consumeSet(type, set -> set.removeIf(record -> record.type().equals(type) && record.user().equals(id)));
    }

    public boolean hasRecordPlayer(RecordType type, String id) {
        AtomicBoolean check = new AtomicBoolean(false);
        consumeSet(type, set -> check.set(set.stream().anyMatch(record -> record.user().equals(id))));
        return check.get();
    }

    public void consumeSet(RecordType type, Consumer<Set<UserRecord>> consumer) {
        consumer.accept(type.equals(RecordType.BAN) ? banned : trusted);
    }

    public Set<UserRecord> getTotalRecords() {
        Set<UserRecord> total = new HashSet<>(banned);
        total.addAll(trusted);
        return total;
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
