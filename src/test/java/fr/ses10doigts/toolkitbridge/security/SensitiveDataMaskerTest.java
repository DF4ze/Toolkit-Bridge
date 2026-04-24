package fr.ses10doigts.toolkitbridge.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void masksKeepingLastFourCharacters() {
        assertThat(SensitiveDataMasker.mask("abcd123456")).isEqualTo("******3456");
    }

    @Test
    void masksEntireShortString() {
        assertThat(SensitiveDataMasker.mask("1234")).isEqualTo("****");
        assertThat(SensitiveDataMasker.mask("12")).isEqualTo("**");
    }

    @Test
    void keepsNullAndEmptyValuesAsIs() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("")).isEmpty();
    }
}
