import sbt.Keys._
import sbt._

object Publishing {
  private val buildTimestamp = System.currentTimeMillis() / 1000

  private val artifactory = s"artifactregistry://us-central1-maven.pkg.dev/dsp-artifact-registry/"

  private def artifactoryResolver(isSnapshot: Boolean): Resolver = {
    val repoType = if (isSnapshot) "snapshot" else "release"
    val repoUrl =
      s"${artifactory}libs-$repoType-standard"
    val repoName = "artifactory-publish"
    repoName at repoUrl
  }

  val publishSettings: Seq[Setting[_]] =
  //we only publish to libs-release-standard because of a bug in sbt that makes snapshots take
  //priority over the local package cache. see here: https://github.com/sbt/sbt/issues/2687#issuecomment-236586241
    Seq(
      publishTo := Some(artifactoryResolver(false)),
      publishArtifact in Compile := true,
      publishArtifact in Test := true
    )
}
