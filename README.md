# Noswear Android client (shocking watch profanity tracking)

As part of Noswear project this Android client application
*  handles command over Wi-Fi from Noswear voice recognition server;
 * sending it to NRF51 board.
 
In the future this APP could be also appears as a standalone voice recognition server.

## This app handles requests from Wi-Fi:
request with param shocking=on, like

> 192.168.x.x/?shocking=on

 will trigger sending BLE command to NRF51-board to change SHOCKINGPIN state.
 
## NRF51 BLE:

Service UUID: `00001523-1212-EFDE-1523-785FEABCD123`

A simplified proprietary service by Nordic Semiconductor, containing two characteristics one to 
control LED 3 and Button 1:

- First characteristic controls the shocking pin state
  - UUID: **`00001525-1212-EFDE-1523-785FEABCD123`**
  - Value: **`1`** => shocking pin on command


## Requirements

* This application depends on [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) version 2.
* Android http-server library [Nanohttpd](https://github.com/NanoHttpd/nanohttpd).


## Note

In order to scan for Bluetooth LE device the Location permission must be granted and, on some phones, 
the Location must be enabled. This app will not use the location information in any way.
