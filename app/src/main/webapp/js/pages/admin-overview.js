// The administrator's overview (ADM-8): books on sale, books running low and orders waiting to be
// shipped. The numbers come from the catalog and the sales module separately (ADR-0021).

import { api } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatDateTime, formatMoney, t } from '../i18n.js';
import { showError } from '../layout.js';
import { startAdminPage } from '../admin.js';

const LOW_STOCK_BELOW = 5;

await startAdminPage();

try {
  const [stats, waiting] = await Promise.all([
    api.get(`/admin/books/stats?lowStockBelow=${LOW_STOCK_BELOW}`),
    api.get('/admin/orders?status=PAID&size=5'),
  ]);
  fill($('#figures'),
    figure(t('admin.booksOnSale'), stats.activeBooks, t('admin.ofTotal', { total: stats.totalBooks })),
    figure(t('admin.runningLow'), stats.lowStock.length, t('admin.fewerThan', { count: stats.lowStockBelow })),
    figure(t('admin.toShip'), waiting.totalElements, t('status.PAID')));
  fill($('#low-stock'), stats.lowStock.length === 0
    ? h('p', { class: 'hint' }, t('admin.noLowStock'))
    : h('div', { class: 'table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {}, h('th', {}, t('cart.book')), h('th', { class: 'number' }, t('admin.stock')))),
      h('tbody', {}, stats.lowStock.map((book) => h('tr', {},
        h('td', {}, h('a', { href: `admin-book.html?id=${book.id}` }, book.title), h('div', { class: 'hint' }, book.authorNames)),
        h('td', { class: 'number' }, book.stock)))))));
  fill($('#waiting-orders'), waiting.totalElements === 0
    ? h('p', { class: 'hint' }, t('admin.noWaitingOrders'))
    : [h('ul', { class: 'plain-list' }, waiting.content.map((order) => h('li', {},
      h('a', { href: `admin-orders.html?number=${encodeURIComponent(order.orderNumber)}` }, order.orderNumber),
      ` · ${formatDateTime(order.placedAt)} · ${formatMoney(order.total)}`))),
    h('p', {}, h('a', { href: 'admin-orders.html?status=PAID' }, t('admin.allWaitingOrders')))]);
} catch (error) {
  showError(error);
}

function figure(label, value, note) {
  return h('div', { class: 'figure panel' }, h('span', { class: 'figure-label' }, label),
    h('strong', { class: 'figure-value' }, value), h('span', { class: 'hint' }, note));
}
