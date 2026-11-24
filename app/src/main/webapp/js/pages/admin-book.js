// Adding or changing one book (ADM-1, ADM-3) and its cover image (ADM-4). Without ?id= the page
// adds a book; the ISBN can only be entered then.

import { api, ifMatch, upload } from '../api.js';
import { $, fill, formValues, h, param } from '../dom.js';
import { t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { categoryOptions, startAdminPage } from '../admin.js';

await startAdminPage();
const form = $('#book');
const bookId = param('id');
/** The book as last read; its version goes into If-Match, so nobody overwrites a newer save (ADR-0022). */
let current = null;

try {
  const [authors, categories] = await Promise.all([api.get('/admin/authors'), api.get('/categories')]);
  fill(form.elements.namedItem('authorIds'), authors.map((author) => h('option', { value: author.id }, author.name)));
  fill(form.elements.namedItem('categoryId'), categoryOptions(categories));
  if (bookId) {
    showBook(await api.get(`/admin/books/${encodeURIComponent(bookId)}`));
  } else {
    $('#heading').textContent = t('admin.newBook');
  }
} catch (error) {
  showError(error);
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(form, async () => {
    const values = formValues(form);
    const body = {
      isbn: values.isbn,
      title: values.title,
      authorIds: [...form.elements.namedItem('authorIds').selectedOptions].map((option) => Number(option.value)),
      categoryId: Number(values.categoryId),
      publisher: values.publisher || null,
      publicationYear: values.publicationYear ? Number(values.publicationYear) : null,
      language: values.language,
      pageCount: Number(values.pageCount || 0),
      description: values.description || null,
      price: Number(values.price),
      stock: Number(values.stock),
    };
    if (bookId) {
      showBook(await api.put(`/admin/books/${bookId}`, body, ifMatch(current)));
      showMessage(t('admin.saved', { title: body.title }), 'success');
    } else {
      const created = await api.post('/admin/books', body);
      location.replace(`admin-book.html?id=${created.id}&created=1`);
    }
  }, form);
});

$('#cover-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const coverForm = event.currentTarget;
  busy(coverForm, async () => {
    const file = coverForm.elements.namedItem('file').files[0];
    showBook(await upload(`/admin/books/${bookId}/cover`, file));
    coverForm.reset();
    showMessage(t('admin.coverSaved'), 'success');
  }, coverForm);
});

$('#active').addEventListener('change', (event) => {
  const box = event.currentTarget;
  busy(box, async () => {
    const changed = await api.put(`/admin/books/${bookId}/active`, { active: box.checked }, ifMatch(current));
    current = changed;
    showMessage(t(changed.active ? 'admin.backOnSale' : 'admin.offSale', { title: changed.title }), 'success');
  });
});

function showBook(book) {
  current = book;
  if (param('created')) {
    showMessage(t('admin.created', { title: book.title }), 'success');
  }
  $('#heading').textContent = book.title;
  document.title = `${book.title} · ${t('admin.heading')}`;
  const fields = form.elements;
  fields.namedItem('isbn').value = book.isbn;
  fields.namedItem('isbn').readOnly = true;
  fields.namedItem('title').value = book.title;
  const authorIds = new Set(book.authors.map((author) => String(author.id)));
  [...fields.namedItem('authorIds').options].forEach((option) => { option.selected = authorIds.has(option.value); });
  fields.namedItem('categoryId').value = String(book.categoryId);
  fields.namedItem('language').value = book.language;
  fields.namedItem('publisher').value = book.publisher ?? '';
  fields.namedItem('publicationYear').value = book.publicationYear ?? '';
  fields.namedItem('pageCount').value = book.pageCount;
  fields.namedItem('price').value = book.price.amount;
  fields.namedItem('stock').value = book.stock;
  fields.namedItem('description').value = book.description ?? '';
  $('#cover-panel').hidden = false;
  $('#active').checked = book.active;
  fill($('#cover'), book.coverImage
    ? h('img', { class: 'cover admin-cover', src: `/api/v1/books/${book.id}/cover?v=${encodeURIComponent(book.coverImage)}`,
      alt: t('book.coverOf', { title: book.title }) })
    : h('p', { class: 'hint' }, t('admin.noCover')));
}
