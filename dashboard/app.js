const backendInput = document.querySelector("#backend-url");
const dpcSelect = document.querySelector("#dpc-select");
const dpcSelection = document.querySelector("#dpc-selection");
const refreshButton = document.querySelector("#refresh-button");
const loadDpcsButton = document.querySelector("#load-dpcs-button");
const connectionState = document.querySelector("#connection-state");
const connectionLabel = document.querySelector("#connection-label");
const errorMessage = document.querySelector("#error-message");
const callsBody = document.querySelector("#calls-body");
const companiesBody = document.querySelector("#companies-body");
const statusFilter = document.querySelector("#status-filter");
const searchFilter = document.querySelector("#search-filter");

const state = {
  dpcs: [],
  calls: [],
  contacts: new Map(),
  companies: [],
};

backendInput.value = localStorage.getItem("dpc-dashboard-backend-url") || backendInput.value;

refreshButton.addEventListener("click", loadDashboard);
loadDpcsButton.addEventListener("click", loadDpcs);
backendInput.addEventListener("change", clearDpcSelection);
dpcSelect.addEventListener("change", () => {
  const dpc = state.dpcs.find((item) => String(item.id) === dpcSelect.value);
  dpcSelection.textContent = dpc
    ? `Selected DPC ID: ${dpc.id} — ${dpc.name}`
    : "Choose a DPC to see its ID and load its call records.";
  refreshButton.disabled = !dpc;
  if (dpc) {
    localStorage.setItem("dpc-dashboard-dpc-id", String(dpc.id));
  } else {
    localStorage.removeItem("dpc-dashboard-dpc-id");
  }
});
statusFilter.addEventListener("change", renderCalls);
searchFilter.addEventListener("input", renderCalls);

function setConnection(label, mode = "") {
  connectionLabel.textContent = label;
  connectionState.className = `connection ${mode}`.trim();
}

function setError(message) {
  errorMessage.textContent = message;
  errorMessage.hidden = !message;
}

function clearDpcSelection() {
  state.dpcs = [];
  dpcSelect.replaceChildren(new Option("Load the DPC list first", ""));
  dpcSelect.disabled = true;
  dpcSelection.textContent = "Load the DPC list, then select a DPC to see its ID.";
  refreshButton.disabled = true;
  localStorage.removeItem("dpc-dashboard-dpc-id");
}

function apiBaseUrl() {
  const rawUrl = backendInput.value.trim().replace(/\/+$/, "");
  const parsedUrl = new URL(rawUrl);
  if (!["http:", "https:"].includes(parsedUrl.protocol)) {
    throw new Error("Backend URL must use HTTP or HTTPS.");
  }
  return parsedUrl.toString().replace(/\/+$/, "");
}

async function getJson(baseUrl, path) {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    const details = await response.text();
    throw new Error(`Backend returned HTTP ${response.status}${details ? `: ${details}` : ""}`);
  }
  return response.json();
}

async function loadDashboard() {
  setError("");
  const dpcId = Number(dpcSelect.value);
  if (!Number.isSafeInteger(dpcId) || dpcId < 1) {
    setError("Load the DPC list and select a DPC first.");
    dpcSelect.focus();
    return;
  }

  const baseUrl = getBaseUrl();
  if (!baseUrl) return;

  refreshButton.disabled = true;
  refreshButton.textContent = "Loading…";
  setConnection("Loading dashboard…", "loading");

  try {
    const [health, calls, contacts, companies] = await Promise.all([
      getJson(baseUrl, "/api/health"),
      getJson(baseUrl, `/api/call-records?dpcId=${encodeURIComponent(dpcId)}`),
      getJson(baseUrl, "/api/contacts"),
      getJson(baseUrl, "/api/outreach-statuses"),
    ]);

    if (health.status !== "UP") {
      throw new Error("The backend health check did not report UP.");
    }
    if (![calls, contacts, companies].every(Array.isArray)) {
      throw new Error("The backend returned data in an unexpected format.");
    }

    state.calls = calls;
    state.contacts = new Map(contacts.map((contact) => [contact.id, contact]));
    state.companies = companies;
    renderDashboard();
    setConnection("Backend connected", "connected");
  } catch (error) {
    setConnection("Connection failed");
    setError(`${error.message || "Could not load dashboard data."} Check that the backend is running and try again.`);
  } finally {
    refreshButton.textContent = "Refresh dashboard";
    refreshButton.disabled = !dpcSelect.value;
  }
}

function getBaseUrl() {
  try {
    const baseUrl = apiBaseUrl();
    localStorage.setItem("dpc-dashboard-backend-url", baseUrl);
    return baseUrl;
  } catch (error) {
    setError(error.message || "Enter a valid backend URL.");
    backendInput.focus();
    return null;
  }
}

async function loadDpcs() {
  setError("");
  const baseUrl = getBaseUrl();
  if (!baseUrl) return;

  const savedDpcId = localStorage.getItem("dpc-dashboard-dpc-id");
  clearDpcSelection();
  try {
    loadDpcsButton.disabled = true;
    loadDpcsButton.textContent = "Loading…";
    setConnection("Loading DPC directory…", "loading");
    const [health, dpcs] = await Promise.all([
      getJson(baseUrl, "/api/health"),
      getJson(baseUrl, "/api/dpcs"),
    ]);
    if (health.status !== "UP") {
      throw new Error("The backend health check did not report UP.");
    }
    if (!Array.isArray(dpcs)) {
      throw new Error("The backend returned the DPC directory in an unexpected format.");
    }
    state.dpcs = dpcs;
    dpcSelect.replaceChildren(new Option(
      dpcs.length ? "Select a DPC…" : "No DPCs found",
      "",
    ));
    for (const dpc of dpcs) {
      dpcSelect.add(new Option(`${dpc.name} — ID: ${dpc.id}`, String(dpc.id)));
    }
    dpcSelect.disabled = dpcs.length === 0;
    if (savedDpcId && dpcs.some((dpc) => String(dpc.id) === savedDpcId)) {
      dpcSelect.value = savedDpcId;
      dpcSelect.dispatchEvent(new Event("change"));
    } else {
      dpcSelect.value = "";
      dpcSelection.textContent = dpcs.length
        ? `${dpcs.length} DPC${dpcs.length === 1 ? "" : "s"} available. Select one to see its ID.`
        : "No DPCs are registered in the backend.";
      refreshButton.disabled = true;
    }
    setConnection("Backend connected", "connected");
    setError(dpcs.length ? "" : "No DPCs are registered. Add DPC records before using the dashboard.");
  } catch (error) {
    setConnection("Connection failed");
    dpcSelect.replaceChildren(new Option("DPC list unavailable", ""));
    setError(`${error.message || "Could not load the DPC directory."} Check that the backend is running and try again.`);
  } finally {
    loadDpcsButton.disabled = false;
    loadDpcsButton.textContent = state.dpcs.length ? "Refresh DPC list" : "Load DPC list";
  }
}

function renderDashboard() {
  document.querySelector("#metric-total").textContent = String(state.calls.length);
  document.querySelector("#metric-matched").textContent =
    String(state.calls.filter((call) => call.contactId !== null).length);
  document.querySelector("#metric-long").textContent =
    String(state.calls.filter((call) => call.verificationStatus === "MATCHED_LONG_CALL").length);
  document.querySelector("#metric-companies").textContent = String(state.companies.length);

  const statuses = [...new Set(state.calls.map((call) => call.verificationStatus))].sort();
  statusFilter.replaceChildren(new Option("All statuses", ""));
  for (const status of statuses) {
    statusFilter.add(new Option(status.replaceAll("_", " "), status));
  }
  statusFilter.disabled = false;
  searchFilter.disabled = false;

  renderCalls();
  renderCompanies();
}

function renderCalls() {
  const selectedStatus = statusFilter.value;
  const query = searchFilter.value.trim().toLocaleLowerCase();
  const filteredCalls = state.calls.filter((call) => {
    const contact = state.contacts.get(call.contactId);
    const company = contact?.companyName || "";
    const searchable = `${call.phoneNumber} ${company} ${contact?.name || ""} ${call.id}`.toLocaleLowerCase();
    return (!selectedStatus || call.verificationStatus === selectedStatus) &&
      (!query || searchable.includes(query));
  }).slice(0, 100);

  callsBody.replaceChildren();
  if (filteredCalls.length === 0) {
    addEmptyRow(callsBody, "No call records match this DPC and filter.", 6);
    return;
  }

  for (const call of filteredCalls) {
    const contact = state.contacts.get(call.contactId);
    const contactCell = document.createElement("td");
    contactCell.append(
      makeCellText(contact?.name || "Unmatched number", "primary-cell"),
      makeCellText(contact?.companyName || "No contact match", "secondary-cell"),
    );

    const row = document.createElement("tr");
    row.append(
      makeCell(formatDate(call.callTimestamp)),
      contactCell,
      makeCell(call.phoneNumber),
      makeCell(formatDuration(call.durationSeconds)),
      makeBadgeCell(call.verificationStatus),
      makeCell(`#${call.id}`),
    );
    callsBody.append(row);
  }
}

function renderCompanies() {
  companiesBody.replaceChildren();
  if (state.companies.length === 0) {
    addEmptyRow(companiesBody, "No company outreach statuses have been recorded yet.", 4);
    return;
  }

  for (const company of state.companies) {
    const row = document.createElement("tr");
    row.append(
      makeCell(company.companyName),
      makeCell(company.latestCallTimestamp ? formatDate(company.latestCallTimestamp) : "—"),
      makeBadgeCell(company.status),
      makeCell(formatDate(company.updatedAt)),
    );
    companiesBody.append(row);
  }
}

function makeCell(value) {
  const cell = document.createElement("td");
  cell.textContent = value ?? "—";
  return cell;
}

function makeCellText(value, className) {
  const element = document.createElement("span");
  element.className = className;
  element.textContent = value;
  return element;
}

function makeBadgeCell(value) {
  const cell = document.createElement("td");
  const badge = document.createElement("span");
  badge.className = `badge ${String(value).toLowerCase().replaceAll("_", "-")}`;
  badge.textContent = String(value).replaceAll("_", " ");
  cell.append(badge);
  return cell;
}

function addEmptyRow(tableBody, message, columnCount) {
  const row = document.createElement("tr");
  const cell = makeCell(message);
  cell.colSpan = columnCount;
  cell.className = "empty-state";
  row.append(cell);
  tableBody.append(row);
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatDuration(seconds) {
  if (!Number.isFinite(seconds)) return "—";
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return minutes ? `${minutes}m ${remainder}s` : `${remainder}s`;
}
