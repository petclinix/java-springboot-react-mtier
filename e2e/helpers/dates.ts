/**
 * Returns tomorrow's date as YYYY-MM-DD, suitable for filling an
 * `<input type="date">`. Locations created via ensureVetIsAlwaysOpen() are
 * open every day of the week, so "tomorrow" always yields a non-empty slot
 * list regardless of which appointment type is chosen or which weekday the
 * suite happens to run on.
 */
export function tomorrowDateString(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().substring(0, 10);
}

/**
 * Returns yesterday's date as YYYY-MM-DD — used to exercise the "no slots in
 * the past" behavior (the backend filters out already-elapsed slots rather
 * than rejecting the date outright).
 */
export function yesterdayDateString(): string {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().substring(0, 10);
}
