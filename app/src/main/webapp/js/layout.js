// What every page shares: the dictionary, the signed-in caller, the header with the cart badge,
// messages and form errors.

import { api, ApiError } from './api.js';
import { $, fill, h } from './dom.js';
import { LANGUAGES, language, loadDictionary, problemMessage, switchLanguage, t, translate } from './i18n.js';

let caller = null;

/**
 * Starts a page: loads the texts, finds out who is signed in and draws the header.
 *
 * @param {{signInRequired?: boolean, adminRequired?: boolean}} [options] whether the page is for signed-in
 *   customers or for administrators only; the API checks the role again (ACC-6)
 * @returns {Promise<object|null>} the signed-in customer's profile, or null for a visitor
 */
export async function startPage({ signInRequired = false, adminRequired = false } = {}) {
  await loadDictionary();
  caller = await api.get('/session').catch((error) => {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  });
  const here = encodeURIComponent(location.pathname.replace(/^\//, '') + location.search);
  if ((signInRequired || adminRequired) && !caller) {
    location.replace(`login.html?next=${here}`);
    return new Promise(() => {});
  }
  if (adminRequired && caller.role !== 'ADMIN') {
    location.replace('index.html');
    return new Promise(() => {});
  }
  drawHeader();
  translate();
  await refreshCartBadge();
  return caller;
}

/** @returns {object|null} the signed-in customer's profile */
export function currentCaller() {
  return caller;
}

function link(href, key, extra = {}) {
  const current = location.pathname.endsWith(`/${href}`) || (href === 'index.html' && location.pathname === '/');
  return h('a', { href, 'aria-current': current ? 'page' : null, ...extra }, t(key));
}

function drawHeader() {
  const header = $('#site-header');
  const nav = h('nav', { 'aria-label': t('nav.label') },
    link('index.html', 'nav.catalog'),
    h('a', { href: 'cart.html', class: 'cart-link' }, t('nav.cart'), ' ', h('span', { id: 'cart-badge', class: 'badge' }, '0')));
  if (caller?.role === 'ADMIN') {
    nav.append(link('admin.html', 'nav.admin'));
  }
  if (caller) {
    nav.append(link('orders.html', 'nav.orders'));
    if (caller.role === 'CUSTOMER') {
      nav.append(link('favorites.html', 'nav.favorites'));
    }
    nav.append(link('account.html', 'nav.account'),
      h('button', { type: 'button', class: 'link-button', onclick: signOut }, t('nav.signOut')));
  } else {
    nav.append(link('login.html', 'nav.signIn'), link('register.html', 'nav.register'));
  }
  const languages = h('div', { class: 'languages', role: 'group', 'aria-label': t('nav.language') },
    LANGUAGES.map((code) => h('button', {
      type: 'button',
      class: code === language() ? 'active' : null,
      'aria-pressed': code === language() ? 'true' : 'false',
      onclick: () => switchLanguage(code),
    }, code.toUpperCase())));
  fill(header,
    h('a', { href: 'index.html', class: 'brand' }, t('site.name')),
    caller ? h('span', { class: 'greeting' }, t('nav.hello', { name: caller.firstName })) : null,
    nav,
    languages);
}

async function signOut() {
  await api.delete('/session');
  location.href = 'index.html';
}

/** Shows the number of books in the cart in the header. */
export async function refreshCartBadge(cart) {
  const badge = $('#cart-badge');
  if (!badge) {
    return;
  }
  try {
    const current = cart ?? await api.get('/cart');
    badge.textContent = String(current.totalQuantity);
  } catch {
    badge.textContent = '?';
  }
}

/**
 * Shows a message above the page's content.
 *
 * @param {string} text the message
 * @param {'info'|'success'|'error'} [kind] its kind
 */
export function showMessage(text, kind = 'info') {
  const box = $('#messages');
  fill(box, h('p', { class: `message ${kind}`, role: kind === 'error' ? 'alert' : 'status' }, text));
  box.scrollIntoView({ block: 'nearest' });
}

/** Removes the messages. */
export function clearMessages() {
  fill($('#messages'));
}

/**
 * Shows a failed call: its message, and next to the form fields the errors the server found in them.
 *
 * @param {Error} error the failure
 * @param {HTMLFormElement} [form] the form that was sent
 */
export function showError(error, form) {
  if (form) {
    form.querySelectorAll('.field-error').forEach((element) => element.remove());
    form.querySelectorAll('[aria-invalid]').forEach((element) => element.removeAttribute('aria-invalid'));
  }
  const fieldErrors = error?.problem?.errors ?? [];
  const unplaced = [];
  for (const fieldError of fieldErrors) {
    const field = form?.elements.namedItem(fieldError.field.split('.').pop());
    if (field instanceof HTMLElement) {
      field.setAttribute('aria-invalid', 'true');
      field.insertAdjacentElement('afterend', h('span', { class: 'field-error' }, fieldError.message));
    } else {
      unplaced.push(`${fieldError.field}: ${fieldError.message}`);
    }
  }
  const reference = error?.status >= 500 && error.problem?.correlationId
    ? t('problem.reference', { id: error.problem.correlationId })
    : null;
  showMessage([problemMessage(error), ...unplaced, reference].filter(Boolean).join(' '), 'error');
}

/**
 * Runs an action for a button or form and shows its failure; the control is disabled meanwhile.
 *
 * @param {HTMLElement} control the button or form
 * @param {() => Promise<void>} action what to do
 * @param {HTMLFormElement} [form] the form whose fields may have errors
 */
export async function busy(control, action, form) {
  const buttons = control instanceof HTMLFormElement ? [...control.querySelectorAll('button')] : [control];
  buttons.forEach((button) => { button.disabled = true; });
  try {
    await action();
  } catch (error) {
    showError(error, form);
  } finally {
    buttons.forEach((button) => { button.disabled = false; });
  }
}
