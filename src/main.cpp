#include <Arduino.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <DHT.h>
#include <Wire.h>
#include "Adafruit_SGP30.h"
#include "wifipwd.h"

#define DHT11_PIN  21 // ESP32 pin GPIO21 connected to DHT11 sensor

DHT dht11(DHT11_PIN, DHT11);

// const char *dest_ip = "10.0.8.73";
const char *dest_ip = "255.255.255.255";
const int dest_port = 3213;

WiFiUDP udp;

Adafruit_SGP30 sgp;
#define SDA 23
#define SCL 22

uint32_t getAbsoluteHumidity(float temperature, float humidity) {
	// approximation formula from Sensirion SGP30 Driver Integration chapter 3.15
	const float absoluteHumidity = 216.7f * ((humidity / 100.0f) * 6.112f * exp((17.62f * temperature) / (243.12f + temperature)) / (273.15f + temperature)); // [g/m^3]
	const uint32_t absoluteHumidityScaled = static_cast<uint32_t>(1000.0f * absoluteHumidity); // [mg/m^3]
	return absoluteHumidityScaled;
}

void setup() {
	Serial.begin(921600);
	Serial.println("Hello wRoom");
	pinMode(LED_BUILTIN, OUTPUT);
	dht11.begin();
	Wire.begin(SDA, SCL);
	
	if (! sgp.begin()){
		while (1) {
			Serial.println("I2C Sensor not found :(");
			delay(200);
		}
	}

	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

int counter = 0;
int state = 0;

void loop() {
	/*
	switch (state) {
		case 0:
			delay(600);
			digitalWrite(LED_BUILTIN, HIGH);
			Serial.println("ping");
			break;
		case 1:
			delay(600);
			Serial.println("pong");
			break;
		case 2:
			delay(600);
			digitalWrite(LED_BUILTIN, LOW);
			Serial.println("oink");
			break;
	}
	state = (state + 1) % 3;
	*/

	Serial.println(" .><. ");

	if (WiFi.status() != WL_CONNECTED) {
		Serial.println("no wifi bye");
		delay(1000);
		return;
	}

	// .------: temperature and humidity :------.
	float humi  = dht11.readHumidity();
	// read temperature in Celsius
	float tempC = dht11.readTemperature();

	if (isnan(humi) || isnan(tempC)) {
		Serial.println("Failed to read from DHT sensor!");
		return;
	}
	Serial.print("Temp "); Serial.print(tempC); Serial.println("°C\t");
	Serial.print("Humidity "); Serial.print(humi); Serial.println("%");

	// .------: SGP30 :------.
	sgp.setHumidity(getAbsoluteHumidity(tempC, humi));
	if (! sgp.IAQmeasure()) {
		Serial.println("Measurement failed");
		return;
	}
	Serial.print("TVOC "); Serial.print(sgp.TVOC); Serial.println(" ppb\t");
	Serial.print("eCO2 "); Serial.print(sgp.eCO2); Serial.println(" ppm");
	delay(1000);

	counter++;
	if (counter == 30) {
		counter = 0;

		uint16_t TVOC_base, eCO2_base;
		if (! sgp.getIAQBaseline(&eCO2_base, &TVOC_base)) {
			Serial.println("Failed to get baseline readings");
			return;
		}
		Serial.print("****Baseline values: eCO2: 0x"); Serial.print(eCO2_base, HEX);
		Serial.print(" & TVOC: 0x"); Serial.println(TVOC_base, HEX);
	}
	

	// .------: communication :------.
	if (counter % 5 == 0) {
		const size_t LEN = 24;
		byte packet[LEN];
		memset(packet, '/', LEN);
		strcpy((char *)packet, "oing");
		float *f = (float *)(packet + 4);
		*f = tempC;
		f++;
		*f = humi;
		f++;
		int *p = reinterpret_cast<int*>(f);
		*p = (int)sgp.TVOC;
		p++;
		*p = (int)sgp.eCO2;


		Serial.print("sending packet ");
		udp.beginPacket(dest_ip, dest_port);
		udp.write(packet, LEN);
		udp.endPacket();
		for (int i = 0; i < LEN; i++) {
			Serial.print(packet[i]);
			Serial.print(" ");
		}
	}
}
