import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getTicket } from '../api';

export default function PaymentSuccess() {
  const [ticket, setTicket] = useState(null);
  const [message, setMessage] = useState('Payment successful, redirecting...');
  const showId = sessionStorage.getItem('payment_showId');
  const seatId = sessionStorage.getItem('payment_seatId');

  useEffect(() => {
    if (!showId || !seatId) {
      setMessage('Missing booking info.');
      return;
    }
    let cancelled = false;
    const poll = async () => {
      for (let i = 0; i < 30; i++) {
        if (cancelled) return;
        try {
          const t = await getTicket(showId, seatId);
          if (t) {
            setTicket(t);
            setMessage('');
            sessionStorage.removeItem('payment_showId');
            sessionStorage.removeItem('payment_seatId');
            return;
          }
        } catch {
          // ignore
        }
        await new Promise((r) => setTimeout(r, 2000));
      }
      if (!cancelled) setMessage('Ticket not ready yet. Please check My Tickets later.');
    };
    poll();
    return () => { cancelled = true; };
  }, [showId, seatId]);

  return (
    <div className="page payment-result success">
      <h1>Payment successful</h1>
      {message && <p>{message}</p>}
      {ticket && (
        <>
          <div className="ticket-card">
            <h2>Your ticket</h2>
            <p><strong>Ticket ID:</strong> {ticket.ticketId}</p>
            <p><strong>Show ID:</strong> {ticket.showId}</p>
            <p><strong>Seat ID:</strong> {ticket.seatId}</p>
            <p><strong>Transaction ID:</strong> {ticket.transactionId}</p>
          </div>
          <p style={{ marginTop: '1rem' }}>
            <Link to="/cities" className="btn-primary">Go back to main page</Link>
          </p>
        </>
      )}
      {!ticket && message && (message.includes('not ready') || message.includes('Missing')) && (
        <p style={{ marginTop: '1rem' }}>
          <Link to="/cities" className="btn-primary">Go back to main page</Link>
        </p>
      )}
    </div>
  );
}
