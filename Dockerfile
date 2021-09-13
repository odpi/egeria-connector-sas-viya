# SPDX-License-Identifier: Apache-2.0
# Copyright Contributors to the Egeria project

# Thes are optional tags used to add additional metadata into the docker image
# These may be supplied by the pipeline in future - until then they will default

ARG version=latest
ARG EGERIA_BASE_IMAGE=docker.io/odpi/egeria
# DEFER setting this for now, using the ${version}:
# ARG EGERIA_IMAGE_DEFAULT_TAG=latest

# This Dockerfile should be run from the parent directory of the egeria-connector-sas-viya directory
# ie
# docker -f ./Dockerfile 

FROM ${EGERIA_BASE_IMAGE}:${version}


ENV version ${version}


# Labels from https://github.com/opencontainers/image-spec/blob/master/annotations.md#pre-defined-annotation-keys (with additions prefixed    ext)
# We should inherit all the base labels from the egeria image and only overwrite what is necessary.
LABEL org.opencontainers.image.description = "Egeria with SAS Viya connector" \
      org.opencontainers.image.documentation = "https://github.com/odpi/egeria-connector-sas-viya"

WORKDIR .
COPY build/libs/egeria-connector-viya-4-${version}*.jar /deployments/server/lib

# Mount security/trustedcerts.jks at runtime
ENV JAVA_OPTS_APPEND -XX:MaxMetaspaceSize=1g -Djavax.net.ssl.trustStore=/security/trustedcerts.jks -Dsas.egeria.repositoryconnector.ssl.trustAll=false
