package toy.pki.ca.domain.extension;

public record BasicConstraintsProfile(
        boolean ca,
        Integer pathLenConstraint
){
}
