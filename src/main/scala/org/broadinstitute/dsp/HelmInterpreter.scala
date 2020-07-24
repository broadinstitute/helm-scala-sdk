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
  val helmClient = Native.load(
    "helm",
    classOf[HelmJnaClient])

  def install(releaseName: String,
              chartName: String,
              setArgs: String
             ): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(blockingF(F.delay(helmClient.install(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer,
        releaseName,
        chartName,
        setArgs
      ))))
      _ <- Kleisli.liftF(translateResult("helm install", r))
    } yield ()
  }

  def listHelm(): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(blockingF(F.delay(helmClient.listHelm(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer
      ))))
      // TODO: Make 'helm list' return String
      _ <- Kleisli.liftF(translateResult("helm list", "ok"))
    } yield ()
  }

  def uninstall(): Kleisli[F, AuthContext, Unit] = {
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(blockingF(F.delay(helmClient.uninstallRelease(
        ctx.namespace,
        ctx.kubeToken,
        ctx.kubeApiServer
      ))))
      // TODO: Make 'helm uninstall' return String
      _ <- Kleisli.liftF(translateResult("helm list", "ok"))
    } yield ()
  }

  def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => println(s"The command '${cmd}' succeeded"); logger.info(s"The command '${cmd}' succeeded")
    case s => println(s"The command '${cmd}' failed with exception: ${s}"); F.raiseError(HelmException(s))
  }

  private def blockingF[A](fa: F[A]): F[A] = concurrencyBound.withPermit(blocker.blockOn(fa))
}
