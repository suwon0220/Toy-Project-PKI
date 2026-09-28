// package toy.pki.kms.domain.algorithm;

// import java.security.spec.AlgorithmParameterSpec;
// import java.security.spec.ECGenParameterSpec;
// import java.security.spec.RSAKeyGenParameterSpec;

// import lombok.Getter;
// import lombok.RequiredArgsConstructor;

// @Getter
// @RequiredArgsConstructor
// public enum KeyGenerationParameter {
//     RSA_2048(
//             KeyAlgorithm.RSA,
//             new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4)
//     ),
//     RSA_4096(
//             KeyAlgorithm.RSA,
//             new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4)
//     ),
//     EC_P256(
//             KeyAlgorithm.EC,
//             new ECGenParameterSpec("secp256r1")
//     ),
//     EC_P384(
//             KeyAlgorithm.EC,
//             new ECGenParameterSpec("secp384r1")
//     ),
//     EC_P521(
//             KeyAlgorithm.EC,
//             new ECGenParameterSpec("secp521r1")
//     );

//     private final KeyAlgorithm algorithm;
//     private final AlgorithmParameterSpec algorithmParameterSpec;
// }
