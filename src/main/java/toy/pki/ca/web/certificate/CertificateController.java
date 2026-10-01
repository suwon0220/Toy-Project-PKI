package toy.pki.ca.web.certificate;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.ca.domain.certificate.ManagedX509Certificate;
import toy.pki.ca.domain.certificate.ManagedX509CertificateService;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.domain.profile.ProfileService;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.web.certificate.dto.CertificateFilter;
import toy.pki.ca.web.certificate.dto.CertificateIssueForm;
import toy.pki.ca.web.certificate.dto.CertificatePreview;
import toy.pki.ca.web.certificate.dto.CertificateSummary;
import toy.pki.ca.web.certificate.dto.KeyMode;
import toy.pki.kms.domain.key.KeyGenerationService;
import toy.pki.kms.domain.key.KeyID;
import toy.pki.kms.domain.key.KeyManagingService;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.request.KeyGenerationRequest;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/certificates")
public class CertificateController {

    private final ManagedX509CertificateService managedX509CertificateService;
    private final KeyGenerationService keyGenerationService;
    private final ProfileService profileService;
    private final KeyManagingService keyManagingService;

    @ModelAttribute("sortDir")
    public String sortDir() {
        return "test";
    }

    @ModelAttribute ("filter")
    public CertificateFilter filter() {
        return new CertificateFilter();
    }

    @ModelAttribute ("profiles")
    public List<Profile> profiles() {
        return profileService.listProfilesByStatus(ProfileStatus.ACTIVE);
    }

    @ModelAttribute ("certificateIssueForm")
    public CertificateIssueForm certificateIssueForm() {
        return new CertificateIssueForm();
    }

    @GetMapping("")
    public String certificates(
        @RequestParam(required = false) @ModelAttribute("filter") CertificateFilter filter,
        @RequestParam(required = false) String mode,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size,
        @ModelAttribute CertificateIssueForm certificateIssueForm,
        Model model
    ) {
        log.info("Listing certificates with filter: {}, mode: {}, page: {}, size: {}", filter, mode, page, size);
        List<CertificateSummary> certificateSummaries = managedX509CertificateService.listCertificates().stream()
            .map(c -> new CertificateSummary(
                c.getCertificate().getSerialNumber().toString(),
                c.getCertificate().getSubjectX500Principal().toString()
            )).toList();

        model.addAttribute("size", size);
        model.addAttribute("page", page);
        model.addAttribute("sortField", "notAfter");
        model.addAttribute("sortDir", "asc");
        model.addAttribute("caList", List.of());
        model.addAttribute("statuses", List.of());
        model.addAttribute("totalCertificateCount", certificateSummaries.size());

        if (!"issue".equals(mode)) {
            model.asMap().remove("certificateIssueForm");
        }
        return "pki/certificates/list";
    }

    @PostMapping("/issue")
    public String issueCertificate(
        @Validated @ModelAttribute CertificateIssueForm certificateIssueForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) throws GeneralSecurityException {
        log.info("Issuing certificate with form: {}", certificateIssueForm);
        // TODO: remove direct dependency on Bounty Castle and implement certificate issuance logic in CertificateService
        if (bindingResult.hasErrors()) {
            log.info("Validation errors in certificate issue form: {}", bindingResult);
            return "pki/certificates/list";
        }

        Profile profile = profileService.getProfile(certificateIssueForm.getProfileId());

        Instant now = Instant.now();
        BigInteger serialNumber = new BigInteger(160, new SecureRandom()).abs();
        if (serialNumber.signum() == 0) {
            serialNumber = serialNumber.add(BigInteger.ONE);
        }

        Date notBefore = Date.from(now);
        int validityDays = certificateIssueForm.getValidityDays() != null ? certificateIssueForm.getValidityDays() : profile.getDefaultValidDays();
        Date notAfter = Date.from(now.plus(validityDays, ChronoUnit.DAYS));
        X500Name SubjectDn = certificateIssueForm.getSubjectDn().toX500Name();
        X500Name IssuerDn;

        if (CertType.ROOT_CA.equals(profile.getCertType())) {
            IssuerDn = SubjectDn;
        } else {
            ManagedX509Certificate issuerCertificate = managedX509CertificateService.findById(certificateIssueForm.getIssuerCertificateId());
            IssuerDn = X500Name.getInstance(issuerCertificate.getCertificate().getSubjectX500Principal());
        }

//        CertificatePreview certificatePreview = null;
////        CertificatePreview certificatePreview = new CertificatePreview();
////        certificatePreview.setCertType(profile.getCertType());
////        certificatePreview.setSubjectDn(SubjectDn.toString());
////        certificatePreview.setIssuerDn(IssuerDn.toString());
////        certificatePreview.setNotAfter(notAfter);
//
//        model.addAttribute("certificatePreview", certificatePreview);

        ManagedKey managedKey = null;
        if (KeyMode.NEW.equals(certificateIssueForm.getKeyMode())) {
            if (certificateIssueForm.getKeyAlgorithmPreset() == null) {
                bindingResult.rejectValue("keyAlgorithm", "NotEmpty");
                return "pki/certificates/list";
            }

            String managedKeyDisplayName = certificateIssueForm.getKeyAlias().isEmpty() ? "cert-" + serialNumber.toString() : certificateIssueForm.getKeyAlias();
            KeyGenerationRequest keyGenerationRequest = new KeyGenerationRequest(managedKeyDisplayName, certificateIssueForm.getKeyAlgorithmPreset());
            managedKey = keyGenerationService.generate(keyGenerationRequest);
        } else {
            if (certificateIssueForm.getKeyAlias() == null || certificateIssueForm.getKeyAlias().isEmpty()) {
                bindingResult.rejectValue("keyAlias", "NotEmpty");
                return "pki/certificates/list";
            }
            KeyID keyId = new KeyID(certificateIssueForm.getKmsKeyId());

            if (!keyManagingService.hasKey(keyId)) {
                bindingResult.rejectValue("kmsKeyId", "NotFound");
                return "pki/certificates/list";
            }
            PublicKey subjectPublicKey = keyManagingService.getPublicKey(keyId);
        }

        JcaX509v3CertificateBuilder jcaX509v3CertificateBuilder = new JcaX509v3CertificateBuilder(
            IssuerDn,
            serialNumber,
            notBefore,
            notAfter,
            SubjectDn,
            managedKey != null
            ? managedKey.getKeyPair().getPublic()
            : keyManagingService.getPublicKey(new KeyID(certificateIssueForm.getKmsKeyId()))
        );

        JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();

        if (KeyMode.NEW.equals(certificateIssueForm.getKeyMode()) && managedKey != null) {
            keyManagingService.saveKey(managedKey);
        }
        managedX509CertificateService.save(jcaX509v3CertificateBuilder, profile, managedKey != null ? managedKey.getId() : new KeyID(certificateIssueForm.getKmsKeyId()));
        return "redirect:/pki/certificates";
    }

    
}
