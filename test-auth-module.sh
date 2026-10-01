#!/bin/bash
BASE="http://localhost:8081/api/v1"
echo "=== 1) Register ==="
REG=$(curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Carlos","lastName":"Perez","email":"carlos.perez@example.com","role":"ADMIN","institutionId":"inst-1"}')
echo "$REG"
USER_ID=$(echo "$REG" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')
TEMP_PASS=$(echo "$REG" | sed -n 's/.*"temporaryPassword":"\([^"]*\)".*/\1/p')
echo "userId=$USER_ID tempPass=$TEMP_PASS"

echo "=== 2) Login ==="
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"carlos.perez@example.com\",\"password\":\"$TEMP_PASS\"}")
echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

echo "=== 3) GET users by institution WITH token (should be 200) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/users/institution/inst-1 -H "Authorization: Bearer $TOKEN"

echo "=== 4) GET users by institution WITHOUT token (should be 401) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/users/institution/inst-1

echo "=== 5) Toggle status WITH token (should be 200) ==="
curl -s -w '\nHTTP:%{http_code}\n' -X PATCH $BASE/users/$USER_ID/status -H "Authorization: Bearer $TOKEN"

echo "=== 6) Delete/disable user WITH token (should be 204) ==="
curl -s -w '\nHTTP:%{http_code}\n' -X DELETE $BASE/users/$USER_ID -H "Authorization: Bearer $TOKEN"

echo "=== 7) Swagger still public (should be 200/302) ==="
curl -s -o /dev/null -w 'HTTP:%{http_code}\n' $BASE/../swagger-ui.html
