#!/bin/bash

MICROSERVICE=${1:-"all"}

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${RED}Stopping microservices...${NC}"

load_env_file() {
    local file_path=$1
    
    if [ -f "$file_path" ]; then
        echo -e "${CYAN}Loading ports from .env file...${NC}"
        
        while IFS= read -r line || [ -n "$line" ]; do
            line=$(echo "$line" | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            
            if [[ -z "$line" ]] || [[ "$line" =~ ^# ]]; then
                continue
            fi
            
            if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
                local key="${BASH_REMATCH[1]}"
                local value="${BASH_REMATCH[2]}"
                key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                
                if [[ "$value" =~ ^\".*\"$ ]] || [[ "$value" =~ ^\'.*\'$ ]]; then
                    value="${value:1:-1}"
                fi
                
                export "$key=$value"
            fi
        done < "$file_path"
    else
        echo -e "${RED}No .env file found. Stopping script.${NC}"
        exit 1
    fi
}

load_env_file ".env"

API_GATEWAY_PORT=${API_GATEWAY_PORT:-8080}
EDUCATION_PORT=${EDUCATION_PORT:-""}
AUTH_PORT=${AUTH_PORT:-""}
ADMIN_PORT=${ADMIN_PORT:-""}

# lsof/pkill are not available in Git Bash on Windows: fall back to netstat/taskkill there.
pids_on_port() {
    if command -v lsof >/dev/null 2>&1; then
        lsof -ti:$1 2>/dev/null
    else
        netstat -ano 2>/dev/null | awk -v p=":$1" '$2 ~ p"$" && $4 == "LISTENING" { print $5 }' | sort -u
    fi
}

kill_pid() {
    if command -v taskkill >/dev/null 2>&1; then
        taskkill //PID "$1" //F >/dev/null 2>&1
    else
        kill -9 "$1" 2>/dev/null
    fi
}

stop_microservice_on_port() {
    local port=$1
    local service_name=$2
    
    if [ -z "$port" ]; then
        echo -e "${YELLOW}No port configured for $service_name${NC}"
        return
    fi
    
    local pids=$(pids_on_port $port)
    
    if [ -z "$pids" ]; then
        echo -e "${YELLOW}No $service_name process found on port $port${NC}"
        return
    fi
    
    for pid in $pids; do
        if [ -n "$pid" ]; then
            echo -e "${RED}Killing $service_name on port $port (PID: $pid)${NC}"
            kill_pid $pid
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}$service_name stopped successfully${NC}"
            else
                echo -e "${RED}Error stopping $service_name on port $port${NC}"
            fi
        fi
    done
}

case "${MICROSERVICE,,}" in
    "all")
        echo -e "${RED}Stopping all microservices...${NC}"
        stop_microservice_on_port "$API_GATEWAY_PORT" "API Gateway"
        stop_microservice_on_port "$EDUCATION_PORT" "Education Service"
        stop_microservice_on_port "$AUTH_PORT" "Auth Service"
        stop_microservice_on_port "$ADMIN_PORT" "Admin Service"
        
        echo -e "\n${CYAN}Killing all remaining Java processes...${NC}"
        if command -v pkill >/dev/null 2>&1 && pkill -9 java 2>/dev/null; then
            echo -e "${GREEN}All Java processes killed${NC}"
        else
            echo -e "${YELLOW}No Java processes found${NC}"
        fi
        ;;
    "ms-auth")
        echo -e "${RED}Stopping Auth microservice...${NC}"
        stop_microservice_on_port "$AUTH_PORT" "Auth Service"
        ;;
    "ms-education")
        echo -e "${RED}Stopping Education microservice...${NC}"
        stop_microservice_on_port "$EDUCATION_PORT" "Education Service"
        ;;
    "ms-admin")
        echo -e "${RED}Stopping Admin microservice...${NC}"
        stop_microservice_on_port "$ADMIN_PORT" "Admin Service"
        ;;
    "api-gateway")
        echo -e "${RED}Stopping API Gateway...${NC}"
        stop_microservice_on_port "$API_GATEWAY_PORT" "API Gateway"
        ;;
    *)
        echo -e "${RED}Unknown microservice: $MICROSERVICE${NC}"
        echo -e "${YELLOW}Available options: all, ms-auth, ms-education, ms-admin, api-gateway${NC}"
        exit 1
        ;;
esac

echo -e "\n${GREEN}Done!${NC}"

