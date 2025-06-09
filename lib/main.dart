import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:osc/osc.dart';
import 'package:sensors_plus/sensors_plus.dart';

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
  final _streamSubscriptions = <StreamSubscription<dynamic>>[];
  UserAccelerometerEvent? _userAccelerometerEvent;
  GyroscopeEvent? _gyroscopeEvent;
  MagnetometerEvent? _magnetometerEvent;

  @override
  void initState() {
    super.initState();

    // Subscribe to accelerometer events
    const sensorInterval = SensorInterval.normalInterval;

    _streamSubscriptions.add(
      userAccelerometerEventStream(samplingPeriod: sensorInterval).listen(
        (UserAccelerometerEvent ev) {
          setState(() {
            _userAccelerometerEvent = ev;
          });
          oscData("accelerometer", ev.x, ev.y, ev.z);
        },
        onError: sensorError("Accelerometer"),
        cancelOnError: true,
      ),
    );

    _streamSubscriptions.add(
      gyroscopeEventStream(samplingPeriod: sensorInterval).listen(
        (GyroscopeEvent ev) {
          setState(() {
            _gyroscopeEvent = ev;
          });
          oscData("gyroscope", ev.x, ev.y, ev.z);
        },
        onError: sensorError("Gyroscope"),
        cancelOnError: true,
      ),
    );

    _streamSubscriptions.add(
      magnetometerEventStream(samplingPeriod: sensorInterval).listen(
        (MagnetometerEvent ev) {
          setState(() {
            _magnetometerEvent = ev;
          });
          oscData("magnetometer", ev.x, ev.y, ev.z);
        },
        onError: sensorError("Magnetometer"),
        cancelOnError: true,
      ),
    );
  }

  Function sensorError(String name) {
    return (_) {
      showDialog(
        context: context,
        builder: (_) => AlertDialog(
          title: Text("Sensor Not Found"),
          content: Text("This device does not support the $name sensor."),
        ),
      );
    };
  }

  void oscData(String address, double x, double y, double z) {
    RawDatagramSocket.bind(InternetAddress.anyIPv4, 0).then((socket) {
      final message = OSCMessage("/$address", arguments: [x, y, z]);
      socket.send(message.toBytes(), InternetAddress("192.168.77.233"), 8000);
    });
  }

  @override
  void dispose() {
    // Cancel all subscriptions
    super.dispose();
    for (final subscription in _streamSubscriptions) {
      subscription.cancel();
    }
  }

  @override
  Widget build(BuildContext context) {
    final mx = _magnetometerEvent?.x.toStringAsFixed(2) ?? '0.00';
    final my = _magnetometerEvent?.y.toStringAsFixed(2) ?? '0.00';
    final mz = _magnetometerEvent?.z.toStringAsFixed(2) ?? '0.00';

    final gx = _gyroscopeEvent?.x.toStringAsFixed(2) ?? '0.00';
    final gy = _gyroscopeEvent?.y.toStringAsFixed(2) ?? '0.00';
    final gz = _gyroscopeEvent?.z.toStringAsFixed(2) ?? '0.00';

    final ax = _userAccelerometerEvent?.x.toStringAsFixed(2) ?? '0.00';
    final ay = _userAccelerometerEvent?.y.toStringAsFixed(2) ?? '0.00';
    final az = _userAccelerometerEvent?.z.toStringAsFixed(2) ?? '0.00';

    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text("Magnetometer"),
          Text('X: $mx'),
          Text('Y: $my'),
          Text('Z: $mz'),
          Text("Gyroscope"),
          Text('X: $gx'),
          Text('Y: $gy'),
          Text('Z: $gz'),
          Text("Accelerometer"),
          Text('X: $ax'),
          Text('Y: $ay'),
          Text('Z: $az'),
        ],
      ),
    );
  }
}
