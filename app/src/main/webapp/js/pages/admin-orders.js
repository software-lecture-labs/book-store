// Every order (ADM-5): filtered by status; one order with ?number= and the statuses it may move to.

import { api, query } from '../api.js';
import { $, fill, h, param } from '../dom.js';
import { formatDateTime, formatMoney, t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { pager, startAdminPage } from '../admin.js';

const STATUSES = ['CREATED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

await startAdminPage();

const number = param('number');
if (number) {
  await showOrder(number);
} else {
  await showList(param('status') ?? '', 0);
}

function status(code) {
  return h('span', { class: `status ${code}` }, t(`status.${code}`));
}

async function showList(filter, page) {
  history.replaceState(null, '', `admin-orders.html${query({ status: filter })}`);
  const box = $('#orders');
  try {
    const result = await api.get(`/admin/orders${query({ status: filter, page, size: 20 })}`);
    const select = h('select', { id: 'status-filter', onchange: (event) => showList(event.target.value, 0) },
      h('option', { value: '' }, t('admin.allStatuses')),
      STATUSES.map((code) => h('option', { value: code, selected: code === filter }, t(`status.${code}`))));
    const pages = h('nav', { class: 'pager' });
    pager(pages, result, (next) => showList(filter, next));
    fill(box,
      h('div', { class: 'heading-row' }, h('h1', {}, t('admin.orders')),
        h('label', { class: 'inline' }, t('orders.status'), select)),
      result.totalElements === 0 ? h('p', { class: 'empty' }, t('admin.noOrders')) : h('div', { class: 'panel table-wrap' },
        h('table', {},
          h('thead', {}, h('tr', {}, h('th', {}, t('orders.number')), h('th', {}, t('orders.date')),
            h('th', {}, t('orders.status')), h('th', {}, t('checkout.payment')),
            h('th', { class: 'number' }, t('orders.books')), h('th', { class: 'number' }, t('cart.total')))),
          h('tbody', {}, result.content.map((order) => h('tr', {},
            h('td', {}, h('a', { href: `admin-orders.html?number=${encodeURIComponent(order.orderNumber)}` }, order.orderNumber)),
            h('td', {}, formatDateTime(order.placedAt)),
            h('td', {}, status(order.status)),
            h('td', {}, t(`payment.${order.paymentMethod}`)),
            h('td', { class: 'number' }, order.totalQuantity),
            h('td', { class: 'number' }, formatMoney(order.total))))))),
      pages);
  } catch (error) {
    showError(error);
  }
}

async function showOrder(orderNumber) {
  try {
    drawOrder(await api.get(`/admin/orders/${encodeURIComponent(orderNumber)}`));
  } catch (error) {
    showError(error);
  }
}

function drawOrder(order) {
  const address = order.shippingAddress;
  const moves = [...order.nextStatuses].sort((a, b) => STATUSES.indexOf(a) - STATUSES.indexOf(b)).map((next) => {
    const button = h('button', { type: 'button', class: next === 'CANCELLED' ? 'danger' : null },
      t(`admin.moveTo.${next}`));
    button.addEventListener('click', () => busy(button, async () => {
      if (next === 'CANCELLED' && !confirm(t('admin.confirmCancel', { number: order.orderNumber }))) {
        return;
      }
      drawOrder(await api.put(`/admin/orders/${encodeURIComponent(order.orderNumber)}/status`, { status: next }));
      showMessage(t('admin.statusChanged', { status: t(`status.${next}`) }), 'success');
    }));
    return button;
  });
  fill($('#orders'),
    h('p', {}, h('a', { href: 'admin-orders.html' }, t('admin.backToOrders'))),
    h('h1', {}, t('orders.order', { number: order.orderNumber })),
    h('div', { class: 'two-columns' },
      h('section', { class: 'panel' },
        h('dl', { class: 'facts' },
          h('dt', {}, t('orders.status')), h('dd', {}, status(order.status)),
          h('dt', {}, t('admin.customerNo')), h('dd', {}, order.customerId),
          h('dt', {}, t('orders.date')), h('dd', {}, formatDateTime(order.placedAt)),
          h('dt', {}, t('orders.updated')), h('dd', {}, formatDateTime(order.statusChangedAt)),
          h('dt', {}, t('checkout.payment')), h('dd', {}, t(`payment.${order.paymentMethod}`))),
        moves.length === 0 ? h('p', { class: 'hint' }, t('admin.finalStatus')) : h('div', { class: 'actions' }, moves),
        h('h2', {}, t('checkout.address')),
        h('address', {}, address.recipient, h('br'), address.line, h('br'),
          `${address.district} / ${address.city} ${address.postalCode}`, h('br'), address.phone)),
      h('section', { class: 'panel' },
        h('div', { class: 'table-wrap' }, h('table', {},
          h('thead', {}, h('tr', {}, h('th', {}, t('cart.book')), h('th', { class: 'number' }, t('cart.quantity')),
            h('th', { class: 'number' }, t('cart.lineTotal')))),
          h('tbody', {}, order.lines.map((line) => h('tr', {},
            h('td', {}, h('a', { href: `admin-book.html?id=${line.bookId}` }, line.bookTitle)),
            h('td', { class: 'number' }, line.quantity),
            h('td', { class: 'number' }, formatMoney(line.lineTotal))))))),
        h('dl', { class: 'totals' },
          h('dt', {}, t('cart.subtotal')), h('dd', {}, formatMoney(order.subtotal)),
          h('dt', {}, t('cart.shipping')), h('dd', {}, formatMoney(order.shippingCost)),
          h('dt', { class: 'grand' }, t('cart.total')), h('dd', { class: 'grand' }, formatMoney(order.total))))));
}
