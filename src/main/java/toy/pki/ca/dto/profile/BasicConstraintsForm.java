package toy.pki.ca.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.OptionalInt;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicConstraintsForm {
        private static final int EMPTY_PATH_LEN_CONSTRAINT = -1;

        private boolean isCA;
        private Integer pathLenConstraint;

    public BasicConstraintsForm(boolean isCA) {
        this.isCA = isCA;
        this.pathLenConstraint = EMPTY_PATH_LEN_CONSTRAINT;
    }
}
