#!/usr/bin/env bash

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_PID_FILE="${APP_DIR}/ms079.pid"
SERVER_LOG_DIR="${APP_DIR}/logs"
SERVER_MAIN_CLASS="com.github.mrzhqiang.maplestory.MapleStoryApplication"
SERVER_JAVA_OPTS="${SERVER_JAVA_OPTS:--server -Dfile.encoding=UTF-8 -Dwz.path=wz}"

mkdir -p "${SERVER_LOG_DIR}"

if [[ -f "${SERVER_PID_FILE}" ]]; then
    server_pid="$(cat "${SERVER_PID_FILE}")"
    if kill -0 "${server_pid}" >/dev/null 2>&1; then
        echo "The ms079 server is already running with PID ${server_pid}."
        exit 1
    fi
fi

nohup java ${SERVER_JAVA_OPTS} \
    -cp "${APP_DIR}/*:${APP_DIR}/lib/*" \
    "${SERVER_MAIN_CLASS}" \
    >"${SERVER_LOG_DIR}/ms079.out" 2>&1 &

server_pid=$!
echo "${server_pid}" >"${SERVER_PID_FILE}"
echo "Started the ms079 server with PID ${server_pid}."
