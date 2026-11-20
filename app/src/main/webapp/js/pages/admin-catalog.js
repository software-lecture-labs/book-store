// Authors and categories (ADM-2): add, rename, and remove those no book uses.

import { api } from '../api.js';
import { $, fill, formValues, h } from '../dom.js';
import { t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { categoryOptions, startAdminPage } from '../admin.js';

await startAdminPage();
await Promise.all([showAuthors(), showCategories()]);

$('#new-author').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  busy(form, async () => {
    await api.post('/admin/authors', formValues(form));
    form.reset();
    await showAuthors();
  }, form);
});

$('#new-category').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  busy(form, async () => {
    const values = formValues(form);
    await api.post('/admin/categories', { name: values.name, parentId: values.parentId ? Number(values.parentId) : null });
    form.reset();
    await showCategories();
  }, form);
});

async function showAuthors() {
  try {
    const authors = await api.get('/admin/authors');
    fill($('#authors'), authors.length === 0
      ? h('li', { class: 'hint' }, t('admin.noAuthors'))
      : authors.map((author) => editableItem(author.name, t('admin.renameAuthor', { name: author.name }),
        async (name) => api.put(`/admin/authors/${author.id}`, { name }),
        async () => api.delete(`/admin/authors/${author.id}`),
        showAuthors)));
  } catch (error) {
    showError(error);
  }
}

async function showCategories() {
  try {
    const tree = await api.get('/categories');
    const parent = $('#parentId');
    fill(parent, h('option', { value: '' }, t('admin.topLevel')), categoryOptions(tree));
    const items = [];
    const walk = (nodes, depth) => nodes.forEach((node) => {
      const item = editableItem(node.name, t('admin.renameCategory', { name: node.name }),
        async (name) => api.put(`/admin/categories/${node.id}`, { name }),
        async () => api.delete(`/admin/categories/${node.id}`),
        showCategories);
      item.style.paddingLeft = `${depth * 1.5}rem`;
      items.push(item);
      walk(node.children, depth + 1);
    });
    walk(tree, 0);
    fill($('#categories'), items.length === 0 ? h('li', { class: 'hint' }, t('admin.noCategories')) : items);
  } catch (error) {
    showError(error);
  }
}

/** A name with rename and remove buttons; removing something still in use comes back as a problem. */
function editableItem(name, renameLabel, rename, remove, refresh) {
  const renameButton = h('button', { type: 'button', class: 'secondary small' }, t('admin.rename'));
  renameButton.addEventListener('click', () => busy(renameButton, async () => {
    const newName = prompt(renameLabel, name);
    if (newName && newName.trim() && newName.trim() !== name) {
      await rename(newName.trim());
      await refresh();
    }
  }));
  const removeButton = h('button', { type: 'button', class: 'danger small' }, t('account.remove'));
  removeButton.addEventListener('click', () => busy(removeButton, async () => {
    if (confirm(t('admin.confirmRemove', { name }))) {
      await remove();
      showMessage(t('admin.removed', { name }), 'success');
      await refresh();
    }
  }));
  return h('li', {}, h('span', {}, name), h('span', { class: 'actions' }, renameButton, removeButton));
}
