import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getShows } from '../api';

function parseDT(s) {
  if (!s) return { date: '', time: '' };
  const d = new Date(s);
  return {
    date: d.toLocaleDateString(),
    time: d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    sortKey: d.getTime(),
  };
}

export default function ShowList() {
  const { cityId, movieId } = useParams();
  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
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

  if (loading) return <div className="page">Loading shows...</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;

  return (
    <div className="page">
      <h1>Choose a show</h1>
      <p className="muted">Sorted by date, then start time</p>
      <ul className="card-list show-list">
        {shows.map((s) => (
          <li key={s.showId}>
            <button
              type="button"
              className="card"
              onClick={() => navigate(`/shows/${s.showId}/seats`)}
            >
              <strong>{s.date}</strong>
              <span>{s.time} – {s.endInfo.time}</span>
              <span className="muted">Show ID: {s.showId}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
