const EARTH_RADIUS_KM = 6371.0088;
const BASE_FARE_LKR = 200;
const PER_KM_RATE_LKR = 80;
const PER_MIN_RATE_LKR = 10;
const SURGE_MULTIPLIER = 1.5;
const MINIMUM_FARE_LKR = 300;

/**
 * Returns the Haversine distance between two latitude/longitude points in km.
 * Coordinates are degrees; latitude must be in [-90, 90] and longitude in [-180, 180].
 */
function haversineDistanceKm(fromLat, fromLng, toLat, toLng) {
  const coordinates = [fromLat, fromLng, toLat, toLng];
  if (!coordinates.every(Number.isFinite)) {
    throw new TypeError('Coordinates must be finite numbers');
  }
  if (fromLat < -90 || fromLat > 90 || toLat < -90 || toLat > 90
      || fromLng < -180 || fromLng > 180 || toLng < -180 || toLng > 180) {
    throw new RangeError('Coordinates are outside the valid latitude/longitude range');
  }

  const radians = (degrees) => (degrees * Math.PI) / 180;
  const latitudeDelta = radians(toLat - fromLat);
  const longitudeDelta = radians(toLng - fromLng);
  const startLatitude = radians(fromLat);
  const endLatitude = radians(toLat);

  const a = (Math.sin(latitudeDelta / 2) ** 2)
    + (Math.cos(startLatitude) * Math.cos(endLatitude)
      * (Math.sin(longitudeDelta / 2) ** 2));
  const boundedA = Math.min(1, Math.max(0, a));
  return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA));
}

function isPeakHour(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    throw new TypeError('time must be a valid Date');
  }

  // Read the clock in the business timezone so peak hours remain 7–9am and 5–7pm locally.
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Colombo',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const hour = Number(parts.find((part) => part.type === 'hour').value);
  return (hour >= 7 && hour < 9) || (hour >= 17 && hour < 19);
}

/**
 * Fare rule: (baseFare + distanceKm * perKmRate + durationMin * perMinRate)
 * multiplied by the peak surge factor, with a minimum charge of LKR 300.
 * The surge windows use Asia/Colombo local time and are start-inclusive/end-exclusive.
 */
function calculateFare({ distanceKm, durationMin, time = new Date() }) {
  if (!Number.isFinite(distanceKm) || distanceKm < 0) {
    throw new RangeError('distanceKm must be a finite non-negative number');
  }
  if (!Number.isFinite(durationMin) || durationMin < 0) {
    throw new RangeError('durationMin must be a finite non-negative number');
  }

  const base = BASE_FARE_LKR + (distanceKm * PER_KM_RATE_LKR) + (durationMin * PER_MIN_RATE_LKR);
  const multiplier = isPeakHour(time) ? SURGE_MULTIPLIER : 1;
  const fare = Math.max(MINIMUM_FARE_LKR, base * multiplier);
  return Math.round((fare + Number.EPSILON) * 100) / 100;
}

module.exports = {
  BASE_FARE_LKR,
  PER_KM_RATE_LKR,
  PER_MIN_RATE_LKR,
  SURGE_MULTIPLIER,
  MINIMUM_FARE_LKR,
  calculateFare,
  haversineDistanceKm,
  isPeakHour,
};
