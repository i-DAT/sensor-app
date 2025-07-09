import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const _channel = MethodChannel('gyro_udp_channel');

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

  Future<void> sendTargetAddress(String ipAddress) async {
    await _channel.invokeMethod('setTargetAddress', {'ip': ipAddress});
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SizedBox(
        width: 180,
        child: TextField(
          decoration: InputDecoration(
            labelText: "Host address",
            border: OutlineInputBorder(),
          ),
          onSubmitted: (value) {
            setState(() => address = value);
            sendTargetAddress(value);
          },
        ),
      ),
    );
  }
}
