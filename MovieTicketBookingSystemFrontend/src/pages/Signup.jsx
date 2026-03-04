import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signup } from '../api';

export default function Signup() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [alert, setAlert] = useState(null);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setAlert(null);
    try {
      await signup(username, password, email, name);
      setAlert({ type: 'success', text: 'Account created' });
      setTimeout(() => {
        setAlert(null);
        navigate('/login');
      }, 1500);
    } catch (err) {
      setError(err.message || 'Signup failed');
    }
  }

  return (
    <div className="page">
      <h1>Sign up</h1>
      {alert && <div className={`alert alert-${alert.type}`} role="alert">{alert.text}</div>}
      {error && <div className="alert alert-error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <button type="submit">Sign up</button>
      </form>
      <p><Link to="/login">Login</Link> if you already have an account.</p>
    </div>
  );
}
