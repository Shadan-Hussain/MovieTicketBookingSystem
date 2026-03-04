import { useNavigate, Link } from 'react-router-dom';

/** Back icon button for top-left of page. Use `to` for explicit path or omit to use browser back. */
export default function BackButton({ to, ariaLabel = 'Go back' }) {
  const navigate = useNavigate();

  const className = 'back-button';

  if (to != null && to !== '') {
    return (
      <Link to={to} className={className} aria-label={ariaLabel}>
        <span className="back-icon" aria-hidden>←</span>
      </Link>
    );
  }

  return (
    <button type="button" className={className} aria-label={ariaLabel} onClick={() => navigate(-1)}>
      <span className="back-icon" aria-hidden>←</span>
    </button>
  );
}
