import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { authService } from '@/services';
import { tokenStore } from '@/lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [kullanici, setKullanici] = useState(() => tokenStore.user());
  const [girisli, setGirisli] = useState(() => Boolean(tokenStore.access()));

  const login = useCallback(async (kullaniciAdi, sifre) => {
    const token = await authService.login(kullaniciAdi, sifre);
    tokenStore.set(token, { kullaniciAdi });
    setKullanici({ kullaniciAdi });
    setGirisli(true);
    return token;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    tokenStore.clear();
    setKullanici(null);
    setGirisli(false);
  }, []);

  const value = useMemo(() => ({ kullanici, girisli, login, logout }), [kullanici, girisli, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth, AuthProvider içinde kullanılmalıdır');
  return ctx;
}
