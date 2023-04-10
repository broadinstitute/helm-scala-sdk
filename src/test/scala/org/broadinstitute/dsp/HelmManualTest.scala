package org.broadinstitute.dsp

import java.nio.file.Path
import java.nio.file.Paths
import cats.effect.IO
import cats.effect.unsafe.implicits.global

/**
 * How to run the test:
 *
 *
 * run the `helm repo` commands in the Usage section of the Readme
 * export CLUSTER=<cluster-name>
 * export PROJECT=<google-project>
 * gcloud auth application-default login
 * kubectl container clusters create $CLUSTER --project $PROJECT --region us-central1-a
 * gcloud container clusters get-credentials --project $PROJECT --zone us-central1 $CLUSTER
 * APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"gke_${PROJECT}_us-central1_${CLUSTER}\")].cluster.server}")
 * TOKEN=$(kubectl get secrets -o jsonpath="{.items[?(@.metadata.annotations['kubernetes\.io/service-account\.name']=='default')].data.token}"|base64 --decode)
 * # If you don't have .jq installed, brew install jq`
 * gcloud container clusters describe test-cluster --zone us-central1 --format=json | jq .masterAuth.clusterCaCertificate | tr -d '"' | base64 --decode > temp.cert
 * kubectl create ns test-namespace
 * kubectl create rolebinding rb-default-edit --clusterrole=edit --serviceaccount=test-namespace:default --namespace=test-namespace
 *
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = test-namespace; val token = ...; val apiServer = ...; val caCertFile = ...;
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer, caCertFile)
 *    val release = "gxy-rls"; val chartName = "galaxy/galaxykubeman"; val chartVersion = "0.7.2"; val values = ""
 *    test.callInstallChart(release, chartName, chartVersion, values)
 *    val newChartVersion = "0.7.3"; val newValues = ""
 *    test.callUpgradeChart(release, chartName, NewChartVersion, newValues)
 *
 *
 * The ServiceAccount associated with the token needs to have a role sufficient for the release to perform
 * the required operations on the cluster. For example, to grant 'edit' role across the cluster:
 *
 */
final class HelmManualTest(namespace: String, token: String, apiServer: String, caCertFile: String)
    extends HelmScalaSdkTestSuite {
  val helmClient = new HelmInterpreter[IO](semaphore)
  val authContext = AuthContext(
    Namespace(namespace), // "" is interpreted as all namespaces
    KubeToken(token),
    KubeApiServer(apiServer),
    CaCertFile(Paths.get(caCertFile))
  )

  def callInstallChart(release: String, chartName: String, chartVersion: String, values: String): Unit =
    helmClient
      .installChart(Release(release), ChartName(chartName), ChartVersion(chartVersion), Values(values))
      .run(authContext)
      .unsafeRunSync()

  def listHelm(): Unit =
    helmClient
      .listHelm()
      .run(authContext)
      .unsafeRunSync()

  def callUninstall(release: String, keepHistory: Boolean): Unit =
    helmClient
      .uninstall(Release(release), keepHistory)
      .run(authContext)
      .unsafeRunSync()
}
