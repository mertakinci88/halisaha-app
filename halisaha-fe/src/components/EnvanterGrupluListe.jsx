import { useMemo, useState } from 'react';
import { ChevronDown, ChevronRight, Pencil, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr } from '@/components/ui/table';
import { stokDurumu, tl } from '@/lib/format';
import { miktar } from '@/lib/envanter';
import { cn } from '@/lib/utils';

/**
 * Envanter listesi, türe göre gruplu: her tür bir başlık (beden sayısı, toplam stok, stok uyarıları), bedenler altında.
 * Tüm türler kapalı gelir; başlığa tıklayınca açılır / kapanır.
 * Liste tür ve beden sırasıyla gelmelidir.
 */
export default function EnvanterGrupluListe({ liste, onGuncelle, onSil }) {
  const [turId, setTurId] = useState('');
  const [sadeceSorunlu, setSadeceSorunlu] = useState(false);
  const [acik, setAcik] = useState({}); // { turId: true|false } kullanıcının elle açıp kapattıkları

  const gruplar = useMemo(() => {
    const harita = new Map();
    for (const e of liste) {
      if (!harita.has(e.turId)) harita.set(e.turId, { turId: e.turId, ad: e.turAd, bedenli: e.bedenli, birim: e.birim, kayitlar: [] });
      harita.get(e.turId).kayitlar.push(e);
    }
    return [...harita.values()].map((g) => ({
      ...g,
      toplam: g.kayitlar.reduce((t, e) => t + e.adet, 0),
      tukenen: g.kayitlar.filter((e) => stokDurumu(e) === 'TUKENDI').length,
      az: g.kayitlar.filter((e) => stokDurumu(e) === 'AZ').length,
    }));
  }, [liste]);

  const sorunluSayisi = gruplar.reduce((t, g) => t + g.tukenen + g.az, 0);
  const gorunen = gruplar
    .filter((g) => !turId || String(g.turId) === turId)
    .map((g) => (sadeceSorunlu ? { ...g, kayitlar: g.kayitlar.filter((e) => stokDurumu(e)) } : g))
    .filter((g) => g.kayitlar.length > 0);

  const acikMi = (g) => acik[g.turId] ?? false;
  const hepsiniAc = (deger) => setAcik(Object.fromEntries(gorunen.map((g) => [g.turId, deger])));
  const hepsiAcik = gorunen.length > 0 && gorunen.every(acikMi);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2.5">
        <Select value={turId} onChange={(e) => setTurId(e.target.value)} aria-label="Envanter türü" className="!w-auto min-w-[200px] max-w-full">
          <option value="">Tüm envanterler</option>
          {gruplar.map((g) => (
            <option key={g.turId} value={g.turId}>{g.ad}</option>
          ))}
        </Select>
        <Seg>
          <SegOpt active={!sadeceSorunlu} onClick={() => setSadeceSorunlu(false)}>Tümü</SegOpt>
          <SegOpt active={sadeceSorunlu} onClick={() => setSadeceSorunlu(true)}>
            Stoğu az olanlar{sorunluSayisi > 0 ? ' (' + sorunluSayisi + ')' : ''}
          </SegOpt>
        </Seg>
        {gorunen.length > 1 ? (
          <Button variant="cerceve" size="sm" className="ml-auto" onClick={() => hepsiniAc(!hepsiAcik)}>
            {hepsiAcik ? 'Hepsini kapat' : 'Hepsini aç'}
          </Button>
        ) : null}
      </div>

      {gorunen.length === 0 ? (
        <div className="px-2 py-6 text-center text-ink/55">
          {liste.length === 0 ? 'Envanter kaydı bulunamadı.' : 'Bu filtreye uyan envanter yok.'}
        </div>
      ) : (
        gorunen.map((g) => {
          const ac = acikMi(g);
          return (
            <div key={g.turId} className="border border-divider">
              <button
                type="button"
                aria-expanded={ac}
                onClick={() => setAcik((p) => ({ ...p, [g.turId]: !ac }))}
                className={cn(
                  'flex w-full cursor-pointer flex-wrap items-center gap-x-3 gap-y-1.5 bg-transparent px-3 py-2.5 text-left hover:bg-ink/4',
                  ac && 'border-b border-divider',
                )}
              >
                {ac ? <ChevronDown size={18} strokeWidth={1.5} /> : <ChevronRight size={18} strokeWidth={1.5} />}
                <span className="font-heading text-[18px]">{g.ad}</span>
                <span className="text-[14px] text-ink/60">
                  {g.bedenli ? g.kayitlar.length + ' beden · ' : ''}
                  Toplam {g.birim === 'çift' ? g.toplam + ' çift' : g.toplam + ' adet'}
                </span>
                <span className="flex flex-wrap gap-1.5 sm:ml-auto">
                  {g.tukenen > 0 ? <Tag tone="danger">{g.tukenen} tükendi</Tag> : null}
                  {g.az > 0 ? <Tag tone="outline">{g.az} stok az</Tag> : null}
                </span>
              </button>

              {ac ? (
                <div className="px-3 pb-2">
                  <Table kart>
                    <thead>
                      <tr>
                        <Th>Beden</Th>
                        <Th className="text-right">Satış fiyatı</Th>
                        <Th className="text-right">Adet</Th>
                        <Th>Uyarı</Th>
                        <Th />
                      </tr>
                    </thead>
                    <tbody>
                      {g.kayitlar.map((e) => (
                        <Tr key={e.id}>
                          <Td tam className="font-semibold sm:font-normal">{e.bedenli ? e.beden : 'Standart'}</Td>
                          <Td etiket="Satış fiyatı" className="text-right tabular-nums">{tl(e.satisFiyati)}</Td>
                          <Td etiket="Adet" className="text-right tabular-nums">{miktar(e.birim, e.adet)}</Td>
                          <Td>
                            {stokDurumu(e) === 'TUKENDI' ? (
                              <Tag tone="danger">Tükendi</Tag>
                            ) : stokDurumu(e) === 'AZ' ? (
                              <Tag tone="outline">Stok az</Tag>
                            ) : null}
                          </Td>
                          <Td tam className="whitespace-nowrap text-right">
                            <Button variant="cerceve" size="sm" onClick={() => onGuncelle(e)}>
                              <Pencil size={14} strokeWidth={1.5} />
                              <span>Güncelle</span>
                            </Button>
                            <Button variant="cerceveHata" size="sm" className="ml-1" onClick={() => onSil(e)}>
                              <Trash2 size={14} strokeWidth={1.5} />
                              <span>Sil</span>
                            </Button>
                          </Td>
                        </Tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              ) : null}
            </div>
          );
        })
      )}
    </div>
  );
}
