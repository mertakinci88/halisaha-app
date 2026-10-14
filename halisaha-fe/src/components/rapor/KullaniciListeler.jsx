import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { Select } from '@/components/ui/input';
import { kisaTarih, ODEME_YONTEMI_LABEL, tarihSaat, tl } from '@/lib/format';
import { raporService } from '@/services';
import { Aciklama, Kutu, Ozet, OzetIzgara, RaporDurumu, useRapor } from './RaporBilesenleri';

const KAYNAK = { REZERVASYON: 'Rezervasyon', OGRENCI: 'Öğrenci ödemesi', ENVANTER: 'Envanter satışı', ABONELIK: 'Abonelik' };

const KesildiNotu = ({ kesildi }) =>
  kesildi ? <Aciklama>İlk 500 kayıt gösteriliyor; daha dar bir tarih aralığı seçerek diğerlerine ulaşabilirsiniz.</Aciklama> : null;

/** İade yapılan ödemeler: kaynağa göre toplamlar ve tek tek iadeler. */
export function IadelerGorunumu({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.kullaniciIadeler({ baslangic, bitis }), [baslangic, bitis]);
  return (
    <RaporDurumu rapor={rapor}>
      {(v) => (
        <>
          <OzetIzgara>
            <Ozet vurgu etiket="Toplam iade" deger={tl(v.toplam)} bilgi="Seçilen dönemde geri verilen toplam para (iade tarihine göre)." />
            {v.kaynaklar.map((k) => (
              <Ozet key={k.kaynak} etiket={KAYNAK[k.kaynak]} deger={tl(k.toplam)} alt={k.adet + ' iade'} />
            ))}
          </OzetIzgara>
          <Kutu baslik="İadeler" bilgi="Her iade; kim yaptığı, kime ait olduğu, tutarı, şekli ve nedeniyle listelenir.">
            <Table kart>
              <thead>
                <tr>
                  <Th>Tarih</Th>
                  <Th>Ne için</Th>
                  <Th>Kime ait</Th>
                  <Th className="text-right">Tutar</Th>
                  <Th>Şekil</Th>
                  <Th>İadeyi yapan</Th>
                  <Th>Neden</Th>
                </tr>
              </thead>
              <tbody>
                {v.satirlar.length === 0 ? <Bos colSpan={7}>Bu dönemde iade yok.</Bos> : null}
                {v.satirlar.map((s) => (
                  <Tr key={s.id}>
                    <Td etiket="Tarih" className="whitespace-nowrap">{kisaTarih(s.tarih)}</Td>
                    <Td etiket="Ne için">{KAYNAK[s.kaynak]}</Td>
                    <Td etiket="Kime ait">{s.ilgili || '—'}</Td>
                    <Td etiket="Tutar" className="whitespace-nowrap text-right tabular-nums">{tl(s.tutar)}</Td>
                    <Td etiket="Şekil">{ODEME_YONTEMI_LABEL[s.iadeSekli]}</Td>
                    <Td etiket="İadeyi yapan">{s.kullanici || <span className="text-ink/55">Kayıt yok</span>}</Td>
                    <Td etiket="Neden">{s.neden || '—'}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <KesildiNotu kesildi={v.kesildi} />
        </>
      )}
    </RaporDurumu>
  );
}

/** İptal edilen rezervasyon ve abonelikler: kim, ne zaman, neden. */
export function IptallerGorunumu({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.kullaniciIptaller({ baslangic, bitis }), [baslangic, bitis]);
  return (
    <RaporDurumu rapor={rapor}>
      {(v) => (
        <>
          <OzetIzgara>
            <Ozet
              vurgu
              etiket="Toplam iptal"
              deger={v.toplam}
              bilgi="Seçilen dönemde iptal edilen rezervasyon ve abonelik sayısı. Bir abonelik iptal edilince gelecek haftalarının iptali ayrıca sayılmaz."
            />
          </OzetIzgara>
          <Kutu baslik="Kullanıcılara göre iptaller" bilgi="Hangi kullanıcının kaç rezervasyon ve abonelik iptal ettiği. “Belirtilmemiş”, iptal eden bilgisi tutulmadan önce iptal edilen kayıtlardır.">
            <Table kart>
              <thead>
                <tr>
                  <Th>Kullanıcı</Th>
                  <Th className="text-right">Rezervasyon</Th>
                  <Th className="text-right">Abonelik</Th>
                </tr>
              </thead>
              <tbody>
                {v.kisiler.length === 0 ? <Bos colSpan={3}>Kullanıcı bulunamadı.</Bos> : null}
                {v.kisiler.map((k) => (
                  <Tr key={k.kullaniciId ?? 'belirsiz'}>
                    <Td etiket="Kullanıcı">{k.ad}</Td>
                    <Td etiket="Rezervasyon" className="text-right tabular-nums">{k.rezervasyon}</Td>
                    <Td etiket="Abonelik" className="text-right tabular-nums">{k.abonelik}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <Kutu baslik="İptal listesi" bilgi="Her iptalin kim tarafından, ne zaman ve hangi nedenle yapıldığı. Eski kayıtlarda bu bilgi yoksa “Kayıt yok” yazar.">
            <Table kart>
              <thead>
                <tr>
                  <Th>İptal zamanı</Th>
                  <Th>Tür</Th>
                  <Th>Müşteri</Th>
                  <Th>Saha</Th>
                  <Th>Seans</Th>
                  <Th>İptal eden</Th>
                  <Th>Neden</Th>
                </tr>
              </thead>
              <tbody>
                {v.satirlar.length === 0 ? <Bos colSpan={7}>Bu dönemde iptal yok.</Bos> : null}
                {v.satirlar.map((s) => (
                  <Tr key={s.tur + s.id}>
                    <Td etiket="İptal zamanı" className="whitespace-nowrap">{s.iptalTarih ? tarihSaat(s.iptalTarih) : <span className="text-ink/55">Kayıt yok</span>}</Td>
                    <Td etiket="Tür">{s.tur === 'ABONELIK' ? 'Abonelik' : 'Rezervasyon'}</Td>
                    <Td etiket="Müşteri">
                      <Link to={(s.tur === 'ABONELIK' ? '/aboneler/' : '/rezervasyon/') + s.id} className="text-accent-700 underline">{s.ad}</Link>
                    </Td>
                    <Td etiket="Saha">{s.saha}</Td>
                    <Td etiket="Seans" className="whitespace-nowrap">{s.seans ? (s.tur === 'ABONELIK' ? kisaTarih(s.seans) : tarihSaat(s.seans)) : '—'}</Td>
                    <Td etiket="İptal eden">{s.iptalEden || <span className="text-ink/55">Kayıt yok</span>}</Td>
                    <Td etiket="Neden">{s.neden || '—'}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <KesildiNotu kesildi={v.kesildi} />
        </>
      )}
    </RaporDurumu>
  );
}

/** Yoklamayı kim, hangi gün, hangi grup için aldı. */
export function YoklamaGorunumu({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.kullaniciYoklama({ baslangic, bitis }), [baslangic, bitis]);
  return (
    <RaporDurumu rapor={rapor}>
      {(v) => (
        <>
          <Kutu baslik="Yoklamayı alanlar" bilgi="Dönemde yoklama alan her kullanıcının kaç gün yoklama aldığı, kaç farklı grup için aldığı ve son yoklamasının tarihi.">
            <Table kart>
              <thead>
                <tr>
                  <Th>Kullanıcı</Th>
                  <Th className="text-right">Yoklama alınan gün</Th>
                  <Th className="text-right">Grup sayısı</Th>
                  <Th>Son yoklama</Th>
                </tr>
              </thead>
              <tbody>
                {v.kullanicilar.length === 0 ? <Bos colSpan={4}>Bu dönemde yoklama alınmamış.</Bos> : null}
                {v.kullanicilar.map((k) => (
                  <Tr key={k.kullaniciId ?? 'belirsiz'}>
                    <Td etiket="Kullanıcı">{k.ad}</Td>
                    <Td etiket="Yoklama alınan gün" className="text-right tabular-nums">{k.yoklamaGunu}</Td>
                    <Td etiket="Grup sayısı" className="text-right tabular-nums">{k.grupSayisi}</Td>
                    <Td etiket="Son yoklama">{kisaTarih(k.sonYoklama)}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <Kutu baslik="Yoklama kayıtları" bilgi="Her gün, her grup için yoklamayı kimin aldığı ve kaç öğrencinin geldiği, gelmediği, izinli olduğu.">
            <Table kart>
              <thead>
                <tr>
                  <Th>Tarih</Th>
                  <Th>Grup</Th>
                  <Th>Yoklamayı alan</Th>
                  <Th className="text-right">Öğrenci</Th>
                  <Th className="text-right">Geldi</Th>
                  <Th className="text-right">Gelmedi</Th>
                  <Th className="text-right">İzinli</Th>
                </tr>
              </thead>
              <tbody>
                {v.gunler.length === 0 ? <Bos colSpan={7}>Bu dönemde yoklama alınmamış.</Bos> : null}
                {v.gunler.map((g, i) => (
                  <Tr key={g.tarih + '-' + (g.grupId ?? 'g') + '-' + i}>
                    <Td etiket="Tarih" className="whitespace-nowrap">{kisaTarih(g.tarih)}</Td>
                    <Td etiket="Grup">{g.grupAd}</Td>
                    <Td etiket="Yoklamayı alan">{g.kullanici}</Td>
                    <Td etiket="Öğrenci" className="text-right tabular-nums">{g.ogrenci}</Td>
                    <Td etiket="Geldi" className="text-right tabular-nums">{g.geldi}</Td>
                    <Td etiket="Gelmedi" className="text-right tabular-nums">{g.gelmedi}</Td>
                    <Td etiket="İzinli" className="text-right tabular-nums">{g.izinli}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <KesildiNotu kesildi={v.kesildi} />
        </>
      )}
    </RaporDurumu>
  );
}

/** "Mozilla/5.0 (iPhone; ...) ... Safari" -> "Safari · iPhone" */
const tarayiciOzet = (ua) => {
  if (!ua) return '—';
  const tarayici = /Edg\//.test(ua) ? 'Edge' : /OPR\//.test(ua) ? 'Opera' : /Firefox\//.test(ua) ? 'Firefox' : /Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : 'Tarayıcı';
  const cihaz = /iPhone/.test(ua) ? 'iPhone' : /iPad/.test(ua) ? 'iPad' : /Android/.test(ua) ? 'Android' : /Windows/.test(ua) ? 'Windows' : /Mac OS X/.test(ua) ? 'Mac' : /Linux/.test(ua) ? 'Linux' : '';
  return tarayici + (cihaz ? ' · ' + cihaz : '');
};

const OLAY = {
  GIRIS: { ad: 'Giriş', sinif: 'bg-basari-100 text-basari-800' },
  CIKIS: { ad: 'Çıkış', sinif: 'bg-neutral-100 text-neutral-800' },
  BASARISIZ_GIRIS: { ad: 'Başarısız giriş', sinif: 'bg-red-100 text-red-800' },
};

/** Kim ne zaman giriş / çıkış yaptı, hangi adresten; başarısız denemeler de görünür. */
export function GirisGorunumu({ baslangic, bitis }) {
  const [kullaniciId, setKullaniciId] = useState('');
  const [kullanicilar, setKullanicilar] = useState([]);
  useEffect(() => {
    raporService
      .kullaniciIslemler({ baslangic, bitis })
      .then((r) => setKullanicilar(r.satirlar.filter((s) => s.kullaniciId != null)))
      .catch(() => setKullanicilar([]));
  }, [baslangic, bitis]);
  const rapor = useRapor(
    () => raporService.kullaniciGiris({ baslangic, bitis, kullaniciId: kullaniciId || undefined }),
    [baslangic, bitis, kullaniciId],
  );
  return (
    <>
      <div className="max-w-[320px]">
        <Select value={kullaniciId} onChange={(e) => setKullaniciId(e.target.value)} aria-label="Kullanıcı">
          <option value="">Tüm kullanıcılar</option>
          {kullanicilar.map((k) => (
            <option key={k.kullaniciId} value={k.kullaniciId}>{k.ad}</option>
          ))}
        </Select>
      </div>
      {!kullaniciId ? (
        <Kutu
          baslik="Kullanıcılara göre giriş çıkış"
          bilgi="Dönemde her kullanıcının kaç kez giriş yaptığı; son giriş ve son çıkış zamanları dönemden bağımsız, en güncel olanlardır. Çıkış düğmesine basılmadan kapanan (süresi dolan) oturumlar çıkış sayılmaz; bu yüzden son çıkış, son girişten eski olabilir."
        >
          <Table kart>
            <thead>
              <tr>
                <Th>Kullanıcı</Th>
                <Th className="text-right">Giriş sayısı</Th>
                <Th>Son giriş</Th>
                <Th>Son çıkış</Th>
              </tr>
            </thead>
            <tbody>
              {kullanicilar.length === 0 ? <Bos colSpan={4}>Kullanıcı bulunamadı.</Bos> : null}
              {kullanicilar.map((k) => (
                <Tr key={k.kullaniciId}>
                  <Td etiket="Kullanıcı">{k.ad}</Td>
                  <Td etiket="Giriş sayısı" className="text-right tabular-nums">{k.giris}</Td>
                  <Td etiket="Son giriş" className="whitespace-nowrap">{k.sonGiris ? tarihSaat(k.sonGiris) : <Tag tone="neutral">Kayıt yok</Tag>}</Td>
                  <Td etiket="Son çıkış" className="whitespace-nowrap">{k.sonCikis ? tarihSaat(k.sonCikis) : <Tag tone="neutral">Kayıt yok</Tag>}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </Kutu>
      ) : null}
      <RaporDurumu rapor={rapor}>
        {(v) => (
          <>
            <Kutu
              baslik="Giriş ve çıkış geçmişi"
              bilgi="Her giriş, çıkış ve başarısız giriş denemesi zamanı ve bağlantı adresiyle kaydedilir. Oturum süresi dolduğu için kendiliğinden kapanan oturumlar çıkış olarak görünmez. Hiç tanımadığımız bir kullanıcı adıyla yapılan denemeler bu listede yer almaz."
            >
              <Table kart>
                <thead>
                  <tr>
                    <Th>Zaman</Th>
                    <Th>Kullanıcı</Th>
                    <Th>Olay</Th>
                    <Th>Adres</Th>
                    <Th>Cihaz</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.satirlar.length === 0 ? <Bos colSpan={5}>Bu dönemde kayıt yok.</Bos> : null}
                  {v.satirlar.map((s, i) => (
                    <Tr key={s.zaman + '-' + i}>
                      <Td etiket="Zaman" className="whitespace-nowrap">{tarihSaat(s.zaman)}</Td>
                      <Td etiket="Kullanıcı">{s.ad || s.kullaniciAdi}</Td>
                      <Td etiket="Olay"><Tag className={OLAY[s.olay]?.sinif}>{OLAY[s.olay]?.ad || s.olay}</Tag></Td>
                      <Td etiket="Adres" className="whitespace-nowrap tabular-nums">{s.ip || '—'}</Td>
                      <Td etiket="Cihaz"><span title={s.tarayici || undefined}>{tarayiciOzet(s.tarayici)}</span></Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>
            <KesildiNotu kesildi={v.kesildi} />
          </>
        )}
      </RaporDurumu>
    </>
  );
}
