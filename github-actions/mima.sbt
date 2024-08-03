import com.typesafe.tools.mima.core._
mimaBinaryIssueFilters ++= Seq(
  "org.typelevel.sbt.gha.WorkflowJob.withId",
  "org.typelevel.sbt.gha.WorkflowJob.withName",
  "org.typelevel.sbt.gha.WorkflowJob.withSteps",
  "org.typelevel.sbt.gha.WorkflowJob.withSbtStepPreamble",
  "org.typelevel.sbt.gha.WorkflowJob.withCond",
  "org.typelevel.sbt.gha.WorkflowJob.withPermissions",
  "org.typelevel.sbt.gha.WorkflowJob.withEnv",
  "org.typelevel.sbt.gha.WorkflowJob.withOses",
  "org.typelevel.sbt.gha.WorkflowJob.withScalas",
  "org.typelevel.sbt.gha.WorkflowJob.withJavas",
  "org.typelevel.sbt.gha.WorkflowJob.withNeeds",
  "org.typelevel.sbt.gha.WorkflowJob.withMatrixFailFast",
  "org.typelevel.sbt.gha.WorkflowJob.withMatrixAdds",
  "org.typelevel.sbt.gha.WorkflowJob.withMatrixIncs",
  "org.typelevel.sbt.gha.WorkflowJob.withMatrixExcs",
  "org.typelevel.sbt.gha.WorkflowJob.withRunsOnExtraLabels",
  "org.typelevel.sbt.gha.WorkflowJob.withContainer",
  "org.typelevel.sbt.gha.WorkflowJob.withEnvironment",
  "org.typelevel.sbt.gha.WorkflowJob.withConcurrency",
  "org.typelevel.sbt.gha.WorkflowJob.withTimeoutMinutes"
).map(ProblemFilters.exclude[ReversedMissingMethodProblem](_))
