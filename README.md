# Halı Saha Yönetim Sistemi

Monorepo: backend ve frontend uygulamaları ayrı klasörlerde, tek repoda.

- [`halisaha-be`](./halisaha-be) — Spring Boot 4 (Java 21) REST API. Kurulum
  ve deploy detayları için `halisaha-be/deploy/README.md`.
- [`halisaha-fe`](./halisaha-fe) — React + Vite + Tailwind CSS frontend.
- [`project-scope.md`](./project-scope.md) — proje kapsamı ve veri modeli.
- [`project-tech-stack.md`](./project-tech-stack.md) — kullanılan teknoloji
  yığını.

## CI/CD

`.github/workflows/be-ci-cd.yml` ve `fe-ci-cd.yml`, sırasıyla yalnızca
`halisaha-be/**` ve `halisaha-fe/**` altında değişiklik olduğunda tetiklenir:
build & test → Docker image (GHCR) → Hostinger VPS'e (Docker + Traefik)
deploy. Detaylar için `halisaha-be/deploy/README.md`.
