# helm-scala-sdk

Package we're trying to export https://github.com/helm/helm/tree/master/pkg/action

Borrowed approach from https://medium.com/learning-the-go-programming-language/calling-go-functions-from-other-languages-4c7d8bcc69bf

helm go sdk https://pkg.go.dev/helm.sh/helm/v3@v3.1.2

```
cd helm-go-lib
go build -o libhelm.dylib -buildmode=c-shared main.go
```

# Usage
- Add the library to project dependency
```
libraryDependencies += "org.broadinstitute.dsp" % "helm-scala-sdk_2.13" % "0.0.1"
```

- Set JVM parameter `-Djna.library.path=<path-to-generated-shared-go-library>`
- Note that since this is a helm sdk, you must update the local helm repo with the most recent charts for your app. For galaxy and cromwell the commands are as follows
```
helm repo add stable https://kubernetes-charts.storage.googleapis.com/ 
helm repo add galaxy https://raw.githubusercontent.com/cloudve/helm-charts/anvil/ 
helm repo add cromwell-helm https://broadinstitute.github.io/cromwhelm/charts/
helm repo update
```

# Publishing
Publish locally

`sbt publishLocal` (`sbt ++publishLocal` to cross build multiple versions of scala)

Publish to JFrog
Before running the publishing command, make sure to bump the package version in built.sbt.
Make sure to compile/publish with Java 17, this is what leonardo expects!
You can then find the artifactory credentials on vault (`secret/dsp/accts/artifactory/dsdejenkins`).
Export both ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD to your environment.

`sbt publish` (`sbt ++publish` to cross build multiple versions of scala)

