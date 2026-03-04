import { useState, useEffect } from 'react';
import { getMyTickets } from '../api';
import { formatShowTimingDateAndStart, formatShowDuration } from '../utils/dateFormat';

export default function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyTickets()
      .then(setTickets)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page">Loading…</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;

  return (
    <div className="page">
      <h1>My tickets</h1>
      {tickets.length === 0 ? (
        <p className="muted">You have no tickets yet.</p>
      ) : (
        <ul className="card-list">
          {tickets.map((t) => (
            <li key={t.ticketId} className="ticket-card">
              <p><strong>Ticket ID:</strong> {t.ticketId}</p>
              <p><strong>Show ID:</strong> {t.showId}</p>
              <p><strong>Seat number:</strong> {t.seatNumber ?? t.seatId ?? '—'}</p>
              <p><strong>Transaction ID:</strong> {t.transactionId}</p>
              {t.theatreName && <p><strong>Theatre:</strong> {t.theatreName}</p>}
              {t.theatreAddress && <p><strong>Address:</strong> {t.theatreAddress}</p>}
              {t.hallName && <p><strong>Hall:</strong> {t.hallName}</p>}
              {t.showStartTime && <p><strong>Show timing:</strong> {formatShowTimingDateAndStart(t.showStartTime)}</p>}
              {formatShowDuration(t.showStartTime, t.showEndTime) !== '—' && (
                <p><strong>Show duration:</strong> {formatShowDuration(t.showStartTime, t.showEndTime)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
