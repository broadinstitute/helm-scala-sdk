package main

import "C"

import (
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/strvals"
	"log"
	"os"

	"github.com/golang/glog"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
)

//export listHelm
func listHelm(namespace, kubeToken, apiServer string) {
	settings := cli.New()
	settings.KubeToken = kubeToken
	settings.KubeAPIServer = apiServer

	// kubeConfig := kube.GetConfig("/Users/qi/.kube/config  ", "", "galaxy")
	actionConfig := new(action.Configuration)
	// You can pass an empty string instead of settings.Namespace() to list all namespaces
	if err := actionConfig.Init(settings.RESTClientGetter(), namespace, os.Getenv("HELM_DRIVER"), log.Printf); err != nil {
		log.Printf("%+v", err)
		os.Exit(1)
	}

	client := action.NewList(actionConfig)
	// Only list deployed
	client.Deployed = true
	results, err := client.Run()
	if err != nil {
		log.Printf("%+v", err)
		os.Exit(1)
	}

	for _, rel := range results {
		log.Printf("%+v", rel)
	}
}

//export install
func install(namespace string, kubeToken string, apiServer string, releaseName string, chartName string, setArgs string) *C.char {
	// cli.New() gets the deployment namespace from env variable so we're setting it below
	// namespace we pass into actionConfig.Init sets the release namespace, not the deployment namespace
	// TODO see if we can create a custom EnvSettings with values below overridden instead of setting env variables
	os.Setenv("HELM_NAMESPACE", namespace)
	os.Setenv("HELM_KUBETOKEN", kubeToken)
	os.Setenv("HELM_KUBEAPISERVER", apiServer)
	settings := cli.New()

	actionConfig := new(action.Configuration)
	// You can pass an empty string instead of settings.Namespace() to list all namespaces
	if err := actionConfig.Init(settings.RESTClientGetter(), namespace, os.Getenv("HELM_DRIVER"), log.Printf); err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	n, _, _ := settings.RESTClientGetter().ToRawKubeConfigLoader().Namespace()
	kt := settings.KubeToken
	fmt.Println("RESTClientGetter namespace is ", n)
	fmt.Println("KubeToken is ", kt)

	client := action.NewInstall(actionConfig)
	client.DependencyUpdate = true
	client.Namespace = namespace
	client.ReleaseName = releaseName
	client.Atomic = true
	//client.DryRun = true

	fmt.Println("client.Namespace is ", client.Namespace)

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	if err != nil {
		return C.CString(err.Error())
	}

	// Check chart dependencies to make sure all are present in /charts
	chartRequested, err := loader.Load(cp)
	//fmt.Printf("chartRequested is ", *chartRequested)
	if err != nil {
		return C.CString(err.Error())
	}

	// Adapted from the example below to override chart values as in CLI --set
	// https://github.com/PrasadG193/helm-clientgo-example
	providers := getter.All(settings)
	valueOpts := &values.Options{}

	// Combine overrides from different sources, if any
	values, err := valueOpts.MergeValues(providers)
	if err != nil {
		log.Fatal(err)
	}

	// Add --set overrides in the form of comma-separated key=value pairs
	if err := strvals.ParseInto(setArgs, values); err != nil {
		log.Fatal(errors.Wrap(err, "failed parsing --set data"))
	}
	
	rls, err := client.Run(chartRequested, values)
	fmt.Printf("%+v", "err is ", err)
	fmt.Printf("%+v", "rls is ", rls)
	if err != nil {
		return C.CString(err.Error())
	}

	glog.Info("\nFinished installing %s", releaseName)
	return C.CString("ok")
}

//export uninstallRelease
func uninstallRelease(namespace, kubeToken, apiServer, releaseName string) *C.char {
	settings := cli.New()
	actionConfig := new(action.Configuration)
	err := actionConfig.Init(settings.RESTClientGetter(), namespace, os.Getenv("HELM_DRIVER"), glog.Infof)
	if err != nil {
		return C.CString(err.Error())
	}

	client := action.NewUninstall(actionConfig)
	_, err = client.Run(releaseName)
	if err != nil {
		return C.CString(err.Error())
	}

	glog.Info("Finished installing %s", releaseName)
	return C.CString("ok")
}

func main() {
	defaultSaToken := "eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tOHhqd2MiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjllNzc5YzI3LWFiNjAtMTFlYS1iMjY5LTQyMDEwYTgwMDBlZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.atM5y4Qd5sgTDQ-JpWCFBdVE0jZYUz0kdDDsqqnK__H1MnWsti0jVy5JNjurFH5dNbjYkDW0uW37agMCM-1hWGAFBKYYL4RQZdNNwfxGw_VXtDmWEC896OphTHDKOAbC9h-C6RfzwJC1--D3nGZfdgrqMeE6U4Fi0LXc0PIRBUM9BdgHY5Dr0s2bKsjTfUe0huru2YRNM7NZtbIPSYd2J680Mcn0Z7OpshpY0JnOkmMjGsdqw6fLLMhzGf9OHZN5LBal8aTUHRSVIgRrpXejNzjP91QCbfGMe9v9FnZNwzlltFSMsE3J-aQtw282f5o8H_djWrwv0-p-OkcX3kNQJw"
	// test-helm-client-0722-1-sa
	customSaToken := "eyJhbGciOiJSUzI1NiIsImtpZCI6Il8ybnZKZUhmbU5MYUcyNlpvVGxBYW5hTHdMVFpxSWVQTmJnUzZ0UXNMbGMifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1ucyIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYS10b2tlbi00ZHRmaCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJ0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjZjZTg0N2EyLTA5MGQtNDk3My04YWQ4LWFkNDIzMzQxNTBjZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDp0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1uczp0ZXN0LWhlbG0tY2xpZW50LTA3MjItMS1zYSJ9.mT87HdtohqFnR8vPFWMIowMzJownaiiY38JxV0IoebhdJkDzF5LRUQbmtv0qQmxnC-85PrFIRvMzWvly4u5P7iFEXVlP6Iv0g28i5iPbZycK7ASU4HnJnOhIW14SIP6-m-iI4FD5gk8SNQHsNFMMhanUcnolbj8iZDJLgVrgYr95etq6KZmu_lCQf6Ps0GtdR5axgtYXKtuVHDk27EgbWXqB_OUc6IdAyrhW4PT2dqhcqP-WhIWV0rScN2L9XHpcv_a3dsVUDUAVYdh6vPoSuXnDWpDLOf18EKSnR1AMD-mg5IOMAAbG55tuNohy-JrD1RqMUthQ2ankMhhAmREwIg"

	_ = defaultSaToken
	kubeToken := customSaToken

	// comma seperated values to set
	nginxArgs := "home=dummy-home,appVersion=blah-version"
	//mysqlArgs := map[string]string{ "set": "mysqlRootPassword=admin@123,persistence.enabled=false,imagePullPolicy=Always" }

	install(
		"test-helm-client-0722-1-ns",
		kubeToken,
		"https://34.66.249.164",
		"bitnami-nginx-api-rls-0724-1",
		"bitnami/nginx",
		nginxArgs,
		)
}
