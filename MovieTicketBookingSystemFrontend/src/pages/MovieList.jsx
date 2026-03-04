import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMoviesByCity, getPosterUrl } from '../api';

export default function MovieList() {
  const { cityId } = useParams();
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [failedPosters, setFailedPosters] = useState(() => new Set());
  const navigate = useNavigate();

  function onPosterError(movieId) {
    setFailedPosters((prev) => new Set(prev).add(movieId));
  }

  useEffect(() => {
    getMoviesByCity(cityId)
      .then(setMovies)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [cityId]);

  if (loading) return <div className="page">Loading movies...</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;

  return (
    <div className="page">
      <h1>Choose a movie</h1>
      <ul className="card-list movie-cards">
        {movies.map((m) => (
          <li key={m.movieId}>
            <button
              type="button"
              className="card movie-card"
              onClick={() => navigate(`/cities/${cityId}/movies/${m.movieId}`)}
            >
              <div className="movie-poster-box">
                {getPosterUrl(m) && !failedPosters.has(m.movieId) ? (
                  <img src={getPosterUrl(m)} alt="" className="movie-poster" onError={() => onPosterError(m.movieId)} />
                ) : null}
                <span className={`movie-poster-placeholder ${!getPosterUrl(m) || failedPosters.has(m.movieId) ? 'visible' : ''}`} aria-hidden>No poster</span>
              </div>
              <div className="movie-card-info">
                <strong>{m.name}</strong>
                <span>{m.language}</span>
              </div>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
