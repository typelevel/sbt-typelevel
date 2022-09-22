# sbt-typelevel-site

**sbt-typelevel-site** is an optional plugin for generating websites with [mdoc](https://scalameta.org/mdoc/)
and [Laika](https://planet42.github.io/Laika/) and deploying to GitHub Pages from CI.
You can add it to your build alongside either the  **sbt-typelevel** or **sbt-typelevel-ci-release** plugin
or also use it stand-alone.

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

Place your `.md` files in the `docs/` directory of your project. To preview locally, run `docs/tlSitePreview`.
This will start a preview server at http://localhost:4242.

The site is generated using [mdoc](https://scalameta.org/mdoc/) and [Laika](https://planet42.github.io/Laika/)
and published to the `gh-pages` branch on every push to the specified branch.

You will also need to configure your repository settings:

1. Grant "Read and write" permissions to workflows. This enables them to push to the `gh-pages` branch.
  `https://github.com/{user}/{repo}/settings/actions`
2. Set the GitHub pages source to the `/` (root) directory on the `gh-pages` branch.
  `https://github.com/{user}/{repo}/settings/pages`


## Configuration

If you run the plugin with its defaults it will generate a site that will look like this documentation.

Below we'll describe how the default settings differ from Laika standalone
as well as a few pointers for the most relevant customization options.


### Site Default Settings

Whereas Laika standalone is a general purpose tool, this plugin's domain is project documentation
which allows us to make a few additional assumptions and add useful defaults based on those.

On top of the defaults of Laika standalone, sbt-typelevel adds:

* GitHubFlavor for Markdown is enabled by default (e.g. for fenced code blocks).
* Laika's builtin syntax highlighting is enabled by default (which does not require JavaScript highlighters).
* Metadata is pre-populated based on sbt settings (e.g. title, author, version).
* A link to the GitHub repository is inserted into the top navigation bar based on the output of `git ls-remote --get-url`.
* If you define the `tlSiteApiUrl` setting, a link to the API documentation is inserted into the top navigation bar
  and a redirect for `/api/` to that URL will be added to your site.


#### Additional Defaults for Typelevel Projects

If the generated documentation is for a Typelevel project, you can optionally enable a set of additional defaults
on top of the generic defaults listed in the previous section:

```scala
tlSiteIsTypelevelProject := true
```

With the flag above (which defaults to `false`) these additional settings apply:

* The home link in the top navigation bar carries the Typelevel logo and points to the Typelevel site.
* Links to the Typelevel Discord and Typelevel Twitter are inserted into the top navigation bar.
* The Typelevel favicons are used for the generared site.
* A default footer for Typelevel projects is added to the bottom of each page.
* Theme support for the browser's dark mode is disabled.


### Customization

All customization options are based on Laika's configuration APIs for which we refer you to the comprehensive [Laika manual][Laika]
and specifically the [`laikaTheme` setting](https://planet42.github.io/Laika/0.18/02-running-laika/01-sbt-plugin.html#laikatheme-setting).

@:callout(warning)
For all code examples in the Laika manual you need to replace `Helium.defaults` with `tlSiteHelium.value`
**unless** you explicitly want to remove all additional defaults listed above.
Everything else should be identical with using Laika standalone.
@:@

Some of the customization options which are most likely of interest in the context of project documentation:

* **Versioned Documentation** - Laika can generate versioned documentation that works well for a standard workflow
  where maintenance branches update only the pages specific to that version.
  It inserts a dynamic version switcher into the top navigation bar that is driven by configuration
  (meaning older versions can see newer versions without re-publishing them).
  Examples for existing versioned sites are [Laika] or [http4s].
  See [Versioned Documentation] for details.

* **Landing Page** - Laika comes with a default look & feel for a landing page, but it is disabled by default,
  as it needs to be populated with your content (text, links, logo, etc.).
  Example sites with a standard landing page are again [Laika] or [http4s].
  See [Website Landing Page] for details.

* **Theme Colors and Fonts** - The color theme (including syntax highlighting) and font choices can be adjusted
  without the need for handwritten CSS.
  An example of a site with a different color scheme is [http4s].
  See [Colors] or [Fonts] for details.

* **Additional Links** - Both the main/left navigation panel and the top navigation bar can be populated
  with additional icon links, text links or menus.
  See [Navigation, Links & Favicons][laika-nav] for details.

For a complete list of customization options please see the full [Laika] documentation.


[Laika]: https://planet42.github.io/Laika/index.html
[http4s]: https://http4s.org/
[Versioned Documentation]: https://planet42.github.io/Laika/0.18/03-preparing-content/01-directory-structure.html#versioned-documentation
[Website Landing Page]: https://planet42.github.io/Laika/0.18/03-preparing-content/03-theme-settings.html#website-landing-page
[Colors]: https://planet42.github.io/Laika/0.18/03-preparing-content/03-theme-settings.html#colors
[Fonts]: https://planet42.github.io/Laika/0.18/03-preparing-content/03-theme-settings.html#fonts
[laika-nav]: https://planet42.github.io/Laika/0.18/03-preparing-content/03-theme-settings.html#navigation-links-favicons


## FAQ

### How can I include my project version on the website?

**sbt-typelevel-site** automatically adds `VERSION` and `SNAPSHOT_VERSION` to the `mdocVariables` setting
which can be used with [variable injection](https://scalameta.org/mdoc/docs/why.html#variable-injection).

For example, the sbt-typelevel `VERSION` is `@VERSION@` and `SNAPSHOT_VERSION` is `@SNAPSHOT_VERSION@`.

### How can I publish "unidoc" API docs?

If you generate your API documentation with [sbt-unidoc](https://github.com/sbt/sbt-unidoc),
you can use the `TypelevelUnidocPlugin` to publish a Scaladoc-only artifact to Sonatype/Maven alongside your library artifacts.
This makes it possible to browse your unidocs at [javadoc.io](https://www.javadoc.io/);
for example, the sbt-typelevel [API docs](@API_URL@) are published like this.

```scala
// Make sure to add to your root aggregate so it gets published!
lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin) // also enables the ScalaUnidocPlugin
  .settings(
    name := "woozle-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core.jvm, heffalump)
  )
```
