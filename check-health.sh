#!/bin/bash
for p in 8080 8081 8082 8083; do
  echo "port $p:"
  curl -s -m 3 http://localhost:$p/actuator/health
  echo
done
