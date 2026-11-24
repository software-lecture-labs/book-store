// Calls the REST API under /api/v1 (ADR-0009, ADR-0011). Every request carries the client header
// that the server requires on state-changing requests signed in with the session cookie (ADR-0020).

/** The header the server checks against cross-site requests. */
export const CLIENT_HEADER = 'X-Bookstore-Client';

/** A failed call: the HTTP status and the RFC 9457 problem the server sent. */
export class ApiError extends Error {
  /**
   * @param {number} status the HTTP status
   * @param {object|null} problem the problem body
   */
  constructor(status, problem) {
    super(problem?.detail ?? `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem ?? {};
    this.code = this.problem.code ?? null;
  }
}

async function request(method, path, body, extraHeaders = {}) {
  const headers = { [CLIENT_HEADER]: 'web', Accept: 'application/json', ...extraHeaders };
  const options = { method, headers, credentials: 'same-origin' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(`/api/v1${path}`, options);
  } catch (networkFailure) {
    throw new ApiError(0, { code: 'network', detail: networkFailure.message });
  }
  if (response.status === 204) {
    return null;
  }
  const type = response.headers.get('Content-Type') ?? '';
  const data = type.includes('json') ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(response.status, data);
  }
  return data;
}

/** The API's methods; each returns the parsed JSON body or throws an {@link ApiError}. */
export const api = {
  get: (path) => request('GET', path),
  post: (path, body = {}, headers = {}) => request('POST', path, body, headers),
  put: (path, body = {}, headers = {}) => request('PUT', path, body, headers),
  delete: (path) => request('DELETE', path),
};

/**
 * Builds a query string from the given values, leaving out empty ones.
 *
 * @param {object} values the parameters
 * @returns {string} "?a=1&b=2", or "" if nothing is set
 */
export function query(values) {
  const parameters = new URLSearchParams();
  for (const [name, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '' && value !== false) {
      parameters.set(name, String(value));
    }
  }
  const text = parameters.toString();
  return text ? `?${text}` : '';
}

/**
 * Sends a file as the body of a PUT, for example a cover image (ADR-0021).
 *
 * @param {string} path the path under /api/v1
 * @param {File} file the file; its type becomes the Content-Type
 * @returns {Promise<object>} the parsed JSON answer
 */
export async function upload(path, file) {
  let response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method: 'PUT',
      headers: { [CLIENT_HEADER]: 'web', Accept: 'application/json', 'Content-Type': file.type || 'application/octet-stream' },
      credentials: 'same-origin',
      body: file,
    });
  } catch (networkFailure) {
    throw new ApiError(0, { code: 'network', detail: networkFailure.message });
  }
  const data = (response.headers.get('Content-Type') ?? '').includes('json') ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(response.status, data);
  }
  return data;
}

/**
 * The If-Match header for a record the page read, so a change made meanwhile by someone else is
 * refused instead of overwritten (ADR-0022).
 *
 * @param {{version: number}} record what the API answered
 * @returns {object} the header
 */
export function ifMatch(record) {
  return { 'If-Match': `"${record.version}"` };
}
