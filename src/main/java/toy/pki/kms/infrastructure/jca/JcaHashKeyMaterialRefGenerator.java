package toy.pki.kms.infrastructure.jca;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.application.port.KeyMaterialRefGenerator;
import toy.pki.kms.domain.key.KeyMaterialRef;

@Component
@RequiredArgsConstructor
public class JcaHashKeyMaterialRefGenerator implements KeyMaterialRefGenerator {

    private final MessageDigest keyIdMessageDigest;

    /**
     * KeyID 생성
     * @param publicKey 공개키
     * @return KeyId "<algorithm>:<hash>"로 생성된 KeyId
     */
    @Override
    public KeyMaterialRef generate(PublicKey publicKey) {
        byte[] hash = keyIdMessageDigest.digest(publicKey.getEncoded());
        return new KeyMaterialRef(keyIdMessageDigest.getAlgorithm()+":"+ HexFormat.of().formatHex(hash));
    }
}
