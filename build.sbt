val Http4sVersion = "0.21.2"
val CirceVersion = "0.13.0"
val Specs2Version = "4.8.3"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "org.broadinstitute.dsp",
    name := "helm-scala-sdk",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.2",
    crossScalaVersions := List("2.12.10", "2.13.2"),
    libraryDependencies ++= Seq(
      "net.java.dev.jna" % "jna" % "5.5.0",
      "co.fs2" %% "fs2-core" % "2.3.0",
      "io.chrisdavenport" %% "log4cats-core"    % "1.0.1",
      "io.chrisdavenport" %% "log4cats-slf4j"   % "1.0.1",
      "org.scalatest" %% "scalatest" % "3.3.0-SNAP2" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0"),
    Publishing.publishSettings
  )

scalacOptions ++= Seq(
//  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
//  "-Xfatal-warnings"
)

javaOptions in console ++= Seq("-Djna.library.path=/Users/qi/workspace/helm-java-sdk/helm-go-lib")