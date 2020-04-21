# helm-java-sdk

Package we're trying to export https://github.com/helm/helm/tree/master/pkg/action

Borrowed approach from https://medium.com/learning-the-go-programming-language/calling-go-functions-from-other-languages-4c7d8bcc69bf

helm go sdk https://pkg.go.dev/helm.sh/helm/v3@v3.1.2

```
cd helm-go-lib
go build -o libhelm.dylib -buildmode=c-shared main.go
```

```bash
 nm  --extern-only /Users/qi/workspace/helm-java-sdk/helm-go-lib/libhelm.dylib|grep Add                                                                   [20/04/15|12:08PM]
00000000010bc250 T _Add
00000000010bbea0 T __cgoexp_44472ad02a5b_Add
```

Set JVM parameter `-Djna.library.path=/Users/qi/workspace/helm-java-sdk/helm-go-lib`