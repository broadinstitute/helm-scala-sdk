Depends on [helm](https://pkg.go.dev/helm.sh/helm/v3) and [k8s.io](https://pkg.go.dev/k8s.io/cli-runtime) Go packages.

Update `go.mod` to bump dependencies
Run `go mod tidy` to resolve dependencies
Run `go build -o libhelm.dylib -buildmode=c-shared main.go` to build on mac
Run `go build -o libhelm.so -buildmode=c-shared main.go` to build on linux


