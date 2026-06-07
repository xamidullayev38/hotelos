#!/usr/bin/env bash
# Launches all HotelOS processes in the background. Use ./stop.sh to terminate.
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p logs data run

# Start order matters: broker first so subscribers can connect on boot.
start() {
  local name="$1"
  local jar="$2"
  echo "starting $name..."
  java -jar "$jar" > "logs/$name.log" 2>&1 &
  echo $! > "run/$name.pid"
}

start broker          broker/target/broker-1.0.0.jar
sleep 2
start reception      reception-service/target/reception-service-1.0.0.jar
start housekeeping   housekeeping-service/target/housekeeping-service-1.0.0.jar
start roomservice    roomservice-service/target/roomservice-service-1.0.0.jar
start maintenance    maintenance-service/target/maintenance-service-1.0.0.jar
start dashboard      dashboard/target/dashboard-1.0.0.jar

cat <<EOF

HotelOS is starting. Give it ~8 seconds.

  Broker        ws://localhost:4005/broker
  Reception     http://localhost:4001
  Housekeeping  http://localhost:4002
  Room Service  http://localhost:4003
  Maintenance   http://localhost:4004
  Dashboard     http://localhost:4000   (sign in with token: demo-token)

Logs:  logs/*.log
Stop:  ./stop.sh

EOF
