#!/usr/bin/env bash
# EC2 instance'ında (Ubuntu 24.04 LTS) bir kez, elle çalıştırılır.
# Amaç: Docker + Docker Compose plugin kurmak ve uygulama dizinini hazırlamak.
#
# Kullanım:
#   scp -i key.pem deploy/setup-ec2.sh ubuntu@<EC2_IP>:~/
#   ssh -i key.pem ubuntu@<EC2_IP>
#   chmod +x setup-ec2.sh && ./setup-ec2.sh

set -euo pipefail

echo "==> Paket listesi güncelleniyor..."
sudo apt-get update -y

echo "==> Docker kuruluyor..."
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> Mevcut kullanıcı docker grubuna ekleniyor (sudo'suz docker için, yeniden login gerekir)..."
sudo usermod -aG docker "$USER"

echo "==> Uygulama dizini oluşturuluyor: /opt/halisaha-be"
sudo mkdir -p /opt/halisaha-be
sudo chown "$USER":"$USER" /opt/halisaha-be

echo ""
echo "==> Kurulum tamamlandı."
echo "Sıradaki adımlar:"
echo "  1) Bu oturumdan çıkıp tekrar ssh ile bağlanın (docker grubu üyeliğinin geçerli olması için)."
echo "  2) deploy/.env.example dosyasını referans alarak /opt/halisaha-be/.env dosyasını elle oluşturun."
echo "  3) GitHub Actions ilk 'main' push'unda otomatik deploy edecek."
