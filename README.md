# PakKom Exambro V5.1

Android Secure Examination Browser untuk web utama:

https://komarudingalasta.github.io/pakkom-exambro/

## Perbaikan V5.1

- Device Readiness Check: internet tervalidasi, baterai, anti-screenshot, mode ujian.
- Safe Exit: Lock Task dilepas sebelum halaman selesai/keluar.
- Akses Guru terlihat pada header dan tetap dilindungi PIN.
- Emergency backup: logo PakKom masih dapat diketuk 5× untuk membuka Akses Guru.
- Session Recovery: status ujian aktif dan URL terakhir disimpan lokal.
- Connection Recovery: layar ramah saat offline/error dan tombol Coba Lagi.
- Auto-retry saat koneksi kembali.
- Status ONLINE/OFFLINE di header ujian.
- Anti screenshot menggunakan FLAG_SECURE.
- JavaScript bridge `PakKomExambro.finishExam()` untuk pelepasan ujian otomatis setelah web selesai menyimpan jawaban.

## PIN guru awal

`2468`

Ganti konstanta `TEACHER_PIN` di `MainActivity.java` sebelum penggunaan massal.

## Catatan penting

Screen pinning/Lock Task dari aplikasi biasa bukan kiosk Device Owner absolut. Kiosk penuh memerlukan provisioning Device Owner/MDM pada perangkat sekolah.
