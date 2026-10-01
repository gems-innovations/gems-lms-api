#!/bin/bash
BASE="http://localhost:8080/api/v1"
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"email":"sec.tester@example.com","password":"Aa1!F7!f$Z2p"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
USER_ID=$(echo "$LOGIN" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')

echo "=== Publish course 1 ==="
curl -s -w '\nHTTP:%{http_code}\n' -X PUT $BASE/courses/1 -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"title":"Curso de Prueba","institutionId":"inst-1","status":"published"}'

echo "=== Enroll user $USER_ID in course 1 ==="
ENROLL=$(curl -s -X POST $BASE/enrollments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "{\"studentId\": $USER_ID, \"courseId\": 1}")
echo "$ENROLL"
ENROLLMENT_ID=$(echo "$ENROLL" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "enrollmentId=$ENROLLMENT_ID"

echo "=== Course after enroll ==="
curl -s $BASE/courses/1 -H "Authorization: Bearer $TOKEN"
echo

echo "=== Student enrollments ==="
curl -s $BASE/enrollments/student/$USER_ID -H "Authorization: Bearer $TOKEN"
echo

echo "=== Update progress to 100 ==="
curl -s -w '\nHTTP:%{http_code}\n' -X PUT "$BASE/enrollments/$ENROLLMENT_ID/progress?progress=100" -H "Authorization: Bearer $TOKEN"
