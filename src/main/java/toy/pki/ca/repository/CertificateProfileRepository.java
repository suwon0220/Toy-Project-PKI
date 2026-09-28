package toy.pki.ca.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import toy.pki.ca.domain.CertificateData;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class CertificateProfileRepository {

    private final Map<BigInteger, CertificateData> certificateStore = new ConcurrentHashMap<>();

    public void save(@NonNull CertificateData certificateData) {
        certificateStore.put(certificateData.getSerialNumber(), certificateData);
    }

    public CertificateData findBySerialNumber(@NonNull String serialNumber) {
        return certificateStore.get(serialNumber);
    }

    public List<CertificateData> findAll() {
        return certificateStore.values().stream()
                .filter(Objects::nonNull)
                .map(obj -> obj)
                .toList();
    }

    public void clear() {
        certificateStore.clear();
    }

}
