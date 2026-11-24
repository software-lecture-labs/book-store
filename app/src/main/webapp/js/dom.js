// Small helpers for building the page from data with DOM nodes only, so text from the API is never
// read as HTML (ADR-0011).

/**
 * Creates an element.
 *
 * @param {string} tag the tag name
 * @param {object} [attrs] attributes; "class", "dataset", "on<event>" handlers and boolean values are understood
 * @param {...(Node|string|number|Array|null|false)} children the children; strings become text
 * @returns {HTMLElement} the element
 */
export function h(tag, attrs = {}, ...children) {
  const element = document.createElement(tag);
  for (const [name, value] of Object.entries(attrs ?? {})) {
    if (value == null || value === false) {
      continue;
    }
    if (name.startsWith('on') && typeof value === 'function') {
      element.addEventListener(name.slice(2), value);
    } else if (name === 'class') {
      element.className = value;
    } else if (name === 'dataset') {
      Object.assign(element.dataset, value);
    } else if (name === 'value') {
      element.value = value;
    } else if (name === 'checked' || name === 'selected' || name === 'disabled') {
      element[name] = Boolean(value);
    } else {
      element.setAttribute(name, value === true ? '' : String(value));
    }
  }
  for (const child of children.flat(Infinity)) {
    if (child != null && child !== false) {
      element.append(child instanceof Node ? child : String(child));
    }
  }
  return element;
}

/**
 * Finds an element.
 *
 * @param {string} selector a CSS selector
 * @param {ParentNode} [root] where to look
 * @returns {HTMLElement|null} the first match
 */
export function $(selector, root = document) {
  return root.querySelector(selector);
}

/**
 * Replaces an element's children.
 *
 * @param {HTMLElement} element the element
 * @param {...(Node|string|Array|null)} children the new children
 * @returns {HTMLElement} the element
 */
export function fill(element, ...children) {
  element.replaceChildren();
  for (const child of children.flat(Infinity)) {
    if (child != null && child !== false) {
      element.append(child instanceof Node ? child : String(child));
    }
  }
  return element;
}

/**
 * Reads a query parameter of the page's address.
 *
 * @param {string} name the parameter
 * @returns {string|null} its value
 */
export function param(name) {
  return new URLSearchParams(location.search).get(name);
}

/**
 * Reads a form's fields as an object of strings; checkboxes become booleans.
 *
 * @param {HTMLFormElement} form the form
 * @returns {object} the values by field name
 */
export function formValues(form) {
  const values = {};
  for (const field of form.elements) {
    if (!field.name) {
      continue;
    }
    if (field.type === 'checkbox') {
      values[field.name] = field.checked;
    } else if (field.type === 'radio') {
      if (field.checked) {
        values[field.name] = field.value;
      }
    } else {
      values[field.name] = field.value.trim();
    }
  }
  return values;
}
