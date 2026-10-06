package com.asfaw.pastebin.paste;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    private final IdGenerator generator = new IdGenerator();

    @Test
    void hasExpectedLength() {
        assertThat(generator.generate()).hasSize(IdGenerator.LENGTH);
    }

    @RepeatedTest(5)
    void usesOnlyBase62Characters() {
        assertThat(generator.generate()).matches("[0-9A-Za-z]{8}");
    }

    @Test
    void producesNoDuplicatesInLargeSample() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            assertThat(seen.add(generator.generate())).isTrue();
        }
    }
}
