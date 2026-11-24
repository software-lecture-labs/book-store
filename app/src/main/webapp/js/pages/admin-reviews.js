// Every review, newest first (ADM-7): an unsuitable one is removed together with its rating.

import { api } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatDateTime, t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { pager, startAdminPage } from '../admin.js';

await startAdminPage();
await show(0);

async function show(page) {
  try {
    const result = await api.get(`/admin/reviews?page=${page}&size=20`);
    fill($('#reviews'), result.content.length === 0
      ? h('li', { class: 'empty' }, t('admin.noReviews'))
      : result.content.map((review) => item(review, page)));
    pager($('#pager'), result, show);
  } catch (error) {
    showError(error);
  }
}

function item(review, page) {
  const remove = h('button', { type: 'button', class: 'danger small' }, t('admin.removeReview'));
  remove.addEventListener('click', () => busy(remove, async () => {
    if (!confirm(t('admin.confirmRemoveReview', { name: review.authorName }))) {
      return;
    }
    await api.delete(`/admin/reviews/${review.id}`);
    showMessage(t('admin.reviewRemoved'), 'success');
    await show(page);
  }));
  return h('li', { class: 'panel' },
    h('p', { class: 'meta' },
      h('span', { class: 'stars', 'aria-label': t('reviews.starsLabel', { stars: review.stars }) },
        '★'.repeat(review.stars) + '☆'.repeat(5 - review.stars)),
      ' ', review.authorName, ' · ', formatDateTime(review.createdAt), ' · ',
      h('a', { href: `book.html?id=${review.bookId}` }, t('admin.bookNo', { id: review.bookId }))),
    review.comment ? h('p', {}, review.comment) : h('p', { class: 'hint' }, t('admin.noComment')),
    h('div', { class: 'actions' }, remove));
}
