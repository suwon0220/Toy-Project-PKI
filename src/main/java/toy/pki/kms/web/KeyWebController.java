package toy.pki.kms.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.kms.application.model.KeyGenerationRequest;
import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.domain.SignatureAlgorithm;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/kms")
public class KeyWebController {

    private final KeyManagementService keyManagementService;

    @ModelAttribute("algorithms")
    public KeyAlgorithm[] algorithms() {
        return KeyAlgorithm.values();
    }

    @ModelAttribute("keyAlgorithmPresets")
    public KeyAlgorithmPreset[] keyAlgorithmPresets() {
        return KeyAlgorithmPreset.values();
    }

    @ModelAttribute("signatureAlgorithms")
    public SignatureAlgorithm[] signatureAlgorithms() {
        return new SignatureAlgorithm[]{
            SignatureAlgorithm.RSA_PKCS1_V1_5, SignatureAlgorithm.ECDSA,
            SignatureAlgorithm.Ed25519, SignatureAlgorithm.Ed448
        };
    }

    @ModelAttribute("hashAlgorithms")
    public HashAlgorithm[] hashAlgorithms() {
        return HashAlgorithm.values();
    }

    @ModelAttribute("keys")
    public List<ManagedKey> keys(@ModelAttribute KeyQuery keyQuery) {
        return keyManagementService.search(keyQuery.toCriteria());
    }

    @GetMapping("")
    public String keys(@ModelAttribute KeyGenerationRequest request) {
        return "pki/kms/index";
    }

    // 키 생성
    @PostMapping("")
    public String saveKey(
        @Validated @ModelAttribute KeyGenerationRequest keyGenerationRequest,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "pki/kms/index";
        }

        keyManagementService.generate(
            keyGenerationRequest.keyAlgorithmPreset().name(), keyGenerationRequest.alias());
        redirectAttributes.addFlashAttribute("flashSuccess", "Key generated successfully.");
        return "redirect:/pki/kms";
    }

    @PostMapping("/keys/{keyId}/delete")
    public String delete(
        @PathVariable KeyId keyId,
        @RequestParam String confirmKeyId,
        RedirectAttributes redirectAttributes) {
        if (!keyId.value().equals(confirmKeyId.strip())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "키 ID 확인값이 일치하지 않습니다");
        }

        keyManagementService.delete(keyId);
        redirectAttributes.addFlashAttribute("flashSuccess", "키를 삭제했습니다.");
        return "redirect:/pki/kms";
    }

    @PostMapping("keys/{keyId}/sign")
    public String sign(
        @PathVariable KeyId keyId,
        @RequestParam SignatureAlgorithm signatureAlgorithm,
        @RequestParam(required = false) HashAlgorithm hashAlgorithm,
        @RequestParam DataEncoding encoding,
        @RequestParam String data,
        RedirectAttributes redirectAttributes) throws GeneralSecurityException {
        byte[] bytes;
        try {
            bytes = switch (encoding) {
                case TEXT -> data.getBytes(StandardCharsets.UTF_8);
                case BASE64 -> Base64.getDecoder().decode(data.strip());
                case HEX -> HexFormat.of().parseHex(data.strip());
            };
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "입력 데이터의 인코딩 형식이 올바르지 않습니다",
                e);
        }

        byte[] signature = keyManagementService.sign(keyId, signatureAlgorithm, hashAlgorithm, bytes);

        HashAlgorithm dataHashAlgorithm = switch (signatureAlgorithm) {
            case Ed25519, Ed448 -> HashAlgorithm.SHA256;
            default -> hashAlgorithm;
        };
        String dataHash = HexFormat.of()
                                   .formatHex(
                                       MessageDigest.getInstance(dataHashAlgorithm.getAlgorithmName()).digest(bytes));

        String signatureBase64 = Base64.getEncoder().encodeToString(signature);
        redirectAttributes.addFlashAttribute(
            "signResult",
            Map.of(
                "dataHashAlgorithm", dataHashAlgorithm.getAlgorithmName(),
                "dataHashHex", dataHash,
                "signatureBase64", signatureBase64));
        redirectAttributes.addFlashAttribute("signatureForm", signatureFormData(
            keyId, signatureAlgorithm, hashAlgorithm, encoding, data, signatureBase64));

        redirectAttributes.addFlashAttribute("flashSuccess", "Data signed successfully.");

        redirectAttributes.addAttribute("opKeyId", keyId.value());
        redirectAttributes.addAttribute("op", "sign");

        return "redirect:/pki/kms";
    }

    @PostMapping("/keys/{keyId}/verify/prepare")
    public String prepareVerify(
        @PathVariable KeyId keyId,
        @RequestParam SignatureAlgorithm signatureAlgorithm,
        @RequestParam(required = false) HashAlgorithm hashAlgorithm,
        @RequestParam DataEncoding encoding,
        @RequestParam String data,
        @RequestParam String signature,
        @RequestParam(required = false) KeyAlgorithm keyAlgorithm,
        @RequestParam(required = false) String keyword,
        RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("signatureForm", signatureFormData(
            keyId, signatureAlgorithm, hashAlgorithm, encoding, data, signature));
        redirectAttributes.addAttribute("opKeyId", keyId.value());
        redirectAttributes.addAttribute("op", "verify");
        if (keyAlgorithm != null) {
            redirectAttributes.addAttribute("keyAlgorithm", keyAlgorithm);
        }
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        return "redirect:/pki/kms";
    }

    @PostMapping("/keys/{keyId}/verify")
    public String verify(
        @PathVariable KeyId keyId,
        @RequestParam SignatureAlgorithm signatureAlgorithm,
        @RequestParam(required = false) HashAlgorithm hashAlgorithm,
        @RequestParam DataEncoding encoding,
        @RequestParam String data,
        @RequestParam String signature,
        RedirectAttributes redirectAttributes) {
        byte[] dataBytes;
        byte[] signatureBytes;

        try {
            dataBytes = switch (encoding) {
                case TEXT -> data.getBytes(StandardCharsets.UTF_8);
                case BASE64 -> Base64.getDecoder().decode(data.strip());
                case HEX -> HexFormat.of().parseHex(data.strip());
            };

            signatureBytes = Base64.getDecoder().decode(signature.strip());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "원본 데이터 또는 서명의 인코딩 형식이 올바르지 않습니다",
                e);
        }

        boolean valid = keyManagementService.verify(
            keyId,
            signatureAlgorithm,
            hashAlgorithm,
            dataBytes,
            signatureBytes);

        redirectAttributes.addFlashAttribute("signatureForm", signatureFormData(
            keyId, signatureAlgorithm, hashAlgorithm, encoding, data, signature));
        redirectAttributes.addFlashAttribute(
            "verifyResult",
            Map.of("valid", valid));
        redirectAttributes.addAttribute("opKeyId", keyId.value());
        redirectAttributes.addAttribute("op", "verify");

        return "redirect:/pki/kms";
    }

    private SignatureFormData signatureFormData(
        KeyId keyId,
        SignatureAlgorithm signatureAlgorithm,
        HashAlgorithm hashAlgorithm,
        DataEncoding encoding,
        String data,
        String signature) {
        return new SignatureFormData(
            keyId.value(), signatureAlgorithm.name(),
            hashAlgorithm == null ? null : hashAlgorithm.name(), encoding.name(), data, signature);
    }

    private enum DataEncoding {
        TEXT,
        BASE64,
        HEX
    }

    @Data
    public static class KeyQuery {

        private KeyAlgorithm keyAlgorithm;
        private String keyword;
        private String keyId;

        public String getSearchKeyword() {
            return keyword != null ? keyword : keyId;
        }

        public KeySearchCriteria toCriteria() {
            return new KeySearchCriteria(
                keyAlgorithm,
                getSearchKeyword());
        }
    }
}
