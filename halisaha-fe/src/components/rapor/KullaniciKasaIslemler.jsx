import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { kisaTarih, tl } from '@/lib/format';
import { Aciklama, Kutu, Ozet, OzetIzgara, RaporDurumu, useRapor } from './RaporBilesenleri';
import { raporService } from '@/services';

const ROL = { ADMIN: 'Yönetici', PERSONEL: 'Personel', SUPER_ADMIN: 'Süper yönetici' };

/** Havale / EFT ile alınan paranın hangi hesaba (bankaya) yattığı: her hesap için bir satır. */
function HavaleHesaplari({ hesaplar }) {
  if (!hesaplar || hesaplar.length === 0) return <span className="text-ink/45">—</span>;
  return (
    <div className="flex flex-col gap-0.5">
      {hesaplar.map((h) => (
        <div key={h.hesapId ?? 'yok'} className="flex justify-between gap-3 text-[14px]">
          <span className={h.hesapId == null ? 'text-ink/55' : ''}>{h.hesapAd}</span>
          <span className="tabular-nums">{tl(h.tutar)}</span>
        </div>
      ))}
    </div>
  );
}

export function KisiHucresi({ ad, rol }) {
  return (
    <div className="flex flex-wrap items-center gap-x-2">
      <span className={ad === 'Belirtilmemiş' ? 'text-ink/60' : ''}>{ad}</span>
      {rol ? <span className="text-[12.5px] text-ink/50">{ROL[rol] || rol}</span> : null}
    </div>
  );
}

/** Her kullanıcının dönemde aldığı tahsilat ve yaptığı iade; tek gün seçiliyse gün sonu özeti. */
export function KasaGorunumu({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.kullaniciKasa({ baslangic, bitis }), [baslangic, bitis]);
  const tekGun = baslangic === bitis;

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        const t = v.toplam;
        return (
          <>
            {tekGun ? <Aciklama>Gün sonu özeti · {kisaTarih(baslangic)}</Aciklama> : null}
            <OzetIzgara className="lg:grid-cols-5">
              <Ozet
                vurgu
                etiket="Net tahsilat"
                deger={tl(t.net)}
                alt={'Tahsilat ' + tl(t.tahsilat) + ' · İade ' + tl(t.iade)}
                bilgi="Seçilen dönemde tüm kullanıcıların aldığı paradan yapılan iadelerin düşülmüş hali."
              />
              <Ozet etiket="Nakit" deger={tl(t.nakit)} bilgi="Nakit olarak alınan toplam para." />
              <Ozet etiket="Kart" deger={tl(t.kart)} bilgi="Kartla alınan toplam para." />
              <Ozet etiket="Havale" deger={tl(t.havale)} bilgi="Havale / EFT ile alınan toplam para. Hangi hesaba yattığı aşağıdaki tabloda “Havalenin yattığı hesap” sütununda görünür." />
              <Ozet
                etiket="Kasadaki nakit"
                deger={tl(t.netNakit)}
                bilgi="Nakit tahsilattan nakit iadeler düşülmüştür. Kasada bu kadar nakit olmalıdır (kasaya önceden konulan para dahil değildir)."
              />
            </OzetIzgara>

            <Kutu
              baslik={tekGun ? 'Kullanıcılara göre gün sonu' : 'Kullanıcılara göre tahsilat'}
              bilgi="Parayı kim aldıysa onun adına yazılır. Her satır, o kullanıcının dönemde aldığı tahsilat ile yaptığı iadeyi gösterir. “Havalenin yattığı hesap” sütunu, havale ile alınan paranın hangi hesaba (bankaya) yattığını gösterir (iadeler dahil değildir). Kullanıcı bilgisi tutulmadan önceki kayıtlar “Belirtilmemiş” satırında toplanır."
            >
              <Table kart>
                <thead>
                  <tr>
                    <Th>Kullanıcı</Th>
                    <Th className="text-right">Rezervasyon</Th>
                    <Th className="text-right">Öğrenci</Th>
                    <Th className="text-right">Envanter</Th>
                    <Th className="text-right">Abonelik</Th>
                    <Th className="text-right">Tahsilat</Th>
                    <Th className="text-right">Nakit</Th>
                    <Th className="text-right">Kart</Th>
                    <Th className="text-right">Havale</Th>
                    <Th className="min-w-[190px]">Havalenin yattığı hesap</Th>
                    <Th className="text-right">İade</Th>
                    <Th className="text-right">Net</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.satirlar.length === 0 ? <Bos colSpan={12}>Kullanıcı bulunamadı.</Bos> : null}
                  {v.satirlar.map((s) => (
                    <Tr key={s.kullaniciId ?? 'belirsiz'}>
                      <Td tam className="font-semibold sm:font-normal"><KisiHucresi ad={s.ad} rol={s.rol} /></Td>
                      <Td etiket="Rezervasyon" className="whitespace-nowrap text-right tabular-nums">{tl(s.rezervasyon)}</Td>
                      <Td etiket="Öğrenci" className="whitespace-nowrap text-right tabular-nums">{tl(s.ogrenci)}</Td>
                      <Td etiket="Envanter" className="whitespace-nowrap text-right tabular-nums">{tl(s.envanter)}</Td>
                      <Td etiket="Abonelik" className="whitespace-nowrap text-right tabular-nums">{tl(s.abonelik)}</Td>
                      <Td etiket="Tahsilat" className="whitespace-nowrap text-right font-semibold tabular-nums">{tl(s.tahsilat)}</Td>
                      <Td etiket="Nakit" className="whitespace-nowrap text-right tabular-nums">{tl(s.nakit)}</Td>
                      <Td etiket="Kart" className="whitespace-nowrap text-right tabular-nums">{tl(s.kart)}</Td>
                      <Td etiket="Havale" className="whitespace-nowrap text-right tabular-nums">{tl(s.havale)}</Td>
                      <Td etiket="Havalenin yattığı hesap"><HavaleHesaplari hesaplar={s.havaleHesaplari} /></Td>
                      <Td etiket="İade" className="whitespace-nowrap text-right tabular-nums">{Number(s.iade) > 0 ? '−' + tl(s.iade) : '—'}</Td>
                      <Td etiket="Net" className="whitespace-nowrap text-right font-bold tabular-nums">{tl(s.net)}</Td>
                    </Tr>
                  ))}
                  <tr>
                    <Td className="font-bold">Toplam</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.rezervasyon)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.ogrenci)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.envanter)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.abonelik)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.tahsilat)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.nakit)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.kart)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.havale)}</Td>
                    <Td><HavaleHesaplari hesaplar={t.havaleHesaplari} /></Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{Number(t.iade) > 0 ? '−' + tl(t.iade) : '—'}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(t.net)}</Td>
                  </tr>
                </tbody>
              </Table>
            </Kutu>
          </>
        );
      }}
    </RaporDurumu>
  );
}

/** Her kullanıcının dönemde yaptığı işlemlerin sayısı. */
export function IslemlerGorunumu({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.kullaniciIslemler({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => (
        <Kutu
          baslik="Kullanıcıların yaptığı işlemler"
          bilgi="Dönemde her kullanıcının kaç rezervasyon girdiği, kaç tane iptal ettiği, kaç öğrenci ödemesi aldığı, kaç envanter satışı ve abonelik kaydettiği, kaç gün yoklama aldığı, kaç iade yaptığı ve kaç transfer talebi açtığı. Abonelikten oluşan haftalık rezervasyonlar ve abonelik iptali sonucu iptal edilen haftalar sayılmaz."
        >
          <Table kart>
            <thead>
              <tr>
                <Th>Kullanıcı</Th>
                <Th className="text-right">Rezervasyon</Th>
                <Th className="text-right">Rezervasyon iptali</Th>
                <Th className="text-right">Öğrenci ödemesi</Th>
                <Th className="text-right">Envanter satışı</Th>
                <Th className="text-right">Abonelik</Th>
                <Th className="text-right">Yoklama (gün)</Th>
                <Th className="text-right">İade</Th>
                <Th className="text-right">Transfer talebi</Th>
              </tr>
            </thead>
            <tbody>
              {v.satirlar.length === 0 ? <Bos colSpan={9}>Kullanıcı bulunamadı.</Bos> : null}
              {v.satirlar.map((s) => (
                <Tr key={s.kullaniciId ?? 'belirsiz'}>
                  <Td tam className="font-semibold sm:font-normal"><KisiHucresi ad={s.ad} rol={s.rol} /></Td>
                  <Td etiket="Rezervasyon" className="text-right tabular-nums">{s.rezervasyon}</Td>
                  <Td etiket="Rezervasyon iptali" className="text-right tabular-nums">{s.rezervasyonIptal}</Td>
                  <Td etiket="Öğrenci ödemesi" className="text-right tabular-nums">{s.ogrenciOdeme}</Td>
                  <Td etiket="Envanter satışı" className="text-right tabular-nums">{s.envanterSatis}</Td>
                  <Td etiket="Abonelik" className="text-right tabular-nums">{s.abonelik}</Td>
                  <Td etiket="Yoklama (gün)" className="text-right tabular-nums">{s.yoklamaGunu}</Td>
                  <Td etiket="İade" className="text-right tabular-nums">{s.iade}</Td>
                  <Td etiket="Transfer talebi" className="text-right tabular-nums">{s.transferTalebi}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </Kutu>
      )}
    </RaporDurumu>
  );
}
