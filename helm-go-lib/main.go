package main

import "C"

import (
	"fmt"
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
func install(namespace, kubeToken, apiServer, releaseName, chartName, filePath string) *C.char {
	settings := cli.New()
	fmt.Printf("Top of install()\n")

	actionConfig := new(action.Configuration)
	// You can pass an empty string instead of settings.Namespace() to list all namespaces
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
	//fmt.Printf("chartRequested is ", *chartRequested)
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
	finalOpts := map[string]interface{}{}
	fmt.Printf("Before filePath if\n")
	if filePath != "" {
		fmt.Printf("if filePath != empty \n")
		bytes, err := readFile(filePath, p)
		if err != nil {
			return C.CString(err.Error())
		}

		if err := yaml.Unmarshal(bytes, &currentMap); err != nil {
			return C.CString("failed to parse values")
		}
		// Merge with the previous map
		finalOpts = mergeMaps(base, currentMap)
	} else {
		fmt.Printf("if filePath is empty \n")
		finalOpts = base
	}
	
	//fmt.Printf("finalOpts are ", &finalOpts)
	rls, err := client.Run(chartRequested, finalOpts)
	fmt.Printf("%+v", "err is ", err)
	fmt.Printf("%+v", "rls is ", rls)
	if err != nil {
		return C.CString(err.Error())
	}

	glog.Info("Finished installing %s", releaseName)
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

func main() {
	install(
		"test-helm-client-0716-1",
		"eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tOHhqd2MiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjllNzc5YzI3LWFiNjAtMTFlYS1iMjY5LTQyMDEwYTgwMDBlZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.atM5y4Qd5sgTDQ-JpWCFBdVE0jZYUz0kdDDsqqnK__H1MnWsti0jVy5JNjurFH5dNbjYkDW0uW37agMCM-1hWGAFBKYYL4RQZdNNwfxGw_VXtDmWEC896OphTHDKOAbC9h-C6RfzwJC1--D3nGZfdgrqMeE6U4Fi0LXc0PIRBUM9BdgHY5Dr0s2bKsjTfUe0huru2YRNM7NZtbIPSYd2J680Mcn0Z7OpshpY0JnOkmMjGsdqw6fLLMhzGf9OHZN5LBal8aTUHRSVIgRrpXejNzjP91QCbfGMe9v9FnZNwzlltFSMsE3J-aQtw282f5o8H_djWrwv0-p-OkcX3kNQJw",
		"https://34.66.249.164",
		//"nginx-api-rls-0721-1",
		"bitnami-nginx-api-rls-0721-2",
		//"nginx-stable/nginx-ingress",
		"bitnami/nginx",
		//"/Users/kyuksel/gke_experiment/kubernetes-ingress/deployments/helm-chart/values.yaml",
		"",
		)
}
