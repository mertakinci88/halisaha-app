import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { rezervasyonService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import {
  ODEME_DURUMU_LABEL,
  ODEME_YONTEMI_LABEL,
  kisaTarih,
  saatOf,
  sureSaat,
  tl,
} from '@/lib/format';

export default function RezervasyonDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [rez, setRez] = useState(null);
  const [urunler, setUrunler] = useState([]);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(false);
  const [iptalOnay, setIptalOnay] = useState(false);

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    Promise.all([rezervasyonService.get(id), urunService.list()])
      .then(([r, u]) => {
        setRez(r);
        setUrunler(u);
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  const urunEkle = async (urunId) => {
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunEkle(id, urunId, 1));
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const urunCikar = async (rezervasyonUrunId) => {
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunCikar(id, rezervasyonUrunId));
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const iptalEt = async () => {
    setIptalOnay(false);
    setIslemde(true);
    try {
      await rezervasyonService.iptal(id);
      navigate('/takvim');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const odemeTamamla = async () => {
    if (!rez) return;
    setIslemde(true);
    try {
      const guncel = await rezervasyonService.update(id, {
        sahaId: rez.sahaId,
        adSoyad: rez.adSoyad,
        baslangicTarih: rez.baslangicTarih,
        bitisTarih: rez.bitisTarih,
        telNo: rez.telNo,
        odemeYontemi: rez.odemeYontemi || 'NAKIT',
        odemeDurumu: 'ODENDI',
      });
      setRez(guncel);
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!rez) return <Hata mesaj={hata || 'Rezervasyon bulunamadı.'} onTekrar={yukle} />;

  const sure = sureSaat(rez.baslangicTarih, rez.bitisTarih);
  const urunToplam = (rez.urunler || []).reduce((t, s) => t + Number(s.araToplam || 0), 0);
  const sahaTutar = Number(rez.odenecekTutar || 0) - urunToplam;

  const alanlar = [
    { etiket: 'Saha', deger: rez.sahaAd },
    { etiket: 'Tarih', deger: kisaTarih(rez.baslangicTarih) },
    { etiket: 'Saat', deger: saatOf(rez.baslangicTarih) + ' – ' + saatOf(rez.bitisTarih) },
    { etiket: 'Telefon', deger: rez.telNo },
    { etiket: 'Ödeme yöntemi', deger: ODEME_YONTEMI_LABEL[rez.odemeYontemi] || '—' },
    { etiket: 'Ödeme durumu', deger: ODEME_DURUMU_LABEL[rez.odemeDurumu] || '—' },
  ];

  return (
    <div>
      <PageHeader kicker="Rezervasyon" baslik="Rezervasyon detayı" />
      <Hata mesaj={hata} />

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]">
        <div className="grid content-start gap-5">
          <Card>
            <div className="flex flex-wrap items-baseline gap-2.5">
              <h4 className="text-[22px]">{rez.adSoyad}</h4>
              <Tag tone={rez.durum === 'AKTIF' ? 'accent' : 'outline'}>
                {rez.durum === 'AKTIF' ? 'Aktif' : 'İptal'}
              </Tag>
            </div>
            <div className="grid gap-x-4 gap-y-3 [grid-template-columns:repeat(auto-fit,minmax(130px,1fr))]">
              {alanlar.map((a) => (
                <div key={a.etiket}>
                  <div className="text-[11.5px] uppercase tracking-[0.1em] text-ink/55">{a.etiket}</div>
                  <div className="text-[15.5px]">{a.deger}</div>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <h4 className="text-[22px]">Kafeterya · ürün ekle</h4>
            <p className="m-0 text-[13.5px] text-ink/55">
              Seans sırasında satılan ürünleri ekleyin; tutar rezervasyona işlenir.
            </p>
            <Table>
              <thead>
                <tr>
                  <Th>Ürün</Th>
                  <Th className="text-right">Fiyat</Th>
                  <Th className="text-right">Stok</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {urunler.map((u) => (
                  <Tr key={u.id}>
                    <Td>{u.ad}</Td>
                    <Td className="text-right tabular-nums">{tl(u.fiyat)}</Td>
                    <Td className="text-right tabular-nums text-ink/55">{u.adet}</Td>
                    <Td className="text-right">
                      <Button variant="secondary" size="sm" disabled={islemde} onClick={() => urunEkle(u.id)}>
                        + Ekle
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Card>
        </div>

        <Card className="gap-3">
          <h4 className="text-[22px]">Ödeme özeti</h4>
          <div className="flex justify-between text-[15.5px]">
            <span className="text-ink/55">
              {rez.sahaAd} · {sure} saat
            </span>
            <span className="tabular-nums">{tl(sahaTutar)}</span>
          </div>

          {(rez.urunler || []).map((s) => (
            <div key={s.id} className="flex items-center justify-between gap-2 text-[15.5px]">
              <span className="text-ink/55">
                {s.urunAd} × {s.miktar}
              </span>
              <span className="flex items-center gap-2">
                <span className="tabular-nums">{tl(s.araToplam)}</span>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={islemde}
                  onClick={() => urunCikar(s.id)}
                >
                  <X size={14} strokeWidth={1.75} />
                  <span>Çıkar</span>
                </Button>
              </span>
            </div>
          ))}

          <div className="h-px bg-divider" />
          <div className="flex items-baseline justify-between">
            <span className="font-heading text-[18px] font-semibold">Toplam</span>
            <span className="font-heading text-[34px] font-semibold tabular-nums">{tl(rez.odenecekTutar)}</span>
          </div>

          <div className="mt-1.5 grid gap-2">
            <Button
              variant="primary"
              disabled={islemde || rez.odemeDurumu === 'ODENDI'}
              onClick={odemeTamamla}
            >
              {rez.odemeDurumu === 'ODENDI' ? 'Ödeme alındı' : 'Ödemeyi tamamla'}
            </Button>
            <Button
              variant="secondary"
              disabled={islemde || rez.durum !== 'AKTIF'}
              onClick={() => setIptalOnay(true)}
            >
              Rezervasyonu iptal et
            </Button>
            <Button variant="ghost" onClick={() => navigate('/takvim')}>
              Takvime dön
            </Button>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={iptalOnay}
        title="Rezervasyon iptal edilsin mi?"
        description="Bu işlem geri alınamaz."
        isim={rez.adSoyad}
        confirmLabel="İptal et"
        onConfirm={iptalEt}
        onCancel={() => setIptalOnay(false)}
      />
    </div>
  );
}
