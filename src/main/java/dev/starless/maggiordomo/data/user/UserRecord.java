package dev.starless.maggiordomo.data.user;

import dev.starless.maggiordomo.data.enums.RecordType;

public record UserRecord(RecordType type, String user) {
}
