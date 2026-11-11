import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { ErrorNote } from '../ui';

export default function Account() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ currentPassword: '', newPassword: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const leave = (notice) => { logout(); nav('/login', { state: { notice } }); };

  const changePassword = async (e) => {
    e.preventDefault();
    setError(''); setBusy(true);
    try {
      await api('/api/account/password', { method: 'PUT', body: f });
      leave('Password changed. Sign in with your new password.');
    } catch (err) { setError(err.message); setBusy(false); }
  };

  const everywhere = async () => {
    try {
      await api('/api/account/sign-out-everywhere', { method: 'POST' });
      leave('Signed out on all devices.');
    } catch (err) { setError(err.message); }
  };

  return (
    <>
      <header className="page-head"><h1>Account</h1></header>
      <section className="panel narrow">
        <h2>{user.name}</h2>
        <p className="muted">{user.email}</p>
      </section>

      <form className="panel narrow" onSubmit={changePassword}>
        <h2>Change password</h2>
        <label>Current password
          <input type="password" required value={f.currentPassword} onChange={(e) => setF({ ...f, currentPassword: e.target.value })} />
        </label>
        <label>New password
          <input type="password" required minLength={8} value={f.newPassword} onChange={(e) => setF({ ...f, newPassword: e.target.value })} />
        </label>
        <p className="muted small">At least 8 characters, with a letter and a number. You will be signed out on every device.</p>
        <ErrorNote error={error} />
        <button className="btn primary" disabled={busy}>Change password</button>
      </form>

      <section className="panel narrow">
        <h2>Sign out everywhere</h2>
        <p className="muted small">Ends every active session for this account, including this one.</p>
        <button className="btn" onClick={everywhere}>Sign out on all devices</button>
      </section>
    </>
  );
}
