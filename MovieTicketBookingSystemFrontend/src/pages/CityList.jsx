import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCities } from '../api';

export default function CityList() {
  const [cities, setCities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getCities()
      .then(setCities)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page">Loading cities...</div>;
  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;

  return (
    <div className="page">
      <h1>Choose a city</h1>
      <ul className="card-list">
        {cities.map((c) => (
          <li key={c.cityId}>
            <button
              type="button"
              className="card"
              onClick={() => navigate(`/cities/${c.cityId}/movies`)}
            >
              {c.name}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
