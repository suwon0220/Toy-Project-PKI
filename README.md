# Toy PKI

A simple implementation of a Public Key Infrastructure (PKI) system in Java.

## Project Policy

### Profiles

The following policies are recommended for this project:

- If `isCa` denotes a CA that issues certificates, require `KEY_CERT_SIGN` when `isCa == true`.
- Require Key Usage in every profile and reject an empty `keyUsages` set.
- Restrict combinations that use CA keys for both signing and encryption or key agreement.

Do not enforce the following rules:

- Forbid `CRL_SIGN` when `isCa == false`.
- Require `CRL_SIGN` whenever `isCa == true`.

Dedicated CRL signing certificates may exist, and not every CA signs CRLs directly.
