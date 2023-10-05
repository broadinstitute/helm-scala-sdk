package org.broadinstitute.dsp

import java.nio.file.Paths
import cats.effect.IO
import cats.effect.unsafe.implicits.global

/**
 *
 * run the `helm repo` commands in the Usage section of the Readme
 * you might want to test this on existing clusters / apps that you might have running in your BEE or Dev environments,
 * so running the following query in your leonardo DB will give you the information you need to set things up:
 *
 *  SELECT
    k.cloudProvider,
    k.cloudContext, # Either the google project id, or the azure MRG (last substring)
    k.clusterName,
    k.location,
    a.appType,
    a.appName,
    a.chart,
    a.release,
    ns.namespaceName,
    a.creator,
    a.workspaceId
    FROM APP a, NODEPOOL np, KUBERNETES_CLUSTER k, NAMESPACE ns
    WHERE k.id = np.clusterId
    AND np.id = a.nodepoolId
    AND a.namespaceId = ns.id
    AND a.status = 'RUNNING'
    AND a.creator = '<PASS YOUR EMAIL ADDRESS>;
 *
 *
 * GOOGLE
 * Create a cromwell app on your BEE from a GCP workspace and export the google project id, kubernetes cluster name and the kubernetes cluster region
 * export CLUSTER=<cluster-name> (e.g. kb771641-52ff-418f-acf4-8831f70cbc2e)
 * export PROJECT=<google-project> (e.g. terra-quality-ecf0104e)
 * export LOCATION=<cluster-location> (e.g. us-central1-a)
 * export NAMESPACE=<ns-name> (e.g. qyq2do-cromwell-ns)
 * export RELEASE=<rs-name> (e.g. qyq2do-cromwell-rls)
 * gcloud auth login (your broadinstitute email should work)
 * gcloud container clusters get-credentials $CLUSTER --project $PROJECT --zone $LOCATION
 * APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"gke_${PROJECT}_${LOCATION}_${CLUSTER}\")].cluster.server}")
 * If you don't have .jq installed, brew install jq`, make sure you are on the VPN as well
 * TOKEN=$(kubectl get secrets -o jsonpath="{.items[?(@.metadata.annotations['kubernetes\.io/service-account\.name']=='default')].data.token}"|base64 --decode)
 * gcloud container clusters describe $CLUSTER --project $PROJECT --zone $LOCATION --format=json | jq .masterAuth.clusterCaCertificate | tr -d '"' | base64 --decode > temp.cert
 * You can extract the values of the existing release in your namespace by running the command
 * helm get values ${RELEASE} -n ${NAMESPACE}
 * Now this bit is annoying, but copy/paste the yaml output into a text editor (like VSCode), and reformat it into the list of strings
 * that the scala function expects (see example: https://github.com/DataBiosphere/leonardo/blob/3f57bc933f3d7448fedae164bb8243bdb5e1c09d/http/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/util/AKSInterpreter.scala#L515)
 *
 * If you want to test the install function then you need to create a new namespace
 * kubectl create ns test-namespace
 * kubectl create rolebinding rb-default-edit --clusterrole=edit --serviceaccount=test-namespace:default --namespace=test-namespace
 *
 *
 * * sbt -Djna.library.path=<PATH TO YOUR HELM SCALA SDK REPO ROOT>/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = "<PASS NAMESPACE NAME>"; val token = "<PASS TOKEN VALUE>"; val apiServer = "<PASS APISERVER VALUE>"; val caCertFile = "<PASS PATH TO CERT FILE>";
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer, caCertFile)
 *
 *    // If you want to test the install
 *    val release = "<PASS RELEASE NAME>"; val chartName = "cromwell-helm/cromwell"; val chartVersion = "<PASS CHART VERSION YOU WANT TO INSTALL>"; val values = List(<PASS REFORMATTED VALUES>).mkString(",")

 *    test.callInstallChart(release, chartName, chartVersion, values)
 *    // If you want to test the upgrade
 *    val release = "<PASS RELEASE NAME>"; val chartName = "cromwell-helm/cromwell-on-azure";
 *    val newChartVersion = "<PASS NEW CHART VERSION>"; val newValues = List(<PASS REFORMATTED VALUES>).mkString(",")
 *    test.callUpgradeChart(release, chartName, newChartVersion, newValues)
 *
 *
 * AZURE
 * Create a cromwell app on your BEE from an Azure workspace and export the Managed resource group
 * export MRG=<mrg-name> (e.g. mrg-terra-dev-previ-20230214105302)
 * export NAMESPACE=<ns-name> (e.g. ojnm62-coa-ns)
 * export RELEASE=<rs-name> (e.g. ojnm62-coa-rls)
 * Login with your test.firecloud account so you have permission to list the clusters
 * az login
 * export CLUSTER=$(az aks list -g $MRG  --query "[].name" -o tsv)
 * az aks get-credentials --resource-group $MRG --name $CLUSTER
 * APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"${CLUSTER}\")].cluster.server}")
 * TOKEN=$(kubectl config view -o jsonpath="{.users[?(@.name==\"clusterUser_${MRG}_${CLUSTER}\")].user.token}" --raw )
 * kubectl config view -o jsonpath="{.clusters[?(@.name==\"${CLUSTER}\")].cluster.certificate-authority-data}" --raw  | base64 --decode > temp.cert
 * You can extract the values of the existing release in your namespace by running the command
 * helm get values ${RELEASE} -n ${NAMESPACE}
 * Now this bit is annoying, but copy/paste the yaml output into a text editor, and reformat it into the list of strings
 * that the scala function expects (see example: https://github.com/DataBiosphere/leonardo/blob/3f57bc933f3d7448fedae164bb8243bdb5e1c09d/http/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/util/AKSInterpreter.scala#L515)
 *
 * If you want to test the install function then you need to create a new namespace
 * kubectl create ns test-namespace
 * kubectl create rolebinding rb-default-edit --clusterrole=edit --serviceaccount=test-namespace:default --namespace=test-namespace
 *
 * * sbt -Djna.library.path=<PATH TO YOUR HELM SCALA SDK REPO ROOT>/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = "<PASS NAMESPACE NAME>"; val token = "<PASS TOKEN VALUE>"; val apiServer = "<PASS APISERVER VALUE>"; val caCertFile = "<PASS PATH TO CERT FILE>";
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer, caCertFile)
 *
 *    // If you want to test the install
 *    val release = "<PASS RELEASE NAME>"; val chartName = "cromwell-helm/cromwell-on-azure"; val chartVersion = "0.2.213"; val values = List(<PASS REFORMATTED VALUES>).mkString(",")

 *    test.callInstallChart(release, chartName, chartVersion, values)
 *    // If you want to test the upgrade
 *    val release = "<PASS RELEASE NAME>"; val chartName = "cromwell-helm/cromwell-on-azure";
 *    val newChartVersion = "<PASS NEW CHART VERSION>"; val newValues = List(<PASS REFORMATTED VALUES>).mkString(",")
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

  def callUpdateAndPull(chartName: String, chartVersion: String, destDir: String): Unit =
    helmClient
      .updateAndPull(ChartName(chartName), ChartVersion(chartVersion), destDir)
      .run(authContext)
      .unsafeRunSync()

  def callUninstall(release: String, keepHistory: Boolean): Unit =
    helmClient
      .uninstall(Release(release), keepHistory)
      .run(authContext)
      .unsafeRunSync()

}
