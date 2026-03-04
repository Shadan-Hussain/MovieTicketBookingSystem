import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMovie, getPosterUrl } from '../api';
import BackButton from '../components/BackButton';

export default function MovieDetail() {
  const { cityId, movieId } = useParams();
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [failedPoster, setFailedPoster] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getMovie(movieId)
      .then(setMovie)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [movieId]);

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
          {getPosterUrl(movie) && !failedPoster ? (
            <img
              src={getPosterUrl(movie)}
              alt=""
              className="movie-detail-poster"
              onError={() => setFailedPoster(true)}
            />
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
