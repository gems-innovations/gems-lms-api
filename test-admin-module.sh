#!/bin/bash
BASE="http://localhost:8080/api/v1"

echo "=== 0) Login as existing admin (sec.tester) ==="
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"sec.tester@example.com","password":"Aa1!F7!f$Z2p"}')
echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then echo "LOGIN FAILED, aborting"; exit 1; fi

echo "=== 1) Create institution (no id, with branding) ==="
CREATE=$(curl -s -X POST $BASE/institutions -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "name": "Universidad de Prueba",
  "type": "university",
  "metadata": { "description": "Test institution", "website": "https://test.edu", "contactEmail": "info@test.edu", "maxUsers": 500 },
  "branding": { "type": "color-badge", "colorPrimary": "#123456", "colorSecondary": "#654321", "darkMode": true }
}')
echo "$CREATE"
INST_ID=$(echo "$CREATE" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo "institutionId=$INST_ID"

echo "=== 2) Get by id (verify branding + status defaulted to pending) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/institutions/$INST_ID -H "Authorization: Bearer $TOKEN"

echo "=== 3) List with pagination (page=1, limit=5) ==="
curl -s -w '\nHTTP:%{http_code}\n' "$BASE/institutions?page=1&limit=5" -H "Authorization: Bearer $TOKEN"

echo "=== 4) List with search filter ==="
curl -s -w '\nHTTP:%{http_code}\n' "$BASE/institutions?search=prueba" -H "Authorization: Bearer $TOKEN"

echo "=== 5) Update institution (partial: only status + one branding field) ==="
curl -s -w '\nHTTP:%{http_code}\n' -X PUT $BASE/institutions/$INST_ID -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "name": "Universidad de Prueba",
  "type": "university",
  "status": "active",
  "branding": { "colorPrimary": "#ABCDEF" }
}'

echo "=== 6) Get by id again (verify status=active, colorPrimary changed, colorSecondary preserved) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/institutions/$INST_ID -H "Authorization: Bearer $TOKEN"

echo "=== 7) List without token (expect 401) ==="
curl -s -o /dev/null -w 'HTTP:%{http_code}\n' $BASE/institutions

echo "=== 8) Delete institution ==="
curl -s -w '\nHTTP:%{http_code}\n' -X DELETE $BASE/institutions/$INST_ID -H "Authorization: Bearer $TOKEN"

echo "=== 9) Get by id after delete (expect 404) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/institutions/$INST_ID -H "Authorization: Bearer $TOKEN"
