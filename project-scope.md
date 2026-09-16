# Halı Saha Yönetim Sistemi

## Problem
Bir halı saha işletmesi var, yazılımları olmadığı için halısahanın rezervasyon, kafeterya, öğrenci vb. bilgilerini excelde tutmak zorunda kalıyorlar.
Bir süre sonra veri büyüyünce takip etmesi zor oluyor.

## Çözüm
Bir halı saha yönetim sistemi yazılımı geliştir. Bu yazılımda saatlik rezervasyon kayıtları alabilmeliyim, birden fazla saha olursa sahaları yönetebilmeliyim,
satış yapılan kafeterya ürünlerini yönetebilmeli ve rezervasyon bilgisine ekleyerek toplam ödenecek tutarı görebilmeliyim, öğrenci bilgilerini ve ödeme bilgilerini yönetebilmeliyim. 

## Kullanıcı
Halı saha işletmesinde çalışan kişiler. Sisteme kullanıcı adı/şifre ile giriş yaparlar (JWT tabanlı login/logout).

## Özellikler
- Kullanıcılar sisteme kullanıcı adı ve şifre ile giriş yapabilmeli (login), oturumu sonlandırabilmeli (logout). Kimliği doğrulanmamış istekler API'ye erişememeli.
- Rezervasyonları ekleyebileceğim bir sayfa olmalı. Önce saha seçimi yapılıp o saha için rezervasyon bilgisini almak için detay ekran açılmalı.
- Rezervasyonlar iptal edilebilmeli. Dolu ve boş olan saatler belirgin olmalı.
- Öğrencileri kayıt edebileceğim ve listeleyebileceğim ayrı sayfalar olmalı. Kayıt ekranında detay bilgileri alınıp kaydedilmeli, listeleme ekranında tabloda listelenmeli.
- Grup kayıt edebileceğim ve listeleyebileceğim ayrı sayfalar olmalı. Kayıt ekranında detay bilgileri alınıp kaydedilmeli, listeleme ekranında tabloda listelenmeli.
- Saha kayıt edebileceğim ve listeleyebileceğim ayrı sayfalar olmalı. Kayıt ekranında detay bilgileri alınıp kaydedilmeli, listeleme ekranında tabloda listelenmeli.
- Kafeteryada satılan ürünleri tutar ve miktar bilgisiyle yönetebileceğim bir ekran olmalı. Tüm ürünlerin listendiği ayrı bir ekranda olmalı.

## Veri
Kullanici:
    - id
    - kullanici_adi
    - sifre (hash'lenmiş)
    - ad_soyad
    - rol
    - durum
    - kayit_tarih

Saha:
    - id
    - ad
    - durum
    - saatlik_ucret

Rezervasyon:
    - id
    - saha_id
    - ad_soyad
    - baslangic_tarih (gün ay yıl saat)
    - bitis_tarih (gün ay yıl saat)
    - durum
    - tel_no
    - odeme_yontemi
    - odeme_durumu
    - odenecek_tutar

Öğrenci:
    - id
    - grup_id
    - ad_soyad
    - dogum_tarih
    - veli_ad_soyad
    - veli_tel_no
    - durum
    - antrenman_gun_sayisi
    - aylik_aidat
    - odeme_durumu
    - odeme_plani
    - odenen_donem
    - odeme_tarihi
    - sonraki_odeme_tarih
    - odenen_tutar
    - odeme_sekli
    - kayit_tarih
    - not

Grup
    - id
    - ad

Ürün
    - id
    - ad
    - adet
    - fiyat

RezervasyonUrun
    - id
    - rezervasyon_id
    - urun_id
    - miktar

