import { useCallback, useEffect, useState } from "react";
import { api, getToken, setToken } from "./api.js";

const STAGES = ["WISHLIST", "DRAFTING", "APPLIED", "ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"];
const LABEL = {
  WISHLIST: "Wishlist", DRAFTING: "Drafting", APPLIED: "Applied", ASSESSMENT: "Assessment",
  INTERVIEW: "Interview", OFFER: "Offer", REJECTED: "Rejected", WITHDRAWN: "Withdrawn",
};

function daysLeft(deadline) {
  if (!deadline) return null;
  const today = new Date(); today.setHours(0, 0, 0, 0);
  return Math.round((new Date(deadline + "T00:00:00") - today) / 86400000);
}

function deadlineText(deadline) {
  const d = daysLeft(deadline);
  if (d === null) return "";
  if (d < 0) return `${-d}d overdue`;
  if (d === 0) return "due today";
  return `due in ${d}d`;
}

/* ---------- sign in ---------- */
function Auth({ onDone }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function submit(e) {
    e.preventDefault();
    setError("");
    try {
      const { token } = await api.post(`/api/auth/${mode}`, { email, password });
      setToken(token);
      onDone();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <main className="auth">
      <h1 className="wordmark">Orbit</h1>
      <p className="lede">Every application, deadline and reply in one place, so nothing slips past you.</p>
      <form onSubmit={submit} className="panel">
        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8}
                 autoComplete={mode === "login" ? "current-password" : "new-password"} />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="primary">{mode === "login" ? "Sign in" : "Create account"}</button>
        <button type="button" className="link" onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {mode === "login" ? "New here? Create an account" : "Have an account? Sign in"}
        </button>
      </form>
    </main>
  );
}

/* ---------- pipeline ---------- */
function AddForm({ onAdded }) {
  const empty = { company: "", roleTitle: "", deadline: "", jobUrl: "" };
  const [form, setForm] = useState(empty);
  const [error, setError] = useState("");
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError("");
    try {
      await api.post("/api/applications", { ...form, deadline: form.deadline || null, jobUrl: form.jobUrl || null });
      setForm(empty);
      onAdded();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="add" onSubmit={submit}>
      <input placeholder="Company" value={form.company} onChange={set("company")} required aria-label="Company" />
      <input placeholder="Role" value={form.roleTitle} onChange={set("roleTitle")} required aria-label="Role" />
      <input type="date" value={form.deadline} onChange={set("deadline")} aria-label="Closing date" />
      <input placeholder="Posting link (optional)" value={form.jobUrl} onChange={set("jobUrl")} aria-label="Posting link" />
      <button className="primary">Add application</button>
      {error && <p className="error" role="alert">{error}</p>}
    </form>
  );
}

function Card({ app, onChange, onDelete }) {
  const d = daysLeft(app.deadline);
  const urgent = d !== null && d <= 3 && ["WISHLIST", "DRAFTING"].includes(app.status);
  return (
    <article className="card">
      <h3>{app.company}</h3>
      <p>{app.roleTitle}</p>
      {app.deadline && <span className={urgent ? "chip urgent" : "chip"}>{deadlineText(app.deadline)}</span>}
      {app.matchScore != null && <span className="chip">{app.matchScore}% match</span>}
      {app.riskLevel && <span className={app.riskLevel === "HIGH" ? "chip urgent" : "chip"}>{app.riskLevel.toLowerCase()} risk</span>}
      <div className="card-actions">
        <select value={app.status} onChange={(e) => onChange(app, e.target.value)} aria-label={`Move ${app.company}`}>
          {STAGES.map((s) => <option key={s} value={s}>{LABEL[s]}</option>)}
        </select>
        <button className="link" onClick={() => onDelete(app)} aria-label={`Delete ${app.company}`}>Delete</button>
      </div>
    </article>
  );
}

function Pipeline({ apps, due, audit, reload }) {
  async function move(app, status) {
    await api.put(`/api/applications/${app.id}`, {
      company: app.company, roleTitle: app.roleTitle, jobUrl: app.jobUrl,
      jobDescription: app.jobDescription, deadline: app.deadline, notes: app.notes, status,
    });
    reload();
  }
  async function remove(app) {
    if (window.confirm(`Delete ${app.company}?`)) {
      await api.del(`/api/applications/${app.id}`);
      reload();
    }
  }

  return (
    <div className="layout">
      <section>
        <AddForm onAdded={reload} />
        {apps.length === 0 && <p className="empty">No applications yet. Add the first one above and it will appear in Wishlist.</p>}
        <div className="pipeline">
          {STAGES.map((stage, i) => {
            const items = apps.filter((a) => a.status === stage);
            const progress = i <= 5 ? Math.round((i / 5) * 100) : 0;
            return (
              <div key={stage} className="column">
                <h2><span className="ring" style={{ "--p": progress }} />{LABEL[stage]}<small>{items.length}</small></h2>
                {items.map((a) => <Card key={a.id} app={a} onChange={move} onDelete={remove} />)}
              </div>
            );
          })}
        </div>
      </section>
      <aside>
        <div className="panel">
          <h2>Finish these first</h2>
          {due.length === 0 && <p className="muted">Nothing unsent is closing in the next 14 days.</p>}
          <ul>{due.map((a) => <li key={a.id}><strong>{a.company}</strong> <span className="muted">{deadlineText(a.deadline)}</span></li>)}</ul>
        </div>
        <div className="panel">
          <h2>Recent activity</h2>
          {audit.length === 0 && <p className="muted">Changes you make will be logged here.</p>}
          <ul className="log">{audit.slice(0, 10).map((e) => <li key={e.id}>{e.detail}<br /><span className="muted">{new Date(e.occurredAt).toLocaleString()}</span></li>)}</ul>
        </div>
      </aside>
    </div>
  );
}

/* ---------- tools ---------- */
function Tools({ apps, reload }) {
  const [cv, setCv] = useState("");
  const [jd, setJd] = useState("");
  const [appId, setAppId] = useState("");
  const [match, setMatch] = useState(null);
  const [risk, setRisk] = useState(null);
  const [r, setR] = useState({ company: "", contactEmail: "", websiteUrl: "", postingText: "" });
  const [error, setError] = useState("");

  async function runMatch(e) {
    e.preventDefault(); setError("");
    try { setMatch(await api.post("/api/tools/match", { cvText: cv, jobDescription: jd, applicationId: appId || null })); reload(); }
    catch (err) { setError(err.message); }
  }
  async function runRisk(e) {
    e.preventDefault(); setError("");
    try { setRisk(await api.post("/api/tools/risk", { ...r, applicationId: appId || null })); reload(); }
    catch (err) { setError(err.message); }
  }

  return (
    <div className="tools">
      {error && <p className="error" role="alert">{error}</p>}
      <label className="panel">Save results to
        <select value={appId} onChange={(e) => setAppId(e.target.value)}>
          <option value="">Don't save</option>
          {apps.map((a) => <option key={a.id} value={a.id}>{a.company} - {a.roleTitle}</option>)}
        </select>
      </label>
      <form className="panel" onSubmit={runMatch}>
        <h2>CV match</h2>
        <p className="muted">Compares your CV with a job description and lists the keywords you are missing. It is a guide, not how every employer's system scores you.</p>
        <label>Your CV text<textarea rows={7} value={cv} onChange={(e) => setCv(e.target.value)} required /></label>
        <label>Job description<textarea rows={7} value={jd} onChange={(e) => setJd(e.target.value)} required /></label>
        <button className="primary">Check match</button>
        {match && (
          <div className="result">
            <p className="score">{match.score}% of the key terms are in your CV</p>
            <p><strong>Missing:</strong> {match.missing.join(", ") || "none"}</p>
            <p><strong>Covered:</strong> {match.matched.join(", ") || "none"}</p>
          </div>
        )}
      </form>
      <form className="panel" onSubmit={runRisk}>
        <h2>Posting risk check</h2>
        <p className="muted">Looks for common scam signals. A low result does not prove a company is real, so confirm it on the official website and CIPC.</p>
        <label>Company<input value={r.company} onChange={(e) => setR({ ...r, company: e.target.value })} /></label>
        <label>Contact email<input value={r.contactEmail} onChange={(e) => setR({ ...r, contactEmail: e.target.value })} /></label>
        <label>Company website<input value={r.websiteUrl} onChange={(e) => setR({ ...r, websiteUrl: e.target.value })} /></label>
        <label>Posting text<textarea rows={5} value={r.postingText} onChange={(e) => setR({ ...r, postingText: e.target.value })} /></label>
        <button className="primary">Check posting</button>
        {risk && (
          <div className="result">
            <p className={risk.level === "HIGH" ? "score bad" : "score"}>{risk.level.toLowerCase()} risk</p>
            <ul>{risk.reasons.map((x) => <li key={x}>{x}</li>)}</ul>
          </div>
        )}
      </form>
    </div>
  );
}

/* ---------- shell ---------- */
export default function App() {
  const [authed, setAuthed] = useState(!!getToken());
  const [view, setView] = useState("pipeline");
  const [apps, setApps] = useState([]);
  const [due, setDue] = useState([]);
  const [audit, setAudit] = useState([]);

  const load = useCallback(async () => {
    const [a, d, l] = await Promise.all([
      api.get("/api/applications"), api.get("/api/applications/due-soon?days=14"), api.get("/api/audit"),
    ]);
    setApps(a); setDue(d); setAudit(l);
  }, []);

  useEffect(() => { if (authed) load().catch(() => {}); }, [authed, load]);

  if (!authed) return <Auth onDone={() => setAuthed(true)} />;

  return (
    <>
      <header className="top">
        <h1 className="wordmark">Orbit</h1>
        <nav>
          <button className={view === "pipeline" ? "tab on" : "tab"} onClick={() => setView("pipeline")}>Applications</button>
          <button className={view === "tools" ? "tab on" : "tab"} onClick={() => setView("tools")}>Check a posting</button>
        </nav>
        <button className="link" onClick={() => { setToken(null); setAuthed(false); }}>Sign out</button>
      </header>
      <main>
        {view === "pipeline" ? <Pipeline apps={apps} due={due} audit={audit} reload={load} /> : <Tools apps={apps} reload={load} />}
      </main>
    </>
  );
}
