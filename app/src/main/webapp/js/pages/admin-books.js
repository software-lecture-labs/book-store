// Every book, those off sale too (ADM-1, ADM-3): price and stock change in place, a switch takes a
// book off sale or puts it back.

import { api, ifMatch, query } from '../api.js';
import { $, fill, h } from '../dom.js';
import { t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { pager, startAdminPage } from '../admin.js';

await startAdminPage();
const form = $('#search');
form.elements.namedItem('q').value = new URLSearchParams(location.search).get('q') ?? '';
await show(0);

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  await show(0);
});

async function show(page) {
  const text = form.elements.namedItem('q').value.trim();
  history.replaceState(null, '', `admin-books.html${query({ q: text })}`);
  try {
    const result = await api.get(`/admin/books${query({ q: text, page, size: 20 })}`);
    fill($('#books'), result.content.length === 0
      ? h('tr', {}, h('td', { colspan: 5, class: 'empty' }, t('catalog.none')))
      : result.content.map(row));
    pager($('#pager'), result, show);
  } catch (error) {
    showError(error);
  }
}

function row(book) {
  // The version this row last saw: every change sends it as If-Match and keeps the new one (ADR-0022).
  let seen = book;
  const price = h('input', { type: 'number', min: '0.01', step: '0.01', value: book.price.amount, class: 'short',
    'aria-label': t('admin.priceOf', { title: book.title }) });
  price.addEventListener('change', () => busy(price, async () => {
    seen = await api.put(`/admin/books/${book.id}/price`, { price: Number(price.value) }, ifMatch(seen));
    showMessage(t('admin.saved', { title: book.title }), 'success');
  }));
  const stock = h('input', { type: 'number', min: 0, step: 1, value: book.stock, class: 'short',
    'aria-label': t('admin.stockOf', { title: book.title }) });
  stock.addEventListener('change', () => busy(stock, async () => {
    seen = await api.put(`/admin/books/${book.id}/stock`, { stock: Number(stock.value) }, ifMatch(seen));
    showMessage(t('admin.saved', { title: book.title }), 'success');
  }));
  const active = h('input', { type: 'checkbox', checked: book.active, 'aria-label': t('admin.onSaleOf', { title: book.title }) });
  active.addEventListener('change', () => busy(active, async () => {
    seen = await api.put(`/admin/books/${book.id}/active`, { active: active.checked }, ifMatch(seen));
    showMessage(t(seen.active ? 'admin.backOnSale' : 'admin.offSale', { title: book.title }), 'success');
  }));
  return h('tr', { class: book.active ? null : 'muted' },
    h('td', {}, h('a', { href: `admin-book.html?id=${book.id}` }, book.title),
      h('div', { class: 'hint' }, `${book.authorNames} · ${book.isbn}`)),
    h('td', {}, book.category),
    h('td', {}, price),
    h('td', {}, stock),
    h('td', {}, active));
}
