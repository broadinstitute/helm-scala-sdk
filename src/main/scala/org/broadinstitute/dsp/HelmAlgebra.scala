package org.broadinstitute.dsp

import java.nio.file.Path

import cats.data.Kleisli

import scala.util.control.NoStackTrace

trait HelmAlgebra[F[_]] {
  def installChart(
    release: Release,
    chartName: ChartName,
    chartVersion: ChartVersion,
    values: Values,
    createNamespace: Boolean = false
  ): Kleisli[F, AuthContext, Unit]

  def listHelm(): Kleisli[F, AuthContext, Unit]

  def updateAndPullChart(chartName: ChartName, chartVersion: ChartVersion, destDir: String): Kleisli[F, Unit, Unit]

  // Set keepHistory to true to make helm retain a record to the release which can make debuggingg easier.
  // https://helm.sh/docs/intro/using_helm/#helm-uninstall-uninstalling-a-release
  def uninstall(release: Release, keepHistory: Boolean = false): Kleisli[F, AuthContext, Unit]

  def upgradeChart(
    release: Release,
    chartName: ChartName,
    chartVersion: ChartVersion,
    values: Values
  ): Kleisli[F, AuthContext, Unit]

}

final case class HelmException(message: String) extends NoStackTrace {
  override def getMessage: String = message
}

final case class AuthContext(
  namespace: Namespace,
  kubeToken: KubeToken,
  kubeApiServer: KubeApiServer,
  caCertFile: CaCertFile
)

final case class Namespace(asString: String) extends AnyVal
final case class Release(asString: String) extends AnyVal

final case class ChartName(asString: String) extends AnyVal
final case class ChartVersion(asString: String) extends AnyVal

// If running on GKE, this can be a Google access token as long as it
// includes GKE scopes.
final case class KubeToken(asString: String) extends AnyVal

// Note: this value should be a full URL (e.g. https://<ip-address>)
final case class KubeApiServer(asString: String) extends AnyVal

// Path to a cert file for the certificate authority
// Can be retrieved from the GKE cluster as cluster.getMasterAuth.getClusterCaCertificate
// Unfortunately the Go client requires a file path; it can't accept the data as a string or stream.
final case class CaCertFile(path: Path) extends AnyVal

// Values can be in comma-separated key/value pair format as in CLI --set (e.g. "key1=v1,key2.key3=v2")
// More complex use cases are also supported as depicted below
// https://helm.sh/docs/intro/using_helm/#the-format-and-limitations-of---set
// TODO: Look into better representing values (e.g. Map[String, String])
// once we know what fulfils our use case for overriding chart values
final case class Values(asString: String) extends AnyVal
