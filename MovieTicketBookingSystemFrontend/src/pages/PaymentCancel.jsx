import { Link } from 'react-router-dom';

export default function PaymentCancel() {
  return (
    <div className="page payment-result fail">
      <h1>Payment failed</h1>
      <p>Payment was cancelled or failed. You have not been charged.</p>
      <Link to="/cities" className="btn-primary">Back to cities</Link>
    </div>
  );
}
