# Migrating to SBT 2.0

This branch of sbt-typelevel supports SBT 2.x. Follow these steps to upgrade your project.

## Publish a local snapshot

sbt-typelevel and its Laika dependency are not yet published. You will need to build and publish them locally.

1. Check out [the Laika SBT 2.0 PR branch](https://github.com/typelevel/Laika/pull/774) and publish it locally with `tlReleaseLocal`. This will publish Laika for both Scala 2.12 and 3 (SBT 1 and 2 respectively).
2. Check out this branch of sbt-typelevel. Upgrade the version in `site/build.sbt` to your local Laika snapshot.
2. Enter the shell with `sbt`. The `sbt-typelevel` build is self-referential, so this will build `sbt-typevel` on SBT 2.
3. Publish jars for Scala 2.12 and 3 with `tlReleaseLocal`.

## Upgrade your sbt-typelevel dependency on SBT 1

Upgrade your `sbt-typelevel` version to your local snapshot. Check that you can enter the sbt shell.

This version contains binary-incompatible changes:
 - The `sbt-gpg` dependency has been replaced with `sbt-pgp`
 - The `sbt-header` plugin organization has changed. If you import `sbtheader` explicitly, you will need to remove the `de.heikoseeberger` prefix

   ```diff
   - import de.heikoseeberger.sbtheader.HeaderPlugin
   + import sbtheader.HeaderPlugin
   ```

## Upgrade other dependencies on SBT 1

Upgrade all other plugin dependencies to their latest versions. The SBT 2.x plugin ecosystem is currently very active, so bugs are still being addressed.

## Upgrade SBT to `2.1.0-RC1`

Upgrade your `build.properties` to `2.1.0-RC1` or later.

Attempt to enter the sbt shell. You will almost certainly encounter issues.

Follow the [SBT 2 migration guide](https://www.scala-sbt.org/2.x/docs/en/changes/migrating-from-sbt-1.x.html). You can use the [Error reference](#error-reference) to troubleshoot.

## Run `githubWorkflowGenerate`

If you use `sbt-typelevel-ci`, run `githubWorkflowGenerate`.

All workflow steps will be the same, but the ordering of Native, JS and JVM build steps may differ. 

```diff
     name: Test
     strategy:
       matrix:
...
-        project: [rootJS, rootJVM, rootNative]
+        project: [rootJVM, rootNative, rootJS]
...
-      - name: scalaJSLink
-        if: matrix.project == 'rootJS'
-        run: sbt 'project ${{ matrix.project }}' '++ ${{ matrix.scala }}' Test/scalaJSLinkerResult
-
       - name: nativeLink
         if: matrix.project == 'rootNative'
         run: sbt 'project ${{ matrix.project }}' '++ ${{ matrix.scala }}' Test/nativeLink

+      - name: scalaJSLink
+        if: matrix.project == 'rootJS'
+        run: sbt 'project ${{ matrix.project }}' '++ ${{ matrix.scala }}' Test/scalaJSLinkerResult
+
```

The target directories will differ.

```
       - name: Make target directories
         if: github.event_name != 'pull_request' && (startsWith(github.ref, 'refs/tags/v') || github.ref == 'refs/heads/main')
-        run: mkdir -p modules/scalacheck/native/target modules/framework-cats/native/target modules/framework/jvm/target 
+        run: mkdir -p target/out/native0.5/scala-2.13.18/weaver-cats target/out/native0.5/scala-2.13.18/weaver-framework 
```

## Run `mergifyGenerate`

If you use `sbt-typelevel-mergify`, run `mergifyGenerate` to update your `.mergify.yml`.

The contents will be the same, but the ordering may differ. 

```diff
   conditions:
   - author=scala-steward
   - body~=labels:.*early-semver-patch
-  - status-success=Test (ubuntu-22.04, 2.12, temurin@8, disciplineNative)
   - status-success=Test (ubuntu-22.04, 2.12, temurin@8, disciplineJS)
   - status-success=Test (ubuntu-22.04, 2.12, temurin@8, disciplineJVM)
-  - status-success=Test (ubuntu-22.04, 2.13, temurin@8, disciplineNative)
+  - status-success=Test (ubuntu-22.04, 2.12, temurin@8, disciplineNative)
   - status-success=Test (ubuntu-22.04, 2.13, temurin@8, disciplineJS)
   - status-success=Test (ubuntu-22.04, 2.13, temurin@8, disciplineJVM)
-  - status-success=Test (ubuntu-22.04, 3, temurin@8, disciplineNative)
+  - status-success=Test (ubuntu-22.04, 2.13, temurin@8, disciplineNative)
   - status-success=Test (ubuntu-22.04, 3, temurin@8, disciplineJS)
   - status-success=Test (ubuntu-22.04, 3, temurin@8, disciplineJVM)
+  - status-success=Test (ubuntu-22.04, 3, temurin@8, disciplineNative)
```

## Verify other `sbt-typelevel` tasks

Run the following tasks:

 - `compile` and `test`
 - `scalafixAll` and `scalafmtAll`
 - `mergifyCheck` and `mergifyGenerate`
 - `tlSite` and `tlSitePreview`, if using `sbt-typelevel-site`
 - `tlReleaseLocal` and `tlRelease`
 - `prePR`
 
## Make a PR to run GitHub Actions workflows

@:callout(info)
CI builds require a published version of sbt-typelevel. As sbt-typelevel has not yet been published, this step will not work.
@:@

Verify that all checks succeed.

## Merge the PR to publish snapshot artifacts

@:callout(info)
CI builds require a published version of sbt-typelevel. As sbt-typelevel has not yet been published, this step will not work.
@:@

Verify that snapshot artifacts are published. If you're using `sbt-typelevel-site`, verify that your GitHub pages were published.

# Error reference

Below is a list of errors you may encounter when upgrading to SBT 2. They are not specific to `sbt-typelevel`. If you encounter an errorthat isn't documented here, please raise a PR to include it.

If you think your error is a bug in `sbt-typelevel` or any of its included plugins, please [report an issue](#reporting-issues).

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

@:callout(todo)
See https://www.scala-sbt.org/2.x/docs/en/changes/migrating-from-sbt-1.x.html . Should this use LocalProject
@:@

### Modules were resolved with conflicting cross-version suffixes

You may see this error on attempting to enter the SBT shell.

```sh
[error] Modules were resolved with conflicting cross-version suffixes in ProjectRef(uri("file:/dev/typelevel/scalacheck/project/"), "sbt-scalacheck-build"):
[error]    com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core _3, _2.13
[error]    org.scala-lang.modules:scala-xml _3, _2.13
[error]    org.scala-lang.modules:scala-collection-compat _3, _2.13
[error] java.lang.RuntimeException: Conflicting cross-version suffixes in: com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core, org
```

If you depend on `sbt-scalafmt`, ensure your dependency version is `2.6.2` or higher.

If this doesn't work, add the `2.13` dependencies to `excludeDependencies` as a workaround and [report an issue](#reporting-issues).

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

This originates from `sbt-git`. If you explicitly include the `sbt-git` plugin, update it to `2.2.0` or higher.

If that doesn't work, add `gitDescribedVersion` to `excludeLintKeys` and [report an issue](#reporting-issues).

```scala
Global / excludeLintKeys += git.gitDescribedVersion
```

### `import de.heikoseeberger.sbtheader.HeaderPlugin` not found

The `sbt-header` organization and package have changed. Remove the `de.heikoseeberger` prefix.

```diff
- import de.heikoseeberger.sbtheader.HeaderPlugin
+ import sbtheader.HeaderPlugin
```

### Generated GitHub workflows steps are in a different order

This is expected and should not cause any issues in your CI build.

### `sbt.librarymanagement.ResolveException`: Error downloading a library dependency

```
// [error] (coreJS / update) sbt.librarymanagement.ResolveException: Error downloading org.scala-js:scalajs-test-interface_sjs1_2.13:1.22.0
// [error]   Not found
// [error]   Not found
// [error]   not found: https://repo1.maven.org/maven2/org/scala-js/scalajs-test-interface_sjs1_2.13/1.22.0/scalajs-test-interface_sjs1_2.13-1.22.0.pom
```

The behaviour of `%%` has changed to apply to all platforms. This means that SBT 2 will resolve `js` and `native` artifacts, when an artifact is only published for JVM. See [the SBT 2 migration guide](https://www.scala-sbt.org/2.x/docs/en/changes/migrating-from-sbt-1.x.html#changes-to-) for more details.

Verify that the dependency URL is correct. In this example, it is incorrect as `scalajs-test-interface_sjs1_2.13` should be `scalajs-test-interface_2.13`.

Specify `.platform(Platform.jvm)` to indicate that the module is JVM-specific.

```diff
libraryDependencies += ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion)
+    .platform(Platform.jvm)
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

# `java.io.File` and `Path` are not valid output types for a cached task

```
-- Error: /Users/zainab/dev/typelevel/cats/build.sbt:31:2 ----------------------
 31 |  doctestGenTests := Seq.empty,
    |  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    |java.io.File and Path are not valid output types for a cached task.
```

Any usages of the `File` and `Path` types must be updated to support caching.

In this example, the `doctestGenTests` task key is defined incorrectly in `sbt-doctest`. This was fixed in a `sbt-doctest` version `0.13.0`. Upgrading `sbt-doctest` resolves the issue.

# Reporting issues

You can report issues by commenting on [the SBT 2.x GitHub issue](https://github.com/typelevel/sbt-typelevel/issues/899).

# Known issues

The following problems are under investigation.

## `tlSitePreview` on `sbtn` does not host a site

This is under investigation.
