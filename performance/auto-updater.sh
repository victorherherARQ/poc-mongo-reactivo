#!/bin/bash

# Configuration
DB_NAME="poc_reactive"
COLLECTION="pendingRequests"
MONGO_URI="mongodb://localhost:27017/$DB_NAME?replicaSet=rs0"

echo "Starting Auto-Updater for Performance Tests..."
echo "Monitoring $COLLECTION for PENDING documents..."

while true; do
  # Find all PENDING documents and update them to COMPLETED
  # We use --quiet to reduce logs and --eval to run the command
  docker exec -i $(docker ps -qf "name=poc-mongo") mongosh --quiet "$MONGO_URI" --eval "
    db.$COLLECTION.updateMany(
      { status: 'PENDING' },
      { \$set: { status: 'COMPLETED', updatedAt: new Date() } }
    )
  "

  # Sleep a bit to avoid CPU saturation, but fast enough for the test
  sleep 0.5
done
