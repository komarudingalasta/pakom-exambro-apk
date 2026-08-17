# PakKom Exambro V5.2.1

Perbaikan khusus menu Bantuan Siswa.

## Bantuan Siswa
Menu Bantuan sekarang menggunakan tombol besar vertikal yang selalu terlihat:
1. REFRESH HALAMAN — reload WebView, mode ujian tetap aktif.
2. CEK KONEKSI — cek internet tanpa meninggalkan ujian.
3. INFO APLIKASI — versi/status APK.
4. KELUAR DARURAT — selalu terlihat di bagian bawah.

Keluar Darurat:
- tidak memerlukan PIN siswa,
- meminta alasan,
- meminta konfirmasi kedua,
- menjalankan Safe Exit (stopLockTask + pulihkan navigasi),
- menyimpan alasan dan waktu secara lokal.

## Guru
Tombol GURU tetap memakai PIN awal 2468.
Ketuk logo PakKom 5x tetap menjadi akses guru cadangan.

Web utama:
https://komarudingalasta.github.io/pakkom-exambro/

Bridge selesai ujian:
PakKomExambro.finishExam()
