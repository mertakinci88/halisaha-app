import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const ACCESS_KEY = 'hs_access_token';
const REFRESH_KEY = 'hs_refresh_token';
const USER_KEY = 'hs_kullanici';

export const tokenStore = {
  access: () => localStorage.getItem(ACCESS_KEY),
  refresh: () => localStorage.getItem(REFRESH_KEY),
  user: () => {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    } catch {
      return null;
    }
  },
  set(tokenResponse, kullanici) {
    if (tokenResponse?.accessToken) localStorage.setItem(ACCESS_KEY, tokenResponse.accessToken);
    if (tokenResponse?.refreshToken) localStorage.setItem(REFRESH_KEY, tokenResponse.refreshToken);
    if (kullanici) localStorage.setItem(USER_KEY, JSON.stringify(kullanici));
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = tokenStore.access();
  if (token) config.headers.Authorization = 'Bearer ' + token;
  return config;
});

let yenileniyor = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config || {};
    const status = error.response?.status;
    const refreshToken = tokenStore.refresh();
    const authIstegi = String(original.url || '').includes('/api/auth/');
    // Backend, kimliksiz/süresi dolmuş istekte normalde 401 döner; Spring Security'nin
    // varsayılan giriş noktası yanlış yapılandırılırsa 403 dönebildiği için burada aynı
    // şekilde ele alınıyor.
    const oturumHatasi = status === 401 || status === 403;

    if (oturumHatasi && refreshToken && !original._retry && !authIstegi) {
      original._retry = true;
      try {
        yenileniyor =
          yenileniyor ||
          axios.post(API_BASE_URL + '/api/auth/refresh', { refreshToken }).then((r) => r.data);
        const data = await yenileniyor;
        yenileniyor = null;
        tokenStore.set(data);
        original.headers = original.headers || {};
        original.headers.Authorization = 'Bearer ' + data.accessToken;
        return api.request(original);
      } catch (e) {
        yenileniyor = null;
        tokenStore.clear();
        window.location.assign('/giris');
        return Promise.reject(e);
      }
    }

    if (oturumHatasi && !authIstegi) {
      tokenStore.clear();
      window.location.assign('/giris');
    }
    return Promise.reject(error);
  },
);

/** Backend GlobalExceptionHandler'ın ErrorResponse gövdesinden okunabilir mesaj çıkarır */
export const apiHata = (error) =>
  error?.response?.data?.message ||
  error?.response?.data?.error ||
  error?.message ||
  'Beklenmeyen bir hata oluştu';
