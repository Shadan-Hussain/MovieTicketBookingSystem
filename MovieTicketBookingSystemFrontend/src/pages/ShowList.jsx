import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getShows } from '../api';

function parseDT(s) {
  if (!s) return { date: '', dateKey: '', time: '' };
  const d = new Date(s);
  const dateKey = d.toISOString().slice(0, 10);
  return {
    date: d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
    dateKey,
    time: d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    sortKey: d.getTime(),
  };
}

export default function ShowList() {
  const { cityId, movieId } = useParams();
  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedDateKey, setSelectedDateKey] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getShows(cityId, movieId)
      .then((list) => {
        const withSort = list.map((s) => ({
          ...s,
          ...parseDT(s.startTime),
          endInfo: parseDT(s.endTime),
        }));
        withSort.sort((a, b) => a.sortKey - b.sortKey);
        setShows(withSort);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [cityId, movieId]);

  const { dateOptions, showsByTheatre } = useMemo(() => {
    const dates = [...new Set(shows.map((s) => s.dateKey).filter(Boolean))].sort();
    const firstDate = dates[0] || '';
    const selected = selectedDateKey || firstDate;
    const forDate = shows.filter((s) => s.dateKey === selected);
    const byTheatre = {};
    forDate.forEach((s) => {
      const key = s.theatreName || `Hall ${s.hallId}`;
      if (!byTheatre[key]) byTheatre[key] = [];
      byTheatre[key].push(s);
    });
    Object.keys(byTheatre).forEach((k) => byTheatre[k].sort((a, b) => a.sortKey - b.sortKey));
    return {
      dateOptions: dates.map((dk) => {
        const s = shows.find((x) => x.dateKey === dk);
        return { dateKey: dk, label: s ? s.date : dk };
      }),
      showsByTheatre: byTheatre,
    };
  }, [shows, selectedDateKey]);

  useEffect(() => {
    if (dateOptions.length && !selectedDateKey) setSelectedDateKey(dateOptions[0].dateKey);
  }, [dateOptions, selectedDateKey]);

  if (loading) return <div className="page">Loading shows...</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;
  if (shows.length === 0) return <div className="page"><p className="muted">No shows available.</p></div>;

  return (
    <div className="page show-list-page">
      <h1>Choose a show</h1>
      <div className="show-dates-row" role="tablist" aria-label="Select date">
        <div className="show-dates-scroll">
          {dateOptions.map(({ dateKey, label }) => (
            <button
              key={dateKey}
              type="button"
              className={`show-date-chip ${selectedDateKey === dateKey ? 'selected' : ''}`}
              onClick={() => setSelectedDateKey(dateKey)}
              role="tab"
              aria-selected={selectedDateKey === dateKey}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      <div className="show-theatres">
        {Object.entries(showsByTheatre).map(([theatreName, theatreShows]) => (
          <div key={theatreName} className="show-theatre-block">
            <h2 className="show-theatre-name">{theatreName}</h2>
            <div className="show-time-boxes">
              {theatreShows.map((s) => (
                <button
                  key={s.showId}
                  type="button"
                  className="show-time-box"
                  onClick={() => navigate(`/shows/${s.showId}/seats`)}
                >
                  <span className="show-time">{s.time}</span>
                  <span className="show-time-end">{s.endInfo.time}</span>
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
