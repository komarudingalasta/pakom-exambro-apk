# PakKom Exambro V5.2

Web utama: https://komarudingalasta.github.io/pakkom-exambro/

## V5.2
- Safe Exit, Session Recovery, Connection Recovery, Device Readiness Check.
- BANTUAN SISWA: Refresh halaman, Cek koneksi, Info aplikasi, Keluar Darurat.
- Refresh tidak melepas mode ujian.
- Keluar Darurat meminta alasan + konfirmasi kedua, lalu Safe Exit.
- Alasan dan waktu emergency exit disimpan lokal untuk diagnostik.
- GURU: PIN 2468 untuk reload, kembali ke beranda, atau keluar.
- Ketuk logo PakKom 5x tetap menjadi akses guru cadangan.
- Anti-screenshot FLAG_SECURE.
- Bridge web: PakKomExambro.finishExam().

Catatan: agar emergency exit muncul di dashboard admin, web/Firestore perlu integrasi pencatatan tambahan.
