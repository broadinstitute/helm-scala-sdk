package main

import "C"

import (
	"io/ioutil"
	"log"
	"net/url"
	"os"
	"strings"

	"github.com/golang/glog"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/getter"
	_ "k8s.io/client-go/plugin/pkg/client/auth"
	"sigs.k8s.io/yaml"
)

//export listHelm
func listHelm(namespace, kubeToken, apiServer string) {
	settings := cli.New()
	settings.KubeToken = kubeToken
	settings.KubeAPIServer = apiServer

	// kubConfig := kube.GetConfig("/Users/qi/.kube/config  ", "", "galaxy")
	actionConfig := new(action.Configuration)
	// You can pass an empty string instead of settings.Namespace() to list
	// all namespaces
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
func install(namespace, releaseName, chartName, filePath string) *C.char {
	settings := cli.New()

	actionConfig := new(action.Configuration)
	// You can pass an empty string instead of settings.Namespace() to list
	// all namespaces
	if err := actionConfig.Init(settings.RESTClientGetter(), namespace, os.Getenv("HELM_DRIVER"), log.Printf); err != nil {
		log.Printf("%+v", err)
		return C.CString(err.Error())
	}

	client := action.NewInstall(actionConfig)
	client.DependencyUpdate = true
	client.Namespace = namespace
	client.ReleaseName = releaseName
	client.Atomic = true
	// client.DryRun = true

	cp, err := client.ChartPathOptions.LocateChart(chartName, settings)
	if err != nil {
		return C.CString(err.Error())
	}

	// Check chart dependencies to make sure all are present in /charts
	chartRequested, err := loader.Load(cp)
	if err != nil {
		return C.CString(err.Error())
	}

	p := getter.All(settings)
	base := map[string]interface{}{
		"persistence.accessMode":              "ReadWriteMany",
		"persistence.storageClass":            "nfs",
		"image.tag":                           "20.01-dev",
		"service.type":                        "LoadBalancer",
		"service.port":                        "80",
		"ingress.enabled":                     "false",
		"postgresql.persistence.storageClass": "standard",
	}

	currentMap := map[string]interface{}{}
	bytes, err := readFile(filePath, p)
	if err != nil {
		return C.CString(err.Error())
	}

	if err := yaml.Unmarshal(bytes, &currentMap); err != nil {
		return C.CString("failed to parse values")
	}
	// Merge with the previous map
	finalOpts := mergeMaps(base, currentMap)

	_, err = client.Run(chartRequested, finalOpts)
	if err != nil {
		return C.CString(err.Error())
	}

	glog.Info("Finished installing %s", releaseName)
	return C.CString("ok")
}

func uninstallRelease(namespace, releaseName string) string {
	// actionConfig, err := actionConfigInit(namespace)
	settings := cli.New()
	actionConfig := new(action.Configuration)
	err := actionConfig.Init(settings.RESTClientGetter(), namespace, os.Getenv("HELM_DRIVER"), glog.Infof)
	if err != nil {
		return err.Error()
	}

	if err != nil {
		return err.Error()
	}
	client := action.NewUninstall(actionConfig)
	_, err = client.Run(releaseName)
	if err != nil {
		return err.Error()
	}

	glog.Info("Finished installing %s", releaseName)
	return "ok"
}

func mergeMaps(a, b map[string]interface{}) map[string]interface{} {
	out := make(map[string]interface{}, len(a))
	for k, v := range a {
		out[k] = v
	}
	for k, v := range b {
		if v, ok := v.(map[string]interface{}); ok {
			if bv, ok := out[k]; ok {
				if bv, ok := bv.(map[string]interface{}); ok {
					out[k] = mergeMaps(bv, v)
					continue
				}
			}
		}
		out[k] = v
	}
	return out
}

// readFile load a file from stdin, the local directory, or a remote file with a url.
func readFile(filePath string, p getter.Providers) ([]byte, error) {
	if strings.TrimSpace(filePath) == "-" {
		return ioutil.ReadAll(os.Stdin)
	}
	u, _ := url.Parse(filePath)

	g, err := p.ByScheme(u.Scheme)
	if err != nil {
		return ioutil.ReadFile(filePath)
	}
	data, err := g.Get(filePath, getter.WithURL(filePath))
	return data.Bytes(), err
}

//export uninstallCloudmanRelease
func uninstallCloudmanRelease() {
	res := uninstallRelease("galaxy", "ky-glxy-0409-rls-galaxy")
	glog.Info(res)
}

func main() {
// 	listHelm(
// 		"galaxy",
// 		"your token token",
// 		"https://35.225.164.84",
// 	)
}
