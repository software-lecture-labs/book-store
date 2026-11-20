// Shows the parts of a cart that the cart and checkout pages share: what changed since the books
// were added (CRT-4) and the totals.

import { fill, h } from './dom.js';
import { formatMoney, t } from './i18n.js';

/**
 * Lists the changes the server made to a cart against the catalog.
 *
 * @param {HTMLElement} box where to show them
 * @param {Array<object>} notices the cart's notices
 */
export function showNotices(box, notices) {
  fill(box, notices.length === 0 ? null : h('div', { class: 'message', role: 'status' },
    h('p', {}, t('cart.changedHeading')),
    h('ul', {}, notices.map((notice) => h('li', {}, noticeText(notice))))));
}

function noticeText(notice) {
  switch (notice.type) {
    case 'REMOVED':
      return t('cart.noticeRemoved', { title: notice.bookTitle });
    case 'QUANTITY_REDUCED':
      return t('cart.noticeReduced', { title: notice.bookTitle, from: notice.fromQuantity, to: notice.toQuantity });
    case 'PRICE_CHANGED':
      return t('cart.noticePrice', { title: notice.bookTitle, from: formatMoney(notice.fromPrice), to: formatMoney(notice.toPrice) });
    default:
      return notice.bookTitle;
  }
}

/**
 * Shows a cart's subtotal, shipping and total.
 *
 * @param {object} cart the cart
 * @returns {HTMLElement} the totals
 */
export function totals(cart) {
  return h('dl', { class: 'totals' },
    h('dt', {}, t('cart.subtotal')), h('dd', {}, formatMoney(cart.subtotal)),
    h('dt', {}, t('cart.shipping')),
    h('dd', {}, cart.shippingCost.amount === 0 ? t('cart.freeShipping') : formatMoney(cart.shippingCost)),
    h('dt', { class: 'grand' }, t('cart.total')), h('dd', { class: 'grand' }, formatMoney(cart.total)));
}
