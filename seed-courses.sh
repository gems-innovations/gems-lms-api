#!/bin/bash
BASE="http://localhost:8080/api/v1"
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"email":"sec.tester@example.com","password":"Aa1!F7!f$Z2p"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

curl -s -X POST $BASE/courses -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "title": "Introduccion a Angular",
  "description": "Aprende los fundamentos de Angular: componentes, servicios y routing.",
  "difficulty": "beginner",
  "tags": ["Angular", "TypeScript"],
  "instructorName": "Valentina Torres",
  "institutionId": "inst-1",
  "status": "published",
  "modules": [{"title":"Fundamentos","orderIndex":1,"lessons":[{"title":"Componentes","orderIndex":1,"contents":[]},{"title":"Servicios","orderIndex":2,"contents":[]}]}]
}' > /dev/null

curl -s -X POST $BASE/courses -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "title": "Spring Boot Avanzado",
  "description": "R2DBC, WebFlux y arquitectura reactiva con Spring Boot.",
  "difficulty": "advanced",
  "tags": ["Java", "Spring Boot", "Reactive"],
  "instructorName": "Dr. Andres Mora",
  "institutionId": "inst-1",
  "status": "published",
  "modules": [{"title":"Programacion Reactiva","orderIndex":1,"lessons":[{"title":"Mono y Flux","orderIndex":1,"contents":[]}]}]
}' > /dev/null

echo "Seeded. Listing:"
curl -s "$BASE/courses?page=1&limit=10" -H "Authorization: Bearer $TOKEN"
