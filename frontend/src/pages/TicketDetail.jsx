import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { Badge, SlaBar, ErrorNote, STATUSES, fmt, label } from '../ui';

export default function TicketDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [t, setT] = useState(null);
  const [comments, setComments] = useState([]);
  const [history, setHistory] = useState([]);
  const [agents, setAgents] = useState([]);
  const [assignee, setAssignee] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const [ticket, cs, hs] = await Promise.all([
        api(`/api/tickets/${id}`), api(`/api/tickets/${id}/comments`), api(`/api/tickets/${id}/history`),
      ]);
      setT(ticket); setComments(cs); setHistory(hs); setError('');
    } catch (e) { setError(e.message); }
  };
  useEffect(() => { load(); }, [id]);
  useEffect(() => {
    if (user.role === 'ADMIN') api('/api/users/support').then(setAgents).catch(() => {});
  }, [user.role]);

  const act = async (fn) => { try { setError(''); await fn(); await load(); } catch (e) { setError(e.message); } };
  const put = (path, body) => () => api(`/api/tickets/${id}/${path}`, { method: 'PUT', body });

  const send = async (e) => {
    e.preventDefault();
    if (!msg.trim()) return;
    try { await api(`/api/tickets/${id}/comments`, { method: 'POST', body: { message: msg } }); setMsg(''); load(); }
    catch (err) { setError(err.message); }
  };

  if (!t) return <><Link to="/" className="back">Back to tickets</Link><ErrorNote error={error} />{!error && <p className="muted">Loading…</p>}</>;

  const mine = t.assignedToId === user.id;
  const canSetStatus = user.role === 'ADMIN' || (user.role === 'SUPPORT' && mine);

  return (
    <>
      <Link to="/" className="back">Back to tickets</Link>
      <header className="page-head">
        <h1>#{t.id} {t.title}</h1>
        <div className="row"><Badge kind="p" value={t.priority} /><Badge kind="s" value={t.status} /></div>
      </header>
      <ErrorNote error={error} />

      <div className="detail">
        <section className="panel">
          <h2>Description</h2>
          <p className="prewrap">{t.description || <span className="muted">No details provided.</span>}</p>

          <h2>Conversation</h2>
          {comments.length === 0 && <p className="muted">No comments yet.</p>}
          <ul className="comments">
            {comments.map((c) => (
              <li key={c.id} className={c.userId === user.id ? 'mine' : ''}>
                <div className="c-head"><strong>{c.userName}</strong> <span className="muted small">{label(c.userRole)} · {fmt(c.createdAt)}</span></div>
                <div className="prewrap">{c.message}</div>
              </li>
            ))}
          </ul>
          <form onSubmit={send} className="comment-form">
            <textarea rows={3} value={msg} onChange={(e) => setMsg(e.target.value)} placeholder="Write a reply" />
            <button className="btn primary" disabled={!msg.trim()}>Post comment</button>
          </form>

          <h2 className="gap-top">Activity</h2>
          {history.length === 0 && <p className="muted">No activity recorded yet.</p>}
          <ol className="timeline">
            {history.map((h) => (
              <li key={h.id}>
                <div>{h.details}</div>
                <div className="muted small">{h.actorName} · {fmt(h.createdAt)}</div>
              </li>
            ))}
          </ol>
        </section>

        <aside className="panel meta">
          <h2>SLA</h2>
          <SlaBar ticket={t} />
          <dl>
            <dt>Requested by</dt><dd>{t.createdByName}</dd>
            <dt>Assigned to</dt><dd>{t.assignedToName || 'Unassigned'}</dd>
            <dt>Category</dt><dd>{t.category || '—'}</dd>
            <dt>Created</dt><dd>{fmt(t.createdAt)}</dd>
            <dt>Due</dt><dd>{fmt(t.dueAt)}</dd>
            {t.resolvedAt && (<><dt>Resolved</dt><dd>{fmt(t.resolvedAt)}</dd></>)}
          </dl>

          {user.role === 'SUPPORT' && !t.assignedToId && (
            <button className="btn primary block" onClick={() => act(put('claim'))}>Pick up this ticket</button>
          )}
          {user.role === 'ADMIN' && (
            <div className="stack">
              <label>Assign to
                <select value={assignee} onChange={(e) => setAssignee(e.target.value)}>
                  <option value="">Choose an agent</option>
                  {agents.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                </select>
              </label>
              <button className="btn block" disabled={!assignee} onClick={() => act(put('assign', { assigneeId: Number(assignee) }))}>Assign</button>
            </div>
          )}
          {canSetStatus && (
            <label>Status
              <select value={t.status} onChange={(e) => act(put('status', { status: e.target.value }))}>
                {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
              </select>
            </label>
          )}
          {user.role === 'EMPLOYEE' && t.status !== 'CLOSED' && (
            <button className="btn block" onClick={() => act(put('status', { status: 'CLOSED' }))}>
              {t.status === 'RESOLVED' ? 'Confirm fixed and close' : 'Close ticket'}
            </button>
          )}
        </aside>
      </div>
    </>
  );
}
