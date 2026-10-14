import { useEffect, useState } from 'react';
import { ArrowLeft, Ban, Plus, Save, Trash2 } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select, TelInput } from '@/components/ui/input';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { ConfirmDialog } from '@/components/ui/dialog';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import HesapSecimi from '@/components/HesapSecimi';
import IadeBolumu from '@/components/IadeBolumu';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import { AboneDurumEtiketi } from '@/pages/AboneListePage';
import { aboneService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { kisaTarih, ODEME_YONTEMI_LABEL, saatOf, telGoster, telRakam, tl, toIsoDate, tutarSinirla } from '@/lib/format';
import { gunAdi, saatAraligi } from '@/lib/abone';

const BUGUN = toIsoDate(new Date());

const haftaDurumu = (h) => {
  if (h.durum === 'IPTAL') return <Tag tone="danger">İptal</Tag>;
  if (new Date(h.baslangicTarih) < new Date()) return <Tag tone="outline">Geçti</Tag>;
  return <Tag tone="accent">Planlı</Tag>;
};

export default function AboneDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [abone, setAbone] = useState(null);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [form, setForm] = useState(null); // { adSoyad, telNo, ucret, not }
  const [kaydediyor, setKaydediyor] = useState(false);
  const [iptalOnay, setIptalOnay] = useState(false);
  const [iptalNeden, setIptalNeden] = useState('');
  const [silinecekOdeme, setSilinecekOdeme] = useState(null);
  const [odeme, setOdeme] = useState({ tutar: '', sekil: '', hesapId: '', tarih: BUGUN });
  const [odemeEkliyor, setOdemeEkliyor] = useState(false);

  const uygula = (a) => {
    setAbone(a);
    setForm({ adSoyad: a.adSoyad, telNo: telRakam(a.telNo), ucret: String(a.ucret), not: a.not || '' });
    setOdeme((o) => ({ ...o, tutar: Number(a.kalanTutar) > 0 ? String(a.kalanTutar) : '' }));
  };

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    aboneService
      .get(id)
      .then(uygula)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, [id]);

  if (yukleniyor) return <Yukleniyor />;
  if (!abone) {
    return (
      <div>
        <PageHeader kicker="Abonelik" baslik="Abonelik detayı" />
        <Hata mesaj={hata} onTekrar={yukle} />
      </div>
    );
  }

  const aktif = abone.durum === 'AKTIF';
  const degisti =
    form.adSoyad !== abone.adSoyad ||
    '0' + form.telNo !== abone.telNo ||
    Number(form.ucret) !== Number(abone.ucret) ||
    (form.not || '') !== (abone.not || '');
  const kalan = Number(abone.kalanTutar);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    if (!/^5\d{9}$/.test(form.telNo)) {
      setHata('Telefon numarası 10 haneli olmalı ve 5 ile başlamalıdır (örn. 546 976 46 13).');
      return;
    }
    setKaydediyor(true);
    try {
      uygula(await aboneService.update(id, {
        adSoyad: form.adSoyad.trim(),
        telNo: '0' + form.telNo,
        ucret: Number(String(form.ucret).replace(',', '.')),
        not: form.not.trim() || null,
      }));
      toast.basari('Abonelik güncellendi.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const iptalEt = async () => {
    setIptalOnay(false);
    try {
      uygula(await aboneService.iptal(id, iptalNeden.trim()));
      toast.basari('Abonelik iptal edildi; gelecekteki haftalık rezervasyonlar iptal edildi.');
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const odemeEkle = async (e) => {
    e.preventDefault();
    setHata('');
    if (!odeme.sekil) {
      setHata('Lütfen ödeme şeklini seçiniz.');
      return;
    }
    setOdemeEkliyor(true);
    try {
      uygula(await aboneService.odemeEkle(id, {
        tutar: Number(String(odeme.tutar).replace(',', '.')),
        odemeSekli: odeme.sekil,
        hesapId: odeme.sekil === 'HAVALE_EFT' && odeme.hesapId ? Number(odeme.hesapId) : null,
        odemeTarihi: odeme.tarih || null,
      }));
      setOdeme((o) => ({ ...o, sekil: '', hesapId: '', tarih: BUGUN }));
      toast.basari('Ödeme eklendi.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setOdemeEkliyor(false);
    }
  };

  const odemeSil = async () => {
    const o = silinecekOdeme;
    setSilinecekOdeme(null);
    try {
      uygula(await aboneService.odemeSil(id, o.id));
      toast.basari('Ödeme silindi.');
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const bilgiSatiri = (etiket, deger) => (
    <div className="flex justify-between gap-3 text-[15px]">
      <span className="text-ink/55">{etiket}</span>
      <span className="text-right">{deger}</span>
    </div>
  );

  return (
    <div>
      <PageHeader kicker="Abonelik" baslik={abone.adSoyad}>
        <Button variant="secondary" onClick={() => navigate('/aboneler')}>
          <ArrowLeft size={16} strokeWidth={1.5} />
          <span>Aboneler</span>
        </Button>
      </PageHeader>
      <Hata mesaj={hata} />

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(340px,100%),1fr))]">
        <div className="grid content-start gap-5">
          <Card className="gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="text-[22px]">Rezervasyon bilgileri</h4>
              <span className="ml-auto"><AboneDurumEtiketi abone={abone} /></span>
            </div>
            {bilgiSatiri('Saha', abone.sahaAd)}
            {bilgiSatiri('Gün ve saat', gunAdi(abone.haftaGunu) + ' · ' + saatAraligi(abone.baslangicDakika, abone.sureDakika))}
            {bilgiSatiri('Dönem', kisaTarih(abone.baslangicTarih) + ' – ' + kisaTarih(abone.bitisTarih))}
            {bilgiSatiri('Kayıt', kisaTarih(abone.kayitTarih) + (abone.kaydedenAd ? ' · ' + abone.kaydedenAd : ''))}
            {abone.iptalTarih ? bilgiSatiri('İptal tarihi', kisaTarih(abone.iptalTarih)) : null}
            {abone.durum === 'IPTAL' ? bilgiSatiri('İptal eden', abone.iptalEdenAd || 'Kayıt yok') : null}
            {abone.iptalNeden ? bilgiSatiri('İptal nedeni', abone.iptalNeden) : null}
            <p className="m-0 text-[13px] text-ink/55">
              Gün, saat ve tarihler değiştirilemez; değişiklik için aboneliği iptal edip yeni abonelik açın.
            </p>
          </Card>

          <Card className="gap-3.5">
            <h4 className="text-[22px]">Abone bilgileri</h4>
            <form onSubmit={kaydet} className="grid gap-3.5">
              <Field label="Ad soyad">
                <Input value={form.adSoyad} onChange={(e) => setForm({ ...form, adSoyad: e.target.value })} required maxLength={255} />
              </Field>
              <Field label="Telefon">
                <TelInput value={form.telNo} onChange={(v) => setForm({ ...form, telNo: v })} required />
              </Field>
              <Field label="Abonelik ücreti (₺)">
                <Input
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputMode="decimal"
                  value={form.ucret}
                  onChange={(e) => setForm({ ...form, ucret: tutarSinirla(e.target.value, 10000000) })}
                  required
                />
              </Field>
              <Field label="Not">
                <Input value={form.not} onChange={(e) => setForm({ ...form, not: e.target.value })} maxLength={255} />
              </Field>
              <p className="m-0 text-[13px] text-ink/55">
                Ad ve telefon değişirse henüz gerçekleşmemiş haftalık rezervasyonlarda da güncellenir.
              </p>
              <div className="flex gap-2">
                <Button type="submit" variant="primary" className="flex-1" disabled={!degisti || kaydediyor}>
                  <Save size={16} strokeWidth={1.5} />
                  <span>{kaydediyor ? 'Kaydediliyor…' : 'Kaydet'}</span>
                </Button>
                {aktif ? (
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => {
                      setIptalNeden('');
                      setIptalOnay(true);
                    }}
                  >
                    <Ban size={16} strokeWidth={1.5} />
                    <span>Aboneliği iptal et</span>
                  </Button>
                ) : null}
              </div>
            </form>
          </Card>
        </div>

        <div className="grid content-start gap-5">
          <Card className="gap-3.5">
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="text-[22px]">Ödemeler</h4>
              <span className="ml-auto"><OdemeDurumuEtiketi durum={abone.odemeDurumu} /></span>
            </div>
            <Table kart>
              <thead>
                <tr>
                  <Th>Tarih</Th>
                  <Th className="text-right">Tutar</Th>
                  <Th>Ödeme şekli</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {abone.odemeler.length === 0 ? <Bos colSpan={4}>Ödeme kaydı yok.</Bos> : null}
                {abone.odemeler.map((o) => (
                  <Tr key={o.id}>
                    <Td etiket="Tarih" className="whitespace-nowrap">{kisaTarih(o.odemeTarihi)}</Td>
                    <Td etiket="Tutar" className="whitespace-nowrap text-right tabular-nums">{tl(o.tutar)}</Td>
                    <Td etiket="Ödeme şekli">{ODEME_YONTEMI_LABEL[o.odemeSekli]}{o.hesapAd ? ' (' + o.hesapAd + ')' : ''}</Td>
                    <Td tam className="text-right">
                      <Button
                        variant="cerceveHata"
                        size="sm"
                        disabled={abone.odemeler.length === 1}
                        title={abone.odemeler.length === 1 ? 'Aboneliğin en az bir ödemesi olmalı' : undefined}
                        onClick={() => setSilinecekOdeme(o)}
                      >
                        <Trash2 size={14} strokeWidth={1.5} />
                        <span>Sil</span>
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <div className="flex flex-col gap-1 border-t border-divider pt-2.5 text-[15px]">
              <div className="flex justify-between"><span>Abonelik ücreti</span><b className="tabular-nums">{tl(abone.ucret)}</b></div>
              <div className="flex justify-between"><span>Ödenen</span><b className="tabular-nums">{tl(abone.odenenTutar)}</b></div>
              <div className="flex justify-between"><span>Kalan</span><b className="tabular-nums">{kalan > 0 ? tl(kalan) : '—'}</b></div>
            </div>

            <IadeBolumu
              listeParams={{ abonelikId: abone.id }}
              kaynaklar={[{ id: abone.id, alan: 'abonelikId', etiket: 'Abonelik', odenen: Number(abone.odenenTutar) }]}
            />

            {kalan > 0 ? (
              <form onSubmit={odemeEkle} className="grid gap-3 border-t border-divider pt-3">
                <div className="font-heading text-[18px] font-semibold">Yeni ödeme</div>
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Tutar (₺)">
                    <Input
                      type="number"
                      min="0.01"
                      step="0.01"
                      inputMode="decimal"
                      value={odeme.tutar}
                      onChange={(e) => setOdeme({ ...odeme, tutar: tutarSinirla(e.target.value, kalan) })}
                      required
                    />
                  </Field>
                  <Field label="Ödeme tarihi">
                    <Input type="date" max={BUGUN} value={odeme.tarih} onChange={(e) => setOdeme({ ...odeme, tarih: e.target.value })} required />
                  </Field>
                </div>
                <Field label="Ödeme şekli">
                  <Select
                    value={odeme.sekil}
                    onChange={(e) => setOdeme({ ...odeme, sekil: e.target.value, hesapId: e.target.value === 'HAVALE_EFT' ? odeme.hesapId : '' })}
                    required
                  >
                    <option value="">Seçiniz</option>
                    {Object.entries(ODEME_YONTEMI_LABEL).map(([kod, ad]) => (
                      <option key={kod} value={kod}>{ad}</option>
                    ))}
                  </Select>
                </Field>
                {odeme.sekil === 'HAVALE_EFT' ? <HesapSecimi value={odeme.hesapId} onChange={(v) => setOdeme({ ...odeme, hesapId: v })} /> : null}
                <Button type="submit" variant="primary" disabled={odemeEkliyor}>
                  <Plus size={16} strokeWidth={1.5} />
                  <span>{odemeEkliyor ? 'Ekleniyor…' : 'Ödeme ekle'}</span>
                </Button>
              </form>
            ) : null}
          </Card>

          <Card className="gap-3">
            <h4 className="text-[22px]">Haftalık rezervasyonlar</h4>
            <p className="m-0 text-[13px] text-ink/55">
              Bir haftayı iptal etmek ya da ürün eklemek için tarihe tıklayın; rezervasyon sayfasında yapılır.
            </p>
            <div className="max-h-[420px] overflow-y-auto">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Tarih</Th>
                    <Th>Saat</Th>
                    <Th>Durum</Th>
                  </tr>
                </thead>
                <tbody>
                  {abone.haftalar.length === 0 ? <Bos colSpan={3}>Rezervasyon yok.</Bos> : null}
                  {abone.haftalar.map((h) => (
                    <Tr key={h.rezervasyonId}>
                      <Td etiket="Tarih" className="whitespace-nowrap">
                        <Link to={'/rezervasyon/' + h.rezervasyonId} className="text-accent-700 underline">{kisaTarih(h.baslangicTarih)}</Link>
                      </Td>
                      <Td etiket="Saat" className="whitespace-nowrap">{saatOf(h.baslangicTarih)} – {saatOf(h.bitisTarih)}</Td>
                      <Td etiket="Durum">{haftaDurumu(h)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </div>
          </Card>
        </div>
      </div>

      <ConfirmDialog
        open={iptalOnay}
        title="Abonelik iptal edilsin mi?"
        description="Henüz gerçekleşmemiş haftalık rezervasyonların hepsi iptal edilir ve saha boşalır. Geçmiş haftalar ve alınan ödemeler silinmez. Bu işlem geri alınamaz."
        isim={abone.adSoyad + ' · ' + gunAdi(abone.haftaGunu) + ' ' + saatAraligi(abone.baslangicDakika, abone.sureDakika)}
        confirmLabel="Aboneliği iptal et"
        tehlikeli
        onConfirm={iptalEt}
        onCancel={() => setIptalOnay(false)}
      >
        <Input
          value={iptalNeden}
          onChange={(e) => setIptalNeden(e.target.value)}
          maxLength={255}
          placeholder="İptal nedeni (isteğe bağlı)"
          aria-label="İptal nedeni"
        />
      </ConfirmDialog>
      <ConfirmDialog
        open={!!silinecekOdeme}
        title="Ödeme silinsin mi?"
        description="Bu ödeme abonelikten düşülür; kalan tutar artar."
        isim={silinecekOdeme ? tl(silinecekOdeme.tutar) + ' · ' + kisaTarih(silinecekOdeme.odemeTarihi) : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={odemeSil}
        onCancel={() => setSilinecekOdeme(null)}
      />
    </div>
  );
}
