package org.broadinstitute.dsp

import cats.effect.ExitCode.Success
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger = Slf4jLogger.getLogger[IO]
//    helm.installCloudMan().as(Success)

    val helmClient = new Helm[IO]
    helmClient.install(
      "galaxy",
      "qi-galaxy-rls-3",
      "galaxyproject/galaxy",
      "/Users/qi/workspace/galaxy-cvmfs-csi-helm/galaxy-cvmfs-csi/values.yaml"
    ).as(Success)
  }
}