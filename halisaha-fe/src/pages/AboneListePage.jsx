import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil, Plus } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import { aboneService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import { kisaTarih, telGoster, tl, tutarSinirla } from '@/lib/format';
import { gunAdi, saatAraligi } from '@/lib/abone';

/** Abonelik durumu etiketi: Aktif / İptal / Süresi doldu */
export function AboneDurumEtiketi({ abone }) {
  if (abone.durum === 'IPTAL') return <Tag tone="danger">İptal</Tag>;
  if (abone.suresiDoldu) return <Tag tone="warning">Süresi doldu</Tag>;
  return <Tag tone="accent">Aktif</Tag>;
}

export default function AboneListePage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [hata, setHata] = useHata();
  const [durum, setDurum] = useState('AKTIF'); // AKTIF | IPTAL | '' (tümü)
  const [arama, setArama] = useState('');
  const [aramaAktif, setAramaAktif] = useState('');
  const [liste, setListe] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [standart, setStandart] = useState(null); // kayıtlı standart ücret (null = tanımsız)
  const [ucretDuzen, setUcretDuzen] = useState(null); // null = düzenlenmiyor
  const [ucretKaydediyor, setUcretKaydediyor] = useState(false);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    aboneService
      .list({ durum: durum || undefined, q: aramaAktif || undefined })
      .then(setListe)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, [durum, aramaAktif]);

  useEffect(() => {
    aboneService
      .ayar()
      .then((a) => setStandart(a.standartUcret))
      .catch(() => {});
  }, []);

  // Arama kutusu yazıldıkça kısa gecikmeyle uygulanır
  useEffect(() => {
    const t = setTimeout(() => setAramaAktif(arama.trim()), 300);
    return () => clearTimeout(t);
  }, [arama]);

  const ucretKaydet = async () => {
    const tutar = Number(String(ucretDuzen).replace(',', '.'));
    if (!(tutar >= 0)) return;
    setUcretKaydediyor(true);
    try {
      const a = await aboneService.ayarKaydet({ standartUcret: tutar });
      setStandart(a.standartUcret);
      setUcretDuzen(null);
      toast.basari('Standart abonelik ücreti kaydedildi.');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setUcretKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Abonelik" baslik="Aboneler" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="mb-5 gap-2">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <div>
            <div className="text-[11.5px] uppercase tracking-[0.1em] text-accent-700">Standart abonelik ücreti</div>
            {ucretDuzen === null ? (
              <div className="font-heading text-[28px] leading-tight">
                {standart === null ? <span className="text-[20px] text-ink/55">Tanımlı değil</span> : tl(standart)}
              </div>
            ) : (
              <div className="mt-1 flex items-center gap-2">
                <div className="relative">
                  <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/55">₺</span>
                  <Input
                    type="number"
                    min="0"
                    step="1"
                    inputMode="decimal"
                    autoFocus
                    value={ucretDuzen}
                    onChange={(e) => setUcretDuzen(tutarSinirla(e.target.value, 10000000))}
                    className="w-40 pl-7"
                    aria-label="Standart abonelik ücreti"
                  />
                </div>
                <Button variant="primary" disabled={ucretKaydediyor || ucretDuzen === ''} onClick={ucretKaydet}>Kaydet</Button>
                <Button variant="danger" onClick={() => setUcretDuzen(null)}>Vazgeç</Button>
              </div>
            )}
          </div>
          {isAdmin && ucretDuzen === null ? (
            <Button variant="secondary" size="sm" onClick={() => setUcretDuzen(standart === null ? '' : String(standart))}>
              <Pencil size={14} strokeWidth={1.5} />
              <span>Değiştir</span>
            </Button>
          ) : null}
        </div>
        <p className="m-0 text-[13px] text-ink/55">
          Yeni abonelik kaydında ücret bu tutardan gelir; kayıt sırasında elle değiştirilebilir.
          {!isAdmin ? ' Standart ücreti yalnızca yönetici değiştirebilir.' : ''}
        </p>
      </Card>

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <Seg>
            <SegOpt active={durum === 'AKTIF'} onClick={() => setDurum('AKTIF')}>Aktif</SegOpt>
            <SegOpt active={durum === 'IPTAL'} onClick={() => setDurum('IPTAL')}>İptal</SegOpt>
            <SegOpt active={durum === ''} onClick={() => setDurum('')}>Tümü</SegOpt>
          </Seg>
          <Input
            type="search"
            placeholder="Ad soyad ya da telefon ara"
            value={arama}
            onChange={(e) => setArama(e.target.value)}
            className="max-w-[300px]"
            aria-label="Abone ara"
          />
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/aboneler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni abonelik</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Abone</Th>
                <Th>Saha</Th>
                <Th>Gün ve saat</Th>
                <Th>Dönem</Th>
                <Th className="text-right">Ücret</Th>
                <Th className="text-right">Kalan</Th>
                <Th>Ödeme</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {liste.length === 0 ? (
                <Bos colSpan={9}>{aramaAktif ? 'Aramaya uyan abonelik bulunamadı.' : 'Abonelik kaydı bulunamadı.'}</Bos>
              ) : (
                liste.map((a) => (
                  <Tr key={a.id}>
                    <Td tam className="font-semibold sm:font-normal">
                      <div>{a.adSoyad}</div>
                      <div className="text-[13px] text-ink/55">{telGoster(a.telNo)}</div>
                    </Td>
                    <Td etiket="Saha">{a.sahaAd}</Td>
                    <Td etiket="Gün ve saat">
                      {gunAdi(a.haftaGunu)} · {saatAraligi(a.baslangicDakika, a.sureDakika)}
                    </Td>
                    <Td etiket="Dönem" className="whitespace-nowrap">
                      {kisaTarih(a.baslangicTarih)} – {kisaTarih(a.bitisTarih)}
                    </Td>
                    <Td etiket="Ücret" className="whitespace-nowrap text-right tabular-nums">{tl(a.ucret)}</Td>
                    <Td etiket="Kalan" className="whitespace-nowrap text-right tabular-nums">
                      {Number(a.kalanTutar) > 0 ? tl(a.kalanTutar) : '—'}
                    </Td>
                    <Td etiket="Ödeme"><OdemeDurumuEtiketi durum={a.odemeDurumu} /></Td>
                    <Td etiket="Durum"><AboneDurumEtiketi abone={a} /></Td>
                    <Td tam className="text-right">
                      <Button variant="guncelle" size="sm" onClick={() => navigate('/aboneler/' + a.id)}>
                        Detay
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>
        )}
      </Card>
    </div>
  );
}
