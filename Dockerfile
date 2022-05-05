FROM golang:1.14.6-stretch AS helm-go-lib-builder

# Build helm lib
RUN mkdir /helm-go-lib-build && \
    cd /helm-go-lib-build && \
    git clone https://github.com/broadinstitute/helm-scala-sdk.git && \
    cd helm-scala-sdk && \
    git checkout master && \
    cd helm-go-lib && \
    go build -o libhelm.so -buildmode=c-shared main.go

# Move helm lib to appropriate dir
RUN mkdir /build && \
    cp /helm-go-lib-build/helm-scala-sdk/helm-go-lib/* /build

CMD ["/bin/sh"]
