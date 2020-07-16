package org.broadinstitute.dsp

import cats.effect.IO

/**
 * How to run this:
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell, call the function directly, eg: org.broadinstitute.dsp.HelmManualTest.listHelm()
 *
 * KubeApiServer and KubeToken can be retrieved via kubectl as described below:
 * https://kubernetes.io/docs/tasks/administer-cluster/access-cluster-api/#without-kubectl-proxy
 */
object HelmManualTest extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)
  val authContext = AuthContext(
    Namespace("galaxy"),
    KubeToken("your token"),
    KubeApiServer("https://35.225.164.84")
  )

  def install(): Unit = {
    helmClient.install(
      "ky-galaxy-rls-0716-1",
      "galaxyproject/galaxy",
      "/Users/kyuksel/gke_experiment/galaxy-cvmfs-csi-helm/galaxy-cvmfs-csi/values.yaml"
    ).run(authContext)
      .unsafeRunSync()
  }

  def listHelm(): Unit = {
    helmClient.listHelm().run(authContext).unsafeRunSync()
  }
}
