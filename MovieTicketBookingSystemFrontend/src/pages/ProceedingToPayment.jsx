import { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { lockSeat, getPaymentSession } from '../api';

let paymentFlowStarted = false;

export default function ProceedingToPayment() {
  const { showId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState('');
  const [status, setStatus] = useState('Locking seat…');

  useEffect(() => {
    const seatIdFromState = location.state?.seatId;
    const seatIdFromStorage = sessionStorage.getItem('payment_seatId');
    const seatId = seatIdFromState != null ? String(seatIdFromState) : seatIdFromStorage;
    if (!showId || !seatId) {
      setError('Missing show or seat. Go back and choose a seat.');
      return;
    }
    if (paymentFlowStarted) return;
    paymentFlowStarted = true;

    (async () => {
      try {
        setStatus('Locking seat…');
        await lockSeat(showId, seatId);
        setStatus('Proceeding to payment…');
        const data = await getPaymentSession(showId, seatId);
        const sessionUrl = data?.sessionUrl ?? data?.url;
        if (sessionUrl) {
          sessionStorage.setItem('payment_showId', showId);
          sessionStorage.setItem('payment_seatId', seatId);
          window.location.replace(sessionUrl);
          return;
        }
        setError('No payment URL received');
      } catch (err) {
        paymentFlowStarted = false;
        setError(err?.message || err?.error || 'Something went wrong.');
      }
    })();
    return () => {
      setTimeout(() => { paymentFlowStarted = false; }, 100);
    };
  }, [showId, location.state?.seatId]);

  if (error) {
    return (
      <div className="page">
        <div className="alert alert-error">{error}</div>
        <button type="button" onClick={() => navigate(-1)}>Go back</button>
      </div>
    );
  }

  return (
    <div className="page payment-proceeding">
      <h1>Proceeding to payment</h1>
      <p>{status}</p>
    </div>
  );
}
