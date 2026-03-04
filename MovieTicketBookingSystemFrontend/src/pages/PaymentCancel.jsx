import { Link } from 'react-router-dom';

export default function PaymentCancel() {
  return (
    <div className="page payment-result">
      <h1>Payment cancelled</h1>
      <Link to="/cities" className="btn-primary">Back to main page</Link>
    </div>
  );
}
