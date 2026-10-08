package toy.pki.ca.web.certificate;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import toy.pki.ca.application.certificate.service.MyCertificateService;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSerialNumber;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/certificates")
public class CertificateController {

    private final MyCertificateService certificateService;
    private final KeyManagementService keyManagementService;
    private final CertificateProfileService certificateProfileService;
    @Value("${pki.time-zone:Asia/Seoul}") private ZoneId zone;

    @ModelAttribute("profiles")
    public List<CertificateProfile> profiles() {
        return certificateProfileService.findAll().stream()
                                        .filter(profile -> profile.getStatus() == ProfileStatus.ACTIVE).toList();
    }

    @ModelAttribute("keyAlgorithmPresets")
    public KeyAlgorithmPreset[] keyAlgorithmPresets() {
        return KeyAlgorithmPreset.values();
    }

    @GetMapping({"", "/{certificateId}"})
    public String certificates(
        @PathVariable(required = false) String certificateId,
        @ModelAttribute("filter") CertificateFilter filter,
        @ModelAttribute("issueFormInput") CertificateIssueForm input,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "notAfter,desc") String sort,
        @RequestParam(required = false) String mode,
        @RequestParam(defaultValue = "info") String op,
        Model model) {
        list(model, filter, page, size, sort);
        model.addAttribute("detailTab", List.of("info", "download", "ocsp", "validate", "revoke").contains(op)
            ? op : "info");
        model.addAttribute("crlDialog", "crl".equals(mode));
        if (certificateId != null) {
            model.addAttribute("cert", view(find(certificateId)));
        }
        if ("issue".equals(mode)) {
            CertificateProfile profile = input.getProfileId() == null ? profiles().stream().findFirst().orElse(null)
                                                                      : selectedProfile(input);
            if (profile != null) {
                input.setProfileId(profile.getId());
                if (profile.getCertificateType() == CertificateType.ROOT_CA) {
                    input.setIssuerCertificateId(null);
                }
                if (input.getSubjectKeySpec() == null && profile.getSubjectKeyPolicy() != null) {
                    input.setSubjectKeySpec(profile.getSubjectKeyPolicy()
                                                   .allowedKeys()
                                                   .stream()
                                                   .sorted()
                                                   .findFirst()
                                                   .orElse(null));
                }
            }
            issueOptions(model, input);
        }
        return "pki/certificates/list";
    }

    @PostMapping("/issue")
    public String issueCertificate(
        @Valid @ModelAttribute("certificateIssueForm") CertificateIssueForm form,
        BindingResult errors, Model model, RedirectAttributes redirect, HttpSession session)
        throws GeneralSecurityException, IOException {
        CertificateProfile profile = selectedProfile(form);
        if (profile != null && profile.getCertificateType() == CertificateType.ROOT_CA) {
            form.setIssuerCertificateId(null);
        }
        validate(form, profile, errors);
        if (!errors.hasErrors()) {
            synchronized (session) {
                try {
                    KeyId keyId;
                    if (form.getKeyMode() == KeyMode.NEW) {
                        CertificateSerialNumber serial = CertificateSerialNumber.generate();
                        ManagedKey key = keyManagementService.generate(form.getSubjectKeySpec()
                                                                           .name(), "cert-" + serial.hex());
                        keyId = key.getKeyId();
                        session.setAttribute("certificateSerial:" + keyId.value(), serial);
                        form.setKmsKeyId(keyId.value());
                        form.setKeyMode(KeyMode.EXISTING);
                    } else {
                        ManagedKey key = keyManagementService.findAll()
                                                             .stream()
                                                             .filter(candidate -> candidate.getKeyId()
                                                                                           .value()
                                                                                           .equals(form.getKmsKeyId()))
                                                             .findFirst()
                                                             .orElseThrow(() -> new IllegalArgumentException("선택한 KMS 키를 찾을 수 없습니다."));
                        if (!profile.getSubjectKeyPolicy()
                                    .allows(SubjectKeySpec.valueOf(key.getKeyAlgorithmPreset().name()))) {
                            throw new IllegalArgumentException("프로파일이 허용하지 않는 키입니다.");
                        }
                        keyId = key.getKeyId();
                    }
                    String serialAttribute = "certificateSerial:" + keyId.value();
                    CertificateSerialNumber serial = (CertificateSerialNumber) session.getAttribute(serialAttribute);
                    CertificateId id = certificateService.issue(form.toCommand(keyId, zone), serial == null
                                                                                             ? CertificateSerialNumber.generate()
                                                                                             : serial);
                    session.removeAttribute(serialAttribute);
                    redirect.addFlashAttribute("flashSuccess", "인증서를 발급했습니다.");
                    return "redirect:/pki/certificates/" + id.id();
                } catch (IllegalArgumentException e) {
                    errors.reject("certificate.issue.failed", new Object[]{e.getMessage()}, null);
                }
            }
        }
        list(model, new CertificateFilter(), 0, 20, "notAfter,desc");
        issueOptions(model, form);
        return "pki/certificates/list";
    }

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> download(
        @PathVariable String certificateId,
        @RequestParam(defaultValue = "PEM") DownloadFormat format) throws GeneralSecurityException {
        X509Certificate certificate = (X509Certificate) find(certificateId).getCertificate();
        byte[] bytes = format == DownloadFormat.DER ? certificate.getEncoded()
                                                    : CertificateView.pem(certificate)
                                                                     .getBytes(StandardCharsets.US_ASCII);
        String suffix = format == DownloadFormat.DER ? ".der" : ".pem";
        String contentType = format == DownloadFormat.DER ? "application/pkix-cert" : "application/x-pem-file";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                             .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                                                                        .filename(certificate.getSerialNumber()
                                                                                                             .toString(16) + suffix)
                                                                                        .build()
                                                                                        .toString()).body(bytes);
    }

    @PostMapping("/{certificateId}/revoke")
    public String revoke(
        @PathVariable String certificateId, @RequestParam(defaultValue = "") String confirmSerial,
        RedirectAttributes redirect) {
        CertificateView certificate = view(find(certificateId));
        String serial = certificate.getSerial();
        if (!serial.substring(Math.max(0, serial.length() - 6)).equalsIgnoreCase(confirmSerial.strip())) {
            redirect.addFlashAttribute("revokeError", "시리얼 끝 6자리가 일치하지 않습니다.");
            redirect.addAttribute("op", "revoke");
        } else {
            certificateService.revoke(certificateId);
            redirect.addFlashAttribute("flashSuccess", "인증서를 폐기했습니다.");
        }
        return "redirect:/pki/certificates/" + certificateId;
    }

    private void list(Model model, CertificateFilter filter, int page, int size, String sort) {
        Map<String, String> names = profileNames();
        List<CertificateView> all = certificateService.findAll().stream()
                                                      .map(certificate -> view(certificate, names)).toList();
        String[] ordering = sort.split(",", -1);
        String field = ordering[0];
        String direction = ordering.length == 2 ? ordering[1] : "asc";
        if (ordering.length > 2 || !(direction.equals("asc") || direction.equals("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort");
        }
        Comparator<CertificateView> comparator = switch (field) {
            case "title" -> Comparator.comparing(CertificateView::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "subjectCn" -> Comparator.comparing(CertificateView::getSubjectCn, String.CASE_INSENSITIVE_ORDER);
            case "profileName" -> Comparator.comparing(CertificateView::getProfileName, String.CASE_INSENSITIVE_ORDER);
            case "notAfter" -> Comparator.comparing(CertificateView::getNotAfter);
            case "status" -> Comparator.comparing(certificate -> certificate.getStatus().name());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field");
        };
        if (direction.equals("desc")) {
            comparator = comparator.reversed();
        }
        String query = filter.getQ() == null ? null : filter.getQ().strip().toLowerCase(Locale.ROOT);
        List<CertificateView> filtered = all.stream()
                                            .filter(certificate -> query == null || query.isBlank()
                                                || (certificate.getSerial() + " " + certificate.getSubjectCn() + " " + certificate.getAlias())
                                                .toLowerCase(Locale.ROOT).contains(query))
                                            .filter(certificate -> filter.getStatus() == null || certificate.getStatus() == filter.getStatus())
                                            .filter(certificate -> filter.getProfileName() == null || filter.getProfileName()
                                                                                                            .isBlank()
                                                || certificate.getProfileName().equals(filter.getProfileName()))
                                            .sorted(comparator.thenComparing(certificate -> certificate.getId().id()))
                                            .toList();
        int number = Math.clamp(Math.ceilDiv(filtered.size(), size) - 1, 0, page);
        int start = number * size;
        model.addAttribute("page", new CertificatePage(filtered.subList(start, Math.min(start + size, filtered.size())), number, size, filtered.size()));
        model.addAttribute("filter", filter);
        model.addAttribute("totalCertificateCount", all.size());
        model.addAttribute("statuses", CertificateStatus.values());
        model.addAttribute("profileNames", all.stream()
                                              .map(CertificateView::getProfileName)
                                              .distinct()
                                              .sorted()
                                              .toList());
        model.addAttribute("sortField", field);
        model.addAttribute("sortDir", direction);
        model.addAttribute("displayTimeZone", zone.getId());
        model.addAttribute("crlIssuers", all.stream().filter(CertificateView::isCrlEligible)
                                            .sorted(Comparator.comparing(CertificateView::getTitle)).toList());
    }

    private void issueOptions(Model model, CertificateIssueForm form) {
        model.addAttribute("certificateIssueForm", form);
        model.addAttribute("displayTimeZone", zone.getId());
        CertificateProfile selected = selectedProfile(form);
        Map<String, String> names = profileNames();
        model.addAttribute("issuerCerts", certificateService.findAll()
                                                            .stream()
                                                            .map(certificate -> view(certificate, names))
                                                            .filter(CertificateView::isIssuerEligible)
                                                            .filter(certificate -> selected == null || selected.getCertificateType() != CertificateType.INTERMEDIATE_CA
                                                                || certificate.getPathLength() > 0)
                                                            .toList());
        if (form.getKeyMode() == KeyMode.EXISTING) {
            model.addAttribute("kmsKeys", keyManagementService.findAll());
        }
    }

    private void validate(CertificateIssueForm form, CertificateProfile profile, BindingResult errors) {
        if (profile == null) {
            if (!errors.hasFieldErrors("profileId")) {
                errors.rejectValue("profileId", "certificate.profile.required");
            }
            return;
        }
        if (form.getKeyMode() == KeyMode.NEW && (form.getSubjectKeySpec() == null
            || profile.getSubjectKeyPolicy() == null || !profile.getSubjectKeyPolicy()
                                                                .allows(form.getSubjectKeySpec()))) {
            errors.rejectValue("subjectKeySpec", "certificate.key.notAllowed");
        }
        if (form.getKeyMode() == KeyMode.EXISTING && (form.getKmsKeyId() == null || form.getKmsKeyId().isBlank())) {
            errors.rejectValue("kmsKeyId", "certificate.key.required");
        }
        if (profile.getCertificateType() != CertificateType.ROOT_CA && form.getIssuerCertificateId() == null) {
            errors.rejectValue("issuerCertificateId", "certificate.issuer.required");
        }
        if (form.getValidityDays() != null && form.getValidityDays() > profile.getMaxValidityDays()) {
            errors.rejectValue("validityDays", "certificate.validity.max", new Object[]{profile.getMaxValidityDays()}, null);
        }
        var dn = profile.getDnPolicy();
        var subject = form.getSubjectDn();
        if (dn != null && subject != null) {
            requiredDn(errors, "domainComponent", dn.domainComponent(), subject.getDomainComponent());
            requiredDn(errors, "commonName", dn.commonName(), subject.getCommonName());
            requiredDn(errors, "organization", dn.organization(), subject.getOrganization());
            requiredDn(errors, "organizationUnit", dn.organizationUnit(), subject.getOrganizationUnit());
            requiredDn(errors, "country", dn.country(), subject.getCountry());
        }
    }

    private void requiredDn(BindingResult errors, String field, DnAttributePolicy policy, String value) {
        if (policy != null && policy.required() && (policy.fixedValue() == null || policy.fixedValue()
                                                                                         .isBlank())
            && (value == null || value.isBlank())) {
            errors.rejectValue("subjectDn." + field, "certificate.dn.required");
        }
    }

    private CertificateProfile selectedProfile(CertificateIssueForm form) {
        return profiles().stream()
                         .filter(profile -> profile.getId().equals(form.getProfileId()))
                         .findFirst()
                         .orElse(null);
    }

    private MyCertificate find(String id) {
        try {
            return certificateService.findById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "인증서를 찾을 수 없습니다.", e);
        }
    }

    private Map<String, String> profileNames() {
        return certificateProfileService.findAll().stream().collect(Collectors.toMap(profile -> profile.getId().value(),
            profile -> profile.getAlias() == null ? profile.getId().value() : profile.getAlias()));
    }

    private CertificateView view(MyCertificate certificate) {
        return view(certificate, profileNames());
    }

    private CertificateView view(MyCertificate certificate, Map<String, String> names) {
        return new CertificateView(certificate, names.getOrDefault(certificate.getProfileId()
                                                                              .value(), certificate.getProfileId()
                                                                                                   .value()), zone);
    }

    public enum DownloadFormat {
        PEM,
        DER
    }
}
