// The catalog: search, filters, sorting and pages (CAT-1 – CAT-4). The search is kept in the
// page's address, so it can be shared and the back button works.

import { api, query } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatMoney, t } from '../i18n.js';
import { favoriteButton, loadFavoriteIds } from '../favorites.js';
import { busy, refreshCartBadge, showError, showMessage, startPage } from '../layout.js';

const PAGE_SIZE = 12;
const form = $('#search');

const caller = await startPage();
const favoriteIds = caller?.role === 'CUSTOMER' ? await loadFavoriteIds() : new Set();
await loadCategories();
readSearchFromAddress();
await search();

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  await search(0);
});

async function loadCategories() {
  const select = form.elements.namedItem('category');
  try {
    const tree = await api.get('/categories');
    const options = [];
    const walk = (nodes, depth) => nodes.forEach((node) => {
      options.push(h('option', { value: node.id }, `${' '.repeat(depth)}${node.name}`));
      walk(node.children, depth + 1);
    });
    walk(tree, 0);
    select.append(...options);
  } catch (error) {
    showError(error);
  }
}

function readSearchFromAddress() {
  const parameters = new URLSearchParams(location.search);
  form.elements.namedItem('q').value = parameters.get('q') ?? '';
  form.elements.namedItem('category').value = parameters.get('category') ?? '';
  form.elements.namedItem('language').value = parameters.get('language') ?? '';
  form.elements.namedItem('inStock').checked = parameters.get('inStock') === 'true';
  const sort = `${parameters.get('sort') ?? 'TITLE'}:${parameters.get('order') ?? 'asc'}`;
  if ([...form.elements.namedItem('sort').options].some((option) => option.value === sort)) {
    form.elements.namedItem('sort').value = sort;
  }
}

function currentSearch(page) {
  const [sort, order] = form.elements.namedItem('sort').value.split(':');
  return {
    q: form.elements.namedItem('q').value.trim(),
    category: form.elements.namedItem('category').value,
    language: form.elements.namedItem('language').value,
    inStock: form.elements.namedItem('inStock').checked,
    sort,
    order,
    page: page ?? Number(new URLSearchParams(location.search).get('page') ?? 0),
  };
}

async function search(page) {
  const criteria = currentSearch(page);
  history.replaceState(null, '', `index.html${query({ ...criteria, page: criteria.page || null })}`);
  try {
    const result = await api.get(`/books${query({ ...criteria, size: PAGE_SIZE })}`);
    showBooks(result);
  } catch (error) {
    showError(error, form);
  }
}

function showBooks(result) {
  fill($('#summary'), result.totalElements === 0 ? t('catalog.none') : t('catalog.found', { count: result.totalElements }));
  fill($('#books'), result.content.map(bookCard));
  const pager = $('#pager');
  fill(pager);
  if (result.totalPages > 1) {
    pager.append(
      h('button', { type: 'button', class: 'secondary', disabled: result.number === 0, onclick: () => search(result.number - 1) },
        t('pager.previous')),
      h('span', {}, t('pager.position', { page: result.number + 1, pages: result.totalPages })),
      h('button', { type: 'button', class: 'secondary', disabled: result.number + 1 >= result.totalPages,
        onclick: () => search(result.number + 1) }, t('pager.next')));
  }
}

function bookCard(book) {
  const addButton = h('button', { type: 'button', disabled: !book.inStock }, t('book.addToCart'));
  addButton.addEventListener('click', () => busy(addButton, async () => {
    const cart = await api.post('/cart/items', { bookId: book.id, quantity: 1 });
    await refreshCartBadge(cart);
    showMessage(t('cart.added', { title: book.title }), 'success');
  }));
  const cover = book.coverImage
    ? h('img', { class: 'cover', src: `/api/v1/books/${book.id}/cover`, alt: '', loading: 'lazy' })
    : h('div', { class: 'cover placeholder', 'aria-hidden': 'true' }, book.title.charAt(0));
  return h('li', {}, h('article', { class: 'book-card panel' },
    h('a', { href: `book.html?id=${book.id}`, tabindex: -1 }, cover),
    h('h3', {}, h('a', { href: `book.html?id=${book.id}` }, book.title)),
    h('p', { class: 'authors' }, book.authorNames),
    book.ratingCount > 0 && book.averageRating != null
      ? h('p', { class: 'hint' }, h('span', { class: 'stars' }, '★'), ` ${Number(book.averageRating).toFixed(1)} (${book.ratingCount})`)
      : null,
    h('p', { class: book.inStock ? 'stock' : 'stock out' }, t(book.inStock ? 'book.inStock' : 'book.outOfStock')),
    caller?.role === 'CUSTOMER'
      ? h('div', { class: 'actions' }, favoriteButton(book.id, book.title, favoriteIds))
      : null,
    h('div', { class: 'bottom' }, h('span', { class: 'price' }, formatMoney(book.price)), addButton)));
}
