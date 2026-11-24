// The signed-in customer's orders (ORD-5): the list, newest first, and one order with ?number=.

import { api } from '../api.js';
import { $, fill, h, param } from '../dom.js';
import { formatDateTime, formatMoney, t } from '../i18n.js';
import { busy, showError, showMessage, startPage } from '../layout.js';

await startPage({ signInRequired: true });

const number = param('number');
if (number) {
  if (param('placed')) {
    showMessage(t('orders.placed', { number }), 'success');
  }
  await showOrder(number);
} else {
  await showList(0);
}

function status(code) {
  return h('span', { class: `status ${code}` }, t(`status.${code}`));
}

async function showList(page) {
  const box = $('#orders');
  try {
    const result = await api.get(`/orders?page=${page}&size=10`);
    if (result.totalElements === 0) {
      fill(box, h('h1', {}, t('orders.heading')),
        h('p', { class: 'empty' }, t('orders.none'), ' ', h('a', { href: 'index.html' }, t('cart.browse'))));
      return;
    }
    const pager = h('nav', { class: 'pager' });
    if (result.totalPages > 1) {
      pager.append(
        h('button', { type: 'button', class: 'secondary', disabled: page === 0, onclick: () => showList(page - 1) },
          t('pager.previous')),
        h('span', {}, t('pager.position', { page: page + 1, pages: result.totalPages })),
        h('button', { type: 'button', class: 'secondary', disabled: page + 1 >= result.totalPages,
          onclick: () => showList(page + 1) }, t('pager.next')));
    }
    fill(box, h('h1', {}, t('orders.heading')),
      h('div', { class: 'panel table-wrap' }, h('table', {},
        h('thead', {}, h('tr', {}, h('th', {}, t('orders.number')), h('th', {}, t('orders.date')),
          h('th', {}, t('orders.status')), h('th', { class: 'number' }, t('orders.books')),
          h('th', { class: 'number' }, t('cart.total')))),
        h('tbody', {}, result.content.map((order) => h('tr', {},
          h('td', {}, h('a', { href: `orders.html?number=${encodeURIComponent(order.orderNumber)}` }, order.orderNumber)),
          h('td', {}, formatDateTime(order.placedAt)),
          h('td', {}, status(order.status)),
          h('td', { class: 'number' }, order.totalQuantity),
          h('td', { class: 'number' }, formatMoney(order.total))))))),
      pager);
  } catch (error) {
    showError(error);
  }
}

async function showOrder(orderNumber) {
  const box = $('#orders');
  try {
    const order = await api.get(`/orders/${encodeURIComponent(orderNumber)}`);
    const address = order.shippingAddress;
    const cancelButton = ['CREATED', 'PAID'].includes(order.status)
      ? h('button', { type: 'button', class: 'danger' }, t('orders.cancel'))
      : null;
    if (cancelButton) {
      cancelButton.addEventListener('click', () => busy(cancelButton, async () => {
        if (!confirm(t('orders.confirmCancel', { number: order.orderNumber }))) {
          return;
        }
        const cancelled = await api.post(`/orders/${encodeURIComponent(order.orderNumber)}/cancel`, {});
        showMessage(t('orders.cancelled'), 'success');
        await showOrder(cancelled.orderNumber);
      }));
    }
    fill(box,
      h('p', {}, h('a', { href: 'orders.html' }, t('orders.back'))),
      h('h1', {}, t('orders.order', { number: order.orderNumber })),
      h('div', { class: 'two-columns' },
        h('section', { class: 'panel' },
          h('dl', { class: 'facts' },
            h('dt', {}, t('orders.status')), h('dd', {}, status(order.status)),
            h('dt', {}, t('orders.date')), h('dd', {}, formatDateTime(order.placedAt)),
            h('dt', {}, t('orders.updated')), h('dd', {}, formatDateTime(order.statusChangedAt)),
            h('dt', {}, t('checkout.payment')), h('dd', {}, t(`payment.${order.paymentMethod}`))),
          cancelButton ? h('div', { class: 'actions' }, cancelButton) : null,
          h('h2', {}, t('checkout.address')),
          h('address', {}, address.recipient, h('br'), address.line, h('br'),
            `${address.district} / ${address.city} ${address.postalCode}`, h('br'), address.phone)),
        h('section', { class: 'panel' },
          h('div', { class: 'table-wrap' }, h('table', {},
            h('thead', {}, h('tr', {}, h('th', {}, t('cart.book')), h('th', { class: 'number' }, t('cart.quantity')),
              h('th', { class: 'number' }, t('cart.lineTotal')))),
            h('tbody', {}, order.lines.map((line) => h('tr', {},
              h('td', {}, h('a', { href: `book.html?id=${line.bookId}` }, line.bookTitle)),
              h('td', { class: 'number' }, line.quantity),
              h('td', { class: 'number' }, formatMoney(line.lineTotal))))))),
          h('dl', { class: 'totals' },
            h('dt', {}, t('cart.subtotal')), h('dd', {}, formatMoney(order.subtotal)),
            h('dt', {}, t('cart.shipping')), h('dd', {}, formatMoney(order.shippingCost)),
            h('dt', { class: 'grand' }, t('cart.total')), h('dd', { class: 'grand' }, formatMoney(order.total))))));
  } catch (error) {
    showError(error);
  }
}
