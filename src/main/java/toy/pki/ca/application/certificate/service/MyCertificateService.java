package toy.pki.ca.application.certificate.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateIssuer;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.application.certificate.model.CreateMyCertificateCommand;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSerialNumber;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyCertificateService {

    private final MyCertificateRepository certificateRepository;
    private final CertificateProfileService profileService;
    private final BouncyCastleCertificateIssuer certificateIssuer;
    private final CertificateValidationService certificateValidationService;

    public CertificateId issue(IssueCertificateCommand request) throws GeneralSecurityException, IOException {
        return issue(request, CertificateSerialNumber.generate());
    }

    public CertificateId issue(IssueCertificateCommand request, CertificateSerialNumber serial)
        throws GeneralSecurityException, IOException {
        CertificateProfile profile = profileService.findById(request.profileId().value());
        MyCertificate issuer = null;
        if (profile.getCertificateType() != CertificateType.ROOT_CA) {
            if (request.issuerCertificateId() == null) {
                throw new IllegalArgumentException("An issuer certificate is required");
            }
            issuer = findById(request.issuerCertificateId().id());
            certificateValidationService.requireValidIssuer(issuer.getId());
        }
        MyCertificate certificate = new MyCertificate(new CertificateId(UUID.randomUUID().toString()),
            profile.getId(), certificateIssuer.issue(profile, request, issuer, serial));
        certificate.setAlias(request.alias());
        certificate.setDescription(request.description());
        certificate.setSubjectKeyId(request.subjectKeyId());
        certificate.setIssuerCertificateId(issuer == null ? null : issuer.getId());
        certificateRepository.save(certificate);
        log.info("Certificate issued: certificateId={}, profileId={}, subjectKeyId={}, issuerCertificateId={}",
            certificate.getId().id(), profile.getId().value(), request.subjectKeyId()
                                                                      .value(), certificate.getIssuerCertificateId());
        return certificate.getId();
    }

    public CertificateId create(CreateMyCertificateCommand request) {
        MyCertificate certificate = new MyCertificate(
            new CertificateId(UUID.randomUUID().toString()), request.profileId(), request.certificate());
        certificate.setAlias(request.alias());
        certificate.setDescription(request.description());
        certificateRepository.save(certificate);
        log.info("Certificate saved: certificateId={}, profileId={}", certificate.getId().id(),
            request.profileId().value());
        return certificate.getId();
    }

    public MyCertificate findById(String certificateId) {
        return certificateRepository.findById(new CertificateId(certificateId))
                                    .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certificateId));
    }

    public List<MyCertificate> findAll() {
        return certificateRepository.findAll();
    }

    public List<MyCertificate> search(CertificateSearchCriteria criteria) {
        return certificateRepository.findByCriteria(criteria);
    }

    public void expire(String certificateId) {
        certificateRepository.findById(new CertificateId(certificateId)).ifPresent(MyCertificate::expire);
    }

    public void revoke(String certificateId) {
        MyCertificate certificate = findById(certificateId);
        certificate.revoke();
        certificateRepository.save(certificate);
        log.info("Certificate revoked: certificateId={}, revokedAt={}", certificateId, certificate.getRevokedAt());
    }

    public void delete(String certificateId) {
        certificateRepository.delete(new CertificateId(certificateId));
        log.info("Certificate deleted: certificateId={}", certificateId);
    }

}
