import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMovie, hasPoster, fetchPosterBlobUrl } from '../api';
import BackButton from '../components/BackButton';

export default function MovieDetail() {
  const { cityId, movieId } = useParams();
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [posterBlobUrl, setPosterBlobUrl] = useState(null);
  const [failedPoster, setFailedPoster] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getMovie(movieId)
      .then(setMovie)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [movieId]);

  const posterUrlRef = useRef(null);
  useEffect(() => {
    if (!movie || !hasPoster(movie)) {
      if (posterUrlRef.current) {
        URL.revokeObjectURL(posterUrlRef.current);
        posterUrlRef.current = null;
      }
      setPosterBlobUrl(null);
      return;
    }
    let cancelled = false;
    fetchPosterBlobUrl(movie.movieId)
      .then((url) => {
        if (cancelled) {
          URL.revokeObjectURL(url);
          return;
        }
        if (posterUrlRef.current) URL.revokeObjectURL(posterUrlRef.current);
        posterUrlRef.current = url;
        setPosterBlobUrl(url);
      })
      .catch(() => {
        if (!cancelled) setFailedPoster(true);
      });
    return () => {
      cancelled = true;
      if (posterUrlRef.current) {
        URL.revokeObjectURL(posterUrlRef.current);
        posterUrlRef.current = null;
      }
      setPosterBlobUrl(null);
    };
  }, [movie, movieId]);

  if (loading) {
    return (
      <div className="page">
        <div className="page-header-with-back">
          <BackButton to={`/cities/${cityId}/movies`} />
        </div>
        <p>Loading...</p>
      </div>
    );
  }
  if (error) {
    return (
      <div className="page">
        <div className="page-header-with-back">
          <BackButton to={`/cities/${cityId}/movies`} />
        </div>
        <div className="alert alert-error">{error}</div>
      </div>
    );
  }
  if (!movie) return null;

  return (
    <div className="page movie-detail-page">
      <div className="page-header-with-back">
        <BackButton to={`/cities/${cityId}/movies`} />
      </div>
      <div className="movie-detail-layout">
        <div className="movie-detail-poster-wrap">
          {posterBlobUrl && !failedPoster ? (
            <img
              src={posterBlobUrl}
              alt=""
              className="movie-detail-poster"
              onError={() => setFailedPoster(true)}
            />
          ) : hasPoster(movie) && !failedPoster ? (
            <div className="movie-detail-poster-placeholder">Loading…</div>
          ) : (
            <div className="movie-detail-poster-placeholder">No poster</div>
          )}
        </div>
        <div className="movie-detail-info">
          <h1>{movie.name}</h1>
          <p className="movie-detail-meta">
            {movie.language}
            {movie.durationMins != null && ` · ${movie.durationMins} min`}
          </p>
          {movie.description && <p className="movie-detail-description">{movie.description}</p>}
          <button
            type="button"
            className="btn-primary"
            onClick={() => navigate(`/cities/${cityId}/movies/${movieId}/shows`)}
          >
            Book tickets
          </button>
        </div>
      </div>
    </div>
  );
}
