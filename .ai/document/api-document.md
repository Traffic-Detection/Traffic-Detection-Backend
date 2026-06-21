# API Documentation

## Signal History API

### Get all signal history

- **URL:** `/api/signal-history`
- **Method:** `GET`
- **Description:** Retrieves a list of all signal history records.
- **Success Response:**
  - **Code:** 200 OK
  - **Content:** `application/json`
  - **Body:**
    ```json
    [
      {
        "id": 1,
        "intersectionId": 1,
        "laneId": 1,
        "greenDuration": 30,
        "redDuration": 30,
        "appliedAt": "2024-07-28T10:00:00Z"
      },
      {
        "id": 2,
        "intersectionId": 1,
        "laneId": 2,
        "greenDuration": 40,
        "redDuration": 20,
        "appliedAt": "2024-07-28T10:05:00Z"
      }
    ]
    ```

## Traffic Log API

### Get all traffic logs

- **URL:** `/api/traffic-logs`
- **Method:** `GET`
- **Description:** Retrieves a list of all traffic log records.
- **Success Response:**
  - **Code:** 200 OK
  - **Content:** `application/json`
  - **Body:**
    ```json
    [
      {
        "id": 1,
        "laneId": 1,
        "vehicleCount": 50,
        "congestionLevel": 75.5,
        "recordedAt": "2024-07-28T10:00:00Z"
      },
      {
        "id": 2,
        "laneId": 2,
        "vehicleCount": 20,
        "congestionLevel": 30.0,
        "recordedAt": "2024-07-28T10:05:00Z"
      }
    ]
    ```
