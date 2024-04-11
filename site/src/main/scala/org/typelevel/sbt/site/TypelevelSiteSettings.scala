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

  private object colors {
    val primary = Color.hex("385a70")
    val primaryMedium = Color.hex("8ebac7")
    val primaryLight = Color.hex("f4f8fa")
    val secondary = Color.hex("fe4559")
    val text = Color.hex("21303f")
    val background = Color.hex("ffffff")
    val landingPageGradient = Color.hex("b0cfd8")
    val info = Color.hex("ddeaed")
    val warning = Color.hex("f9f5d9")
    val error = Color.hex("ffe7e7")

    val syntaxBase: ColorQuintet = ColorQuintet(
      colors.text, // background
      Color.hex("73ad9b"), // comments
      Color.hex("b2adb4"), // punctuation
      Color.hex("ffb4b5"), // identifier
      Color.hex("e6e8ea") // unclassified
    )
    val syntaxWheel: ColorQuintet = ColorQuintet(
      Color.hex("8fa1c9"), // substitution, annotation
      Color.hex("81e67b"), // keyword, escape-sequence
      Color.hex("ffde6d"), // declaration name
      Color.hex("86aac1"), // literals
      Color.hex("f86971") // type/class name
    )
  }

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
        primary = colors.primary,
        secondary = colors.secondary,
        primaryMedium = colors.primaryMedium,
        primaryLight = colors.primaryLight,
        text = colors.text,
        background = colors.background,
        bgGradient = (colors.landingPageGradient, colors.primaryLight)
      )
      .site
      .messageColors(
        info = colors.primary,
        infoLight = colors.info,
        warning = colors.primary,
        warningLight = colors.warning,
        error = colors.primary,
        errorLight = colors.error
      )
      .site
      .syntaxHighlightingColors(
        base = colors.syntaxBase,
        wheel = colors.syntaxWheel
      )
  }

  @deprecated("color properties will be removed from public API", "0.7.0") val slateBlue =
    colors.primary
  @deprecated(
    "color properties will be removed from public API",
    "0.7.0") val mediumSlateCyanButDarker =
    colors.primaryMedium
  @deprecated(
    "color properties will be removed from public API",
    "0.7.0") val lighterSlateCyan = colors.primaryLight
  @deprecated("color properties will be removed from public API", "0.7.0") val brightRedTl =
    colors.secondary
  @deprecated("color properties will be removed from public API", "0.7.0") val gunmetalTl =
    colors.text
  @deprecated("color properties will be removed from public API", "0.7.0") val whiteTl =
    colors.background
  @deprecated("color properties will be removed from public API", "0.7.0") val mediumSlateCyan =
    colors.landingPageGradient
  @deprecated("color properties will be removed from public API", "0.7.0") val lightSlateCyan =
    colors.info
  @deprecated("color properties will be removed from public API", "0.7.0") val softYellow =
    colors.warning
  @deprecated("color properties will be removed from public API", "0.7.0") val lightPink =
    colors.error
  @deprecated("color properties will be removed from public API", "0.7.0") val pinkTl =
    colors.syntaxBase.c4
  @deprecated("color properties will be removed from public API", "0.7.0") val platinumTl =
    colors.syntaxBase.c5
  @deprecated("color properties will be removed from public API", "0.7.0") val coralTl =
    colors.syntaxWheel.c5

}
