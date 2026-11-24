// Every account (ADM-6): a switch stops an account from signing in or lets it in again. The
// server refuses it for administrators (ADR-0021).

import { api } from '../api.js';
import { $, fill, h } from '../dom.js';
import { formatDateTime, t } from '../i18n.js';
import { busy, showError, showMessage } from '../layout.js';
import { pager, startAdminPage } from '../admin.js';

await startAdminPage();
await show(0);

async function show(page) {
  try {
    const result = await api.get(`/admin/customers?page=${page}&size=20`);
    fill($('#customers'), result.content.map(row));
    pager($('#pager'), result, show);
  } catch (error) {
    showError(error);
  }
}

function row(customer) {
  const name = `${customer.firstName} ${customer.lastName}`;
  const active = h('input', {
    type: 'checkbox', checked: customer.active, disabled: customer.role === 'ADMIN',
    'aria-label': t('admin.canSignInOf', { name }),
  });
  active.addEventListener('change', () => busy(active, async () => {
    try {
      const changed = await api.put(`/admin/customers/${customer.id}/active`, { active: active.checked });
      showMessage(t(changed.active ? 'admin.accountOn' : 'admin.accountOff', { name }), 'success');
    } catch (error) {
      active.checked = !active.checked;
      throw error;
    }
  }));
  return h('tr', { class: customer.active ? null : 'muted' },
    h('td', {}, name),
    h('td', {}, customer.email),
    h('td', {}, t(`role.${customer.role}`)),
    h('td', {}, formatDateTime(customer.registeredAt)),
    h('td', {}, active));
}
