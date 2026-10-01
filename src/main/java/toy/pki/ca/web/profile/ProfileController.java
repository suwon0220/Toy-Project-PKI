package toy.pki.ca.web.profile;

import java.util.List;

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
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.extension.KeyUsageBit;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.domain.profile.ProfileMapper;
import toy.pki.ca.domain.profile.ProfileService;
import toy.pki.ca.web.profile.dto.ProfileSaveForm;
import toy.pki.ca.web.profile.dto.ProfileUpdateForm;
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
    public KeyUsageBit[] keyUsages() {
        return KeyUsageBit.values();
    }

    @ModelAttribute("standardExtendedKeyUsages")
    public StandardExtendedKeyUsage[] standardExtendedKeyUsages() {
        return StandardExtendedKeyUsage.values();
    }

    @ModelAttribute("profiles")
    public List<Profile> profiles() {
        return profileService.listProfiles();
    }

    @GetMapping("")
    public String profiles(
        @RequestParam(required = false) Long profileId,
        Model model
    ) {
        Profile profile = profileId != null ? profileService.getProfile(profileId): null;
        ProfileUpdateForm profileUpdateForm = ProfileMapper.toUpdateForm(profile);
        model.addAttribute("profileUpdateForm", profileUpdateForm);
        return "/pki/profiles/index";
    }

    @PostMapping("/create")
    public String createProfile(
        @Validated @ModelAttribute ProfileSaveForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
        log.info("certificate profile: {}", form.toString());

        if (bindingResult.hasErrors()) {
            log.error("Binding errors: {}", bindingResult.getAllErrors());
            return "pki/profiles/index";
        }
        Long profileId = profileService.createProfile(form);
        redirectAttributes.addFlashAttribute("success", "Profile created successfully");
        // redirectAttributes.addAttribute("profileId", profileId);
        return "redirect:/pki/profiles";
    }

    @PostMapping("/activate")
    public String activate(
        @RequestParam Long profileId,
        RedirectAttributes redirectAttributes
    ) {
        profileService.activateProfile(profileId);
        redirectAttributes.addFlashAttribute("success", "Profile activated successfully");
        return "redirect:/pki/profiles";
    }

//    @PostMapping("/{profileId}/update")
//    public String updateProfile(
//        @RequestParam UUID profileId,
//        @Validated @ModelAttribute ProfileUpdateForm form,
//        BindingResult bindingResult,
//        RedirectAttributes redirectAttributes) {
//        log.info("certificate profile: {}", form.toString());
//
//        if (bindingResult.hasErrors()) {
//            log.error("Binding errors: {}", bindingResult.getAllErrors());
//            redirectAttributes.addAttribute("error", "Validation failed");
//            return "/pki/profiles/index";
//        }
//        profileService.updateProfile(profileId, form);
//        return "redirect:/pki/profiles";
//    }

}
