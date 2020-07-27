package org.broadinstitute.dsp

import cats.effect.IO

/**
 * How to run the test:
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell:
 *    val namespace = ...; val token = ...; ...
 *    val test = new org.broadinstitute.dsp.HelmManualTest(namespace, token, apiServer)
 *    test.installChart(release, chart, values)
 *
 * KubeApiServer and KubeToken can be retrieved via kubectl as described below:
 * https://kubernetes.io/docs/tasks/administer-cluster/access-cluster-api/#without-kubectl-proxy
 *
 * The ServiceAccount associated with the token needs to have a role sufficient for the release to perform
 * the required operations on the cluster. For example, to grant 'edit' role across the cluster:
 *    kubectl create rolebinding rb-default-edit \
 *      --clusterrole=edit \
 *      --serviceaccount=<your-namespace>:<your-serviceaccount> \
 *      --namespace=<your-namespace>
 */
final class HelmManualTest(namespace: String, token: String, apiServer: String) extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)
  val authContext = AuthContext(
    Namespace(namespace), // "" is interpreted as all namespaces
    KubeToken(token),
    KubeApiServer(apiServer)
  )

  def callInstallChart(release: String, chart: String, values: String): Unit = {
    helmClient
      .installChart(Release(release), Chart(chart), Value(values))
      .run(authContext)
      .unsafeRunSync()
  }

  def listHelm(): Unit = {
    helmClient
      .listHelm()
      .run(authContext)
      .unsafeRunSync()
  }
}
