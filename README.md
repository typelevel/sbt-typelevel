# sbt-typelevel

## Quick start
```scala
// For a project releasing to sonatype from CI
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "<version>") // plugins.sbt
enablePlugins(TypelevelCiReleasePlugin) // build.sbt

// Or, for a project not releasing from CI
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "<version>") // plugins.sbt
```

## Customization

sbt-typelevel is made up of several independent plugins. If you don't like some of them, you can create your own top-level plugin by mixing and matching.

- `NoPublishPlugin`: Straightforward no-publish settings. No dependencies.
- `TypelevelKernelPlugin`: Shared basic settings/utilities. No dependencies.
- `TypelevelSettingsPlugin`: Good and/or opinionated defaults for scalac settings, Scala.js settings, etc. Depends on sbt-git, sbt-typelevel-kernel.
- `TypelevelVersioningPlugin`: Establishes semantic versioning. Depends on sbt-git, sbt-typelevel-kernel.
- `TypelevelMimaPlugin`: Determines previous Mima artifacts via git tags. Depends on sbt-mima-plugin, sbt-git, sbt-typelevel-kernel.
- `TypelevelSonatypePlugin`: Provides a `release` command for publishing binary-compatible artifacts to sonatype. Depends on sbt-mima-plugin, sbt-sonatype.
- `TypelevelSonatypeCiReleasePlugin`: Integrates sonatype publishing into your GitHub workflow. Depends on sbt-typelevel-sonatype, sbt-github-actions.
- `TypelevelCiSigningPlugin`: Sign your artifacts in CI, with your choice of a password-protected or non-password protected key. Depends on sbt-gpg, sbt-github-actions.
- `TypelevelCiPlugin`: Run tests and check binary-compatibility in CI. Depends on sbt-mima-plugin, sbt-github-actions.
- `TypelevelPlugin`: The top-level plugin for non-ci-publishing projects. Brings together the no-publish, settings, versioning, mima, and CI plugins.
- `TypelevelCiReleasePlugin`: The top-level plugin for ci-publishing projects. Brings together the `TypelevelPlugin`, CI signing plugin, and sonatype CI release plugin.
