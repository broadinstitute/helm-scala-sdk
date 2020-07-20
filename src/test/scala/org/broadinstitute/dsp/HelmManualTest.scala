package org.broadinstitute.dsp

import cats.effect.IO

/**
 * How to run this:
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell, call the function directly, eg: org.broadinstitute.dsp.HelmManualTest.listHelm()
 *
 * KubeApiServer and KubeToken can be retrieved via kubectl as described below:
 * https://kubernetes.io/docs/tasks/administer-cluster/access-cluster-api/#without-kubectl-proxy
 */
object HelmManualTest extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)
  val authContext = AuthContext(
    Namespace("test-helm-client-0716-1"), // "" is interpreted as all namespaces
//    KubeToken("your token"),
    KubeToken("eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tOHhqd2MiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjllNzc5YzI3LWFiNjAtMTFlYS1iMjY5LTQyMDEwYTgwMDBlZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.atM5y4Qd5sgTDQ-JpWCFBdVE0jZYUz0kdDDsqqnK__H1MnWsti0jVy5JNjurFH5dNbjYkDW0uW37agMCM-1hWGAFBKYYL4RQZdNNwfxGw_VXtDmWEC896OphTHDKOAbC9h-C6RfzwJC1--D3nGZfdgrqMeE6U4Fi0LXc0PIRBUM9BdgHY5Dr0s2bKsjTfUe0huru2YRNM7NZtbIPSYd2J680Mcn0Z7OpshpY0JnOkmMjGsdqw6fLLMhzGf9OHZN5LBal8aTUHRSVIgRrpXejNzjP91QCbfGMe9v9FnZNwzlltFSMsE3J-aQtw282f5o8H_djWrwv0-p-OkcX3kNQJw"),
    KubeApiServer("https://34.66.249.164")
  )

  def install(): Unit = {
    helmClient.install(
      "nginx-api-rls-0720-1",
      "nginx-stable/nginx-ingress",
//      "/Users/kyuksel/gke_experiment/galaxy-cvmfs-csi-helm/galaxy-cvmfs-csi/values.yaml"
      "/Users/kyuksel/gke_experiment/kubernetes-ingress/deployments/helm-chart/values.yaml"
    ).run(authContext)
      .unsafeRunSync()
  }

  /**
   * Requires the 'default' serviceaccount to have edit role on the cluster.
   * This can be granted via
   *  kubectl create rolebinding rb-default-edit \
   *    --clusterrole=edit \
   *    --serviceaccount=default:default \
   *    --namespace=<your-namespace-in-authContext>
   */
  def listHelm(): Unit = {
    helmClient.listHelm().run(authContext).unsafeRunSync()
  }
}
