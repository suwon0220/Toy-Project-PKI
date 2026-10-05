package toy.pki.kms.infrastructure.jca.signer;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import toy.pki.kms.adapter.keymaterial.jca.JcaSignatureOperator;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Component
@RequiredArgsConstructor
public class JcaSignatureOperatorRegistry {

    private final List<JcaSignatureOperator> signers;

    public JcaSignatureOperator get(SignatureParameters parameters) {
        List<JcaSignatureOperator> matches = signers.stream()
            .filter(signer -> signer.supports(parameters))
            .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "Unsupported signature parameters: " + parameters);
        }

        if (matches.size() > 1) {
            throw new IllegalStateException(
                "Multiple signers support parameters: " + parameters);
        }

        return matches.get(0);
    }

}
