const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8081';

function getToken() {
  return localStorage.getItem('token');
}

function getAuthHeaders() {
  const token = getToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function signup(username, password, email, name) {
  const res = await fetch(`${API_BASE}/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, email, name }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || err.error || 'Signup failed');
  }
  return res.json();
}

export async function login(username, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || err.error || 'Login failed');
  }
  return res.json();
}

export async function getCities() {
  const res = await fetch(`${API_BASE}/cities`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch cities');
  return res.json();
}

export async function getMoviesByCity(cityId) {
  const res = await fetch(`${API_BASE}/movies?city_id=${cityId}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch movies');
  return res.json();
}

export async function getMovie(movieId) {
  const res = await fetch(`${API_BASE}/movies/${movieId}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch movie');
  return res.json();
}

export async function getShows(cityId, movieId) {
  const res = await fetch(`${API_BASE}/shows?city_id=${cityId}&movie_id=${movieId}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch shows');
  return res.json();
}

export async function getShow(showId) {
  const res = await fetch(`${API_BASE}/shows/${showId}`, { headers: getAuthHeaders() });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed to fetch show');
  }
  return res.json();
}

export async function getSeatsForShow(showId) {
  const res = await fetch(`${API_BASE}/shows/${showId}/seats`, { headers: getAuthHeaders() });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed to fetch seats');
  }
  return res.json();
}

export async function lockSeat(showId, seatId) {
  const res = await fetch(`${API_BASE}/shows/${showId}/seats/${seatId}/lock`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Lock failed');
  }
  return res.json();
}

export async function getPaymentSession(showId, seatId) {
  const res = await fetch(`${API_BASE}/shows/${showId}/seats/${seatId}/payment-session`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Payment session failed');
  }
  return res.json();
}

export async function getTicket(showId, seatId) {
  const res = await fetch(`${API_BASE}/tickets?show_id=${showId}&seat_id=${seatId}`, { headers: getAuthHeaders() });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed to fetch ticket');
  }
  return res.json();
}

export async function getMyTickets() {
  const res = await fetch(`${API_BASE}/tickets`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch tickets');
  return res.json();
}

export async function getMyTransactions() {
  const res = await fetch(`${API_BASE}/transactions`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch transactions');
  return res.json();
}

// Admin options for dropdowns (id + label)
export async function getAdminTheatres() {
  const res = await fetch(`${API_BASE}/admin/options/theatres`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch theatres');
  return res.json();
}
export async function getAdminHalls() {
  const res = await fetch(`${API_BASE}/admin/options/halls`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch halls');
  return res.json();
}
export async function getAdminMovies() {
  const res = await fetch(`${API_BASE}/admin/options/movies`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to fetch movies');
  return res.json();
}

// Admin APIs
export async function adminAddCity(name, stateCode) {
  const res = await fetch(`${API_BASE}/admin/cities`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ name, stateCode }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json();
}

export async function adminAddTheatre(cityId, name, address) {
  const res = await fetch(`${API_BASE}/admin/theatres`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ cityId, name, address }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json();
}

export async function adminAddHall(theatreId, name) {
  const res = await fetch(`${API_BASE}/admin/halls`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ theatreId, name }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json(); // { id: hallId }
}

/** Add movie with optional poster. Sends multipart: part "movie" (JSON), optional part "file" (poster image). */
export async function adminAddMovie(name, durationMins, description, language, posterFile = null) {
  const formData = new FormData();
  formData.append('movie', new Blob([JSON.stringify({ name, durationMins, description, language })], { type: 'application/json' }));
  if (posterFile) formData.append('file', posterFile);
  const headers = getToken() ? { Authorization: `Bearer ${getToken()}` } : {};
  const res = await fetch(`${API_BASE}/admin/movies`, {
    method: 'POST',
    headers,
    body: formData,
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json();
}

/** Whether movie has a poster (for conditional loading). */
export function hasPoster(movie) {
  return Boolean(movie?.hasPoster && movie?.movieId);
}

/**
 * Fetches poster image with JWT and returns a blob URL. Caller must revoke the URL when done:
 * URL.revokeObjectURL(url) to avoid memory leaks.
 */
export async function fetchPosterBlobUrl(movieId) {
  const token = getToken();
  const res = await fetch(`${API_BASE}/movies/${movieId}/poster`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Poster not found');
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

export async function adminAddSeats(hallId, rows, cols, premiumRowStart, premiumRowEnd, priceNormal, pricePremium) {
  const res = await fetch(`${API_BASE}/admin/halls/${hallId}/seats`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      rows: Number(rows),
      cols: Number(cols),
      premiumRowStart: premiumRowStart !== '' ? Number(premiumRowStart) : undefined,
      premiumRowEnd: premiumRowEnd !== '' ? Number(premiumRowEnd) : undefined,
      priceNormal: priceNormal !== '' ? Number(priceNormal) : undefined,
      pricePremium: pricePremium !== '' ? Number(pricePremium) : undefined,
    }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json();
}

export async function adminAddShow(movieId, hallId, startTime, endTime) {
  const res = await fetch(`${API_BASE}/admin/shows`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ movieId: Number(movieId), hallId: Number(hallId), startTime, endTime }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || data.error || 'Failed');
  }
  return res.json();
}
