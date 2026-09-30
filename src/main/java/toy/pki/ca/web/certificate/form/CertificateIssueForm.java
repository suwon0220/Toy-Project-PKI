package toy.pki.ca.web.certificate.form;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;

@Data
public class CertificateIssueForm {

    @NotNull 
    private SignMode signMode;
    private String caId;
    
    @NotNull 
    private Long profileId;

    @NotNull 
    private Integer validityDays;

    @NotNull 
    private LocalDateTime notBefore;

    private KeySource keySource;

    private MultipartFile csrFile;

    private String csrPEM;

    private UUID kmsKeyId;
    
    private KeyAlgorithm keyAlgorithm;
    private String p12Password;

    private DistinguishedName subjectDn;

    private boolean registerAsCa;
}
