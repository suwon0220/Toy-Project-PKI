package toy.pki.ca.web.certificate.form;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum KeySource {
    CSR("PKCS#10 업로드"),
    KMS("등록된 KMS 키"),
    NEW("새로운 키 생성");

    private final String description;
}
