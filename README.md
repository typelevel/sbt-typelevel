# sbt-typelevel [![sbt-typelevel Scala version support](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel/latest-by-scala-version.svg?targetType=Sbt)](https://index.scala-lang.org/typelevel/sbt-typelevel/sbt-typelevel)

sbt-typelevel helps Scala projects to publish early-semantically-versioned, binary-compatible artifacts to Sonatype/Maven from GitHub actions. It is a collection of plugins that work well individually and even better together.

## Quick start
```scala
// Pick one, for project/plugins.sbt

// Full service, batteries-included, let's go!
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "<version>")

// Set me up for CI release, but don't touch my scalacOptions!
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "<version>")

// Then, in your build.sbt
ThisBuild / tlBaseVersion := "0.4" // your current series x.y
ThisBuild / developers +=
  tlGitHubDev("armanbilge", "Arman Bilge") // your GitHub handle and name
```

Then, on GitHub set the following secrets on your repository:
- `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
- `PGP_SECRET`: output of `gpg --armor --export-secret-keys $LONG_ID | base64`
- `PGP_PASSPHRASE` (optional, use only if your key is passphrase-protected)

Visit https://typelevel.org/sbt-typelevel for detailed documentation.
