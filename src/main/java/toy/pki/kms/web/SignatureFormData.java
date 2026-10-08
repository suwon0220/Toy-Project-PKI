package toy.pki.kms.web;

public record SignatureFormData(
    String keyId,
    String signatureAlgorithm,
    String hashAlgorithm,
    String encoding,
    String data,
    String signature
) {
}
