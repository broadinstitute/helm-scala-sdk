package org.broadinstitute.dsp

import cats.data.Kleisli
import cats.effect.std.Semaphore
import cats.effect.{Async}
import cats.implicits._
import com.sun.jna.Native
import org.broadinstitute.dsp.implicits._
import org.typelevel.log4cats.Logger

import scala.language.implicitConversions

class HelmInterpreter[F[_]](concurrencyBound: Semaphore[F])(implicit logger: Logger[F], F: Async[F])
    extends HelmAlgebra[F] {

  val helmClient = Native.load("helm", classOf[HelmJnaClient])

  override def installChart(release: Release,
                            chartName: ChartName,
                            chartVersion: ChartVersion,
                            values: Values,
                            createNamespace: Boolean = false): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(
        boundF(
          F.delay(
            helmClient.installChart(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              release,
              chartName,
              chartVersion,
              values,
              createNamespace
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
        boundF(
          F.blocking(
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

  def pullChart(chartName: ChartName, chartVersion: ChartVersion, destDir: String): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(
        boundF(
          F.blocking(
            helmClient.pullChart(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              chartName,
              chartVersion,
              destDir
            )
          )
        )
      )
      // TODO: Make 'helm list' return String
      _ <- Kleisli.liftF(translateResult("helm pullChart", "ok"))
    } yield ()

  override def uninstall(release: Release, keepHistory: Boolean): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      _ <- Kleisli.liftF(
        boundF(
          F.blocking(
            helmClient.uninstallRelease(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              release,
              keepHistory
            )
          )
        )
      )
      _ <- Kleisli.liftF(translateResult("helm uninstall", "ok"))
    } yield ()

  override def upgradeChart(release: Release,
                            chartName: ChartName,
                            chartVersion: ChartVersion,
                            values: Values): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(
        boundF(
          F.delay(
            helmClient.upgradeChart(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              release,
              chartName,
              chartVersion,
              values
            )
          )
        )
      )
      _ <- Kleisli.liftF(translateResult("helm upgradeChart", r))
    } yield ()

  override def pullChart(
    chartName: ChartName,
    chartVersion: ChartVersion
  ): Kleisli[F, AuthContext, Unit] =
    for {
      ctx <- Kleisli.ask[F, AuthContext]
      r <- Kleisli.liftF(
        boundF(
          F.delay(
            helmClient.pullChart(
              ctx.namespace,
              ctx.kubeToken,
              ctx.kubeApiServer,
              ctx.caCertFile,
              chartName,
              chartVersion
            )
          )
        )
      )
      _ <- Kleisli.liftF(translateResult("helm pullChart", r))
    } yield ()

  private def translateResult(cmd: String, result: String): F[Unit] = result match {
    case "ok" => logger.info(s"The command '${cmd}' succeeded")
    case s if s.contains("cannot re-use a name that is still in use") =>
      logger.info(
        s"The command ${cmd} encountered a conflict. This is likely a resubmission of the same command. This will not raise an exception to ensure idempotency. Helm command output: ${s}"
      )
    case s => logger.info(s"The command '${cmd}' failed with result $result") >> F.raiseError(HelmException(s))
  }

  private def boundF[A](fa: F[A]): F[A] = concurrencyBound.permit.use(_ => fa)
}
