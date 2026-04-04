#!/bin/bash
# MediHelp - Restart all services
SCRIPT_DIR="$(dirname $(realpath $0))"
echo "Stopping all services..."
bash "$SCRIPT_DIR/stop-all.sh"
echo ""
echo "Starting all services..."
bash "$SCRIPT_DIR/start-all.sh" "$@"
