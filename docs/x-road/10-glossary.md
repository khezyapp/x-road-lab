# Glossary

Status: **Verified** from the X-Road terms document and the v7 manuals. Keep definitions short; follow the links from [the knowledge-base index](../index.md) for detail.

| Term | Meaning in this lab |
| --- | --- |
| **X-Road instance** | One X-Road ecosystem with its own Central Server, identifiers, and trust policy. Lab instance: `DEV`. |
| **Member** | An organization registered in the instance, identified by instance/member class/member code. |
| **Member class** | A category of member, such as `GOV`, `BIZ`, or `ORG`. The instance defines the allowed classes. |
| **Subsystem** | A functionally separate part of a member's information system. It is the usual client identity and the owner of services. |
| **Service** | A provider subsystem's callable operation, identified by service code and optional version. |
| **Service code** | The name used for routing and ACLs. Versions do not change the ACL match. |
| **Security Server (SS)** | The member's X-Road gateway. Handles mutual TLS, signatures, OCSP, ACLs, and message logging. |
| **Central Server (CS)** | The registry and policy authority. Publishes global configuration and approves registrations. Not in the message path. |
| **Configuration Proxy (CP)** | Optional distribution mirror for global configuration. |
| **Global configuration** | Signed set of members, Security Servers, certificates, and security policy distributed by the CS. |
| **Configuration anchor** | The XML trust input that tells an SS where to download global configuration and how to verify it. |
| **Trusted anchor** | An anchor from another instance, used for federation. |
| **Certification Authority (CA)** | Issues authentication certificates to Security Servers and signing certificates to members. Must be approved by the CS. |
| **Authentication certificate** | Binds a Security Server's identity to its TLS key. Used for SS-to-SS mutual TLS. |
| **Signing certificate** | Binds a member to its message-signing key. Subsystems of the member share it. |
| **Software token** | File/software storage for keys and certificates. The lab uses SoftHSM/software tokens. |
| **HSM / SSCD** | Hardware secure signature-creation device for keys. Optional; not needed for the lab. |
| **OCSP** | Online Certificate Status Protocol. Security Servers check certificate revocation and exchange OCSP responses during transport. |
| **TSA** | Time-Stamping Authority. Provides RFC 3161 timestamps for the message log. |
| **Message log** | The SS's signed, time-stamped record of requests/responses. Used as digital evidence. |
| **Batch signature** | Signature over multiple message parts, used for attachments/high-volume signing. |
| **Information system (IS)** | The organization's application behind the SS. In this lab, a Spring Boot 4 app. |
| **Adapter** | A service-side component that speaks the X-Road SOAP/REST protocol and forwards to the business logic. |
| **Service description** | WSDL, REST base path, or OpenAPI document that tells the SS what a provider offers. |
| **Service URL** | The provider IS address the SS calls after routing. Different from the service description URL. |
| **ACL / access rights** | The provider's allow-list of consumer subsystems or groups for a service or endpoint. |
| **Global group** | A group defined in the CS; usable in ACLs across Security Servers. |
| **Local group** | A group defined by one SS client; usable only for that client's services. |
| **Management service** | X-Road service used by Security Servers to submit registration/change requests to the CS. |
| **Federation** | Trust relationship between two X-Road instances that allows cross-instance service calls. |
| **`r1`** | REST message protocol major version prefix in the URL. Not a service version. |
| **`X-Road-Client`** | REST header carrying the consumer subsystem ID. Mandatory on requests. |
| **`X-Road-Error`** | REST response header present only when X-Road/SS produced the error. |
| **Request hash** | `X-Road-Request-Hash` response header binding a response to its request. |
| **Operational monitoring** | SS data about request volume, timings, and errors. Optional. |
| **Environmental monitoring** | SS data about OS, packages, disk, CPU, and X-Road version. Optional. |
