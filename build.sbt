lazy val root = (project in file("."))
  .settings(
    organization := "org.broadinstitute.dsp",
    name := "helm-scala-sdk",
    version := "0.0.2",
    scalaVersion := "2.13.5",
    crossScalaVersions := List("2.12.10", "2.13.5"),
    libraryDependencies ++= Seq(
      "net.java.dev.jna" % "jna" % "5.5.0",
      "co.fs2" %% "fs2-core" % "2.5.3",
      "org.typelevel" %% "log4cats-slf4j"   % "1.2.0",
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
  "-feature"
//  "-Xfatal-warnings"
)

javaOptions in console ++= Seq("-Djna.library.path=/Users/qi/workspace/helm-java-sdk/helm-go-lib")
