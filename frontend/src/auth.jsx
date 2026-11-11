import { createContext, useContext, useEffect, useState } from 'react';
import { api, clearSession } from './api';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

const stored = () => { try { return JSON.parse(localStorage.getItem('user')); } catch { return null; } };

export function AuthProvider({ children }) {
  const [user, setUser] = useState(stored);

  // api.js refreshes tokens in the background; pick up the new user data (e.g. a changed role)
  useEffect(() => {
    const sync = () => setUser(stored());
    window.addEventListener('session-updated', sync);
    return () => window.removeEventListener('session-updated', sync);
  }, []);

  const save = (d) => {
    localStorage.setItem('token', d.token);
    localStorage.setItem('refreshToken', d.refreshToken);
    localStorage.setItem('user', JSON.stringify(d));
    setUser(d);
  };
  const login = async (email, password) =>
    save(await api('/api/auth/login', { method: 'POST', body: { email, password } }));
  const register = async (name, email, password) =>
    save(await api('/api/auth/register', { method: 'POST', body: { name, email, password } }));
  const logout = () => {
    const rt = localStorage.getItem('refreshToken');
    if (rt) api('/api/auth/logout', { method: 'POST', body: { refreshToken: rt } }).catch(() => {});
    clearSession();
    setUser(null);
  };

  return <Ctx.Provider value={{ user, login, register, logout }}>{children}</Ctx.Provider>;
}
