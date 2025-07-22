# Sensor App

This app records sensor data from a mobile device or smartwatch and sends it to a host on the local
network using the OSC protocol.

## Installation

Install the APK from the [releases page](https://github.com/i-DAT/sensor-app/releases) by
downloading it to your device and opening it.
You may need to allow installation from unknown sources in the device settings.

## Usage

The app initially waits for a host on the same local network.
It detects this by waiting for an OSC message of the form `/host <host-name> <host-ip> <host-port>`
at `239.255.255.250:4001`.

OSC messages are sent to this IP/port:

- `/rotation <float x> <float y> <float z>`
- `/single_tap`
- `/double_tap`
- `/long_press`
- `/drag_start <float x> <float y>`
- `/drag <float x> <float y>`
- `/drag_end`

The app also listens for OSC messages from the host.

## Development

Install [Android Studio](https://developer.android.com/studio), then open the project and connect a
device.

A debug server can be found in [server/server.py](server/server.py).

## Smartwatches

Most Android WearOS watches don't have USB ports, so installation must be wireless.
Follow the development setup instructions above, and make sure the watch is connected to the same
network as the development computer.

In the watch settings, find "About watch > Software information > Software version" and tap it until
developer mode is enabled.
Then, in the watch settings, find "Developer options > Wireless debugging" and enable it.
Tap "Pair new device" then run `adb pair <ip>:<port> <pairing-code>` using the information displayed
on the watch.
Once this is successful, go back to the Wireless debugging screen and run `adb connect <ip>:<port>`
using the information displayed.
This will be the same IP but a **different port**.
The watch should now show as a device in Android Studio.