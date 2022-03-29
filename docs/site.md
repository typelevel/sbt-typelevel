# sbt-typelevel-site

**sbt-typelevel-site** is an optional plugin for generating websites with [mdoc](https://scalameta.org/mdoc/) and [Laika](https://planet42.github.io/Laika/) and deploying to GitHub Pages from CI. You can add it to your build alongside either the  **sbt-typelevel** or **sbt-typelevel-ci-release** plugin.

## Quick start

#### `project/plugins.sbt`

```scala
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "@VERSION@")
```

#### `build.sbt`

```scala
// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

lazy val docs = project.in(file("site")).enablePlugins(TypelevelSitePlugin)
```

Place your `.md` files in the `docs/` directory of your project. To preview locally, run `docs/tlSitePreview`. This will start a preview server at http://localhost:4242.

The site is generated using [mdoc](https://scalameta.org/mdoc/) and [Laika](https://planet42.github.io/Laika/) and published to the `gh-pages` branch on every push to the specified branch. Make sure to enable GitHub Pages in your repository settings.

### How can I include my project version on the website?

**sbt-typelevel-site** automatically adds `VERSION` and `SNAPSHOT_VERSION` to the `mdocVariables` setting which can be used with [variable injection](https://scalameta.org/mdoc/docs/why.html#variable-injection).

For example, the sbt-typelevel `VERSION` is `@VERSION@` and `SNAPSHOT_VERSION` is `@SNAPSHOT_VERSION@`.

### How can I publish "unidoc" API docs?

If you generate your API documentation with [sbt-unidoc](https://github.com/sbt/sbt-unidoc), you can use the `TypelevelUnidocPlugin` to publish a Scaladoc-only artifact to Sonatype/Maven alongside your library artifacts. This makes it possible to browse your unidocs at [javadoc.io](https://www.javadoc.io/); for example, the sbt-typelevel [API docs](@API_URL@) are published like this.

```scala
// Make sure to add to your root aggregate so it gets published!
lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin) // also enables the ScalaUnidocPlugin
  .settings(
    name := "woozle-docs"
  )
```

### How can I customize my website's appearance?

We refer you to the comprehensive [Laika manual](https://planet42.github.io/Laika/index.html) and specifically the [`laikaTheme` setting](https://planet42.github.io/Laika/0.18/02-running-laika/01-sbt-plugin.html#laikatheme-setting).
