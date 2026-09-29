package toy.pki.ca.dto.profile;

import jakarta.annotation.Nonnull;
import lombok.Data;

@Data
public class ValidityForm {

    @Nonnull
    private Integer maxValidDays;

    @Nonnull
    private Integer minValidDays;

    public Integer defaultValidDays() {
        return maxValidDays - minValidDays;
    }
}
