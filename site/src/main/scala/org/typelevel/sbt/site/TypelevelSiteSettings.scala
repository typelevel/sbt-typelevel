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
import laika.helium.config.*
import org.typelevel.sbt.TypelevelGitHubPlugin.gitHubUserRepo
import sbt.Def.*
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
  }

}
