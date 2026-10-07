package toy.pki.kms.web;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class KeyIssueForm {
    String alias;
    @NotEmpty String keyAlgorithm;
}
