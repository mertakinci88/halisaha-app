import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Layers, Package, Pencil, Plus, Shapes, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import EnvanterGrupluListe from '@/components/EnvanterGrupluListe';
import { envanterService, envanterSetService, envanterTurService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { tl } from '@/lib/format';

export default function EnvanterListePage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const setlerSekmesi = params.get('sekme') === 'setler';
  const turlerSekmesi = params.get('sekme') === 'turler';
  const [liste, setListe] = useState([]);
  const [setler, setSetler] = useState([]);
  const [turler, setTurler] = useState([]);
  const [silinecekTur, setSilinecekTur] = useState(null);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);
  const [silinecekSet, setSilinecekSet] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([envanterService.list(), envanterSetService.list(), envanterTurService.list()])
      .then(([e, s, t]) => {
        setListe(e);
        setSetler(s);
        setTurler(t);
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

  const turSil = async () => {
    const t = silinecekTur;
    setSilinecekTur(null);
    try {
      await envanterTurService.remove(t.id);
      toast.basari('Envanter türü silindi.');
      yukle();
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const sekmeSec = (sekme) => setParams(sekme === 'setler' || sekme === 'turler' ? { sekme } : {}, { replace: true });

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Envanter" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Seg className="mb-3.5">
        <SegOpt active={!setlerSekmesi && !turlerSekmesi} onClick={() => sekmeSec('envanterler')}>
          <Package size={16} strokeWidth={1.5} />
          <span>Envanterler</span>
        </SegOpt>
        <SegOpt active={setlerSekmesi} onClick={() => sekmeSec('setler')}>
          <Layers size={16} strokeWidth={1.5} />
          <span>Setler</span>
        </SegOpt>
        <SegOpt active={turlerSekmesi} onClick={() => sekmeSec('turler')}>
          <Shapes size={16} strokeWidth={1.5} />
          <span>Türler</span>
        </SegOpt>
      </Seg>

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">{turlerSekmesi ? 'Envanter türleri' : setlerSekmesi ? 'Set listesi' : 'Envanter listesi'}</h4>
          {turlerSekmesi ? (
            <Button variant="primary" className="ml-auto" onClick={() => navigate('/envanter/tur/yeni')}>
              <Plus size={16} strokeWidth={1.5} />
              <span>Yeni tür</span>
            </Button>
          ) : setlerSekmesi ? (
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
        ) : turlerSekmesi ? (
          <Table kart>
            <thead>
              <tr>
                <Th>Envanter</Th>
                <Th>Beden</Th>
                <Th>Birim</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {turler.length === 0 ? (
                <Bos colSpan={4}>Henüz envanter türü tanımlı değil. "Yeni tür" ile ekleyin.</Bos>
              ) : (
                turler.map((t) => (
                  <Tr key={t.id}>
                    <Td tam className="font-semibold sm:font-normal">{t.ad}</Td>
                    <Td tam etiket="Beden">
                      {t.bedenli ? (
                        <div className="flex flex-wrap gap-1">
                          {t.bedenler.map((b) => (
                            <Tag key={b} tone="outline">{b}</Tag>
                          ))}
                        </div>
                      ) : (
                        'Bedensiz'
                      )}
                    </Td>
                    <Td etiket="Birim">{t.birim === 'çift' ? 'Çift' : 'Adet'}</Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="cerceve" size="sm" onClick={() => navigate('/envanter/tur/' + t.id)}>
                        <Pencil size={14} strokeWidth={1.5} />
                        <span>Güncelle</span>
                      </Button>
                      <Button variant="cerceveHata" size="sm" className="ml-1" onClick={() => setSilinecekTur(t)}>
                        <Trash2 size={14} strokeWidth={1.5} />
                        <span>Sil</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>
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
          <EnvanterGrupluListe liste={liste} onGuncelle={(e) => navigate('/envanter/' + e.id)} onSil={setSilinecek} />
        )}
      </Card>

      <ConfirmDialog
        open={!!silinecekTur}
        title="Envanter türü silinsin mi?"
        description="Bu türden envanter kaydı varsa silinemez."
        isim={silinecekTur ? silinecekTur.ad : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={turSil}
        onCancel={() => setSilinecekTur(null)}
      />
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
