package toy.pki.kms.domain.request;

import lombok.Data;
import toy.pki.kms.domain.parameter.SignatureParameter;

@Data
public class SigGenRequest {

    private SignatureParameter signatureParameter;
    private byte[] data;
}
