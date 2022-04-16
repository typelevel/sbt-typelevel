# sbt-typelevel [![sbt-typelevel Scala version support](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel/latest-by-scala-version.svg?targetType=Sbt)](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel) [![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/D7wY3aH7BQ)

sbt-typelevel configures [sbt](https://www.scala-sbt.org/) for developing, testing, cross-building, publishing, and documenting your Scala library on GitHub, with a focus on semantic versioning and binary compatibility. It is a collection of plugins that work well individually and even better together.

## Features

- Auto-generated GitHub actions workflows, parallelized on Scala version and platform (JVM, JS, Native)
- git-based dynamic versioning
- Binary-compatibility checking with [MiMa](https://github.com/lightbend/mima), following [early semantic versioning](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy)
- CI publishing of releases and snapshots to Sonatype/Maven
- CI deployed GitHub pages websites generated with [mdoc](https://github.com/scalameta/mdoc/) and [Laika](https://github.com/planet42/laika)
- Auto-populated settings for various boilerplate (SCM info, API doc urls, Scala.js sourcemaps, etc.)

## Get Started

Visit https://typelevel.org/sbt-typelevel for a quick start example and detailed documentation.

## Giter8 Template

Find the Giter8 template companion project at [typelevel.g8](https://github.com/typelevel/typelevel.g8)
