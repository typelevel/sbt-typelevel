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

```sh
sbt new typelevel/typelevel.g8
```

Visit https://typelevel.org/sbt-typelevel for a quick start example and detailed documentation.
Find the Giter8 template companion project at [typelevel.g8](https://github.com/typelevel/typelevel.g8).

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/i10416"><img src="https://avatars.githubusercontent.com/u/39330037?v=4?s=64" width="64px;" alt="110416"/><br /><sub><b>110416</b></sub></a><br /><a href="#research-i10416" title="Research">🔬</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/osleonard"><img src="https://avatars.githubusercontent.com/u/4851473?v=4?s=64" width="64px;" alt="Akinmolayan Olushola"/><br /><sub><b>Akinmolayan Olushola</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=osleonard" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/valencik"><img src="https://avatars.githubusercontent.com/u/5440389?v=4?s=64" width="64px;" alt="Andrew Valencik"/><br /><sub><b>Andrew Valencik</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=valencik" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/commits?author=valencik" title="Documentation">📖</a> <a href="#tool-valencik" title="Tools">🔧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://toniogela.dev/"><img src="https://avatars.githubusercontent.com/u/41690956?v=4?s=64" width="64px;" alt="Antonio Gelameris"/><br /><sub><b>Antonio Gelameris</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=TonioGela" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/pulls?q=is%3Apr+reviewed-by%3ATonioGela" title="Reviewed Pull Requests">👀</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/armanbilge"><img src="https://avatars.githubusercontent.com/u/3119428?v=4?s=64" width="64px;" alt="Arman Bilge"/><br /><sub><b>Arman Bilge</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=armanbilge" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/pulls?q=is%3Apr+reviewed-by%3Aarmanbilge" title="Reviewed Pull Requests">👀</a> <a href="https://github.com/typelevel/sbt-typelevel/commits?author=armanbilge" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/bplommer"><img src="https://avatars.githubusercontent.com/u/8990749?v=4?s=64" width="64px;" alt="Ben Plommer"/><br /><sub><b>Ben Plommer</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=bplommer" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.planetholt.com/"><img src="https://avatars.githubusercontent.com/u/1455476?v=4?s=64" width="64px;" alt="Brian P. Holt"/><br /><sub><b>Brian P. Holt</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=bpholt" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://christopherdavenport.github.io/sonatype-stats/"><img src="https://avatars.githubusercontent.com/u/10272700?v=4?s=64" width="64px;" alt="Christopher Davenport"/><br /><sub><b>Christopher Davenport</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=ChristopherDavenport" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://indieweb.social/@danicheg"><img src="https://avatars.githubusercontent.com/u/19841757?v=4?s=64" width="64px;" alt="Daniel Esik"/><br /><sub><b>Daniel Esik</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=danicheg" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/commits?author=danicheg" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/djspiewak"><img src="https://avatars.githubusercontent.com/u/752?v=4?s=64" width="64px;" alt="Daniel Spiewak"/><br /><sub><b>Daniel Spiewak</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=djspiewak" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://dgregory.dev/"><img src="https://avatars.githubusercontent.com/u/2992938?v=4?s=64" width="64px;" alt="David Gregory"/><br /><sub><b>David Gregory</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=DavidGregory084" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/pulls?q=is%3Apr+reviewed-by%3ADavidGregory084" title="Reviewed Pull Requests">👀</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/isomarcte"><img src="https://avatars.githubusercontent.com/u/6734045?v=4?s=64" width="64px;" alt="David Strawn"/><br /><sub><b>David Strawn</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=isomarcte" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/j-mie6"><img src="https://avatars.githubusercontent.com/u/5148976?v=4?s=64" width="64px;" alt="Jamie Willis"/><br /><sub><b>Jamie Willis</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=j-mie6" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.planet42.org/"><img src="https://avatars.githubusercontent.com/u/3116929?v=4?s=64" width="64px;" alt="Jens Halm"/><br /><sub><b>Jens Halm</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=jenshalm" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/commits?author=jenshalm" title="Documentation">📖</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/reardonj"><img src="https://avatars.githubusercontent.com/u/142968?v=4?s=64" width="64px;" alt="Justin Reardon"/><br /><sub><b>Justin Reardon</b></sub></a><br /><a href="#research-reardonj" title="Research">🔬</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://ochenashko.com/"><img src="https://avatars.githubusercontent.com/u/6395483?v=4?s=64" width="64px;" alt="Maksym Ochenashko"/><br /><sub><b>Maksym Ochenashko</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=iRevive" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://medium.com/@pjfanning"><img src="https://avatars.githubusercontent.com/u/11783444?v=4?s=64" width="64px;" alt="PJ Fanning"/><br /><sub><b>PJ Fanning</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=pjfanning" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://rossabaker.com/"><img src="https://avatars.githubusercontent.com/u/142698?v=4?s=64" width="64px;" alt="Ross A. Baker"/><br /><sub><b>Ross A. Baker</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=rossabaker" title="Code">💻</a> <a href="#ideas-rossabaker" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/typelevel/sbt-typelevel/pulls?q=is%3Apr+reviewed-by%3Arossabaker" title="Reviewed Pull Requests">👀</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/satorg"><img src="https://avatars.githubusercontent.com/u/3954178?v=4?s=64" width="64px;" alt="Sergey Torgashov"/><br /><sub><b>Sergey Torgashov</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=satorg" title="Code">💻</a> <a href="https://github.com/typelevel/sbt-typelevel/pulls?q=is%3Apr+reviewed-by%3Asatorg" title="Reviewed Pull Requests">👀</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Quafadas"><img src="https://avatars.githubusercontent.com/u/24899792?v=4?s=64" width="64px;" alt="Simon Parten"/><br /><sub><b>Simon Parten</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=Quafadas" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.linkedin.com/in/vasilvasilev97"><img src="https://avatars.githubusercontent.com/u/7115459?v=4?s=64" width="64px;" alt="Vasil Vasilev"/><br /><sub><b>Vasil Vasilev</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=vasilmkd" title="Code">💻</a> <a href="#ideas-vasilmkd" title="Ideas, Planning, & Feedback">🤔</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zetashift"><img src="https://avatars.githubusercontent.com/u/1857826?v=4?s=64" width="64px;" alt="zetashift"/><br /><sub><b>zetashift</b></sub></a><br /><a href="https://github.com/typelevel/sbt-typelevel/commits?author=zetashift" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!