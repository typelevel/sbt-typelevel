import com.typesafe.tools.mima.core._
mimaBinaryIssueFilters ++= Seq(
  "org.typelevel.sbt.gha.WorkflowJob.with*",
  "org.typelevel.sbt.gha.WorkflowJob.append*",
  "org.typelevel.sbt.gha.WorkflowJob.update*",
  "org.typelevel.sbt.gha.WorkflowStep.update*",
  "org.typelevel.sbt.gha.WorkflowStep.add*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.add*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.update*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.add*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.update*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.add*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.update*"
).map(ProblemFilters.exclude[ReversedMissingMethodProblem](_))
