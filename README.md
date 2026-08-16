# PakKom Exambro V5

Project Android dibuat ulang dari nol.

## Fitur
- Halaman persiapan perangkat.
- Cek koneksi internet.
- WebView ujian PakKom Exambro.
- FLAG_SECURE untuk mencegah screenshot/rekaman layar pada level Android yang didukung.
- Immersive fullscreen.
- Screen pinning / lock task ketika tersedia.
- Safe exit: stopLockTask dipanggil sebelum halaman selesai.
- Emergency Exit guru: tekan logo PakKom 5 kali, lalu masukkan PIN.
- Bridge JavaScript `PakKomExambro.finishExam()` agar web dapat memberi tahu APK bahwa ujian selesai.
- GitHub Actions build APK.

## URL web
Default:
`https://komarudingalasta.github.io/pakkom-exambro/`

Ubah konstanta `EXAM_URL` di `MainActivity.java` bila diperlukan.

## PIN Emergency Exit
Default sementara: `2468`.
WAJIB diganti sebelum digunakan massal.

## Integrasi selesai ujian dari web
Saat siswa benar-benar selesai ujian, jalankan:

```javascript
if (window.PakKomExambro && window.PakKomExambro.finishExam) {
  window.PakKomExambro.finishExam();
}
```

APK akan melepas lock task terlebih dahulu, lalu menampilkan halaman "UJIAN TELAH SELESAI".

## Build via GitHub
Upload seluruh isi project ke root repository. Workflow harus berada di:
`.github/workflows/build-apk.yml`

Buka GitHub > Actions > Build PakKom Exambro V5 APK > Run workflow.

## Catatan keamanan
Aplikasi Android biasa tidak dapat menjadi kiosk mode penuh tanpa provisioning Device Owner/MDM. V5 menggunakan screen pinning/lock task yang tersedia untuk aplikasi biasa. Untuk perangkat sekolah yang dikelola, Device Owner dapat ditambahkan pada versi berikutnya.
