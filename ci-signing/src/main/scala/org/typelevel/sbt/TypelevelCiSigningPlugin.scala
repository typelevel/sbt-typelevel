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

package org.typelevel.sbt

import io.crashbox.gpg.SbtGpg
import sbt._, Keys._
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GitHubActionsPlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._

object TypelevelCiSigningPlugin extends AutoPlugin {

  override def requires = SbtGpg && GitHubActionsPlugin && GenerativePlugin

  override def trigger = allRequirements

  override def buildSettings = Seq(
    githubWorkflowPublishEnv ++= Map(
      "PGP_SECRET" -> s"$${{ secrets.PGP_SECRET }}",
      "PGP_PASSPHRASE" -> s"$${{ secrets.PGP_PASSPHRASE }}"
    ),
    githubWorkflowPublishPreamble := Seq(
      WorkflowStep.Run( // if your key is not passphrase-protected
        List("echo $PGP_SECRET | base64 -d | gpg --import"),
        name = Some("Import signing key"),
        cond = Some("env.PGP_SECRET != '' && env.PGP_PASSPHRASE == ''")
      ),
      WorkflowStep.Run( // if your key is passphrase-protected
        List(
          "echo \"$PGP_SECRET\" | base64 -d > /tmp/signing-key.gpg",
          "echo \"$PGP_PASSPHRASE\" | gpg --pinentry-mode loopback --passphrase-fd 0 --import /tmp/signing-key.gpg",
          "(echo \"$PGP_PASSPHRASE\"; echo; echo) | gpg --command-fd 0 --pinentry-mode loopback --change-passphrase $(gpg --list-secret-keys --with-colons 2> /dev/null | grep '^sec:' | cut --delimiter ':' --fields 5 | tail -n 1)"
        ),
        name = Some("Import signing key and strip passphrase"),
        cond = Some("env.PGP_SECRET != '' && env.PGP_PASSPHRASE != ''")
      )
    )
  )

  import SbtGpg.autoImport._

  override def projectSettings = Seq(
    gpgWarnOnFailure := isSnapshot.value
  )

}
