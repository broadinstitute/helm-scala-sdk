package org.broadinstitute.dsp

import cats.data.Kleisli

import scala.util.control.NoStackTrace

trait HelmAlgebra[F[_]] {
  // 'values' can be in comma-separated key/value pair format as in CLI --set (e.g. "key1=v1,key2.key3=v2")
  // More complex use cases are also supported as depicted below
  // https://helm.sh/docs/intro/using_helm/#the-format-and-limitations-of---set
  def installChart(
    release: Release,
    chart: Chart,
    // TODO: Look into better representing values (e.g. Map[String, String])
    // once we know what fulfils our use case for overriding chart values
    values: Values
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
final case class Release(asString: String) extends AnyVal
final case class Chart(asString: String) extends AnyVal
final case class Values(asString: String) extends AnyVal
