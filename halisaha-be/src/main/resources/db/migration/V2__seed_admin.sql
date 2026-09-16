-- İlk giriş için varsayılan admin kullanıcısı: admin / admin123
-- Üretim ortamına geçmeden önce bu şifre mutlaka değiştirilmelidir.
INSERT INTO kullanici (kullanici_adi, sifre, ad_soyad, rol, durum, kayit_tarih)
VALUES ('admin', '$2b$10$PJmIy52AiowYZQoIv.58e.YwRTy55omXC5p5yschjzeHK0f50vIf2', 'Sistem Yöneticisi', 'ADMIN', 'AKTIF', now());
