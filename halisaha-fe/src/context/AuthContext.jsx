import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '@/services';
import { tokenStore } from '@/lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [kullanici, setKullanici] = useState(() => tokenStore.user());
  const [girisli, setGirisli] = useState(() => Boolean(tokenStore.access()));
  const [yukleniyor, setYukleniyor] = useState(() => Boolean(tokenStore.access()));

  useEffect(() => {
    if (!tokenStore.access()) {
      setYukleniyor(false);
      return;
    }
    authService
      .me()
      .then((me) => {
        tokenStore.set(null, me);
        setKullanici(me);
      })
      .catch(() => {})
      .finally(() => setYukleniyor(false));
  }, []);

  const login = useCallback(async (kullaniciAdi, sifre) => {
    const token = await authService.login(kullaniciAdi, sifre);
    tokenStore.set(token);
    const me = await authService.me();
    tokenStore.set(null, me);
    setKullanici(me);
    setGirisli(true);
    return token;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    tokenStore.clear();
    setKullanici(null);
    setGirisli(false);
  }, []);

  const isAdmin = kullanici?.rol === 'ADMIN';
  const yetkiVar = useCallback(
    (modul) => isAdmin || Boolean(kullanici?.yetkiler?.includes(modul)),
    [isAdmin, kullanici],
  );

  const value = useMemo(
    () => ({ kullanici, girisli, yukleniyor, isAdmin, yetkiVar, login, logout }),
    [kullanici, girisli, yukleniyor, isAdmin, yetkiVar, login, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth, AuthProvider içinde kullanılmalıdır');
  return ctx;
}
