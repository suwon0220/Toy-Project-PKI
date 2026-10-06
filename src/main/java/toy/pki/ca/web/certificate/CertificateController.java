package toy.pki.ca.web.certificate;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.ca.application.profile.model.CreateProfileCommand;
import toy.pki.ca.application.profile.model.UpdateProfileCommand;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.SanPolicy;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeyPolicy;
import toy.pki.ca.web.profile.CreateProfileForm;
import toy.pki.ca.web.profile.UpdateProfileForm;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/certificates")
public class CertificateController {

    private final CertificateProfileService certificateProfileService;

    @ModelAttribute("profiles")
    public List<CertificateProfile> profiles() {
        return certificateProfileService.findAll().stream()
            .filter(profile -> !("DRAFT").equals(profile.getStatus().name()))
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
        @RequestParam(required = false) String profileId,
        @RequestParam(required = false) KeyMode keyMode,
        @RequestParam(required = false) String issuerCertificateId,
        Model model
    ) {
        if ("issue".equals(mode)) {
            CertificateIssueForm form = new CertificateIssueForm();
            form.setProfileId(new ProfileId(profileId));
            form.setIssuerCertificateId(new CertificateId(issuerCertificateId));
            form.setKeyMode(keyMode == null ? KeyMode.NEW : keyMode);

            model.addAttribute("certificateIssueForm", form);
            // profiles, issuerCerts, keyAlgorithms도 준비
        }

        return "pki/certificates/list";
    }

    @PostMapping("/issue")
    public String issueCertificate(
        @ModelAttribute("filter") CertificateFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "notAfter,desc") String sort,
        @RequestParam(required = false) String mode,
        @RequestParam(required = false) String profileId,
        @RequestParam(required = false) KeyMode keyMode,
        @RequestParam(required = false) String issuerCertificateId,
        Model model
    ) {
        if ("issue".equals(mode)) {
            CertificateIssueForm form = new CertificateIssueForm();
            form.setProfileId(new ProfileId(profileId));
            form.setIssuerCertificateId(new CertificateId(issuerCertificateId));
            form.setKeyMode(keyMode == null ? KeyMode.NEW : keyMode);

            model.addAttribute("certificateIssueForm", form);
            // profiles, issuerCerts, keyAlgorithms도 준비
        }

        return "pki/certificates/list";
    }
}
