const nock = require('nock');
const { validateUser } = require('./accountServiceClient');

describe('accountServiceClient', () => {
  afterEach(() => nock.cleanAll());

  test('validates user identity and role using the bearer token', async () => {
    const scope = nock('http://localhost:8081', {
      reqheaders: { authorization: 'Bearer account-token' },
    }).get('/api/users/user-123').reply(200, { id: 'user-123', role: 'PASSENGER' });

    await expect(validateUser({ userId: 'user-123', requiredRole: 'PASSENGER', token: 'account-token' }))
      .resolves.toMatchObject({ id: 'user-123', role: 'PASSENGER' });
    expect(scope.isDone()).toBe(true);
  });

  test('normalizes a prefixed role before comparison', async () => {
    nock('http://localhost:8081').get('/api/users/driver-123').reply(200, { role: 'ROLE_DRIVER' });
    await expect(validateUser({ userId: 'driver-123', requiredRole: 'DRIVER' })).resolves.toMatchObject({ role: 'ROLE_DRIVER' });
  });

  test('raises a typed forbidden error when the role does not match', async () => {
    nock('http://localhost:8081').get('/api/users/user-123').reply(200, { role: 'DRIVER' });
    await expect(validateUser({ userId: 'user-123', requiredRole: 'PASSENGER' })).rejects.toMatchObject({
      name: 'ServiceClientError',
      code: 'ACCOUNT_SERVICE_ROLE_MISMATCH',
      statusCode: 403,
    });
  });

  test('retries transient account errors twice then returns typed error', async () => {
    nock('http://localhost:8081').get('/api/users/user-123').times(3).reply(503, {});
    await expect(validateUser({ userId: 'user-123', requiredRole: 'PASSENGER' })).rejects.toMatchObject({
      name: 'ServiceClientError',
      code: 'ACCOUNT_SERVICE_HTTP_ERROR',
      statusCode: 503,
    });
  });
});
