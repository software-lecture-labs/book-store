// The cart (CRT-1 – CRT-5): a visitor's cart lives in the session and joins the stored cart at
// sign-in (ADR-0020). Quantities are changed in place; every answer is the whole cart.

import { api } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatMoney, t } from '../i18n.js';
import { busy, refreshCartBadge, showError, startPage } from '../layout.js';
import { showNotices, totals } from '../cart-view.js';

const caller = await startPage();

try {
  show(await api.get('/cart'));
} catch (error) {
  showError(error);
}

function show(cart) {
  refreshCartBadge(cart);
  showNotices($('#notices'), cart.notices);
  const box = $('#cart');
  if (cart.lines.length === 0) {
    fill(box, h('p', { class: 'empty' }, t('cart.empty'), ' ', h('a', { href: 'index.html' }, t('cart.browse'))));
    return;
  }
  const clear = h('button', { type: 'button', class: 'danger' }, t('cart.clear'));
  clear.addEventListener('click', () => busy(clear, async () => show(await api.delete('/cart'))));
  const checkout = caller
    ? h('a', { class: 'button', href: 'checkout.html' }, t('cart.checkout'))
    : h('a', { class: 'button', href: `login.html?next=${encodeURIComponent('checkout.html')}` }, t('cart.signInToCheckout'));
  fill(box,
    h('div', { class: 'table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {},
        h('th', {}, t('cart.book')), h('th', { class: 'number' }, t('cart.unitPrice')),
        h('th', {}, t('cart.quantity')), h('th', { class: 'number' }, t('cart.lineTotal')), h('th', {}, ''))),
      h('tbody', {}, cart.lines.map(line)))),
    totals(cart),
    caller ? null : h('p', { class: 'hint' }, t('cart.guestHint')),
    h('div', { class: 'actions' }, clear, checkout));
}

function line(item) {
  const quantity = h('input', {
    type: 'number', min: 0, max: Math.min(10, item.availableStock), value: item.quantity,
    'aria-label': t('cart.quantityOf', { title: item.title }),
  });
  quantity.addEventListener('change', () => busy(quantity, async () => {
    show(await api.put(`/cart/items/${item.bookId}`, { quantity: Number(quantity.value) }));
  }));
  const remove = h('button', { type: 'button', class: 'secondary' }, t('cart.remove'));
  remove.addEventListener('click', () => busy(remove, async () => show(await api.delete(`/cart/items/${item.bookId}`))));
  return h('tr', {},
    h('td', {}, h('a', { href: `book.html?id=${item.bookId}` }, item.title), h('div', { class: 'hint' }, item.authorNames)),
    h('td', { class: 'number' }, formatMoney(item.unitPrice)),
    h('td', {}, quantity),
    h('td', { class: 'number' }, formatMoney(item.lineTotal)),
    h('td', {}, remove));
}
