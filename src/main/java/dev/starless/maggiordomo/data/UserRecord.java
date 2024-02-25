package dev.starless.maggiordomo.data;

public record UserRecord<T extends Enum<T>>(T type, String user) {
}
