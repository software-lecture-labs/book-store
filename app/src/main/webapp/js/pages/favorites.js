// The signed-in customer's favorite books.

import { api, query } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatMoney, t } from '../i18n.js';
import { favoriteButton, loadFavoriteIds } from '../favorites.js';
import { showError, startPage } from '../layout.js';

const PAGE_SIZE = 12;
const caller = await startPage({ signInRequired: true });

if (caller.role !== 'CUSTOMER') {
  location.replace('admin.html');
} else {
  await showFavorites(0);
}

async function showFavorites(page) {
  const box = $('#favorites');
  try {
    const result = await api.get(`/me/favorites${query({ page, size: PAGE_SIZE })}`);
    if (result.totalElements > 0 && page >= result.totalPages) {
      await showFavorites(result.totalPages - 1);
      return;
    }
    if (result.totalElements === 0) {
      fill(box, h('h1', {}, t('favorites.heading')),
        h('p', { class: 'empty' }, t('favorites.none')),
        h('p', {}, h('a', { href: 'index.html' }, t('cart.browse'))));
      return;
    }
    const favoriteIds = new Set(result.content.map(Number));
    const books = await Promise.all(result.content.map(async (id) => {
      try {
        return await api.get(`/books/${id}`);
      } catch (error) {
        if (error.status === 404) {
          return { id, unavailable: true };
        }
        throw error;
      }
    }));
    fill(box, h('h1', {}, t('favorites.heading')),
      h('ul', { class: 'books' }, books.map((book) => favoriteCard(book, favoriteIds, page))),
      pager(result, page));
  } catch (error) {
    showError(error);
  }
}

function favoriteCard(book, favoriteIds, page) {
  if (book.unavailable) {
    return h('li', {}, h('article', { class: 'panel' },
      h('p', {}, t('favorites.unavailable')),
      favoriteButton(book.id, t('favorites.unavailable'), favoriteIds, () => showFavorites(page))));
  }
  const cover = book.coverImage
    ? h('img', { class: 'cover', src: `/api/v1/books/${book.id}/cover`, alt: '', loading: 'lazy' })
    : h('div', { class: 'cover placeholder', 'aria-hidden': 'true' }, book.title.charAt(0));
  return h('li', {}, h('article', { class: 'book-card panel' },
    h('a', { href: `book.html?id=${book.id}`, tabindex: -1 }, cover),
    h('h2', {}, h('a', { href: `book.html?id=${book.id}` }, book.title)),
    h('p', { class: 'authors' }, book.authors.map((author) => author.name).join(', ')),
    h('p', { class: 'price' }, formatMoney(book.price)),
    favoriteButton(book.id, book.title, favoriteIds, () => showFavorites(page))));
}

function pager(result, page) {
  if (result.totalPages <= 1) {
    return null;
  }
  return h('nav', { class: 'pager', 'aria-label': t('catalog.pages') },
    h('button', { type: 'button', class: 'secondary', disabled: page === 0,
      onclick: () => showFavorites(page - 1) }, t('pager.previous')),
    h('span', {}, t('pager.position', { page: page + 1, pages: result.totalPages })),
    h('button', { type: 'button', class: 'secondary', disabled: page + 1 >= result.totalPages,
      onclick: () => showFavorites(page + 1) }, t('pager.next')));
}
