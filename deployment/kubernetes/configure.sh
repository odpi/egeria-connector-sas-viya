#!/usr/bin/env sh

if [ "$#" -lt 1 ]; then
    echo "Usage: configure.sh -i"
    echo "       configure.sh <SAS Viya cluster root url> <Egeria admin username> <Catalog username> <Catalog password>"
    echo "                    [<Kafka hostname> <Kafka port> <cohort name>]"
    exit 1
fi

export KAFKA_HOST

if [ "$1" = "-i" ]; then
    echo "Enter SAS Viya cluster root url (e.g. https://myhost.com): "
    read CLUSTER_ROOT_URL
    echo ""
    echo "Enter Egeria admin username:"
    read EGERIA_USER
    echo ""
    echo "Enter Catalog service username:"
    read CATALOG_USER
    echo ""
    echo "Enter Catalog service password:"
    read CATALOG_PASS
    echo ""
    echo "Enter Kafka hostname [kafkahost]:"
    read KAFKA_HOST
    echo ""
    echo "Enter Kafka port [9092]:"
    read KAFKA_PORT
    echo ""
    echo "Enter cohort name []:"
    read COHORT_NAME
elif [ "$#" -lt 4 ]; then
    echo "Usage: configure.sh -i"
    echo "       configure.sh <SAS Viya cluster root url> <Egeria admin username> <Catalog username> <Catalog password>"
    echo "                    [<Kafka hostname> <Kafka port> <cohort name>]"
    exit 1
else
    # set variables from command line
    CLUSTER_ROOT_URL=$1
    EGERIA_USER=$2
    CATALOG_USER=$3
    CATALOG_PASS=$4
    KAFKA_HOST=$5
    KAFKA_PORT=$6
    COHORT_NAME=$7
fi

# default KAFKA_HOST if necessary
if [ "${KAFKA_HOST}" = "" ]; then
    KAFKA_HOST="kafkahost"
fi

# default KAFKA_PORT if necessary
if [ "${KAFKA_PORT}" = "" ]; then
    KAFKA_PORT="9092"
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


# Configure Catalog connection
echo "Configuring Catalog connection for ${CLUSTER_SCHEME}://${CLUSTER_HOST} ..."
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

# Configure cohort event bus
echo "Configuring cohort event bus to ${KAFKA_HOST}:${KAFKA_PORT} ..."
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/event-bus?connectorProvider=org.odpi.openmetadata.adapters.eventbus.topic.kafka.KafkaOpenMetadataTopicProvider&topicURLRoot=OMRSTopic" \
--header "Content-Type: application/json" \
--data-raw "{
  \"producer\": {
    \"bootstrap.servers\":\"${KAFKA_HOST}:${KAFKA_PORT}\"
  },
  \"consumer\": {
    \"bootstrap.servers\":\"${KAFKA_HOST}:${KAFKA_PORT}\"
  }
}"

# Configure event connector (Viya RabbitMQ)
echo "Configuring event connector (Viya RabbitMQ) ..."
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/local-repository/event-mapper-details?connectorProvider=org.odpi.openmetadata.connector.sas.event.mapper.RepositoryEventMapperProvider&eventSource=" \
--header "Content-Type: application/json" \
--data-raw "{}"

# Join Cohort
if [ "${COHORT_NAME}" != "" ]; then
    echo "Joining Cohort: ${COHORT_NAME} ..."
    curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/cohorts/${COHORT_NAME}"
fi


# Start Egeria Server
echo "Starting Egeria Server ..."
curl --location --request POST -k "${CLUSTER_SCHEME}://${CLUSTER_HOST}/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/instance" \
--header "Content-Type: application/json"


echo "Done."
exit 0
