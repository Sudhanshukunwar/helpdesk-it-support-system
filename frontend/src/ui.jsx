// Small shared UI helpers.
export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const CATEGORIES = ['Hardware', 'Software', 'Network', 'Account access', 'Other'];

export const label = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase().replace('_', ' ') : '');

// Backend sends UTC LocalDateTime without a zone marker.
export const toDate = (s) => (s ? new Date(/Z|[+-]\d\d:\d\d$/.test(s) ? s : s + 'Z') : null);

export const fmt = (s) =>
  s ? toDate(s).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export function Badge({ kind, value }) {
  return <span className={`badge ${kind} ${value}`}>{label(value)}</span>;
}

function span(ms) {
  const m = Math.max(0, Math.round(ms / 60000));
  const d = Math.floor(m / 1440), h = Math.floor((m % 1440) / 60), mm = m % 60;
  if (d) return `${d}d ${h}h`;
  if (h) return `${h}h ${mm}m`;
  return `${mm}m`;
}

/** The SLA clock: how much of the resolution window has been used. */
export function SlaBar({ ticket }) {
  const created = toDate(ticket.createdAt), due = toDate(ticket.dueAt);
  const done = ticket.status === 'RESOLVED' || ticket.status === 'CLOSED';
  if (done) {
    const took = toDate(ticket.resolvedAt) - created;
    const met = toDate(ticket.resolvedAt) <= due;
    return (
      <div className="sla">
        <div className="sla-track"><div className={`sla-fill ${met ? 'ok' : 'breach'}`} style={{ width: '100%' }} /></div>
        <span className="sla-text">{met ? 'Met' : 'Missed'} · {span(took)}</span>
      </div>
    );
  }
  const total = due - created;
  const used = Date.now() - created;
  const pct = Math.min(100, Math.max(2, (used / total) * 100));
  const over = Date.now() > due;
  const state = over ? 'breach' : pct > 75 ? 'warn' : 'ok';
  return (
    <div className="sla">
      <div className="sla-track"><div className={`sla-fill ${state}`} style={{ width: `${pct}%` }} /></div>
      <span className={`sla-text ${state}`}>{over ? `Overdue ${span(Date.now() - due)}` : `${span(due - Date.now())} left`}</span>
    </div>
  );
}

export function ErrorNote({ error }) {
  return error ? <div className="error" role="alert">{error}</div> : null;
}
