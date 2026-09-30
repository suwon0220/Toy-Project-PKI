package toy.pki.kms.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;
import toy.pki.kms.domain.key.ManagedKey;

import java.util.List;
import toy.pki.kms.domain.key.ManagedKeyRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ManagedKeyRepositoryTest {

    @Autowired
    ManagedKeyRepository managedKeyRepository;

    @AfterEach
    void afterEach() {
        managedKeyRepository.clearStore();
    }

    @Test
    void findByKeyID() {
        ManagedKey managedKey = new ManagedKey(KeyAlgorithmPreset.RSA_4096, null);
        ManagedKey saveManagedKey = managedKeyRepository.save(managedKey);
        ManagedKey findMangedKey = managedKeyRepository.findByKeyID(managedKey.getId());

        assertThat(findMangedKey).isEqualTo(saveManagedKey);
    }

    @Test
    void findAll() {
        ManagedKey managedKey1 = new ManagedKey(KeyAlgorithmPreset.RSA_4096, null);
        ManagedKey managedKey2 = new ManagedKey(KeyAlgorithmPreset.EC_P256, null);
        ManagedKey managedKey3 = new ManagedKey(KeyAlgorithmPreset.EC_P521, null);

        managedKeyRepository.save(managedKey1);
        managedKeyRepository.save(managedKey2);
        managedKeyRepository.save(managedKey3);

        List<ManagedKey> managedKeys = managedKeyRepository.findAll();

        assertThat(managedKeys.size()).isEqualTo(3);
        assertThat(managedKeys).contains(managedKey1, managedKey2, managedKey3);
    }
}
