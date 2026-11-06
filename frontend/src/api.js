// Thin fetch wrapper. The token lives in localStorage for now; moving it to an
// httpOnly cookie is on the task board (it reduces the impact of XSS).
const BASE = import.meta.env.VITE_API_URL ?? "";
const KEY = "orbit_token";

export const getToken = () => localStorage.getItem(KEY);
export const setToken = (t) => (t ? localStorage.setItem(KEY, t) : localStorage.removeItem(KEY));

async function request(path, options = {}) {
  const token = getToken();
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (res.status === 401 && token) {
    setToken(null);
    window.location.reload();
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.message || body.error || message;
    } catch { /* no JSON body */ }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

const send = (method) => (path, body) =>
  request(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });

export const api = {
  get: (path) => request(path),
  post: send("POST"),
  put: send("PUT"),
  del: send("DELETE"),
};
