import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Layers, Package, Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { envanterService, envanterSetService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { stokDurumu, tl } from '@/lib/format';
import { miktar } from '@/lib/envanter';

export default function EnvanterListePage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const setlerSekmesi = params.get('sekme') === 'setler';
  const [liste, setListe] = useState([]);
  const [setler, setSetler] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);
  const [silinecekSet, setSilinecekSet] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([envanterService.list(), envanterSetService.list()])
      .then(([e, s]) => {
        setListe(e);
        setSetler(s);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const sil = async () => {
    const e = silinecek;
    setSilinecek(null);
    try {
      await envanterService.remove(e.id);
      toast.basari('Envanter silindi.');
      yukle();
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const setSil = async () => {
    const s = silinecekSet;
    setSilinecekSet(null);
    try {
      await envanterSetService.remove(s.id);
      toast.basari('Set silindi.');
      yukle();
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const sekmeSec = (sekme) => setParams(sekme === 'setler' ? { sekme } : {}, { replace: true });

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Envanter" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Seg className="mb-3.5">
        <SegOpt active={!setlerSekmesi} onClick={() => sekmeSec('envanterler')}>
          <Package size={16} strokeWidth={1.5} />
          <span>Envanterler</span>
        </SegOpt>
        <SegOpt active={setlerSekmesi} onClick={() => sekmeSec('setler')}>
          <Layers size={16} strokeWidth={1.5} />
          <span>Setler</span>
        </SegOpt>
      </Seg>

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">{setlerSekmesi ? 'Set listesi' : 'Envanter listesi'}</h4>
          {setlerSekmesi ? (
            <Button variant="primary" className="ml-auto" onClick={() => navigate('/envanter/set/yeni')}>
              <Plus size={16} strokeWidth={1.5} />
              <span>Yeni set</span>
            </Button>
          ) : (
            <Button variant="primary" className="ml-auto" onClick={() => navigate('/envanter/yeni')}>
              <Plus size={16} strokeWidth={1.5} />
              <span>Yeni envanter</span>
            </Button>
          )}
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : setlerSekmesi ? (
          <Table kart>
            <thead>
              <tr>
                <Th>Set adı</Th>
                <Th>Beden</Th>
                <Th>Envanterler</Th>
                <Th className="text-right">Set tutarı</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {setler.length === 0 ? (
                <Bos colSpan={5}>Henüz set tanımlı değil.</Bos>
              ) : (
                setler.map((s) => (
                  <Tr key={s.id}>
                    <Td tam className="font-semibold sm:font-normal">{s.ad}</Td>
                    <Td tam etiket="Beden">{s.beden}</Td>
                    <Td tam etiket="Envanterler">
                      <div className="flex flex-wrap gap-1">
                        {s.kalemler.map((k) => (
                          <Tag key={k.envanterId} tone="outline">
                            {k.beden === 'STANDART' ? k.turAd : k.turAd + ' (' + k.beden + ')'}
                          </Tag>
                        ))}
                      </div>
                    </Td>
                    <Td tam etiket="Set tutarı" className="whitespace-nowrap text-right tabular-nums">{tl(s.tutar)}</Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="cerceve" size="sm" onClick={() => navigate('/envanter/set/' + s.id)}>
                        <Pencil size={14} strokeWidth={1.5} />
                        <span>Güncelle</span>
                      </Button>
                      <Button variant="cerceveHata" size="sm" className="ml-1" onClick={() => setSilinecekSet(s)}>
                        <Trash2 size={14} strokeWidth={1.5} />
                        <span>Sil</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Envanter</Th>
                <Th>Beden</Th>
                <Th className="text-right">Satış fiyatı</Th>
                <Th className="text-right">Adet</Th>
                <Th>Uyarı</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {liste.length === 0 ? (
                <Bos colSpan={6}>Envanter kaydı bulunamadı.</Bos>
              ) : (
                liste.map((e) => (
                  <Tr key={e.id}>
                    <Td tam className="font-semibold sm:font-normal">{e.turAd}</Td>
                    <Td etiket="Beden">{e.bedenli ? e.beden : 'Standart'}</Td>
                    <Td etiket="Satış fiyatı" className="text-right tabular-nums">{tl(e.satisFiyati)}</Td>
                    <Td etiket="Adet" className="text-right tabular-nums">{miktar(e.tur, e.adet)}</Td>
                    <Td>
                      {stokDurumu(e) === 'TUKENDI' ? (
                        <Tag tone="danger">Tükendi</Tag>
                      ) : stokDurumu(e) === 'AZ' ? (
                        <Tag tone="outline">Stok az</Tag>
                      ) : null}
                    </Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="cerceve" size="sm" onClick={() => navigate('/envanter/' + e.id)}>
                        <Pencil size={14} strokeWidth={1.5} />
                        <span>Güncelle</span>
                      </Button>
                      <Button variant="cerceveHata" size="sm" className="ml-1" onClick={() => setSilinecek(e)}>
                        <Trash2 size={14} strokeWidth={1.5} />
                        <span>Sil</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>
        )}
      </Card>

      <ConfirmDialog
        open={!!silinecek}
        title="Envanter silinsin mi?"
        description="Bu işlem geri alınamaz. Satışta kullanılan envanter silinemez."
        isim={silinecek ? silinecek.turAd + (silinecek.bedenli ? ' · ' + silinecek.beden : '') : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
      <ConfirmDialog
        open={!!silinecekSet}
        title="Set silinsin mi?"
        description="Set silinir, envanter kayıtları ve geçmiş satışlar etkilenmez."
        isim={silinecekSet ? silinecekSet.ad : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={setSil}
        onCancel={() => setSilinecekSet(null)}
      />
    </div>
  );
}
