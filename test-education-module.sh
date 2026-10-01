#!/bin/bash
BASE="http://localhost:8080/api/v1"

echo "=== 0) Login as admin ==="
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"sec.tester@example.com","password":"Aa1!F7!f$Z2p"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
USER_ID=$(echo "$LOGIN" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')
echo "userId=$USER_ID"
if [ -z "$TOKEN" ]; then echo "LOGIN FAILED, aborting"; exit 1; fi

echo "=== 1) Create course with modules/lessons ==="
CREATE=$(curl -s -X POST $BASE/courses -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "title": "Curso de Prueba",
  "description": "Un curso de prueba end to end",
  "difficulty": "intermediate",
  "tags": ["Test", "E2E"],
  "thumbnailUrl": "https://example.com/thumb.png",
  "instructorName": "Prof. Prueba",
  "institutionId": "inst-1",
  "modules": [
    { "title": "Modulo 1", "orderIndex": 1, "lessons": [
      { "title": "Leccion 1", "orderIndex": 1, "contents": [
        { "type": "VIDEO", "value": "https://youtube.com/embed/xyz", "orderIndex": 1 }
      ]},
      { "title": "Leccion 2", "orderIndex": 2, "contents": [] }
    ]}
  ]
}')
echo "$CREATE"
COURSE_ID=$(echo "$CREATE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "courseId=$COURSE_ID"

echo "=== 2) Get by id (verify totalLessons=2, tags, difficulty) ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/courses/$COURSE_ID -H "Authorization: Bearer $TOKEN"

echo "=== 3) List with pagination + filter difficulty=intermediate ==="
curl -s -w '\nHTTP:%{http_code}\n' "$BASE/courses?page=1&limit=5&difficulty=intermediate" -H "Authorization: Bearer $TOKEN"

echo "=== 4) List with search ==="
curl -s -w '\nHTTP:%{http_code}\n' "$BASE/courses?search=prueba" -H "Authorization: Bearer $TOKEN"

echo "=== 5) Update course: publish it ==="
curl -s -w '\nHTTP:%{http_code}\n' -X PUT $BASE/courses/$COURSE_ID -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "title": "Curso de Prueba",
  "institutionId": "inst-1",
  "status": "published"
}'

echo "=== 6) Enroll the logged-in user (no local Student row needed) ==="
ENROLL=$(curl -s -w '\nHTTP:%{http_code}\n' -X POST $BASE/enrollments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "{\"studentId\": $USER_ID, \"courseId\": $COURSE_ID}")
echo "$ENROLL"

echo "=== 7) Get course again (verify enrolledCount=1) ==="
curl -s $BASE/courses/$COURSE_ID -H "Authorization: Bearer $TOKEN"
echo

echo "=== 8) Get student enrollments ==="
curl -s -w '\nHTTP:%{http_code}\n' $BASE/enrollments/student/$USER_ID -H "Authorization: Bearer $TOKEN"

echo "=== 9) Update enrollment progress to 100 ==="
ENROLLMENT_ID=$(echo "$ENROLL" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
curl -s -w '\nHTTP:%{http_code}\n' -X PUT "$BASE/enrollments/$ENROLLMENT_ID/progress?progress=100" -H "Authorization: Bearer $TOKEN"
