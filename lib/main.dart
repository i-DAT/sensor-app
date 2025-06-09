import 'dart:async';

import 'package:flutter/material.dart';
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
    // Subscribe to accelerometer events
    super.initState();
    const sensorInterval = SensorInterval.normalInterval;

    _streamSubscriptions.add(
      userAccelerometerEventStream(samplingPeriod: sensorInterval).listen(
        (UserAccelerometerEvent event) {
          setState(() {
            _userAccelerometerEvent = event;
          });
        },
        onError: sensorError("Accelerometer"),
        cancelOnError: true,
      ),
    );

    _streamSubscriptions.add(
      gyroscopeEventStream(samplingPeriod: sensorInterval).listen(
        (GyroscopeEvent event) {
          setState(() {
            _gyroscopeEvent = event;
          });
        },
        onError: sensorError("Gyroscope"),
        cancelOnError: true,
      ),
    );

    _streamSubscriptions.add(
      magnetometerEventStream(samplingPeriod: sensorInterval).listen(
        (MagnetometerEvent event) {
          setState(() {
            _magnetometerEvent = event;
          });
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
