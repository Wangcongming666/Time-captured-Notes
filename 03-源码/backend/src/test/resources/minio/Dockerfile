# Test fixture built from a pinned official source release; no third-party server image.
FROM golang:1.24.8 AS build
ARG HTTP_PROXY
ARG HTTPS_PROXY
RUN CGO_ENABLED=0 GOBIN=/out go install github.com/minio/minio@RELEASE.2025-10-15T17-29-55Z
FROM debian:bookworm-slim
COPY --from=build /out/minio /usr/local/bin/minio
EXPOSE 9000
ENTRYPOINT ["minio"]
