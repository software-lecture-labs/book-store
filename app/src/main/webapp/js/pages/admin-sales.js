// The administrator's sales report: settled orders for the last 30 UTC calendar days.

import { api } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatDate, formatMoney, t } from '../i18n.js';
import { showError } from '../layout.js';
import { startAdminPage } from '../admin.js';

await startAdminPage();

try {
  const report = await api.get('/admin/sales');
  fill($('#sales-period'), t('admin.salesPeriod', {
    from: formatDate(report.from),
    to: formatDate(report.to),
  }));
  fill($('#sales-figures'),
    figure(t('admin.revenue'), formatMoney(report.revenue)),
    figure(t('admin.reportOrders'), report.orderCount),
    figure(t('admin.unitsSold'), report.unitsSold));
  fill($('#sales-chart'), chart(report.daily));
  fill($('#best-selling'), bookTable(report.bestSelling));
  fill($('#least-selling'), bookTable(report.leastSelling));
} catch (error) {
  showError(error);
}

function figure(label, value) {
  return h('div', { class: 'figure panel' }, h('span', { class: 'figure-label' }, label),
    h('strong', { class: 'figure-value' }, value));
}

function chart(days) {
  const maximum = Math.max(0, ...days.map((day) => day.revenue.amount));
  const dateFormatter = new Intl.DateTimeFormat(document.documentElement.lang, {
    day: 'numeric',
    month: 'numeric',
    timeZone: 'UTC',
  });
  return h('ol', { class: 'sales-chart', 'aria-label': t('admin.dailyRevenue') }, days.map((day) => {
    const date = new Date(`${day.date}T00:00:00Z`);
    const amount = day.revenue.amount;
    const height = maximum > 0 && amount > 0 ? Math.max(2, amount / maximum * 100) : 0;
    const label = t('admin.chartBar', { date: formatDate(day.date), revenue: formatMoney(day.revenue) });
    return h('li', { class: 'sales-chart-day' },
      h('div', { class: 'sales-chart-track' },
        h('div', { class: 'sales-chart-bar', role: 'img', 'aria-label': label, title: label,
          style: `height: ${height}%` })),
      h('time', { dateTime: day.date }, dateFormatter.format(date)));
  }));
}

function bookTable(books) {
  if (books.length === 0) {
    return h('p', { class: 'hint' }, t('admin.noBookSales'));
  }
  return h('div', { class: 'table-wrap' }, h('table', {},
    h('thead', {}, h('tr', {},
      h('th', {}, t('admin.bookName')),
      h('th', { class: 'number' }, t('admin.unitsSold')),
      h('th', { class: 'number' }, t('admin.bookRevenue')))),
    h('tbody', {}, books.map((book) => h('tr', {},
      h('td', {}, book.title),
      h('td', { class: 'number' }, book.unitsSold),
      h('td', { class: 'number' }, formatMoney(book.revenue)))))));
}
