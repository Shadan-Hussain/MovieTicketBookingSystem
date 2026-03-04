/**
 * Format date as dd/mm/yyyy.
 * @param {Date|string} date - Date instance or ISO date string
 * @returns {string} e.g. "04/03/2025"
 */
export function formatDateDDMMYYYY(date) {
  if (date == null) return '—';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d.getTime())) return '—';
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Format date and time as dd/mm/yyyy, HH:mm.
 * @param {Date|string} date
 * @returns {string} e.g. "04/03/2025, 14:30"
 */
export function formatDateTimeDDMMYYYY(date) {
  if (date == null) return '—';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d.getTime())) return '—';
  const dateStr = formatDateDDMMYYYY(d);
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${dateStr}, ${hours}:${minutes}`;
}

/**
 * Format duration in minutes as "Xh Ym".
 * @param {number} totalMinutes
 * @returns {string} e.g. "2h 30m" or "45m"
 */
function formatDuration(totalMinutes) {
  if (totalMinutes == null || totalMinutes < 0) return '';
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

/**
 * Format time only as HH:mm from ISO string or Date.
 * @param {string|Date} date - ISO string or Date
 * @returns {string} e.g. "14:30"
 */
export function formatTimeHHMM(date) {
  if (date == null) return '—';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d.getTime())) return '—';
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/**
 * Format show timing for ticket: date + start time only.
 * @param {string} startTime - ISO string for start
 * @returns {string} e.g. "04/03/2025, 14:30"
 */
export function formatShowTimingDateAndStart(startTime) {
  if (!startTime) return '—';
  const start = new Date(startTime);
  if (isNaN(start.getTime())) return '—';
  const dateStr = formatDateDDMMYYYY(start);
  const startStr = `${String(start.getHours()).padStart(2, '0')}:${String(start.getMinutes()).padStart(2, '0')}`;
  return `${dateStr}, ${startStr}`;
}

/**
 * Format show duration (end_time - start_time) as "Xh Ym".
 * @param {string} startTime - ISO string for start
 * @param {string} endTime - ISO string for end
 * @returns {string} e.g. "2h 30m" or "—"
 */
export function formatShowDuration(startTime, endTime) {
  if (!startTime || !endTime) return '—';
  const start = new Date(startTime);
  const end = new Date(endTime);
  if (isNaN(start.getTime()) || isNaN(end.getTime())) return '—';
  const durationMinutes = Math.round((end.getTime() - start.getTime()) / 60000);
  if (durationMinutes < 0) return '—';
  return formatDuration(durationMinutes);
}
