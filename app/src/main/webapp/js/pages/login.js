// Signing in (ACC-3, ADR-0020): the password is sent once; the session cookie does the rest.
// A visitor's cart joins the stored cart on the server.

import { api } from '../api.js';
import { $, formValues, param } from '../dom.js';
import { t } from '../i18n.js';
import { busy, showMessage, startPage } from '../layout.js';

const caller = await startPage();
const form = $('#login');

/** Only pages of this site are followed after signing in. */
function nextPage() {
  const next = param('next');
  return next && /^[a-z][a-z-]*\.html(\?[^\s]*)?$/.test(next) ? next : 'index.html';
}

if (caller) {
  location.replace(nextPage());
}
if (param('registered')) {
  showMessage(t('login.registered'), 'success');
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(form, async () => {
    await api.post('/session', formValues(form));
    location.href = nextPage();
  }, form);
});
