// VITE_API_URL: unset -> local dev backend; "same-origin" -> relative URLs (behind a reverse proxy); otherwise a full URL.
const ENV_URL = import.meta.env.VITE_API_URL;
const BASE = ENV_URL === undefined || ENV_URL === '' ? 'http://localhost:8080' : ENV_URL === 'same-origin' ? '' : ENV_URL;

export function clearSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

let refreshing = null; // one refresh at a time, so parallel 401s don't reuse the same single-use token

async function refreshTokens() {
  const rt = localStorage.getItem('refreshToken');
  if (!rt) throw new Error('No session');
  const res = await fetch(BASE + '/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: rt }),
  });
  if (!res.ok) throw new Error('Session expired');
  const d = await res.json();
  localStorage.setItem('token', d.token);
  localStorage.setItem('refreshToken', d.refreshToken);
  localStorage.setItem('user', JSON.stringify(d));
  window.dispatchEvent(new Event('session-updated'));
}

export async function api(path, opts = {}, retried = false) {
  const { method = 'GET', body } = opts;
  const token = localStorage.getItem('token');
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && !path.startsWith('/api/auth')) {
    if (!retried) {
      try {
        refreshing = refreshing || refreshTokens().finally(() => { refreshing = null; });
        await refreshing;
        return api(path, opts, true);
      } catch { /* fall through to sign-out */ }
    }
    clearSession();
    window.location.assign('/login');
    throw new Error('Your session expired. Sign in again.');
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`);
  return data;
}
