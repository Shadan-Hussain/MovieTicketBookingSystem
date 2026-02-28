import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../api';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const data = await login(username, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('userId', String(data.userId));
      localStorage.setItem('role', data.role || 'USER');
      onLogin?.();
      navigate(data.role === 'ADMIN' ? '/manage' : '/cities');
    } catch (err) {
      setError(err.message || 'Login failed');
    }
  }

  return (
    <div className="page">
      <h1>Login</h1>
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
        <button type="submit">Login</button>
      </form>
      <p><Link to="/signup">Sign up</Link> if you don&apos;t have an account.</p>
    </div>
  );
}
