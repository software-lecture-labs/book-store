// Registering (ACC-1, ACC-2), then signing in at once with the same password.

import { api } from '../api.js';
import { $, formValues } from '../dom.js';
import { busy, startPage } from '../layout.js';

const caller = await startPage();
const form = $('#register');

if (caller) {
  location.replace('account.html');
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(form, async () => {
    const values = formValues(form);
    await api.post('/customers', values);
    try {
      await api.post('/session', { email: values.email, password: values.password });
      location.href = 'account.html';
    } catch {
      location.href = 'login.html?registered=1';
    }
  }, form);
});
