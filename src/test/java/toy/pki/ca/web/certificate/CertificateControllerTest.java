package toy.pki.ca.web.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.DataBinder;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.SubjectKeySpec;

@WebMvcTest(CertificateController.class)
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CertificateProfileService certificateProfileService;

    @Test
    void bindsSubjectDnFieldsAsInputValues() {
        CertificateIssueForm form = new CertificateIssueForm();
        DataBinder binder = new DataBinder(form, "certificateIssueForm");
        binder.bind(new MutablePropertyValues(Map.of(
            "subjectDn.domainComponent", "example",
            "subjectDn.commonName", "root.example.com",
            "subjectDn.organization", "Example",
            "subjectDn.organizationUnit", "PKI",
            "subjectDn.country", "KR"
        )));

        assertThat(binder.getBindingResult().getAllErrors()).isEmpty();
        assertThat(form.getSubjectDn().getDomainComponent()).isEqualTo("example");
        assertThat(form.getSubjectDn().getCommonName()).isEqualTo("root.example.com");
        assertThat(form.getSubjectDn().getOrganization()).isEqualTo("Example");
        assertThat(form.getSubjectDn().getOrganizationUnit()).isEqualTo("PKI");
        assertThat(form.getSubjectDn().getCountry()).isEqualTo("KR");
    }

    @ParameterizedTest
    @EnumSource(KeyMode.class)
    void rendersIssueDialogWithoutProfiles(KeyMode keyMode) throws Exception {
        when(certificateProfileService.findAll()).thenReturn(List.of());

        MvcResult result = mockMvc.perform(get("/pki/certificates")
                    .param("mode", "issue")
                    .param("keyMode", keyMode.name()))
               .andExpect(status().isOk())
               .andExpect(view().name("pki/certificates/list"))
               .andExpect(content().string(containsString("name=\"validityDays\"")))
               .andExpect(content().string(containsString("name=\"notBefore\"")))
               .andExpect(content().string(containsString("name=\"subjectDn.commonName\"")))
               .andExpect(content().string(containsString(keyMode == KeyMode.NEW
                   ? "name=\"subjectKeySpec\"" : "name=\"kmsKeyId\"")))
               .andReturn();

        Matcher subjectInputs = Pattern.compile("<input\\b[^>]*\\bname=\"subjectDn\\.([^\"]+)\"[^>]*>")
            .matcher(result.getResponse().getContentAsString());
        assertThat(subjectInputs.results().map(match -> match.group(1)).toList())
            .containsExactlyInAnyOrder("domainComponent", "commonName", "organizationUnit", "organization", "country");
    }

    @Test
    void rendersFixedDomainComponentInRootSubjectAndIssuer() throws Exception {
        CertificateProfile root = profile(CertificateType.ROOT_CA, ProfileStatus.ACTIVE);
        root.setDnPolicy(new DnPolicy(
            new DnAttributePolicy(true, "V2G"),
            new DnAttributePolicy(true, "root.example.com"),
            new DnAttributePolicy(false),
            new DnAttributePolicy("Example"),
            new DnAttributePolicy("KR")));
        when(certificateProfileService.findAll()).thenReturn(List.of(root));

        MvcResult result = mockMvc.perform(get("/pki/certificates").param("mode", "issue"))
            .andExpect(status().isOk())
            .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(tag(html, "<input\\b[^>]*\\bid=\"subjectDn\\.domainComponent\"[^>]*>"))
            .contains("value=\"V2G\"", "readonly=\"readonly\"", "required=\"required\"");
        String expectedDn = "DC=V2G, CN=root.example.com, O=Example, C=KR";
        assertThat(Pattern.compile(Pattern.quote(expectedDn)).matcher(html).results().count())
            .isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(CertificateType.class)
    void issuerSelectionDependsOnProfileType(CertificateType type) throws Exception {
        CertificateProfile profile = profile(type, ProfileStatus.ACTIVE);
        when(certificateProfileService.findAll()).thenReturn(List.of(profile));

        MvcResult result = mockMvc.perform(get("/pki/certificates")
                .param("mode", "issue")
                .param("profileId", profile.getId().value())
                .param("issuerCertificateId", "previous-issuer"))
            .andExpect(status().isOk())
            .andReturn();

        String html = result.getResponse().getContentAsString();
        String issuerSelect = tag(html, "<select\\b[^>]*\\bid=\"issuerCertificateId\"[^>]*>");
        String issueButton = tag(html, "<button\\b[^>]*>인증서 발급</button>");
        CertificateIssueForm form = (CertificateIssueForm) result.getModelAndView()
            .getModel().get("certificateIssueForm");
        assertThat(form.getProfileId()).isEqualTo(profile.getId());

        if (type == CertificateType.ROOT_CA) {
            assertThat(issuerSelect).contains("disabled=\"disabled\"").doesNotContain("required=");
            assertThat(issueButton).doesNotContain("disabled=");
            assertThat(form.getIssuerCertificateId()).isNull();
            assertThat(html).contains("자체 서명 (발급 CA 불필요)")
                .doesNotContain("선택할 발급 CA 인증서가 없습니다.");
        } else {
            assertThat(issuerSelect).contains("required=\"required\"").doesNotContain("disabled=");
            assertThat(issueButton).contains("disabled=\"disabled\"");
            assertThat(form.getIssuerCertificateId().id()).isEqualTo("previous-issuer");
            assertThat(html).contains("선택할 발급 CA 인증서가 없습니다.");
        }
    }

    @Test
    void openingIssueDialogSelectsFirstActiveProfile() throws Exception {
        CertificateProfile draft = profile(CertificateType.END_ENTITY, ProfileStatus.DRAFT);
        CertificateProfile root = profile(CertificateType.ROOT_CA, ProfileStatus.ACTIVE);
        when(certificateProfileService.findAll()).thenReturn(List.of(draft, root));

        MvcResult result = mockMvc.perform(get("/pki/certificates").param("mode", "issue"))
            .andExpect(status().isOk())
            .andReturn();

        CertificateIssueForm form = (CertificateIssueForm) result.getModelAndView()
            .getModel().get("certificateIssueForm");
        assertThat(form.getProfileId()).isEqualTo(root.getId());
        assertThat(result.getResponse().getContentAsString())
            .contains("value=\"" + root.getId().value() + "\"")
            .doesNotContain("value=\"" + draft.getId().value() + "\"");
    }

    @Test
    void submittedRootCaFormDiscardsIssuer() throws Exception {
        CertificateProfile root = profile(CertificateType.ROOT_CA, ProfileStatus.ACTIVE);
        when(certificateProfileService.findAll()).thenReturn(List.of(root));

        MvcResult result = mockMvc.perform(post("/pki/certificates/issue")
                .param("mode", "issue")
                .param("profileId", root.getId().value())
                .param("issuerCertificateId", "previous-issuer"))
            .andExpect(status().isOk())
            .andReturn();

        CertificateIssueForm form = (CertificateIssueForm) result.getModelAndView()
            .getModel().get("certificateIssueForm");
        assertThat(form.getIssuerCertificateId()).isNull();
    }

    private CertificateProfile profile(CertificateType type, ProfileStatus status) {
        return new CertificateProfile(
            new ProfileId(type.name() + "-" + status.name()), type.name(), null, status, 365, 730,
            new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)),
            Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256),
            new SanPolicy(false, Set.of()), null, type, null,
            Set.of(KeyUsage.DIGITAL_SIGNATURE), Set.of());
    }

    private String tag(String html, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(html);
        assertThat(matcher.find()).as("HTML tag matching %s", expression).isTrue();
        return matcher.group();
    }
}
