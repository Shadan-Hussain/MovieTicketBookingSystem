import { useState, useEffect } from 'react';
import { getMyTransactions } from '../api';

export default function TransactionHistory() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyTransactions()
      .then(setTransactions)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page">Loading…</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;

  return (
    <div className="page transaction-history">
      <h1>Transaction history</h1>
      {transactions.length === 0 ? (
        <p className="muted">No transactions yet.</p>
      ) : (
        <ul className="card-list">
          {transactions.map((t) => (
            <li key={t.transactionId} className="transaction-card">
              <p><strong>Transaction ID:</strong> {t.transactionId}</p>
              <p><strong>Show ID:</strong> {t.showId} · <strong>Seat ID:</strong> {t.seatId}</p>
              <p><strong>Amount:</strong> {t.amount / 100} {t.currency?.toUpperCase()}</p>
              <p><strong>Status:</strong> {t.status}</p>
              <p><strong>Created:</strong> {t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
