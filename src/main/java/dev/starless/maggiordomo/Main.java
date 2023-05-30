package dev.starless.maggiordomo;

import lombok.Getter;

public class Main {

    @Getter private static final String version = "2.0.0";

    public static void main(String[] args) {
        Bot.getInstance().start();
    }
}