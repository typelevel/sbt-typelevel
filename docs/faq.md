# FAQ

## How do I cut a release?

Create a release on GitHub with a v-prefixed, semantically-versioned tag (or, tag a commit locally and push to GitHub). This will start a CI release. Example tags: `v0.4.2`, `v1.2.3`, `v1.0.0-M1`, `v1.2.0-RC2`. 

It is also possible to run the release process entirely locally by invoking the `tlRelease` command, assuming that you have correctly configured your PGP keys and Sonatype credentials.

## How do I introduce breaking changes intended for my next version?

Bump your `tlBaseVersion` to the next breaking-version according to early-semver, e.g. 0.7 to 0.8 or 4.2 to 5.0.

## How do I indicate the first version that I published Scala 3 artifacts for?

```scala
ThisBuild / tlVersionIntroduced := Map("3" -> "0.4.2")
```

## How do I locally prepare my PR for CI?

**sbt-typelevel** comes with a `prePR` command, which updates the GitHub workflow, generates headers, runs `scalafmt`, and clean compiles your code.

## How do I disable fatal warnings in CI?

If you are using **sbt-typelevel** fatal warnings are on by default in CI.

```scala
ThisBuild / tlFatalWarningsInCi := false
```

If you are only using **sbt-typelevel-ci-release**, you are completely in charge of your own `scalacOptions`, including fatal warnings.

## How do I publish snapshots in CI?

```scala
// any branches you want snapshots of
ThisBuild / tlCiReleaseBranches := Seq("main")
```

Make sure to `reload` sbt and run `githubWorkflowGenerate` after making a change to this setting.

## What happens if I push a tag and commit at the same time?

It Just Works™.

## How do I split my CI matrix into separate jobs for JVM, JS, etc?

```scala
// Before
val root = project.in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(
    coreJVM,
    coreJS,
    io.jvm,
    node.js,
    io.js,
    scodec.jvm,
    scodec.js,
    protocols.jvm,
    protocols.js,
    reactiveStreams,
    benchmark
  )

// After
val root = tlCrossRootProject
  .aggregate(core, io, node, scodec, protocols, reactiveStreams, benchmark)
```

## How do I publish to `s01.oss.sonatype.org`?
```scala
ThisBuild / tlSonatypeUseLegacyHost := false
```

## How do I publish a site like this one?

```scala
// project/plugins.sbt
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "@VERSION@")
// build.sbt
ThisBuild / tlSitePublishBranch := Some("main") // deploy docs from this branch
lazy val docs = project.in(file("site")).enablePlugins(TypelevelSitePlugin)
```

Place your `.md` files in the `docs/` directory of your project. The site is generated using [mdoc](https://scalameta.org/mdoc/) and [Laika](https://planet42.github.io/Laika/) and published to the `gh-pages` branch on every push to the specified branch. Make sure to enable GitHub pages in your repo settings. To preview locally, run `docs/mdoc` and then `docs/laikaPreview`.