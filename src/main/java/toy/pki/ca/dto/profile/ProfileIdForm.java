package toy.pki.ca.dto.profile;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileIdForm {
    @NotBlank String alias;
    @NotBlank Optional<String> description;
}
