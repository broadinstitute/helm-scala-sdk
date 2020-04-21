package org.broadinstitute.dsp

import cats.effect.{Async, IO}
import com.sun.jna.Native
import io.chrisdavenport.log4cats.Logger
import cats.implicits._
import scala.util.control.NoStackTrace

class Helm[F[_]](implicit logger: Logger[F], F: Async[F]) {
  val helClient = Native.load(
    "helm",
    classOf[HelmJnaClient])

  def install(
               namespace: String,
               releaseName: String,
               chartName: String,
               filePath: String
             ): F[Unit] = {
    val params = List(namespace, releaseName, chartName, filePath).map {
      s =>
        val goString = new HelmJnaClient.GoString.ByValue()
        goString.p = namespace
        goString.n = namespace.length
        goString
    }

    F.delay(helClient.install(
      params(0),
      params(1),
      params(2),
      params(3)
    )).flatMap {
      s =>
        translateResult("helm install", s)
    }
  }

  def installCloudMan(): IO[Unit] = IO(helClient.installCloudMan())
  def uninstall(): IO[Unit] = IO(helClient.uninstallCloudmanRelease())

  def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => logger.info(s"${cmd} succeeded")
    case s => F.raiseError(HelmException(s))
  }
}

final case class HelmException(message: String) extends NoStackTrace {
  override def getMessage: String = message
}