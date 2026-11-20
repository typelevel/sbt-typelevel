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

import com.timushev.sbt.updates.UpdatesFinder
import com.timushev.sbt.updates.metadata.MetadataLoaderFactory
import com.timushev.sbt.updates.metadata.MetadataLoader
import com.timushev.sbt.updates.versions.Version

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

import sbt.librarymanagement.ModuleID
import sbt.librarymanagement.Resolver
import sbt.util.Logger

/**
 * Looks up the latest release of a module.
 *
 * This is used to populate Scala JS and Scala Native versions in the documentation.
 */
object LatestVersion {

  /**
   * The latest stable version of `organization:name`. This is either `currentVersion` or newer.
   */
  def apply(organization: String, name: String, currentVersion: String): String = {
    val module = ModuleID(organization, name, currentVersion)
    val maybeLoader = MetadataLoaderFactory
      .loader(
        logger = Logger.Null,
        authentications = Nil
      )
      .lift(Resolver.DefaultMavenRepository)

    def findRecentVersions(loader: MetadataLoader): Option[Set[Version]] = {
      val timeout = 30.seconds
      Try(
        Await.result(
          UpdatesFinder.findUpdates(Seq(loader), allowPreRelease = false)(module),
          timeout)
      ).toOption
    }
    val mostRecentVersion = for {
      loader <- maybeLoader
      recentVersions <- findRecentVersions(loader)
      mostRecentVersion <- recentVersions.maxOption.map(_.text)
    } yield mostRecentVersion
    mostRecentVersion.getOrElse(currentVersion)
  }
}
