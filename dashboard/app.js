const backendInput = document.querySelector("#backend-url");
const connectForm = document.querySelector("#connect-form");
const connectButton = document.querySelector("#connect-button");
const modePanel = document.querySelector("#mode-panel");
const normalModeButton = document.querySelector("#normal-mode-button");
const adminModeButton = document.querySelector("#admin-mode-button");
const simulationModeButton = document.querySelector("#simulation-mode-button");
const adminSigninForm = document.querySelector("#admin-signin-form");
const adminUsername = document.querySelector("#admin-username");
const adminPassword = document.querySelector("#admin-password");
const adminSigninButton = document.querySelector("#admin-signin-button");
const adminSigninMessage = document.querySelector("#admin-signin-message");
const simulationSigninForm = document.querySelector("#simulation-signin-form");
const simulationUsername = document.querySelector("#simulation-username");
const simulationPassword = document.querySelector("#simulation-password");
const simulationSigninButton = document.querySelector("#simulation-signin-button");
const simulationSigninMessage = document.querySelector("#simulation-signin-message");
const connectionPanel = document.querySelector("#connection-panel");
const normalDashboard = document.querySelector("#normal-dashboard");
const adminDashboard = document.querySelector("#admin-dashboard");
const simulationDashboard = document.querySelector("#simulation-dashboard");
const simulationEntryView = document.querySelector("#simulation-entry-view");
const simulationHistoryView = document.querySelector("#simulation-call-history");
const simulationDpcSelect = document.querySelector("#simulation-dpc");
const simulationLogDpcSelect = document.querySelector("#simulation-log-dpc");
const simulationForm = document.querySelector("#simulation-form");
const simulationContactsBody = document.querySelector("#simulation-contacts-body");
const simulationContactCount = document.querySelector("#simulation-contact-count");
const simulationCallsBody = document.querySelector("#simulation-calls-body");
const simulationHistoryError = document.querySelector("#simulation-history-error");
const simulationPhoneInput = document.querySelector("#simulation-phone");
const simulationTimestampInput = document.querySelector("#simulation-timestamp");
const simulationDurationInput = document.querySelector("#simulation-duration");
const simulationSubmitButton = document.querySelector("#simulation-submit-button");
const simulationMessage = document.querySelector("#simulation-message");
const pageTitle = document.querySelector("#page-title");
const pageSubtitle = document.querySelector("#page-subtitle");
const dpcSelect = document.querySelector("#dpc-select");
const dpcSelection = document.querySelector("#dpc-selection");
const refreshButton = document.querySelector("#refresh-button");
const loadDpcsButton = document.querySelector("#load-dpcs-button");
const connectionState = document.querySelector("#connection-state");
const connectionLabel = document.querySelector("#connection-label");
const errorMessage = document.querySelector("#error-message");
const normalErrorMessage = document.querySelector("#normal-error");
const callsBody = document.querySelector("#calls-body");
const companiesBody = document.querySelector("#companies-body");
const adminCompaniesBody = document.querySelector("#admin-companies-body");
const adminContactsBody = document.querySelector("#admin-contacts-body");
const adminStatusesBody = document.querySelector("#admin-statuses-body");
const adminDpcsBody = document.querySelector("#admin-dpcs-body");
const adminDashboardMessage = document.querySelector("#admin-dashboard-message");
const dpcForm = document.querySelector("#dpc-form");
const dpcNameInput = document.querySelector("#dpc-name");
const dpcSaveButton = document.querySelector("#dpc-save-button");
const companyForm = document.querySelector("#company-form");
const companyIdInput = document.querySelector("#company-id");
const companyNameInput = document.querySelector("#company-name");
const companySaveButton = document.querySelector("#company-save-button");
const companyCancelButton = document.querySelector("#company-cancel-button");
const contactForm = document.querySelector("#contact-form");
const contactIdInput = document.querySelector("#contact-id");
const contactNameInput = document.querySelector("#contact-name");
const contactPhoneInput = document.querySelector("#contact-phone");
const contactCompanySelect = document.querySelector("#contact-company");
const contactSaveButton = document.querySelector("#contact-save-button");
const contactCancelButton = document.querySelector("#contact-cancel-button");
const deleteConfirmDialog = document.querySelector("#delete-confirm-dialog");
const deleteConfirmForm = document.querySelector("#delete-confirm-form");
const deleteConfirmDescription = document.querySelector("#delete-confirm-description");
const deleteConfirmUsernameField = document.querySelector("#delete-confirm-username-field");
const deleteConfirmUsername = document.querySelector("#delete-confirm-username");
const deleteConfirmPasswordLabel = document.querySelector("#delete-confirm-password-label");
const deleteConfirmPassword = document.querySelector("#delete-confirm-password");
const deleteConfirmError = document.querySelector("#delete-confirm-error");
const deleteConfirmButton = document.querySelector("#delete-confirm-button");
const statusFilter = document.querySelector("#status-filter");
const searchFilter = document.querySelector("#search-filter");
const callMatchDialog = document.querySelector("#call-match-dialog");
const callMatchForm = document.querySelector("#call-match-form");
const callMatchDescription = document.querySelector("#call-match-description");
const callMatchName = document.querySelector("#call-match-name");
const callMatchCompany = document.querySelector("#call-match-company");
const callMatchError = document.querySelector("#call-match-error");
const callMatchSubmit = document.querySelector("#call-match-submit");

const state = {
  dpcs: [],
  calls: [],
  contacts: new Map(),
  companies: [],
  adminCredentials: null,
  simulationCredentials: null,
  simulationDpcs: [],
  simulationContacts: [],
  simulationCalls: [],
  adminCompanies: [],
  adminContacts: [],
  adminDpcs: [],
  adminOutreachStatuses: [],
  pendingDelete: null,
  pendingCallMatch: null,
  callMatchSubmitting: false,
};

const servedByBackend = location.port === "8080" &&
  ["localhost", "127.0.0.1", "[::1]"].includes(location.hostname);
backendInput.value = servedByBackend
  ? location.origin
  : localStorage.getItem("dpc-dashboard-backend-url") || backendInput.value;

connectForm.addEventListener("submit", connectToBackend);
normalModeButton.addEventListener("click", openNormalDashboard);
adminModeButton.addEventListener("click", () => {
  simulationSigninForm.hidden = true;
  adminSigninForm.hidden = false;
  adminUsername.focus();
});
simulationModeButton.addEventListener("click", () => {
  adminSigninForm.hidden = true;
  simulationSigninForm.hidden = false;
  simulationUsername.focus();
});
adminSigninForm.addEventListener("submit", loginAdmin);
simulationSigninForm.addEventListener("submit", loginSimulation);
simulationForm.addEventListener("submit", addSimulatedCall);
document.querySelector("#simulation-refresh-button").addEventListener("click", loadSimulationDirectory);
document.querySelector("#simulation-history-button").addEventListener("click", openSimulationCallHistory);
document.querySelector("#simulation-history-refresh").addEventListener("click", loadSimulationCalls);
document.querySelector("#simulation-history-back").addEventListener("click", closeSimulationCallHistory);
simulationLogDpcSelect.addEventListener("change", loadSimulationCalls);
simulationCallsBody.addEventListener("click", handleSimulationCallAction);
document.querySelector("#simulation-back-button").addEventListener("click", returnToModeChoice);
document.querySelector("#change-backend-button").addEventListener("click", () => {
  state.adminCredentials = null;
  state.simulationCredentials = null;
  simulationDashboard.hidden = true;
  simulationHistoryView.hidden = true;
  simulationEntryView.hidden = false;
  adminUsername.value = "";
  adminPassword.value = "";
  simulationUsername.value = "";
  simulationPassword.value = "";
  adminSigninForm.hidden = true;
  simulationSigninForm.hidden = true;
  modePanel.hidden = true;
  connectionPanel.hidden = false;
  pageSubtitle.textContent = "Connect to the backend to continue.";
  setConnection("Not connected");
  backendInput.focus();
});
refreshButton.addEventListener("click", loadDashboard);
loadDpcsButton.addEventListener("click", loadDpcs);
backendInput.addEventListener("change", () => {
  clearDpcSelection();
});
document.querySelector("#normal-back-button").addEventListener("click", returnToModeChoice);
document.querySelector("#admin-back-button").addEventListener("click", () => {
  state.adminCredentials = null;
  returnToModeChoice();
});
document.querySelector("#admin-logout-button").addEventListener("click", () => {
  state.adminCredentials = null;
  returnToModeChoice();
});
document.querySelector("#admin-refresh-button").addEventListener("click", loadAdminData);
dpcForm.addEventListener("submit", saveDpc);
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
callsBody.addEventListener("click", handleCallAction);
callMatchForm.addEventListener("submit", matchCall);
document.querySelector("#call-match-cancel").addEventListener("click", closeCallMatchDialog);
callMatchDialog.addEventListener("cancel", (event) => {
  if (state.callMatchSubmitting) event.preventDefault();
});
callMatchDialog.addEventListener("close", () => {
  if (!state.callMatchSubmitting) {
    state.pendingCallMatch = null;
    callMatchForm.reset();
  }
});
companyForm.addEventListener("submit", saveCompany);
companyCancelButton.addEventListener("click", resetCompanyForm);
contactForm.addEventListener("submit", saveContact);
contactCancelButton.addEventListener("click", resetContactForm);
deleteConfirmForm.addEventListener("submit", confirmDelete);
document.querySelector("#delete-cancel-button").addEventListener("click", closeDeleteConfirmation);
deleteConfirmDialog.addEventListener("cancel", (event) => {
  if (deleteConfirmButton.disabled) event.preventDefault();
});
deleteConfirmDialog.addEventListener("close", () => {
  if (!deleteConfirmButton.disabled) {
    state.pendingDelete = null;
    deleteConfirmUsername.value = "";
    deleteConfirmPassword.value = "";
  }
});
adminCompaniesBody.addEventListener("click", handleCompanyAction);
adminStatusesBody.addEventListener("change", updateCoreOutreachStatus);
adminContactsBody.addEventListener("click", handleContactAction);

function setConnection(label, mode = "") {
  connectionLabel.textContent = label;
  connectionState.className = `connection ${mode}`.trim();
}

function setError(message) {
  errorMessage.textContent = message;
  errorMessage.hidden = !message;
}

function setNormalError(message) {
  normalErrorMessage.textContent = message;
  normalErrorMessage.hidden = !message;
}

function showAdminMessage(message, isError = false) {
  adminDashboardMessage.textContent = message;
  adminDashboardMessage.className = `form-message ${isError ? "error" : "success"}`.trim();
}

async function connectToBackend(event) {
  event.preventDefault();
  setError("");

  if (location.protocol === "file:") {
    setError("Open the dashboard from http://127.0.0.1:8000 instead of opening index.html directly. From the repository root, run: py -m http.server 8000 --bind 127.0.0.1 --directory dashboard");
    return;
  }

  let baseUrl;
  try {
    baseUrl = apiBaseUrl();
  } catch (error) {
    setError(error.message || "Enter a valid backend URL.");
    backendInput.focus();
    return;
  }

  connectButton.disabled = true;
  connectButton.textContent = "Connecting…";
  setConnection("Checking backend…", "loading");
  try {
    const [health, dpcs] = await Promise.all([
      getJson(baseUrl, "/api/health", { timeoutMs: 8000 }),
      getJson(baseUrl, "/api/dpcs", { timeoutMs: 8000 }),
    ]);
    if (health.status !== "UP") {
      throw new Error("The backend health check did not report UP.");
    }
    if (!Array.isArray(dpcs)) {
      throw new Error("The backend is reachable but returned an unexpected DPC directory response.");
    }

    backendInput.value = baseUrl;
    localStorage.setItem("dpc-dashboard-backend-url", baseUrl);
    connectionPanel.hidden = true;
    modePanel.hidden = false;
    setConnection("Backend connected", "connected");
    pageSubtitle.textContent = "Choose the dashboard that matches your role.";
  } catch (error) {
    setConnection("Connection failed");
    const guidance = error instanceof TypeError
      ? " Check that the backend URL is correct and that the backend is running. If the dashboard is served separately, its origin must be allowed by the backend."
      : "";
    setError(`${error.message || "Could not connect to the backend."}${guidance}`);
  } finally {
    connectButton.disabled = false;
    connectButton.textContent = "Continue";
  }
}

async function openNormalDashboard() {
  adminUsername.value = "";
  adminPassword.value = "";
  adminSigninForm.hidden = true;
  adminSigninMessage.hidden = true;
  modePanel.hidden = true;
  normalDashboard.hidden = false;
  pageTitle.textContent = "Outreach overview";
  pageSubtitle.textContent = "Call activity, verification, and company follow-ups.";
  setNormalError("");
  await loadDpcs();
}

async function returnToModeChoice() {
  state.adminCredentials = null;
  state.simulationCredentials = null;
  normalDashboard.hidden = true;
  adminDashboard.hidden = true;
  simulationDashboard.hidden = true;
  simulationHistoryView.hidden = true;
  simulationEntryView.hidden = false;
  connectionPanel.hidden = true;
  modePanel.hidden = false;
  adminSigninForm.hidden = true;
  simulationSigninForm.hidden = true;
  adminSigninMessage.hidden = true;
  simulationSigninMessage.hidden = true;
  adminUsername.value = "";
  adminPassword.value = "";
  simulationUsername.value = "";
  simulationPassword.value = "";
  simulationDpcSelect.replaceChildren(new Option("Sign in to load DPCs", ""));
  simulationLogDpcSelect.replaceChildren(new Option("Sign in to load DPCs", ""));
  simulationContactsBody.replaceChildren();
  addEmptyRow(simulationContactsBody, "Sign in to load contacts.", 3);
  simulationCallsBody.replaceChildren();
  addEmptyRow(simulationCallsBody, "Select a DPC to view its simulated call logs.", 6);
  simulationHistoryError.hidden = true;
  simulationContactCount.textContent = "";
  simulationForm.reset();
  setDefaultSimulationTimestamp();
  simulationMessage.textContent = "";
  state.simulationDpcs = [];
  state.simulationContacts = [];
  state.simulationCalls = [];
  resetCompanyForm();
  resetContactForm();
  pageTitle.textContent = "DPC Outreach Monitor";
  pageSubtitle.textContent = "Checking backend connection…";
  setConnection("Checking backend…", "loading");
  try {
    const baseUrl = apiBaseUrl();
    const [health, dpcs] = await Promise.all([
      getJson(baseUrl, "/api/health", { timeoutMs: 8000 }),
      getJson(baseUrl, "/api/dpcs", { timeoutMs: 8000 }),
    ]);
    if (health.status !== "UP" || !Array.isArray(dpcs)) {
      throw new Error("Backend health or DPC directory check failed.");
    }
    pageSubtitle.textContent = "Choose the dashboard that matches your role.";
    setConnection("Backend connected", "connected");
  } catch (error) {
    pageSubtitle.textContent =
      `The backend is not reachable (${error.message || "connection check failed"}). Start it or change the backend URL to continue.`;
    setConnection("Backend disconnected");
  }
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
  if (parsedUrl.hostname === "localhost" && parsedUrl.origin !== location.origin) {
    parsedUrl.hostname = "127.0.0.1";
  }
  return parsedUrl.toString().replace(/\/+$/, "");
}

async function getJson(baseUrl, path, { timeoutMs = 15000 } = {}) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  let response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      headers: { Accept: "application/json" },
      signal: controller.signal,
    });
  } catch (error) {
    if (controller.signal.aborted) {
      throw new Error(`Backend request timed out after ${Math.ceil(timeoutMs / 1000)} seconds.`);
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
  if (!response.ok) {
    throw new Error(await responseError(response, path));
  }
  return response.json();
}

async function loadDashboard() {
  setNormalError("");
  const dpcId = Number(dpcSelect.value);
  if (!Number.isSafeInteger(dpcId) || dpcId < 1) {
    setNormalError("Load the DPC list and select a DPC first.");
    dpcSelect.focus();
    return;
  }

  let baseUrl;
  try {
    baseUrl = apiBaseUrl();
  } catch (error) {
    setNormalError(error.message || "Enter a valid backend URL.");
    return;
  }

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
    setNormalError(error.message || "Could not load dashboard data.");
  } finally {
    refreshButton.textContent = "Refresh dashboard";
    refreshButton.disabled = !dpcSelect.value;
  }
}

async function loadDpcs() {
  setNormalError("");
  let baseUrl;
  try {
    baseUrl = apiBaseUrl();
  } catch (error) {
    setNormalError(error.message || "Enter a valid backend URL.");
    return;
  }

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
    setNormalError(dpcs.length ? "" : "No DPCs are registered. Add DPC records before using the dashboard.");
  } catch (error) {
    setConnection("Connection failed");
    dpcSelect.replaceChildren(new Option("DPC list unavailable", ""));
    setNormalError(error.message || "Could not load the DPC directory.");
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

  const statuses = [...new Set(state.calls.map(displayedVerificationStatus))].sort();
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
    return (!selectedStatus || displayedVerificationStatus(call) === selectedStatus) &&
      (!query || searchable.includes(query));
  }).slice(0, 100);

  callsBody.replaceChildren();
  if (filteredCalls.length === 0) {
    addEmptyRow(callsBody, "No call records match this DPC and filter.", 8);
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
      makeBadgeCell(displayedVerificationStatus(call)),
      makeCell(displayCallSource(call)),
      makeCell(`#${call.id}`),
    );
    const actions = document.createElement("td");
    if (displayedVerificationStatus(call) === "UNMATCHED_NUMBER") {
      const matchButton = document.createElement("button");
      matchButton.type = "button";
      matchButton.className = "button button-small button-secondary";
      matchButton.dataset.action = "match-call";
      matchButton.dataset.id = String(call.id);
      matchButton.textContent = "Match to company";
      actions.append(matchButton);
    }
    actions.append(createDeleteCallButton(call.id));
    row.append(actions);
    callsBody.append(row);
  }
}

function displayedVerificationStatus(call) {
  return call.contactId === null && call.verificationStatus !== "DUPLICATE_CALL"
    ? "UNMATCHED_NUMBER"
    : call.verificationStatus;
}

function displayCallSource(call) {
  if (call.callSource === "SIMULATION") return "Simulation";
  if (call.callType === "DIALER") return "App · Dialer";
  if (call.callType === "MANUAL_METADATA") return "App · Manual metadata";
  return "App · Type not recorded";
}

function createDeleteCallButton(id) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "button button-small button-danger";
  button.dataset.action = "delete-call";
  button.dataset.id = String(id);
  button.textContent = "Delete call";
  return button;
}

function handleCallAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const call = state.calls.find((item) => item.id === Number(button.dataset.id));
  if (button.dataset.action === "delete-call") {
    if (!call) return;
    openDeleteConfirmation({
      kind: "call-record",
      id: call.id,
      source: "APP",
      description: `Permanently delete call record #${call.id} (${call.phoneNumber})? This cannot be undone. If it is the latest call for its company, the call-status summary will be recalculated.`,
    });
    return;
  }
  if (button.dataset.action !== "match-call") return;
  if (!call || displayedVerificationStatus(call) !== "UNMATCHED_NUMBER") return;

  state.pendingCallMatch = call;
  const existingContacts = [...state.contacts.values()].filter(
    (contact) => contact.phoneNumber.replace(/\D/g, "") === call.phoneNumber.replace(/\D/g, ""),
  );
  const existingContactNote = existingContacts.length
    ? ` A contact with this number already exists: ${existingContacts.map((contact) => `${contact.name} (${contact.companyName})`).join(", ")}. A separate contact will be created for the name you enter; future calls from this number will remain unmatched for manual assignment.`
    : "";
  callMatchDescription.textContent =
    `Create a new contact for ${call.phoneNumber} and associate this call with the selected company.${existingContactNote}`;
  callMatchCompany.replaceChildren(new Option("Choose a company", ""));
  for (const company of state.companies) {
    callMatchCompany.add(new Option(company.companyName, String(company.companyId)));
  }
  if (state.companies.length === 0) {
    callMatchCompany.add(new Option("No companies are registered", ""));
  }
  callMatchError.hidden = true;
  callMatchError.textContent = "";
  callMatchSubmit.disabled = state.companies.length === 0;
  if (state.companies.length === 0) {
    callMatchError.textContent = "There are no companies to choose from. Ask an admin to add a company first.";
    callMatchError.hidden = false;
  }
  callMatchDialog.showModal();
  callMatchName.focus();
}

function handleSimulationCallAction(event) {
  const button = event.target.closest('button[data-action="delete-call"]');
  if (!button) return;
  const call = state.simulationCalls.find((item) => item.id === Number(button.dataset.id));
  if (!call) return;
  openDeleteConfirmation({
    kind: "call-record",
    id: call.id,
    source: "SIMULATION",
    description: `Permanently delete simulated call record #${call.id} (${call.phoneNumber})? This cannot be undone.`,
  });
}

function closeCallMatchDialog() {
  if (state.callMatchSubmitting) return;
  callMatchDialog.close();
  state.pendingCallMatch = null;
  callMatchForm.reset();
}

async function matchCall(event) {
  event.preventDefault();
  const call = state.pendingCallMatch;
  const contactName = callMatchName.value.trim();
  const companyId = Number(callMatchCompany.value);
  if (!call || !contactName || !Number.isSafeInteger(companyId) || companyId < 1) return;

  callMatchSubmit.disabled = true;
  state.callMatchSubmitting = true;
  callMatchError.hidden = true;
  try {
    const baseUrl = apiBaseUrl();
    const response = await fetch(`${baseUrl}/api/call-records/${call.id}/match`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ contactName, companyId }),
    });
    if (!response.ok) {
      throw new Error(await responseError(response, `/api/call-records/${call.id}/match`));
    }
    callMatchDialog.close();
    state.pendingCallMatch = null;
    callMatchForm.reset();
    await loadDashboard();
  } catch (error) {
    callMatchError.textContent = error.message || "Could not match this call.";
    callMatchError.hidden = false;
  } finally {
    state.callMatchSubmitting = false;
    callMatchSubmit.disabled = state.companies.length === 0;
  }
}

function renderCompanies() {
  companiesBody.replaceChildren();
  if (state.companies.length === 0) {
    addEmptyRow(companiesBody, "No companies are registered in the backend.", 5);
    return;
  }

  for (const company of state.companies) {
    const row = document.createElement("tr");
    const decision = company.coreOutreachStatus === "NOT_SET"
      ? "Not set"
      : displayStatus(company.coreOutreachStatus || "NOT_SET");
    row.append(
      makeCell(company.companyName),
      makeCell(decision),
      makeCell(formatDate(company.latestCallTimestamp)),
      company.status ? makeBadgeCell(company.status) : makeCell("No call recorded"),
      makeCell(formatDate(company.updatedAt)),
    );
    companiesBody.append(row);
  }
}

async function loginSimulation(event) {
  event.preventDefault();
  simulationSigninMessage.hidden = true;
  const username = simulationUsername.value.trim();
  const password = simulationPassword.value;
  if (!username || !password) {
    showSimulationSigninError("Enter both the admin username and password.");
    return;
  }

  try {
    ensureSecureAdminConnection(apiBaseUrl());
  } catch (error) {
    showSimulationSigninError(error.message || "Enter a valid backend URL.");
    return;
  }

  simulationSigninButton.disabled = true;
  simulationSigninButton.textContent = "Signing in…";
  try {
    await requestAdminWithCredentials("/api/admin/companies", {}, username, password);
    const [dpcs, contacts] = await Promise.all([
      getJson(apiBaseUrl(), "/api/dpcs"),
      getJson(apiBaseUrl(), "/api/contacts"),
    ]);
    if (!Array.isArray(dpcs) || !Array.isArray(contacts)) {
      throw new Error("Backend returned the DPC or contact directory in an unexpected format.");
    }

    state.simulationCredentials = { username, password };
    simulationUsername.value = "";
    simulationPassword.value = "";
    modePanel.hidden = true;
    simulationDashboard.hidden = false;
    simulationHistoryView.hidden = true;
    simulationEntryView.hidden = false;
    pageTitle.textContent = "DPC Simulation";
    pageSubtitle.textContent = "Add test call logs for an available DPC.";
    document.querySelector("#simulation-welcome").textContent = `Signed in as ${username}`;
    setConnection("DPC simulation signed in", "connected");
    renderSimulationDpcs(dpcs);
    renderSimulationContacts(contacts);
    setDefaultSimulationTimestamp();
    simulationMessage.textContent = dpcs.length
      ? "Choose a DPC and enter the simulated call details."
      : "No DPCs are registered. Create a DPC before adding simulated calls.";
    simulationMessage.className = dpcs.length ? "form-message simulation-message" : "form-message error simulation-message";
  } catch (error) {
    const message = error instanceof TypeError
      ? "Could not reach the backend. Check that it is running and allows requests from this dashboard."
      : error.message || "Could not verify admin access.";
    if (message.includes("Check the username and password")) {
      simulationPassword.value = "";
    }
    showSimulationSigninError(message);
  } finally {
    simulationSigninButton.disabled = false;
    simulationSigninButton.textContent = "Sign in for DPC Simulation";
  }
}

function showSimulationSigninError(message) {
  simulationSigninMessage.textContent = message;
  simulationSigninMessage.hidden = false;
}

function renderSimulationDpcs(dpcs) {
  const selectedDpcId = simulationDpcSelect.value;
  const selectedLogDpcId = simulationLogDpcSelect.value;
  state.simulationDpcs = dpcs;
  simulationDpcSelect.replaceChildren(new Option(
    dpcs.length ? "Select a DPC…" : "No DPCs available",
    "",
  ));
  simulationLogDpcSelect.replaceChildren(new Option(
    dpcs.length ? "Select a DPC…" : "No DPCs available",
    "",
  ));
  for (const dpc of dpcs) {
    simulationDpcSelect.add(new Option(`${dpc.name} — ID: ${dpc.id}`, String(dpc.id)));
    simulationLogDpcSelect.add(new Option(`${dpc.name} — ID: ${dpc.id}`, String(dpc.id)));
  }
  if (dpcs.some((dpc) => String(dpc.id) === selectedDpcId)) simulationDpcSelect.value = selectedDpcId;
  if (dpcs.some((dpc) => String(dpc.id) === selectedLogDpcId)) {
    simulationLogDpcSelect.value = selectedLogDpcId;
  }
  simulationSubmitButton.disabled = dpcs.length === 0;
}

function renderSimulationContacts(contacts) {
  state.simulationContacts = contacts;
  simulationContactsBody.replaceChildren();
  simulationContactCount.textContent =
    `${contacts.length} contact${contacts.length === 1 ? "" : "s"}`;
  if (!contacts.length) {
    addEmptyRow(simulationContactsBody, "No contacts are registered.", 3);
    return;
  }
  for (const contact of contacts) {
    const row = document.createElement("tr");
    row.append(
      makeCell(contact.name),
      makeCell(contact.phoneNumber),
      makeCell(contact.companyName),
    );
    simulationContactsBody.append(row);
  }
}

function setDefaultSimulationTimestamp() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  simulationTimestampInput.value = now.toISOString().slice(0, 16);
}

async function openSimulationCallHistory() {
  simulationEntryView.hidden = true;
  simulationHistoryView.hidden = false;
  simulationHistoryError.hidden = true;
  if (!simulationLogDpcSelect.value && simulationDpcSelect.value) {
    simulationLogDpcSelect.value = simulationDpcSelect.value;
  }
  await loadSimulationCalls();
}

function closeSimulationCallHistory() {
  simulationHistoryView.hidden = true;
  simulationEntryView.hidden = false;
}

async function loadSimulationCalls() {
  if (!state.simulationCredentials || simulationHistoryView.hidden) return;
  simulationHistoryError.hidden = true;
  const dpcId = Number(simulationLogDpcSelect.value);
  if (!Number.isSafeInteger(dpcId) || dpcId < 1) {
    simulationCallsBody.replaceChildren();
    addEmptyRow(simulationCallsBody, "Select a DPC to view its simulated call logs.", 5);
    return;
  }

  const refreshButton = document.querySelector("#simulation-history-refresh");
  refreshButton.disabled = true;
  refreshButton.textContent = "Loading…";
  try {
    const calls = await requestAdminWithCredentials(
      `/api/admin/simulation/call-records?dpcId=${encodeURIComponent(dpcId)}`,
      {},
      state.simulationCredentials.username,
      state.simulationCredentials.password,
    );
    if (!Array.isArray(calls)) {
      throw new Error("Backend returned simulated call logs in an unexpected format.");
    }
    state.simulationCalls = calls;
    renderSimulationCalls();
  } catch (error) {
    simulationHistoryError.textContent = error.message || "Could not load simulated call logs.";
    simulationHistoryError.hidden = false;
    simulationCallsBody.replaceChildren();
    addEmptyRow(simulationCallsBody, "Could not load simulated call logs.", 6);
  } finally {
    refreshButton.disabled = false;
    refreshButton.textContent = "Load logs";
  }
}

function renderSimulationCalls() {
  simulationCallsBody.replaceChildren();
  if (state.simulationCalls.length === 0) {
    addEmptyRow(simulationCallsBody, "No simulated call logs exist for this DPC.", 6);
    return;
  }
  for (const call of state.simulationCalls) {
    const row = document.createElement("tr");
    row.append(
      makeCell(formatDate(call.callTimestamp)),
      makeCell(call.phoneNumber),
      makeCell(formatDuration(call.durationSeconds)),
      makeBadgeCell(displayedVerificationStatus(call)),
      makeCell(`#${call.id}`),
    );
    const actions = document.createElement("td");
    actions.append(createDeleteCallButton(call.id));
    row.append(actions);
    simulationCallsBody.append(row);
  }
}

async function loadSimulationDirectory() {
  if (!state.simulationCredentials) return;
  const refreshButton = document.querySelector("#simulation-refresh-button");
  refreshButton.disabled = true;
  refreshButton.textContent = "Loading…";
  simulationMessage.textContent = "";
  try {
    await requestAdminWithCredentials(
      "/api/admin/companies",
      {},
      state.simulationCredentials.username,
      state.simulationCredentials.password,
    );
    const [dpcs, contacts] = await Promise.all([
      getJson(apiBaseUrl(), "/api/dpcs"),
      getJson(apiBaseUrl(), "/api/contacts"),
    ]);
    if (!Array.isArray(dpcs) || !Array.isArray(contacts)) {
      throw new Error("Backend returned the DPC or contact directory in an unexpected format.");
    }
    renderSimulationDpcs(dpcs);
    renderSimulationContacts(contacts);
    simulationMessage.textContent = dpcs.length
      ? "DPC and contact directories refreshed."
      : "No DPCs are registered. Create a DPC before adding simulated calls.";
    simulationMessage.className = dpcs.length ? "form-message simulation-message" : "form-message error simulation-message";
  } catch (error) {
    simulationMessage.textContent = error.message || "Could not refresh the DPC list.";
    simulationMessage.className = "form-message error simulation-message";
  } finally {
    refreshButton.disabled = false;
    refreshButton.textContent = "Refresh directory";
  }
}

async function addSimulatedCall(event) {
  event.preventDefault();
  if (!state.simulationCredentials) {
    simulationMessage.textContent = "Sign in as Admin to add simulated call logs.";
    simulationMessage.className = "form-message error simulation-message";
    return;
  }

  const dpcId = Number(simulationDpcSelect.value);
  const durationSeconds = Number(simulationDurationInput.value);
  const callTimestamp = new Date(simulationTimestampInput.value);
  if (!Number.isSafeInteger(dpcId) || dpcId < 1 ||
      !Number.isSafeInteger(durationSeconds) || durationSeconds < 0 ||
      Number.isNaN(callTimestamp.getTime())) {
    simulationMessage.textContent = "Choose a DPC and enter a valid call time and non-negative whole-number duration.";
    simulationMessage.className = "form-message error simulation-message";
    return;
  }

  simulationSubmitButton.disabled = true;
  simulationSubmitButton.textContent = "Adding call…";
  simulationMessage.textContent = "";
  try {
    const record = await requestAdminWithCredentials(
      "/api/admin/simulation/call-records",
      {
        method: "POST",
        body: JSON.stringify({
          dpcId,
          phoneNumber: simulationPhoneInput.value.trim(),
          callTimestamp: callTimestamp.toISOString(),
          durationSeconds,
        }),
      },
      state.simulationCredentials.username,
      state.simulationCredentials.password,
    );
    const selectedDpc = state.simulationDpcs.find((dpc) => dpc.id === dpcId);
    simulationForm.reset();
    if (selectedDpc) simulationDpcSelect.value = String(selectedDpc.id);
    setDefaultSimulationTimestamp();
    simulationMessage.textContent =
      `Call log #${record.id} added for ${selectedDpc?.name || `DPC ${dpcId}`} with status ${displayStatus(record.verificationStatus)}.`;
    simulationMessage.className = "form-message simulation-message";
  } catch (error) {
    simulationMessage.textContent = error.message || "Could not add the simulated call log.";
    simulationMessage.className = "form-message error simulation-message";
  } finally {
    simulationSubmitButton.disabled = state.simulationDpcs.length === 0;
    simulationSubmitButton.textContent = "Add call log";
  }
}

async function loginAdmin(event) {
  event.preventDefault();
  adminSigninMessage.hidden = true;
  const username = adminUsername.value.trim();
  const password = adminPassword.value;
  if (!username || !password) {
    showAdminSigninError("Enter both the admin username and password.");
    return;
  }

  let baseUrl;
  try {
    baseUrl = apiBaseUrl();
    ensureSecureAdminConnection(baseUrl);
  } catch (error) {
    showAdminSigninError(error.message || "Enter a valid backend URL.");
    return;
  }

  adminSigninButton.disabled = true;
  adminSigninButton.textContent = "Signing in…";
  try {
    const response = await fetch(`${baseUrl}/api/admin/companies`, {
      headers: {
        Accept: "application/json",
        Authorization: basicAuthorization(username, password),
      },
    });
    if (response.status === 401 || response.status === 403) {
      throw new Error("Admin sign-in failed. Check the username and password.");
    }
    if (!response.ok) {
      throw new Error(`Could not verify admin access (${await responseError(response, "/api/admin/companies")}).`);
    }
    const companies = await response.json();
    if (!Array.isArray(companies)) {
      throw new Error("Backend returned an unexpected response while verifying admin access.");
    }

    state.adminCredentials = { username, password };
    state.adminCompanies = companies;
    adminPassword.value = "";
    modePanel.hidden = true;
    adminDashboard.hidden = false;
    pageTitle.textContent = "Admin dashboard";
    pageSubtitle.textContent = "Manage the company and contact directory and CORE follow-up decisions.";
    document.querySelector("#admin-welcome").textContent = `Signed in as ${username}`;
    setConnection("Admin signed in", "connected");
    await loadAdminData();
  } catch (error) {
    const message = error instanceof TypeError
      ? "Could not reach the backend. Check that it is running and allows requests from this dashboard."
      : error.message || "Could not verify admin access.";
    if (message.includes("Check the username and password")) {
      adminPassword.value = "";
    }
    showAdminSigninError(message);
  } finally {
    adminSigninButton.disabled = false;
    adminSigninButton.textContent = "Sign in as Admin";
  }
}

function showAdminSigninError(message) {
  adminSigninMessage.textContent = message;
  adminSigninMessage.hidden = false;
}

async function loadAdminData() {
  if (!state.adminCredentials) return;
  const refreshButton = document.querySelector("#admin-refresh-button");
  refreshButton.disabled = true;
  refreshButton.textContent = "Refreshing…";
  try {
    const [companies, contacts, statuses, dpcs] = await Promise.all([
      requestAdmin("/api/admin/companies"),
      getJson(apiBaseUrl(), "/api/contacts"),
      getJson(apiBaseUrl(), "/api/outreach-statuses"),
      getJson(apiBaseUrl(), "/api/dpcs"),
    ]);
    if (![companies, contacts, statuses, dpcs].every(Array.isArray)) {
      throw new Error("Backend returned data in an unexpected format.");
    }
    state.adminCompanies = companies;
    state.adminContacts = contacts;
    state.adminOutreachStatuses = statuses;
    state.adminDpcs = dpcs;
    renderAdminDashboard();
    showAdminMessage("Admin data is up to date.");
    return true;
  } catch (error) {
    showAdminMessage(error.message || "Could not load admin data.", true);
    return false;
  } finally {
    refreshButton.disabled = false;
    refreshButton.textContent = "Refresh data";
  }
}

async function requestAdminWithCredentials(path, options, username, password) {
  const baseUrl = apiBaseUrl();
  ensureSecureAdminConnection(baseUrl);
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      Authorization: basicAuthorization(username, password),
      ...options.headers,
    },
  });
  if (response.status === 401 || response.status === 403) {
    throw new Error("Admin sign-in failed. Check the username and password.");
  }
  if (!response.ok) {
    throw new Error(await responseError(response, path));
  }
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function requestAdmin(path, options = {}, passwordConfirmation = null) {
  if (!state.adminCredentials) {
    throw new Error("Sign in as Admin to continue.");
  }
  const baseUrl = apiBaseUrl();
  ensureSecureAdminConnection(baseUrl);
  const credentials = state.adminCredentials;
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      Authorization: basicAuthorization(
        credentials.username,
        passwordConfirmation === null ? credentials.password : passwordConfirmation,
      ),
      ...options.headers,
    },
  });
  if (response.status === 401 || response.status === 403) {
    if (passwordConfirmation !== null) {
      throw new Error("The admin password was not accepted. Check it and try again.");
    }
    state.adminCredentials = null;
    returnToModeChoice();
    adminSigninForm.hidden = false;
    showAdminSigninError("Admin session expired or was rejected. Sign in again.");
    throw new Error("Admin session expired or was rejected. Sign in again.");
  }
  try {
    if (!response.ok) {
      throw new Error(await responseError(response, path));
    }
    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error("Backend returned an invalid response.");
    }
    throw error;
  }
}

async function responseError(response, path) {
  const text = await response.text();
  let detail = "";
  if (text) {
    try {
      const body = JSON.parse(text);
      detail = body.detail || body.message || body.title || "";
    } catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
    }
  }
  if (response.status === 404 && path?.startsWith("/api/")) {
    const endpoint = path.split("?")[0];
    return `Backend returned HTTP 404 for ${endpoint}. The backend is reachable, but this API endpoint is unavailable. The dashboard may be connected to an older or different backend; stop the old backend and start this project's backend, then reconnect.${detail ? ` Server response: ${detail}` : ""}`;
  }
  if (detail) return `Backend returned HTTP ${response.status}: ${detail}`;
  return `Backend returned HTTP ${response.status}${text ? `: ${text}` : ""}`;
}

function renderAdminDashboard() {
  const selectedCompanyId = contactCompanySelect.value;
  const statusByCompany = new Map(
    state.adminOutreachStatuses.map((status) => [status.companyId, status]),
  );
  const companyOptions = state.adminCompanies
    .map((company) => new Option(company.name, String(company.id)));
  contactCompanySelect.replaceChildren(new Option("Select a company", ""));
  for (const option of companyOptions) {
    contactCompanySelect.add(option);
  }
  if (state.adminCompanies.some((company) => String(company.id) === selectedCompanyId)) {
    contactCompanySelect.value = selectedCompanyId;
  }

  adminDpcsBody.replaceChildren();
  if (!state.adminDpcs.length) {
    addEmptyRow(adminDpcsBody, "No DPCs yet. Create one above.", 2);
  }
  for (const dpc of state.adminDpcs) {
    const row = document.createElement("tr");
    row.append(makeCell(`#${dpc.id}`), makeCell(dpc.name));
    adminDpcsBody.append(row);
  }

  adminCompaniesBody.replaceChildren();
  if (!state.adminCompanies.length) {
    addEmptyRow(adminCompaniesBody, "No companies yet. Add one above.", 3);
  }
  for (const company of state.adminCompanies) {
    const status = statusByCompany.get(company.id);
    const row = document.createElement("tr");
    const actions = document.createElement("td");
    actions.append(
      makeActionButton("Edit", "edit-company", company.id),
      makeActionButton("Delete", "delete-company", company.id, "button-danger"),
    );
    row.append(
      makeCell(company.name),
      status ? makeCell(displayStatus(status.coreOutreachStatus || "NOT_SET")) : makeCell("Not set"),
      actions,
    );
    adminCompaniesBody.append(row);
  }

  adminContactsBody.replaceChildren();
  if (!state.adminContacts.length) {
    addEmptyRow(adminContactsBody, "No contacts yet. Add one above.", 4);
  }
  for (const contact of state.adminContacts) {
    const row = document.createElement("tr");
    const actions = document.createElement("td");
    actions.append(
      makeActionButton("Edit", "edit-contact", contact.id),
      makeActionButton("Delete", "delete-contact", contact.id, "button-danger"),
    );
    row.append(
      makeCell(contact.name),
      makeCell(contact.phoneNumber),
      makeCell(contact.companyName),
      actions,
    );
    adminContactsBody.append(row);
  }

  adminStatusesBody.replaceChildren();
  if (!state.adminOutreachStatuses.length) {
    addEmptyRow(adminStatusesBody, "No companies are registered.", 3);
  }
  for (const status of state.adminOutreachStatuses) {
    const row = document.createElement("tr");
    const statusCell = document.createElement("td");
    const statusSelect = makeCoreStatusSelect(status.companyId, status.coreOutreachStatus);
    statusCell.append(statusSelect);
    row.append(
      makeCell(status.companyName),
      statusCell,
      status.status ? makeBadgeCell(status.status) : makeCell("No call recorded"),
    );
    adminStatusesBody.append(row);
  }
}

function makeActionButton(label, action, id, className = "button-secondary") {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `button button-small ${className}`;
  button.dataset.action = action;
  button.dataset.id = String(id);
  button.textContent = label;
  return button;
}

function makeCoreStatusSelect(companyId, value) {
  const select = document.createElement("select");
  select.className = "core-status-select";
  select.dataset.companyId = String(companyId);
  for (const [status, label] of [
    ["NOT_SET", "Not set"],
    ["CONFIRMED", "Confirmed"],
    ["PENDING", "Pending"],
    ["DECLINED", "Declined"],
  ]) {
    select.add(new Option(label, status));
  }
  select.value = value || "NOT_SET";
  return select;
}

async function saveDpc(event) {
  event.preventDefault();
  const name = dpcNameInput.value.trim();
  if (!name) return;
  dpcSaveButton.disabled = true;
  try {
    const created = await requestAdmin("/api/admin/dpcs", {
      method: "POST",
      body: JSON.stringify({ name }),
    });
    dpcForm.reset();
    await loadAdminData();
    showAdminMessage(`Created DPC ${created.name} (ID: ${created.id}).`);
  } catch (error) {
    showAdminMessage(error.message || "Could not create DPC.", true);
  } finally {
    dpcSaveButton.disabled = false;
  }
}

async function saveCompany(event) {
  event.preventDefault();
  const id = companyIdInput.value;
  const name = companyNameInput.value.trim();
  if (!name) return;
  const submitButton = companySaveButton;
  submitButton.disabled = true;
  try {
    await requestAdmin(id ? `/api/admin/companies/${id}` : "/api/admin/companies", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify({ name }),
    });
    resetCompanyForm();
    await loadAdminData();
    showAdminMessage(id ? "Company updated." : "Company created.");
  } catch (error) {
    showAdminMessage(error.message || "Could not save company.", true);
  } finally {
    submitButton.disabled = false;
  }
}

async function saveContact(event) {
  event.preventDefault();
  const id = contactIdInput.value;
  const request = {
    name: contactNameInput.value.trim(),
    phoneNumber: contactPhoneInput.value.trim(),
    companyId: Number(contactCompanySelect.value),
  };
  if (!request.name || !request.phoneNumber || !request.companyId) return;
  contactSaveButton.disabled = true;
  try {
    await requestAdmin(id ? `/api/admin/contacts/${id}` : "/api/admin/contacts", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(request),
    });
    resetContactForm();
    await loadAdminData();
    showAdminMessage(id ? "Contact updated." : "Contact created.");
  } catch (error) {
    showAdminMessage(error.message || "Could not save contact.", true);
  } finally {
    contactSaveButton.disabled = false;
  }
}

async function handleCompanyAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const id = Number(button.dataset.id);
  const company = state.adminCompanies.find((item) => item.id === id);
  if (!company) return;
  if (button.dataset.action === "edit-company") {
    companyIdInput.value = String(company.id);
    companyNameInput.value = company.name;
    companySaveButton.textContent = "Save company";
    companyCancelButton.hidden = false;
    companyNameInput.focus();
    return;
  }
  if (button.dataset.action === "delete-company") {
    openDeleteConfirmation({
      kind: "company",
      id,
      name: company.name,
      description: `Delete ${company.name} and all of its contacts? Call history will be kept as unmatched records. Its outreach statuses will also be removed.`,
    });
  }
}

async function handleContactAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const id = Number(button.dataset.id);
  const contact = state.adminContacts.find((item) => item.id === id);
  if (!contact) return;
  if (button.dataset.action === "edit-contact") {
    contactIdInput.value = String(contact.id);
    contactNameInput.value = contact.name;
    contactPhoneInput.value = contact.phoneNumber;
    contactCompanySelect.value = String(contact.companyId);
    contactSaveButton.textContent = "Save contact";
    contactCancelButton.hidden = false;
    contactNameInput.focus();
    return;
  }
  if (button.dataset.action === "delete-contact") {
    openDeleteConfirmation({
      kind: "contact",
      id,
      name: contact.name,
      description: `Delete ${contact.name}? Their call history will be kept as unmatched records.`,
    });
  }
}

function openDeleteConfirmation(deletion) {
  state.pendingDelete = deletion;
  deleteConfirmDescription.textContent = deletion.description;
  const needsAdminCredentials = deletion.kind === "call-record";
  deleteConfirmUsernameField.hidden = !needsAdminCredentials;
  deleteConfirmUsername.required = needsAdminCredentials;
  deleteConfirmUsername.value = "";
  deleteConfirmPasswordLabel.textContent = needsAdminCredentials
    ? "Admin password"
    : "Enter your current admin password";
  deleteConfirmPassword.value = "";
  deleteConfirmError.hidden = true;
  deleteConfirmError.textContent = "";
  deleteConfirmButton.disabled = false;
  deleteConfirmDialog.showModal();
  (needsAdminCredentials ? deleteConfirmUsername : deleteConfirmPassword).focus();
}

function closeDeleteConfirmation() {
  if (deleteConfirmButton.disabled) return;
  deleteConfirmDialog.close();
  state.pendingDelete = null;
  deleteConfirmUsername.value = "";
  deleteConfirmPassword.value = "";
}

async function confirmDelete(event) {
  event.preventDefault();
  const deletion = state.pendingDelete;
  const username = deleteConfirmUsername.value.trim();
  const password = deleteConfirmPassword.value;
  if (!deletion || !password || (deletion.kind === "call-record" && !username)) return;

  deleteConfirmButton.disabled = true;
  deleteConfirmError.hidden = true;
  try {
    if (deletion.kind === "call-record") {
      await requestAdminWithCredentials(
        `/api/admin/call-records/${deletion.id}`,
        { method: "DELETE" },
        username,
        password,
      );
      deleteConfirmDialog.close();
      state.pendingDelete = null;
      deleteConfirmUsername.value = "";
      deleteConfirmPassword.value = "";
      if (deletion.source === "SIMULATION") {
        await loadSimulationCalls();
      } else {
        await loadDashboard();
      }
      return;
    }

    const resource = deletion.kind === "company" ? "companies" : "contacts";
    await requestAdmin(`/api/admin/${resource}/${deletion.id}`, { method: "DELETE" }, password);
    deleteConfirmDialog.close();
    state.pendingDelete = null;
    deleteConfirmUsername.value = "";
    deleteConfirmPassword.value = "";
    const refreshed = await loadAdminData();
    const resourceName = deletion.kind === "company" ? "Company" : "Contact";
    showAdminMessage(
      refreshed
        ? `${resourceName} deleted. Call history was preserved as unmatched.`
        : `${resourceName} deleted, but the dashboard could not refresh its data.`,
      !refreshed,
    );
  } catch (error) {
    deleteConfirmError.textContent = error.message || "Could not delete this record.";
    deleteConfirmError.hidden = false;
    deleteConfirmPassword.value = "";
    deleteConfirmPassword.focus();
  } finally {
    deleteConfirmButton.disabled = false;
  }
}

function resetCompanyForm() {
  companyIdInput.value = "";
  companyNameInput.value = "";
  companySaveButton.textContent = "Add company";
  companyCancelButton.hidden = true;
}

function resetContactForm() {
  contactIdInput.value = "";
  contactNameInput.value = "";
  contactPhoneInput.value = "";
  contactCompanySelect.value = "";
  contactSaveButton.textContent = "Add contact";
  contactCancelButton.hidden = true;
}

async function updateCoreOutreachStatus(event) {
  const statusSelect = event.target.closest(".core-status-select");
  if (!statusSelect) return;
  const companyId = Number(statusSelect.dataset.companyId);
  const company = state.adminOutreachStatuses.find((item) => item.companyId === companyId);
  if (!company) return;
  const previousStatus = company.coreOutreachStatus || "NOT_SET";
  const selectedStatus = statusSelect.value;
  statusSelect.disabled = true;
  try {
    const updated = await requestAdmin(`/api/admin/companies/${companyId}/outreach-status`, {
      method: "PUT",
      body: JSON.stringify({ status: selectedStatus }),
    });
    company.coreOutreachStatus = updated.status;
    company.coreOutreachStatusUpdatedAt = updated.updatedAt;
    showAdminMessage(`Updated ${company.companyName} to ${displayStatus(updated.status)}.`);
  } catch (error) {
    company.coreOutreachStatus = previousStatus;
    showAdminMessage(error.message || "Could not update outreach decision.", true);
  } finally {
    renderAdminDashboard();
  }
}

function ensureSecureAdminConnection(baseUrl) {
  const backend = new URL(baseUrl);
  if (backend.protocol !== "https:" &&
      !["localhost", "127.0.0.1", "[::1]"].includes(backend.hostname)) {
    throw new Error("Use HTTPS before sending admin credentials to a remote backend.");
  }
}

function basicAuthorization(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
  return `Basic ${btoa(binary)}`;
}

function displayStatus(status) {
  return String(status).replaceAll("_", " ").toLocaleLowerCase()
    .replace(/\b\w/g, (letter) => letter.toLocaleUpperCase());
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
