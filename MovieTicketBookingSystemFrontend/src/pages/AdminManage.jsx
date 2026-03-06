import { useState, useEffect } from 'react';
import {
  getCities,
  getAdminTheatres,
  getAdminHalls,
  getAdminMovies,
  adminAddCity,
  adminAddTheatre,
  adminAddHall,
  adminAddMovie,
  adminAddShow,
} from '../api';

const SECTION_KEYS = ['city', 'theatre', 'hall', 'movie', 'show'];
const INIT_SECTION_ALERT = Object.fromEntries(SECTION_KEYS.map((k) => [k, null]));

export default function AdminManage() {
  const [sectionAlert, setSectionAlert] = useState(INIT_SECTION_ALERT);
  const [cities, setCities] = useState([]);
  const [theatres, setTheatres] = useState([]);
  const [halls, setHalls] = useState([]);
  const [movies, setMovies] = useState([]);
  const [posterFile, setPosterFile] = useState(null);
  const [form, setForm] = useState({
    city: { name: '' },
    theatre: { cityId: '', name: '', address: '' },
    hall: { theatreId: '', name: '', rows: '', cols: '', premiumRowEnd: '', priceNormal: '100', pricePremium: '200' },
    movie: { name: '', durationHours: '0', durationMinutes: '0', description: '', language: '' },
    show: { movieId: '', hallId: '', startTime: '', endTime: '' },
  });

  useEffect(() => {
    getCities().then(setCities).catch(() => setCities([]));
    getAdminTheatres().then(setTheatres).catch(() => setTheatres([]));
    getAdminHalls().then(setHalls).catch(() => setHalls([]));
    getAdminMovies().then(setMovies).catch(() => setMovies([]));
  }, []);

  function showSuccess(section) {
    setSectionAlert((s) => ({ ...s, [section]: { type: 'success', text: 'Insert successful' } }));
    setTimeout(() => setSectionAlert((s) => ({ ...s, [section]: null })), 3000);
  }
  function showError(section, msg) {
    setSectionAlert((s) => ({ ...s, [section]: { type: 'error', text: msg } }));
  }
  function clearSectionAlert(section) {
    setSectionAlert((s) => ({ ...s, [section]: null }));
  }

  async function handleCity(e) {
    e.preventDefault();
    clearSectionAlert('city');
    try {
      await adminAddCity(form.city.name);
      setForm((f) => ({ ...f, city: { name: '' } }));
      showSuccess('city');
      getCities().then(setCities).catch(() => {});
    } catch (err) {
      showError('city', err.message || 'Invalid entry');
    }
  }
  async function handleTheatre(e) {
    e.preventDefault();
    clearSectionAlert('theatre');
    try {
      await adminAddTheatre(Number(form.theatre.cityId), form.theatre.name, form.theatre.address);
      setForm((f) => ({ ...f, theatre: { ...f.theatre, name: '', address: '' } }));
      showSuccess('theatre');
      getAdminTheatres().then(setTheatres).catch(() => {});
    } catch (err) {
      showError('theatre', err.message || 'Invalid entry');
    }
  }
  async function handleHall(e) {
    e.preventDefault();
    clearSectionAlert('hall');
    try {
      await adminAddHall(
        Number(form.hall.theatreId),
        form.hall.name,
        Number(form.hall.rows),
        Number(form.hall.cols),
        Number(form.hall.premiumRowEnd),
        Number(form.hall.priceNormal),
        Number(form.hall.pricePremium)
      );
      setForm((f) => ({
        ...f,
        hall: { ...f.hall, name: '', rows: '', cols: '', premiumRowEnd: '', priceNormal: '100', pricePremium: '200' },
      }));
      showSuccess('hall');
      getAdminHalls().then(setHalls).catch(() => {});
    } catch (err) {
      showError('hall', err.message || 'Invalid entry');
    }
  }
  async function handleMovie(e) {
    e.preventDefault();
    clearSectionAlert('movie');
    const totalMins = Number(form.movie.durationHours) * 60 + Number(form.movie.durationMinutes);
    if (totalMins <= 0) {
      showError('movie', 'Duration must be at least 1 minute');
      return;
    }
    try {
      await adminAddMovie(
        form.movie.name,
        totalMins,
        form.movie.description,
        form.movie.language,
        posterFile || undefined
      );
      setPosterFile(null);
      setForm((f) => ({ ...f, movie: { name: '', durationHours: '0', durationMinutes: '0', description: '', language: '' } }));
      showSuccess('movie');
      getAdminMovies().then(setMovies).catch(() => {});
    } catch (err) {
      showError('movie', err.message || 'Invalid entry');
    }
  }
  function toOffsetDateTime(localStr) {
    if (!localStr) return '';
    const d = new Date(localStr);
    const offsetMin = -d.getTimezoneOffset();
    const sign = offsetMin >= 0 ? '+' : '-';
    const hrs = String(Math.floor(Math.abs(offsetMin) / 60)).padStart(2, '0');
    const mins = String(Math.abs(offsetMin) % 60).padStart(2, '0');
    return localStr.length === 16 ? `${localStr}:00${sign}${hrs}:${mins}` : localStr;
  }
  async function handleShow(e) {
    e.preventDefault();
    clearSectionAlert('show');
    try {
      await adminAddShow(
        form.show.movieId,
        form.show.hallId,
        toOffsetDateTime(form.show.startTime),
        toOffsetDateTime(form.show.endTime)
      );
      setForm((f) => ({ ...f, show: { movieId: '', hallId: '', startTime: '', endTime: '' } }));
      showSuccess('show');
    } catch (err) {
      showError('show', err.message || 'Invalid entry');
    }
  }

  return (
    <div className="page admin-manage">
      <h1>Manage database</h1>
      <section className="admin-section">
        <h2>Add city</h2>
        <form onSubmit={handleCity}>
          <div className="form-row">
            <label>Name</label>
            <input
              value={form.city.name}
              onChange={(e) => setForm((f) => ({ ...f, city: { ...f.city, name: e.target.value } }))}
              required
            />
          </div>
          <div className="form-actions">
            <button type="submit">Add city</button>
            {sectionAlert.city && (
              <span className={`alert alert-${sectionAlert.city.type}`} role="alert">
                {sectionAlert.city.text}
              </span>
            )}
          </div>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add theatre</h2>
        <form onSubmit={handleTheatre}>
          <div className="form-row">
            <label>City</label>
            <select
              value={form.theatre.cityId}
              onChange={(e) => setForm((f) => ({ ...f, theatre: { ...f.theatre, cityId: e.target.value } }))}
              required
            >
              <option value="">Select city</option>
              {cities.map((c) => (
                <option key={c.cityId} value={c.cityId}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="form-row">
            <label>Name</label>
            <input
              value={form.theatre.name}
              onChange={(e) => setForm((f) => ({ ...f, theatre: { ...f.theatre, name: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Address</label>
            <input
              value={form.theatre.address}
              onChange={(e) => setForm((f) => ({ ...f, theatre: { ...f.theatre, address: e.target.value } }))}
              required
            />
          </div>
          <div className="form-actions">
            <button type="submit">Add theatre</button>
            {sectionAlert.theatre && (
              <span className={`alert alert-${sectionAlert.theatre.type}`} role="alert">
                {sectionAlert.theatre.text}
              </span>
            )}
          </div>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add hall (and seats)</h2>
        <form onSubmit={handleHall}>
          <div className="form-row">
            <label>Theatre</label>
            <select
              value={form.hall.theatreId}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, theatreId: e.target.value } }))}
              required
            >
              <option value="">Select theatre</option>
              {theatres.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </select>
          </div>
          <div className="form-row">
            <label>Hall name</label>
            <input
              value={form.hall.name}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, name: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Rows</label>
            <input
              type="number"
              value={form.hall.rows}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, rows: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Cols</label>
            <input
              type="number"
              value={form.hall.cols}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, cols: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Premium row end (1-based, required)</label>
            <input
              type="number"
              min="0"
              value={form.hall.premiumRowEnd}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, premiumRowEnd: e.target.value } }))}
              required
            />
          </div>
          <p className="muted" style={{ marginTop: '-10px' }}>
            0 = no premium rows. Example: 2 = rows 1 and 2 are premium.
          </p>
          <div className="form-row">
            <label>Price normal (₹, required)</label>
            <input
              type="number"
              min="0"
              placeholder="Rupees"
              value={form.hall.priceNormal}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, priceNormal: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Price premium (₹, required)</label>
            <input
              type="number"
              min="0"
              placeholder="Rupees"
              value={form.hall.pricePremium}
              onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, pricePremium: e.target.value } }))}
              required
            />
          </div>
          <div className="form-actions">
            <button type="submit">Add hall</button>
            {sectionAlert.hall && (
              <span className={`alert alert-${sectionAlert.hall.type}`} role="alert">
                {sectionAlert.hall.text}
              </span>
            )}
          </div>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add movie</h2>
        <form onSubmit={handleMovie}>
          <div className="form-row">
            <label>Name</label>
            <input
              value={form.movie.name}
              onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, name: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Duration (hh:mm)</label>
            <div className="duration-inputs">
              <input
                type="number"
                min="0"
                max="23"
                placeholder="HH"
                value={form.movie.durationHours}
                onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, durationHours: e.target.value } }))}
              />
              <span className="duration-sep">:</span>
              <input
                type="number"
                min="0"
                max="59"
                placeholder="MM"
                value={form.movie.durationMinutes}
                onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, durationMinutes: e.target.value } }))}
              />
            </div>
          </div>
          <div className="form-row">
            <label>Description</label>
            <input
              value={form.movie.description}
              onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, description: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Language</label>
            <input
              value={form.movie.language}
              onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, language: e.target.value } }))}
              required
            />
          </div>
          <div className="form-row">
            <label>Poster image (optional, max 2 MB, JPEG/PNG/WebP)</label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => setPosterFile(e.target.files?.[0] ?? null)}
            />
          </div>
          {posterFile && <p className="muted">Selected: {posterFile.name}</p>}
          <div className="form-actions">
            <button type="submit">Add movie</button>
            {sectionAlert.movie && (
              <span className={`alert alert-${sectionAlert.movie.type}`} role="alert">
                {sectionAlert.movie.text}
              </span>
            )}
          </div>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add show</h2>
        <form onSubmit={handleShow}>
          <div className="form-row">
            <label>Movie</label>
            <select
              value={form.show.movieId}
              onChange={(e) => {
                const movieId = e.target.value;
                setForm((f) => {
                  const next = { ...f, show: { ...f.show, movieId } };
                  if (f.show.startTime) {
                    const sel = movies.find((m) => String(m.id) === String(movieId));
                    const movieMins = sel?.durationMins != null ? Number(sel.durationMins) : 0;
                    const d = new Date(f.show.startTime);
                    d.setMinutes(d.getMinutes() + movieMins + 30);
                    next.show.endTime = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                  }
                  return next;
                });
              }}
              required
            >
              <option value="">Select movie</option>
              {movies.map((m) => (
                <option key={m.id} value={m.id}>{m.label}</option>
              ))}
            </select>
          </div>
          <div className="form-row">
            <label>Hall</label>
            <select
              value={form.show.hallId}
              onChange={(e) => setForm((f) => ({ ...f, show: { ...f.show, hallId: e.target.value } }))}
              required
            >
              <option value="">Select hall (theatre / hall)</option>
              {halls.map((h) => (
                <option key={h.id} value={h.id}>{h.label}</option>
              ))}
            </select>
          </div>
          <div className="form-row">
            <label>Start time</label>
            <input
              type="datetime-local"
              min={(() => {
                const d = new Date();
                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
              })()}
              value={form.show.startTime}
              onChange={(e) => {
                const start = e.target.value;
                if (!start) {
                  setForm((f) => ({ ...f, show: { ...f.show, startTime: '', endTime: '' } }));
                  return;
                }
                const sel = movies.find((m) => String(m.id) === String(form.show.movieId));
                const movieMins = sel?.durationMins != null ? Number(sel.durationMins) : 0;
                const totalMins = movieMins + 30;
                const d = new Date(start);
                d.setMinutes(d.getMinutes() + totalMins);
                const end = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                setForm((f) => ({ ...f, show: { ...f.show, startTime: start, endTime: end } }));
              }}
              required
            />
          </div>
          <div className="form-row">
            <label>End time (start + movie duration + 30 min)</label>
            <input
              type="datetime-local"
              value={form.show.endTime}
              readOnly
              tabIndex={-1}
              required
            />
          </div>
          <div className="form-actions">
            <button type="submit">Add show</button>
            {sectionAlert.show && (
              <span className={`alert alert-${sectionAlert.show.type}`} role="alert">
                {sectionAlert.show.text}
              </span>
            )}
          </div>
        </form>
        <p className="muted">Use ISO format for times (e.g. 2025-03-15T18:00). Backend expects OffsetDateTime.</p>
      </section>
    </div>
  );
}
