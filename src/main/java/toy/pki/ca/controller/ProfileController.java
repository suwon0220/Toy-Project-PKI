package toy.pki.ca.controller;

import java.util.List;
import java.util.UUID;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.domain.CertType;
import toy.pki.ca.domain.extension.KeyUsage;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.dto.profile.ProfileForm;
import toy.pki.ca.service.ProfileService;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/profiles")
public class ProfileController {

    private final ProfileService profileService;

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
        Model model) {
        log.info("certificate profile: {}", profileForm);

        List<Profile> profiles = profileService.findAll();
        model.addAttribute("profiles", profiles);
        return "/pki/profiles/index";
    }

    @PostMapping
    public String saveProfile(
        @Validated @ModelAttribute ProfileForm profileForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
        log.info("certificate profile: {}", profileForm.toString());

        if (bindingResult.hasErrors()) {
            log.error("Binding errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addAttribute("error", "Validation failed");
            return "/pki/profiles/index";
        }
        profileService.addProfile(profileForm);
        return "redirect:/pki/profiles";
    }

}
