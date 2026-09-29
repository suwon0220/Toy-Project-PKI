package toy.pki.ca.dto.profile;

public record AuthorityInformationAccessForm(
        boolean ocspEnabled,
        boolean caIssuersEnabled
){
}
