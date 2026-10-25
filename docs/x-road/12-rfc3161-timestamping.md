---
title: RFC 3161 Time-Stamping
description: Cheatsheet for the Time-Stamp Protocol — ASN.1 shapes, OIDs, status codes, transport rules, and Java (BouncyCastle) request/verify snippets.
outline: deep
---

# RFC 3161 Time-Stamping Cheatsheet

Status: **Verified** 2026-10-23 against [RFC 3161](https://www.rfc-editor.org/info/rfc3161/) and BouncyCastle `bcpkix-jdk18on` 1.86. The Java snippets on this page compile and were round-tripped (request → response → offline verification) against a local TSA stand-in.

Use this page for the wire format and for Java integration. For where X-Road uses it, see [Protocols and Security](03-protocols-and-security.md#transport-and-evidence).

## What a time-stamp token proves

| Question | Answer |
| --- | --- |
| What is signed? | A hash of the data (`messageImprint`), the time (`genTime`), a policy OID, and a unique serial number. |
| Does the TSA see my data? | **No.** It only receives the digest and MUST NOT examine it beyond checking the length. |
| What does the token prove? | The data existed **before** `genTime` (within `accuracy`). |
| What does it not prove? | Authorship, correctness, or that the data is unchanged since — you must re-hash the data and compare. |
| Who identifies the requester? | Nobody. The request carries no identity; authentication is out of band (mTLS, API key). |
| Which key signs? | A key reserved for time-stamping; the certificate MUST carry the critical `id-kp-timeStamping` EKU and no other EKU. |

## Flow in five steps

1. Hash the data (SHA-256 or stronger).
2. Build `TimeStampReq`: `version=1`, `messageImprint`, fresh `nonce`, `certReq=true`.
3. `POST` the DER bytes with `Content-Type: application/timestamp-query`.
4. Read `TimeStampResp`; require `status` 0 (`granted`) or 1 (`grantedWithMods`) and a token.
5. Verify: nonce, imprint, signature, signer certificate (ESSCertID), EKU, policy, `genTime`.

## Transport

| Item | Value |
| --- | --- |
| HTTP method | `POST` |
| Request media type | `application/timestamp-query` |
| Response media type | `application/timestamp-reply` |
| Response media type (RFC §3.4 wording) | `application/timestamp-response` — the RFC is inconsistent here; accept both, send the `-reply` form in `Accept` |
| HTTP body | **Raw DER**, never base64. Base64 is only for the e-mail transport (§3.1). |
| File extensions | `.tsq` (request), `.tsr` (reply) |
| Other transports | e-mail (`application/timestamp-query/reply`, base64), raw TCP/318, file |
| Auth | Not in RFC 3161. Use mTLS or an API gateway. |

## OIDs you will actually type

| OID | Name | Where |
| --- | --- | --- |
| `1.3.6.1.5.5.7.3.8` | `id-kp-timeStamping` | EKU in TSA cert |
| `1.2.840.113549.1.7.2` | `id-signedData` | token contentType |
| `1.2.840.113549.1.9.16.1.4` | `id-ct-TSTInfo` | SignedData eContentType |
| `1.2.840.113549.1.9.16.2.14` | `id-aa-timeStampToken` | CMS timestamp attribute |
| `1.3.6.1.5.5.7.48.3` | `id-ad-timeStamping` | SIA accessMethod |
| `2.16.840.1.101.3.4.2.1` | `id-sha256` | imprint digest |
| `2.16.840.1.101.3.4.2.2` | `id-sha384` | imprint digest |
| `2.16.840.1.101.3.4.2.3` | `id-sha512` | imprint digest |
| `1.3.14.3.2.26` | `id-sha1` | legacy, avoid |

Notes on the first two rows: the TSA certificate EKU **must be critical and must be the only EKU**; the SIA extension is optional and only tells clients where the TSA lives. `id-aa-timeStampToken` is the CMS unsigned attribute used to time-stamp an existing signature (Appendix A), not part of the token itself.

## ASN.1 shapes

```text
TimeStampReq ::= SEQUENCE {
  version         INTEGER { v1(1) },
  messageImprint  MessageImprint,
  reqPolicy       TSAPolicyId OPTIONAL,
  nonce           INTEGER OPTIONAL,
  certReq         BOOLEAN DEFAULT FALSE,
  extensions      [0] IMPLICIT Extensions OPTIONAL }

MessageImprint ::= SEQUENCE {
  hashAlgorithm   AlgorithmIdentifier,
  hashedMessage   OCTET STRING }

TimeStampResp ::= SEQUENCE {
  status          PKIStatusInfo,
  -- present only when status is 0 or 1
  timeStampToken  TimeStampToken OPTIONAL }

PKIStatusInfo ::= SEQUENCE {
  status          PKIStatus,
  statusString    PKIFreeText OPTIONAL,
  failInfo        PKIFailureInfo OPTIONAL }

-- TSTInfo is the content inside the CMS SignedData token
TSTInfo ::= SEQUENCE {
  version         INTEGER { v1(1) },
  policy          TSAPolicyId,
  messageImprint  MessageImprint,    -- must equal the request
  serialNumber    INTEGER,           -- unique per TSA, <= 160 bits
  genTime         GeneralizedTime,   -- UTC "Z", seconds required
  accuracy        Accuracy OPTIONAL,
  ordering        BOOLEAN DEFAULT FALSE,
  nonce           INTEGER OPTIONAL,  -- echoed if sent
  tsa             [0] GeneralName OPTIONAL,
  extensions      [1] IMPLICIT Extensions OPTIONAL }

Accuracy ::= SEQUENCE {
  seconds  INTEGER OPTIONAL,
  millis   [0] INTEGER (1..999) OPTIONAL,
  micros   [1] INTEGER (1..999) OPTIONAL }
```

`genTime` is a `GeneralizedTime` in UTC with seconds and a mandatory `Z`, optionally with fractional seconds: `20261023164210Z`, `20261023164210.52Z`.

## Status codes

| `PKIStatus` | Value | Meaning |
| --- | --- | --- |
| `granted` | 0 | Token present |
| `grantedWithMods` | 1 | Token present, TSA modified the request (e.g. policy) |
| `rejection` | 2 | No token; read `failInfo` |
| `waiting` | 3 | No token yet |
| `revocationWarning` | 4 | Warning only |
| `revocationNotification` | 5 | TSA certificate revoked |

Unknown status values are a client error — fail closed.

## Failure info bits

| `PKIFailureInfo` | Bit | Typical cause |
| --- | --- | --- |
| `badAlg` | 0 | TSA does not support the digest OID |
| `badRequest` | 2 | Transaction not permitted/supported |
| `badDataFormat` | 5 | Body is not DER, or imprint length does not match the digest |
| `timeNotAvailable` | 14 | TSA time source down — retry later, this is TSA-side |
| `unacceptedPolicy` | 15 | `reqPolicy` not offered by this TSA |
| `unacceptedExtension` | 16 | Requested extension unsupported (critical or not) |
| `addInfoNotAvailable` | 17 | Requested additional info unavailable |
| `systemFailure` | 25 | TSA internal failure — retry with backoff |

## Verification checklist

| Check | Java | Reject |
| --- | --- | --- |
| Status 0/1, token present | `getStatus()` | anything else |
| Nonce and imprint | `response.validate(tsq)` | throws |
| CMS signature | `token.validate(v)` | throws |
| Signer = ESSCertID | `token.getSID()` | empty match |
| TSA EKU `1.3.6.1.5.5.7.3.8` | `getExtendedKeyUsage()` | missing |
| Policy accepted | `getPolicy()` | not allow-listed |
| `genTime` plausible | `getGenTime()` | clock skew |
| Cert valid at `genTime` | chain + CRL/OCSP | revoked before |

Step 8 is the one people get wrong: a token stays valid after the TSA certificate expires, as long as the certificate was valid when the token was created.

## Java setup

```xml
<!-- bcpkix pulls in bcprov and bcutil -->
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcpkix-jdk18on</artifactId>
  <version>1.86</version>
</dependency>
```

Gradle: `implementation("org.bouncycastle:bcpkix-jdk18on:1.86")`. Register the provider once at startup, then build every operator with `.setProvider("BC")`. The snippets below share one class: imports from earlier blocks apply to later ones.

```java
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class Tsp {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
}
```

### Build and send a request

```java
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;

static byte[] sha256(byte[] data) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(data);
}

static byte[] requestTimestamp(String tsaUrl, byte[] data) throws Exception {
    // 1. Only the hash leaves the process.
    byte[] imprint = sha256(data);

    // 2. nonce makes each reply usable exactly once; certReq asks for the TSA certificate.
    TimeStampRequestGenerator gen = new TimeStampRequestGenerator();
    gen.setCertReq(true);
    gen.setReqPolicy("1.3.6.1.4.1.99999.1.1");   // omit if the TSA has a single policy
    BigInteger nonce = new BigInteger(64, new SecureRandom());
    TimeStampRequest tsq = gen.generate(NISTObjectIdentifiers.id_sha256, imprint, nonce);

    // 3. Raw DER body. Do not base64-encode it and do not use multipart.
    HttpRequest request = HttpRequest.newBuilder(URI.create(tsaUrl))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/timestamp-query")
        .header("Accept", "application/timestamp-reply")
        .POST(HttpRequest.BodyPublishers.ofByteArray(tsq.getEncoded()))
        .build();

    HttpResponse<byte[]> response = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
        .send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() != 200) {
        throw new IOException("TSA returned HTTP " + response.statusCode());
    }
    return response.body();
}
```

### Verify the response

```java
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TSPValidationException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;

static final String TS_EKU = "1.3.6.1.5.5.7.3.8";   // id-kp-timeStamping

static TimeStampToken verify(TimeStampRequest tsq, byte[] responseBytes) throws Exception {
    TimeStampResponse response = new TimeStampResponse(responseBytes);

    // Checks status is granted, the nonce is the one we sent, and the imprint matches.
    response.validate(tsq);

    if (response.getStatus() != 0 && response.getStatus() != 1) {
        throw new TSPValidationException("TSA status " + response.getStatus()
            + ", failInfo " + response.getFailInfo());
    }

    TimeStampToken token = response.getTimeStampToken();
    if (token == null) {
        throw new TSPValidationException("granted response without a token");
    }

    // certReq=true should give us the signer certificate. Pin a trusted cert instead
    // if the TSA does not embed it, or if the reply is not authenticated.
    Collection<X509CertificateHolder> matches =
        token.getCertificates().getMatches(token.getSID());
    if (matches.isEmpty()) {
        throw new TSPValidationException("TSA certificate not embedded");
    }
    X509CertificateHolder tsaCert = matches.iterator().next();

    token.validate(new JcaSimpleSignerInfoVerifierBuilder()
        .setProvider("BC").build(tsaCert));

    X509Certificate cert = new JcaX509CertificateConverter()
        .setProvider("BC").getCertificate(tsaCert);
    List<String> eku = cert.getExtendedKeyUsage();
    if (eku == null || !eku.contains(TS_EKU)) {
        throw new TSPValidationException("certificate is not a TSA certificate");
    }
    return token;
}
```

### Verify an archived `.tsr` against the data

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

static void verifyArchived(Path tsr, byte[] data, X509CertificateHolder trustedTsaCert)
        throws Exception {
    TimeStampToken token =
        new TimeStampResponse(Files.readAllBytes(tsr)).getTimeStampToken();

    if (!Arrays.equals(token.getTimeStampInfo().getMessageImprintDigest(), sha256(data))) {
        throw new TSPValidationException("token does not belong to this data");
    }
    token.validate(new JcaSimpleSignerInfoVerifierBuilder()
        .setProvider("BC").build(trustedTsaCert));
}
```

### Read the fields you will log

```java
import java.math.BigInteger;
import java.time.Instant;
import org.bouncycastle.tsp.GenTimeAccuracy;
import org.bouncycastle.tsp.TimeStampTokenInfo;

static void dump(TimeStampToken token) {
    TimeStampTokenInfo info = token.getTimeStampInfo();
    Instant genTime = info.getGenTime().toInstant();          // UTC
    BigInteger serial = info.getSerialNumber();               // up to 160 bits
    String policy = info.getPolicy().getId();
    BigInteger nonce = info.getNonce();                       // null if not requested
    byte[] imprint = info.getMessageImprintDigest();
    GenTimeAccuracy accuracy = info.getGenTimeAccuracy();     // null if TSA omitted it
    boolean ordered = info.isOrdered();
}
```

## Common failures

| Symptom | Cause | Fix |
| --- | --- | --- |
| `status=2`, `failInfo=badAlg` | Digest OID not supported by this TSA | Use `id-sha256`; check the TSA policy document |
| `status=2`, `failInfo=unacceptedPolicy` | `reqPolicy` not offered | Drop `setReqPolicy`, or use the OID the TSA publishes |
| `status=2`, `failInfo=badDataFormat` | Body is base64, JSON-wrapped, or the digest length is wrong | Send raw DER; hash with the algorithm you declare |
| `status=2`, `failInfo=timeNotAvailable` | TSA clock source down | Retry with backoff; nothing to fix on your side |
| HTTP 400/415 | Wrong `Content-Type` | `application/timestamp-query` |
| HTTP 200 but parsing throws | Body is an HTML/JSON error page, or base64 | Log the first bytes and the `Content-Type` |
| `validate(tsq)` throws about the nonce | Replay, stale reply, or you rebuilt the request | Keep the exact `TimeStampRequest` you sent |
| Token signature fails | Wrong certificate, or signed by an intermediate | Select the cert via `token.getSID()`, not "first cert in the bag" |
| Imprint mismatch, signature fine | Hashed the wrong bytes (base64 text, file name, re-encoded JSON) | Hash the exact bytes you store |
| Verification fails months later | TSA certificate expired or rotated | Validate at `genTime` and keep the original TSA cert chain |

## Rules of thumb

- Send a fresh 64-bit `nonce` on every request. Reject replies that do not echo it.
- Always set `certReq=true` **and** pin or chain-verify the TSA certificate; the embedded certificate alone proves nothing.
- Store the `.tsr` bytes verbatim next to the data. Re-encoding or re-signing destroys the evidence.
- Check `id-kp-timeStamping` is present and critical, and that the certificate has no other EKU.
- Treat `genTime` as UTC; do not parse it as local time.
- Prefer SHA-256 or stronger; `badAlg` is the usual answer to MD5/SHA-1 requests.
- A slow reply is suspicious: RFC 3161 §4 warns about delayed responses under a nonce-only model.
- Time-stamp the signature (or the message), not the certificate — see the `id-aa-timeStampToken` attribute in Appendix A.

## Where this appears in the lab

| Place | Fact |
| --- | --- |
| X-Road message log | Security Servers request RFC 3161 timestamps for log records; this is asynchronous, so a short TSA outage does not stop message exchange ([details](03-protocols-and-security.md#transport-and-evidence)). |
| Central Server | The TSA must be approved and configured as a timestamping service before Security Servers use it ([registration checklist](04-registration-and-access.md#checklist-summary)). |
| Dev stack | The `testca` container also runs a test TSA; it is not trusted outside the lab. |
| Debugging | If every exchange fails at once, check TSA/OCSP reachability and clock sync first ([troubleshooting](09-troubleshooting.md)). |

## Upstream

- RFC 3161: <https://www.rfc-editor.org/info/rfc3161/>
- RFC 5816 — ESSCertIDv2 update to RFC 3161 (SHA-256 certificate identifiers)
- BouncyCastle TSP package: `org.bouncycastle.tsp` in `bcpkix-jdk18on`
