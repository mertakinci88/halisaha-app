import { useCallback, useEffect, useState } from 'react';
import { ArrowRight, Pencil, Send, Undo2, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Select, Textarea } from '@/components/ui/input';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { ConfirmDialog } from '@/components/ui/dialog';
import { Yukleniyor } from '@/components/ui/durum';
import { grupService, transferService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { TRANSFER_DURUM, tarihSaat } from '@/lib/format';

/**
 * Öğrenci detayındaki grup transferi: transfer talebi açılır (admin onaylayana kadar öğrenci eski grubunda kalır),
 * bekleyen talep geri çekilebilir; altta öğrencinin nereden nereye, ne zaman, kim tarafından transfer edildiği geçmişi listelenir.
 * grupId: öğrencinin şu anki grubu (değişince geçmiş yenilenir).
 */
export default function OgrenciTransferKarti({ ogrenciId, grupId }) {
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [gruplar, setGruplar] = useState([]);
  const [gecmis, setGecmis] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [hedef, setHedef] = useState('');
  const [aciklama, setAciklama] = useState('');
  const [gonderiyor, setGonderiyor] = useState(false);
  const [talepHatasi, setTalepHatasi] = useState('');
  const [geriCekilecek, setGeriCekilecek] = useState(null);
  const [duzenleniyor, setDuzenleniyor] = useState(false); // bekleyen talebin hedef grubu / açıklaması düzenleniyor

  const yukle = useCallback(() => {
    return Promise.all([grupService.list(), transferService.gecmis(ogrenciId)])
      .then(([g, t]) => {
        setGruplar(g);
        setGecmis(t);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [ogrenciId]);

  useEffect(() => {
    yukle();
  }, [yukle, grupId]);

  const bekleyen = gecmis.find((t) => t.durum === 'BEKLIYOR');
  const hedefler = gruplar.filter((g) => String(g.id) !== String(grupId));

  const duzenlemeyeBasla = () => {
    setTalepHatasi('');
    setHedef(String(bekleyen.hedefGrupId ?? ''));
    setAciklama(bekleyen.aciklama || '');
    setDuzenleniyor(true);
  };

  const vazgec = () => {
    setDuzenleniyor(false);
    setTalepHatasi('');
    setHedef('');
    setAciklama('');
  };

  const talepEt = async (e) => {
    e.preventDefault();
    setTalepHatasi('');
    setGonderiyor(true);
    try {
      const govde = { hedefGrupId: Number(hedef), aciklama: aciklama.trim() || null };
      if (duzenleniyor) {
        await transferService.guncelle(bekleyen.id, govde);
        toast.basari('Transfer talebi güncellendi.');
      } else {
        await transferService.talepEt(ogrenciId, govde);
        toast.basari('Transfer talebi açıldı; admin onayından sonra öğrenci yeni gruba geçer.');
      }
      setDuzenleniyor(false);
      setHedef('');
      setAciklama('');
      await yukle();
    } catch (err) {
      setTalepHatasi(apiHata(err));
    } finally {
      setGonderiyor(false);
    }
  };

  const geriCek = async () => {
    const t = geriCekilecek;
    setGeriCekilecek(null);
    try {
      await transferService.iptal(t.id);
      toast.basari('Transfer talebi geri çekildi.');
      await yukle();
    } catch (err) {
      setHata(apiHata(err));
      yukle();
    }
  };

  const nereden = (t) => (
    <span className="inline-flex flex-wrap items-center gap-1.5">
      <span>{t.kaynakGrupAd || <span className="text-ink/55">Grup yok</span>}</span>
      <ArrowRight size={14} strokeWidth={1.5} className="text-ink/55" />
      <b className="font-semibold">{t.hedefGrupAd}</b>
    </span>
  );

  return (
    <Card className="mt-5 gap-4">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <h4 className="text-[22px]">Transfer bilgileri</h4>
        {yukleniyor ? null : (
          <span className="text-[14px] text-ink/60">
            {gecmis.length === 0 ? 'Transfer kaydı yok' : gecmis.length + ' transfer kaydı'}
          </span>
        )}
      </div>

      {yukleniyor ? (
        <Yukleniyor />
      ) : (
        <>
          {bekleyen && !duzenleniyor ? (
            <div className="flex flex-wrap items-center gap-3 border border-yellow-800/30 bg-yellow-100 px-3 py-2.5 text-[14.5px]">
              <span>
                Onay bekleyen transfer talebi: {nereden(bekleyen)}
                <span className="ml-2 text-ink/60">({tarihSaat(bekleyen.talepTarihi)})</span>
                {bekleyen.aciklama ? <span className="mt-0.5 block text-ink/70">Not: {bekleyen.aciklama}</span> : null}
              </span>
              <span className="ml-auto flex gap-2">
                <Button variant="cerceve" size="sm" onClick={duzenlemeyeBasla}>
                  <Pencil size={14} strokeWidth={1.5} />
                  <span>Güncelle</span>
                </Button>
                <Button variant="cerceveHata" size="sm" onClick={() => setGeriCekilecek(bekleyen)}>
                  <Undo2 size={14} strokeWidth={1.5} />
                  <span>Talebi geri çek</span>
                </Button>
              </span>
            </div>
          ) : (
            <form onSubmit={talepEt} className="grid max-w-[640px] gap-3">
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Hedef grup">
                  <Select value={hedef} onChange={(e) => setHedef(e.target.value)} required>
                    <option value="">Grup seçin…</option>
                    {hedefler.map((g) => (
                      <option key={g.id} value={g.id}>{g.ad}</option>
                    ))}
                  </Select>
                </Field>
                <Field label="Açıklama (isteğe bağlı)">
                  <Textarea
                    className="min-h-10"
                    rows={1}
                    maxLength={500}
                    placeholder="Transfer sebebi"
                    value={aciklama}
                    onChange={(e) => setAciklama(e.target.value)}
                  />
                </Field>
              </div>
              {talepHatasi ? (
                <div role="alert" className="border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">
                  {talepHatasi}
                </div>
              ) : null}
              <div className="flex flex-wrap items-center gap-3">
                <Button type="submit" variant="primary" disabled={!hedef || gonderiyor}>
                  <Send size={16} strokeWidth={1.5} />
                  <span>{gonderiyor ? 'Kaydediliyor…' : duzenleniyor ? 'Talebi güncelle' : 'Transfer talebi aç'}</span>
                </Button>
                {duzenleniyor ? (
                  <Button type="button" variant="cerceveHata" disabled={gonderiyor} onClick={vazgec}>
                    <X size={16} strokeWidth={1.5} />
                    <span>Vazgeç</span>
                  </Button>
                ) : (
                  <span className="text-[13px] text-ink/55">Talep admin onayına gider; onaylanana kadar öğrenci mevcut grubunda kalır.</span>
                )}
              </div>
            </form>
          )}

          {hata ? <div role="alert" className="text-[14px] text-hata-800">{hata}</div> : null}

          <div>
            <div className="mb-1 text-[12.5px] uppercase tracking-[0.08em] text-ink/60">Transfer geçmişi</div>
            <Table kart>
              <thead>
                <tr>
                  <Th>Transfer</Th>
                  <Th>Talep tarihi</Th>
                  <Th>Talep eden</Th>
                  <Th>Durum</Th>
                  <Th>Karar tarihi</Th>
                  <Th>Karar veren</Th>
                  <Th>Not</Th>
                </tr>
              </thead>
              <tbody>
                {gecmis.length === 0 ? (
                  <Bos colSpan={7}>Bu öğrenci için transfer kaydı yok.</Bos>
                ) : (
                  gecmis.map((t) => {
                    const d = TRANSFER_DURUM[t.durum];
                    return (
                      <Tr key={t.id}>
                        <Td tam>{nereden(t)}</Td>
                        <Td etiket="Talep tarihi" className="whitespace-nowrap">{tarihSaat(t.talepTarihi)}</Td>
                        <Td etiket="Talep eden">{t.talepEden || '—'}</Td>
                        <Td etiket="Durum"><Tag tone={d.tone}>{d.ad}</Tag></Td>
                        <Td etiket="Karar tarihi" className="whitespace-nowrap">{t.kararTarihi ? tarihSaat(t.kararTarihi) : '—'}</Td>
                        <Td etiket="Karar veren">{t.kararVeren || '—'}</Td>
                        <Td etiket="Not">{[t.aciklama, t.kararNotu].filter(Boolean).join(' · ') || '—'}</Td>
                      </Tr>
                    );
                  })
                )}
              </tbody>
            </Table>
          </div>
        </>
      )}

      <ConfirmDialog
        open={!!geriCekilecek}
        title="Talep geri çekilsin mi?"
        description="Transfer talebi iptal edilir; öğrenci mevcut grubunda kalır."
        confirmLabel="Geri çek"
        onConfirm={geriCek}
        onCancel={() => setGeriCekilecek(null)}
      />
    </Card>
  );
}
