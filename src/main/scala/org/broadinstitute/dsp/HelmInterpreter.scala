package org.broadinstitute.dsp

import cats.data.Kleisli
import cats.effect.concurrent.Semaphore
import cats.effect.{Async, Blocker, ContextShift}
import com.sun.jna.Native
import io.chrisdavenport.log4cats.Logger
import org.broadinstitute.dsp.implicits._
import scala.language.implicitConversions

class HelmInterpreter[F[_]: ContextShift](blocker: Blocker, concurrencyBound: Semaphore[F])(implicit logger: Logger[F],
                                                                                            F: Async[F])
    extends HelmAlgebra[F] {

  val helmClient = Native.load("helm", classOf[HelmJnaClient])

  override def installChart(release: Release, chart: Chart, values: Values): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(
        blockingF(
          F.delay(
            helmClient.installChart(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              release,
              chart,
              values
            )
          )
        )
      )
      _ <- Kleisli.liftF(translateResult("helm installChart", r))
    } yield ()

  override def listHelm(): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(
        blockingF(
          F.delay(
            helmClient.listHelm(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile
            )
          )
        )
      )
      // TODO: Make 'helm list' return String
      _ <- Kleisli.liftF(translateResult("helm list", "ok"))
    } yield ()

  override def uninstall(release: Release): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(
        blockingF(
          F.delay(
            helmClient.uninstallRelease(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              release
            )
          )
        )
      )
      _ <- Kleisli.liftF(translateResult("helm uninstall", "ok"))
    } yield ()

  private def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => logger.info(s"The command '${cmd}' succeeded")
    case s    => F.raiseError(HelmException(s))
  }

  private def blockingF[A](fa: F[A]): F[A] = concurrencyBound.withPermit(blocker.blockOn(fa))
}
