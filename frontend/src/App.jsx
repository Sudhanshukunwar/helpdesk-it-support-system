import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth';
import { api } from './api';
import Auth from './pages/Auth';
import Tickets from './pages/Tickets';
import TicketDetail from './pages/TicketDetail';
import Notifications from './pages/Notifications';
import Analytics from './pages/Analytics';
import Admin from './pages/Admin';
import Account from './pages/Account';

function Layout() {
  const { user, logout } = useAuth();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let alive = true;
    const poll = () => api('/api/notifications/unread-count').then((r) => alive && setUnread(r.count)).catch(() => {});
    poll();
    const t = setInterval(poll, 20000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  const link = ({ isActive }) => (isActive ? 'nav-link active' : 'nav-link');
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">HelpDesk</div>
        <nav>
          <NavLink to="/" end className={link}>Tickets</NavLink>
          <NavLink to="/notifications" className={link}>
            Notifications {unread > 0 && <span className="count">{unread}</span>}
          </NavLink>
          {user.role !== 'EMPLOYEE' && <NavLink to="/analytics" className={link}>Analytics</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/admin" className={link}>Admin</NavLink>}
          <NavLink to="/account" className={link}>Account</NavLink>
        </nav>
        <div className="me">
          <div className="me-name">{user.name}</div>
          <div className="me-role">{user.role.toLowerCase()}</div>
          <button className="link-btn" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="main"><Outlet /></main>
    </div>
  );
}

function Protected() {
  const { user } = useAuth();
  return user ? <Layout /> : <Navigate to="/login" replace />;
}

function RoleOnly({ allow, children }) {
  const { user } = useAuth();
  return allow.includes(user.role) ? children : <Navigate to="/" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Auth mode="login" />} />
          <Route path="/register" element={<Auth mode="register" />} />
          <Route element={<Protected />}>
            <Route path="/" element={<Tickets />} />
            <Route path="/tickets/:id" element={<TicketDetail />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/account" element={<Account />} />
            <Route path="/analytics" element={<RoleOnly allow={['ADMIN', 'SUPPORT']}><Analytics /></RoleOnly>} />
            <Route path="/admin" element={<RoleOnly allow={['ADMIN']}><Admin /></RoleOnly>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
