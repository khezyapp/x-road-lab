# Identifiers

Status: **Verified** from `pr-mess_x-road_message_protocol.md` §1.3/§2.1/§2.7 and `pr-rest_x-road_message_protocol_for_rest.md` §4.1/§4.8.

Every X-Road object has a hierarchical, globally unique identifier. Most integration bugs start with a wrong identifier, a wrong separator, or a character that the protocol does not allow.

## The identifier model

```text
X-Road instance                     DEV
  └─ member class                   BIZ
      └─ member code                SME-CU
          └─ subsystem code         PORTAL
              └─ service code       applicationSubmit
                  └─ service version v1   (optional)
```

| Object | Canonical string form | Notes |
| --- | --- | --- |
| Instance | `DEV` | Only the instance code must be globally unique. Production instances often use a country code (`EE`, `KH`). |
| Member | `DEV/BIZ/SME-CU` | Member class + member code, issued/controlled by the instance. |
| Subsystem | `DEV/BIZ/SME-CU/PORTAL` | Owned by a member; code unique within that member. This is the usual **client** identity. |
| Service | `DEV/BIZ/SME-CU/PORTAL/applicationSubmit` | Provider subsystem + service code + optional version. |
| Security Server | `DEV/BIZ/SME-CU/SS-SMECU` | Owner member + server code. Also called the server identifier. |

- A **member** is an organization. A **subsystem** is a functionally separate part of that organization's information system. Certificates are issued to the member, not to the subsystem; subsystems share the member's signing certificate.
- A **service** belongs to a provider subsystem. The version is optional and is not part of access-control matching: ACLs are granted per service code, across all versions.
- A **Security Server** is identified by its owner member plus a server code. Its authentication certificate binds that identity to a TLS key.

## Allowed characters

The REST protocol restricts identifier values to:

- letters `A...Z` and `a...z`
- digits `0...9`
- symbols `'()+,-.=?` (the apostrophe, parentheses, plus, comma, hyphen, dot, equals, and question mark)

Notably **not allowed**: `/`, `_`, `:`, `@`, spaces, and non-ASCII characters. Slash is the component separator; underscore is a common mistake.

IDs are not normalized for you; keep the casing consistent across the CS, the SS, the application config, and test scripts.

## REST representation

The REST message protocol uses the `r1` prefix and slash-separated components:

```http
POST /r1/{serviceId}/{path}?{query}
X-Road-Client: {client}
```

where:

- `{serviceId}` = `instance/memberClass/memberCode/subsystemCode/serviceCode`
- `{client}` = `instance/memberClass/memberCode/subsystemCode`

Example:

```http
POST /r1/DEV/GOV/CAMD-X/CATALOG/catalogSearch/search?q=acme
X-Road-Client: DEV/BIZ/SME-CU/PORTAL
```

Each component is UTF-8 and **percent-encoded separately**. Do not encode the separators:

```text
valid:    DEV/BIZ/SME-CU/PORTAL/BAR%2FSERVICE   (a service code containing "/")
invalid:  DEV%2FBIZ%2FSME-CU%2FPORTAL%2FBARSERVICE
```

## SOAP representation

SOAP uses the same components as XML elements in the `http://x-road.eu/xsd/identifiers` namespace:

```xml
<xro:client iden:objectType="SUBSYSTEM">
  <iden:xRoadInstance>DEV</iden:xRoadInstance>
  <iden:memberClass>BIZ</iden:memberClass>
  <iden:memberCode>SME-CU</iden:memberCode>
  <iden:subsystemCode>PORTAL</iden:subsystemCode>
</xro:client>
```

The `client` and `protocolVersion` headers are mandatory; `service` is mandatory on requests; `id` is mandatory. See [03-protocols-and-security.md](03-protocols-and-security.md).

## Lab ID scheme

Proposed for the three-organization lab. Change these in one place and keep the lab config in sync.

| Role | Member class | Member code | Subsystem | Security Server | Service |
| --- | --- | --- | --- | --- | --- |
| SME-CU (consumer + provider) | `BIZ` | `SME-CU` | `PORTAL` | `SS-SMECU` | `applicationSubmit` |
| CamDX platform (provider) | `GOV` | `CAMD-X` | `CATALOG` | `SS-CAMDX` | `catalogSearch` |
| Partner agency (MoC placeholder, provider + consumer) | `GOV` | `MOC` | `REGISTRY` | `SS-MOC` | `businessLookup` |

Full IDs:

```text
Instance:          DEV
SME-CU client:     DEV/BIZ/SME-CU/PORTAL
CamDX catalog:     DEV/GOV/CAMD-X/CATALOG
Partner registry:  DEV/GOV/MOC/REGISTRY
```

The upstream Docker demo uses different IDs (`DEV/COM/1234/TestService` and `DEV/COM/4321/TestClient`). Do not mix the two schemes in one environment.

## Common mistakes

- Using a **member** ID where a **subsystem** ID is required, or omitting the subsystem code in `X-Road-Client`.
- Using `_` in a code, for example `SME_CU`; use `-` instead.
- Changing the subsystem code in the app but not in the CS/SS.
- Assuming the service **version** is part of the ACL. It is not; the ACL matches the service code.
- Using the provider's **service URL** as the X-Road URL. The caller uses its own Security Server and the X-Road service ID; the SS maps the ID to the provider URL.
- Forgetting that the instance code is part of every identifier and every REST path.
