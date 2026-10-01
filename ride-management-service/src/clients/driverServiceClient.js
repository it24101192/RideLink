const { createServiceClient } = require('./httpClient');

const client = createServiceClient({
  service: 'driver-service',
  baseURL: process.env.DRIVER_SERVICE_URL || 'http://localhost:3002',
  timeoutMs: Number(process.env.DRIVER_SERVICE_TIMEOUT_MS) || 3000,
  retries: 2,
});

async function getAvailableDrivers({ lat, lng, radius, token }) {
  if (![lat, lng, radius].every(Number.isFinite) || radius <= 0) {
    throw new TypeError('lat, lng, and a positive radius are required');
  }
  return client.request({
    method: 'GET',
    url: '/api/drivers/available',
    params: { lat, lng, radius },
    headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {},
  });
}

module.exports = { getAvailableDrivers };
