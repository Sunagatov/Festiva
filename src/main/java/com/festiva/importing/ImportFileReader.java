package com.festiva.importing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ImportFileReader {
    public static final int MAX_BYTES = 512 * 1024;
    public static final int MAX_LINE_CHARS = 4096;
    public static final int MAX_LINES = 10_000;
    private final TelegramClient client;
    private final String token;

    public ImportFileReader(TelegramClient client, @Value("${telegram.bot.token}") String token) {
        this.client = client;
        this.token = token;
    }

    public List<String> download(String fileId) throws TelegramApiException, IOException {
        var file = client.execute(GetFile.builder().fileId(fileId).build());
        var connection = URI.create("https://api.telegram.org/file/bot" + token + "/" + file.getFilePath())
                .toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setUseCaches(false);
        return read(connection.getInputStream());
    }

    static List<String> read(InputStream input) throws IOException {
        try (input) {
            return readBounded(input);
        }
    }

    private static List<String> readBounded(InputStream input) throws IOException {
        long deadline = System.nanoTime() + 10_000_000_000L;
        var bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (bytes.size() <= MAX_BYTES) {
            if (System.nanoTime() >= deadline) throw new IOException("Import read deadline exceeded");
            int count = input.read(buffer, 0, Math.min(buffer.length, MAX_BYTES + 1 - bytes.size()));
            if (count < 0) break;
            if (count == 0) throw new IOException("Import made no progress");
            bytes.write(buffer, 0, count);
        }
        if (bytes.size() > MAX_BYTES) throw new IOException("Import exceeds byte allowance");
        List<String> lines = bytes.toString(StandardCharsets.UTF_8).lines().toList();
        if (lines.size() > MAX_LINES || lines.stream().anyMatch(line -> line.length() > MAX_LINE_CHARS)) {
            throw new IOException("Import exceeds line allowance");
        }
        return lines;
    }
}
