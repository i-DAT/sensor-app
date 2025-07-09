# Sensor App

This app records sensor data from a mobile device or smartwatch and sends it to a host on the local network using the OSC protocol.
Currently, only rotation data is read, but other data could be read using the `sensor_plus` plugin.

The app has only been tested on Android but should also run on iOS out of the box.

## Installation

Install the APK from the [releases page](https://github.com/i-DAT/sensor-app/releases) by downloading it to your device and opening it.
You may need to allow installation from unknown sources in the device settings.

## Usage

When the app is open and the device screen is on, sensor data is monitored and displayed.
When a host address is entered, the app will send the sensor data to that host using the OSC protocol.
It is configured to send packets to port 8000 in the format `/rotation float float float`.

The device must be on the same network as the host (note, many networks such as Eduroam will block OSC messages).
To find the host's address, run `ipconfig` in a windows command prompt or `ip a` in a Linux shell and look for the ipv4 address, usually of the form `192.168.xxx.xxx`.

Finally, run a server on the host device to receive incoming messages.
The simplest server, for testing, is a Python script such as:

```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", 8000))

while True:
    data, addr = sock.recvfrom(1024)
    print(data)
```

## Development

Install Flutter using the [installation instructions](https://docs.flutter.dev/get-started/install) and ensure that `flutter doctor` reports no errors.
The easiest development experience is with VSCode and the [Flutter extension](https://marketplace.visualstudio.com/items?itemName=Dart-Code.flutter).

To build a release APK, run `flutter build apk --release && flutter install`.

## Smartwatches

Most Android WearOS watches don't have USB ports, so installation must be wireless.
Follow the development setup instructions above, and make sure the watch is connected to the same network as the development computer.

In the watch settings, find "About watch > Software information > Software version" and tap it until developer mode is enabled.
Then, in the watch settings, find "Developer options > Wireless debugging" and enable it.
Tap "Pair new device" then run `adb pair <ip>:<port> <pairing-code>` using the information displayed on the watch.
Once this is successful, go back to the Wireless debugging screen and run `adb connect <ip>:<port>` using the information displayed.
This will be the same IP but a **different port**.
The watch should show up as a device in VSCode or when using `flutter run`.
