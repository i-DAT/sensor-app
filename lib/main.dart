import 'dart:io';

import 'package:flutter/material.dart';
import 'package:osc/osc.dart';
import 'package:flutter_rotation_sensor/flutter_rotation_sensor.dart';

void main() {
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(body: Center(child: Sensor())),
    );
  }
}

class Sensor extends StatefulWidget {
  const Sensor({super.key});

  @override
  State<Sensor> createState() => _SensorState();
}

class _SensorState extends State<Sensor> {
  String address = "";
  bool valid = false;

  @override
  void initState() {
    super.initState();
    RotationSensor.samplingPeriod = SensorInterval.fastestInterval;
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder(
      stream: RotationSensor.orientationStream,
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        final data = snapshot.data!.eulerAngles;
        RawDatagramSocket.bind(InternetAddress.anyIPv4, 0).then((socket) {
          final message = OSCMessage(
            "/rotation",
            arguments: [data.pitch, data.roll, data.azimuth],
          );
          try {
            final a = InternetAddress(address);
            final n = socket.send(message.toBytes(), a, 8000);
            setState(() => valid = n > 0);
          } catch (_) {
            setState(() => valid = false);
          }
        });
        return Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text("Rotation Sensor Data"),
              Text("X: ${data.pitch.toStringAsFixed(2)}"),
              Text("Y: ${data.roll.toStringAsFixed(2)}"),
              Text("Z: ${data.azimuth.toStringAsFixed(2)}"),
              const SizedBox(height: 80),
              SizedBox(
                width: 200,
                child: TextField(
                  decoration: InputDecoration(
                    labelText: "Enter OSC Address",
                    errorText: valid ? null : "Invalid address",
                    border: OutlineInputBorder(),
                  ),
                  onSubmitted: (value) {
                    setState(() => address = value);
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
