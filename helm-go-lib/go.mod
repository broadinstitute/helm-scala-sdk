module broadinstitute/helm-scala-sdk

go 1.16

replace (
	github.com/docker/distribution => github.com/docker/distribution v0.0.0-20191216044856-a8371794149d
	github.com/docker/docker => github.com/moby/moby v17.12.0-ce-rc1.0.20200618181300-9dc6525e6118+incompatible
)

require (
	github.com/frankban/quicktest v1.14.5 // indirect
	github.com/golang/glog v1.0.0
	github.com/hashicorp/golang-lru v1.0.2 // indirect
	github.com/mattn/go-isatty v0.0.19 // indirect
	github.com/mattn/go-runewidth v0.0.15 // indirect
	github.com/rivo/uniseg v0.4.4 // indirect
	github.com/sirupsen/logrus v1.9.3 // indirect
	github.com/spf13/cobra v1.7.0 // indirect
	go.starlark.net v0.0.0-20230925163745-10651d5192ab // indirect
	golang.org/x/net v0.17.0 // indirect
	golang.org/x/sync v0.3.0 // indirect
	helm.sh/helm/v3 v3.11.2
	k8s.io/cli-runtime v0.26.3
	k8s.io/client-go v0.26.3
)
