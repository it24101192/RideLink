const { createServiceClient } = require('./httpClient');

const client = createServiceClient({
  service: 'account-service',
  baseURL: process.env.ACCOUNT_SERVICE_URL || 'http://localhost:8081',
  timeoutMs: Number(process.env.ACCOUNT_SERVICE_TIMEOUT_MS) || 3000,
  retries: 2,
});

async function validateUser({ userId, requiredRole, token }) {
  if (typeof userId !== 'string' || userId.length === 0) throw new TypeError('userId is required');
  if (typeof requiredRole !== 'string' || requiredRole.length === 0) throw new TypeError('requiredRole is required');
  const user = await client.request({
    method: 'GET',
    url: `/api/users/${encodeURIComponent(userId)}`,
    headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {},
  });
  const role = String(user && user.role || '').replace(/^ROLE_/, '').toUpperCase();
  if (role !== requiredRole.replace(/^ROLE_/, '').toUpperCase()) {
    const { ServiceClientError } = require('./httpClient');
    throw new ServiceClientError({
      service: 'account-service',
      code: 'ACCOUNT_SERVICE_ROLE_MISMATCH',
      message: 'Account Service user role does not match the required role',
      statusCode: 403,
    });
  }
  return user;
}

module.exports = { validateUser };
