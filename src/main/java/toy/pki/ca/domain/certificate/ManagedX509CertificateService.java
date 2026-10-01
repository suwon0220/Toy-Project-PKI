package toy.pki.ca.domain.certificate;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagedX509CertificateService {

    private final ManagedX509CertificateRepository repository;


    public List<ManagedX509Certificate> listCertificates() {
        return repository.findAll();
    }

    public ManagedX509Certificate findById(Long id) {
        return repository.findById(id);
    }

    public Long save(ManagedX509Certificate certificate) {
        return repository.save(certificate);
    }

}
