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

import cats.data.NonEmptyList
import laika.helium.Helium
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.helium.config.TextLink
import laika.helium.config.ThemeNavigationSection
import laika.theme.ThemeProvider
import org.typelevel.sbt.TypelevelGitHubPlugin.gitHubUserRepo
import org.typelevel.sbt.TypelevelKernelPlugin.autoImport.tlIsScala3
import org.typelevel.sbt.TypelevelSitePlugin.autoImport.tlSiteApiUrl
import org.typelevel.sbt.TypelevelSitePlugin.autoImport.tlSiteHeliumExtensions
import org.typelevel.sbt.TypelevelSitePlugin.autoImport.tlSiteRelatedProjects
import sbt.Def.*
import sbt.Keys.developers
import sbt.Keys.scmInfo
import sbt.Keys.version

import scala.annotation.nowarn

object GenericSiteSettings {

  val apiLink: Initialize[Option[IconLink]] = setting {
    tlSiteApiUrl.value.map { url => IconLink.external(url.toString, HeliumIcon.api) }
  }

  val githubLink: Initialize[Option[IconLink]] = setting {
    scmInfo.value.map { info => IconLink.external(info.browseUrl.toString, HeliumIcon.github) }
  }

  @nowarn("cat=deprecation")
  val themeExtensions: Initialize[ThemeProvider] = setting {
    // TODO - inline when deprecated class gets removed
    TypelevelHeliumExtensions(
      tlIsScala3.value,
      tlSiteApiUrl.value
    )
  }

  @nowarn("cat=deprecation")
  private val legacyRelatedProjects: Initialize[Option[ThemeNavigationSection]] = setting {
    NonEmptyList.fromList(tlSiteRelatedProjects.value.toList).map { projects =>
      ThemeNavigationSection(
        "Related Projects",
        projects.map { case (name, url) => TextLink.external(url.toString, name) })
    }
  }

  @nowarn("cat=deprecation")
  val defaults: Initialize[Helium] = setting {
    Helium
      .defaults
      .extendWith(tlSiteHeliumExtensions.value)
      .site
      .metadata(
        title = gitHubUserRepo.value.map(_._2),
        authors = developers.value.map(_.name),
        language = Some("en"),
        version = Some(version.value)
      )
      .site
      .mainNavigation(appendLinks = legacyRelatedProjects.value.toList)
      .site
      .topNavigationBar(
        navLinks = apiLink.value.toList ++ githubLink.value.toList
      )
  }

}
