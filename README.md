
# Smart Water Leakage Detection System

## Project Overview
This project is a Java-based Smart Water Leakage Detection System.
It simulates water flow readings and identifies possible leaks
when the flow rate exceeds a defined threshold.

## Features
- Water flow simulation
- Automatic leak detection
- Warning when high flow is detected
- Dashboard displaying water flow status
- Reading history saved in a CSV file

## Technologies Used
- Java 21
- HTML
- CSS
- JavaScript
- Java HTTP Server
- CSV file storage

## How to Run
1. Install Java Development Kit (JDK) 21 or later.
2. Open the project folder in VS Code.
3. Open the terminal in the project folder.
4. Compile the program:

   javac --add-modules jdk.httpserver WaterLeakApp.java

5. Run the program:

   java --add-modules jdk.httpserver WaterLeakApp

6. Open http://localhost:8080 in a browser.

## How It Works
The application simulates normal and abnormal water flow.
When flow exceeds 10 litres per minute, it displays a leak warning.
Readings are stored locally in readings.csv.

## Note
This is a software simulation. Physical leak detection requires
appropriate water-flow sensors and hardware.
