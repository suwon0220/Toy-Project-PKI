package toy.pki.ca.web.profile;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import toy.pki.ca.application.model.CreateProfileCommand;
import toy.pki.ca.application.port.CertificateProfileRepository;
import toy.pki.ca.application.service.CertificateProfileService;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SanPolicy;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeyPolicy;
import toy.pki.kms.web.KeyAlgorithmPreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CertificateProfileService service;
    @Autowired private CertificateProfileRepository repository;

    @Test
    void rendersListAndNewFormWithoutCreatingProfiles() throws Exception {
        int before = repository.findAll().size();
        mockMvc.perform(get("/pki/profiles"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("profiles"));
        mockMvc.perform(get("/pki/profiles").param("mode", "new"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("createProfileForm"))
            .andExpect(content().string(containsString("name=\"defaultValidityDays\"")))
            .andExpect(content().string(containsString("name=\"keyUsages\"")))
            .andExpect(content().string(containsString("name=\"allowedSanTypes\"")))
            .andExpect(content().string(containsString("RSA_2048")));
        assertThat(repository.findAll()).hasSize(before);
    }

    @ParameterizedTest
    @EnumSource(ProfileStatus.class)
    void rendersExistingProfileAndItsSettings(ProfileStatus status) throws Exception {
        CertificateProfile profile = fixture();
        profile.setStatus(status);
        try {
            MvcResult result = mockMvc.perform(get("/pki/profiles")
                    .param("profileId", profile.getId().value()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("프로파일 편집")))
                .andExpect(content().string(containsString("value=\"730\"")))
                .andExpect(content().string(containsString("1.3.6.1.4.1.55555.1")))
                .andExpect(content().string(containsString("data-k-cert-type=\"CA\"")))
                .andExpect(content().string(containsString(status.name())))
                .andReturn();
            CreateProfileForm form = (CreateProfileForm) result.getModelAndView().getModel().get("createProfileForm");
            assertThat(form.getAlias()).isEqualTo(profile.getAlias());
            assertThat(form.getKeyAlgorithms()).containsExactly(KeyAlgorithmPreset.RSA_2048);
            assertThat(form.getAllowedSanTypes()).containsExactly(SanType.DNS_NAME);
            assertThat(form.getExtendedKeyUsageOids()).containsExactlyInAnyOrder(
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.4.1.55555.1");
            assertThat(form.isCa()).isTrue();
        } finally {
            repository.delete(profile.getId());
        }
    }

    @Test
    void preparesCopyWithEmptyAliasWithoutSavingIt() throws Exception {
        CertificateProfile profile = fixture();
        int before = repository.findAll().size();
        try {
            MvcResult result = mockMvc.perform(get("/pki/profiles")
                    .param("profileId", profile.getId().value()).param("mode", "new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("프로파일 복제")))
                .andReturn();
            CreateProfileForm form = (CreateProfileForm) result.getModelAndView().getModel().get("createProfileForm");
            assertThat(form.getAlias()).isNull();
            assertThat(form.getMaxValidityDays()).isEqualTo(730);
            assertThat(repository.findAll()).hasSize(before);
            assertThat(profile.getAlias()).isNotBlank();
        } finally {
            repository.delete(profile.getId());
        }
    }

    @Test
    void redisplaysInvalidFormAndDoesNotSave() throws Exception {
        int before = repository.findAll().size();
        mockMvc.perform(post("/pki/profiles/create")
                .param("alias", "invalid-profile")
                .param("maxValidityDays", "730")
                .param("subjectOrganization", "Keep this input"))
            .andExpect(status().isOk())
            .andExpect(view().name("pki/profiles/index"))
            .andExpect(model().attributeHasFieldErrors("createProfileForm",
                "defaultValidityDays", "keyAlgorithms", "keyUsages"))
            .andExpect(content().string(containsString("새 프로파일")))
            .andExpect(content().string(containsString("value=\"invalid-profile\"")))
            .andExpect(content().string(containsString("value=\"Keep this input\"")));
        assertThat(repository.findAll()).hasSize(before);
    }

    @Test
    void createsDraftAndRedirectsWithActualId() throws Exception {
        String alias = "profile-" + UUID.randomUUID();
        ProfileId createdId = null;
        try {
            MvcResult result = mockMvc.perform(post("/pki/profiles/create")
                    .param("alias", alias)
                    .param("defaultValidityDays", "90")
                    .param("maxValidityDays", "365")
                    .param("keyAlgorithms", "EC_P256")
                    .param("keyUsages", "DIGITAL_SIGNATURE")
                    .param("sanRequired", "true")
                    .param("allowedSanTypes", "DNS_NAME", "IP_ADDRESS")
                    .param("extendedKeyUsageOids", "1.3.6.1.5.5.7.3.1", "1.3.6.1.4.1.55555.1", "")
                    .param("subjectOrganization", "Example"))
                .andExpect(status().is3xxRedirection()).andReturn();
            CertificateProfile created = repository.findAll().stream()
                .filter(profile -> alias.equals(profile.getAlias())).findFirst().orElseThrow();
            createdId = created.getId();
            assertThat(created.getStatus()).isEqualTo(ProfileStatus.DRAFT);
            assertThat(created.getSubjectKeyPolicy().allows(KeyAlgorithmPreset.EC_P256.toParameters())).isTrue();
            assertThat(created.getSanPolicy().required()).isTrue();
            assertThat(created.getSanPolicy().allowedTypes()).containsExactlyInAnyOrder(SanType.DNS_NAME, SanType.IP_ADDRESS);
            assertThat(created.getExtendedKeyUsages()).containsExactlyInAnyOrder(
                new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.1"), new ExtendedKeyUsageOid("1.3.6.1.4.1.55555.1"));
            assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/pki/profiles?profileId=" + createdId.value());
            mockMvc.perform(get(URI.create(result.getResponse().getRedirectedUrl())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(alias)))
                .andExpect(content().string(containsString("프로파일 편집")));
        } finally {
            if (createdId != null) {
                repository.delete(createdId);
            }
        }
    }

    @Test
    void returnsNotFoundForMissingProfile() throws Exception {
        mockMvc.perform(get("/pki/profiles").param("profileId", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    private CertificateProfile fixture() {
        ProfileId id = service.createDraft(new CreateProfileCommand(
            "profile-" + UUID.randomUUID(), "Description", 90, 730,
            new SubjectKeyPolicy(Set.of(KeyAlgorithmPreset.RSA_2048.toParameters())),
            new SanPolicy(true, Set.of(SanType.DNS_NAME)),
            "Example", "Unit", "Seoul", "Seoul", "KR", true, 0,
            Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.DIGITAL_SIGNATURE),
            Set.of(new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.1"), new ExtendedKeyUsageOid("1.3.6.1.4.1.55555.1"))));
        return service.findById(id.value());
    }
}
