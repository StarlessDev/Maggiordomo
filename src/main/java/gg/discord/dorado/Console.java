package gg.discord.dorado;

import java.util.Scanner;

public class Console extends Thread {

    private final Scanner scanner;

    public Console() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        handle();
    }

    private void handle() {
        String cmd = scanner.nextLine().toLowerCase();

        if (cmd.equals("stop") || cmd.equals("end")) {
            scanner.close(); // Chiudi lo scanner
            System.exit(0); // Ferma il programma
            return;
        }

        handle();
    }
}
