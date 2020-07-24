package org.broadinstitute.dsp

import cats.effect.IO

/**
 * How to run the test:
 * sbt -Djna.library.path=/Users/qi/workspace/helm-scala-sdk/helm-go-lib test:console
 *
 * Once inside sbt shell, call the function directly, eg: org.broadinstitute.dsp.HelmManualTest.listHelm()
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
object HelmManualTest extends HelmScalaSdkTestSuite {
  val helmClient = new Helm[IO](blocker, semaphore)
  val authContext = AuthContext(
    Namespace("test-helm-client-0722-1-ns"), // "" is interpreted as all namespaces
//    KubeToken("your token"),
    KubeToken("eyJhbGciOiJSUzI1NiIsImtpZCI6Il8ybnZKZUhmbU5MYUcyNlpvVGxBYW5hTHdMVFpxSWVQTmJnUzZ0UXNMbGMifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1ucyIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYS10b2tlbi00ZHRmaCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjZjZTg0N2EyLTA5MGQtNDk3My04YWQ4LWFkNDIzMzQxNTBjZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDp0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1uczp0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYSJ9.mT87HdtohqFnR8vPFWMIowMzJownaiiY38JxV0IoebhdJkDzF5LRUQbmtv0qQmxnC-85PrFIRvMzWvly4u5P7iFEXVlP6Iv0g28i5iPbZycK7ASU4HnJnOhIW14SIP6-m-iI4FD5gk8SNQHsNFMMhanUcnolbj8iZDJLgVrgYr95etq6KZmu_lCQf6Ps0GtdR5axgtYXKtuVHDk27EgbWXqB_OUc6IdAyrhW4PT2dqhcqP-WhIWV0rScN2L9XHpcv_a3dsVUDUAVYdh6vPoSuXnDWpDLOf18EKSnR1AMD-mg5IOMAAbG55tuNohy-JrD1RqMUthQ2ankMhhAmREwIg"),
    KubeApiServer("https://34.66.249.164")
  )

  def installChart(): Unit = {
    helmClient.installChart(
      "bitnami-nginx-api-rls-0724-16",
      "bitnami/nginx",
      "key1.key2=v1,key3.key4.key5=v2"
    ).run(authContext)
      .unsafeRunSync()
  }

  def listHelm(): Unit = {
    helmClient.listHelm().run(authContext).unsafeRunSync()
  }
}
