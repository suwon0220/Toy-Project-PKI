package toy.pki.ca.domain.extension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StandardExtendedKeyUsage {

    SERVER_AUTH(
            "1.3.6.1.5.5.7.3.1",
            "TLS Web Server Authentication"
    ),

    CLIENT_AUTH(
            "1.3.6.1.5.5.7.3.2",
            "TLS Web Client Authentication"
    ),

    CODE_SIGNING(
            "1.3.6.1.5.5.7.3.3",
            "Code Signing"
    ),

    EMAIL_PROTECTION(
            "1.3.6.1.5.5.7.3.4",
            "E-mail Protection"
    ),

    TIME_STAMPING(
            "1.3.6.1.5.5.7.3.8",
            "Time Stamping"
    ),

    OCSP_SIGNING(
            "1.3.6.1.5.5.7.3.9",
            "OCSP Signing"
    );

    private final String oid;
    private final String displayName;
}