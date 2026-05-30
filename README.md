# Cascara Meta

This repository facilitates building and publishing all *Cascara* modules.

* Cascara Common
  - [cascara-common](https://github.com/qishr/cascara-common)
  - [cascara-common-io](https://github.com/qishr/cascara-common-io)
* Cascara Language Processors
  - [cascara-lang-json](https://github.com/qishr/cascara-lang-json)
  - [cascara-lang-xml](https://github.com/qishr/cascara-lang-xml)
  - [cascara-lang-yaml](https://github.com/qishr/cascara-lang-yaml)
* Cascara Schema
  - [cascara-schema](https://github.com/qishr/cascara-schema)
* Cascara UI
  - [cascara-ui](https://github.com/qishr/cascara-ui)
* Cascara macOS Support
  - [cascara-macos-files](https://github.com/qishr/cascara-macos-files)

## What This Repository Contains

### cascara-meta-bom

### cascara-meta-conventions

A Gradle conventions plugin for building *Cascara* modules.

This plugin provides:

#### Java setup

- Manifest attributes
- Build date

#### Testing

- JUnit Platform enabled
- Test logging configured
- Jacoco wired in
- Jacoco report generation

#### Tasks

- `runtimeModulePath`

### cascara-meta-java-library

This plugin provides:

#### Java setup

- Java toolchain 21
- Source Jars
- Javadoc Jars

### cascara-meta-publish

A Gradle plugin for publishing *Cascara* modules to Maven Local.

This plugin provides:

#### Publishing

- Maven publication
- POM metadata
- Developer info
- SCM info
- Local staging repository

#### Signing

- Publication signing wired in

#### Tasks

- `publishToMavenLocal`

### cascara-meta-release

A utility project for releasing a *Cascara* version on Maven Central.

This project uses JReleaser and cascara-meta-bom to coordinate releases containing a set of versioned *Cascara* modules.

#### Tasks

- `createReleaseTasks`
- `releaseAll`


