# Security Policy

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Use GitHub's [Private Vulnerability Reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) instead:

1. Go to the [Security tab](../../security) of this repository.
2. Click **"Report a vulnerability"**.
3. Fill in the form — include reproduction steps and any proposed fix.

You will be notified through GitHub when the report is acknowledged. Expect a first response within **7 days**.

## Supported versions

Only `main` is supported. There are no released versions yet.

## Scope

In scope:

- Authentication and authorization bugs (Quick Connect, password login, JWT handling, refresh-token rotation, session-expired handling).
- On-device secret handling (token storage via DataStore + Tink, Android Keystore usage).
- Network security misconfigurations (TLS, certificate validation).
- Any path by which a malicious app on the same device could obtain Stoganet TV tokens or impersonate the user.

Out of scope:

- Findings that require pre-existing root / debug access to the Android device.
- Issues against the `api-proxy` backend — report those in the [`api-proxy`](https://github.com/Stoganet/api-proxy) repo.
- Denial of service against a single user-controlled installation.
