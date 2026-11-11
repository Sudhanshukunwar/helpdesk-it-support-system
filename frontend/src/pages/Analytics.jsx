import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api';
import { ErrorNote, label } from '../ui';

const STATUS_COLORS = { OPEN: '#2B6CB0', IN_PROGRESS: '#B7791F', RESOLVED: '#2F855A', CLOSED: '#718096' };
const PRIORITY_COLORS = { LOW: '#718096', MEDIUM: '#2B6CB0', HIGH: '#C05621', CRITICAL: '#C53030' };

const toRows = (obj) => Object.entries(obj).map(([k, v]) => ({ key: k, name: label(k), count: v }));

export default function Analytics() {
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api('/api/analytics/summary').then(setD).catch((e) => setError(e.message)); }, []);

  if (error) return <ErrorNote error={error} />;
  if (!d) return <p className="muted">Loading analytics…</p>;

  return (
    <>
      <header className="page-head"><h1>Analytics</h1></header>
      <div className="stats">
        <div className="stat"><div className="stat-n">{d.total}</div><div className="muted">Total tickets</div></div>
        <div className="stat"><div className="stat-n">{d.avgResolutionHours}h</div><div className="muted">Average time to resolve</div></div>
        <div className="stat"><div className={`stat-n ${d.slaBreachPercent > 0 ? 'red' : ''}`}>{d.slaBreachPercent}%</div><div className="muted">Tickets that breached SLA</div></div>
      </div>

      <div className="charts">
        <section className="panel">
          <h2>Tickets by status</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={toRows(d.byStatus)}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip />
              <Bar dataKey="count" name="Tickets">{toRows(d.byStatus).map((r) => <Cell key={r.key} fill={STATUS_COLORS[r.key]} />)}</Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>
        <section className="panel">
          <h2>Tickets by priority</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={toRows(d.byPriority)}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip />
              <Bar dataKey="count" name="Tickets">{toRows(d.byPriority).map((r) => <Cell key={r.key} fill={PRIORITY_COLORS[r.key]} />)}</Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>
        <section className="panel wide">
          <h2>Workload per agent</h2>
          {d.agentLoad.length === 0 ? <p className="muted">No tickets are assigned yet.</p> : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={d.agentLoad}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Legend />
                <Bar dataKey="active" name="Active" stackId="a" fill="#B7791F" />
                <Bar dataKey="resolved" name="Resolved" stackId="a" fill="#2F855A" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </section>
      </div>
    </>
  );
}
