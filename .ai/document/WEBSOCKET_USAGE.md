# WebSocket Usage for Frontend

This document outlines how to connect to the WebSocket server and interpret the messages for real-time traffic updates.

## 1. Endpoint

To connect to the WebSocket, you need to use the following endpoint with a library that supports STOMP over WebSocket (like SockJS and Stomp.js).

- **URL**: `/traffic-ws`

Example connection with SockJS and Stomp.js:

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const socket = new SockJS('/traffic-ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    // ... subscribe to topics
});
```

## 2. Listening for Events

Once connected, you can subscribe to the `/topic/traffic` destination to receive real-time traffic updates.

- **Topic**: `/topic/traffic`

Example subscription:

```javascript
stompClient.subscribe('/topic/traffic', function (message) {
    const trafficDecision = JSON.parse(message.body);
    console.log(trafficDecision);
    // ... handle the message
});
```

## 3. Message Payload

The message received on the `/topic/traffic` topic will be a JSON object with the following structure.

### `TrafficDecisionMessage`

| Field | Type | Description |
| :--- | :--- | :--- |
| `northSouth` | `TrafficInput` | Traffic data for the North-South lane. |
| `eastWest` | `TrafficInput` | Traffic data for the East-West lane. |
| `decision` | `IntersectionDecision` | The calculated signal decisions for the intersection. |
| `timestamp` | `String` (ISO 8601) | The timestamp when the decision was made. |

### `TrafficInput`

| Field | Type | Description |
| :--- | :--- | :--- |
| `laneName` | `String` | The name of the lane (e.g., "North-South"). |
| `vehicleCount` | `int` | The number of vehicles detected. |
| `congestionLevel`| `double` | A calculated level of traffic congestion. |

### `IntersectionDecision`

| Field | Type | Description |
| :--- | :--- | :--- |
| `northSouthDecision`| `SignalDecision` | Signal timing decision for the North-South lane. |
| `eastWestDecision` | `SignalDecision` | Signal timing decision for the East-West lane. |

### `SignalDecision`

| Field | Type | Description |
| :--- | :--- | :--- |
| `laneName` | `String` | The name of the lane. |
| `greenDuration` | `int` | The duration in seconds for the green light. |
| `redDuration` | `int` | The duration in seconds for the red light. |
| `reason` | `String` | The reason for the decision. |

### Example Message:

```json
{
  "northSouth": {
    "laneName": "North-South",
    "vehicleCount": 25,
    "congestionLevel": 0.6
  },
  "eastWest": {
    "laneName": "East-West",
    "vehicleCount": 10,
    "congestionLevel": 0.25
  },
  "decision": {
    "northSouthDecision": {
      "laneName": "North-South",
      "greenDuration": 45,
      "redDuration": 15,
      "reason": "Higher congestion detected."
    },
    "eastWestDecision": {
      "laneName": "East-West",
      "greenDuration": 15,
      "redDuration": 45,
      "reason": "Derived from North-South synchronization."
    }
  },
  "timestamp": "2023-10-27T10:00:00Z"
}
```
