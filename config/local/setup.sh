#!/bin/bash
#
#  Copyright 2024-2025 Sweden Connect
#
#  Licensed under the Apache License, Version 2.0 (the "License")
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
set -e

echo "Setting up local development environment..."

# Variables
CERT_NAME="snake-oil"
CERT_FILE="${CERT_NAME}.crt"
KEY_FILE="${CERT_NAME}.key"
P12_FILE="${CERT_NAME}.p12"
PUBLIC_PEM_FILE="${CERT_NAME}.pem"
PASSWORD="EBA4286F-4C20-4BAF-BE54-95FA31646DF5"

echo "Generate a private key..."
openssl genpkey -algorithm RSA -out $KEY_FILE -pkeyopt rsa_keygen_bits:2048

echo "Create a self-signed certificate..."
openssl req -new -x509 -key $KEY_FILE -out $CERT_FILE -days 365 -subj "/CN=${CERT_NAME}"

echo "Extract the public key to PEM format..."
openssl rsa -pubout -in $KEY_FILE -out $PUBLIC_PEM_FILE

echo "Created public PEM file: $PUBLIC_PEM_FILE"

echo "Create a .p12 keystore..."
openssl pkcs12 -export -out $P12_FILE -in $CERT_FILE -inkey $KEY_FILE -name "${CERT_NAME}" -passout pass:$PASSWORD

# Clean up
rm $CERT_FILE $KEY_FILE

echo "Created .p12 keystore: $P12_FILE"

echo "Starting Docker services..."
docker compose up -d

echo ""
echo "✓ Local environment setup complete!"
echo ""
echo "To start the registry application:"
echo "cd ../../ && mvn spring-boot:run -pl service \\"
echo "  -Dspring-boot.run.arguments=\"--spring.profiles.active=docker --spring.config.import=../config/local/application-docker.yml\""
