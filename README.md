# Noswear project (shocking watch profanity tracking)  

[Noswear_Java](https://github.com/StarrLucky/Noswear_Java): voice recognition app  

[Noswear_Shocking_Board](https://github.com/StarrLucky/Noswear_Shocking_Board): controlling shocking circuit by receiving commands from BLE android app.  

--- 

## Noswear Android client

As part of Noswear project this Android application
* handling commands over Wi-Fi from voice recognition server (Noswear_Java);
* sending commands to NRF51 board.

* recognising speech from Android device microphone 
* sending shocking commands to NRF51 board if profanity in speech is present

### This app handles requests from Wi-Fi:
request with param shocking=on, like

> 192.168.x.x/?shocking=on

 will trigger sending BLE command to NRF51-board to change SHOCKINGPIN state.
 
### Requirements

* This application depends on [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) version 2.
* Android http-server library [Nanohttpd](https://github.com/NanoHttpd/nanohttpd).

----

  ![Noswear shocking watch pcb](https://github.com/StarrLucky/Noswear_Shocking_Board/blob/master/pcb.jpg)

----
