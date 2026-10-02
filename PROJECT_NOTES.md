# TeamUP - Catatan Project

Terakhir diupdate: 30 Sep 2026 (GUI custom v1)

## Status singkat

- Semua fungsi **sudah jalan normal** (menurut hasil tes user).
- Build lewat GitHub Actions (Gradle) sukses setelah fix `ChatPrompt.java`.
- **Yang masih kurang: tampilan GUI.** Masih chest biasa, item polos + glass pane abu-abu. Lanjut besok fokus ke sini.

## Info teknis

| Item | Isi |
|---|---|
| Nama plugin | TeamUP |
| Platform | Paper 1.20.1 |
| Java | 21 |
| Build | Gradle 8.8 via GitHub Actions |
| Package | `id.teamup` |
| Main class | `id.teamup.TeamUPPlugin` |
| Data | `plugins/TeamUP/data.yml` (YAML) |
| Config | `plugins/TeamUP/config.yml` |

Layout repo GitHub itu **flat**: semua `.java` di root, workflow yang nyusun ke folder package (sama kayak TxAdminMC). Jadi file baru cukup ditaruh di root dan **package-nya harus benar** di baris `package ...;`.

## Konsep (keputusan yang sudah disepakati)

- Team **cuma dibuat admin**. Player yang request jadi **Ketua**.
- 5 rank default: KETUA, WAKIL KETUA, MEMBER, SOLDIER, HANGEROUND.
- Nama rank bisa diubah. Admin ngasih izin per team, lalu Ketua bisa ganti nama rank sendiri.
- Yang boleh naik/turun rank: **Ketua saja**.
- Ga ada friendly fire antar anggota team (termasuk panah).
- Invite lewat **GUI** (kepala player), bukan command. Target dapat chat `[TERIMA] [CANCEL]`, target ga perlu permission.
- Team chat: `/tc pesan` -> `【ALLSTAR】NAMA: pesan`, cuma ke anggota team.
- Tanda teman: prefix `[TEAM]` hijau di atas kepala + tab list.
- Warn team ala "warn server":
  - Admin pilih team -> level 1/2/3 -> pilih pelaku (bisa banyak, online/offline) -> ketik alasan.
  - Pelaku kena ban: level 1 = 1 hari, level 2 = 7 hari, level 3 = 14 hari.
  - Team dapat +1 strike, notif ke team chat.
  - **Level 3: team dibubarkan paksa** (hapus permanen).
- Bikin team baru setelah dibubarkan paksa: dari nol, tapi **maksimal 3 orang yang sama** dari team lama.

## Permission per rank (default di config.yml)

| Rank | Buka menu | Add/kick member | Lihat online | Team chat | Ubah rank |
|---|---|---|---|---|---|
| KETUA | ya | ya | ya | ya | ya |
| WAKIL KETUA | ya | tidak | ya | ya | tidak |
| MEMBER | tidak | tidak | tidak | ya | tidak |
| SOLDIER | tidak | tidak | tidak | tidak | tidak |
| HANGEROUND | tidak | tidak | tidak | tidak | tidak |

Izin "Ubah rank" hardcoded cuma untuk Ketua. Sisanya bisa diedit di `config.yml` bagian `ranks.<RANK>.permissions`.

## Command

| Command | Alias | Fungsi |
|---|---|---|
| `/teamup` | `/tu`, `/team` | Buka menu team |
| `/teamup accept` / `deny` | - | Terima / tolak undangan (dari tombol chat) |
| `/tc <pesan>` | - | Team chat |
| `/teamupadmin` | `/tua` | Menu admin (permission `teamup.admin`, default op) |
| `/tua create <team> <player>` | - | Bikin team lewat command |
| `/tua delete <team>` | - | Hapus team |
| `/tua list` | - | Daftar team |

## Struktur menu (GUI)

```
/teamup  -> TeamMenu
  |- Anggota        -> MembersMenu   (kiri: naik rank, kanan: turun rank, shift+kanan: kick)
  |- Member Online  -> OnlineMenu
  |- Tambah Member  -> AddMemberMenu (klik kepala = kirim undangan)
  |- Ganti Nama Rank-> RankNamesMenu (input lewat chat)

/tua  -> AdminMenu
  |- Add Team   -> CreateTeamPickMenu (pilih kepala, lalu ketik nama team di chat)
  |- List Team  -> TeamListMenu
  |     kiri: buka sebagai ketua (TeamMenu mode admin)
  |     kanan: warn | shift+kanan: hapus (ConfirmMenu)
  |- Warn Team  -> TeamListMenu (mode warn) -> WarnLevelMenu -> WarnMemberMenu
```

Mode admin di `TeamMenu` nambah tombol: Warn Team, toggle izin ganti nama rank, Hapus Team.

## Peta file

| File | Fungsi |
|---|---|
| `TeamUPPlugin` | Main class, daftar listener & command |
| `Team`, `Rank`, `TeamPerm` | Model data |
| `TeamManager` | Inti: buat/hapus team, member, rank, warn+ban, simpan `data.yml` |
| `InviteManager` | Undangan + tombol chat + expire |
| `ChatPrompt` | Minta input teks dari player lewat chat (dipakai GUI) |
| `NametagManager` | Prefix `[TEAM]` lewat scoreboard team |
| `Menu`, `PagedMenu`, `MenuListener` | Kerangka GUI (holder-based, halaman 45 slot) |
| `TeamMenu`, `MembersMenu`, `OnlineMenu`, `AddMemberMenu`, `RankNamesMenu`, `ConfirmMenu` | Menu team |
| `AdminMenu`, `TeamListMenu`, `CreateTeamPickMenu`, `WarnLevelMenu`, `WarnMemberMenu` | Menu admin |
| `TeamUPCommand`, `TeamChatCommand`, `AdminCommand` | Command |
| `GameListener` | Chat prompt, friendly fire, join/quit |
| `Items`, `Msg` | Helper item & pesan (kode warna `&`) |

## Config penting (config.yml)

- `max-members` (default 20, batas keras 44)
- `invite-expire-seconds` (60)
- `friendly-fire` (false)
- `nametag.enabled` (true)
- `warn-ban-days` ([1, 7, 14])
- `recreate-max-same-members` (3)
- `ranks.<RANK>.name` dan `.permissions`

## Catatan teknis

- Ban pakai **nama player** (`BanList.Type.NAME`), cocok untuk server cracked. Kalau server online-mode, ganti ke ban by UUID/profile.
- `data.yml` disimpan langsung (sync) tiap ada perubahan. Aman untuk data kecil.
- Riwayat team yang dibubarkan paksa disimpan di `data.yml` bagian `dissolved` (buat aturan max 3 orang sama).
- Hati-hati `Bukkit.getScheduler().runTask(...)`: jangan pakai method reference (`p::closeInventory`), pakai lambda `() -> ...` (bikin compiler ambigu).

## TODO besok: perbaiki tampilan GUI

Saat ini: chest biasa, item polos, semua menu mirip-mirip.

Ide, urut dari yang paling gampang (tanpa resource pack dulu, karena main dari HP/launcher):

1. **Border dan dekorasi**: pola frame pakai glass pane berwarna (misal hitam + aksen merah/biru), bukan abu-abu polos semua.
2. **Judul menu berwarna** dengan gradient/format lebih rapi, ikon unicode.
3. **Susun layout** biar tombol simetris dan tiap menu punya identitas (warna beda: team = biru, admin = merah, warn = oranye).
4. **Item lebih informatif**: lore rapi (garis pemisah, ikon status online/offline, ringkasan rank), enchant glow buat item aktif.
5. **Sound effect** saat klik, buka menu, sukses, error.
6. **Tombol navigasi konsisten** (kembali, halaman, tutup) di posisi yang sama di semua menu.
7. **Opsional lanjutan**: custom model data + resource pack (texture background menu, ikon kustom) kalau server sudah mau pakai resource pack.

Kalau mau referensi tampilan, kirim screenshot GUI plugin lain yang disukai (atau screenshot TxAdminMC), nanti gw samain gayanya.

## Cara lanjut besok

1. Upload repo / file terbaru ke chat baru (atau kirim ulang file `.java` yang mau diubah).
2. Bilang: "lanjut TeamUP, fokus GUI", terus tempel/kirim file ini.
3. Setelah gw ubah, timpa file di repo, push, ambil jar dari Actions -> Artifacts.

## Update: GUI custom (resource pack)

- Menu sekarang pakai **background gambar custom** lewat trik font di judul inventory (bukan chest abu-abu lagi): panel gelap, header + garis aksen warna tema, sel slot, plat tombol berwarna di slot yang bisa diklik.
- Tombol berlabel: pakai `btn(slot, Btn.X, aksi)` di menu (lebar 2-4 slot, teks sudah ada di gambar). `set(slot,item,aksi)` = plat slot biasa untuk item/kepala. Halaman list ditulis di judul, nav di baris bawah.
- Tema: TEAM = biru, ADMIN = merah, WARN = oranye (`GuiTheme`). Tiap menu override `theme()`.
- File baru: `GuiTheme`, `GuiFont` (susun judul + geser kursor), `Btn` (daftar tombol berlabel; teksnya digambar di pack, urutan enum harus sama dengan pack). Pack = `TeamUP-GUI-pack.zip` (dipasang manual, TIDAK ada di repo).
- `Menu` diubah: judul dibuat dinamis (plat tombol mengikuti slot yang punya aksi), `refresh()` bikin ulang judul kalau posisi tombol berubah, suara klik/buka.
- GUI **selalu** pakai tampilan custom. Tidak ada mode auto, toggle, atau fallback. Pack dipasang manual oleh tiap player (taruh `TeamUP-GUI-pack.zip` di folder resourcepacks, aktifkan di Options > Resource Packs).
- Tombol baru/ganti label: butuh generator pack (tidak ada di repo) supaya pack dan `Btn.java` tetap sinkron.
- Kalau background geser 1 pixel vertikal, ubah `ASCENT` di gen_pack.py (13 -> 12/14) lalu generate ulang.
- Belum dikerjakan: ikon tombol custom (custom model data), lore item yang lebih rapi.

## Update: HUD pojok kanan atas (bossbar + pack)

- `HudManager`: 2 bossbar per player (warna WHITE, progress 0). Baris 1 = tanggal + jam (WIB), baris 2 = `NAMA TEAM - rank` atau `no fraksi`.
- Plat gambar digambar lewat font pack (glyph `\uE900-\uE903` di `teamup:gui`), digeser ke kanan pakai karakter spasi pack. Lebar teks dihitung dari tabel lebar font default.
- Pack juga menimpa `assets/minecraft/textures/gui/bars.png`: baris warna WHITE dibuat transparan (bar HUD tidak kelihatan), warna lain (pink = ender dragon, dll) digambar ulang sederhana.
- Posisi dipengaruhi lebar layar dalam pixel GUI: `hud.gui-width` (default 780 = layar 2340 px, GUI scale 3) dan `hud.margin`. Atur live: `/tua hud <lebar> [margin]`.
- Player tanpa pack: `/teamup hud` buat mematikan HUD (disimpan per player).
- Scoreboard HUD versi sebelumnya dibuang (tidak dipakai lagi).

## Update 1 Okt (2)

- Warn level 3 tidak lagi membubarkan team otomatis: pelaku di-ban sesuai `warn-ban-days` + 1 strike + notif ke team chat. Bubar atau tidak urusan admin.
- Team yang dihapus admin (command, menu list, mode admin) sekarang dicatat sebagai bubar, jadi aturan recreate berlaku.
- Config baru: `recreate.enabled` (true/false) dan `recreate.max-same-members` (default 3). Kunci lama `recreate-max-same-members` masih terbaca kalau yang baru tidak ada.
- HUD: pakai font sendiri `teamup:hud` (ascii.png vanilla + plat, diturunkan 6 px GUI dari posisi bawaan). Default `hud.gui-width` jadi 750 (viewport game 2250 px / GUI scale 3, bukan 2340).

## Update 1 Okt (3) - finishing

- Audit semua file: fix kode warna "Hitam" di ColorMenu (&f -> &0), nama "?" tidak lagi didaftarkan ke scoreboard team (bisa bentrok antar team), nama rank default (KETUA / WAKIL KETUA) kalau config tidak punya kuncinya.
- HUD turun 9 pixel GUI dari posisi bawaan bossbar (hud.json: ascent plat 0, teks -2).
- Banner startup di console (build by kazett, dll). plugin.yml: authors + website.
- Catatan: alias `/team` di plugin.yml kalah dari command vanilla `/team`, pakai `/teamup` atau `/tu`.

## Update 1 Okt (4)

- Teks sambutan (banner) juga tampil di chat saat player join (`welcome.enabled` di config.yml, link GitHub bisa diklik).
- plugin.yml: author, website (GitHub), dan description berisi kredit, supaya muncul saat klik nama plugin di `/plugins` (info detail lewat `/version TeamUP`).
- File `LICENSE` ditambahkan (custom: boleh dipakai & diubah di server sendiri, dilarang redistribusi/jual/hapus kredit).

## Update 1 Okt (5)

- Klik TeamUP di `/plugins` -> server menjalankan `/version TeamUP`; GameListener mencegat perintah itu (`/version|/ver|/about TeamUP`, player & console) dan menampilkan banner kazett + link GitHub. `/plugins` sendiri tidak diubah.

## Update 1 Okt (6)

- `/tua reload` dan `/teamup reload` (permission `teamup.admin`): muat ulang config tanpa restart (HUD ikut di-restart, jadi hud.enabled/logo/warna langsung berlaku).
- `/tua ...` admin only (teamup.admin). `/teamup hud` = toggle HUD untuk player sendiri; kalau `hud.enabled: false` di config sekarang muncul pesan jelas, bukan "dinyalakan".
- Klik TeamUP di `/plugins` -> `/version TeamUP` dicegat -> cuma link GitHub kazett.
- HUD baru: logo opsional (`hud.logo.*`), ikon kalender di plat tanggal, ikon shield/sword di plat fraksi. Plat di pack berwarna abu-abu lalu di-tint dari config (`hud.date.color`, `hud.faction.color: team|#hex`), ikon tidak ikut berubah warna.
- Pack: file baru `logo.png`, `icon_calendar.png`, `icon_shield.png`, `icon_sword.png`; `hud_accent.png` dihapus; glyph di `font/hud.json` (logo \uE910, ikon \uE920-\uE922).

## Update 2 Okt

- Logo HUD sekarang KOTAK: config `hud.logo.size` (default 16) menggantikan `width`/`slots`; jumlah baris dihitung otomatis. `logo.png` harus persegi; `height` provider logo di `font/hud.json` harus sama dengan `size`.
- Nama team maksimal 20 karakter, bisa diubah lewat `team-name-max-length` (2-32). Nama scoreboard team untuk nama panjang memakai hash supaya tidak bentrok.
- Versi 1.1.0 (jar `TeamUP-1.1.0.jar`); banner console menampilkan versi.

## Update 2 Okt (2): layout logo besar

- Layout HUD baru: logo kotak BESAR di kanan (`hud.logo.size`, pilihan 16/24/32/40/48/56/64, default 32), plat tanggal + fraksi di kirinya, rata kanan satu sama lain, tinggi dua plat = tinggi logo 32. Logo digambar di judul bossbar baris tanggal (tidak butuh bossbar tambahan).
- Ukuran dipilih lewat config tanpa edit pack: `font/hud.json` punya 7 varian glyph logo (\uE910..\uE916) yang semuanya memakai `logo.png`.
- `hud.logo.offset-x` untuk menggeser logo. Lebar glyph logo dihitung dari kolom terakhir yang tidak transparan: logo.png bawaan punya 1 pixel hampir transparan di pojok kanan bawah supaya lebarnya penuh.

## Update 2 Okt (3): logo di atas, HUD di bawah

- Layout final: logo kotak di paling atas (rata kanan), plat tanggal + fraksi tepat di bawahnya, rata kanan juga. Logo digambar di bossbar baris 0; bossbar kosong jadi spacer; plat ada di bar setelahnya. Jumlah bar = ceil((size+3)/19) + 2.
- `hud.logo.size`: pilihan 16 / 35 / 54 / 73 (= 19n-3, supaya pas menempel ke plat). Default 54. Dibulatkan ke yang terdekat.
- Nama team: maks 25 (`team-name-max-length`), boleh pakai spasi (`team-name-allow-spaces`, contoh "BRUTAL OF ANARCHY"). `/tua create <nama team boleh spasi> <ketua>`, `/tua delete <nama team>`. Nama scoreboard untuk nama panjang/bersepasi pakai hash.
- Catatan: bossbar vanilla hanya digambar sampai 1/3 tinggi layar, jadi pada GUI scale besar plat yang jauh di bawah bisa tidak tampil; kalau begitu kecilkan `hud.logo.size`.
- logo.png bawaan sekarang 128x128 halus (perisai biru + T).

## Update 2 Okt (4)

- Default logo 73 (dari 54) dan `hud.margin` default 12 (dari 6) supaya HUD agak ke kiri. Config server yang sudah ada tidak ikut berubah: pakai `/tua hud 750 12` dan ubah `hud.logo.size` di plugins/TeamUP/config.yml lalu `/tua reload`.

## Update 2 Okt (5): HUD jadi SATU bossbar

- Masalah: logo besar butuh banyak bossbar (6) -> bossbar lain (ender dragon) terdorong ke tengah layar dan bahkan tidak tergambar (vanilla berhenti menggambar bossbar di 1/3 tinggi layar).
- Solusi: seluruh HUD (logo + 2 plat) digambar dalam 1 bossbar. Horizontal: total lebar judul dipaksa = 2 x tengah layar (hud.gui-width / 2), jadi judul mulai di x = 0 dan posisi kursor = posisi layar. Vertikal: font pack `teamup:hud_<offset>` menurunkan gambar sebesar offset pixel (ascent negatif); offset = tinggi logo + 3 (tanggal) dan +19 lagi (fraksi).
- Ukuran logo (`hud.logo.size`): 16, 24, ..., 96 (kelipatan 8), default 72. Pack punya 23 font hud_<offset>.json (otomatis dibuat; kalau menambah ukuran logo, buat font offset baru: S+3 dan S+22).
- Bossbar lain (naga, dll) sekarang tampil tepat di bawah slot HUD (hanya bergeser 1 baris).

## Update 2 Okt (6)

- Versi plugin 1.2.0, pack diganti nama `TeamUP-GUI-pack-v3.zip` (deskripsi "v3") supaya jar/pack lama tidak tertukar. HUD v3 butuh jar 1.2.0 + pack v3 (font hud_<offset>.json); pack lama + jar baru = kotak-kotak di atas layar.
- `/tua hud check`: kirim baris chat yang memakai font pack (plat + ikon). Kotak-kotak = pack belum terpasang / lama.
