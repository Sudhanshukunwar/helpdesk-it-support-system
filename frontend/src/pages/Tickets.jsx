import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { Badge, SlaBar, ErrorNote, STATUSES, PRIORITIES, CATEGORIES, label } from '../ui';

function NewTicket({ onCreated, onCancel }) {
  const [f, setF] = useState({ title: '', description: '', category: CATEGORIES[0], priority: 'MEDIUM' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try { onCreated(await api('/api/tickets', { method: 'POST', body: f })); }
    catch (err) { setError(err.message); setBusy(false); }
  };

  return (
    <form className="panel form-grid" onSubmit={submit}>
      <label className="span2">What is not working?
        <input required value={f.title} onChange={set('title')} placeholder="e.g. VPN drops every few minutes" />
      </label>
      <label>Category
        <select value={f.category} onChange={set('category')}>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select>
      </label>
      <label>Priority
        <select value={f.priority} onChange={set('priority')}>{PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}</select>
      </label>
      <label className="span2">Details
        <textarea rows={4} value={f.description} onChange={set('description')} placeholder="What happened, since when, and what you already tried." />
      </label>
      <div className="span2"><ErrorNote error={error} /></div>
      <div className="span2 row">
        <button className="btn primary" disabled={busy}>Create ticket</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}

export default function Tickets() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [list, setList] = useState(null);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');
  const [unassigned, setUnassigned] = useState(false);

  const load = () => api('/api/tickets').then(setList).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const shown = useMemo(() => (list || []).filter((t) =>
    (!status || t.status === status) &&
    (!unassigned || !t.assignedToId) &&
    (!q || `${t.title} ${t.id} ${t.category || ''}`.toLowerCase().includes(q.toLowerCase()))), [list, status, q, unassigned]);

  return (
    <>
      <header className="page-head">
        <h1>{user.role === 'EMPLOYEE' ? 'My tickets' : 'Tickets'}</h1>
        {!creating && <button className="btn primary" onClick={() => setCreating(true)}>New ticket</button>}
      </header>

      {creating && <NewTicket onCancel={() => setCreating(false)} onCreated={(t) => nav(`/tickets/${t.id}`)} />}

      <div className="filters">
        <input placeholder="Search by title, number or category" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        {user.role !== 'EMPLOYEE' && (
          <label className="check"><input type="checkbox" checked={unassigned} onChange={(e) => setUnassigned(e.target.checked)} /> Unassigned only</label>
        )}
      </div>

      <ErrorNote error={error} />
      {!list && !error && <p className="muted">Loading tickets…</p>}
      {list && shown.length === 0 && (
        <div className="empty">
          {list.length === 0 ? (user.role === 'EMPLOYEE' ? 'No tickets yet. Create one when something breaks.' : 'No tickets to work on right now.') : 'No tickets match these filters.'}
        </div>
      )}
      {shown.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>#</th><th>Ticket</th><th>Priority</th><th>Status</th><th>Assigned to</th><th>SLA</th></tr></thead>
            <tbody>
              {shown.map((t) => (
                <tr key={t.id} className="clickable" onClick={() => nav(`/tickets/${t.id}`)}>
                  <td className="muted">{t.id}</td>
                  <td><div className="t-title">{t.title}</div><div className="muted small">{t.category} · {t.createdByName}</div></td>
                  <td><Badge kind="p" value={t.priority} /></td>
                  <td><Badge kind="s" value={t.status} /></td>
                  <td>{t.assignedToName || <span className="muted">Unassigned</span>}</td>
                  <td><SlaBar ticket={t} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
