import com.typesafe.tools.mima.core._
mimaBinaryIssueFilters ++= Seq(
  "org.typelevel.sbt.gha.WorkflowJob.*",
  "org.typelevel.sbt.gha.WorkflowStep.**",
  "org.typelevel.sbt.gha.WorkflowStep#Sbt.*",
  "org.typelevel.sbt.gha.WorkflowStep#Run.*",
  "org.typelevel.sbt.gha.WorkflowStep#Use.*",
  "org.typelevel.sbt.gha.Permissions#Specify.*"
).map(ProblemFilters.exclude[ReversedMissingMethodProblem](_))
