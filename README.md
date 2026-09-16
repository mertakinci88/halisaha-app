# Halı Saha Yönetim Sistemi

Monorepo: backend ve frontend uygulamaları ayrı klasörlerde, tek repoda.

- [`halisaha-be`](./halisaha-be) — Spring Boot 4 (Java 21) REST API. Kurulum
  ve deploy detayları için `halisaha-be/deploy/README.md`.
- [`halisaha-fe`](./halisaha-fe) — React + Vite + Tailwind CSS frontend.
- [`project-scope.md`](./project-scope.md) — proje kapsamı ve veri modeli.
- [`project-tech-stack.md`](./project-tech-stack.md) — kullanılan teknoloji
  yığını.

## CI/CD

`.github/workflows/be-ci-cd.yml` yalnızca `halisaha-be/**` altında değişiklik
olduğunda tetiklenir: build & test → Docker image (GHCR) → AWS EC2'ye deploy.
Frontend için ayrı bir workflow henüz eklenmedi.
