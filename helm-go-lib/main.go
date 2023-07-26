package main

import "C"

import (
	"flag"
	"fmt"
	"github.com/Masterminds/semver/v3"
	"helm.sh/helm/v3/pkg/chart"
	"log"
	"os"
	"strings"

	"helm.sh/helm/v3/pkg/strvals"

	"github.com/golang/glog"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	"k8s.io/cli-runtime/pkg/genericclioptions"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
	"k8s.io/client-go/rest"
	"os/exec"
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

	namespace := progArgs[0]
	kubeToken := progArgs[1]
	apiServer := progArgs[2]
	caFile := progArgs[3]
	releaseName := progArgs[4]
	chartName := progArgs[5]
	chartVersion := progArgs[6]
	overrideValues := ""
	if len(progArgs) == 8 {
		overrideValues = progArgs[7]
	}

	installChart(
		namespace,
		kubeToken,
		apiServer,
		caFile,
		releaseName,
		chartName,
		chartVersion,
		overrideValues,
		true,
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
	if chartVersion == "" {
		client.Version = CheckVersion(chartName, "leonardo")
	}
	client.Namespace = namespace
	client.ReleaseName = releaseName
	client.CreateNamespace = createNamespace

	settings := cli.New()
	settings.KubeToken = kubeToken
	settings.KubeAPIServer = apiServer

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	log.Printf("chart path: %s \n", cp)

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

func CheckVersion(chart string, localChartDir string) string {
	log.Printf("Calling checkversion with chart %q, localChartDir %v", chart, localChartDir)

	chartName := chart[strings.LastIndex(chart, "/")+1:]
	log.Printf("Pulled chart name %q", chartName)

	fullLocalChartDir := localChartDir + "/" + chartName
	log.Printf("Full local chart dir %q", fullLocalChartDir)

	latestLocalVersion := ""
	// Load charts from the local directory
	charts, err := loader.LoadDir(fullLocalChartDir)
	if err != nil {
		log.Printf("Failed to load charts from directory %q: %v", fullLocalChartDir, err)
	} else {
		// Find the latest version of the specified chart
		latestLocalVersion = findLatestVersion(charts, chartName)

		if latestLocalVersion == "" {
			log.Printf("Chart %q not found in the local directory\n", chartName)
		}

		log.Printf("Latest version of %s in the local directory is %s\n", chartName, latestLocalVersion)
	}

	// Run the "helm search" command to search for the chart in remote repositories
	cmd := exec.Command("helm", "search", "repo", chartName)
	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Fatalf("Error running 'helm search': %v", err)
	}

	// Process the output to find the latest version of the chart
	latestVersion := extractLatestVersion(string(output))

	if latestVersion == "" {
		log.Printf("Chart %q not found in any remote repository.\n", chartName)
		return ""
	}

	log.Printf("Latest version of %s is %s\n", chartName, latestVersion)

	if latestLocalVersion == "" {
		replaceLocal(chart, localChartDir, fullLocalChartDir)
		return latestVersion
	}

	v1, err := semver.NewVersion(latestLocalVersion)
	if err != nil {
		log.Println("Invalid version 1:", err)
		return ""
	}

	v2, err := semver.NewVersion(latestVersion)
	if err != nil {
		log.Println("Invalid version 2:", err)
		return ""
	}

	// Compare the semantic versions
	comparison := v1.Compare(v2)

	if comparison < 0 {
		log.Printf("%s is less than %s\n", latestLocalVersion, latestVersion)
		replaceLocal(chart, localChartDir, fullLocalChartDir)

		return latestVersion

	}
	return ""
}

func replaceLocal(chart string, localChartDir string, fullLocalChartDir string) {
	//pull latest version locally
	exec.Command("rm", "-rf", fullLocalChartDir).Run()
	cmd := exec.Command("helm", "pull", "--untar", "-d", localChartDir, chart)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	err := cmd.Run()
	if err != nil {
		log.Fatalf("Error running 'helm pull': %v", err)
	}

	log.Printf("Chart %q downloaded successfully\n", chart)
}

// Function to extract the latest version from "helm search" output
func extractLatestVersion(output string) string {
	lines := strings.Split(output, "\n")
	if len(lines) >= 2 {
		// Assuming the second line contains the latest chart version
		// The output format of "helm search" is subject to change,
		// so you may need to adjust this logic based on the actual output.
		return strings.Fields(lines[1])[1]
	}
	return ""
}

// Function to find the latest version of the specified chart
func findLatestVersion(charts *chart.Chart, chartName string) string {
	var latestVersion string
	//for _, c := range charts {
	if charts.Metadata.Name == chartName {
		if latestVersion == "" || charts.Metadata.Version > latestVersion {
			latestVersion = charts.Metadata.Version
		}
	}
	//}
	return latestVersion
}
