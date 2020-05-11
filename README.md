# Noswear Android client

As part of Noswear project this Android client application
*  handles command over Wi-Fi from Noswear voice recognition server;
 * sending it to NRF51 board.
 
In the future this APP could be also appears as a standalone voice recognition server.

## This app handles requests from Wi-Fi:
request with param *shocking=on*, like

> 192.168.x.x/?shocking=on

 will trigger sending BLE command (*LEDPIN* high state, for example) into NRF51-board.
 
## NRF51 BLE:

Service UUID: `00001523-1212-EFDE-1523-785FEABCD123`

A simplified proprietary service by Nordic Semiconductor, containing two characteristics one to 
control LED 3 and Button 1:

- First characteristic controls the LED state (On/Off).
  - UUID: **`00001525-1212-EFDE-1523-785FEABCD123`**
  - Value: **`1`** => LED On
  - Value: **`0`** => LED Off

- Second characteristic notifies central of the button state on change (Pressed/Released).
  - UUID: **`00001524-1212-EFDE-1523-785FEABCD123`**
  - Value: **`1`** => Button Pressed
  - Value: **`0`** => Button Released

## Requirements

* This application depends on [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) version 2.
* Android http-server library [Nanohttpd](https://github.com/NanoHttpd/nanohttpd).


## Note

In order to scan for Bluetooth LE device the Location permission must be granted and, on some phones, 
the Location must be enabled. This app will not use the location information in any way.