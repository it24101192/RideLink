const axios = require('axios');

const DEFAULT_TIMEOUT_MS = 3000;
const DEFAULT_RETRIES = 2;
const RETRY_DELAY_MS = 100;

class ServiceClientError extends Error {
  constructor({ service, code, message, statusCode = 502, cause }) {
    super(message);
    this.name = 'ServiceClientError';
    this.service = service;
    this.code = code;
    this.statusCode = statusCode;
    if (cause) this.cause = cause;
  }
}

function mapAxiosError(error, service) {
  if (error instanceof ServiceClientError) return error;
  const serviceCode = service.toUpperCase().replace(/[^A-Z0-9]+/g, '_');
  if (error.response) {
    const statusCode = error.response.status;
    return new ServiceClientError({
      service,
      code: statusCode === 404 ? `${serviceCode}_NOT_FOUND` : `${serviceCode}_HTTP_ERROR`,
      message: `${service} returned HTTP ${statusCode}`,
      statusCode: statusCode >= 400 && statusCode < 500 ? 502 : 503,
      cause: error,
    });
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return new ServiceClientError({
      service,
      code: `${serviceCode}_TIMEOUT`,
      message: `${service} request timed out`,
      statusCode: 504,
      cause: error,
    });
  }
  return new ServiceClientError({
    service,
    code: `${serviceCode}_UNAVAILABLE`,
    message: `${service} is unavailable`,
    statusCode: 503,
    cause: error,
  });
}

function createServiceClient({ service, baseURL, timeoutMs = DEFAULT_TIMEOUT_MS, retries = DEFAULT_RETRIES, axiosInstance }) {
  if (!service || !baseURL) throw new TypeError('service and baseURL are required');
  const client = axiosInstance || axios.create({ baseURL, timeout: timeoutMs });

  async function request(config) {
    let lastError;
    for (let attempt = 0; attempt <= retries; attempt += 1) {
      try {
        const response = await client.request({ timeout: timeoutMs, ...config });
        return response.data;
      } catch (error) {
        lastError = error;
        const retryable = !error.response || error.response.status >= 500;
        if (!retryable || attempt === retries) throw mapAxiosError(error, service);
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS * (attempt + 1)));
      }
    }
    throw mapAxiosError(lastError, service);
  }

  return { request };
}

module.exports = { ServiceClientError, createServiceClient, mapAxiosError };
