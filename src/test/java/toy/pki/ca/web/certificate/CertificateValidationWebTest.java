package toy.pki.ca.web.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateStatusService;
import toy.pki.ca.application.certificate.model.CertificateValidationResult;
import toy.pki.ca.application.certificate.model.CertificateValidationResult.Reason;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.application.certificate.service.CertificateValidationService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.profile.ProfileId;

class CertificateValidationWebTest {

    private final CertificateId id = new CertificateId(UUID.randomUUID().toString());
    private final MyCertificateRepository repository = mock(MyCertificateRepository.class);
    private final CertificateValidationService validation = mock(CertificateValidationService.class);
    private final X509Certificate certificate = mock(X509Certificate.class);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void postsValidationAndRedirectsToItsOwnTabWithTheResult(boolean valid) throws Exception {
        var result = result(valid);
        when(repository.findById(id)).thenReturn(Optional.of(managed()));
        when(validation.validate(id)).thenReturn(result);
        var controller = new CertificateStatusController(mock(BouncyCastleCertificateStatusService.class),
            repository, validation);

        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(post("/pki/certificates/" + id.id() + "/validate"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/pki/certificates/" + id.id() + "?op=validate"))
            .andExpect(flash().attribute("validationCheck", result));
        verify(validation).validate(id);
    }

    @Test
    void missingTargetReturnsNotFoundWithoutRunningValidation() throws Exception {
        when(repository.findById(id)).thenReturn(Optional.empty());
        var controller = new CertificateStatusController(mock(BouncyCastleCertificateStatusService.class),
            repository, validation);
        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(post("/pki/certificates/" + id.id() + "/validate"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(validation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersRealTemplateForValidAndRevokedAncestorResults(boolean valid) {
        String html = render(result(valid));
        assertThat(html).contains("체인 유효성 검증", "/validate", "저장소의 자체 서명 루트 CA");
        assertThat(html).contains(valid ? "관리 체인이 유효합니다." : "관리 체인이 유효하지 않습니다.");
        assertThat(html).contains(valid ? "체인 검증 성공" : "폐지된 인증서입니다: Intermediate 1");
        if (!valid) {
            assertThat(html).contains("원인 인증서 보기");
        }
    }

    @Test
    void rendersInitialValidationTabWithoutAResult() {
        assertThat(render(null)).contains("체인 유효성 검증").doesNotContain("data-validation-result");
    }

    private String render(CertificateValidationResult result) {
        when(certificate.getNotAfter()).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        request.setRequestURI("/pki/certificates/" + id.id());
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        var context = new WebContext(application.buildExchange(request, new MockHttpServletResponse()), Locale.KOREAN);
        context.setVariable("cert", new CertificateView(managed(), "Test", ZoneId.of("Asia/Seoul")));
        context.setVariable("detailTab", "validate");
        context.setVariable("displayTimeZone", "Asia/Seoul");
        context.setVariable("validationCheck", result);
        return engine.process("pki/certificates/detail", context);
    }

    private MyCertificate managed() {
        var managed = new MyCertificate(id, new ProfileId("test"), certificate);
        managed.setAlias("Leaf");
        return managed;
    }

    private CertificateValidationResult result(boolean valid) {
        return new CertificateValidationResult(valid ? Reason.VALID : Reason.REVOKED,
            valid ? "체인 검증 성공" : "폐지된 인증서입니다: Intermediate 1",
            valid ? null : id, List.of(id), Instant.now());
    }
}
