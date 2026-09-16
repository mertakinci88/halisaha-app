# Teknoloji Yığını

## Backend
- Java 21
- Spring Boot 4 (mevcut projede kurulu sürüm, ör. 4.1.1)
- Spring Data JPA
- Spring Security + JWT (kullanıcı adı/şifre ile login, JWT tabanlı stateless authentication, logout)
- Gradle (build aracı)
- PostgreSQL (veritabanı)
- Flyway (veritabanı şema migration aracı; `spring.jpa.hibernate.ddl-auto` yerine versiyonlanmış SQL migration dosyaları kullanılır: `src/main/resources/db/migration`)
- Docker Compose (yerel geliştirmede PostgreSQL ve backend uygulamasını ayağa kaldırmak için)

## Frontend
- React
- Tailwind CSS v4
- shadcn/ui (UI komponent kütüphanesi)
- Axios (API çağrımları için)

## Notlar
- Bu dosya proje boyunca referans alınacak; yeni bir teknoloji/kütüphane eklenmesi gerektiğinde önce burası güncellenecek.
- Backend kodlaması bu yığına uygun şekilde ilerleyecek (örn. Spring Boot 4'e özgü starter isimlendirmeleri: `spring-boot-starter-webmvc`, `spring-boot-starter-security-test` vb.).
