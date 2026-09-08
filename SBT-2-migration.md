This branch has a working version of SBT typelevel on SBT 2.

It incorporates changes from the following PRs.
 - [Replace sbt-gpg with sbt-pgp](https://github.com/typelevel/sbt-typelevel/pull/907). 
 - [Cross build modules for SBT 2](https://github.com/typelevel/sbt-typelevel/pull/912), which cross builds non-site modules. 

It uses a local snapshot of Laika on SBT 2 to build site modules.

# How to publish a local snapshot

1. Check out [the Laika SBT 2.0 PR branch](https://github.com/typelevel/Laika/pull/774) and build it locally. Upgrade the version in `site/build.sbt` to your local snapshot.

2. Enter the shell with `sbt`. The `sbt-typelevel` build is self-referential, so this will build `sbt-typevel` on SBT 2.
3. Publish jars for Scala 3 with `++ 3 publishLocal`. Note that `tlReleaseLocal` does not work.

# How to upgrade projects

You can use the local snapshot to test out `sbt-typelevel` on other projects. Change the SBT version of your project to `2.0.8`, and upgrade the version of `sbt-typelevel` to your local snapshot. 

Consult the `Known issues` section below on any errors.

## Known issues

### Overlapping output directories

You may see this error on attempting to enter the SBT shell.

```
[error] Overlapping output directories:/dev/typelevel/scalacheck/target/out/jvm/scala-2.13.18/scalacheck:
[error] 	ProjectRef(file:/dev/typelevel/scalacheck/,coreJVM)
[error] 	ProjectRef(file:/dev/typelevel/scalacheck/,root)
[error] 	ProjectRef(file:/dev/typelevel/scalacheck/,rootJS)
[error] 	ProjectRef(file:/dev/typelevel/scalacheck/,rootJVM)
[error] 	ProjectRef(file:/dev/typelevel/scalacheck/,rootNative)
```

This occurs when there is an unscoped `name` setting in the `build.sbt`. 

```scala
name := "scalacheck"
```

The `name` setting should be specified per module.

To address the issue, delete the `name` setting and specify it under any modules

```diff
- name := "scalacheck"

lazy val core = crossProject(...)
  .in(file("core"))
  .settings(
+    name := "scalacheck",
  )
```

### Modules were resolved with conflicting cross-version suffixes

You may see this error on attempting to enter the SBT shell.

```sh
[error] Modules were resolved with conflicting cross-version suffixes in ProjectRef(uri("file:/dev/typelevel/scalacheck/project/"), "sbt-scalacheck-build"):
[error]    com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core _3, _2.13
[error]    org.scala-lang.modules:scala-xml _3, _2.13
[error]    org.scala-lang.modules:scala-collection-compat _3, _2.13
[error] java.lang.RuntimeException: Conflicting cross-version suffixes in: com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core, org
```

If you depend on `sbt-scalafmt`, ensure your dependency version is `2.6.2`.

If this doesn't work, add the `2.13` dependencies to `excludeDependencies` as a workaround.

```scala
 excludeDependencies ++= {
   Seq(
     ExclusionRule(
       "org.scala-lang.modules",
       "scala-collection-compat_2.13"
     ),
     ExclusionRule("org.scala-lang.modules", "scala-xml_2.13"),
     ExclusionRule("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13")
   )
 }
```

### `tlReleaseLocal` fails with `deliver/makeIvyXml requires an Ivy-based publishing plugin`

```
[error] (core / deliverLocal) deliver/makeIvyXml requires an Ivy-based publishing plugin, which is not part of this sbt distribution.
```

This has been raised in [sbt-pgp](https://github.com/sbt/sbt-pgp/issues/246). 

As a workaround, use `publishLocal` instead.

### Warning: `gitDescribedVersion` is not used by any other settings/tasks


```sh
[warn] there are 5 keys that are not used by any other settings/tasks:
[warn]
[warn] * core / gitDescribedVersion
[warn]   +- gitDescribedVersion := {
[warn]       val projectPatterns = gitDescribePatterns.value
[warn]       val buildPatterns = (ThisBuild / gitDescribePatterns).value
[warn]       val projectTagToVersionNumber = gitTagToVersionNumber.value
[warn]       val buildTagToVersionNumber = (ThisBuild / gitTagToVersionNumber).value
[warn]       if (projectPatterns == buildPatterns && projectTagToVersionNumber == buildTagToVersionNumber)
[warn]         (ThisBuild / gitDescribedVersion).value
[warn]       else gitReader.value.withGit(_.describedVersion(projectPatterns)).map(v => projectTagToVersionNumber(v).getOrElse(v))
[warn]     }:167
```

This has been raised in [sbt-git](https://github.com/sbt/sbt-git/issues/379).

As a workaround, add `gitDescribedVersion` to `excludeLintKeys`.

```scala
Global / excludeLintKeys += git.gitDescribedVersion
```

### `import de.heikoseeberger.sbtheader.HeaderPlugin` not found

The `sbt-header` organization and package have changed. Remove the `de.heikoseeberger` prefix.

```diff
- import de.heikoseeberger.sbtheader.HeaderPlugin
+ import sbtheader.HeaderPlugin
```

### Version conflicts in library dependencies in Native builds

```
[error] (coreNative / update) found version conflict(s) in library dependencies; some are suspected to be binary incompatible:
[error] 	* org.scala-native:test-interface_native0.5_3:0.5.11 (strict) is selected over 0.5.8 for test
[error] 	    +- org.typelevel:discipline-core_native0.5_3:1.7.0-112-07d9dad-20260908T094537Z-SNAPSHOT (depends on 0.5.11)
[error] 	    +- org.scalacheck:scalacheck_native0.5_3:1.19.0       (depends on 0.5.8)
```

SBT 2.0 enables strict eviction checks for test dependencies. This surfaces eviction errors that would have otherwise been surpressed in SBT 1. 

Resolve these by upgrading dependencies. In the example above, `scalacheck` must be upgraded.

As a workaround, specify a different dependency scheme for `test-interface_native0.5`.

```scala
ThisBuild / libraryDependencySchemes += "org.scala-native" %% "test-interface_native0.5" % VersionScheme.EarlySemVer
```
