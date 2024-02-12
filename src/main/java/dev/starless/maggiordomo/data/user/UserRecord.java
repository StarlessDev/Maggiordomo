package dev.starless.maggiordomo.data.user;

public record UserRecord<T extends Enum<T>>(T type, String user) {
}
