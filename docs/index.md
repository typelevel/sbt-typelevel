# sbt-typelevel

sbt-typelevel helps Scala projects to publish early-semantically-versioned, binary-compatible artifacts to Sonatype/Maven from GitHub actions. It is a collection of plugins that work well individually and even better together.

## Quick start

[![sbt-typelevel Scala version support](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel/latest-by-scala-version.svg?targetType=Sbt)](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/D7wY3aH7BQ)

Pick either the `sbt-typelevel` (recommended) or `sbt-typelevel-ci-release` plugin.

#### `project/plugins.sbt`

```scala
// Full service, batteries-included, let's go!
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "@VERSION@")

// Set me up for CI release, but don't touch my scalacOptions!
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "@VERSION@")
```

Then configure your build.

#### `build.sbt`

```scala
ThisBuild / tlBaseVersion := "0.4" // your current series x.y

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := Seq(
  // your GitHub handle and name
  tlGitHubDev("armanbilge", "Arman Bilge")
)

val Scala213 = "2.13.8"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.1.1")
ThisBuild / scalaVersion := Scala213 // the default Scala

lazy val root = tlCrossRootProject.aggregate(core, extra, tests)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .settings(
    name := "woozle-core",
    libraryDependencies += "org.typelevel" %%% "cats-core" % "2.7.0"
  )

lazy val extra = project
  .in(file("extra"))
  .dependsOn(core.jvm)
  .settings(name := "woozle-extra")

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .in(file("tests"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, extra)
  .settings(
    name := "woozle-tests",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test
  )
```

Next, run `githubWorkflowGenerate` in sbt to automatically generate the GitHub Actions workflows.
This will create a build matrix with axes for Scala version and target platform (JVM, JS, etc.).
It will also setup a job for publishing tagged releases e.g. `v0.4.5` to Sonatype/Maven.

Finally, on GitHub set the following secrets on your repository:

- `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
- `PGP_SECRET`: output of `gpg --armor --export-secret-keys $LONG_ID | base64`
- `PGP_PASSPHRASE` (optional, use only if your key is passphrase-protected)
