// Shared customer-favorite controls for the catalog, book details and favorites page.

import { api } from './api.js';
import { h } from './dom.js';
import { t } from './i18n.js';
import { busy, showMessage } from './layout.js';

const PAGE_SIZE = 100;

export async function loadFavoriteIds() {
  const ids = new Set();
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const result = await api.get(`/me/favorites?page=${page}&size=${PAGE_SIZE}`);
    result.content.forEach((id) => ids.add(Number(id)));
    totalPages = result.totalPages;
    page++;
  }
  return ids;
}

export function favoriteButton(bookId, title, favoriteIds, onChange = () => {}) {
  const button = h('button', { type: 'button', class: 'secondary small' });
  const update = () => {
    const active = favoriteIds.has(Number(bookId));
    button.textContent = t(active ? 'favorites.remove' : 'favorites.add');
    button.setAttribute('aria-pressed', String(active));
  };
  update();
  button.addEventListener('click', () => busy(button, async () => {
    const id = Number(bookId);
    if (favoriteIds.has(id)) {
      await api.delete(`/me/favorites/${id}`);
      favoriteIds.delete(id);
      showMessage(t('favorites.removedMessage', { title }), 'success');
    } else {
      await api.put(`/me/favorites/${id}`);
      favoriteIds.add(id);
      showMessage(t('favorites.addedMessage', { title }), 'success');
    }
    update();
    await onChange();
  }));
  return button;
}
