module broadinstitute/helm-scala-sdk

go 1.16

replace (
	github.com/docker/distribution => github.com/docker/distribution v0.0.0-20191216044856-a8371794149d
	github.com/docker/docker => github.com/moby/moby v17.12.0-ce-rc1.0.20200618181300-9dc6525e6118+incompatible
)

require (
	github.com/golang/glog v0.0.0-20160126235308-23def4e6c14b
	helm.sh/helm/v3 v3.5.3
	k8s.io/cli-runtime v0.20.2
	k8s.io/client-go v0.20.2
)
