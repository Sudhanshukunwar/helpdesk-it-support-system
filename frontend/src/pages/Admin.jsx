import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { ErrorNote, label } from '../ui';

const ROLES = ['EMPLOYEE', 'SUPPORT', 'ADMIN'];

export default function Admin() {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [sla, setSla] = useState([]);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');

  const load = () => {
    api('/api/users').then(setUsers).catch((e) => setError(e.message));
    api('/api/sla').then(setSla).catch((e) => setError(e.message));
  };
  useEffect(load, []);

  const updateUser = async (id, body) => {
    try { setError(''); await api(`/api/users/${id}`, { method: 'PUT', body }); load(); }
    catch (e) { setError(e.message); }
  };
  const editSla = (p, k, v) => setSla(sla.map((s) => (s.priority === p ? { ...s, [k]: Number(v) } : s)));
  const saveSla = async (s) => {
    try {
      setError('');
      await api(`/api/sla/${s.priority}`, { method: 'PUT', body: { responseHours: s.responseHours, resolutionHours: s.resolutionHours } });
      setSaved(label(s.priority)); setTimeout(() => setSaved(''), 2000);
    } catch (e) { setError(e.message); }
  };

  return (
    <>
      <header className="page-head"><h1>Admin</h1></header>
      <ErrorNote error={error} />

      <section className="panel">
        <h2>SLA rules</h2>
        <p className="muted small">New tickets get their due time from these resolution hours. Existing tickets keep their original due time.</p>
        <table>
          <thead><tr><th>Priority</th><th>First response (hours)</th><th>Resolution (hours)</th><th /></tr></thead>
          <tbody>
            {sla.map((s) => (
              <tr key={s.priority}>
                <td>{label(s.priority)}</td>
                <td><input type="number" min="1" className="num" value={s.responseHours} onChange={(e) => editSla(s.priority, 'responseHours', e.target.value)} /></td>
                <td><input type="number" min="1" className="num" value={s.resolutionHours} onChange={(e) => editSla(s.priority, 'resolutionHours', e.target.value)} /></td>
                <td><button className="btn" onClick={() => saveSla(s)}>{saved === label(s.priority) ? 'Saved' : 'Save'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="panel">
        <h2>Users</h2>
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Access</th></tr></thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === user.id;
              return (
                <tr key={u.id}>
                  <td>{u.name}{self && <span className="muted small"> (you)</span>}</td>
                  <td>{u.email}</td>
                  <td>
                    <select disabled={self} value={u.role} onChange={(e) => updateUser(u.id, { role: e.target.value })}>
                      {ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
                    </select>
                  </td>
                  <td>
                    <button className="btn" disabled={self} onClick={() => updateUser(u.id, { active: !u.active })}>
                      {u.active ? 'Deactivate' : 'Reactivate'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </>
  );
}
