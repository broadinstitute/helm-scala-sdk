# This container is used in the firecloud-develop docker-rsync-local-leonardo
# The use case is to build a platform-independent go executable of this library for mounting into a docker container
# This is useful, as leonardo depends on it, but a mac-built version is not compatible

FROM golang:1.23 AS helm-go-lib-builder

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
