package toy.pki.ca.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import toy.pki.ca.domain.CertType;
import toy.pki.ca.domain.extension.KeyUsage;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.dto.profile.ProfileForm;
import toy.pki.ca.repository.CertificateProfileRepository;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/profiles")
public class ProfileController {

    private final CertificateProfileRepository certificateProfileRepository;

    @ModelAttribute("certTypes")
    public CertType[] certTypes() {
        return CertType.values();
    }

    @ModelAttribute("keyAlgorithmPresets")
    public KeyAlgorithmPreset[] keyAlgorithmPresets() {
        return KeyAlgorithmPreset.values();
    }

    @ModelAttribute("keyUsages")
    public KeyUsage[] keyUsages() {
        return KeyUsage.values();
    }

    @ModelAttribute("standardExtendedKeyUsages")
    public StandardExtendedKeyUsage[] standardExtendedKeyUsages() {
        return StandardExtendedKeyUsage.values();
    }

    @GetMapping
    public String getAllProfiles(
            @RequestParam(required = false) UUID profileId,
            @RequestParam(required = false) UUID copyId,
            @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            Model model
    ) {
        log.info("certificate profile: {}", profileForm);


        List<Profile> profiles = certificateProfileRepository.findAll();
        model.addAttribute("profiles", profiles);
        return "/pki/profiles/index";
    }

    @PostMapping
    public String saveProfile(
            @Validated @ModelAttribute ProfileForm profileForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        log.info("certificate profile: {}", profileForm);

        if (bindingResult.hasErrors()) {
            log.error("Binding errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addAttribute("error", "Validation failed");
            return "/pki/profiles/index";
        }

        return "redirect:/pki/profiles/index";
    }

}
