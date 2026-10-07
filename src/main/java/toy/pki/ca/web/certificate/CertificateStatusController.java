package toy.pki.ca.web.certificate;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateStatusService;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.domain.certificate.CertificateId;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki")
public class CertificateStatusController {

    private final BouncyCastleCertificateStatusService statusService;
    private final MyCertificateRepository repository;

    @GetMapping("/certificates/crl")
    public ResponseEntity<byte[]> crl(@RequestParam String issuerId, @RequestParam(defaultValue = "DER") CrlFormat format)
        throws GeneralSecurityException, IOException {
        CertificateId id = requireCertificate(issuerId);
        var crl = statusService.crl(id);
        byte[] bytes = crl.getEncoded();
        if (format == CrlFormat.PEM) bytes = ("-----BEGIN X509 CRL-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(bytes)
            + "\n-----END X509 CRL-----\n").getBytes(StandardCharsets.US_ASCII);
        return attachment(bytes, format == CrlFormat.DER ? "application/pkix-crl" : "application/x-pem-file",
            "crl-" + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(crl.getIssuerX500Principal().getEncoded()), 0, 8) + (format == CrlFormat.DER ? ".crl" : ".pem"));
    }

    @PostMapping("/certificates/{certificateId}/status-check")
    public String check(@PathVariable String certificateId, RedirectAttributes redirect) {
        CertificateId id = requireCertificate(certificateId);
        try {
            redirect.addFlashAttribute("ocspCheck", statusService.check(id));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("ocspError", e.getMessage());
        } catch (GeneralSecurityException | IOException e) {
            log.warn("OCSP check failed: certificateId={}, cause={}", certificateId, e.getClass().getSimpleName());
            redirect.addFlashAttribute("ocspError", "OCSP 응답을 검증하지 못했습니다. 발급 CA의 유효기간과 서명 키를 확인해 주세요.");
        }
        redirect.addAttribute("op", "ocsp");
        return "redirect:/pki/certificates/" + id.id();
    }

    @GetMapping("/certificates/{certificateId}/ocsp-request")
    public ResponseEntity<byte[]> request(@PathVariable String certificateId) throws GeneralSecurityException, IOException {
        return attachment(statusService.request(requireCertificate(certificateId)), "application/ocsp-request", "ocsp-request.der");
    }

    @PostMapping(value = "/ocsp/{issuerId}", consumes = "application/ocsp-request", produces = "application/ocsp-response")
    public ResponseEntity<byte[]> respond(@PathVariable String issuerId, HttpServletRequest request)
        throws GeneralSecurityException, IOException {
        byte[] response = statusService.respond(requireCertificate(issuerId), request.getInputStream().readNBytes(8193));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("application/ocsp-response")).body(response);
    }

    private CertificateId requireCertificate(String value) {
        CertificateId id = new CertificateId(value);
        if (repository.findById(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "인증서를 찾을 수 없습니다.");
        return id;
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String type, String filename) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString()).body(bytes);
    }

    public enum CrlFormat { PEM, DER }
}
