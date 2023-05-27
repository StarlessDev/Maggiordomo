package gg.discord.dorado.data.user;

import gg.discord.dorado.data.enums.RecordType;
import gg.discord.dorado.data.enums.VCStatus;
import it.ayyjava.storage.annotations.MongoKey;
import it.ayyjava.storage.annotations.MongoObject;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@MongoObject(database = "Maggiordomo", collection = "VCs")
public class VC implements Comparable<VC> {

    @MongoKey private final String guild;
    @MongoKey private final String user;
    @Setter private String channel;
    @Setter private Instant lastJoin;
    private Instant lastModification;

    // Questi sono i permessi
    private final Set<PlayerRecord> trusted;
    private final Set<PlayerRecord> banned;

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

        this.title = name;
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
        PlayerRecord record = new PlayerRecord(type, guild, channel, id);
        consumeSet(type, set -> {
            if (set.size() == 25) return;
            set.add(record);
        });
    }

    public void removeRecordPlayer(RecordType type, String id) {
        consumeSet(type, set -> set.removeIf(record -> record.type().equals(type) && record.user().equals(id)));
    }

    public boolean hasRecordPlayer(RecordType type, String id) {
        AtomicBoolean check = new AtomicBoolean(false);
        consumeSet(type, set -> check.set(set.stream().anyMatch(record -> record.user().equals(id))));
        return check.get();
    }

    public void consumeSet(RecordType type, Consumer<Set<PlayerRecord>> consumer) {
        consumer.accept(type.equals(RecordType.BAN) ? banned : trusted);
    }

    public Set<PlayerRecord> getTotalRecords() {
        Set<PlayerRecord> total = new HashSet<>(banned);
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

    public void updateLastModification() {
        updateLastModification(Instant.now());
    }

    public void updateLastModification(@Nullable Instant instant) {
        lastModification = instant == null ? Instant.EPOCH : instant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VC vc = (VC) o;

        if (!guild.equals(vc.guild)) return false;
        return user.equals(vc.user);
    }

    @Override
    public int hashCode() {
        int result = guild.hashCode();
        result = 31 * result + user.hashCode();
        return result;
    }

    @Override
    public int compareTo(@NotNull VC o) {
        return lastModification.compareTo(o.getLastModification());
    }
}
