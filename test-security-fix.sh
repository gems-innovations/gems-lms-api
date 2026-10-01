#!/bin/bash
echo "=== Login via gateway (8080) ==="
LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"carlos.perez@example.com","password":"WRONG"}')
echo "$LOGIN"

echo "=== Register a fresh admin for this test ==="
REG=$(curl -s -X POST http://localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Sec","lastName":"Tester","email":"sec.tester@example.com","role":"ADMIN","institutionId":"inst-1"}')
echo "$REG"
TEMP_PASS=$(echo "$REG" | sed -n 's/.*"temporaryPassword":"\([^"]*\)".*/\1/p')

echo "=== Login via gateway ==="
LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"sec.tester@example.com\",\"password\":\"$TEMP_PASS\"}")
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "token acquired: ${TOKEN:0:20}..."

echo "=== ms-admin protected route WITHOUT token (expect 401) ==="
curl -s -o /dev/null -w 'HTTP:%{http_code}\n' http://localhost:8082/api/v1/institutions

echo "=== ms-admin protected route WITH token (expect 200, empty list ok) ==="
curl -s -w '\nHTTP:%{http_code}\n' http://localhost:8082/api/v1/institutions -H "Authorization: Bearer $TOKEN"

echo "=== ms-education protected route WITHOUT token (expect 401) ==="
curl -s -o /dev/null -w 'HTTP:%{http_code}\n' http://localhost:8083/api/v1/courses

echo "=== ms-education protected route WITH token (expect 200) ==="
curl -s -w '\nHTTP:%{http_code}\n' http://localhost:8083/api/v1/courses -H "Authorization: Bearer $TOKEN"

echo "=== api-gateway routing: users by institution via gateway WITH token (expect 200) ==="
curl -s -w '\nHTTP:%{http_code}\n' http://localhost:8080/api/v1/users/institution/inst-1 -H "Authorization: Bearer $TOKEN"

echo "=== Swagger still public on ms-admin (expect 302) ==="
curl -s -o /dev/null -w 'HTTP:%{http_code}\n' http://localhost:8082/swagger-ui.html
