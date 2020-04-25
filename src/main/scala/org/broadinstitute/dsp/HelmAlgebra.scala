package org.broadinstitute.dsp

import cats.data.Kleisli

import scala.util.control.NoStackTrace

trait HelmAlgebra[F[_]] {

  def install(
               releaseName: String,
               chartName: String,
               filePath: String
             ): Kleisli[F, AuthContext, Unit]

  def listHelm(): Kleisli[F, AuthContext, Unit]

  def uninstall(): Kleisli[F, AuthContext, Unit]
}

final case class HelmException(message: String) extends NoStackTrace {
  override def getMessage: String = message
}

final case class AuthContext(
                              namespace: Namespace,
                              kubeToken: KubeToken,
                              kubeApiServer: KubeApiServer
                            )

final case class Namespace(asString: String) extends AnyVal
final case class KubeToken(asString: String) extends AnyVal
final case class KubeApiServer(asString: String) extends AnyVal