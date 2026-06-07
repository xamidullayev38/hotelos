// HotelOS Operations Dashboard — vanilla JS client
// Connects to the local services for snapshots and to /live for jonli yangilanish.

const SERVICES = {
  reception:   'http://localhost:4001',
  housekeeping:'http://localhost:4002',
  roomservice: 'http://localhost:4003',
  maintenance: 'http://localhost:4004',
};

const state = {
  rooms: new Map(),   // roomNumber -> room
  orders: new Map(),  // orderId -> order
  issues: new Map(),  // issueId -> issue
  token: null,
};

const $ = (id) => document.getElementById(id);

document.addEventListener('DOMContentLoaded', () => {
  $('signin').addEventListener('click', signIn);
  $('token').addEventListener('keydown', (e) => { if (e.key === 'Enter') signIn(); });
});

function signIn() {
  const token = $('token').value.trim();
  if (!token) {
    $('auth-error').textContent = 'Token is required.';
    return;
  }
  state.token = token;
  $('auth-gate').classList.add('hidden');
  $('board').classList.remove('hidden');
  bootstrap();
}

async function bootstrap() {
  await Promise.all([loadRooms(), loadOrders(), loadIssues()]);
  connectLive();
}

async function loadRooms() {
  try {
    const res = await fetch(`${SERVICES.reception}/rooms`);
    const rooms = await res.json();
    rooms.forEach(r => state.rooms.set(r.number, r));
    renderRooms();
  } catch (e) { log('error', `rooms fetch failed: ${e.message}`); }
}

async function loadOrders() {
  try {
    const res = await fetch(`${SERVICES.roomservice}/orders`);
    const orders = await res.json();
    orders.forEach(o => state.orders.set(o.id, o));
    renderOrders();
  } catch (e) { log('error', `orders fetch failed: ${e.message}`); }
}

async function loadIssues() {
  try {
    const res = await fetch(`${SERVICES.maintenance}/queue`);
    const issues = await res.json();
    issues.forEach(i => state.issues.set(i.id, i));
    renderIssues();
  } catch (e) { log('error', `issues fetch failed: ${e.message}`); }
}

function connectLive() {
  const ws = new WebSocket(`ws://${location.host}/live`);
  ws.onopen = () => {
    $('conn-status').textContent = 'live';
    $('conn-status').classList.remove('offline');
    $('conn-status').classList.add('online');
  };
  ws.onclose = () => {
    $('conn-status').textContent = 'offline';
    $('conn-status').classList.remove('online');
    $('conn-status').classList.add('offline');
    setTimeout(connectLive, 2000);
  };
  ws.onmessage = (evt) => {
    try {
      const msg = JSON.parse(evt.data);
      handleEvent(msg.topic, msg.payload || {});
    } catch (e) { console.error('bad message', e); }
  };
}

function handleEvent(topic, payload) {
  log(topic, JSON.stringify(payload));
  switch (topic) {
    case 'room.status_changed': {
      const r = state.rooms.get(payload.roomNumber);
      if (r) { r.status = payload.status; renderRooms(); }
      break;
    }
    case 'room.vacated': {
      const r = state.rooms.get(payload.roomNumber);
      if (r) { r.status = 'DIRTY'; renderRooms(); }
      break;
    }
    case 'order.created': {
      state.orders.set(payload.orderId, {
        id: payload.orderId,
        roomNumber: payload.roomNumber,
        status: payload.status || 'RECEIVED',
        total: payload.total,
      });
      renderOrders();
      break;
    }
    case 'order.status_changed': {
      const o = state.orders.get(payload.orderId);
      if (o) { o.status = payload.status; renderOrders(); }
      break;
    }
    case 'maintenance.reported': {
      state.issues.set(payload.issueId, {
        id: payload.issueId,
        roomNumber: payload.roomNumber,
        urgency: payload.urgency,
        assignedTo: payload.assignedTo,
        description: payload.description,
      });
      renderIssues();
      break;
    }
    case 'maintenance.resolved': {
      state.issues.delete(payload.issueId);
      renderIssues();
      break;
    }
  }
}

/**
 * HTML-escape any value that came from the server. The maintenance description
 * is free-form and is shown verbatim on the dashboard — without escaping a
 * malicious report could inject <script> into another operator's browser.
 * Addresses Task 3.2 (data exposure / XSS).
 */
function esc(v) {
  if (v === null || v === undefined) return '';
  return String(v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const KNOWN_STATUSES = ['CLEAN', 'DIRTY', 'CLEANING', 'MAINTENANCE', 'OCCUPIED'];
const KNOWN_URGENCY  = ['CRITICAL', 'HIGH', 'NORMAL', 'LOW'];

function renderRooms() {
  const grid = $('rooms');
  const rooms = [...state.rooms.values()].sort((a, b) => String(a.number).localeCompare(String(b.number)));
  grid.innerHTML = rooms.map(r => {
    const statusClass = KNOWN_STATUSES.includes(r.status) ? r.status : 'CLEAN';
    return `
      <div class="room ${statusClass}">
        <div class="num">${esc(r.number)}</div>
        <div class="type">${esc(r.type)}</div>
        <div class="stat">${esc(r.status)}</div>
      </div>`;
  }).join('');
  $('rooms-count').textContent = `${rooms.length} total`;
}

function renderOrders() {
  const tbody = $('orders').querySelector('tbody');
  const orders = [...state.orders.values()]
    .filter(o => o.status !== 'DELIVERED')
    .sort((a, b) => a.id - b.id);
  tbody.innerHTML = orders.map(o => `
    <tr><td>${esc(o.id)}</td><td>${esc(o.roomNumber)}</td><td>${esc(o.status)}</td><td>${esc(o.total ?? '')}</td></tr>
  `).join('');
  $('orders-count').textContent = `${orders.length} active`;
}

function renderIssues() {
  const tbody = $('issues').querySelector('tbody');
  const issues = [...state.issues.values()]
    .sort((a, b) => {
      const order = { CRITICAL: 0, HIGH: 1, NORMAL: 2, LOW: 3 };
      return (order[a.urgency] ?? 99) - (order[b.urgency] ?? 99);
    });
  tbody.innerHTML = issues.map(i => {
    const urgClass = KNOWN_URGENCY.includes(i.urgency) ? i.urgency : 'NORMAL';
    return `
      <tr>
        <td>${esc(i.id)}</td>
        <td>${esc(i.roomNumber)}</td>
        <td class="urg-${urgClass}">${esc(i.urgency)}</td>
        <td>${esc(i.assignedTo ?? '')}</td>
        <td>${esc(i.description ?? '')}</td>
      </tr>`;
  }).join('');
  $('issues-count').textContent = `${issues.length} open`;
}

function log(topic, message) {
  const ul = $('log');
  const li = document.createElement('li');
  const t = new Date().toLocaleTimeString();
  // Use DOM APIs instead of innerHTML so untrusted message text cannot inject
  const timeSpan = document.createElement('span');
  timeSpan.className = 't';
  timeSpan.textContent = t;
  const topicSpan = document.createElement('span');
  topicSpan.className = 'topic';
  topicSpan.textContent = topic;
  const text = document.createTextNode(message);
  li.appendChild(timeSpan);
  li.appendChild(topicSpan);
  li.appendChild(text);
  ul.prepend(li);
  while (ul.childNodes.length > 60) ul.removeChild(ul.lastChild);
}
