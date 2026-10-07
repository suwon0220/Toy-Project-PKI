package toy.pki.ca.web.certificate;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/certificates")
public class CertificateController {

    private final KeyManagementService keyManagementService;
    private final CertificateProfileService certificateProfileService;

    @ModelAttribute("profiles")
    public List<CertificateProfile> profiles() {
        return certificateProfileService.findAll().stream()
            .filter(profile -> profile.getStatus() == ProfileStatus.ACTIVE)
            .collect(Collectors.toList());
    }

    @ModelAttribute
    public void formOptions(Model model) {
        model.addAttribute("keyAlgorithmPresets", KeyAlgorithmPreset.values());
        model.addAttribute("keyUsages", KeyUsage.values());
        model.addAttribute("sanTypes", SanType.values());
        Map<String, String> ekuOptions = new LinkedHashMap<>();
        ekuOptions.put("1.3.6.1.5.5.7.3.1", "serverAuth");
        ekuOptions.put("1.3.6.1.5.5.7.3.2", "clientAuth");
        ekuOptions.put("1.3.6.1.5.5.7.3.3", "codeSigning");
        ekuOptions.put("1.3.6.1.5.5.7.3.4", "emailProtection");
        ekuOptions.put("1.3.6.1.5.5.7.3.8", "timeStamping");
        ekuOptions.put("1.3.6.1.5.5.7.3.9", "OCSPSigning");
        model.addAttribute("extendedKeyUsageOptions", ekuOptions);
    }

    @GetMapping("")
    public String certificates(
        @ModelAttribute("filter") CertificateFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "notAfter,desc") String sort,
        @RequestParam(required = false) String mode,
        @RequestParam(required = false) KeyMode keyMode,
        @RequestParam(required = false) String profileId,
        @RequestParam(required = false) String issuerCertificateId,
        Model model
    ) {
        if ("issue".equals(mode)) {
            CertificateIssueForm form = prepareIssueForm(profileId, issuerCertificateId, keyMode);

            model.addAttribute("certificateIssueForm", form);
            // profiles, issuerCerts, keyAlgorithms도 준비
        }

        return "pki/certificates/list";
    }

    @PostMapping("/issue")
    public String issueCertificate(
        @RequestParam(required = false) String profileId,
        @RequestParam(required = false) String issuerCertificateId,
        @Validated @ModelAttribute CertificateIssueForm certificateIssueForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if(bindingResult.hasErrors()) {
            log.debug("Validation errors found: {}", bindingResult.getAllErrors());
            model.addAttribute("certificateIssueForm", certificateIssueForm);
            return "pki/certificates/list"; // 발급 폼으로 돌아가기
        }

        log.debug("Request to issue certificate with profileId: {}, issuerCertificateId: {}, form: {}", profileId, issuerCertificateId, certificateIssueForm);


        // If key mode is NEW, generate a new key pair and store it in KMS
        if (certificateIssueForm.getKeyMode() == KeyMode.NEW) {
            // Generate a new key pair and store it in KMS
            try {
                keyManagementService.generate(certificateIssueForm.getSubjectKeySpec()
                                                                  .name(), certificateIssueForm.getKeyAlias());
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        }

//        return "pki/certificates/"++"detail"; // 발급 후 상세 페이지로 리다이렉트
        return "pki/certificates/list";
    }

    private CertificateIssueForm prepareIssueForm(String profileId, String issuerCertificateId, KeyMode keyMode) {
        List<CertificateProfile> availableProfiles = profiles();
        CertificateProfile selectedProfile = availableProfiles.stream()
            .filter(profile -> profile.getId().value().equals(profileId))
            .findFirst()
            .orElse(availableProfiles.isEmpty() ? null : availableProfiles.get(0));

        CertificateIssueForm form = new CertificateIssueForm();
        form.setProfileId(selectedProfile == null ? null : selectedProfile.getId());
        if (selectedProfile != null && selectedProfile.getCertificateType() != CertificateType.ROOT_CA
            && issuerCertificateId != null && !issuerCertificateId.isBlank()) {
            form.setIssuerCertificateId(new CertificateId(issuerCertificateId));
        }
        form.setKeyMode(keyMode == null ? KeyMode.NEW : keyMode);
        return form;
    }
}
