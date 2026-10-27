import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronDown, ChevronLeft, ChevronRight, CircleCheck, CircleOff, Eye, Plus, Search, SlidersHorizontal, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { grupService, ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { AYLAR, tl } from '@/lib/format';
import { ENVANTER_TURLERI } from '@/lib/envanter';
import BilgiIkonu, { BilgiMaddeleri } from '@/components/BilgiIkonu';

const BOYUT = 10;
const ODEME_PARAMLARI = ['odendi', 'bekleyen', 'hic', 'kismi', 'gecikmis'];
const ODEME_SECENEKLERI = [
  { deger: 'odendi', ad: 'Ödendi' },
  { deger: 'bekleyen', ad: 'Ödeme bekleyen' },
  { deger: 'hic', ad: '\u00a0\u00a0– Hiç ödeme yapmamış' },
  { deger: 'kismi', ad: '\u00a0\u00a0– Kısmi ödeme yapmış' },
  { deger: 'gecikmis', ad: '\u00a0\u00a0– Gecikmiş' },
];
// ?envanter=teslim | borclu | almamis | tamam (eksik = teslim; eski bağlantılar için): envanter durumuna göre filtre
const ENVANTER_PARAMLARI = {
  teslim: 'TESLIM_BEKLEYEN',
  eksik: 'TESLIM_BEKLEYEN',
  borclu: 'BORCLU',
  almamis: 'ALMAMIS',
  tamam: 'TAMAM',
};
const ENVANTER_SECENEKLERI = [
  { deger: 'teslim', ad: 'Teslim bekleyen' },
  { deger: 'borclu', ad: 'Ödeme borcu olan' },
  { deger: 'almamis', ad: 'Envanter almamış' },
  { deger: 'tamam', ad: 'Hepsi tamam' },
];
// ?kayit=bu_ay | son_3_ay: kayıt tarihine göre filtre
const KAYIT_SECENEKLERI = [
  { deger: 'bu_ay', ad: 'Bu ay kayıt olanlar' },
  { deger: 'son_3_ay', ad: 'Son 3 ayda kayıt olanlar' },
];
// ?antrenman=2 | 3 | 4: haftalık antrenman günü sayısı (kayıt formundaki seçeneklerle aynı)
const ANTRENMAN_GUNLERI = ['2', '3', '4'];
const BOS_SAYFA = { icerik: [], sayfa: 0, boyut: BOYUT, toplamKayit: 0, toplamSayfa: 0 };

export default function OgrenciListePage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  // ?odeme=odendi | bekleyen | hic | kismi | gecikmis: içinde bulunulan ayın ödeme durumuna göre filtre (panodaki bağlantılar bekleyen kullanır)
  const odeme = ODEME_PARAMLARI.includes(params.get('odeme')) ? params.get('odeme') : '';
  const envanterParam = params.get('envanter');
  const envanter = envanterParam === 'eksik' ? 'teslim' : ENVANTER_PARAMLARI[envanterParam] ? envanterParam : '';
  // ?tur=<envanter türü>: yalnızca teslim bekleyen filtresiyle anlamlıdır (hangi ürün bekleniyor)
  const tur = envanter === 'teslim' && ENVANTER_TURLERI.some((t) => t.kod === params.get('tur')) ? params.get('tur') : '';
  // ?grup=<id>: Gruplar sayfasından seçilen grubun öğrencileri
  const grupId = Number(params.get('grup')) || null;
  const kayit = KAYIT_SECENEKLERI.some((k) => k.deger === params.get('kayit')) ? params.get('kayit') : '';
  const antrenman = ANTRENMAN_GUNLERI.includes(params.get('antrenman')) ? params.get('antrenman') : '';
  // ?transfer=bekleyen: onay bekleyen grup değişikliği talebi olan öğrenciler
  const transferBekleyen = params.get('transfer') === 'bekleyen';
  // ?devamsiz=<n>: son 30 günde en az n kez "Gelmedi" yoklaması olan öğrenciler
  const devamsiz = Number(params.get('devamsiz')) > 0 ? String(Math.floor(Number(params.get('devamsiz')))) : '';
  const [gruplar, setGruplar] = useState([]);
  const [durum, setDurum] = useState('AKTIF'); // AKTIF | PASIF
  const [panelAcik, setPanelAcik] = useState(Boolean(odeme || envanter || kayit || antrenman || transferBekleyen || devamsiz));
  const [arama, setArama] = useState('');
  const [aramaAktif, setAramaAktif] = useState('');
  const [sayfa, setSayfa] = useState(0);
  const [veri, setVeri] = useState(BOS_SAYFA);
  const [hata, setHata] = useHata();
  const [yukleniyor, setYukleniyor] = useState(true);

  const ara = (e) => {
    e?.preventDefault?.();
    setAramaAktif(arama.trim());
    setSayfa(0);
  };

  const aramaDegisti = (e) => {
    const deger = e.target.value;
    setArama(deger);
    // Input'un X (temizle) işaretine basılınca veya elle tamamen silinince
    // beklemeden arama sıfırlanır, ilk sayfa filtresiz listelenir.
    if (deger === '' && aramaAktif !== '') {
      setAramaAktif('');
      setSayfa(0);
    }
  };

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    ogrenciService
      .list({ q: aramaAktif || undefined, grupId: grupId || undefined, durum: durum || undefined, odeme: odeme ? odeme.toUpperCase() : undefined, envanter: envanter ? ENVANTER_PARAMLARI[envanter] : undefined, envanterTur: tur || undefined, kayit: kayit ? kayit.toUpperCase() : undefined, antrenmanGun: antrenman || undefined, transferBekleyen: transferBekleyen || undefined, devamsizlik: devamsiz || undefined, sayfa, boyut: BOYUT })
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [aramaAktif, grupId, durum, odeme, envanter, tur, kayit, antrenman, transferBekleyen, devamsiz, sayfa]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  // Grup seçimi için grup listesi; alınamazsa liste boş kalır
  useEffect(() => {
    grupService.list().then(setGruplar).catch(() => setGruplar([]));
  }, []);

  // Grup ve ödeme / envanter filtresi adres çubuğundaki parametrelerde tutulur; biri değişirken diğerleri korunur
  const filtreYaz = (degisen) => {
    const f = { grup: grupId, odeme, envanter, tur, kayit, antrenman, transfer: transferBekleyen ? 'bekleyen' : '', devamsiz, ...degisen };
    setSayfa(0);
    setParams(
      {
        ...(f.grup ? { grup: String(f.grup) } : {}),
        ...(f.odeme ? { odeme: f.odeme } : {}),
        ...(f.envanter ? { envanter: f.envanter } : {}),
        ...(f.envanter === 'teslim' && f.tur ? { tur: f.tur } : {}),
        ...(f.kayit ? { kayit: f.kayit } : {}),
        ...(f.antrenman ? { antrenman: f.antrenman } : {}),
        ...(f.transfer ? { transfer: f.transfer } : {}),
        ...(f.devamsiz ? { devamsiz: f.devamsiz } : {}),
      },
      { replace: true },
    );
  };

  const durumSec = (d) => {
    setDurum(d);
    setSayfa(0);
  };

  // Panelde kaç filtre açık (düğmede rozet olarak gösterilir)
  const panelFiltreSayisi = [odeme, envanter, kayit, antrenman, transferBekleyen, devamsiz].filter(Boolean).length;

  // Varsayılan görünümden (Aktif, Tüm gruplar, boş arama, panel filtresiz) farklı bir filtre var mı
  const filtreVar = Boolean(grupId) || durum !== 'AKTIF' || panelFiltreSayisi > 0 || aramaAktif !== '' || arama !== '';

  const filtreleriTemizle = () => {
    setArama('');
    setAramaAktif('');
    setDurum('AKTIF');
    setSayfa(0);
    setParams({}, { replace: true });
  };

  const toplamSayfa = Math.max(1, veri.toplamSayfa);

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Öğrenciler" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <form onSubmit={ara} className="flex w-full gap-2 sm:w-auto">
            <Input
              type="search"
              placeholder="Öğrenci, veli veya grup adıyla ara"
              value={arama}
              onChange={aramaDegisti}
              className="min-w-0 flex-1 sm:w-[340px] sm:flex-none"
            />
            <Button type="submit" variant="gri">
              <Search size={16} strokeWidth={1.5} />
              <span>Ara</span>
            </Button>
          </form>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/ogrenciler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni öğrenci</span>
          </Button>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <Select aria-label="Grup" className="w-auto" value={grupId || ''} onChange={(e) => filtreYaz({ grup: e.target.value })}>
            <option value="">Tüm gruplar</option>
            {gruplar.map((g) => (
              <option key={g.id} value={g.id}>
                {g.ad}
              </option>
            ))}
          </Select>
          <Seg>
            <SegOpt active={durum === 'AKTIF'} onClick={() => durumSec('AKTIF')}>
              <CircleCheck size={16} strokeWidth={1.5} />
              <span>Aktif</span>
            </SegOpt>
            <SegOpt active={durum === 'PASIF'} onClick={() => durumSec('PASIF')}>
              <CircleOff size={16} strokeWidth={1.5} />
              <span>Pasif</span>
            </SegOpt>
          </Seg>
          <Button variant={panelFiltreSayisi > 0 ? 'cerceve' : 'secondary'} aria-expanded={panelAcik} onClick={() => setPanelAcik((a) => !a)}>
            <SlidersHorizontal size={16} strokeWidth={1.5} />
            <span>Filtreler{panelFiltreSayisi > 0 ? ' (' + panelFiltreSayisi + ')' : ''}</span>
            <ChevronDown size={15} strokeWidth={1.5} className={panelAcik ? 'rotate-180' : ''} />
          </Button>
          <span className="text-[13.5px] text-ink/55">{veri.toplamKayit} öğrenci</span>
          {filtreVar ? (
            <Button variant="ghost" size="sm" onClick={filtreleriTemizle}>
              <X size={14} strokeWidth={1.5} />
              <span>Filtreleri temizle</span>
            </Button>
          ) : null}
        </div>

        {panelAcik ? (
          <div className="grid gap-3 border border-hairline bg-ink/3 p-3 sm:grid-cols-2 lg:grid-cols-3">
            <Field label={<>Bu ayki ödeme durumu<BilgiIkonu
                  metin={
                    <BilgiMaddeleri
                      giris={`Yalnızca içinde bulunulan ay (${AYLAR[new Date().getMonth()]} ${new Date().getFullYear()}) için geçerlidir; geçmiş aylar listelenmez.`}
                      maddeler={[
                        ['Ödendi', 'Ayı tamamlamış (Tamamı ödendi işaretli ya da parasız kapatılmış).'],
                        ['Ödeme bekleyen', 'Ayı henüz tamamlamamış; kısmi ödeme yapanlar da dahil.'],
                        ['Hiç ödeme yapmamış', 'Bu ay için hiç ödeme girilmemiş.'],
                        ['Kısmi ödeme yapmış', 'Ödeme almış ama ayı tamamlamamış.'],
                        ['Gecikmiş', 'Ayın 15\'i geçtiği halde ayı tamamlamamış.'],
                        ['Ödemesiz öğrenciler', 'Ödeme alınmayan öğrenciler (Ödemesiz etiketli) bu filtrelerin hiçbirinde yer almaz.'],
                      ]}
                    />
                  }
                /></>}>
              <Select value={odeme} onChange={(e) => filtreYaz({ odeme: e.target.value })}>
                <option value="">Tümü</option>
                {ODEME_SECENEKLERI.map((o) => (
                  <option key={o.deger} value={o.deger}>
                    {o.ad}
                  </option>
                ))}
              </Select>
            </Field>
            <div className="grid content-start gap-3">
            <Field label={<>Envanter durumu<BilgiIkonu
                  metin={
                    <BilgiMaddeleri
                      maddeler={[
                        ['Teslim bekleyen', 'Ödemesi alınmış ama teslim edilmemiş ürünü olan.'],
                        ['Ödeme borcu olan', 'En az bir envanter setinin tutarı tam ödenmemiş olan.'],
                        ['Envanter almamış', 'Hiç envanter satışı olmayan.'],
                        ['Hepsi tamam', 'Ödeme borcu ve teslim bekleyen ürünü kalmamış olan.'],
                      ]}
                    />
                  }
                /></>}>
              <Select value={envanter} onChange={(e) => filtreYaz({ envanter: e.target.value, tur: '' })}>
                <option value="">Tümü</option>
                {ENVANTER_SECENEKLERI.map((o) => (
                  <option key={o.deger} value={o.deger}>
                    {o.ad}
                  </option>
                ))}
              </Select>
            </Field>
            {envanter === 'teslim' ? (
              <Field label={<>Teslim bekleyen ürün<BilgiIkonu metin="Hangi ürünü teslim bekleyen öğrencileri görmek için ürünü seçin." /></>}>
                <Select value={tur} onChange={(e) => filtreYaz({ tur: e.target.value })}>
                  <option value="">Tüm ürünler</option>
                  {ENVANTER_TURLERI.map((t) => (
                    <option key={t.kod} value={t.kod}>
                      {t.ad}
                    </option>
                  ))}
                </Select>
              </Field>
            ) : null}
            </div>
            <Field label={<>Kayıt tarihi<BilgiIkonu sag metin="Son 3 ay, içinde bulunulan ayı da kapsar." /></>}>
              <Select value={kayit} onChange={(e) => filtreYaz({ kayit: e.target.value })}>
                <option value="">Tümü</option>
                {KAYIT_SECENEKLERI.map((k) => (
                  <option key={k.deger} value={k.deger}>
                    {k.ad}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label={<>Haftalık antrenman günü<BilgiIkonu metin="Öğrencinin kayıtlı haftalık antrenman günü sayısı." /></>}>
              <Select value={antrenman} onChange={(e) => filtreYaz({ antrenman: e.target.value })}>
                <option value="">Tümü</option>
                {ANTRENMAN_GUNLERI.map((g) => (
                  <option key={g} value={g}>
                    {g} gün
                  </option>
                ))}
              </Select>
            </Field>
            <Field label={<>Devamsızlık (son 30 gün)<BilgiIkonu metin="Son 30 günde “Gelmedi” işaretlenen yoklama sayısı. “İzinli” günler sayılmaz." /></>}>
              <div className="flex items-center gap-2 text-[15px]">
                <span className="text-ink/70">En az</span>
                <Input
                  type="number"
                  min="1"
                  step="1"
                  inputMode="numeric"
                  placeholder="—"
                  aria-label="Devamsızlık sayısı"
                  value={devamsiz}
                  onChange={(e) => filtreYaz({ devamsiz: e.target.value === '' ? '' : String(Math.max(1, Math.floor(Number(e.target.value)) || 1)) })}
                  className="w-20 text-right tabular-nums"
                />
                <span className="text-ink/70">kez gelmeyenler</span>
              </div>
            </Field>
            <label className="relative flex cursor-pointer items-center gap-2 self-start pt-7 text-[15px]">
              <input
                type="checkbox"
                className="h-4 w-4 accent-[var(--color-accent)]"
                checked={transferBekleyen}
                onChange={(e) => filtreYaz({ transfer: e.target.checked ? 'bekleyen' : '' })}
              />
              <span>Transfer bekleyenler</span>
              <BilgiIkonu sag metin="Grup değişikliği talebi onay bekleyen öğrenciler (Transferler sayfasındaki “Onay bekliyor” kayıtları)." />
            </label>
          </div>
        ) : null}

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <>
            <Table kart>
              <thead>
                <tr>
                  <Th>Ad soyad</Th>
                  <Th>Grup</Th>
                  <Th>Veli</Th>
                  <Th>Telefon</Th>
                  <Th>Bu ayki ödeme</Th>
                  <Th>Durum</Th>
                  <Th>Not</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {veri.icerik.length === 0 ? (
                  <Bos colSpan={8}>{filtreVar ? 'Filtrelere uyan öğrenci bulunamadı.' : 'Öğrenci kaydı bulunamadı.'}</Bos>
                ) : (
                  veri.icerik.map((o) => (
                    <Tr key={o.id} className="cursor-pointer" onClick={() => navigate('/ogrenciler/' + o.id)}>
                      <Td tam className="font-semibold sm:font-normal">{o.adSoyad}</Td>
                      <Td etiket="Grup" className="text-ink/55">{o.grupAd || '—'}</Td>
                      <Td etiket="Veli">{o.veliAdSoyad || '—'}</Td>
                      <Td etiket="Telefon" className="tabular-nums">{o.veliTelNo || '—'}</Td>
                      <Td etiket="Bu ayki ödeme">
                        <OdemeDurumuEtiketi durum={o.odemeDurumu} />
                      </Td>
                      <Td etiket="Durum">
                        <Tag tone={o.durum === 'AKTIF' ? 'accent' : 'outline'}>
                          {o.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                        </Tag>
                      </Td>
                      <Td tam etiket="Not" className="max-sm:order-1 max-w-[220px] truncate text-[13.5px] text-ink/70" title={o.not || ''}>
                        {o.not || '—'}
                      </Td>
                      <Td className="text-right">
                        <Button
                          variant="cerceve"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate('/ogrenciler/' + o.id);
                          }}
                        >
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
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={sayfa <= 0}
                  onClick={() => setSayfa((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft size={15} strokeWidth={1.5} />
                  <span>Önceki</span>
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={sayfa + 1 >= toplamSayfa}
                  onClick={() => setSayfa((p) => p + 1)}
                >
                  <span>Sonraki</span>
                  <ChevronRight size={15} strokeWidth={1.5} />
                </Button>
              </div>
              <span className="order-3 text-[13.5px] text-ink/55 sm:absolute sm:right-0 sm:order-none">
                Toplam öğrenci sayısı : {veri.toplamKayit}
              </span>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
