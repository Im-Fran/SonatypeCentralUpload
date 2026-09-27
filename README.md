<div align="center">

# 📦 SonatypeCentralUpload

**An unofficial Gradle plugin that signs, checksums, bundles and uploads your artifacts to [Sonatype Central](https://central.sonatype.com) in one task.**

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/cl.franciscosolis.sonatype-central-upload)](https://plugins.gradle.org/plugin/cl.franciscosolis.sonatype-central-upload)
[![Release](https://img.shields.io/github/v/release/Im-Fran/SonatypeCentralUpload)](https://github.com/Im-Fran/SonatypeCentralUpload/releases)
[![Test](https://img.shields.io/github/actions/workflow/status/Im-Fran/SonatypeCentralUpload/test.yml?label=test)](https://github.com/Im-Fran/SonatypeCentralUpload/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/Im-Fran/SonatypeCentralUpload)](LICENSE)

</div>

---

## 📖 Overview

Publishing to Maven Central through the [Central Publisher Portal](https://central.sonatype.com) requires a bundle with a very specific layout: every file signed with PGP, every file accompanied by MD5/SHA-1/SHA-256/SHA-512 checksums, all laid out in Maven repository structure and zipped. Doing that by hand (or with a pile of custom tasks) is tedious and error-prone.

This plugin adds a single `sonatypeCentralUpload` task that takes your jars and your `pom.xml`, builds that bundle for you, uploads it through the Publisher API and waits for the deployment to be validated (and published, if you want).

It's written in Kotlin, uses [PGPainless](https://github.com/pgpainless/pgpainless) for signing, so **no `gpg` binary is needed** on the machine running the build, which makes it CI-friendly.

---

## ✨ Features

- **One task** — `./gradlew sonatypeCentralUpload` does the whole thing: copy, sign, checksum, zip, upload, poll.
- **Pure-JVM PGP signing** — detached `.asc` signatures generated with PGPainless; no local GnuPG install or keyring required.
- **Keys from a string or a file** — `signingKey` and `publicKey` accept either an armored key block or a path to a file. Literal `\n` sequences are unescaped, so single-line keys stored in CI secrets work.
- **All required checksums** — `.md5`, `.sha1`, `.sha256` and `.sha512` for every artifact and the POM.
- **Public key distribution** — optionally pushes your public key to `keyserver.ubuntu.com` so Central can verify your signatures.
- **Automatic or manual publishing** — publish straight to Central, or stop once the deployment is validated and release it yourself from the portal.
- **Deployment status polling** — reports `VALIDATED` / `PUBLISHED` and fails the build if Central reports `FAILED`.

---

## 📋 Requirements

- **Gradle** running on **Java 17+**
- A **Sonatype Central** account with a verified namespace matching your `group`
- A **user token** generated from your Central account (used as `username` / `password`)
- A **PGP key pair** for signing

---

## 🚀 Usage

### 1. Apply the plugin

**Kotlin DSL**

```kotlin
plugins {
    id("cl.franciscosolis.sonatype-central-upload") version "2.0.0"
}
```

**Groovy DSL**

```groovy
plugins {
    id 'cl.franciscosolis.sonatype-central-upload' version '2.0.0'
}
```

The plugin id, for quick copy-paste:

```txt
cl.franciscosolis.sonatype-central-upload
```

### 2. Configure the task

**Kotlin DSL**

```kotlin
tasks.sonatypeCentralUpload {
    username = System.getenv("SONATYPE_USERNAME")   // Central user token username
    password = System.getenv("SONATYPE_PASSWORD")   // Central user token password

    archives = files(/* jar, sources jar, javadoc jar */)
    pom = file("path/to/pom.xml")

    signingKey = System.getenv("SIGNING_KEY")                 // Armored private key or path to a key file
    signingKeyPassphrase = System.getenv("SIGNING_PASSWORD")  // Optional
    publicKey = System.getenv("PUBLIC_KEY")                   // Optional, armored key or path

    publishingType = "AUTOMATIC"                    // Optional: AUTOMATIC (default) or MANUAL
}
```

**Groovy DSL**

```groovy
sonatypeCentralUpload {
    username = System.getenv("SONATYPE_USERNAME")
    password = System.getenv("SONATYPE_PASSWORD")

    archives = files(/* jar, sources jar, javadoc jar */)
    pom = file("path/to/pom.xml")

    signingKey = System.getenv("SIGNING_KEY")
    signingKeyPassphrase = System.getenv("SIGNING_PASSWORD")
    publicKey = System.getenv("PUBLIC_KEY")

    publishingType = "AUTOMATIC"
}
```

### 3. Run it

```bash
./gradlew sonatypeCentralUpload
```

---

## ⚙️ Configuration

| Property | Required | Description |
|----------|:--------:|-------------|
| `username` | ✅ | Username part of your Central **user token**. |
| `password` | ✅ | Password part of your Central **user token**. |
| `archives` | ✅ | Files to upload (Central expects the jar, `-sources` jar and `-javadoc` jar). |
| `pom` | ✅ | The `pom.xml` for the artifact. It is uploaded as `<name>-<version>.pom`. |
| `signingKey` | ✅ | Armored PGP private key (`-----BEGIN PGP PRIVATE KEY BLOCK-----…`) or a path to a file containing it. |
| `signingKeyPassphrase` | — | Passphrase for the private key, if it has one. |
| `publicKey` | — | Armored PGP public key or a path to it. When set, it's sent to `keyserver.ubuntu.com` before uploading. |
| `publishingType` | — | `AUTOMATIC` (default) publishes after validation. `MANUAL` stops at `VALIDATED` so you can release from the portal. |

### Things to keep in mind

- **Artifact names must match the project.** Every file in `archives` must start with `<project.name lowercased>-<version>`, e.g. `mylib-1.2.0.jar`, `mylib-1.2.0-sources.jar`. Otherwise the task fails.
- **The bundle is built under `build/sonatype-central-upload/`**, following the Maven layout (`<group path>/<name>/<version>/`), and zipped into `build/sonatype-central-upload/<name>-<version>.zip`.
- **Debugging keyserver uploads:** set the environment variable `SONATYPECENTRALUPLOAD_DEBUG` to any value to print the keyserver response and stack traces.
- **Polling:** the task checks the deployment status every 5 seconds. If Central is still processing after a few checks, it logs the current status and finishes, since publishing can take several minutes. Check your Central account for the final result.

---

## 🛠 Development

| | |
|---|---|
| Language | Kotlin 2.4 (JVM 17) |
| Build | Gradle 9.8 (wrapper included) |
| Signing | PGPainless SOP |
| Zipping | zip4j |
| JSON | Gson |

```bash
git clone https://github.com/Im-Fran/SonatypeCentralUpload.git
cd SonatypeCentralUpload
./gradlew build -x functionalTest
```

### Tests

`./gradlew build` compiles, validates the plugin and compiles the functional test. This is what CI runs on every push and PR to `dev`, and it needs no credentials.

The functional test is an end-to-end test that **performs a real upload to Sonatype Central** (and publishes it) using the mock artifacts in `SonatypeCentralUpload/src/functionalTest/resources`. It only runs on release, and needs these environment variables:

| Variable | Description |
|----------|-------------|
| `SONATYPE_USERNAME` | Central user token username |
| `SONATYPE_PASSWORD` | Central user token password |
| `SIGNING_KEY` | Armored PGP private key |
| `SIGNING_PASSWORD` | Private key passphrase |
| `PUBLIC_KEY` | Armored PGP public key |

```bash
./gradlew functionalTest
```

### Releasing

Publishing a GitHub release triggers the [`deploy.yml`](.github/workflows/deploy.yml) workflow. It first runs the end-to-end functional test (environment `Gradle Plugin Test`) and, only if it passes, runs `./gradlew publishPlugins` (environment `Gradle Plugin Portal`) to push the plugin to the Gradle Plugin Portal using the `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` secrets. Bump `version` in `SonatypeCentralUpload/build.gradle.kts` before releasing.

---

## 🤝 Contributing

Issues and pull requests are welcome.

1. Fork the repo
2. Create a branch: `git checkout -b feat/your-feature`
3. Commit: `git commit -m "feat: add your feature"`
4. Push and open a PR against `dev`

---

## 📄 License

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

---

<div align="center">
Made with ☕ by <a href="https://franciscosolis.cl">Fran</a>
</div>
