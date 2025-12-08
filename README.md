# AI2MQTT

**AI2MQTT** is a hotswappable Spring Boot bridge that connects your smart home or enterprise messaging grid to modern AI models.

It listens for prompts on **MQTT** (Home Assistant) or **ActiveMQ**, maintains conversation context (chat chaining), and forwards requests to **OpenAI**, **Google Gemini**, or self-hosted LLMs like **vLLM** or **Ollama**.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Features

* **Hotswappable Messaging:** Switch between **MQTT** (Mosquitto/Home Assistant) and **ActiveMQ Classic** with a single config flag.
* **Universal AI Support:** Works with OpenAI (`gpt-4o`), Google Gemini (`gemini-1.5-flash`), or Local AI (`vLLM`/`Ollama`) via OpenAI-compatible endpoints.
* **Chat Chaining:** Automatically maintains conversation history. Just pass a `threadId` to keep the context alive.
* **Dynamic Personas:** Override the System Prompt per message (e.g., make the AI sarcastic, concise, or specific to a room context on the fly).
* **Home Assistant Auto-Discovery:** Automatically registers as a device in Home Assistant. No YAML config required.
* **Web Dashboard:** Includes a Dark Mode web UI for testing prompts, viewing history, and managing threads.

---

## Quick Start (Docker Compose)

The easiest way to run AI2MQTT is via Docker Compose.

### 1. Create a `.env` file
Create a `.env` file in your directory to store your secrets.

```properties
GEMINI_API_KEY=your_key_here
MQTT_USER=your_mqtt_user
MQTT_PASSWORD=your_mqtt_password
```

### 2. Run with Docker Compose
```yaml
services:
  ai2mqtt:
    image: ghcr.io/azziedevelopment/ai2mqtt:latest
    container_name: ai2mqtt
    restart: unless-stopped
    ports:
      - "8080:8080" # Web Dashboard
    environment:
      # --- SECRETS ---
      - GEMINI_API_KEY=${GEMINI_API_KEY}
      - MQTT_USER=${MQTT_USER}
      - MQTT_PASSWORD=${MQTT_PASSWORD}

      # --- CONFIGURATION ---
      - MESSAGING_TYPE=mqtt
      - OPENAI_MODEL=gemini-2.5-flash
      # Use the OpenAI-compatible endpoint for Gemini
      - OPENAI_BASE_URL=[https://generativelanguage.googleapis.com/v1beta/openai/](https://generativelanguage.googleapis.com/v1beta/openai/)

      # --- CONNECTIVITY ---
      # Use 'core-mosquitto' if running inside HA OS, or your broker IP
      - MQTT_BROKER_URL=tcp://core-mosquitto:1883

    volumes:
      - ./data:/app/data # Persist chat history
```

---

## Usage & Payload Format

Send a JSON payload to the **Prompts Topic** (`ai/prompts`).

### Request Format
```json
{
  "id": "optional-uuid",
  "threadId": "kitchen-display",
  "text": "What should I cook for dinner?",
  "systemPrompt": "You are a Michelin star chef. Be brief.",
  "maxTokens": 200
}
```

| Field | Description |
| :--- | :--- |
| `threadId` | **Crucial.** Messages with the same ID share history/context. |
| `text` | The actual question for the AI. |
| `systemPrompt` | *(Optional)* Override the default persona for this specific message. |

### Response Format
The service replies to `ai/responses`:
```json
{
  "id": "uuid-of-request",
  "threadId": "kitchen-display",
  "response": "How about a pan-seared salmon with asparagus?"
}
```

---

## Home Assistant Integration

When running in `mqtt` mode, AI2MQTT automatically broadcasts a discovery packet on startup.

1.  **Device:** Appears as **"AI2MQTT Bridge"** in HA Devices.
2.  **Sensor:** Creates a sensor `sensor.ai_last_response`.
	* **State:** Truncated text (max 250 chars) to prevent state limit errors.
	* **Attribute `full_text`:** Contains the complete AI response (no length limit).

### Displaying Full Text (Lovelace Markdown Card)
To view the full response in your dashboard, use the markdown card:
```yaml
type: markdown
content: "{{ state_attr('sensor.ai_last_response', 'full_text') }}"
title: AI Response
```

---

## Local Development Setup

To run this project locally, you need **Java 25**.

### 1. Clone & Configure Secrets
```bash
git clone [https://github.com/azziedevelopment/ai2mqtt.git](https://github.com/azziedevelopment/ai2mqtt.git)
cd ai2mqtt
cp src/main/resources/secrets.properties.template src/main/resources/secrets.properties
```
Edit `secrets.properties` with your real API keys. This file is git-ignored.

### 2. Run Dependencies (Dev Mode)
Use the included Compose file to spin up local ActiveMQ and Mosquitto brokers.
```bash
docker-compose up -d
```

### 3. Start the Application
You can run it directly from IntelliJ or via Maven:
```bash
./mvnw spring-boot:run
```

Access the dashboard at `http://localhost:8080`.

---

## Configuration Reference

| Environment Variable | Default | Description |
| :--- | :--- | :--- |
| `MESSAGING_TYPE` | `mqtt` | `mqtt` or `activemq` |
| `OPENAI_API_KEY` | - | Your API Key (Gemini/OpenAI) |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Change for Gemini/LocalAI |
| `OPENAI_MODEL` | `gpt-4o` | Model ID (`gemini-1.5-flash`, etc.) |
| `OPENAI_SYSTEM_PROMPT` | "You are a helpful..." | Default persona |
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | Broker Address |
| `MQTT_USERNAME` | - | Broker User |
| `MQTT_PASSWORD` | - | Broker Password |

---

## License

MIT License. Copyright (c) 2025 Azzie Development.
