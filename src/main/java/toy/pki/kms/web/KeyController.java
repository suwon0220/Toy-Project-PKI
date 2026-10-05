package toy.pki.kms.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.key.HashAlgorithm;
import toy.pki.kms.domain.key.KeyAlgorithm;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.SignatureAlgorithm;
import toy.pki.kms.domain.key.signature.EcdsaSignatureParameters;
import toy.pki.kms.domain.key.signature.Ed25519SignatureParameters;
import toy.pki.kms.domain.key.signature.Ed448SignatureParameters;
import toy.pki.kms.domain.key.signature.RsaPkcs1SignatureParameters;
import toy.pki.kms.domain.key.signature.RsaPssSignatureParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/kms")
public class KeyController {

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
        return SignatureAlgorithm.values();
    }

    @ModelAttribute("hashAlgorithms")
    public HashAlgorithm[] hashAlgorithms() {
        return HashAlgorithm.values();
    }

    @ModelAttribute("keys")
    public List<ManagedKey> keys(@ModelAttribute KeyQuery keyQuery, Model model) {
        List<ManagedKey> keys = keyManagementService.search(keyQuery.toCriteria());
        model.addAttribute("keyAlgorithmLabels", keys.stream()
            .collect(Collectors.toMap(
                ManagedKey::getKeyId,
                key -> KeyAlgorithmPreset.displayName(key.getKeyGenerationParameters()))));
        return keys;
    }

    @GetMapping("")
    public String keys(
        @RequestParam(required = false) KeyId signKeyId,
        @RequestParam(required = false) KeyId deleteKeyId,
        @ModelAttribute KeyGenerationRequest request,
        @ModelAttribute KeyQuery keyQuery,
        Model model) {
        return "pki/kms/index";
    }

    // 키 생성
    @PostMapping("")
    public String saveKey(
        @Validated @ModelAttribute KeyGenerationRequest request,
        BindingResult bindingResult,
        @ModelAttribute KeyQuery keyQuery,
        Model model,
        RedirectAttributes redirectAttributes) throws GeneralSecurityException, IllegalArgumentException {
        log.info("request: {}", request);
        if (bindingResult.hasErrors()) {
            return "pki/kms/index";
        }

        ManagedKey generated = keyManagementService.generate(
            request.providerId(),
            request.toParameters(),
            request.alias());
        redirectAttributes.addFlashAttribute("flashSuccess", "Key generated successfully.");
        return "redirect:/pki/kms";
    }

    @PostMapping("keys/{keyId}/sign")
    public String sign(
        @PathVariable KeyId keyId,
        @RequestParam SignatureAlgorithm signatureAlgorithm,
        @RequestParam(required = false) HashAlgorithm hashAlgorithm,
        @RequestParam DataEncoding encoding,
        @RequestParam String data,
        RedirectAttributes redirectAttributes) throws GeneralSecurityException, IllegalArgumentException {
        ManagedKey key = keyManagementService.findById(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

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

        SignatureParameters signatureParameters = toSignatureParameters(signatureAlgorithm, hashAlgorithm);
        byte[] signature = keyManagementService.sign(keyId, signatureParameters, bytes);

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
        keyManagementService.findById(keyId);
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

        SignatureParameters parameters = toSignatureParameters(signatureAlgorithm, hashAlgorithm);

        boolean valid = keyManagementService.verify(
            keyId,
            parameters,
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

    private enum DataEncoding {
        TEXT,
        BASE64,
        HEX
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

    private SignatureParameters toSignatureParameters(
        SignatureAlgorithm signatureAlgorithm,
        HashAlgorithm hashAlgorithm) {
        // TODO: check hashAlgorithm is not null for algorithms that require it
        return switch (signatureAlgorithm) {
            case DSA ->
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "DSA 서명은 지원하지 않습니다");

            case RSA_PKCS1_V1_5 ->
                new RsaPkcs1SignatureParameters(hashAlgorithm);

            case RSA_PSS ->
                new RsaPssSignatureParameters(
                    hashAlgorithm,
                    hashAlgorithm,
                    32);

            case ECDSA ->
                new EcdsaSignatureParameters(hashAlgorithm);

            case Ed25519 ->
                new Ed25519SignatureParameters();

            case Ed448 ->
                new Ed448SignatureParameters();
        };
    }
}
