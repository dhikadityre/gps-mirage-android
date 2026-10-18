DOD & Test Case — Fitur Rewind Map
1. Tujuan Fitur

Fitur Rewind memungkinkan user mengembalikan posisi map ke posisi acuan sebelumnya berdasarkan riwayat aksi Lock Location, Start Mock/Pinpoint, dan Center Anchor.

Rewind memiliki target akhir yang berbeda berdasarkan status user:

Jika user sudah melakukan Mock/Pinpoint, target akhir Rewind adalah lokasi Mock/Pinpoint terakhir.
Jika user belum melakukan Mock/Pinpoint, target akhir Rewind adalah Center Anchor.
Jika user sudah mencapai target akhir, tombol Rewind dinonaktifkan.
2. Definisi Istilah
Istilah	Definisi
Center Anchor	Posisi pusat awal map yang menjadi fallback jika user belum melakukan Mock/Pinpoint.
Mock/Pinpoint Location	Koordinat saat user melakukan Start Mock atau Pinpoint.
Lock Location	Koordinat terakhir yang disimpan ketika user melakukan Lock Location.
Current Map Position	Posisi map yang sedang ditampilkan, termasuk setelah user menggeser map.
Rewind Stage	Tahap Rewind yang sedang dijalankan berdasarkan riwayat aksi user.
3. Definition of Done (DOD)
3.1 Aturan Utama
Rewind pertama mengembalikan map ke Lock Location terakhir yang valid dan tersimpan.
Jika user sudah melakukan Mock/Pinpoint, Rewind berikutnya mengembalikan map ke Mock/Pinpoint Location terakhir.
Jika user belum melakukan Mock/Pinpoint, Rewind berikutnya mengembalikan map ke Center Anchor.
Jika Rewind menuju Mock/Pinpoint dan status masih Locked, sistem harus melakukan Unlock terlebih dahulu sebelum memindahkan map ke Mock/Pinpoint Location.
Jika user melakukan Unlock manual setelah Rewind pertama, target Rewind berikutnya tetap mengikuti kondisi berikut:
Jika user sudah melakukan Mock/Pinpoint, kembali ke Mock/Pinpoint Location terakhir.
Jika user belum melakukan Mock/Pinpoint, kembali ke Center Anchor.
Setelah map mencapai Mock/Pinpoint Location atau Center Anchor sebagai target akhir, tombol Rewind dinonaktifkan.
Jika Mock/Pinpoint dan Lock berada pada koordinat yang sama, urutan tahap tetap dipertahankan.
Menggeser map secara manual tidak mengubah posisi Mock/Pinpoint, Lock Location yang tersimpan, atau Center Anchor.
Lock ulang setelah Rewind pertama memperbarui target Rewind pertama ke Lock Location terbaru.
Tap Rewind berulang tidak boleh menyebabkan tahap terlewati, state tidak konsisten, atau perpindahan ke koordinat yang salah.
3.2 Acceptance Criteria
AC ID	Acceptance Criteria
AC-01	Sistem menyimpan Center Anchor sebagai posisi awal dan fallback ketika user belum melakukan Mock/Pinpoint.
AC-02	Sistem menyimpan Lock Location terbaru sebagai target Rewind pertama jika memenuhi aturan validitas Lock dan radius.
AC-03	Sistem menyimpan posisi Mock/Pinpoint terakhir sebagai target akhir jika user sudah melakukan Mock/Pinpoint.
AC-04	Rewind pertama mengembalikan map ke Lock Location yang valid dan tersimpan.
AC-05	Setelah Rewind pertama, Rewind berikutnya kembali ke Mock/Pinpoint Location terakhir jika user sudah melakukan Mock/Pinpoint.
AC-06	Jika user belum melakukan Mock/Pinpoint, Rewind berikutnya kembali ke Center Anchor.
AC-07	Jika Rewind menuju Mock/Pinpoint dan status masih Locked, sistem melakukan Unlock terlebih dahulu.
AC-08	Unlock manual setelah Rewind pertama tidak menghapus target Mock/Pinpoint.
AC-09	Setelah map mencapai Mock/Pinpoint atau Center Anchor sebagai target akhir, tombol Rewind dinonaktifkan.
AC-10	Jika koordinat Mock/Pinpoint dan Lock sama, urutan tahap tetap dipertahankan.
AC-11	Perpindahan map biasa tidak mengubah posisi Mock/Pinpoint, Lock Location tersimpan, atau Center Anchor.
AC-12	Tap Rewind berulang tidak boleh melewati tahap atau mengaktifkan kembali Rewind setelah target akhir tercapai.
4. Test Data

Gunakan label koordinat berikut untuk menyamakan hasil pengujian.

Data	Deskripsi
A0	Center Anchor
A	Mock/Pinpoint Location
B	Lock Location pertama
C	Posisi map setelah digeser
D	Lock Location kedua
X	Posisi Lock di luar radius
R	Posisi tepat pada batas radius
Radius	Radius Center Anchor yang dikonfigurasi pada aplikasi

Catatan: A, B, C, D, X, R, dan A0 merupakan label koordinat uji, bukan koordinat geografis sebenarnya.

Definisi Prioritas
P0: Fungsi inti atau risiko state salah yang dapat memblokir fitur.
P1: Kombinasi state penting dan validasi radius.
P2: Edge case, ketahanan interaksi, dan regression.
5. Test Case Positif — Happy Path dan Kombinasi State
TC ID	Priority	Precondition	Steps	Expected Result
RW-001	P0	User belum Mock/Pinpoint dan belum Lock.	1. Buka map. 2. Geser ke C. 3. Tap Rewind.	Map kembali ke Center Anchor A0 dan tombol Rewind dinonaktifkan.
RW-002	P0	User belum Mock/Pinpoint. Lock B valid di dalam radius.	1. Lock di B. 2. Geser ke C. 3. Tap Rewind.	Map kembali ke B.
RW-003	P0	User belum Mock/Pinpoint. Map sudah kembali ke B.	1. Tap Rewind lagi.	Map kembali ke Center Anchor dan tombol Rewind dinonaktifkan.
RW-004	P0	User sudah Mock/Pinpoint di A dan Lock di B. Map berada di C.	1. Tap Rewind.	Map kembali ke B dan status Locked tetap aktif.
RW-005	P0	RW-004 sudah selesai dan status masih Locked.	1. Tap Rewind lagi.	Sistem melakukan Unlock terlebih dahulu, lalu map kembali ke A. Tombol Rewind dinonaktifkan setelah target akhir tercapai.
RW-006	P0	RW-004 sudah selesai dan user melakukan Unlock manual.	1. Tap Rewind lagi.	Map kembali ke Mock/Pinpoint Location A dan tombol Rewind dinonaktifkan.
RW-007	P0	User sudah Mock/Pinpoint di A dan map sudah berada di A sebagai target akhir.	1. Periksa tombol Rewind. 2. Coba tap tombol jika memungkinkan.	Tombol Rewind dinonaktifkan dan tidak ada perpindahan map.
RW-008	P0	User belum Mock/Pinpoint. Lock berada di luar radius.	1. Lock di X. 2. Geser ke C. 3. Tap Rewind.	Map kembali ke Center Anchor dan tombol Rewind dinonaktifkan.
RW-009	P1	Mock/Pinpoint dan Lock berada pada koordinat yang sama, yaitu A.	1. Geser map ke C. 2. Tap Rewind pertama. 3. Tap Rewind kedua.	Tahap pertama menuju Lock A. Tahap kedua menuju Mock/Pinpoint A, dengan Unlock terlebih dahulu jika diperlukan. Setelah target akhir tercapai, tombol Rewind dinonaktifkan.
RW-010	P0	User sudah Mock/Pinpoint di A dan Lock di B.	1. Geser ke C. 2. Rewind ke B. 3. Lock ulang di D. 4. Geser map. 5. Tap Rewind.	Map kembali ke Lock Location terbaru, yaitu D. Rewind berikutnya kembali ke A dan kemudian dinonaktifkan.
6. Test Case Negatif — Validasi State
TC ID	Priority	Precondition	Steps	Expected Result
RW-011	P1	User belum Mock/Pinpoint dan map sudah di Center Anchor.	1. Periksa tombol Rewind.	Tombol Rewind disabled dan tidak bisa memicu aksi.
RW-012	P1	User sudah mencapai Mock/Pinpoint A sebagai target akhir.	1. Periksa tombol Rewind.	Tombol Rewind disabled dan tidak bisa memicu aksi.
RW-013	P0	Proses Rewind sedang berlangsung.	1. Tap Rewind berkali-kali dengan cepat.	Sistem tidak melewati tahap atau menjalankan perpindahan yang tidak konsisten.
RW-014	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Rewind ke B. 2. Unlock manual. 3. Geser ke C. 4. Tap Rewind.	Map kembali ke A.
RW-015	P1	User belum Mock/Pinpoint dan Lock B valid.	1. Rewind ke B. 2. Unlock manual. 3. Geser ke C. 4. Tap Rewind.	Map kembali ke Center Anchor dan tombol Rewind dinonaktifkan.
RW-016	P1	User sudah Mock/Pinpoint A dan Lock B.	1. Rewind pertama. 2. Lakukan Start Mock/Pinpoint ulang di D.	.... Perlu ditentukan apakah Mock terbaru menggantikan target lama dan apakah siklus Rewind di-reset.
RW-017	P1	Lock berada tepat di batas radius.	1. Lock di R. 2. Geser map. 3. Tap Rewind.	.... Tentukan apakah batas radius dianggap valid atau tidak valid.
RW-018	P1	Map sudah mencapai target akhir dan tombol Rewind disabled.	1. Geser map secara manual ke lokasi lain.	.... Tentukan apakah tombol tetap disabled atau diaktifkan kembali ketika map meninggalkan posisi akhir.
7. Test Case Boundary — Radius dan Koordinat
TC ID	Priority	Precondition	Steps	Expected Result
RW-019	P1	User belum Mock/Pinpoint. Lock berada di dalam radius.	1. Lock di koordinat dekat batas radius tetapi masih di dalam. 2. Geser map. 3. Tap Rewind.	Map kembali ke Lock Location yang tersimpan.
RW-020	P1	User belum Mock/Pinpoint. Lock berada di luar radius.	1. Lock di X. 2. Geser map. 3. Tap Rewind.	Map kembali ke Center Anchor.
RW-021	P1	User belum Mock/Pinpoint. Lock berada tepat di batas radius R.	1. Lock di R. 2. Geser map. 3. Tap Rewind.	.... Tentukan apakah batas radius termasuk di dalam atau di luar radius.
RW-022	P1	Mock/Pinpoint A berada di dalam radius dan Lock B berada di luar radius.	1. Geser map ke C. 2. Tap Rewind.	.... Ikuti keputusan final mengenai prioritas Lock versus radius setelah Mock/Pinpoint.
RW-023	P1	Mock/Pinpoint A berada di luar radius.	1. Lock di B. 2. Geser map ke C. 3. Lakukan Rewind tahap pertama dan kedua.	Urutan tahap tetap mengikuti aturan Rewind. Tahap kedua kembali ke A setelah Unlock jika diperlukan.
RW-024	P1	Mock/Pinpoint A dan Lock B berjarak sangat dekat tetapi tidak identik.	1. Geser map. 2. Lakukan Rewind tiga kali.	Tahap Rewind dibedakan berdasarkan riwayat aksi, bukan hanya kedekatan koordinat.
RW-025	P1	Mock/Pinpoint A dan Lock B memiliki koordinat identik.	1. Geser map. 2. Lakukan Rewind.	Tahap Rewind tetap dibedakan berdasarkan riwayat aksi meskipun koordinat sama.
8. Test Case Interaksi Cepat, Kegagalan, dan Recovery
TC ID	Priority	Precondition	Steps	Expected Result
RW-026	P0	Mock/Pinpoint A dan Lock B tersimpan. Map berada di C.	1. Tap Rewind dua kali dengan cepat.	Sistem memproses kedua tahap secara berurutan atau menolak tap kedua sampai tahap pertama selesai. Target tidak boleh terlewati.
RW-027	P1	Animasi Rewind sedang berlangsung.	1. Tap Rewind berulang kali selama animasi.	Tidak terjadi perpindahan yang tidak konsisten atau state yang rusak.
RW-028	P0	Mock/Pinpoint A dan Lock B tersimpan; status Locked.	1. Simulasikan kegagalan Unlock saat Rewind kedua.	Map tidak berpindah ke A sebelum Unlock berhasil. Error ditangani dan user dapat mencoba kembali.
RW-029	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Simulasikan kegagalan perpindahan map saat Rewind pertama.	Posisi acuan dan tahap Rewind tidak dianggap berhasil sebelum perpindahan terkonfirmasi. User dapat mencoba kembali.
RW-030	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Rewind ke B. 2. Pindah halaman. 3. Kembali ke map. 4. Tap Rewind.	.... Tentukan apakah tahap Rewind dipertahankan setelah navigasi halaman.
RW-031	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Rewind ke B. 2. Tutup aplikasi. 3. Buka kembali. 4. Tap Rewind.	.... Tentukan apakah riwayat Rewind dipertahankan setelah aplikasi dimulai ulang.
RW-032	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Lakukan Rewind pertama. 2. Ubah Mock/Pinpoint ke D. 3. Tap Rewind.	.... Tentukan apakah Mock terbaru menggantikan target lama dan apakah siklus Rewind di-reset.
RW-033	P2	Tidak ada posisi Lock atau Mock/Pinpoint yang valid.	1. Tap Rewind beberapa kali.	Tidak terjadi crash. Sistem menggunakan Center Anchor sesuai aturan fallback.
9. Regression Test
TC ID	Priority	Precondition	Steps	Expected Result
RW-034	P1	User belum Mock/Pinpoint.	1. Geser map. 2. Gunakan Lock/Unlock. 3. Tap Rewind.	Fungsi Lock/Unlock tetap bekerja sesuai desain dan target Rewind tetap benar.
RW-035	P1	User sudah Mock/Pinpoint di A.	1. Geser map ke B. 2. Lakukan aksi map lain tanpa Start Mock ulang.	Mock/Pinpoint Location tetap A.
RW-036	P1	Mock/Pinpoint A dan Lock B tersimpan.	1. Rewind ke B. 2. Geser map ke C lagi. 3. Tap Rewind.	Map kembali ke A, bukan ke posisi C atau posisi map sebelumnya.
RW-037	P1	User sudah menyelesaikan Rewind hingga Center Anchor.	1. Geser map ke C. 2. Tap Rewind.	.... Tentukan apakah tombol tetap disabled atau siklus Rewind dapat di-reset.
RW-038	P2	User melakukan Lock dan Rewind berulang.	1. Periksa marker, radius circle, dan indikator status setelah setiap tahap.	Marker, radius, dan status Locked/Unlocked konsisten dengan posisi serta state aktual.
10. Open Decisions

Keputusan berikut perlu disepakati sebelum seluruh test case dianggap final.

ID	Pertanyaan	Rekomendasi
OD-01	Jika Mock sudah dilakukan dan Lock terbaru berada di luar radius, apakah Rewind pertama menuju Lock atau Center Anchor?	Tentukan aturan eksplisit untuk validitas Lock setelah Mock.
OD-02	Apakah Lock tepat di batas radius dianggap di dalam radius?	Gunakan aturan jarak kurang dari atau sama dengan radius sebagai di dalam radius, jika sesuai dengan spesifikasi produk.
OD-03	Jika Unlock gagal saat Rewind kedua, apakah tahap Rewind tetap tahap kedua?	Ya. Jangan lanjutkan perpindahan sampai Unlock berhasil.
OD-04	Apakah riwayat Rewind tersimpan setelah pindah halaman atau aplikasi ditutup?	Tentukan apakah state hanya berlaku selama sesi map atau harus persisten.
OD-05	Jika user melakukan Start Mock/Pinpoint ulang, apakah urutan Rewind dimulai ulang?	Disarankan ya: simpan Mock terbaru dan evaluasi ulang tahap Rewind berdasarkan state terbaru.
OD-06	Setelah tahap akhir tercapai, user menggeser map dan menekan Rewind lagi.	Disarankan tetap disabled sampai ada aksi baru yang secara eksplisit memulai siklus Rewind baru.
11. Checklist Sign-off DOD
Rewind pertama mengembalikan map ke Lock Location terbaru yang valid.
Rewind berikutnya kembali ke Mock/Pinpoint terakhir jika user sudah melakukan Mock/Pinpoint.
Jika belum Mock/Pinpoint, Rewind berikutnya kembali ke Center Anchor.
Unlock dilakukan sebelum map kembali ke Mock/Pinpoint jika status masih Locked.
Unlock manual tidak menghapus target Mock/Pinpoint.
Lock ulang memperbarui target Rewind pertama.
Perpindahan map biasa tidak mengubah posisi acuan yang tersimpan.
Rewind dinonaktifkan setelah target akhir tercapai.
Tap berulang tidak melewati tahap atau menimbulkan state yang tidak konsisten.
Kegagalan Unlock atau perpindahan map ditangani dengan aman.
Semua open decision yang relevan telah disepakati.
12. Rekomendasi Eksekusi QA
Jalankan RW-001 sampai RW-010 sebagai smoke test untuk alur utama.
Jalankan RW-015 sampai RW-025 untuk validasi radius dan koordinat.
Jalankan RW-026 sampai RW-033 untuk menguji robustness dan recovery.
Jalankan RW-034 sampai RW-038 untuk regression test.
Jangan menyatakan DOD selesai sebelum seluruh test case P0 lulus dan open decision yang relevan telah disepakati.