const nock = require('nock');
const { getAvailableDrivers } = require('./driverServiceClient');
const { ServiceClientError } = require('./httpClient');

describe('driverServiceClient', () => {
  afterEach(() => nock.cleanAll());

  test('requests available drivers with location, radius, and bearer token', async () => {
    const scope = nock('http://localhost:3002', {
      reqheaders: { authorization: 'Bearer driver-token' },
    })
      .get('/api/drivers/available')
      .query({ lat: '6.9', lng: '79.8', radius: '10' })
      .reply(200, [{ id: 'driver-1' }]);

    await expect(getAvailableDrivers({ lat: 6.9, lng: 79.8, radius: 10, token: 'driver-token' }))
      .resolves.toEqual([{ id: 'driver-1' }]);
    expect(scope.isDone()).toBe(true);
  });

  test('retries a transient upstream server error twice', async () => {
    const scope = nock('http://localhost:3002')
      .get('/api/drivers/available').query(true).reply(503, {})
      .get('/api/drivers/available').query(true).reply(503, {})
      .get('/api/drivers/available').query(true).reply(200, []);

    await expect(getAvailableDrivers({ lat: 6, lng: 79, radius: 5 })).resolves.toEqual([]);
    expect(scope.isDone()).toBe(true);
  });

  test('maps an exhausted upstream response to a typed service error', async () => {
    nock('http://localhost:3002').get('/api/drivers/available').query(true)
      .times(3).reply(503, {});

    await expect(getAvailableDrivers({ lat: 6, lng: 79, radius: 5 })).rejects.toMatchObject({
      name: 'ServiceClientError',
      service: 'driver-service',
      code: 'DRIVER_SERVICE_HTTP_ERROR',
      statusCode: 503,
    });
    await expect(getAvailableDrivers({ lat: 6, lng: 79, radius: 5 })).rejects.toBeInstanceOf(ServiceClientError);
  });

  test('rejects invalid search parameters before making a request', async () => {
    await expect(getAvailableDrivers({ lat: 91, lng: 79, radius: 0 })).rejects.toThrow(TypeError);
  });
});
