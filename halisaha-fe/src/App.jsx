import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import { useAuth } from '@/context/AuthContext';

import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import TakvimPage from '@/pages/TakvimPage';
import RezervasyonYeniPage from '@/pages/RezervasyonYeniPage';
import RezervasyonDetayPage from '@/pages/RezervasyonDetayPage';
import OgrenciListePage from '@/pages/OgrenciListePage';
import OgrenciKayitPage from '@/pages/OgrenciKayitPage';
import OgrenciDetayPage from '@/pages/OgrenciDetayPage';
import GrupListePage from '@/pages/GrupListePage';
import GrupKayitPage from '@/pages/GrupKayitPage';
import GrupDetayPage from '@/pages/GrupDetayPage';
import SahaListePage from '@/pages/SahaListePage';
import SahaKayitPage from '@/pages/SahaKayitPage';
import SahaDetayPage from '@/pages/SahaDetayPage';
import UrunListePage from '@/pages/UrunListePage';
import UrunKayitPage from '@/pages/UrunKayitPage';
import UrunDetayPage from '@/pages/UrunDetayPage';
import PersonelListePage from '@/pages/PersonelListePage';
import PersonelKayitPage from '@/pages/PersonelKayitPage';
import PersonelYetkiPage from '@/pages/PersonelYetkiPage';

function KorumaliAlan({ children }) {
  const { girisli } = useAuth();
  return girisli ? children : <Navigate to="/giris" replace />;
}

/** Bir menü modülü (ör. "OGRENCI") ya da admin yetkisi gerektiren rotaları korur. */
function YetkiliRota({ modul, sadeceAdmin, children }) {
  const { isAdmin, yetkiVar } = useAuth();
  const yetkili = sadeceAdmin ? isAdmin : yetkiVar(modul);
  return yetkili ? children : <Navigate to="/panel" replace />;
}

export default function App() {
  const { girisli } = useAuth();

  return (
    <Routes>
      <Route path="/giris" element={girisli ? <Navigate to="/panel" replace /> : <LoginPage />} />

      <Route
        element={
          <KorumaliAlan>
            <AppLayout />
          </KorumaliAlan>
        }
      >
        <Route path="/panel" element={<DashboardPage />} />
        <Route path="/takvim" element={<TakvimPage />} />
        <Route path="/rezervasyon/yeni" element={<RezervasyonYeniPage />} />
        <Route path="/rezervasyon/:id" element={<RezervasyonDetayPage />} />

        <Route
          path="/ogrenciler"
          element={<YetkiliRota modul="OGRENCI"><OgrenciListePage /></YetkiliRota>}
        />
        <Route
          path="/ogrenciler/yeni"
          element={<YetkiliRota modul="OGRENCI"><OgrenciKayitPage /></YetkiliRota>}
        />
        <Route
          path="/ogrenciler/:id"
          element={<YetkiliRota modul="OGRENCI"><OgrenciDetayPage /></YetkiliRota>}
        />

        <Route
          path="/gruplar"
          element={<YetkiliRota modul="GRUP"><GrupListePage /></YetkiliRota>}
        />
        <Route
          path="/gruplar/yeni"
          element={<YetkiliRota modul="GRUP"><GrupKayitPage /></YetkiliRota>}
        />
        <Route
          path="/gruplar/:id"
          element={<YetkiliRota modul="GRUP"><GrupDetayPage /></YetkiliRota>}
        />

        <Route
          path="/sahalar"
          element={<YetkiliRota modul="SAHA"><SahaListePage /></YetkiliRota>}
        />
        <Route
          path="/sahalar/yeni"
          element={<YetkiliRota modul="SAHA"><SahaKayitPage /></YetkiliRota>}
        />
        <Route
          path="/sahalar/:id"
          element={<YetkiliRota modul="SAHA"><SahaDetayPage /></YetkiliRota>}
        />

        <Route
          path="/urunler"
          element={<YetkiliRota modul="URUN"><UrunListePage /></YetkiliRota>}
        />
        <Route
          path="/urunler/yeni"
          element={<YetkiliRota modul="URUN"><UrunKayitPage /></YetkiliRota>}
        />
        <Route
          path="/urunler/:id"
          element={<YetkiliRota modul="URUN"><UrunDetayPage /></YetkiliRota>}
        />

        <Route
          path="/personel"
          element={<YetkiliRota sadeceAdmin><PersonelListePage /></YetkiliRota>}
        />
        <Route
          path="/personel/yeni"
          element={<YetkiliRota sadeceAdmin><PersonelKayitPage /></YetkiliRota>}
        />
        <Route
          path="/personel/:id"
          element={<YetkiliRota sadeceAdmin><PersonelYetkiPage /></YetkiliRota>}
        />
      </Route>

      <Route path="*" element={<Navigate to={girisli ? '/panel' : '/giris'} replace />} />
    </Routes>
  );
}
