# sbt-typelevel

sbt-typelevel configures [sbt](https://www.scala-sbt.org/) for developing, testing, cross-building, publishing, and documenting your Scala library on GitHub, with a focus on semantic versioning and binary compatibility. It is a collection of plugins that work well individually and even better together.

## Features

- Auto-generated GitHub actions workflows, parallelized on Scala version and platform (JVM, JS, Native)
- git-based dynamic versioning
- Binary-compatibility checking with [MiMa](https://github.com/lightbend/mima), following [early semantic versioning](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy)
- CI publishing of releases and snapshots to Sonatype/Maven
- CI deployed GitHub pages websites, generated with [mdoc](https://github.com/scalameta/mdoc/) and [Laika](https://github.com/planet42/laika)
- Auto-populated settings for various boilerplate (SCM info, API doc urls, Scala.js sourcemaps, etc.)

## Quick start

[![sbt-typelevel Scala version support](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel/latest-by-scala-version.svg?targetType=Sbt)](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/D7wY3aH7BQ)

Pick either the **sbt-typelevel** (recommended) or **sbt-typelevel-ci-release** plugin.

#### `project/plugins.sbt`

```scala
// Full service, batteries-included, let's go!
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "@VERSION@")

// Set me up for CI release, but don't touch my scalacOptions!
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "@VERSION@")
```

@:callout(info)

**sbt-typelevel-ci-release** includes all the core features mentioned above.

**sbt-typelevel** extends **sbt-typelevel-ci-release** with:

1. **sbt-typelevel-settings**: Good (and/or opinionated ;) defaults for `scalacOptions` and friends. Note that you can also manually add this plugin to a project using **sbt-typelevel-ci-release**.
2. Automated scalafmt and copyright header checks in CI.
3. A `prePR` command and other nice-to-haves.

@:@


### Giter8 Template

The sbt-typelevel project provides a [Giter8 template](http://www.foundweekends.org/giter8/index.html) for quickly starting projects with familiar workflows and best practices.

```sh
sbt new typelevel/typelevel.g8
```

This will help you create a new project with sbt-typelevel, sbt-typelevel-site, and some other boilerplate setup.
Check out the project repo for more details: [typelevel.g8](https://github.com/typelevel/typelevel.g8)


### Configure Your Build

#### `build.sbt`

```scala
ThisBuild / tlBaseVersion := "0.4" // your current series x.y

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("armanbilge", "Arman Bilge")
)

// true by default, set to false to publish to s01.oss.sonatype.org
ThisBuild / tlSonatypeUseLegacyHost := true

val Scala213 = "2.13.8"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.1.1")
ThisBuild / scalaVersion := Scala213 // the default Scala

lazy val root = tlCrossRootProject.aggregate(core, heffalump, tests)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .settings(
    name := "woozle-core",
    description := "Core data types and typeclasses",
    libraryDependencies += "org.typelevel" %%% "cats-core" % "2.7.0"
  )

lazy val heffalump = project
  .in(file("heffalump"))
  .dependsOn(core.jvm)
  .settings(
    name := "woozle-heffalump",
    description := "Integration module with heffalump (JVM only)",
    libraryDependencies += "org.100aker" %% "heffalump-core" % "0.8.21"
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .in(file("tests"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core)
  .settings(
    name := "woozle-tests",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test
  )
```

Next, run `githubWorkflowGenerate` in sbt to automatically generate the GitHub Actions workflows.
This will create a CI matrix parallelized on Scala version and target platform (JVM, JS, etc.) and includes steps for running tests and checking binary compatibility.
It will also setup a job for publishing tagged releases e.g. `v0.4.5` and snapshots to Sonatype/Maven.

Finally, on GitHub set the following secrets on your repository:

- `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
- `PGP_SECRET`: output of `gpg --armor --export-secret-keys $LONG_ID | base64`
- `PGP_PASSPHRASE` (optional, use only if your key is passphrase-protected)

Please see the [Secrets](secrets.md) page for more information and detailed instructions.
