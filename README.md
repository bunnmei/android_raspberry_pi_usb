# raspberry pi pico to Android Sample
raspberry pi pico にはpico-examples > hello_world > usb > hello_usb.c のシリアルデータを受信するAndroidコード。

# 流れ

## Android端末にUSB接続
AndroidにUSBでraspberry pi picoに接続する。
接続していない状態では、アプリが落ちる。（エラー処理する必要がある。）
OPPOやOnePlus端末は、10分でOTGがオフになるので、要注意

## USBの取得
画面の上のボタンを押すとgetDevice()が実行される。
getDecvice()関数内では、UsbManager、UsbDeviceを初期化し、
requestPermissionでパーミッションを取得する。
ポップアップが表示され、OKを選択するとUsbDeviceBroadcastReceiver()コンポーザブルが呼び出され、connect()が実行される。

## connect()内の処理
繰り返し処理で、USB CDCのインターフェイスを取り出し、バルク転送かつバルクIN(デバイスからホスト(Android))のエンドポイントを初期化
openDeviceでデバイスオープン
claimInterfaceで排他制御？
ボーレートなどを使ったバイト配列を作る。
コントロール転送を2回実行する。(引数の値の意味はわからない)
thread内の繰り返し処理でバルク転送で送られてきたデータをprintlnでLogcatに出力している。

#参考
[https://github.com/mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
usb-serial-for-android/usbSerialForAndroid/src/main/java/com/hoho/android/usbserial/driver/CdcAcmSerialDriver.java や CommonUsbSerialPort.java あたりが参考

上記ライブラリを使うほうが簡単でUSBドライバのCH340系などにも対応していてる。
