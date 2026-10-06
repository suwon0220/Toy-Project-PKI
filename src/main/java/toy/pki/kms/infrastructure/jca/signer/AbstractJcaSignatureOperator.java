package toy.pki.kms.infrastructure.jca.signer;

import java.security.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import toy.pki.kms.adapter.keymaterial.jca.JcaSignatureOperator;

public abstract class AbstractJcaSignatureOperator implements JcaSignatureOperator {
    @Autowired
    protected Provider provider;

}
