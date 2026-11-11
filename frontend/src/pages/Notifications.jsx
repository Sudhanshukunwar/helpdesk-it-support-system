import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { ErrorNote, fmt } from '../ui';

export default function Notifications() {
  const nav = useNavigate();
  const [list, setList] = useState(null);
  const [error, setError] = useState('');
  const load = () => api('/api/notifications').then(setList).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const open = async (n) => {
    if (!n.seen) await api(`/api/notifications/${n.id}/read`, { method: 'PUT' }).catch(() => {});
    if (n.ticketId) nav(`/tickets/${n.ticketId}`); else load();
  };
  const readAll = async () => { await api('/api/notifications/read-all', { method: 'PUT' }); load(); };

  return (
    <>
      <header className="page-head">
        <h1>Notifications</h1>
        {list?.some((n) => !n.seen) && <button className="btn" onClick={readAll}>Mark all as read</button>}
      </header>
      <ErrorNote error={error} />
      {list && list.length === 0 && <div className="empty">You are all caught up.</div>}
      <ul className="notes">
        {(list || []).map((n) => (
          <li key={n.id} className={n.seen ? '' : 'unread'} onClick={() => open(n)}>
            <span>{n.message}</span>
            <span className="muted small">{fmt(n.createdAt)}</span>
          </li>
        ))}
      </ul>
    </>
  );
}
