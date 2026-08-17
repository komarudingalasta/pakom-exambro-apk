# PakKom Exambro V5.2.2

Perbaikan utama: Keluar Darurat siswa.

## Perubahan V5.2.2
- Tombol KELUAR DARURAT tetap di Bantuan.
- Setelah alasan + konfirmasi:
  1. status sesi lokal dinonaktifkan,
  2. WebView dihentikan,
  3. `stopLockTask()` dipanggil langsung,
  4. immersive/fullscreen dipulihkan,
  5. Android diarahkan ke Home,
  6. task Exambro ditutup.
- Tidak lagi berhenti pada dialog "Tutup Exambro".
- Jalur keluar guru juga memakai pelepasan Lock Task yang lebih kuat.
- Session Recovery, Connection Recovery, Refresh siswa, dan Safe Finish web tetap tersedia.

PIN guru awal: 2468
Web: https://komarudingalasta.github.io/pakkom-exambro/

Catatan:
Jika perangkat diprovision sebagai Device Owner/MDM dengan kiosk penuh, aplikasi biasa tidak selalu boleh keluar sendiri. V5.2.2 ditujukan untuk mode Lock Task/screen pinning aplikasi biasa.
