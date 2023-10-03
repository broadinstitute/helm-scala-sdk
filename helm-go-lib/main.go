package main

import "C"

import (
	"flag"
	"fmt"
	"log"
	"os"
	"strings"

	"github.com/golang/glog"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/strvals"
	"k8s.io/cli-runtime/pkg/genericclioptions"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
	"k8s.io/client-go/rest"
)

/*
Example command line to run from within the directory this file is in:

	go run main.go \
		-logtostderr=true -stderrthreshold=INFO \
		my-namespace my-kube-token-string https://12.34.567.890 ./ca_file bitnami/nginx 6.0.5 key1=v1,key2.key3=v2
*/
func main() {
	flag.Parse()
	progArgs := flag.Args()
	lenProgArgs := len(progArgs)

	if (lenProgArgs != 7) && (lenProgArgs != 8) {
		fmt.Println("Expected args: <namespace> <kube token> <api server> <ca file> <release name> <chart name> <chart version> <values>",
			"\n\nFound:\n", strings.Join(progArgs, "\n\n\t"))
		return
	}

	// namespace := progArgs[0]
	// kubeToken := progArgs[1]
	// apiServer := progArgs[2]
	// caFile := progArgs[3]
	// releaseName := progArgs[4]
	// chartName := progArgs[5]
	// chartVersion := progArgs[6]
	// overrideValues := ""
	// if len(progArgs) == 8 {
	// 	overrideValues = progArgs[7]
	// }

	// installChart(
	// 	namespace,
	// 	kubeToken,
	// 	apiServer,
	// 	caFile,
	// 	releaseName,
	// 	chartName,
	// 	chartVersion,
	// 	overrideValues,
	// 	true,
	// )

	chartName := "terra-helm/wds"
	chartVersion := "0.31.0"

	pullChart(
		chartName,
		chartVersion,
	)

	glog.Flush()
}

//export listHelm
func listHelm(namespace string, kubeToken string, apiServer string, caFile string) *C.char {
	actionConfig, err := buildActionConfig(namespace, kubeToken, apiServer, caFile)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}
	client := action.NewList(actionConfig)
	// Only list deployed
	client.Deployed = true
	results, err := client.Run()
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	for _, rel := range results {
		log.Printf("%+v", rel)
	}
	return C.CString("ok")
}

// TODO: Do we need to make 'overrideValues' optional?
// TODO: If so, emulate it (perhaps via variadic functions) since Golang doesn't support optional parameters :(
// `HELM_DRIVER` env variable is expected to have been set to the right value (which is likely to be "secret")
//
//export installChart
func installChart(namespace string, kubeToken string, apiServer string, caFile string, releaseName string, chartName string, chartVersion string, overrideValues string, createNamespace bool) *C.char {
	actionConfig, err := buildActionConfig(namespace, kubeToken, apiServer, caFile)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	client := action.NewInstall(actionConfig)
	// TODO: Should we not update all Helm repos by default, and instead define a separate API for it?
	client.DependencyUpdate = true
	client.Version = chartVersion
	client.Namespace = namespace
	client.ReleaseName = releaseName
	client.CreateNamespace = createNamespace

	settings := cli.New()
	settings.KubeToken = kubeToken
	settings.KubeAPIServer = apiServer

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Check chart dependencies to make sure all are present in /charts
	chartRequested, err := loader.Load(cp)
	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Adapted from the example below to override chart values as in CLI --set
	// https://github.com/PrasadG193/helm-clientgo-example
	providers := getter.All(settings)
	valueOpts := &values.Options{}

	// Combine overrides from different sources, if any
	values, err := valueOpts.MergeValues(providers)
	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Add --set overrides in the form of comma-separated key=value pairs
	if err := strvals.ParseInto(overrideValues, values); err != nil {
		log.Printf("%+v", "Failed parsing --set values", err)
		return C.CString(err.Error())
	}

	_, err = client.Run(chartRequested, values)
	if err != nil {
		log.Printf("%+v", "\nerr is ", err, "\n")
		return C.CString(err.Error())
	}

	log.Println("\n\nFinished installing release: ", releaseName)

	return C.CString("ok")
}

//export uninstallRelease
func uninstallRelease(namespace string, kubeToken string, apiServer string, caFile string, releaseName string, keepHistory bool) *C.char {
	actionConfig, err := buildActionConfig(namespace, kubeToken, apiServer, caFile)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	client := action.NewUninstall(actionConfig)
	client.KeepHistory = keepHistory

	_, err = client.Run(releaseName)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	log.Printf("Finished uninstalling %s", releaseName)
	return C.CString("ok")
}

//export upgradeChart
func upgradeChart(namespace string, kubeToken string, apiServer string, caFile string, releaseName string, chartName string, chartVersion string, overrideValues string) *C.char {

	actionConfig, err := buildActionConfig(namespace, kubeToken, apiServer, caFile)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	client := action.NewUpgrade(actionConfig)
	client.Namespace = namespace
	client.Version = chartVersion
	// allow deletion of new resources created in this upgrade when upgrade fails
	client.CleanupOnFail = true
	// when upgrading, reset the values to the ones built into the chart
	client.ResetValues = true

	settings := cli.New()
	settings.KubeToken = kubeToken
	settings.KubeAPIServer = apiServer

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Check chart dependencies to make sure all are present in /charts
	chartRequested, err := loader.Load(cp)

	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Adapted from the example below to override chart values as in CLI --set
	// https://github.com/PrasadG193/helm-clientgo-example
	providers := getter.All(settings)
	valueOpts := &values.Options{}

	// Combine overrides from different sources, if any
	values, err := valueOpts.MergeValues(providers)
	if err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	// Add --set overrides in the form of comma-separated key=value pairs
	if err := strvals.ParseInto(overrideValues, values); err != nil {
		log.Printf("%+v", "Failed parsing --set values", err)
		return C.CString(err.Error())
	}

	_, err = client.Run(releaseName, chartRequested, values)
	if err != nil {
		log.Printf("%+v\n", err)
		return C.CString(err.Error())
	}

	log.Printf("Finished upgrading to release %s", releaseName)
	return C.CString("ok")
}

func buildActionConfig(namespace string, kubeToken string, apiServer string, caFile string) (actionConfig *action.Configuration, err error) {
	var kubeConfig *genericclioptions.ConfigFlags
	kubeConfig = genericclioptions.NewConfigFlags(false)
	kubeConfig.APIServer = &apiServer
	kubeConfig.BearerToken = &kubeToken
	kubeConfig.CAFile = &caFile
	kubeConfig.Namespace = &namespace

	customConfig := &CustomConfigFlags{kubeConfig}

	actionConfig = new(action.Configuration)
	if err := actionConfig.Init(customConfig, namespace, os.Getenv("HELM_DRIVER"), log.Printf); err != nil {
		return nil, err
	}

	return actionConfig, nil
}

// Override ConfigFlags struct (which implments the RESTClientGetter interface)
// to be able to hook into ToRESTConfig() to set QPS and Burst parameters on the k8s rest client.

type CustomConfigFlags struct {
	*genericclioptions.ConfigFlags
}

func (f *CustomConfigFlags) ToRESTConfig() (*rest.Config, error) {
	c, err := f.ConfigFlags.ToRESTConfig()
	if err != nil {
		return nil, err
	}

	// default is 10/5
	c.Burst = 200
	c.QPS = 50
	//log.Printf("Helm client: setting QPS to %f and Burst to %d\n", c.QPS, c.Burst)

	return c, nil
}

func pullChart(namespace string, kubeToken string, apiServer string, caFile string, chartName string, chartVersion string) {

	settings := cli.New()

	// Create a new action configuration
	actionConfig, err := buildActionConfig(namespace, kubeToken, apiServer, caFile)
	if err != nil {
		log.Printf("%+v\n", err)
	}

	// Create a new client configuration
	client := action.NewPull()
	withConfig := action.WithConfig(actionConfig)
	withConfig(client)

	client.Settings = settings

	// TODO do we need this
	// client.ChartPathOptions.RepoURL = "https://charts.helm.sh/stable"
	// client.ChartPathOptions.Version = chartVersion // Specify the desired chart version

	// TODO what should this be?
	destDir := "/leonardo"
	client.DestDir = destDir

	// Perform the chart pull operation
	result, err := client.Run(chartName)
	if err != nil {
		panic(err)
	}

	// Output success message
	fmt.Println("Chart successfully pulled to:", destDir)
	fmt.Println(result)

}
