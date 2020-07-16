package org.broadinstitute.dsp

import cats.effect.IO

/**
 * How to run this:
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell, call the function directly, eg: org.broadinstitute.dsp.HelmMannualTest.listHelm()
 */
object HelmMannualTest extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)
  val authContext = AuthContext(
    Namespace("galaxy"),
    KubeToken("your token"),
    KubeApiServer("https://35.225.164.84")
  )

  def install(): Unit = {
    helmClient.install(
      "qi-galaxy-rls-3",
      "galaxyproject/galaxy",
      "/Users/qi/workspace/galaxy-cvmfs-csi-helm/galaxy-cvmfs-csi/values.yaml"
    ).run(authContext)
      .unsafeRunSync()
  }

  def listHelm(): Unit = {
    helmClient.listHelm().run(authContext).unsafeRunSync()
  }
}
