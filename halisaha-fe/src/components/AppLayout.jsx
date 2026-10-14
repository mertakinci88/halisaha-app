import { useBildirimAlani } from '@/context/ToastContext';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  CalendarDays,
  ClipboardCheck,
  Building2,
  KeyRound,
  Landmark,
  CupSoda,
  LayoutDashboard,
  Package,
  Layers,
  LogOut,
  ArrowRightLeft,
  BarChart3,
  CalendarClock,
  Plus,
  ShieldCheck,
  Square,
  UserRound,
  Users,
} from 'lucide-react';
import AltBilgi from '@/components/AltBilgi';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/context/AuthContext';
import { cn } from '@/lib/utils';

const NAV = [
  { to: '/panel', ad: 'Panel', Icon: LayoutDashboard, modul: null },
  { to: '/takvim', ad: 'Takvim', Icon: CalendarDays, modul: null },
  { to: '/ogrenciler', ad: 'Öğrenciler', Icon: Users, modul: 'OGRENCI' },
  { to: '/gruplar', ad: 'Gruplar', Icon: Layers, modul: 'GRUP' },
  { to: '/yoklama', ad: 'Yoklama', Icon: ClipboardCheck, modul: 'YOKLAMA' },
  { to: '/sahalar', ad: 'Sahalar', Icon: Square, modul: 'SAHA' },
  { to: '/urunler', ad: 'Kafeterya', Icon: CupSoda, modul: 'URUN' },
  { to: '/envanter', ad: 'Envanter', Icon: Package, modul: 'ENVANTER' },
  { to: '/hesaplar', ad: 'Hesaplar', Icon: Landmark, modul: 'HESAP' },
  { to: '/transferler', ad: 'Transferler', Icon: ArrowRightLeft, modul: 'TRANSFER' },
  { to: '/aboneler', ad: 'Aboneler', Icon: CalendarClock, modul: 'ABONELIK' },
];

export default function AppLayout() {
  const { kullanici, logout, isAdmin, isSuperAdmin, yetkiVar } = useAuth();
  const navigate = useNavigate();

  // Süper admin işletmelere bağlı değildir; yalnızca işletme yönetimini görür
  const nav = isSuperAdmin
    ? [{ to: '/isletmeler', ad: 'İşletmeler', Icon: Building2, modul: null }]
    : NAV.filter((m) => !m.modul || yetkiVar(m.modul));
  if (isAdmin) {
    nav.push({ to: '/raporlar', ad: 'Rapor', Icon: BarChart3, modul: null });
    nav.push({ to: '/personel', ad: 'Personel', Icon: ShieldCheck, modul: null });
  }

  const cikis = async () => {
    await logout();
    navigate('/giris', { replace: true });
  };

  return (
    <div className="flex min-h-full flex-col bg-bg text-ink">
      <header className="border-b border-divider">
        <div className="mx-auto flex max-w-[1360px] flex-wrap items-center gap-5 px-5 py-3.5">
          <div className="mr-auto flex items-baseline gap-2 font-heading text-[20px] font-semibold tracking-[0.01em]">
            <span>HALI SAHA</span>
            <span className="text-accent">YÖNETİM</span>
          </div>
          <div className="flex items-center gap-2 text-[13.5px] text-ink/55">
            <UserRound size={16} strokeWidth={1.5} />
            <span>{kullanici?.adSoyad || kullanici?.kullaniciAdi || 'Kullanıcı'}</span>
            {kullanici?.isletmeAd ? (
              <span className="border-l border-divider pl-2 text-ink/70">{kullanici.isletmeAd}</span>
            ) : null}
          </div>
          <Button variant="gri" onClick={() => navigate('/sifre-degistir')}>
            <KeyRound size={16} strokeWidth={1.5} />
            <span>Şifre</span>
          </Button>
          <Button variant="danger" onClick={cikis}>
            <LogOut size={16} strokeWidth={1.5} />
            <span>Çıkış</span>
          </Button>
        </div>

        <div className="mx-auto max-w-[1360px] px-5">
          <nav className="flex flex-wrap gap-0.5">
            {nav.map(({ to, ad, Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  cn(
                    'inline-flex items-center gap-2 border-b-2 px-3.5 py-2.5 font-heading text-[15.5px] font-semibold uppercase tracking-[0.02em] no-underline',
                    isActive
                      ? 'border-accent text-accent-700'
                      : 'border-transparent text-ink/65 hover:border-accent-300 hover:text-accent',
                  )
                }
              >
                <Icon size={16} strokeWidth={1.5} />
                <span>{ad}</span>
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1360px] flex-1 px-5 pb-16 pt-6">
        <Outlet />
      </main>

      <AltBilgi />
    </div>
  );
}

/** sade: Takvim / Hızlı rezervasyon kısayollarını göstermez (işletme bağlamı olmayan sayfalar için). */
export function PageHeader({ kicker, baslik, children, sade = false }) {
  const navigate = useNavigate();
  const bildirimAlani = useBildirimAlani();
  return (
    <div className="mb-5 flex flex-wrap items-end gap-4">
      <div>
        <div className="mb-0.5 text-[11.5px] uppercase tracking-[0.12em] text-ink/55">{kicker}</div>
        <h2 className="text-[36px]">{baslik}</h2>
      </div>
      {/* Bildirimler (başarı/hata) butonların solundaki bu boşlukta, satırın hizasında görünür */}
      <div ref={bildirimAlani} className="relative min-w-0 grow basis-0 self-stretch" />
      <div className="ml-auto flex flex-wrap gap-2">
        {children}
        {sade ? null : (
          <>
            <Button variant="gri" onClick={() => navigate('/takvim')}>
              <CalendarDays size={16} strokeWidth={1.5} />
              <span>Takvim</span>
            </Button>
            <Button variant="primary" onClick={() => navigate('/rezervasyon/yeni')}>
              <Plus size={16} strokeWidth={1.5} />
              <span>Hızlı rezervasyon</span>
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
