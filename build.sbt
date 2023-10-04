lazy val root = (project in file("."))
  .settings(
    organization := "org.broadinstitute.dsp",
    name := "helm-scala-sdk",
    version := "0.0.8.4",
    scalaVersion := "2.13.6",
    crossScalaVersions := List("2.13.6"),
    libraryDependencies ++= Seq(
      "net.java.dev.jna" % "jna" % "5.13.0",
      "co.fs2" %% "fs2-core" % "3.0.6",
      "org.typelevel" %% "log4cats-slf4j"   % "2.1.1",
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

//javaOptions in console ++= Seq("-Djna.library.path=/Users/qi/workspace/helm-java-sdk/helm-go-lib")
