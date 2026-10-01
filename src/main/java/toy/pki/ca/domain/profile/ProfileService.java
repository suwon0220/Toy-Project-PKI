package toy.pki.ca.domain.profile;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.web.profile.dto.ProfileSaveForm;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public List<Profile> listProfiles() {
        return profileRepository.findAll();
    }

    public List<Profile> listProfilesByStatus(ProfileStatus status) {
        return profileRepository.findByStatus(status);
    }

    public Profile getProfile(Long id) {
        Profile profile = profileRepository.findById(id);
        if (profile == null) {
            throw new IllegalArgumentException("Profile with ID " + id + " does not exist.");
        }
        return profile;
    }

    public Long createProfile(ProfileSaveForm saveForm) {
        log.info("Creating new profile: {}", saveForm);
        Profile profile = ProfileMapper.toDomain(saveForm);
        profileRepository.save(profile);
        return profile.getId();
    }

    public Long activateProfile(Long id) {
        Profile profile = profileRepository.findById(id);
        if (profile == null) {
            throw new IllegalArgumentException("Profile with ID " + id + " does not exist.");
        }
        log.info("Activating profile with ID {}", profile.getId());
        profile.setStatus(ProfileStatus.ACTIVE);
        return profile.getId();
    }

    public void removeProfile(Long id) {
        log.info("Removing profile with ID {}", id);
        Profile existingProfile = profileRepository.findById(id);
        if (existingProfile == null) {
            throw new IllegalArgumentException("Profile with ID " + id + " does not exist.");
        }
        // Remove the profile from the repository
        profileRepository.remove(existingProfile);
    }
}
