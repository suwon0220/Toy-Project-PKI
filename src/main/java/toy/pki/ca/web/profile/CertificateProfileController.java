package toy.pki.ca.web.profile;

import jakarta.validation.Valid;
import java.util.HashSet;
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
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pki/profiles")
public class CertificateProfileController {

    private final CertificateProfileService certificateProfileService;

    @ModelAttribute("profiles")
    public List<CertificateProfile> profiles() {
        return certificateProfileService.findAll();
    }

    @ModelAttribute("createProfileForm")
    public CreateProfileForm createProfileForm() {
        return new CreateProfileForm(
            "",
            "",
            365,
            365,
            new HashSet<>(),
            new HashSet<>(),
            false,
            new HashSet<>(),
            new DnPolicyForm(),
            null,
            null,
            new HashSet<>(),
            new HashSet<>());
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
    public String profiles(
        @RequestParam(required = false) String profileId,
        @RequestParam(required = false) String mode,
        Model model) {
        if (profileId != null && !profileId.isBlank()) {
            CertificateProfile profile;
            try {
                profile = certificateProfileService.findById(profileId);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로파일을 찾을 수 없습니다", e);
            }
            CreateProfileForm form = CreateProfileForm.from(profile);
            if ("new".equals(mode)) {
                form.setAlias(null);
            }
            model.addAttribute("selectedProfile", profile);
            model.addAttribute("createProfileForm", form);
        }
        return "pki/profiles/index";
    }

    @PostMapping("/create")
    public String createDraft(
        @Valid @ModelAttribute CreateProfileForm createProfileForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("creating", true);
            return "pki/profiles/index";
        }

        SubjectKeyPolicy subjectKeyPolicy = new SubjectKeyPolicy(
            new HashSet<>(createProfileForm.getKeyAlgorithms()));

        SanPolicy sanPolicy = new SanPolicy(
            createProfileForm.isSanRequired(),
            Set.copyOf(createProfileForm.getAllowedSanTypes()));

        Set<ExtendedKeyUsageOid> extendedKeyUsages = createProfileForm.getExtendedKeyUsageOids()
                                                                      .stream()
                                                                      .filter(oid -> oid != null && !oid.isBlank())
                                                                      .map(String::strip)
                                                                      .map(ExtendedKeyUsageOid::new)
                                                                      .collect(Collectors.toSet());

        CreateProfileCommand request = new CreateProfileCommand(
            createProfileForm.getAlias(),
            createProfileForm.getDescription(),
            createProfileForm.getDefaultValidityDays(),
            createProfileForm.getMaxValidityDays(),
            subjectKeyPolicy,
            createProfileForm.getAllowedSignatures(),
            sanPolicy,
            createProfileForm.getDnPolicy().toPolicy(),
            createProfileForm.getCertificateType(),
            createProfileForm.getPathLenConstraint(),
            createProfileForm.getKeyUsages(),
            extendedKeyUsages);

        ProfileId id = certificateProfileService.createDraft(request);
        redirectAttributes.addAttribute("profileId", id.value());
        return "redirect:/pki/profiles";
    }

    @PostMapping("/{profileId}/update")
    public String updateDraft(
        @PathVariable String profileId,
        @Valid @ModelAttribute UpdateProfileForm updateProfileForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("creating", true);
            return "pki/profiles/index";
        }

        SubjectKeyPolicy subjectKeyPolicy = new SubjectKeyPolicy(
            new HashSet<>(updateProfileForm.getKeyAlgorithms()));

        SanPolicy sanPolicy = new SanPolicy(
            updateProfileForm.isSanRequired(),
            Set.copyOf(updateProfileForm.getAllowedSanTypes()));

        Set<ExtendedKeyUsageOid> extendedKeyUsages = updateProfileForm.getExtendedKeyUsageOids()
                                                                      .stream()
                                                                      .filter(oid -> oid != null && !oid.isBlank())
                                                                      .map(String::strip)
                                                                      .map(ExtendedKeyUsageOid::new)
                                                                      .collect(Collectors.toSet());

        UpdateProfileCommand request = new UpdateProfileCommand(
            profileId,
            updateProfileForm.getAlias(),
            updateProfileForm.getDescription(),
            updateProfileForm.getDefaultValidityDays(),
            updateProfileForm.getMaxValidityDays(),
            subjectKeyPolicy,
            sanPolicy,
            updateProfileForm.getDnPolicy() == null ? null : updateProfileForm.getDnPolicy().toPolicy(),
            updateProfileForm.getCertificateType(),
            updateProfileForm.getPathLenConstraint(),
            updateProfileForm.getKeyUsages(),
            extendedKeyUsages);

        certificateProfileService.updateDraft(request);
        redirectAttributes.addAttribute("profileId", profileId);
        return "redirect:/pki/profiles";
    }

    @PostMapping("/activate")
    public String activate(@RequestParam String profileId) {
        certificateProfileService.activate(profileId);
        return "redirect:/pki/profiles";
    }

    @PostMapping({"/{profileId}/deactivate"})
    public String deactivate(@PathVariable String profileId) {
        certificateProfileService.deactivate(profileId);
        return "redirect:/pki/profiles";
    }

    @PostMapping("/{profileId}/delete")
    public String delete(@PathVariable String profileId) {
        certificateProfileService.delete(profileId);
        return "redirect:/pki/profiles";
    }
}
