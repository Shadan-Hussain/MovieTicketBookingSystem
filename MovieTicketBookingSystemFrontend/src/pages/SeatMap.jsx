import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSeatsForShow } from '../api';

/**
 * Flow: (1) Load seats once from backend, store locally.
 * (2) Selected/unselected state is frontend-only (no backend lock until Proceed).
 * (3) Refresh page = fetch seats again. (4) Proceed to payment = backend lock then redirect.
 */
export default function SeatMap() {
  const { showId } = useParams();
  const [seats, setSeats] = useState([]);
  const [selectedSeatId, setSelectedSeatId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const fetchSeats = useCallback(() => {
    getSeatsForShow(showId)
      .then(setSeats)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [showId]);

  useEffect(() => {
    fetchSeats();
  }, [fetchSeats]);

  function handleSelectSeat(seat) {
    if (seat.status !== 'AVAILABLE') return;
    setError('');
    setSelectedSeatId((prev) => (prev === seat.seatId ? null : seat.seatId));
  }

  function handleProceed() {
    if (!selectedSeatId) return;
    setError('');
    sessionStorage.setItem('payment_showId', showId);
    sessionStorage.setItem('payment_seatId', String(selectedSeatId));
    navigate(`/shows/${showId}/seats/proceeding`, { state: { seatId: selectedSeatId }, replace: true });
  }

  if (loading) return <div className="page">Loading seats...</div>;

  const rows = {};
  seats.forEach((s) => {
    const r = s.rowNum ?? 0;
    if (!rows[r]) rows[r] = [];
    rows[r].push(s);
  });
  const rowNums = Object.keys(rows).map(Number).sort((a, b) => a - b);

  return (
    <div className="page">
      <h1>Choose a seat (one only)</h1>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="seat-legend">
        <span><span className="seat-box available" /> Available</span>
        <span><span className="seat-box locked" /> Locked</span>
        <span><span className="seat-box booked" /> Booked</span>
        <span><span className="seat-box selected" /> Selected</span>
      </div>
      <div className="seat-grid">
        {rowNums.map((r) => (
          <div key={r} className="seat-row">
            {rows[r].sort((a, b) => (a.colNum ?? 0) - (b.colNum ?? 0)).map((s) => {
              const isUnselectable = s.status === 'LOCKED' || s.status === 'BOOKED';
              const isSelected = selectedSeatId === s.seatId;
              return (
                <button
                  key={s.seatId}
                  type="button"
                  className={`seat-box ${s.status?.toLowerCase() ?? 'available'} ${isSelected ? 'selected' : ''}`}
                  disabled={isUnselectable}
                  title={s.number}
                  onClick={() => handleSelectSeat(s)}
                >
                  {s.number}
                </button>
              );
            })}
          </div>
        ))}
      </div>
      {selectedSeatId && (
        <div className="proceed-row">
          <button type="button" className="btn-primary" onClick={handleProceed}>
            Proceed to payment
          </button>
        </div>
      )}
    </div>
  );
}
