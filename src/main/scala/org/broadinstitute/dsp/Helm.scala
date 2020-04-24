package org.broadinstitute.dsp

import cats.effect.concurrent.Semaphore
import cats.effect.{Async, Blocker, ContextShift, IO}
import com.sun.jna.Native
import io.chrisdavenport.log4cats.Logger
import cats.implicits._

import scala.util.control.NoStackTrace

class Helm[F[_]: ContextShift](blocker: Blocker,
                               concurrencyBound: Semaphore[F])(implicit logger: Logger[F], F: Async[F]) {
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

    val res = blockingF(F.delay(helClient.install(
      params(0),
      params(1),
      params(2),
      params(3)
    )))

    res.flatMap {
      s =>
        translateResult("helm install", s)
    }
  }

  def listHelm(): F[Unit] = {
    val res = blockingF(F.delay(helClient.listHelm()))
    res.flatMap {
      s =>
        translateResult("helm list", "ok")
    }
  }

  def uninstall(): F[Unit] = {
    val res = blockingF(F.delay(helClient.uninstallCloudmanRelease()))
    res.flatMap {
      s =>
        translateResult("helm list", "ok")
    }
  }

  def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => logger.info(s"${cmd} succeeded")
    case s => F.raiseError(HelmException(s))
  }

  private def blockingF[A](fa: F[A]): F[A] = concurrencyBound.withPermit(blocker.blockOn(fa))
}

final case class HelmException(message: String) extends NoStackTrace {
  override def getMessage: String = message
}