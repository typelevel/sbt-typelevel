/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.sbt.site

import laika.ast.Image
import laika.ast.LengthUnit.px
import laika.ast.Span
import laika.ast.TemplateString
import laika.helium.Helium
import laika.helium.config._
import laika.theme.config.Color
import org.typelevel.sbt.TypelevelGitHubPlugin.gitHubUserRepo
import sbt.Def._
import sbt.Keys.licenses
import laika.ast.Path.Root

object TypelevelSiteSettings {

  val defaultHomeLink: ThemeLink = ImageLink.external(
    "https://typelevel.org",
    Image.external(s"https://typelevel.org/img/logo.svg")
  )

  val defaultFooter: Initialize[Seq[Span]] = setting {
    val title = gitHubUserRepo.value.map(_._2)
    title.fold(Seq[Span]()) { title =>
      val licensePhrase = licenses.value.headOption.fold("") {
        case (name, url) =>
          s""" distributed under the <a href="${url.toString}">$name</a> license"""
      }
      Seq(TemplateString(
        s"""$title is a <a href="https://typelevel.org/">Typelevel</a> project$licensePhrase."""
      ))
    }
  }

  val chatLink: IconLink = IconLink.external("https://discord.gg/XF3CXcMzqD", HeliumIcon.chat)

  val mastodonLink: IconLink =
    IconLink.external("https://fosstodon.org/@typelevel", HeliumIcon.mastodon)

  val favIcons: Seq[Favicon] = Seq(
    Favicon.external("https://typelevel.org/img/favicon.png", "32x32", "image/png")
  )

  // light theme colours
  // Tl suffix indicates these are lifted directly from somewhere within the Typelevel site
  //val redTl = Color.hex("f8293a")
  val brightRedTl = Color.hex("fe4559")
  val coralTl = Color.hex("f86971")
  val pinkTl = Color.hex("ffb4b5")
  val whiteTl = Color.hex("ffffff")
  val gunmetalTl = Color.hex("21303f")
  val platinumTl = Color.hex("e6e8ea") // e2e4e6?
  //val offWhiteTl = Color.hex("f2f3f4")
  // Extra colours to supplement
  val lightPink = Color.hex("ffe7e7")
  //val lightPinkGrey = Color.hex("f7f3f3")
  //val lightPinkGreyButDarker = Color.hex("efe7e7")
  val slateBlue = Color.hex("335C67")
  val mediumSlateBlue = Color.hex("b0cfd8")
  val lightSlateBlue = Color.hex("ddeaed")
  val lighterSlateBlue = Color.hex("f4f8fa")
  val softYellow = Color.hex("f9f5d9") // f3eab2

  val interimBlueDark = Color.hex("385a70")
  val interimBlueMedium = Color.hex("406881")
  val interimBlueLight = Color.hex("487692")

  val defaults: Initialize[Helium] = setting {
    GenericSiteSettings
      .defaults
      .value
      .site
      .layout(
        topBarHeight = px(50)
      )
      .site
      .darkMode
      .disabled
      .site
      .favIcons(favIcons: _*)
      .site
      .footer(defaultFooter.value: _*)
      .site
      .topNavigationBar(
        homeLink = defaultHomeLink,
        navLinks = List(chatLink, mastodonLink)
      )
      .site
      .themeColors(
        primary = interimBlueDark,
        secondary = brightRedTl,
        // the medium is on slate until the landing page is decoupled
        primaryMedium = lightSlateBlue, // will be when the landing page text is decoupled mediumSlateBlue
        primaryLight = lighterSlateBlue,
        text = gunmetalTl,
        background = whiteTl,
        // interim colours, while we wait for light gradient support on landing pages
        //bgGradient = (mediumSlateBlue, lighterSlateBlue)
        bgGradient = (interimBlueMedium, interimBlueLight)
      )
      .site
      .messageColors(
        info = interimBlueDark,
        infoLight = lightSlateBlue,
        warning = interimBlueDark,
        warningLight = softYellow,
        error = interimBlueDark,
        errorLight = lightPink
      )
      .site
      .syntaxHighlightingColors(
        base = ColorQuintet(
          gunmetalTl,
          Color.hex("73a6ad"), // comments
          Color.hex("b2adb4"), // ?
          pinkTl, // identifier
          platinumTl // base colour
        ),
        wheel = ColorQuintet(
          Color.hex("8fa1c9"), // substitution, annotation
          Color.hex("81e67b"), // keyword, escape-sequence
          Color.hex("ffde6d"), // declaration name
          Color.hex("759EB8"), // literals
          coralTl // type/class name
        )
      )
    // just for testing purposes :) this can be removed later when things are finalised
    /*.site.landingPage(
    logo = Some(Image.external(s"https://typelevel.org/img/logo.svg")),
    title = Some("SBT Typelevel"),
    subtitle = Some("Build Configuration, Done Better than Yesterday?"),
    latestReleases = Seq(
      ReleaseInfo("Latest Stable Release", "0.4.23"),
      ReleaseInfo("Latest Milestone Release", "0.5.0-M5")
    ),
    license = Some("Apache 2.0"),
    titleLinks = Seq(
      VersionMenu.create(unversionedLabel = "Getting Started"),
      LinkGroup.create(
        IconLink.external("https://github.com/abcdefg/", HeliumIcon.github),
        IconLink.external("https://gitter.im/abcdefg/", HeliumIcon.chat),
        IconLink.external("https://twitter.com/abcdefg/", HeliumIcon.twitter)
      )
    ),
    documentationLinks = Seq(
      TextLink.internal(Root / "site.md", "sbt-typelevel-site"),
      TextLink.internal(Root / "index_.md", "home")
    ),
    projectLinks = Seq(
      TextLink.internal(Root / "site.md", "Text Link"),
      ButtonLink.external("http://somewhere.com/", "Button Label"),
      LinkGroup.create(
        IconLink.internal(Root / "site.md", HeliumIcon.demo),
        IconLink.internal(Root / "site.md", HeliumIcon.info)
      )
    ),
    teasers = Seq(
      Teaser("Teaser 1", "Description 1"),
      Teaser("Teaser 2", "Description 2"),
      Teaser("Teaser 3", "Description 3")
    ))*/
  }

}
