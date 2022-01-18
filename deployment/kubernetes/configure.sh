#!/bin/bash

if [ "$#" -lt 1 ]; then
    echo "Usage: configure.sh -i"
    echo "       configure.sh <SAS Viya cluster root url> <Egeria admin username> <Catalog username> <Catalog password>"
    exit 1
fi

if [ "$1" = "-i" ]; then
    echo "Enter SAS Viya cluster root url (e.g. https://myhost.com): "
    read CLUSTER_ROOT_URL
    echo "Enter Egeria admin username"
    read EGERIA_USER
    echo "Enter Catalog service username"
    read CATALOG_USER
    echo "Enter Catalog service username's password"
    read CATALOG_PASS
elif [ "$#" -lt 4 ]; then
    echo "Usage: configure.sh -i"
    echo "       configure.sh <SAS Viya cluster root url> <Egeria admin username> <Catalog username> <Catalog password>"
    exit 1
else
    # set variables from command line
    CLUSTER_ROOT_URL=$1
    EGERIA_USER=$2
    CATALOG_USER=$3
    CATALOG_PASS=$4
fi

# extract the protocol
CLUSTER_SCHEME="$(echo $CLUSTER_ROOT_URL | grep :// | sed -e's,^\(.*://\).*,\1,g')"

# remove the protocol
url=$(echo $CLUSTER_ROOT_URL | sed -e s,$CLUSTER_SCHEME,,g)

# remove "://" from protocol
CLUSTER_SCHEME=${CLUSTER_SCHEME%???}

# extract the user (if any)
user="$(echo $url | grep @ | cut -d@ -f1)"

# extract the host and port
CLUSTER_HOST=$(echo $url | sed -e s,$user@,,g | cut -d/ -f1)

# The name of the Egeria server (do not change this without also modifying the deployment.yaml to match)
EGERIA_SERVER="SASRepositoryProxy"

set -e

echo "Configure Catalog connection"

# Configure Catalog connection
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/local-repository/mode/repository-proxy/connection" \
--header 'Content-Type: application/json' \
--data-raw "{
  \"class\": \"Connection\",
  \"connectorType\": {
    \"class\": \"ConnectorType\",
    \"connectorProviderClassName\": \"org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnectorProvider\"
  },
  \"endpoint\": {
    \"class\": \"Endpoint\",
    \"address\": \"${CLUSTER_HOST}\",
    \"protocol\": \"${CLUSTER_SCHEME}\"
  },
  \"securedProperties\": {
    \"userId\": \"${CATALOG_USER}\",
    \"password\": \"${CATALOG_PASS}\"
  }
}"

echo "Configure cohort event bus"
# Configure cohort event bus
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/event-bus?connectorProvider=org.odpi.openmetadata.adapters.eventbus.topic.kafka.KafkaOpenMetadataTopicProvider&topicURLRoot=OMRSTopic" \
--header "Content-Type: application/json" \
--data-raw "{
  \"producer\": {
    \"bootstrap.servers\":\"kafkahost:9092\"
  },
  \"consumer\": {
    \"bootstrap.servers\":\"kafkahost:9092\"
  }
}"

echo "Configure event connector (Viya RabbitMQ)"
# Configure event connector (Viya RabbitMQ)
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/local-repository/event-mapper-details?connectorProvider=org.odpi.openmetadata.connector.sas.event.mapper.RepositoryEventMapperProvider&eventSource=" \
--header "Content-Type: application/json" \
--data-raw "{}"

echo "Start Egeria Server"
# Start Egeria Server
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/instance" \
--header "Content-Type: application/json"

exit 0
