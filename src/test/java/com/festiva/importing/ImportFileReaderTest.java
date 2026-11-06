package com.festiva.importing;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class ImportFileReaderTest {
    @Test
    void actualBytesAreBoundedWithoutSizeMetadata() {
        var input = new ByteArrayInputStream(new byte[ImportFileReader.MAX_BYTES + 1]) {
            boolean closed;
            public void close() { closed = true; }
        };
        assertThatThrownBy(() -> ImportFileReader.read(input))
                .isInstanceOf(IOException.class).hasMessageContaining("byte allowance");
        assertThat(input.closed).isTrue();
    }

    @Test
    void exactLimitAndUnicodeRemainReadable() throws Exception {
        String text = ("а".repeat(127) + "\n").repeat(2048);
        assertThat(text.getBytes(StandardCharsets.UTF_8).length).isLessThan(ImportFileReader.MAX_BYTES);
        assertThat(ImportFileReader.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))))
                .hasSize(2048).allMatch(line -> line.equals("а".repeat(127)));
        String exact = ("a".repeat(255) + "\n").repeat(2048);
        assertThat(exact.getBytes(StandardCharsets.UTF_8)).hasSize(ImportFileReader.MAX_BYTES);
        assertThat(ImportFileReader.read(new ByteArrayInputStream(exact.getBytes(StandardCharsets.UTF_8)))).hasSize(2048);
    }

    @Test
    void lineLengthAndLineCountHaveSeparateBounds() {
        for (String text : new String[]{"a".repeat(4097), "a\n".repeat(10001)}) {
            assertThatThrownBy(() -> ImportFileReader.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))))
                    .isInstanceOf(IOException.class).hasMessageContaining("line allowance");
        }
    }
}
