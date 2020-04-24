package org.broadinstitute.dsp

import cats.effect.IO

object HelmMannualTest extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)

  def install(): Unit = {
    helmClient.install(
      "galaxy",
      "qi-galaxy-rls-3",
      "galaxyproject/galaxy",
      "/Users/qi/workspace/galaxy-cvmfs-csi-helm/galaxy-cvmfs-csi/values.yaml"
    ).unsafeRunSync()
  }

  def listHelm(): Unit = {
    helmClient.listHelm().unsafeRunSync()
  }
}
