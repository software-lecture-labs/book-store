// The page texts in Turkish and English (NFR-4, ADR-0011). Turkish is the default; the choice is
// remembered in the browser. Error messages are chosen by the problem's code, not its English detail.

const STORAGE_KEY = 'bookstore.language';

/** The languages the pages are written in; the first is the default. */
export const LANGUAGES = ['tr', 'en'];

let dictionary = {};
let current = LANGUAGES[0];

function storedLanguage() {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

/** Loads the dictionary of the chosen language; call it before anything is shown. */
export async function loadDictionary() {
  const stored = storedLanguage();
  current = LANGUAGES.includes(stored) ? stored : LANGUAGES[0];
  document.documentElement.lang = current;
  const response = await fetch(`i18n/${current}.json`, { credentials: 'same-origin' });
  dictionary = await response.json();
}

/** @returns {string} the current language code */
export function language() {
  return current;
}

/**
 * Switches the language and shows the page again.
 *
 * @param {string} code "tr" or "en"
 */
export function switchLanguage(code) {
  try {
    localStorage.setItem(STORAGE_KEY, code);
  } catch {
    // Without storage the choice lasts for this page only.
  }
  location.reload();
}

/**
 * Returns a text.
 *
 * @param {string} key the dictionary key
 * @param {object} [values] values for "{name}" placeholders
 * @returns {string} the text, or the key itself if the dictionary lacks it
 */
export function t(key, values = {}) {
  let text = dictionary[key] ?? key;
  for (const [name, value] of Object.entries(values)) {
    text = text.replaceAll(`{${name}}`, String(value));
  }
  return text;
}

/**
 * Fills the elements marked with data-i18n (text), data-i18n-placeholder, data-i18n-title and data-i18n-label (aria-label).
 *
 * @param {ParentNode} [root] where to look
 */
export function translate(root = document) {
  root.querySelectorAll('[data-i18n]').forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
  root.querySelectorAll('[data-i18n-placeholder]').forEach((element) => {
    element.placeholder = t(element.dataset.i18nPlaceholder);
  });
  root.querySelectorAll('[data-i18n-title]').forEach((element) => {
    element.title = t(element.dataset.i18nTitle);
  });
  root.querySelectorAll('[data-i18n-label]').forEach((element) => {
    element.setAttribute('aria-label', t(element.dataset.i18nLabel));
  });
}

function locale() {
  return current === 'tr' ? 'tr-TR' : 'en-GB';
}

/**
 * Formats money as the API writes it.
 *
 * @param {{amount: number, currency: string}} money the amount
 * @returns {string} for example "95,00 ₺"
 */
export function formatMoney(money) {
  return new Intl.NumberFormat(locale(), { style: 'currency', currency: money.currency, currencyDisplay: 'narrowSymbol' }).format(money.amount);
}

/**
 * Formats a moment of the API.
 *
 * @param {string} instant an ISO-8601 instant
 * @returns {string} the local date and time
 */
export function formatDateTime(instant) {
  return new Intl.DateTimeFormat(locale(), { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(instant));
}

/**
 * Formats an ISO calendar date without changing its UTC day.
 *
 * @param {string} date an ISO date such as "2026-06-22"
 * @returns {string} a localized calendar date
 */
export function formatDate(date) {
  return new Intl.DateTimeFormat(locale(), { dateStyle: 'medium', timeZone: 'UTC' })
    .format(new Date(`${date}T00:00:00Z`));
}

/**
 * Returns the message for a failed call: the text for its problem code if there is one, else the
 * server's own detail.
 *
 * @param {{code?: string, message?: string}} error the failure
 * @returns {string} the message
 */
export function problemMessage(error) {
  const key = `problem.${error?.code}`;
  if (error?.code && key in dictionary) {
    return t(key);
  }
  return error?.message || t('problem.unknown');
}
