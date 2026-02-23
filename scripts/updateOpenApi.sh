#!/bin/bash
#
# Copyright 2026 Sweden Connect
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
#  limitations under the License.
#


# Define the URL and output file
URL="http://localhost:8010/v3/api-docs"
OUTPUT_FILE="../service/api/openapi.json"

# Use curl to fetch the URL and save it to the file
curl -s "$URL" -o "$OUTPUT_FILE"

# Check if the fetch was successful
if [ $? -eq 0 ]; then
  echo "OpenAPI definition successfully saved to $OUTPUT_FILE"
else
  echo "Failed to retrieve OpenAPI definition."
fi