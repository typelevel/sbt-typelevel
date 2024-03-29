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
  // val redTl = Color.hex("f8293a")
  val brightRedTl = Color.hex("fe4559")
  val coralTl = Color.hex("f86971")
  val pinkTl = Color.hex("ffb4b5")
  val whiteTl = Color.hex("ffffff")
  val gunmetalTl = Color.hex("21303f")
  val platinumTl = Color.hex("e6e8ea")
  // Extra colours to supplement
  val lightPink = Color.hex("ffe7e7")
  val slateBlue = Color.hex("385a70") // 406881 (original slateCyan)
  val mediumSlateCyanButDarker = Color.hex("8ebac7")
  val mediumSlateCyan = Color.hex("b0cfd8")
  val lightSlateCyan = Color.hex("ddeaed")
  val lighterSlateCyan = Color.hex("f4f8fa")
  val softYellow = Color.hex("f9f5d9")

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
        primary = slateBlue,
        secondary = brightRedTl,
        primaryMedium = mediumSlateCyanButDarker,
        primaryLight = lighterSlateCyan,
        text = gunmetalTl,
        background = whiteTl,
        bgGradient = (mediumSlateCyan, lighterSlateCyan)
      )
      .site
      .messageColors(
        info = slateBlue,
        infoLight = lightSlateCyan,
        warning = slateBlue,
        warningLight = softYellow,
        error = slateBlue,
        errorLight = lightPink
      )
      .site
      .syntaxHighlightingColors(
        base = ColorQuintet(
          gunmetalTl,
          Color.hex("73ad9b"), // comments
          Color.hex("b2adb4"), // ?
          pinkTl, // identifier
          platinumTl // base colour
        ),
        wheel = ColorQuintet(
          Color.hex("8fa1c9"), // substitution, annotation
          Color.hex("81e67b"), // keyword, escape-sequence
          Color.hex("ffde6d"), // declaration name
          Color.hex("86aac1"), // literals
          coralTl // type/class name
        )
      )
  }
}
