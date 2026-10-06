//package toy.pki.ca.application.certificate.service;
//
//import java.util.Locale;
//import java.util.function.Predicate;
//import toy.pki.ca.domain.certificate.CertificateStatus;
//import toy.pki.ca.domain.profile.CertificateProfile;
//
//public record CertificateService(
//    String q,
//    CertificateStatus status,
//    String profile
//) {
//
//    public static class CertificateSearchCriteria {
//        keyword = keyword == null || keyword.isBlank()
//                  ? null
//                  : keyword.strip().toLowerCase(Locale.ROOT);
//    }
//
//    public Predicate<CertificateProfile> toPredicate() {
//        throw new UnsupportedOperationException("Unimplemented method 'toPredicate'");
//    }
//}
