// The signed-in customer's account (ACC-4, ACC-5): profile, password and saved addresses.

import { api } from '../api.js';
import { $, fill, formValues, h } from '../dom.js';
import { t } from '../i18n.js';
import { busy, clearMessages, showMessage, startPage } from '../layout.js';

const profile = await startPage({ signInRequired: true });
const profileForm = $('#profile');
const passwordForm = $('#password');
const addressForm = $('#new-address');

showProfile(profile);
showAddresses(profile.addresses);
if (location.hash === '#new-address') {
  addressForm.elements.namedItem('label').focus();
}

profileForm.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(profileForm, async () => {
    const updated = await api.put('/me', formValues(profileForm));
    showProfile(updated);
    showMessage(t('account.saved'), 'success');
  }, profileForm);
});

passwordForm.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(passwordForm, async () => {
    await api.put('/me/password', formValues(passwordForm));
    passwordForm.reset();
    showMessage(t('account.passwordChanged'), 'success');
  }, passwordForm);
});

addressForm.addEventListener('submit', (event) => {
  event.preventDefault();
  busy(addressForm, async () => {
    await api.post('/me/addresses', formValues(addressForm));
    addressForm.reset();
    clearMessages();
    showAddresses((await api.get('/me')).addresses);
    showMessage(t('account.addressAdded'), 'success');
  }, addressForm);
});

function showProfile(customer) {
  for (const name of ['firstName', 'lastName', 'email']) {
    profileForm.elements.namedItem(name).value = customer[name];
  }
}

function showAddresses(addresses) {
  const list = $('#address-list');
  if (addresses.length === 0) {
    fill(list, h('li', { class: 'hint' }, t('account.noAddresses')));
    return;
  }
  fill(list, addresses.map((saved) => {
    const makeDefault = h('button', { type: 'button', class: 'secondary' }, t('account.makeDefaultButton'));
    makeDefault.addEventListener('click', () => busy(makeDefault, async () => {
      showAddresses((await api.post(`/me/addresses/${saved.id}/default`)).addresses);
    }));
    const remove = h('button', { type: 'button', class: 'danger' }, t('account.remove'));
    remove.addEventListener('click', () => busy(remove, async () => {
      if (!confirm(t('account.confirmRemove', { label: saved.label }))) {
        return;
      }
      await api.delete(`/me/addresses/${saved.id}`);
      showAddresses((await api.get('/me')).addresses);
    }));
    const address = saved.address;
    return h('li', { class: 'choice' }, h('div', {},
      h('strong', {}, saved.label), saved.isDefault ? h('span', { class: 'status' }, t('account.default')) : null,
      h('address', {}, address.recipient, h('br'), address.line, h('br'),
        `${address.district} / ${address.city} ${address.postalCode}`, h('br'), address.phone),
      h('div', { class: 'actions' }, saved.isDefault ? null : makeDefault, remove)));
  }));
}
