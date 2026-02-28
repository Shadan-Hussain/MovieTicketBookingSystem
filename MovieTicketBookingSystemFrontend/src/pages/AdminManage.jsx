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
  adminAddSeats,
  adminAddShow,
} from '../api';

export default function AdminManage() {
  const [alert, setAlert] = useState(null);
  const [cities, setCities] = useState([]);
  const [theatres, setTheatres] = useState([]);
  const [halls, setHalls] = useState([]);
  const [movies, setMovies] = useState([]);
  const [form, setForm] = useState({
    city: { name: '', stateCode: '' },
    theatre: { cityId: '', name: '', address: '' },
    hall: { theatreId: '', name: '', rows: '', cols: '', premiumRowStart: '', premiumRowEnd: '', priceNormal: '100', pricePremium: '200' },
    movie: { name: '', durationHours: '0', durationMinutes: '0', description: '', language: '', posterUrl: '' },
    show: { movieId: '', hallId: '', startTime: '', endTime: '' },
  });

  useEffect(() => {
    getCities().then(setCities).catch(() => setCities([]));
    getAdminTheatres().then(setTheatres).catch(() => setTheatres([]));
    getAdminHalls().then(setHalls).catch(() => setHalls([]));
    getAdminMovies().then(setMovies).catch(() => setMovies([]));
  }, []);

  function showSuccess() {
    setAlert({ type: 'success', text: 'Insert successful' });
    setTimeout(() => setAlert(null), 3000);
  }
  function showError(msg) {
    setAlert({ type: 'error', text: msg });
  }

  async function handleCity(e) {
    e.preventDefault();
    setAlert(null);
    try {
      await adminAddCity(form.city.name, form.city.stateCode);
      setForm((f) => ({ ...f, city: { name: '', stateCode: '' } }));
      showSuccess();
      getCities().then(setCities).catch(() => {});
    } catch (err) {
      showError(err.message || 'Invalid entry');
    }
  }
  async function handleTheatre(e) {
    e.preventDefault();
    setAlert(null);
    try {
      await adminAddTheatre(Number(form.theatre.cityId), form.theatre.name, form.theatre.address);
      setForm((f) => ({ ...f, theatre: { ...f.theatre, name: '', address: '' } }));
      showSuccess();
      getAdminTheatres().then(setTheatres).catch(() => {});
    } catch (err) {
      showError(err.message || 'Invalid entry');
    }
  }
  async function handleHall(e) {
    e.preventDefault();
    setAlert(null);
    try {
      const created = await adminAddHall(Number(form.hall.theatreId), form.hall.name);
      await adminAddSeats(
        created.id,
        Number(form.hall.rows),
        Number(form.hall.cols),
        form.hall.premiumRowStart === '' ? undefined : Number(form.hall.premiumRowStart),
        form.hall.premiumRowEnd === '' ? undefined : Number(form.hall.premiumRowEnd),
        form.hall.priceNormal === '' ? undefined : Number(form.hall.priceNormal),
        form.hall.pricePremium === '' ? undefined : Number(form.hall.pricePremium)
      );
      setForm((f) => ({
        ...f,
        hall: { ...f.hall, name: '', rows: '', cols: '', premiumRowStart: '', premiumRowEnd: '', priceNormal: '100', pricePremium: '200' },
      }));
      showSuccess();
      getAdminHalls().then(setHalls).catch(() => {});
    } catch (err) {
      showError(err.message || 'Invalid entry');
    }
  }
  async function handleMovie(e) {
    e.preventDefault();
    setAlert(null);
    const totalMins = Number(form.movie.durationHours) * 60 + Number(form.movie.durationMinutes);
    if (totalMins <= 0) {
      showError('Duration must be at least 1 minute');
      return;
    }
    try {
      await adminAddMovie(
        form.movie.name,
        totalMins,
        form.movie.description,
        form.movie.language,
        form.movie.posterUrl || null
      );
      setForm((f) => ({ ...f, movie: { name: '', durationHours: '0', durationMinutes: '0', description: '', language: '', posterUrl: '' } }));
      showSuccess();
      getAdminMovies().then(setMovies).catch(() => {});
    } catch (err) {
      showError(err.message || 'Invalid entry');
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
    setAlert(null);
    try {
      await adminAddShow(
        form.show.movieId,
        form.show.hallId,
        toOffsetDateTime(form.show.startTime),
        toOffsetDateTime(form.show.endTime)
      );
      setForm((f) => ({ ...f, show: { movieId: '', hallId: '', startTime: '', endTime: '' } }));
      showSuccess();
    } catch (err) {
      showError(err.message || 'Invalid entry');
    }
  }

  return (
    <div className="page admin-manage">
      <h1>Manage database</h1>
      {alert && (
        <div className={`alert alert-${alert.type}`} role="alert">
          {alert.text}
        </div>
      )}
      <section className="admin-section">
        <h2>Add city</h2>
        <form onSubmit={handleCity}>
          <input
            placeholder="Name"
            value={form.city.name}
            onChange={(e) => setForm((f) => ({ ...f, city: { ...f.city, name: e.target.value } }))}
            required
          />
          <input
            placeholder="State code"
            value={form.city.stateCode}
            onChange={(e) => setForm((f) => ({ ...f, city: { ...f.city, stateCode: e.target.value } }))}
            required
          />
          <button type="submit">Add city</button>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add theatre</h2>
        <form onSubmit={handleTheatre}>
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
          <input
            placeholder="Name"
            value={form.theatre.name}
            onChange={(e) => setForm((f) => ({ ...f, theatre: { ...f.theatre, name: e.target.value } }))}
            required
          />
          <input
            placeholder="Address"
            value={form.theatre.address}
            onChange={(e) => setForm((f) => ({ ...f, theatre: { ...f.theatre, address: e.target.value } }))}
            required
          />
          <button type="submit">Add theatre</button>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add hall (and seats)</h2>
        <form onSubmit={handleHall}>
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
          <input
            placeholder="Hall name"
            value={form.hall.name}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, name: e.target.value } }))}
            required
          />
          <input
            type="number"
            placeholder="Rows"
            value={form.hall.rows}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, rows: e.target.value } }))}
            required
          />
          <input
            type="number"
            placeholder="Cols"
            value={form.hall.cols}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, cols: e.target.value } }))}
            required
          />
          <input
            type="number"
            placeholder="Premium row start (0-based, optional)"
            value={form.hall.premiumRowStart}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, premiumRowStart: e.target.value } }))}
          />
          <input
            type="number"
            placeholder="Premium row end (optional)"
            value={form.hall.premiumRowEnd}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, premiumRowEnd: e.target.value } }))}
          />
          <input
            type="number"
            placeholder="Price normal (optional)"
            value={form.hall.priceNormal}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, priceNormal: e.target.value } }))}
          />
          <input
            type="number"
            placeholder="Price premium (optional)"
            value={form.hall.pricePremium}
            onChange={(e) => setForm((f) => ({ ...f, hall: { ...f.hall, pricePremium: e.target.value } }))}
          />
          <button type="submit">Add hall</button>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add movie</h2>
        <form onSubmit={handleMovie}>
          <input
            placeholder="Name"
            value={form.movie.name}
            onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, name: e.target.value } }))}
            required
          />
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
          <input
            placeholder="Description"
            value={form.movie.description}
            onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, description: e.target.value } }))}
            required
          />
          <input
            placeholder="Language"
            value={form.movie.language}
            onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, language: e.target.value } }))}
            required
          />
          <input
            placeholder="Poster URL (optional)"
            value={form.movie.posterUrl}
            onChange={(e) => setForm((f) => ({ ...f, movie: { ...f.movie, posterUrl: e.target.value } }))}
          />
          <button type="submit">Add movie</button>
        </form>
      </section>
      <section className="admin-section">
        <h2>Add show</h2>
        <form onSubmit={handleShow}>
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
          <label>Start time</label>
          <input
            type="datetime-local"
            placeholder="Start time"
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
          <label>End time (start + movie duration + 30 min)</label>
          <input
            type="datetime-local"
            placeholder="End time"
            value={form.show.endTime}
            readOnly
            tabIndex={-1}
            required
          />
          <button type="submit">Add show</button>
        </form>
        <p className="muted">Use ISO format for times (e.g. 2025-03-15T18:00). Backend expects OffsetDateTime.</p>
      </section>
    </div>
  );
}
