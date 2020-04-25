package org.broadinstitute.dsp

import cats.data.Kleisli
import cats.effect.concurrent.Semaphore
import cats.effect.{Async, Blocker, ContextShift}
import cats.implicits._
import com.sun.jna.Native
import io.chrisdavenport.log4cats.Logger
import org.broadinstitute.dsp.implicits._
import scala.language.implicitConversions

class Helm[F[_]: ContextShift](blocker: Blocker,
                               concurrencyBound: Semaphore[F])(implicit logger: Logger[F], F: Async[F]) {
  val helClient = Native.load(
    "helm",
    classOf[HelmJnaClient])

  def install(releaseName: String,
             chartName: String,
             filePath: String
             ): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(blockingF(F.delay(helClient.install(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer,
        releaseName,
        chartName,
        filePath
      ))))
      _ <- Kleisli.liftF(translateResult("helm install", r))
    } yield ()
  }

  def listHelm(): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(blockingF(F.delay(helClient.listHelm(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer
      ))))
      _ <- Kleisli.liftF(translateResult("helm list", "ok")) //make listHelm return String
    } yield ()
  }

  def uninstall(): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(blockingF(F.delay(helClient.uninstallRelease(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer
      ))))
      _ <- Kleisli.liftF(translateResult("helm list", "ok")) //make listHelm return String
    } yield ()
  }

  def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => logger.info(s"${cmd} succeeded")
    case s => F.raiseError(HelmException(s))
  }

  private def blockingF[A](fa: F[A]): F[A] = concurrencyBound.withPermit(blocker.blockOn(fa))
}
