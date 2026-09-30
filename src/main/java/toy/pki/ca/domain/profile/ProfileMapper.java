package toy.pki.ca.domain.profile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.springframework.stereotype.Component;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.extension.BasicConstraintsProfile;
import toy.pki.ca.domain.extension.ExtendedKeyUsageProfile;
import toy.pki.ca.domain.extension.ExtendedKeyUsageRecord;
import toy.pki.ca.domain.extension.KeyUsageBit;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;
import toy.pki.ca.web.profile.form.ProfileSaveForm;
import toy.pki.ca.web.profile.form.ProfileUpdateForm;

@Component
public class ProfileMapper {
    public Profile toDomain(ProfileSaveForm form) {
        int keyUsage = 0;
        Set<ExtendedKeyUsageRecord> extendedKeyUsages = new HashSet<>();

        for (KeyUsageBit keyUsageBit : form.getKeyUsageBits()) keyUsage |= keyUsageBit.getValue();
        if(form.getExtendedKeyUsages() != null)
            for (StandardExtendedKeyUsage extendedKeyUsage : form.getExtendedKeyUsages()) {
                extendedKeyUsages.add(
                    new ExtendedKeyUsageRecord(extendedKeyUsage.getOid(), extendedKeyUsage.getDisplayName())
                );
            }

        if(form.getCustomExtendedKeyUsages() != null)
            extendedKeyUsages.addAll(form.getCustomExtendedKeyUsages());

        return new Profile(
            ProfileStatus.DRAFT,
            form.getAlias(),
            form.getDescription(),
            form.getCertType(),
            form.getMaxValidDays(),
            form.getDefaultValidDays(),
            new AllowedKeyAlgorithm(form.getKeyAlgorithms()),
            new KeyUsage(keyUsage),
            form.getSubjectDn(),
            new BasicConstraintsProfile(form.getCertType().equals(CertType.CA), form.getPathLenConstraint()),
            new ExtendedKeyUsageProfile(form.isCriticalExtendedKeyUsage(), extendedKeyUsages),
            form.getAuthorityKeyIdentifier(),
            form.getAuthorityInformationAccess()
        );
    }

    public ProfileUpdateForm toUpdateForm(Profile profile) {
        Set<StandardExtendedKeyUsage> standardExtendedKeyUsages = EnumSet.noneOf(StandardExtendedKeyUsage.class);
        List<ExtendedKeyUsageRecord> customExtendedKeyUsages = new ArrayList<>();
        EnumSet<KeyUsageBit> keyUsageBits = EnumSet.noneOf(KeyUsageBit.class);

        for (ExtendedKeyUsageRecord usage : profile.getExtendedKeyUsage().extendedKeyUsages()) {
            boolean matched = false;
            for (StandardExtendedKeyUsage standard : StandardExtendedKeyUsage.values()) {
                if (standard.getOid().equals(usage.oid())) {
                    standardExtendedKeyUsages.add(standard);
                    matched = true;
                    break;
                }
            }
            if (!matched) customExtendedKeyUsages.add(usage);
        }

        for (KeyUsageBit keyUsageBit : KeyUsageBit.values()) {
            if (profile.getKeyUsage().hasUsages(keyUsageBit.getValue())) {
                keyUsageBits.add(keyUsageBit);
            }
        }

        return new ProfileUpdateForm(
            profile.getAlias(),
            profile.getDescription(),
            profile.getCertType(),
            profile.getMaxValidDays(),
            profile.getDefaultValidDays(),
            profile.getAllowedKeyAlgorithm().keyAlgorithms(),
            keyUsageBits,
            profile.getExtendedKeyUsage().critical(),
            standardExtendedKeyUsages,
            customExtendedKeyUsages,
            profile.getCertType().equals(CertType.CA) ? profile.getBasicConstraints().pathLenConstraint() : null,
            profile.getSubjectDn(),
            profile.getAuthorityKeyIdentifier(),
            profile.getAuthorityInformationAccess()
        );
    }
}
