// What the administration pages share (ADR-0021): the role check, the tabs between the pages,
// a pager and the category choices. The API checks the role on every call as well (ACC-6).

import { $, fill, h } from './dom.js';
import { t } from './i18n.js';
import { startPage } from './layout.js';

const TABS = [
  ['admin.html', 'admin.tabOverview'],
  ['admin-sales.html', 'admin.tabSales'],
  ['admin-books.html', 'admin.tabBooks'],
  ['admin-catalog.html', 'admin.tabCatalog'],
  ['admin-orders.html', 'admin.tabOrders'],
  ['admin-customers.html', 'admin.tabCustomers'],
  ['admin-reviews.html', 'admin.tabReviews'],
];

/**
 * Starts an administration page: only administrators stay, the others are sent away.
 *
 * @returns {Promise<object>} the administrator's profile
 */
export async function startAdminPage() {
  const caller = await startPage({ adminRequired: true });
  const page = location.pathname.split('/').pop() || 'admin.html';
  fill($('#admin-nav'), TABS.map(([href, key]) =>
    h('a', { href, 'aria-current': page === href || (href === 'admin-books.html' && page === 'admin-book.html') ? 'page' : null },
      t(key))));
  return caller;
}

/**
 * Draws the previous and next buttons of a page of results.
 *
 * @param {HTMLElement} box where to draw them
 * @param {{number: number, totalPages: number}} result the page the API sent
 * @param {(page: number) => void} show shows another page
 */
export function pager(box, result, show) {
  fill(box);
  if (result.totalPages <= 1) {
    return;
  }
  box.append(
    h('button', { type: 'button', class: 'secondary', disabled: result.number === 0, onclick: () => show(result.number - 1) },
      t('pager.previous')),
    h('span', {}, t('pager.position', { page: result.number + 1, pages: result.totalPages })),
    h('button', { type: 'button', class: 'secondary', disabled: result.number + 1 >= result.totalPages,
      onclick: () => show(result.number + 1) }, t('pager.next')));
}

/**
 * Lists a category tree as options, children indented under their parents.
 *
 * @param {Array<object>} tree the tree from GET /categories
 * @returns {Array<HTMLOptionElement>} the options
 */
export function categoryOptions(tree) {
  const options = [];
  const walk = (nodes, depth) => nodes.forEach((node) => {
    options.push(h('option', { value: node.id }, `${' '.repeat(depth)}${node.name}`));
    walk(node.children, depth + 1);
  });
  walk(tree, 0);
  return options;
}
