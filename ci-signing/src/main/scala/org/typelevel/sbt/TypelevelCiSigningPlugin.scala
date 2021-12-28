package org.typelevel.sbt

import io.crashbox.gpg.SbtGpg
import sbt._, Keys._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin
import sbtghactions.GenerativePlugin.autoImport._

object TypelevelCiSigningPlugin extends AutoPlugin {

  override def requires = SbtGpg && GitHubActionsPlugin && GenerativePlugin

  override def trigger = noTrigger

  override def buildSettings = Seq(
    githubWorkflowEnv ++= Map(
      "PGP_SECRET" -> s"$${{ secrets.PGP_SECRET }}",
      "PGP_PASSPHRASE" -> s"$${{ secrets.PGP_PASSPHRASE }}"
    ),
    githubWorkflowPublishPreamble ++= Seq(
      WorkflowStep.Run( // if your key is not passphrase-protected
        List("echo $PGP_SECRET | base64 -d | gpg --import"),
        name = Some("Directly import signing key"),
        cond = Some("env.PGP_SECRET != '' && env.PGP_PASSPHRASE == ''")
      ),
      WorkflowStep.Run( // if your key is passphrase protected
        List(
          "echo \"$PGP_SECRET\" | base64 -d > /tmp/signing-key.gpg",
          "echo \"$PGP_PASSPHRASE\" | gpg --pinentry-mode loopback --passphrase-fd 0 --import /tmp/signing-key.gpg",
          "(echo \"$PGP_PASSPHRASE\"; echo; echo) | gpg --command-fd 0 --pinentry-mode loopback --change-passphrase 5EBC14B0F6C55083" // TODO
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
