package toy.pki.ca.web.certificate.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum KeyMode {
    NEW("새로운 키 생성"),
    EXISTING("기존 키 선택");

    private final String description;
}
