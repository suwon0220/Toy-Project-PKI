package toy.pki.ca.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import toy.pki.kms.domain.key.generation.RsaKeyGenerationParameters;

class CertificateProfileValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void startsAsDraftAndRejectsNullStatus() {
        assertThat(profile(false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE), Set.of()).getStatus())
            .isEqualTo(ProfileStatus.DRAFT);
        assertThat(validator.validateValue(CertificateProfile.class, "status", null)).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidValidityRanges")
    void rejectsInvalidValidityRanges(int defaultDays, int maxDays, String property) {
        CertificateProfile profile = profile(
            new ProfileId("test"), defaultDays, maxDays,
            false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE), Set.of());
        assertThat(violations(profile)).contains(property);
    }

    static Stream<Arguments> invalidValidityRanges() {
        return Stream.of(
            Arguments.of(0, 365, "defaultValidityDays"),
            Arguments.of(-1, 365, "defaultValidityDays"),
            Arguments.of(1, 0, "maxValidityDays"),
            Arguments.of(365, 90, "validValidityRange"));
    }

    @ParameterizedTest
    @MethodSource("invalidUsageCombinations")
    void rejectsInvalidBasicConstraintsAndUsage(
        boolean ca, Integer pathLength, Set<KeyUsage> usages, String property) {
        assertThat(violations(profile(ca, pathLength, usages, Set.of()))).contains(property);
    }

    static Stream<Arguments> invalidUsageCombinations() {
        return Stream.of(
            Arguments.of(false, null, Set.of(KeyUsage.KEY_CERT_SIGN), "validCaKeyUsage"),
            Arguments.of(false, 0, Set.of(KeyUsage.DIGITAL_SIGNATURE), "validPathLenConstraint"),
            Arguments.of(true, -1, Set.of(KeyUsage.KEY_CERT_SIGN), "validPathLenConstraint"),
            Arguments.of(true, 0, Set.of(KeyUsage.CRL_SIGN), "validPathLenConstraint"),
            Arguments.of(false, null, Set.of(KeyUsage.ENCIPHER_ONLY), "validKeyAgreementUsage"),
            Arguments.of(false, null, Set.of(KeyUsage.DECIPHER_ONLY), "validKeyAgreementUsage"),
            Arguments.of(false, null, Set.of(), "keyUsages"));
    }

    @ParameterizedTest
    @MethodSource("validUsageCombinations")
    void acceptsValidBasicConstraintsAndUsage(
        boolean ca, Integer pathLength, Set<KeyUsage> usages) {
        assertThat(validator.validate(profile(ca, pathLength, usages, Set.of()))).isEmpty();
    }

    static Stream<Arguments> validUsageCombinations() {
        return Stream.of(
            Arguments.of(false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE)),
            Arguments.of(false, null, Set.of(KeyUsage.CRL_SIGN)),
            Arguments.of(true, 0, Set.of(KeyUsage.KEY_CERT_SIGN)),
            Arguments.of(true, 2, Set.of(KeyUsage.KEY_CERT_SIGN)),
            Arguments.of(true, null, Set.of(KeyUsage.KEY_CERT_SIGN)),
            Arguments.of(false, null, Set.of(KeyUsage.KEY_AGREEMENT, KeyUsage.ENCIPHER_ONLY)),
            Arguments.of(false, null, Set.of(KeyUsage.KEY_AGREEMENT, KeyUsage.DECIPHER_ONLY)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0.0", "0.39", "1.39", "1.2", "2.0", "2.40",
        "2.999999999999999999999999999999.1",
        "1.3.6.1.4.1.99999.1", "1.3.6.1.5.5.7.3.1"
    })
    void acceptsValidOidFormat(String oid) {
        assertThat(validator.validate(new ExtendedKeyUsageOid(oid))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ", "1", "3.1", "-1.2", "1.40", "0.40", "1.999999999999999999999",
        "1.-1", "1.2.", ".1.2", "1..2", "1.02.3", "1.2.x", " 1.2", "1.2 "
    })
    void rejectsInvalidOidFormat(String oid) {
        assertThat(validator.validate(new ExtendedKeyUsageOid(oid))).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("knownEkuCases")
    void checksKnownEkuCompatibilityThroughProfileValidation(
        String oid, KeyUsage compatible, KeyUsage incompatible) {
        Set<ExtendedKeyUsageOid> ekus = Set.of(new ExtendedKeyUsageOid(oid));
        assertThat(validator.validate(profile(false, null, Set.of(compatible), ekus))).isEmpty();
        assertThat(violations(profile(false, null, Set.of(incompatible), ekus)))
            .contains("validExtendedKeyUsages");
    }

    static Stream<Arguments> knownEkuCases() {
        return Stream.of(
            Arguments.of("1.3.6.1.5.5.7.3.1", KeyUsage.DIGITAL_SIGNATURE, KeyUsage.CRL_SIGN),
            Arguments.of("1.3.6.1.5.5.7.3.1", KeyUsage.KEY_ENCIPHERMENT, KeyUsage.CONTENT_COMMITMENT),
            Arguments.of("1.3.6.1.5.5.7.3.2", KeyUsage.KEY_AGREEMENT, KeyUsage.KEY_ENCIPHERMENT),
            Arguments.of("1.3.6.1.5.5.7.3.3", KeyUsage.DIGITAL_SIGNATURE, KeyUsage.CONTENT_COMMITMENT),
            Arguments.of("1.3.6.1.5.5.7.3.4", KeyUsage.KEY_ENCIPHERMENT, KeyUsage.CRL_SIGN),
            Arguments.of("1.3.6.1.5.5.7.3.8", KeyUsage.CONTENT_COMMITMENT, KeyUsage.KEY_ENCIPHERMENT),
            Arguments.of("1.3.6.1.5.5.7.3.9", KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_ENCIPHERMENT));
    }

    @Test
    void validatesEachEkuWithoutRequiringEqualUsageCounts() {
        Set<ExtendedKeyUsageOid> ekus = Set.of(
            new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.1"),
            new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.2"),
            new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.3"));
        assertThat(validator.validate(profile(false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE), ekus)))
            .isEmpty();
        assertThat(violations(profile(false, null, Set.of(KeyUsage.KEY_ENCIPHERMENT), ekus)))
            .contains("validExtendedKeyUsages");
    }

    @Test
    void allowsEkuOnCaAndAllowsCustomAndAnyEku() {
        assertThat(validator.validate(profile(true, null,
            Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.DIGITAL_SIGNATURE),
            Set.of(new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.1")))))
            .isEmpty();
        assertThat(validator.validate(profile(false, null, Set.of(KeyUsage.CRL_SIGN),
            Set.of(new ExtendedKeyUsageOid("1.3.6.1.4.1.99999.1"),
                new ExtendedKeyUsageOid("2.5.29.37.0")))))
            .isEmpty();
    }

    @Test
    void cascadesOidAndProfileIdValidation() {
        assertThat(violations(profile(false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE),
            Set.of(new ExtendedKeyUsageOid("1.40.1")))))
            .anyMatch(path -> path.endsWith("validOidFormat"));
        assertThat(violations(profile(new ProfileId(" "), 90, 365,
            false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE), Set.of())))
            .contains("id.value");
        assertThat(violations(profile(null, 90, 365,
            false, null, Set.of(KeyUsage.DIGITAL_SIGNATURE), Set.of())))
            .contains("id");
    }

    @Test
    void rejectsNullCollectionsAndElementsWithoutValidationExceptions() {
        assertThat(violations(profile(false, null, null, null)))
            .contains("keyUsages", "extendedKeyUsages");
        assertThat(validator.validate(profile(false, null,
            Collections.singleton(null), Collections.singleton(null))))
            .hasSize(2);
    }

    private static Set<String> violations(CertificateProfile profile) {
        return validator.validate(profile).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());
    }

    private static CertificateProfile profile(
        boolean ca, Integer pathLength, Set<KeyUsage> usages, Set<ExtendedKeyUsageOid> ekus) {
        return profile(new ProfileId("test"), 90, 365, ca, pathLength, usages, ekus);
    }

    private static CertificateProfile profile(
        ProfileId id, int defaultDays, int maxDays, boolean ca, Integer pathLength,
        Set<KeyUsage> usages, Set<ExtendedKeyUsageOid> ekus) {
        return new CertificateProfile(id, "test", "test profile", defaultDays, maxDays,
            new SubjectKeyPolicy(Set.of(new RsaKeyGenerationParameters(2048))),
            new SanPolicy(false, Set.of()), null, null, null, null, null,
            ca, pathLength, usages, ekus);
    }
}
