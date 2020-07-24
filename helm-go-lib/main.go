package main

import "C"

import (
	"flag"
	"fmt"
	"helm.sh/helm/v3/pkg/strvals"
	"log"
	"os"
	"strings"

	"github.com/golang/glog"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
)

/*
	Example command line to run from within the directory this file is in:
		go run main.go \
			-logtostderr=true -stderrthreshold=INFO \
			my-namespace my-kube-token-string https://12.34.567.890 bitnami/nginx key1=v1,key2.key3=v2
*/
func main() {
	flag.Parse()
	progArgs := flag.Args()
	lenProgArgs := len(progArgs)

	if (lenProgArgs != 5) && (lenProgArgs != 6)  {
		fmt.Println("Expected args: <namespace> <kube token> <api server> <release name> <chart name> <values>",
			"\n\nFound:\n", strings.Join(progArgs, "\n\n\t"))
		return
	}

	namespace := progArgs[0]
	kubeToken := progArgs[1]
	apiServer := progArgs[2]
	releaseName := progArgs[3]
	chartName := progArgs[4]
	overrideValues := ""
	if len(progArgs) == 6 {
		overrideValues = progArgs[5]
	}

	installChart(
		namespace,
		kubeToken,
		apiServer,
		releaseName,
		chartName,
		overrideValues,
	)

	glog.Flush()
}

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

//export installChart
// TODO: Do we need to make 'overrideValues' optional?
// TODO: If so, emulate it (perhaps via variadic functions) since Golang doesn't support optional parameters :(
func installChart(namespace string, kubeToken string, apiServer string, releaseName string, chartName string, overrideValues string) *C.char {
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
		log.Fatalf("%+v", err)
		return C.CString(err.Error())
	}

	client := action.NewInstall(actionConfig)
	client.DependencyUpdate = true
	client.Namespace = namespace
	client.ReleaseName = releaseName
	client.Atomic = true
	//client.DryRun = true

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	if err != nil {
		log.Fatalf("%+v", err)
	}

	// Check chart dependencies to make sure all are present in /charts
	chartRequested, err := loader.Load(cp)
	if err != nil {
		log.Fatalf("%+v", err)
	}

	// Adapted from the example below to override chart values as in CLI --set
	// https://github.com/PrasadG193/helm-clientgo-example
	providers := getter.All(settings)
	valueOpts := &values.Options{}

	// Combine overrides from different sources, if any
	values, err := valueOpts.MergeValues(providers)
	if err != nil {
		log.Fatalf("%+v", err)
	}

	// Add --set overrides in the form of comma-separated key=value pairs
	if err := strvals.ParseInto(overrideValues, values); err != nil {
		log.Fatal("Failed parsing --set values", err)
	}
	
	_, err = client.Run(chartRequested, values)
	if err != nil {
		log.Fatalf("%+v", "\nerr is ", err, "\n")
	}

	log.Println("\n\nFinished installing release: ", releaseName)

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
