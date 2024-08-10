import com.typesafe.tools.mima.core._
mimaBinaryIssueFilters ++= Seq(
  "org.typelevel.sbt.gha.WorkflowJob.with*",
  "org.typelevel.sbt.gha.WorkflowJob.append*",
  "org.typelevel.sbt.gha.WorkflowJob.updated*",
  "org.typelevel.sbt.gha.WorkflowStep.updated*",
  "org.typelevel.sbt.gha.WorkflowStep.append*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.append*",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.updated*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.append*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.updated*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.with*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.append*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.updated*"
).map(ProblemFilters.exclude[ReversedMissingMethodProblem](_))
