// One book: its details, adding it to the cart, its ratings and reviews (CAT-5, REV-1 – REV-5).

import { api, ApiError } from '../api.js';
import { $, fill, formValues, h, param } from '../dom.js';
import { formatDateTime, formatMoney, t } from '../i18n.js';
import { favoriteButton, loadFavoriteIds } from '../favorites.js';
import { busy, refreshCartBadge, showError, showMessage, startPage } from '../layout.js';

const bookId = Number(param('id'));
const caller = await startPage();
const favoriteIds = caller?.role === 'CUSTOMER' ? await loadFavoriteIds() : new Set();

if (!Number.isInteger(bookId) || bookId <= 0) {
  showMessage(t('book.notFound'), 'error');
} else {
  await showBook();
  await Promise.all([showRatings(), showReviews(0), showMyReview()]);
}

function stars(count) {
  return h('span', { class: 'stars', 'aria-label': t('reviews.starsLabel', { stars: count }) },
    '★'.repeat(count) + '☆'.repeat(5 - count));
}

async function showBook() {
  const box = $('#book');
  let book;
  try {
    book = await api.get(`/books/${bookId}`);
  } catch (error) {
    showError(error);
    return;
  }
  document.title = `${book.title} · ${t('site.name')}`;
  const quantity = h('input', { type: 'number', name: 'quantity', min: 1, max: Math.max(1, Math.min(10, book.stock)), value: 1,
    id: 'quantity' });
  const addForm = h('form', { class: 'actions' },
    h('label', { for: 'quantity', class: 'inline' }, t('book.quantity'), quantity),
    h('button', { type: 'submit', disabled: !book.inStock }, t('book.addToCart')));
  addForm.addEventListener('submit', (event) => {
    event.preventDefault();
    busy(addForm, async () => {
      const cart = await api.post('/cart/items', { bookId, quantity: Number(quantity.value) });
      await refreshCartBadge(cart);
      showMessage(t('cart.added', { title: book.title }), 'success');
    }, addForm);
  });
  const facts = [
    ['book.isbn', book.isbn],
    ['book.category', book.categoryPath.join(' › ')],
    ['book.publisher', book.publisher],
    ['book.year', book.publicationYear],
    ['book.language', t(`language.${book.language}`)],
    ['book.pages', book.pageCount > 0 ? book.pageCount : null],
  ].filter(([, value]) => value !== null && value !== undefined && value !== '');
  fill(box,
    book.coverImage
      ? h('img', { class: 'cover detail-cover', src: `/api/v1/books/${bookId}/cover`, alt: t('book.coverOf', { title: book.title }) })
      : null,
    h('h1', {}, book.title),
    h('p', { class: 'authors' }, book.authors.map((author) => author.name).join(', ')),
    h('p', { class: 'price' }, formatMoney(book.price)),
    h('p', { class: book.inStock ? 'stock' : 'stock out' },
      book.inStock ? t('book.stockLeft', { count: book.stock }) : t('book.outOfStock')),
    caller?.role === 'CUSTOMER' ? favoriteButton(bookId, book.title, favoriteIds) : null,
    addForm,
    h('dl', { class: 'facts' }, facts.map(([key, value]) => [h('dt', {}, t(key)), h('dd', {}, value)])),
    book.description ? h('p', {}, book.description) : null);
}

async function showRatings() {
  try {
    const breakdown = await api.get(`/books/${bookId}/ratings`);
    const total = breakdown.counts.reduce((sum, count) => sum + count, 0);
    const rows = [5, 4, 3, 2, 1].map((star) => {
      const count = breakdown.counts[star - 1];
      const width = total === 0 ? 0 : Math.round((count / total) * 100);
      return h('li', {}, h('span', {}, t('reviews.star', { stars: star })),
        h('span', { class: 'bar' }, h('span', { style: `width: ${width}%` })), h('span', {}, count));
    });
    fill($('#ratings'),
      h('li', { class: 'hint' }, total === 0 ? t('reviews.noRatings')
        : t('reviews.average', { average: Number(breakdown.average).toFixed(1), count: total })),
      rows);
  } catch (error) {
    showError(error);
  }
}

async function showReviews(page) {
  try {
    const result = await api.get(`/books/${bookId}/reviews?page=${page}&size=10`);
    fill($('#reviews'), result.content.length === 0
      ? h('li', { class: 'empty' }, t('reviews.none'))
      : result.content.map((review) => h('li', { class: 'panel' },
        h('p', { class: 'meta' }, stars(review.stars), ' ', review.authorName, ' · ', formatDateTime(review.createdAt),
          review.edited ? ` · ${t('reviews.edited')}` : ''),
        review.comment ? h('p', {}, review.comment) : null)));
    const pager = fill($('#review-pager'));
    if (result.totalPages > 1) {
      pager.append(
        h('button', { type: 'button', class: 'secondary', disabled: page === 0, onclick: () => showReviews(page - 1) },
          t('pager.previous')),
        h('span', {}, t('pager.position', { page: page + 1, pages: result.totalPages })),
        h('button', { type: 'button', class: 'secondary', disabled: page + 1 >= result.totalPages,
          onclick: () => showReviews(page + 1) }, t('pager.next')));
    }
  } catch (error) {
    showError(error);
  }
}

async function showMyReview() {
  const box = $('#my-review');
  if (!caller) {
    fill(box, h('p', { class: 'hint' }, h('a', { href: `login.html?next=${encodeURIComponent(`book.html?id=${bookId}`)}` },
      t('reviews.signInToWrite'))));
    return;
  }
  const mine = await api.get(`/books/${bookId}/reviews/mine`).catch((error) => {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  });
  if (mine) {
    const edit = h('button', { type: 'button', class: 'secondary', onclick: () => fill(box, reviewForm(mine)) }, t('reviews.edit'));
    const remove = h('button', { type: 'button', class: 'danger' }, t('reviews.delete'));
    remove.addEventListener('click', () => busy(remove, async () => {
      if (!confirm(t('reviews.confirmDelete'))) {
        return;
      }
      await api.delete(`/books/${bookId}/reviews/mine`);
      showMessage(t('reviews.deleted'), 'success');
      await refreshReviews();
    }));
    fill(box, h('div', { class: 'panel' }, h('h3', {}, t('reviews.yours')),
      h('p', {}, stars(mine.stars)), mine.comment ? h('p', {}, mine.comment) : null, h('div', { class: 'actions' }, edit, remove)));
    return;
  }
  const { canReview } = await api.get(`/books/${bookId}/reviews/eligibility`);
  fill(box, canReview ? reviewForm(null) : h('p', { class: 'hint' }, t('reviews.notEligible')));
}

function reviewForm(existing) {
  const form = h('form', { class: 'panel' },
    h('h3', {}, t(existing ? 'reviews.editHeading' : 'reviews.write')),
    h('div', { class: 'field' }, h('label', { for: 'stars' }, t('reviews.stars')),
      h('select', { id: 'stars', name: 'stars' }, [5, 4, 3, 2, 1].map((star) =>
        h('option', { value: star, selected: existing ? existing.stars === star : star === 5 }, t('reviews.star', { stars: star }))))),
    h('div', { class: 'field' }, h('label', { for: 'comment' }, t('reviews.comment')),
      h('textarea', { id: 'comment', name: 'comment', rows: 4, maxlength: 2000 }, existing?.comment ?? '')),
    h('div', { class: 'actions' }, h('button', { type: 'submit' }, t('reviews.send'))));
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    busy(form, async () => {
      const values = formValues(form);
      const body = { stars: Number(values.stars), comment: values.comment || null };
      if (existing) {
        await api.put(`/books/${bookId}/reviews/mine`, body);
      } else {
        await api.post(`/books/${bookId}/reviews`, body);
      }
      showMessage(t('reviews.saved'), 'success');
      await refreshReviews();
    }, form);
  });
  return form;
}

async function refreshReviews() {
  await Promise.all([showRatings(), showReviews(0), showMyReview()]);
}
