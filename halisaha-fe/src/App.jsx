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

function KorumaliAlan({ children }) {
  const { girisli } = useAuth();
  return girisli ? children : <Navigate to="/giris" replace />;
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
        <Route path="/ogrenciler" element={<OgrenciListePage />} />
        <Route path="/ogrenciler/yeni" element={<OgrenciKayitPage />} />
        <Route path="/ogrenciler/:id" element={<OgrenciDetayPage />} />
        <Route path="/gruplar" element={<GrupListePage />} />
        <Route path="/gruplar/yeni" element={<GrupKayitPage />} />
        <Route path="/gruplar/:id" element={<GrupDetayPage />} />
        <Route path="/sahalar" element={<SahaListePage />} />
        <Route path="/sahalar/yeni" element={<SahaKayitPage />} />
        <Route path="/sahalar/:id" element={<SahaDetayPage />} />
        <Route path="/urunler" element={<UrunListePage />} />
        <Route path="/urunler/yeni" element={<UrunKayitPage />} />
        <Route path="/urunler/:id" element={<UrunDetayPage />} />
      </Route>

      <Route path="*" element={<Navigate to={girisli ? '/panel' : '/giris'} replace />} />
    </Routes>
  );
}
