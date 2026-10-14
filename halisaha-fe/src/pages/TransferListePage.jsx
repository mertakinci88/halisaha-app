import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Check, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { transferService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { TRANSFER_DURUM, tarihSaat } from '@/lib/format';

/** Admin transfer ekranı: personelin açtığı grup transfer taleplerini görür, onaylar ya da reddeder. */
export default function TransferListePage() {
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [filtre, setFiltre] = useState('BEKLIYOR'); // BEKLIYOR | '' (tümü)
  const [liste, setListe] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [onaylanacak, setOnaylanacak] = useState(null);
  const [reddedilecek, setReddedilecek] = useState(null);
  const [redNedeni, setRedNedeni] = useState('');

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    transferService
      .list(filtre)
      .then(setListe)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, [filtre]);

  const onayla = async () => {
    const t = onaylanacak;
    setOnaylanacak(null);
    try {
      await transferService.onayla(t.id);
      toast.basari(t.ogrenciAd + ' ' + t.hedefGrupAd + ' grubuna transfer edildi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
      yukle();
    }
  };

  const reddet = async () => {
    const t = reddedilecek;
    setReddedilecek(null);
    try {
      await transferService.reddet(t.id, redNedeni.trim());
      toast.basari('Transfer talebi reddedildi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
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
    <div>
      <PageHeader kicker="Okul" baslik="Transferler" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Öğrenci transfer talepleri</h4>
          <Seg className="ml-auto">
            <SegOpt active={filtre === 'BEKLIYOR'} onClick={() => setFiltre('BEKLIYOR')}>Bekleyenler</SegOpt>
            <SegOpt active={filtre === ''} onClick={() => setFiltre('')}>Tümü</SegOpt>
          </Seg>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Öğrenci</Th>
                <Th>Transfer</Th>
                <Th>Talep eden</Th>
                <Th>Talep tarihi</Th>
                <Th>Açıklama</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {liste.length === 0 ? (
                <Bos colSpan={7}>{filtre ? 'Onay bekleyen transfer talebi yok.' : 'Transfer talebi bulunamadı.'}</Bos>
              ) : (
                liste.map((t) => {
                  const d = TRANSFER_DURUM[t.durum];
                  return (
                    <Tr key={t.id}>
                      <Td tam className="font-semibold sm:font-normal">
                        <Link to={'/ogrenciler/' + t.ogrenciId} className="text-accent-700 underline-offset-2 hover:underline">
                          {t.ogrenciAd}
                        </Link>
                      </Td>
                      <Td etiket="Transfer">{nereden(t)}</Td>
                      <Td etiket="Talep eden">{t.talepEden || <span className="text-ink/55">—</span>}</Td>
                      <Td etiket="Talep tarihi" className="whitespace-nowrap">{tarihSaat(t.talepTarihi)}</Td>
                      <Td etiket="Açıklama" className="max-w-[260px]">
                        {t.aciklama || <span className="text-ink/55">—</span>}
                      </Td>
                      <Td etiket="Durum">
                        <Tag tone={d.tone}>{d.ad}</Tag>
                        {t.kararTarihi ? (
                          <div className="mt-1 text-[13px] text-ink/60">
                            {tarihSaat(t.kararTarihi)}
                            {t.kararVeren ? ' · ' + t.kararVeren : ''}
                          </div>
                        ) : null}
                        {t.kararNotu ? (
                          <div className="mt-1 text-[13px]">
                            <span className="font-semibold text-ink/70">{t.durum === 'REDDEDILDI' ? 'Red nedeni:' : 'Not:'}</span>{' '}
                            <span className="text-ink/80">{t.kararNotu}</span>
                          </div>
                        ) : null}
                      </Td>
                      <Td tam className="whitespace-nowrap text-right">
                        {t.durum === 'BEKLIYOR' ? (
                          <>
                            <Button variant="cerceve" size="sm" onClick={() => setOnaylanacak(t)}>
                              <Check size={14} strokeWidth={1.5} />
                              <span>Onayla</span>
                            </Button>
                            <Button
                              variant="cerceveHata"
                              size="sm"
                              className="ml-1"
                              onClick={() => {
                                setRedNedeni('');
                                setReddedilecek(t);
                              }}
                            >
                              <X size={14} strokeWidth={1.5} />
                              <span>Reddet</span>
                            </Button>
                          </>
                        ) : null}
                      </Td>
                    </Tr>
                  );
                })
              )}
            </tbody>
          </Table>
        )}
      </Card>

      <ConfirmDialog
        open={!!onaylanacak}
        title="Transfer onaylansın mı?"
        description="Onaylanınca öğrenci hemen yeni grubuna geçer; transfer öğrencinin geçmişine işlenir."
        isim={onaylanacak ? onaylanacak.ogrenciAd + ' · ' + (onaylanacak.kaynakGrupAd || 'Grup yok') + ' → ' + onaylanacak.hedefGrupAd : ''}
        confirmLabel="Onayla"
        onConfirm={onayla}
        onCancel={() => setOnaylanacak(null)}
      />
      <ConfirmDialog
        open={!!reddedilecek}
        title="Talep reddedilsin mi?"
        description="Öğrenci mevcut grubunda kalır."
        isim={reddedilecek ? reddedilecek.ogrenciAd + ' · ' + (reddedilecek.kaynakGrupAd || 'Grup yok') + ' → ' + reddedilecek.hedefGrupAd : ''}
        confirmLabel="Reddet"
        tehlikeli
        onConfirm={reddet}
        onCancel={() => setReddedilecek(null)}
      >
        <Textarea
          placeholder="Red nedeni (isteğe bağlı)"
          maxLength={500}
          value={redNedeni}
          onChange={(e) => setRedNedeni(e.target.value)}
          aria-label="Red nedeni"
        />
      </ConfirmDialog>
    </div>
  );
}
