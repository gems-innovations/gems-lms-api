#!/bin/bash
LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"email":"sec.tester@example.com","password":"Aa1!F7!f$Z2p"}')
echo "LOGIN: $LOGIN"
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "TOKEN: ${TOKEN:0:20}..."
curl -s -X POST http://localhost:8080/api/v1/institutions -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "id": "inst-1",
  "name": "Universidad Nacional de Colombia",
  "type": "university",
  "status": "active",
  "metadata": {"description":"Principal universidad publica","website":"https://unal.edu.co","contactEmail":"info@unal.edu.co","maxUsers":50000,"subscriptionType":"enterprise"},
  "branding": {"type":"color-badge","colorPrimary":"#6C63FF","colorSecondary":"#1E1B4B","darkMode":true}
}'
