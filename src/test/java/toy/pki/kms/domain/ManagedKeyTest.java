package toy.pki.kms.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import toy.pki.kms.web.KeyAlgorithmPreset;

class ManagedKeyTest {

    private final ManagedKey key = new ManagedKey(new KeyId("key"), KeyAlgorithmPreset.EC_P256, Instant.now());

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void treatsMissingOrBlankAliasAsAbsent(String alias) {
        key.setAlias(alias);
        assertThat(key.getAlias()).isNull();
    }

    @Test
    void trimsAliasBeforeCheckingTheLengthAndKeepsTheLastValidValue() {
        String maximum = "a".repeat(64);
        key.setAlias("  " + maximum + "  ");
        assertThat(key.getAlias()).isEqualTo(maximum);
        assertThatThrownBy(() -> key.setAlias("a".repeat(65))).isInstanceOf(IllegalArgumentException.class);
        assertThat(key.getAlias()).isEqualTo(maximum);
    }
}
