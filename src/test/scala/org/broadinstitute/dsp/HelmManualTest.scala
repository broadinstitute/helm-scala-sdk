package org.broadinstitute.dsp

import java.nio.file.Path
import java.nio.file.Paths
import cats.effect.IO
import cats.effect.unsafe.implicits.global

/**
 *
 * run the `helm repo` commands in the Usage section of the Readme
 *
 * GOOGLE
 * Create a cromwell app on your BEE from a GCP workspace and export the google project id, kubernetes cluster name and the kubernetes cluster region (available in the KUBERNETES_CLUSTERS table)
 * export CLUSTER=<cluster-name> (e.g. kb771641-52ff-418f-acf4-8831f70cbc2e)
 * export PROJECT=<google-project> (e.g. terra-quality-ecf0104e)
 * export REGION=<cluster-region> (e.g. us-central1-a)
 * gcloud auth login (your broadinstitute email should work)
 * gcloud container clusters get-credentials $CLUSTER --project $PROJECT --zone $REGION
 * APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"gke_${PROJECT}_${REGION}_${CLUSTER}\")].cluster.server}")
 * If you don't have .jq installed, brew install jq`, make sure you are on the VPN as well
 * TOKEN=$(kubectl get secrets -o jsonpath="{.items[?(@.metadata.annotations['kubernetes\.io/service-account\.name']=='default')].data.token}"|base64 --decode)
 * gcloud container clusters describe $CLUSTER --project $PROJECT --zone $REGION --format=json | jq .masterAuth.clusterCaCertificate | tr -d '"' | base64 --decode > temp.cert
 * kubectl create ns test-namespace
 * kubectl create rolebinding rb-default-edit --clusterrole=edit --serviceaccount=test-namespace:default --namespace=test-namespace
 *
 * * sbt -Djna.library.path=<PATH TO YOUR HELM SCALA SDK REPO ROOT>/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = "test-namespace"; val token = "<PASS TOKEN VALUE HERE>"; val apiServer = "<PASS APISERVER VALUE HERE>"; val caCertFile = "<PASS PATH TO CERTIFICATE HERE>";
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer, caCertFile)
 *    val release = "cromwell-rls"; val chartName = "cromwell-helm/cromwell"; val chartVersion = "0.2.217"; val values = ""
 *    test.callInstallChart(release, chartName, chartVersion, values)
 *    val newChartVersion = "0.2.218"; val newValues = ""
 *    test.callUpgradeChart(release, chartName, newChartVersion, newValues)
 *
 *
 * AZURE
 * Create a cromwell app on your BEE from an Azure workspace and export the Managed resource group and landing zone (available in the KUBERNETES_CLUSTERS table)
 * export LANDING_ZONE=<cluster-name> (e.g. lz7d0e4d5d10a387b4eac4495)
 * export MRG=<mrg-name> (e.g. mrg-terra-dev-previ-20230214105302)
 * Login with your test.firecloud account so you have permission to list the clusters
 * az login
 * az aks get-credentials --resource-group $MRG --name $LANDING_ZONE
 * APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"${LANDING_ZONE}\")].cluster.server}")
 * TOKEN=$(kubectl config view -o jsonpath="{.users[?(@.name==\"clusterUser_${MRG}_${LANDING_ZONE}\")].user.token}" --raw )
 * kubectl config view -o jsonpath="{.clusters[?(@.name==\"${LANDING_ZONE}\")].cluster.certificate-authority-data}" --raw  | base64 --decode > temp.cert
 * kubectl create ns test-namespace
 * kubectl create rolebinding rb-default-edit --clusterrole=edit --serviceaccount=test-namespace:default --namespace=test-namespace
 *
 * * sbt -Djna.library.path=<PATH TO YOUR HELM SCALA SDK REPO ROOT>/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = "test-namespace"; val token = "<PASS TOKEN VALUE HERE>"; val apiServer = "<PASS APISERVER VALUE HERE>"; val caCertFile = "temp.cert";
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer, caCertFile)
 *    val release = "coa-rls"; val chartName = "cromwell-helm/cromwell-on-azure"; val chartVersion = "0.2.213"; val values = ""
 *    test.callInstallChart(release, chartName, chartVersion, values)
 *    val newChartVersion = " 0.2.216"; val newValues = ""
 *    test.callUpgradeChart(release, chartName, newChartVersion, newValues)
 *
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

  def callUpgradeChart(release: String, chartName: String, chartVersion: String, values: String): Unit =
    helmClient
      .upgradeChart(Release(release), ChartName(chartName), ChartVersion(chartVersion), Values(values))
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
