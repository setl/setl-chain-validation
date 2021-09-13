# Dash Scenario Overlay - Demonstration ONLY, not production release ready
The dash scenario over module overlays the javascript dashboard scenario resource. This is a pure java script port of the php site.

## Usage
If the dash project is included in a spring boot module, navigate to http://server:port/dash.html?host=[HOST]&port=[PORT]

Where [HOST] is the websocket host, and [PORT] is the websocket port.

## Configuration
Modify javascript/mod_init.js to set host and port of websocket connection, or set as parameters on url


e.g.
http://localhost:8090/dash.html?host=10.0.1.43&port=13515#
for cobalt.

http://localhost:8090/dash.html?host=127.0.0.1&port=8090
for local websocket server.