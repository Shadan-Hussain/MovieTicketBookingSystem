import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom';
import Login from './pages/Login';
import Signup from './pages/Signup';
import CityList from './pages/CityList';
import MovieList from './pages/MovieList';
import ShowList from './pages/ShowList';
import SeatMap from './pages/SeatMap';
import ProceedingToPayment from './pages/ProceedingToPayment';
import PaymentSuccess from './pages/PaymentSuccess';
import PaymentCancel from './pages/PaymentCancel';
import AdminManage from './pages/AdminManage';
import MyTickets from './pages/MyTickets';
import TransactionHistory from './pages/TransactionHistory';

function Layout({ children, isLoggedIn, role, onLogout }) {
  const navigate = useNavigate();
  return (
    <>
      {isLoggedIn && (
        <nav className="nav">
          <Link to="/cities">Book ticket</Link>
          <Link to="/my-tickets">My tickets</Link>
          <Link to="/transaction-history">Transaction history</Link>
          {role === 'ADMIN' && <Link to="/manage">Manage database</Link>}
          <button type="button" className="link-btn" onClick={() => { onLogout(); navigate('/login'); }}>
            Logout
          </button>
        </nav>
      )}
      {children}
    </>
  );
}

function RequireAuth({ children, isLoggedIn }) {
  if (!isLoggedIn) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [role, setRole] = useState(() => localStorage.getItem('role') || 'USER');

  useEffect(() => {
    setRole(localStorage.getItem('role') || 'USER');
  }, [token]);

  function handleLogin() {
    setToken(localStorage.getItem('token'));
    setRole(localStorage.getItem('role') || 'USER');
  }
  function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('role');
    setToken(null);
    setRole('USER');
  }

  const isLoggedIn = !!token;

  return (
    <BrowserRouter>
      <Layout isLoggedIn={isLoggedIn} role={role} onLogout={handleLogout}>
        <main className="main">
          <Routes>
            <Route path="/login" element={isLoggedIn ? <Navigate to="/cities" replace /> : <Login onLogin={handleLogin} />} />
            <Route path="/signup" element={isLoggedIn ? <Navigate to="/cities" replace /> : <Signup />} />
            <Route path="/payment/success" element={<PaymentSuccess />} />
            <Route path="/payment/cancel" element={<PaymentCancel />} />
            <Route path="/cities" element={<RequireAuth isLoggedIn={isLoggedIn}><CityList /></RequireAuth>} />
            <Route path="/cities/:cityId/movies" element={<RequireAuth isLoggedIn={isLoggedIn}><MovieList /></RequireAuth>} />
            <Route path="/cities/:cityId/movies/:movieId/shows" element={<RequireAuth isLoggedIn={isLoggedIn}><ShowList /></RequireAuth>} />
            <Route path="/shows/:showId/seats" element={<RequireAuth isLoggedIn={isLoggedIn}><SeatMap /></RequireAuth>} />
            <Route path="/shows/:showId/seats/proceeding" element={<RequireAuth isLoggedIn={isLoggedIn}><ProceedingToPayment /></RequireAuth>} />
            <Route path="/manage" element={<RequireAuth isLoggedIn={isLoggedIn}><AdminManage /></RequireAuth>} />
            <Route path="/my-tickets" element={<RequireAuth isLoggedIn={isLoggedIn}><MyTickets /></RequireAuth>} />
            <Route path="/transaction-history" element={<RequireAuth isLoggedIn={isLoggedIn}><TransactionHistory /></RequireAuth>} />
            <Route path="/" element={<Navigate to={isLoggedIn ? '/cities' : '/login'} replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </Layout>
    </BrowserRouter>
  );
}
