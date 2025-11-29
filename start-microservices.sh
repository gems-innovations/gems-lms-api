#!/bin/bash

MICROSERVICE=${1:-"all"}

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Starting microservices...${NC}"

load_env_file() {
    local file_path=$1
    
    if [ -f "$file_path" ]; then
        echo -e "${CYAN}Loading environment variables from .env file...${NC}"
        
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
        echo -e "${YELLOW}No .env file found, using default values...${NC}"
    fi
}

load_env_file ".env"

setup_java_home() {
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
        echo -e "${CYAN}Using JAVA_HOME: $JAVA_HOME${NC}"
        return 0
    fi
    
    if command -v java >/dev/null 2>&1; then
        local java_path=$(readlink -f $(which java))
        if [ -n "$java_path" ]; then
            local java_home=$(dirname $(dirname "$java_path"))
            if [ -f "$java_home/bin/java" ]; then
                export JAVA_HOME="$java_home"
                echo -e "${CYAN}Auto-detected JAVA_HOME: $JAVA_HOME${NC}"
                return 0
            fi
        fi
    fi
    
    echo -e "${RED}ERROR: Java is not installed or JAVA_HOME is not set.${NC}"
    echo -e "${YELLOW}This project requires Java 24.${NC}"
    echo -e "${CYAN}To install Java 24 on Ubuntu, run:${NC}"
    echo -e "${CYAN}  sudo apt update${NC}"
    echo -e "${CYAN}  sudo apt install openjdk-24-jdk${NC}"
    echo -e "${CYAN}Or install Eclipse Temurin 24:${NC}"
    echo -e "${CYAN}  wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -${NC}"
    echo -e "${CYAN}  echo 'deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print\$2}' /etc/os-release) main' | sudo tee /etc/apt/sources.list.d/adoptium.list${NC}"
    echo -e "${CYAN}  sudo apt update && sudo apt install temurin-24-jdk${NC}"
    echo -e "${YELLOW}After installation, set JAVA_HOME in your .env file:${NC}"
    echo -e "${YELLOW}  JAVA_HOME=/usr/lib/jvm/java-24-openjdk-amd64${NC}"
    exit 1
}

setup_java_home

EDUCATION_PORT=${EDUCATION_PORT:-""}
AUTH_PORT=${AUTH_PORT:-""}
ADMIN_PORT=${ADMIN_PORT:-""}

start_microservice() {
    local name=$1
    local port=$2
    local gradle_task=$3
    local background=${4:-true}
    
    echo -e "${YELLOW}Starting $name on port: $port${NC}"
    
    export "${name}_PORT=$port"
    export JAVA_HOME
    
    if [ "$background" = "true" ]; then
        nohup env JAVA_HOME="$JAVA_HOME" ./gradlew $gradle_task > "logs/${name,,}.log" 2>&1 &
        echo "Started $name (PID: $!)"
    else
        echo -e "${CYAN}Running in foreground. Press Ctrl+C to stop.${NC}"
        env JAVA_HOME="$JAVA_HOME" ./gradlew $gradle_task
    fi
}

case "${MICROSERVICE,,}" in
    "all")
        echo -e "${GREEN}Starting all microservices...${NC}"
        mkdir -p logs
        start_microservice "EDUCATION" "$EDUCATION_PORT" ":ms-education:bootRun"
        sleep 2
        start_microservice "AUTH" "$AUTH_PORT" ":ms-auth:bootRun"
        sleep 2
        start_microservice "ADMIN" "$ADMIN_PORT" ":ms-admin:bootRun"
        echo -e "${GREEN}All microservices are starting in background...${NC}"
        echo -e "${CYAN}Check logs/ directory for output${NC}"
        ;;
    "ms-auth")
        echo -e "${GREEN}Starting Auth microservice...${NC}"
        mkdir -p logs
        start_microservice "AUTH" "$AUTH_PORT" ":ms-auth:bootRun" false
        ;;
    "ms-education")
        echo -e "${GREEN}Starting Education microservice...${NC}"
        mkdir -p logs
        start_microservice "EDUCATION" "$EDUCATION_PORT" ":ms-education:bootRun" false
        ;;
    "ms-admin")
        echo -e "${GREEN}Starting Admin microservice...${NC}"
        mkdir -p logs
        start_microservice "ADMIN" "$ADMIN_PORT" ":ms-admin:bootRun" false
        ;;
    *)
        echo -e "${RED}Unknown microservice: $MICROSERVICE${NC}"
        echo -e "${YELLOW}Available options: all, ms-auth, ms-education, ms-admin${NC}"
        echo -e "${CYAN}Note: When running individual microservices, logs will be shown in console and Ctrl+C will stop the service.${NC}"
        exit 1
        ;;
esac

