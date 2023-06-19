# Customization

The complete list of plugins, settings, and utilities is given below. The **sbt-typelevel-ci-release** and **sbt-typelevel** super-plugins automatically load most of them. The diagram at the bottom of the page shows their inter-dependencies.

Instead of using the super-plugins, for finer-grained control you can always add plugins individually to your project and even build your own custom super-plugin.

## Modules

### sbt-typelevel-no-publish
- `NoPublishPlugin`

### sbt-typelevel-kernel
- `TypelevelKernelPlugin`
- `tlIsScala3` (setting): true if `scalaVersion` is 3.x
- `tlReplaceCommandAlias` (method): replace a `addCommandAlias` definition
- `tlReleaseLocal` (command): alias for `+publishLocal`

### sbt-typelevel-versioning
- `TypelevelVersioningPlugin`: Establishes a git-based, early semantic versioning scheme
- `tlBaseVersion` (setting): the series your project is in. e.g., 0.2, 3.5
- `tlUntaggedAreSnapshots` (setting): If true, an untagged commit is given a snapshot version, e.g. `0.4.1-17-00218f9-SNAPSHOT`. If false, it is given a release version, e.g. `0.4.1-17-00218f9`. (default: true)

### sbt-typelevel-mima
- `TypelevelMimaPlugin`: Determines previous MiMa artifacts via your `version` setting and git tags.
- `tlVersionIntroduced` (setting): A map `scalaBinaryVersion -> version` e.g. `Map("2.13" -> "1.5.2", "3" -> "1.7.1")` used to indicate that a particular `crossScalaVersions` value was introduced in a given version (default: empty).

### sbt-typelevel-sonatype
- `TypelevelSonatypePlugin`: Sets up publishing to Sonatype/Maven.
- `tlRelease` (command): check binary-compatibility and `+publish` to Sonatype
- `tlSonatypeUseLegacyHost` (setting): publish to `oss.sonatype.org` instead of `s01.oss.sonatype.org` (default: false)

### sbt-typelevel-settings
- `TypelevelSettingsPlugin`: Good and/or opinionated defaults for scalac settings etc., inspired by sbt-tpolecat.
- `tlFatalWarnings` (setting): Convert compiler warnings into errors (default: false).

### sbt-typelevel-github
- `TypelevelGitHubPlugin`: Populates boilerplate settings assuming you are using GitHub.
- `TypelevelScalaJSGitHubPlugin`: Points your sourcemaps to GitHub permalinks. Only activated for Scala.js projects.
- `tlGitHubDev(user, fullName)` (method): Helper to create a `Developer` entry from a GitHub username.

### sbt-typelevel-ci
- `TypelevelCiPlugin`: Sets up GitHub actions to run tests and check binary-compatibility in CI.
- `tlCrossRootProject` (method): helper to create a `root` project that can aggregate both `Project`s and `CrossProject`s. Automatically creates separate jobs in the CI matrix for each platform (JVM, JS, etc.).

### sbt-typelevel-sonatype-ci-release
- `TypelevelSonatypeCiReleasePlugin`: Sets up GitHub actions to publish to Sonatype in CI.
- Requires the `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` secrets
- `tlCiReleaseTags` (setting): Controls whether or not v-prefixed tags should be released from CI (default true).
- `tlCiReleaseBranches`: The branches in your repository to release from in CI on every push. Depending on your versioning scheme, they will be either snapshots or (hash) releases. Leave this empty if you only want CI releases for tags. (default: `[]`).

### sbt-typelevel-ci-signing
- `TypelevelCiSigningPlugin`: Sets up GitHub actions to sign your artifacts in CI.
- Requires `PGP_SECRET` secret, with your base64-encoded PGP key
- Optionally set the `PGP_PASSPHRASE` secret, but we do not recommend passphrase-protected keys for new projects. See discussion in [#9](https://github.com/typelevel/sbt-typelevel/discussions/9#discussioncomment-1251774).

### sbt-typelevel-ci-release
- `TypelevelCiReleasePlugin`: The super-plugin that sets you up with versioning, mima, signing, and sonatype publishing, all in GitHub actions.

### sbt-typelevel
- `TypelevelPlugin`: The super-super-plugin intended for bootstrapping the typical Typelevel project. Sets up CI release including snapshots, scalac settings, headers, and formatting.

### sbt-typelevel-site
-  `TypelevelSitePlugin`: Sets up an [mdoc](https://scalameta.org/mdoc/)/[Laika](https://typelevel.org/Laika/)-generated website, automatically published to GitHub pages in CI.
- `tlSitePublishBranch` (setting): The branch to publish the site from on every push. Set this to `None` if you only want to update the site on tag releases. (default: `main`)
- `tlSitePublishTags` (setting): Defines whether the site should be published on tag releases. Note on setting this to `true` requires the `tlSitePublishBranch` setting to be set to `None`. (default: `false`)
- `tlSiteApiUrl` (setting): URL to the API docs. (default: `None`)
- `tlSiteHeliumConfig` (setting): the Laika Helium config. (default: how the sbt-typelevel site looks)

## Dependency diagram

sbt-typelevel plugins are in red and the super-plugins are boxed.

<a href="plugins.svg"><img src="plugins.svg" style="width: 100%"/></a>
