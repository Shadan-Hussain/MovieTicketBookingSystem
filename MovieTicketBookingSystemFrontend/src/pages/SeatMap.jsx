import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getShow, getSeatsForShow } from '../api';
import BackButton from '../components/BackButton';
import { formatDateDDMMYYYY, formatTimeHHMM, formatShowDuration } from '../utils/dateFormat';

/**
 * Flow: (1) Load show details and seats once from backend, store locally.
 * (2) Selected/unselected state is frontend-only (no backend lock until Proceed).
 * (3) Refresh page = fetch again. (4) Proceed to payment = backend lock then redirect.
 */
export default function SeatMap() {
  const { showId } = useParams();
  const [showInfo, setShowInfo] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeatId, setSelectedSeatId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const fetchData = useCallback(() => {
    setLoading(true);
    setError('');
    Promise.all([getShow(showId), getSeatsForShow(showId)])
      .then(([show, seatList]) => {
        setShowInfo(show);
        setSeats(seatList);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [showId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

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

  if (loading) {
    return (
      <div className="page">
        <div className="page-header-with-back">
          <BackButton />
          <h1>Choose a seat</h1>
        </div>
        <p>Loading seats...</p>
      </div>
    );
  }

  const rows = {};
  seats.forEach((s) => {
    const r = s.rowNum ?? 0;
    if (!rows[r]) rows[r] = [];
    rows[r].push(s);
  });
  const rowNums = Object.keys(rows).map(Number).sort((a, b) => a - b);

  const premiumPrice = seats.find((s) => s.type === 'PREMIUM')?.price;
  const normalPrice = seats.find((s) => s.type === 'NORMAL')?.price;

  let lastType = null;
  const rowNumsWithDivider = [];
  for (const r of rowNums) {
    const rowSeats = rows[r];
    const rowType = rowSeats?.[0]?.type ?? 'NORMAL';
    if (lastType != null && rowType !== lastType) {
      const nextLabel = rowType === 'PREMIUM' ? 'Premium' : 'Normal';
      const nextPrice = rowType === 'PREMIUM' ? premiumPrice : normalPrice;
      rowNumsWithDivider.push({ divider: true, sectionLabel: nextLabel, sectionPrice: nextPrice });
    }
    rowNumsWithDivider.push({ divider: false, rowNum: r });
    lastType = rowType;
  }

  const firstRowType = rowNums.length ? (rows[rowNums[0]]?.[0]?.type ?? 'NORMAL') : null;
  const firstSectionLabel = firstRowType === 'PREMIUM' ? 'Premium' : 'Normal';
  const firstSectionPrice = firstRowType === 'PREMIUM' ? premiumPrice : normalPrice;

  return (
    <div className="page seat-map-page">
      <div className="page-header-with-back page-header-with-back--centered">
        <BackButton />
        <div className="page-header-center">
          <h1>Choose a seat</h1>
        </div>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {showInfo && (
        <div className="seat-map-show-info">
          {showInfo.movieName && <p><strong>Movie:</strong> {showInfo.movieName}</p>}
          {showInfo.hallName && <p><strong>Hall:</strong> {showInfo.hallName}</p>}
          {showInfo.theatreName && <p><strong>Theatre:</strong> {showInfo.theatreName}</p>}
          {showInfo.startTime && (
            <>
              <p><strong>Show date:</strong> {formatDateDDMMYYYY(showInfo.startTime)}</p>
              <p><strong>Show start time:</strong> {formatTimeHHMM(showInfo.startTime)}</p>
              {formatShowDuration(showInfo.startTime, showInfo.endTime) !== '—' && (
                <p><strong>Show duration:</strong> {formatShowDuration(showInfo.startTime, showInfo.endTime)}</p>
              )}
            </>
          )}
        </div>
      )}
      <div className="seat-layout-center">
        <div className="seat-grid">
          {firstRowType != null && (
            <div className="seat-section-divider seat-section-divider--first">
              <span className="seat-section-divider-label">
                {firstSectionPrice != null ? `${firstSectionLabel} — ₹${firstSectionPrice}` : firstSectionLabel}
              </span>
            </div>
          )}
          {rowNumsWithDivider.map((item, idx) => {
            if (item.divider) {
              const label = item.sectionPrice != null
                ? `${item.sectionLabel} — ₹${item.sectionPrice}`
                : item.sectionLabel;
              return (
                <div key={`div-${idx}`} className="seat-section-divider">
                  <span className="seat-section-divider-label">{label}</span>
                </div>
              );
            }
          const r = item.rowNum;
          return (
            <div key={r} className="seat-row">
              {rows[r].sort((a, b) => (a.colNum ?? 0) - (b.colNum ?? 0)).map((s) => {
                const isUnselectable = s.status === 'LOCKED' || s.status === 'BOOKED';
                const isSelected = selectedSeatId === s.seatId;
                return (
                  <button
                    key={s.seatId}
                    type="button"
                    className={`seat-box ${s.status === 'AVAILABLE' ? 'available' : 'unavailable'} ${isSelected ? 'selected' : ''}`}
                    disabled={isUnselectable}
                    title={`${s.number}${s.price != null ? ` — ₹${s.price}` : ''}`}
                    onClick={() => handleSelectSeat(s)}
                  >
                    {s.number}
                  </button>
                );
              })}
            </div>
          );
        })}
          <div className="screen-indicator" aria-hidden>
            <span className="screen-label">Screen</span>
          </div>
        </div>
      </div>
      {selectedSeatId && (
        <div className="proceed-row proceed-row--center">
          <button type="button" className="btn-primary" onClick={handleProceed}>
            Proceed to payment
          </button>
        </div>
      )}
    </div>
  );
}
