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
import laika.theme.config.Font
import laika.theme.config.FontDefinition
import laika.theme.config.FontStyle
import laika.theme.config.FontWeight
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

  val twitterLink: IconLink =
    IconLink.external("https://twitter.com/typelevel", HeliumIcon.twitter)

  val favIcons: Seq[Favicon] = Seq(
    Favicon.external("https://typelevel.org/img/favicon.png", "32x32", "image/png")
  )

  val fonts = {
    // default fontPath and fonts taken from Laika:
    // instead of allowing for more fonts to be added, all the fonts must be respecified, to avoid "redundant embedding of unused fonts"
    // as a result, these things have to just be defined again if we want to change them, and we do
    val laikaFontPath = "laika/helium/fonts"
    val tlFontPath = "org/typelevel/sbt/site/fonts"

    Seq(
      FontDefinition(
        Font
          .embedResource(s"$laikaFontPath/Lato/Lato-Regular.ttf")
          .webCSS("https://fonts.googleapis.com/css?family=Lato:400,700"),
        "Lato",
        FontWeight.Normal,
        FontStyle.Normal
      ),
      FontDefinition(
        Font.embedResource(s"$laikaFontPath/Lato/Lato-Italic.ttf"),
        "Lato",
        FontWeight.Normal,
        FontStyle.Italic
      ),
      FontDefinition(
        Font.embedResource(s"$laikaFontPath/Lato/Lato-Bold.ttf"),
        "Lato",
        FontWeight.Bold,
        FontStyle.Normal
      ),
      FontDefinition(
        Font.embedResource(s"$laikaFontPath/Lato/Lato-BoldItalic.ttf"),
        "Lato",
        FontWeight.Bold,
        FontStyle.Italic
      ),
      // Fira Code is the default used by Laika, but that has ligatures
      // Fira Mono is basically the same font, but without ligatures: yay!
      FontDefinition(
        Font
          .embedResource(s"$tlFontPath/FiraMono/FiraMono-Medium.ttf")
          .webCSS("https://fonts.googleapis.com/css?family=Fira+Mono:500"),
        "Fira Mono",
        FontWeight.Normal,
        FontStyle.Normal
      ),
      FontDefinition(
        Font.embedResource(s"$laikaFontPath/icofont/fonts/icofont.ttf"),
        "IcoFont",
        FontWeight.Normal,
        FontStyle.Normal
      )
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
        navLinks = List(chatLink, twitterLink)
      )
      .all
      .fontResources(fonts: _*)
      .all
      .fontFamilies(
        body = "Lato",
        headlines = "Lato",
        code = "Fira Mono" // this bit is changed from Laika defaults
      )
  }

}
