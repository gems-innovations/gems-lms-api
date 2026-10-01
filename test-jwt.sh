#!/bin/bash
RESP=$(curl -s -m 5 -X POST http://localhost:8081/api/v1/auth/login -H 'Content-Type: application/json' -d '{"email":"test3@example.com","password":"Password123!"}')
echo "LOGIN RESPONSE: $RESP"
TOKEN=$(echo "$RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "TOKEN: $TOKEN"
echo "--- GET /api/v1/users WITH token ---"
curl -s -m 5 -w '\nHTTP:%{http_code}\n' http://localhost:8081/api/v1/users -H "Authorization: Bearer $TOKEN"
echo "--- GET /api/v1/users WITHOUT token (should be 401) ---"
curl -s -m 5 -w '\nHTTP:%{http_code}\n' http://localhost:8081/api/v1/users
