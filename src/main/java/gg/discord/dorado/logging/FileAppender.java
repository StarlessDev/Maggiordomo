package gg.discord.dorado.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import gg.discord.dorado.utils.DateUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class FileAppender extends AppenderBase<ILoggingEvent> {

    private final Deque<ILoggingEvent> deque;

    public FileAppender() {
        this.deque = new LinkedBlockingDeque<>();
        Executors.newSingleThreadExecutor().submit(new LogWriter());

        this.start();
    }

    @Override
    protected void append(ILoggingEvent e) {
        deque.add(e);
    }

    class LogWriter implements Runnable {

        private final File dir;
        private final File log;

        public LogWriter() {
            dir = new File("logs");
            log = new File(dir, DateUtils.now(DateUtils.getDayFormatter()) + ".log");
        }

        @Override
        public void run() {
            if (!dir.isDirectory() && !dir.mkdirs()) return;

            if (!log.exists()) {
                try {
                    if (!log.createNewFile()) return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }

            while (true) {
                ILoggingEvent event = deque.poll();
                if (event == null) continue;

                String string = String.format("(%s) %s%n",
                        DateUtils.now(DateUtils.getFullFormatter()),
                        event.getFormattedMessage());
                try {
                    Files.writeString(log.toPath(), string, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
