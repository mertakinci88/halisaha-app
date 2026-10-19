import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Ban, ChevronLeft, ChevronRight, CircleCheck, Eye, List, Pencil, Plus, Save, Search, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import { aboneService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import { kisaTarih, telGoster, tl, tutarSinirla } from '@/lib/format';
import { gunAdi, HAFTA_GUNLERI, saatAraligi } from '@/lib/abone';

/** Abonelik durumu etiketi: Aktif / İptal / Süresi doldu */
export function AboneDurumEtiketi({ abone }) {
  if (abone.durum === 'IPTAL') return <Tag tone="danger">İptal</Tag>;
  if (abone.suresiDoldu) return <Tag tone="warning">Süresi doldu</Tag>;
  return <Tag tone="accent">Aktif</Tag>;
}

const BOYUT = 10;
const BOS_SAYFA = { icerik: [], sayfa: 0, boyut: BOYUT, toplamKayit: 0, toplamSayfa: 0 };
const KISA_ETIKET = { AKTIF: 'aktif abone', IPTAL: 'iptal abone', '': 'abone' };
const TOPLAM_ETIKET = { AKTIF: 'Toplam aktif abone', IPTAL: 'Toplam iptal abone', '': 'Toplam abone' };

export default function AboneListePage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [hata, setHata] = useHata();
  const [durum, setDurum] = useState(''); // '' (tümü) | AKTIF | IPTAL
  const [arama, setArama] = useState('');
  const [aramaAktif, setAramaAktif] = useState('');
  const [sahaId, setSahaId] = useState(''); // '' = Tüm sahalar
  const [gun, setGun] = useState(''); // '' = Tüm günler, 1 (Pazartesi) ... 7 (Pazar)
  const [sahalar, setSahalar] = useState([]);
  const [sayfa, setSayfa] = useState(0);
  const [veri, setVeri] = useState(BOS_SAYFA);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [standart, setStandart] = useState(null); // kayıtlı standart ücret (null = tanımsız)
  const [ucretDuzen, setUcretDuzen] = useState(null); // null = düzenlenmiyor
  const [ucretKaydediyor, setUcretKaydediyor] = useState(false);

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    aboneService
      .list({ durum: durum || undefined, sahaId: sahaId || undefined, haftaGunu: gun || undefined, q: aramaAktif || undefined, sayfa, boyut: BOYUT })
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [durum, sahaId, gun, aramaAktif, sayfa]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  useEffect(() => {
    aboneService
      .ayar()
      .then((a) => setStandart(a.standartUcret))
      .catch(() => {});
    sahaService.list().then(setSahalar).catch(() => setSahalar([]));
  }, []);

  // Arama yalnızca Ara butonuna basılınca (ya da Enter ile) yapılır
  const ara = (e) => {
    e?.preventDefault?.();
    setAramaAktif(arama.trim());
    setSayfa(0);
  };

  const aramaDegisti = (e) => {
    const deger = e.target.value;
    setArama(deger);
    // X (temizle) ile ya da elle tamamen silinince arama sıfırlanır
    if (deger === '' && aramaAktif !== '') {
      setAramaAktif('');
      setSayfa(0);
    }
  };

  const durumSec = (d) => {
    setDurum(d);
    setSayfa(0);
  };

  const sahaSec = (id) => {
    setSahaId(id);
    setSayfa(0);
  };

  const gunSec = (g) => {
    setGun(g);
    setSayfa(0);
  };

  // Varsayılan görünümden (Tümü, Tüm sahalar, Tüm günler, boş arama) farklı bir filtre var mı
  const filtreVar = durum !== '' || sahaId !== '' || gun !== '' || aramaAktif !== '' || arama !== '';

  const filtreleriTemizle = () => {
    setDurum('');
    setSahaId('');
    setGun('');
    setArama('');
    setAramaAktif('');
    setSayfa(0);
  };

  const toplamSayfa = Math.max(1, veri.toplamSayfa);

  const ucretKaydet = async () => {
    const tutar = Number(String(ucretDuzen).replace(',', '.'));
    if (!(tutar >= 0)) return;
    setUcretKaydediyor(true);
    try {
      const a = await aboneService.ayarKaydet({ standartUcret: tutar });
      setStandart(a.standartUcret);
      setUcretDuzen(null);
      toast.basari('Standart depozito kaydedildi.');
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
            <div className="text-[11.5px] uppercase tracking-[0.1em] text-accent-700">Standart depozito</div>
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
                    aria-label="Standart depozito"
                  />
                </div>
                <Button variant="primary" disabled={ucretKaydediyor || ucretDuzen === ''} onClick={ucretKaydet}>
                  <Save size={16} strokeWidth={1.5} />
                  <span>Kaydet</span>
                </Button>
                <Button variant="cerceveHata" onClick={() => setUcretDuzen(null)}>
                  <X size={16} strokeWidth={1.5} />
                  <span>Vazgeç</span>
                </Button>
              </div>
            )}
          </div>
          {isAdmin && ucretDuzen === null ? (
            <Button variant="cerceve" size="sm" onClick={() => setUcretDuzen(standart === null ? '' : String(standart))}>
              <Pencil size={14} strokeWidth={1.5} />
              <span>Değiştir</span>
            </Button>
          ) : null}
        </div>
        <p className="m-0 text-[13px] text-ink/55">
          Yeni abonelik kaydında depozito bu tutardan gelir; kayıt sırasında elle değiştirilebilir.
          {!isAdmin ? ' Standart depozitoyu yalnızca yönetici değiştirebilir.' : ''}
        </p>
      </Card>

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <form onSubmit={ara} className="flex w-full gap-2 sm:w-auto">
            <Input
              type="search"
              placeholder="Ad soyad ya da telefon ara"
              value={arama}
              onChange={aramaDegisti}
              className="min-w-0 flex-1 sm:w-[340px] sm:flex-none"
              aria-label="Abone ara"
            />
            <Button type="submit" variant="gri">
              <Search size={16} strokeWidth={1.5} />
              <span>Ara</span>
            </Button>
          </form>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/aboneler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni abonelik</span>
          </Button>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <Select aria-label="Saha" className="w-auto" value={sahaId} onChange={(e) => sahaSec(e.target.value)}>
            <option value="">Tüm sahalar</option>
            {sahalar.map((sh) => (
              <option key={sh.id} value={sh.id}>
                {sh.ad}
              </option>
            ))}
          </Select>
          <Select aria-label="Gün" className="w-auto" value={gun} onChange={(e) => gunSec(e.target.value)}>
            <option value="">Tüm günler</option>
            {HAFTA_GUNLERI.map((g) => (
              <option key={g.no} value={g.no}>
                {g.ad}
              </option>
            ))}
          </Select>
          <Seg>
            <SegOpt active={durum === ''} onClick={() => durumSec('')}>
              <List size={16} strokeWidth={1.5} />
              <span>Tümü</span>
            </SegOpt>
            <SegOpt active={durum === 'AKTIF'} onClick={() => durumSec('AKTIF')}>
              <CircleCheck size={16} strokeWidth={1.5} />
              <span>Aktif</span>
            </SegOpt>
            <SegOpt active={durum === 'IPTAL'} onClick={() => durumSec('IPTAL')}>
              <Ban size={16} strokeWidth={1.5} />
              <span>İptal</span>
            </SegOpt>
          </Seg>
          <span className="text-[13.5px] text-ink/55">
            {veri.toplamKayit} {KISA_ETIKET[durum]}
          </span>
          {filtreVar ? (
            <Button variant="ghost" size="sm" onClick={filtreleriTemizle}>
              <X size={14} strokeWidth={1.5} />
              <span>Filtreleri temizle</span>
            </Button>
          ) : null}
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <>
          <Table kart>
            <thead>
              <tr>
                <Th>Abone</Th>
                <Th>Saha</Th>
                <Th>Gün ve saat</Th>
                <Th>Dönem</Th>
                <Th className="text-right">Depozito</Th>
                <Th className="text-right">Haftalık</Th>
                <Th className="text-right">Eksik depozito</Th>
                <Th>Ödeme</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {veri.icerik.length === 0 ? (
                <Bos colSpan={10}>{aramaAktif ? 'Aramaya uyan abonelik bulunamadı.' : 'Abonelik kaydı bulunamadı.'}</Bos>
              ) : (
                veri.icerik.map((a) => (
                  <Tr key={a.id} className="cursor-pointer" onClick={() => navigate('/aboneler/' + a.id)}>
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
                    <Td etiket="Depozito" className="whitespace-nowrap text-right tabular-nums">{tl(a.depozito)}</Td>
                    <Td etiket="Haftalık" className="whitespace-nowrap text-right tabular-nums">{tl(a.haftalikUcret)}</Td>
                    <Td etiket="Eksik depozito" className="whitespace-nowrap text-right tabular-nums">
                      {Number(a.kalanTutar) > 0 ? tl(a.kalanTutar) : '—'}
                    </Td>
                    <Td etiket="Ödeme"><OdemeDurumuEtiketi durum={a.odemeDurumu} /></Td>
                    <Td etiket="Durum"><AboneDurumEtiketi abone={a} /></Td>
                    <Td tam className="text-right">
                      <Button variant="cerceve" size="sm" onClick={(e) => { e.stopPropagation(); navigate('/aboneler/' + a.id); }}>
                        <Eye size={14} strokeWidth={1.5} />
                        <span>Detay</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>

          <div className="relative flex flex-col items-center gap-2 pt-1 sm:flex-row sm:justify-center sm:gap-3">
            <span className="order-2 text-[13.5px] text-ink/55 sm:absolute sm:left-0 sm:order-none">
              Sayfa {veri.sayfa + 1} / {toplamSayfa}
            </span>
            <div className="order-1 flex gap-1.5 sm:order-none">
              <Button variant="secondary" size="sm" disabled={sayfa <= 0} onClick={() => setSayfa((p) => Math.max(0, p - 1))}>
                <ChevronLeft size={15} strokeWidth={1.5} />
                <span>Önceki</span>
              </Button>
              <Button variant="secondary" size="sm" disabled={sayfa + 1 >= toplamSayfa} onClick={() => setSayfa((p) => p + 1)}>
                <span>Sonraki</span>
                <ChevronRight size={15} strokeWidth={1.5} />
              </Button>
            </div>
            <span className="order-3 text-[13.5px] text-ink/55 sm:absolute sm:right-0 sm:order-none">
              {TOPLAM_ETIKET[durum]}: {veri.toplamKayit}
            </span>
          </div>
          </>
        )}
      </Card>
    </div>
  );
}
