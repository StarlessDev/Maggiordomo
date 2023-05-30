package dev.starless.maggiordomo.data.user;

import dev.starless.maggiordomo.data.enums.RecordType;

public record PlayerRecord(RecordType type, String guild, String channel, String user) {
}
