import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil, Plus, Trash2, Users } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { grupService, ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function GrupListePage() {
  const navigate = useNavigate();
  const [gruplar, setGruplar] = useState([]);
  const [ogrenciler, setOgrenciler] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([grupService.list(), ogrenciService.list({ boyut: 1000 })])
      .then(([g, o]) => {
        setGruplar(g);
        setOgrenciler(o.icerik);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  // Grubu seçince öğrenciler sayfası o grubun öğrencileriyle açılır
  const ogrencilereGit = (g) => navigate('/ogrenciler?grup=' + g.id);

  const sil = async () => {
    const g = silinecek;
    setSilinecek(null);
    try {
      await grupService.remove(g.id);
      toast.basari('Grup silindi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    }
  };

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Gruplar" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Grup listesi</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/gruplar/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni grup</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Grup adı</Th>
                <Th className="text-right">Öğrenci sayısı</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {gruplar.length === 0 ? (
                <Bos colSpan={3}>Grup kaydı bulunamadı.</Bos>
              ) : (
                gruplar.map((g) => {
                  const ogrenciSayisi = ogrenciler.filter((o) => o.grupId === g.id).length;
                  return (
                  <Tr key={g.id} className={ogrenciSayisi > 0 ? 'cursor-pointer' : undefined} onClick={ogrenciSayisi > 0 ? () => ogrencilereGit(g) : undefined}>
                    <Td tam className="font-semibold sm:font-normal">{g.ad}</Td>
                    <Td etiket="Öğrenci sayısı" className="text-right tabular-nums">
                      {ogrenciSayisi}
                    </Td>
                    <Td tam className="text-right">
                      <div className="flex justify-end gap-1 whitespace-nowrap max-sm:justify-start">
                        {ogrenciSayisi > 0 ? (
                          <Button variant="cerceve" size="sm" onClick={(e) => { e.stopPropagation(); ogrencilereGit(g); }}>
                            <Users size={14} strokeWidth={1.5} />
                            <span>Öğrenciler</span>
                          </Button>
                        ) : null}
                        <Button variant="cerceve" size="sm" onClick={(e) => { e.stopPropagation(); navigate('/gruplar/' + g.id); }}>
                          <Pencil size={14} strokeWidth={1.5} />
                          <span>Güncelle</span>
                        </Button>
                        <Button variant="cerceveHata" size="sm" onClick={(e) => { e.stopPropagation(); setSilinecek(g); }}>
                          <Trash2 size={14} strokeWidth={1.5} />
                          <span>Sil</span>
                        </Button>
                      </div>
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
        open={!!silinecek}
        title="Grup silinsin mi?"
        description="Bu işlem geri alınamaz."
        isim={silinecek?.ad}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
