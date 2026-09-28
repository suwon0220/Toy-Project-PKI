package toy.pki.kms.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;
import toy.pki.kms.domain.algorithm.SignatureAlgorithm;
import toy.pki.kms.domain.key.KeyID;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.request.KeyGenerationRequest;
import toy.pki.kms.repository.ManagedKeyRepository;
import toy.pki.kms.service.KeyManagementService;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/kms")
public class KmsController {

    private final KeyManagementService keyManagementService;
    private final ManagedKeyRepository managedKeyRepository;

    @ModelAttribute("algorithms")
    public KeyAlgorithm[] algorithms() {
        return KeyAlgorithm.values();
    }

    @ModelAttribute("profiles")
    public KeyGenerationProfile[] profiles() {
        return KeyGenerationProfile.values();
    }

    @ModelAttribute("signatureAlgorithms")
    public SignatureAlgorithm[] signatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    @GetMapping
    public String keys(
            @RequestParam(required = false) KeyID signKeyId,
            @RequestParam(required = false) KeyID deleteKeyId,
            @ModelAttribute KeyGenerationRequest request,
            @ModelAttribute KeyQuery keyQuery,
            Model model
    ) {
        log.info("signKeyId: {}, deleteKeyId: {}, keyQuery: {}", signKeyId, deleteKeyId, keyQuery);
        List<ManagedKey> keys = managedKeyRepository.findAll().stream()
                .filter(keyQuery.toPredicate())
                .collect(Collectors.toList());
        model.addAttribute("keys", keys);
        return "/pki/kms/index";
    }

    // 키 생성
    @PostMapping
    public String saveKey(@Validated @ModelAttribute KeyGenerationRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) throws GeneralSecurityException, IllegalArgumentException {
        log.info("request: {}", request);
        if (bindingResult.hasErrors()) {
            return "/pki/kms/index";
        }

        ManagedKey generated = keyManagementService.generate(request.keyGenerationProfile());
        managedKeyRepository.save(generated);
        redirectAttributes.addFlashAttribute("message", "Key generated successfully.");
        return "redirect:/pki/kms";
    }

    @Data
    public static class KeyQuery {

        private KeyAlgorithm keyAlgorithm;
        private KeyGenerationProfile keyGenerationProfile;
        private KeyID keyId;

        public Predicate<ManagedKey> toPredicate() {
            return key -> (keyAlgorithm == null || key.getKeyGenerationProfile().getAlgorithm().equals(keyAlgorithm))
                    && (keyGenerationProfile == null || key.getKeyGenerationProfile().equals(keyGenerationProfile))
                    && (keyId == null || key.getId().equals(keyId));
        }
    }
}
