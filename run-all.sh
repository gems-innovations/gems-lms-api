#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

set -a
source .env
set +a

mkdir -p logs

setsid nohup java -jar api-gateway/build/libs/api-gateway-1.0.0.jar > logs/api-gateway.log 2>&1 < /dev/null &
disown
echo "API Gateway PID: $!"

setsid nohup java -jar ms-education/build/libs/ms-education-1.0.0.jar > logs/ms-education.log 2>&1 < /dev/null &
disown
echo "Education PID: $!"

setsid nohup java -jar ms-auth/build/libs/ms-auth-1.0.0.jar > logs/ms-auth.log 2>&1 < /dev/null &
disown
echo "Auth PID: $!"

setsid nohup java -jar ms-admin/build/libs/ms-admin-1.0.0.jar > logs/ms-admin.log 2>&1 < /dev/null &
disown
echo "Admin PID: $!"
