package gg.discord.dorado.data.user;

import gg.discord.dorado.data.enums.RecordType;

public record PlayerRecord(RecordType type, String guild, String channel, String user) {
}
