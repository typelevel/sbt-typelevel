# FAQ

## How do I cut a release?

Create a release on GitHub with a v-prefixed, semantically-versioned tag (or, tag a commit locally and push to GitHub). This will start a CI release. Example tags: `v0.4.2`, `v1.2.3`, `v1.0.0-M1`, `v1.2.0-RC2`. 

It is also possible to run the release process entirely locally by invoking the `tlRelease` command, assuming that you have correctly configured your PGP keys and Sonatype credentials.

## How do I introduce breaking changes intended for my next version?

Bump your `tlBaseVersion` to the next breaking-version according to early-semver, e.g. 0.7 to 0.8 or 4.2 to 5.0.

## What is a base version anyway?

The "base version" is a concept inherited from [sbt-spiewak](https://github.com/djspiewak/sbt-spiewak/blob/d689a5be2f3dba2c335b2be072870287fda701b8/versioning.md#compatibility-version). It is the first two components `x.y` of your semantic version `x.y.z` which are used to communicate important information about binary- and source-compatibility of your library relative to previous releases.

If your library is in 0.x, when you open a PR:

- **If the change is binary-breaking**: bump the base version from `0.y` to `0.(y+1)` (e.g. 0.4 to 0.5). This will indicate to MiMa to stop checking binary compatibility against the `0.y` series.
- **If the change is a backwards compatible feature or bug fix**: no need to update the base version, although you may choose to do so if introducing significant new functionality.

If your library is in 1.x or beyond, when you open a PR:

- **If the change is binary-breaking**: bump the base version from `x.y` to `(x+1).0` (e.g. 4.2 to 5.0). This will indicate to MiMa to stop checking binary compatibility against the `x` series.
- **If the change is source-breaking**: bump the base version from `x.y` to `x.(y+1)` (e.g. 4.2 to 4.3). You may also want to do this when introducing significant new functionality (or for some projects such as Cats and Cats Effect, any new feature at all).
- **If the change is a backwards-compatible feature or bug fix**: no need to update the base version.

In general, if you attempt to introduce binary-breaking changes without appropriately bumping the base version, your PR will fail in CI due to the MiMa binary-compatibility checks.

## How do I indicate the first version that I published Scala 3 artifacts for?

```scala
ThisBuild / tlVersionIntroduced := Map("3" -> "0.4.2")
```

## How do I locally prepare my PR for CI?

**sbt-typelevel** comes with a `prePR` command, which updates the GitHub workflow, generates headers, runs `scalafmt`, and clean compiles your code.

You may also want to (globally) install the [sbt-rewarn](https://github.com/rtimush/sbt-rewarn) plugin to help you identify and resolve compiler warnings, which by default are fatal in CI.

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

Place your `.md` files in the `docs/` directory of your project. The site is generated using [mdoc](https://scalameta.org/mdoc/) and [Laika](https://planet42.github.io/Laika/) and published to the `gh-pages` branch on every push to the specified branch. Make sure to enable GitHub pages in your repo settings. 

To preview locally, run `docs/mdoc` and then `docs/laikaPreview`. This should (reasonably quickly) start a webserver you can view on localhost.

To enjoy a tighter edit loop: with `docs/laikaPreview` running, consider firing up another terminal and starting another sbt, then run `docs/mdoc --watch` [mdoc docs](https://scalameta.org/mdoc/docs/installation.html#live-reload-html-preview-on-file-save).


