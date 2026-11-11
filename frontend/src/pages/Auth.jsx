import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { ErrorNote } from '../ui';

export default function Auth({ mode }) {
  const { user, login, register } = useAuth();
  const nav = useNavigate();
  const notice = useLocation().state?.notice;
  const [f, setF] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const isLogin = mode === 'login';

  if (user) return <Navigate to="/" replace />;

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    setError(''); setBusy(true);
    try {
      if (isLogin) await login(f.email, f.password);
      else await register(f.name, f.email, f.password);
      nav('/');
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };

  return (
    <div className="auth">
      <form className="auth-card" onSubmit={submit}>
        <h1>{isLogin ? 'Sign in to HelpDesk' : 'Create your account'}</h1>
        <p className="muted">{isLogin ? 'Track your IT requests and see who is on them.' : 'Employees can raise and follow tickets.'}</p>
        {notice && <div className="notice">{notice}</div>}
        {!isLogin && (<label>Name<input required value={f.name} onChange={set('name')} /></label>)}
        <label>Work email<input type="email" required value={f.email} onChange={set('email')} /></label>
        <label>Password<input type="password" required minLength={isLogin ? 1 : 8} value={f.password} onChange={set('password')} /></label>
        {!isLogin && <p className="muted small">At least 8 characters, with a letter and a number.</p>}
        <ErrorNote error={error} />
        <button className="btn primary" disabled={busy}>{isLogin ? 'Sign in' : 'Create account'}</button>
        <p className="muted small">
          {isLogin ? <>New here? <Link to="/register">Create an account</Link></> : <>Already registered? <Link to="/login">Sign in</Link></>}
        </p>
      </form>
    </div>
  );
}
