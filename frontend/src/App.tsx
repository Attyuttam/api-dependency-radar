import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  CheckCircle2,
  ChevronRight,
  CircleHelp,
  Clock3,
  Code2,
  ExternalLink,
  FilePlus2,
  GitCompareArrows,
  Globe2,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  X,
  Zap,
} from "lucide-react";

import {
  api,
  clearCredentials,
  setCredentials,
  type ApiChange,
  type ApiDependency,
  type UserProfile,
} from "./api";

import "./App.css";

type AuthMode = "login" | "register";

function formatDate(value: string | null): string {
  if (!value) return "Never";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "Unknown";

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Something went wrong.";
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();

  const className =
    normalized === "ACTIVE" || normalized === "HEALTHY"
      ? "status-badge status-healthy"
      : normalized === "ERROR" || normalized === "FAILED"
        ? "status-badge status-error"
        : "status-badge status-neutral";

  return (
    <span className={className}>
      <span className="status-dot" />
      {status || "UNKNOWN"}
    </span>
  );
}

function AuthScreen({
  onAuthenticated,
}: {
  onAuthenticated: (user: UserProfile) => void;
}) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    const normalizedEmail = email.trim();

    if (!normalizedEmail || !password) {
      setError("Enter your email and password.");
      return;
    }

    if (mode === "register") {
      if (password.length < 12) {
        setError("Your password must contain at least 12 characters.");
        return;
      }

      if (password.length > 72) {
        setError("Your password must be no longer than 72 characters.");
        return;
      }

      if (password !== confirmPassword) {
        setError("Your passwords do not match.");
        return;
      }
    }

    setBusy(true);

    try {
      if (mode === "register") {
        await api.registerAccount(normalizedEmail, password);
      }

      setCredentials(normalizedEmail, password);

      const user = await api.getCurrentUser();
      onAuthenticated(user);
    } catch (err) {
      clearCredentials();
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-brand-panel">
        <div className="brand-lockup">
          <div className="brand-icon">
            <Activity size={23} />
          </div>
          <span>API Dependency Radar</span>
        </div>

        <div className="auth-pitch">
          <div className="eyebrow">
            <span className="eyebrow-dot" />
            API CHANGE INTELLIGENCE
          </div>

          <h1>
            Know what
            <br />
            changed.
            <br />
            <span>Before it breaks.</span>
          </h1>

          <p>
            Track the APIs your systems depend on. Detect specification
            changes and spot breaking changes before they surprise your team.
          </p>

          <div className="pitch-points">
            <div>
              <ShieldCheck size={19} />
              <span>Understand breaking changes</span>
            </div>
            <div>
              <GitCompareArrows size={19} />
              <span>Track API specification drift</span>
            </div>
            <div>
              <Zap size={19} />
              <span>Check dependencies on demand</span>
            </div>
          </div>
        </div>

        <div className="auth-footer">
          Built for teams that depend on APIs.
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand">
            <div className="brand-icon">
              <Activity size={22} />
            </div>
            <span>API Dependency Radar</span>
          </div>

          <div className="auth-heading">
            <div className="eyebrow">YOUR WORKSPACE</div>
            <h2>
              {mode === "login" ? "Welcome back" : "Create your account"}
            </h2>
            <p>
              {mode === "login"
                ? "Sign in to see your monitored API dependencies."
                : "Start monitoring the APIs your application relies on."}
            </p>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label htmlFor="auth-email">Email address</label>
            <input
              id="auth-email"
              type="email"
              autoComplete="email"
              placeholder="you@company.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              maxLength={320}
              required
            />

            <label htmlFor="auth-password">Password</label>
            <input
              id="auth-password"
              type="password"
              autoComplete={
                mode === "login" ? "current-password" : "new-password"
              }
              placeholder="Enter your password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              maxLength={72}
              required
            />

            {mode === "register" && (
              <>
                <label htmlFor="confirm-password">Confirm password</label>
                <input
                  id="confirm-password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Enter your password again"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  maxLength={72}
                  required
                />
                <p className="field-hint">
                  Use between 12 and 72 characters.
                </p>
              </>
            )}

            {error && (
              <div className="inline-error" role="alert">
                <AlertTriangle size={17} />
                <span>{error}</span>
              </div>
            )}

            <button className="button button-primary auth-submit" disabled={busy}>
              {busy ? (
                <LoaderCircle className="spin" size={18} />
              ) : (
                <ChevronRight size={18} />
              )}
              {busy
                ? "Please wait..."
                : mode === "login"
                  ? "Sign in"
                  : "Create account"}
            </button>
          </form>

          <div className="auth-switch">
            {mode === "login" ? (
              <>
                Don't have an account?{" "}
                <button
                  type="button"
                  onClick={() => {
                    setMode("register");
                    setError("");
                  }}
                >
                  Create one
                </button>
              </>
            ) : (
              <>
                Already have an account?{" "}
                <button
                  type="button"
                  onClick={() => {
                    setMode("login");
                    setError("");
                  }}
                >
                  Sign in
                </button>
              </>
            )}
          </div>

          <div className="auth-security-note">
            <ShieldCheck size={16} />
            <span>Your dependencies are scoped to your account.</span>
          </div>
        </div>
      </section>
    </main>
  );
}

function App() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [dependencies, setDependencies] = useState<ApiDependency[]>([]);
  const [selectedDependency, setSelectedDependency] =
    useState<ApiDependency | null>(null);
  const [changes, setChanges] = useState<ApiChange[]>([]);

  const [initializing, setInitializing] = useState(true);
  const [loadingDependencies, setLoadingDependencies] = useState(false);
  const [loadingChanges, setLoadingChanges] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [pageError, setPageError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [busyDependencyIds, setBusyDependencyIds] = useState<string[]>([]);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [providerName, setProviderName] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [description, setDescription] = useState("");
  const [savingDependency, setSavingDependency] = useState(false);

  const loadDependencies = useCallback(async () => {
    setLoadingDependencies(true);
    setPageError("");

    try {
      const result = await api.listDependencies();
      setDependencies(result);

      setSelectedDependency((current) => {
        if (!current) return null;
        return result.find((item) => item.id === current.id) ?? null;
      });
    } catch (error) {
      setPageError(getErrorMessage(error));
      throw error;
    } finally {
      setLoadingDependencies(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function initialize() {
      if (!sessionStorage.getItem("api-radar-auth")) {
        setInitializing(false);
        return;
      }

      try {
        const currentUser = await api.getCurrentUser();

        if (cancelled) return;

        setUser(currentUser);

        const result = await api.listDependencies();

        if (!cancelled) {
          setDependencies(result);
        }
      } catch {
        if (!cancelled) {
          clearCredentials();
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setInitializing(false);
        }
      }
    }

    void initialize();

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleAuthenticated(currentUser: UserProfile) {
    setUser(currentUser);
    setPageError("");
    setSuccessMessage("");

    try {
      await loadDependencies();
    } catch {
      // The dashboard remains available so the user can retry loading.
    }
  }

  function handleLogout() {
    clearCredentials();
    setUser(null);
    setDependencies([]);
    setSelectedDependency(null);
    setChanges([]);
    setPageError("");
    setSuccessMessage("");
  }

  async function handleCreateDependency(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPageError("");
    setSuccessMessage("");

    setSavingDependency(true);

    try {
      const created = await api.createDependency({
        name: name.trim(),
        providerName: providerName.trim(),
        sourceUrl: sourceUrl.trim(),
        description: description.trim(),
      });

      setDependencies((current) => [
        created,
        ...current.filter((item) => item.id !== created.id),
      ]);

      setName("");
      setProviderName("");
      setSourceUrl("");
      setDescription("");
      setShowAddForm(false);
      setSelectedDependency(created);
      setChanges([]);

      setSuccessMessage(
        "API dependency registered successfully. Its first check is complete.",
      );
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setSavingDependency(false);
    }
  }

  async function handleSelectDependency(dependency: ApiDependency) {
    setSelectedDependency(dependency);
    setChanges([]);
    setPageError("");
    setLoadingChanges(true);

    try {
      const [details, dependencyChanges] = await Promise.all([
        api.getDependency(dependency.id),
        api.getChanges(dependency.id),
      ]);

      setSelectedDependency(details);
      setDependencies((current) =>
        current.map((item) => (item.id === details.id ? details : item)),
      );
      setChanges(dependencyChanges);
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setLoadingChanges(false);
    }
  }

  async function handleCheck(dependency: ApiDependency) {
    setPageError("");
    setSuccessMessage("");

    setBusyDependencyIds((current) => [...current, dependency.id]);

    try {
      const result = await api.checkDependency(dependency.id);

      const [updatedDependency, updatedChanges] = await Promise.all([
        api.getDependency(dependency.id),
        api.getChanges(dependency.id),
      ]);

      setDependencies((current) =>
        current.map((item) =>
          item.id === updatedDependency.id ? updatedDependency : item,
        ),
      );

      if (selectedDependency?.id === dependency.id) {
        setSelectedDependency(updatedDependency);
        setChanges(updatedChanges);
      }

      setSuccessMessage(
        `Check completed: ${result.changesDetected} change(s) detected, including ${result.breakingChangesDetected} breaking change(s).`,
      );
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setBusyDependencyIds((current) =>
        current.filter((id) => id !== dependency.id),
      );
    }
  }

  async function handleDelete(dependency: ApiDependency) {
    const confirmed = window.confirm(
      `Delete "${dependency.name}" and its stored change history? This action cannot be undone.`,
    );

    if (!confirmed) return;

    setDeletingId(dependency.id);
    setPageError("");
    setSuccessMessage("");

    try {
      await api.deleteDependency(dependency.id);

      setDependencies((current) =>
        current.filter((item) => item.id !== dependency.id),
      );

      if (selectedDependency?.id === dependency.id) {
        setSelectedDependency(null);
        setChanges([]);
      }

      setSuccessMessage(`"${dependency.name}" was deleted.`);
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setDeletingId(null);
    }
  }

  const filteredDependencies = dependencies.filter((dependency) => {
    const query = searchTerm.toLowerCase();

    return (
      dependency.name.toLowerCase().includes(query) ||
      (dependency.providerName ?? "").toLowerCase().includes(query) ||
      dependency.sourceUrl.toLowerCase().includes(query)
    );
  });

  const breakingTotal = dependencies.reduce(
    (total, dependency) => total + dependency.breakingChangeCount,
    0,
  );

  const changeTotal = dependencies.reduce(
    (total, dependency) => total + dependency.changeCount,
    0,
  );

  if (initializing) {
    return (
      <main className="app-loading">
        <LoaderCircle size={30} className="spin" />
        <span>Loading your workspace...</span>
      </main>
    );
  }

  if (!user) {
    return <AuthScreen onAuthenticated={handleAuthenticated} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-lockup sidebar-brand">
          <div className="brand-icon">
            <Activity size={22} />
          </div>
          <span>API Dependency Radar</span>
        </div>

        <div className="workspace-label">WORKSPACE</div>

        <button className="nav-item nav-item-active" type="button">
          <LayoutDashboard size={18} />
          <span>Overview</span>
        </button>

        <button
          className="nav-item"
          type="button"
          onClick={() => {
            setSelectedDependency(null);
            setShowAddForm(true);
          }}
        >
          <Code2 size={18} />
          <span>API dependencies</span>
          <span className="nav-count">{dependencies.length}</span>
        </button>

        <div className="sidebar-bottom">
          <div className="sidebar-status">
            <span className="sidebar-status-dot" />
            <div>
              <strong>Workspace connected</strong>
              <span>Authenticated session</span>
            </div>
          </div>

          <button className="profile-button" type="button" onClick={handleLogout}>
            <div className="avatar">
              {user.email.charAt(0).toUpperCase()}
            </div>
            <div className="profile-text">
              <strong>{user.email}</strong>
              <span>Sign out</span>
            </div>
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <ChevronRight size={15} />
            <strong>
              {selectedDependency ? selectedDependency.name : "Overview"}
            </strong>
          </div>

          <div className="topbar-right">
            <span className="environment-badge">
              <span />
              Live data
            </span>
            <div className="avatar avatar-small">
              {user.email.charAt(0).toUpperCase()}
            </div>
          </div>
        </header>

        <div className="page-content">
          <section className="page-heading">
            <div>
              <div className="eyebrow">DEPENDENCY INTELLIGENCE</div>
              <h1>
                {selectedDependency ? selectedDependency.name : "Overview"}
              </h1>
              <p>
                {selectedDependency
                  ? "Inspect the API specification and review detected changes."
                  : "Monitor your API ecosystem and stay ahead of breaking changes."}
              </p>
            </div>

            <div className="heading-actions">
              <button
                className="button button-secondary"
                type="button"
                onClick={() => void loadDependencies().catch(() => undefined)}
                disabled={loadingDependencies}
              >
                <RefreshCw
                  size={16}
                  className={loadingDependencies ? "spin" : ""}
                />
                Refresh
              </button>

              <button
                className="button button-primary"
                type="button"
                onClick={() => {
                  setSelectedDependency(null);
                  setShowAddForm(true);
                }}
              >
                <Plus size={17} />
                Add dependency
              </button>
            </div>
          </section>

          {pageError && (
            <div className="notice notice-error" role="alert">
              <AlertTriangle size={18} />
              <span>{pageError}</span>
              <button
                type="button"
                aria-label="Dismiss error"
                onClick={() => setPageError("")}
              >
                <X size={17} />
              </button>
            </div>
          )}

          {successMessage && (
            <div className="notice notice-success" role="status">
              <CheckCircle2 size={18} />
              <span>{successMessage}</span>
              <button
                type="button"
                aria-label="Dismiss message"
                onClick={() => setSuccessMessage("")}
              >
                <X size={17} />
              </button>
            </div>
          )}

          {!selectedDependency && (
            <>
              <section className="metrics-grid">
                <article className="metric-card">
                  <div className="metric-top">
                    <span>Total dependencies</span>
                    <div className="metric-icon metric-icon-blue">
                      <Globe2 size={19} />
                    </div>
                  </div>
                  <div className="metric-value">{dependencies.length}</div>
                  <div className="metric-caption">
                    Registered OpenAPI specifications
                  </div>
                </article>

                <article className="metric-card">
                  <div className="metric-top">
                    <span>Changes detected</span>
                    <div className="metric-icon metric-icon-purple">
                      <GitCompareArrows size={19} />
                    </div>
                  </div>
                  <div className="metric-value">{changeTotal}</div>
                  <div className="metric-caption">
                    Changes recorded across dependencies
                  </div>
                </article>

                <article className="metric-card">
                  <div className="metric-top">
                    <span>Breaking changes</span>
                    <div className="metric-icon metric-icon-red">
                      <AlertTriangle size={19} />
                    </div>
                  </div>
                  <div className="metric-value">{breakingTotal}</div>
                  <div className="metric-caption">
                    Recorded breaking changes
                  </div>
                </article>

                <article className="metric-card">
                  <div className="metric-top">
                    <span>Latest activity</span>
                    <div className="metric-icon metric-icon-green">
                      <Clock3 size={19} />
                    </div>
                  </div>
                  <div className="metric-value metric-value-small">
                    {dependencies.some((item) => item.lastCheckedAt)
                      ? formatDate(
                        dependencies
                          .map((item) => item.lastCheckedAt)
                          .filter((date): date is string => Boolean(date))
                          .sort()
                          .at(-1) ?? null,
                      )
                      : "No checks"}
                  </div>
                  <div className="metric-caption">
                    Most recent dependency check
                  </div>
                </article>
              </section>

              <section className="panel dependencies-panel">
                <div className="panel-header">
                  <div>
                    <h2>API dependencies</h2>
                    <p>
                      Your registered API specifications and their current
                      status.
                    </p>
                  </div>

                  <div className="search-box">
                    <Search size={17} />
                    <input
                      type="search"
                      placeholder="Search dependencies..."
                      value={searchTerm}
                      onChange={(event) => setSearchTerm(event.target.value)}
                      aria-label="Search API dependencies"
                    />
                  </div>
                </div>

                {loadingDependencies ? (
                  <div className="center-state">
                    <LoaderCircle size={25} className="spin" />
                    <span>Loading dependencies...</span>
                  </div>
                ) : filteredDependencies.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-icon">
                      <FilePlus2 size={27} />
                    </div>
                    <h3>
                      {searchTerm
                        ? "No matching dependencies"
                        : "Your API radar is ready"}
                    </h3>
                    <p>
                      {searchTerm
                        ? "Try another search term."
                        : "Register your first OpenAPI specification to start tracking API changes."}
                    </p>
                    {!searchTerm && (
                      <button
                        className="button button-primary"
                        type="button"
                        onClick={() => setShowAddForm(true)}
                      >
                        <Plus size={17} />
                        Register your first API
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="table-scroll">
                    <table className="dependency-table">
                      <thead>
                        <tr>
                          <th>Dependency</th>
                          <th>Status</th>
                          <th>Changes</th>
                          <th>Last checked</th>
                          <th aria-label="Actions" />
                        </tr>
                      </thead>
                      <tbody>
                        {filteredDependencies.map((dependency) => {
                          const checking = busyDependencyIds.includes(
                            dependency.id,
                          );

                          return (
                            <tr key={dependency.id}>
                              <td>
                                <button
                                  type="button"
                                  className="dependency-name-button"
                                  onClick={() =>
                                    void handleSelectDependency(dependency)
                                  }
                                >
                                  <div className="dependency-icon">
                                    <Code2 size={18} />
                                  </div>
                                  <div className="dependency-name-content">
                                    <strong>{dependency.name}</strong>
                                    <span>
                                      {dependency.providerName ||
                                        dependency.sourceType}
                                    </span>
                                    <small title={dependency.sourceUrl}>
                                      {dependency.sourceUrl}
                                    </small>
                                  </div>
                                </button>
                              </td>

                              <td>
                                <StatusBadge status={dependency.status} />
                              </td>

                              <td>
                                <div className="change-counts">
                                  <span>
                                    {dependency.changeCount} total
                                  </span>
                                  {dependency.breakingChangeCount > 0 && (
                                    <span className="breaking-count">
                                      <AlertTriangle size={13} />
                                      {dependency.breakingChangeCount} breaking
                                    </span>
                                  )}
                                </div>
                              </td>

                              <td>
                                <span className="date-cell">
                                  {formatDate(dependency.lastCheckedAt)}
                                </span>
                              </td>

                              <td>
                                <div className="row-actions">
                                  <button
                                    className="icon-button"
                                    type="button"
                                    title="Check API now"
                                    aria-label={`Check ${dependency.name} now`}
                                    disabled={checking}
                                    onClick={() =>
                                      void handleCheck(dependency)
                                    }
                                  >
                                    {checking ? (
                                      <LoaderCircle
                                        size={17}
                                        className="spin"
                                      />
                                    ) : (
                                      <RefreshCw size={17} />
                                    )}
                                  </button>

                                  <button
                                    className="icon-button icon-button-danger"
                                    type="button"
                                    title="Delete dependency"
                                    aria-label={`Delete ${dependency.name}`}
                                    disabled={deletingId === dependency.id}
                                    onClick={() =>
                                      void handleDelete(dependency)
                                    }
                                  >
                                    {deletingId === dependency.id ? (
                                      <LoaderCircle
                                        size={17}
                                        className="spin"
                                      />
                                    ) : (
                                      <Trash2 size={17} />
                                    )}
                                  </button>

                                  <button
                                    className="icon-button"
                                    type="button"
                                    title="View details"
                                    aria-label={`View ${dependency.name} details`}
                                    onClick={() =>
                                      void handleSelectDependency(dependency)
                                    }
                                  >
                                    <ChevronRight size={18} />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}

                <div className="panel-footer">
                  <span>
                    Showing {filteredDependencies.length} of{" "}
                    {dependencies.length} dependencies
                  </span>
                  <span className="footer-secure">
                    <ShieldCheck size={15} />
                    Account-scoped data
                  </span>
                </div>
              </section>
            </>
          )}

          {selectedDependency && (
            <section className="detail-grid">
              <div className="panel detail-main">
                <div className="panel-header">
                  <div>
                    <div className="eyebrow">SPECIFICATION DETAILS</div>
                    <h2>{selectedDependency.name}</h2>
                    <p>
                      {selectedDependency.description ||
                        "No description has been provided."}
                    </p>
                  </div>
                  <button
                    className="button button-secondary"
                    type="button"
                    onClick={() =>
                      void handleCheck(selectedDependency)
                    }
                    disabled={busyDependencyIds.includes(
                      selectedDependency.id,
                    )}
                  >
                    <RefreshCw
                      size={16}
                      className={
                        busyDependencyIds.includes(selectedDependency.id)
                          ? "spin"
                          : ""
                      }
                    />
                    Check now
                  </button>
                </div>

                <div className="detail-properties">
                  <div>
                    <span>Provider</span>
                    <strong>
                      {selectedDependency.providerName || "Not specified"}
                    </strong>
                  </div>
                  <div>
                    <span>Source type</span>
                    <strong>{selectedDependency.sourceType}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>
                      <StatusBadge status={selectedDependency.status} />
                    </strong>
                  </div>
                  <div>
                    <span>Last checked</span>
                    <strong>
                      {formatDate(selectedDependency.lastCheckedAt)}
                    </strong>
                  </div>
                  <div>
                    <span>Total changes</span>
                    <strong>{selectedDependency.changeCount}</strong>
                  </div>
                  <div>
                    <span>Breaking changes</span>
                    <strong className="breaking-text">
                      {selectedDependency.breakingChangeCount}
                    </strong>
                  </div>
                </div>

                <div className="source-url-block">
                  <div>
                    <Globe2 size={18} />
                    <span>OpenAPI source URL</span>
                  </div>
                  <a
                    href={selectedDependency.sourceUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {selectedDependency.sourceUrl}
                    <ExternalLink size={15} />
                  </a>
                </div>

                <div className="changes-heading">
                  <div>
                    <h3>Detected changes</h3>
                    <p>Changes recorded for this dependency.</p>
                  </div>
                  <span className="count-pill">{changes.length}</span>
                </div>

                {loadingChanges ? (
                  <div className="center-state">
                    <LoaderCircle size={23} className="spin" />
                    <span>Loading change history...</span>
                  </div>
                ) : changes.length === 0 ? (
                  <div className="empty-state compact-empty">
                    <div className="empty-icon">
                      <GitCompareArrows size={24} />
                    </div>
                    <h3>No changes recorded</h3>
                    <p>
                      Changes will appear here when the backend detects
                      differences in the API specification.
                    </p>
                  </div>
                ) : (
                  <div className="change-list">
                    {changes.map((change) => (
                      <article className="change-item" key={change.id}>
                        <div
                          className={`change-item-icon ${change.breaking
                            ? "change-item-icon-danger"
                            : "change-item-icon-normal"
                            }`}
                        >
                          {change.breaking ? (
                            <AlertTriangle size={18} />
                          ) : (
                            <GitCompareArrows size={18} />
                          )}
                        </div>
                        <div className="change-item-content">
                          <div className="change-title-row">
                            <strong>{change.type}</strong>
                            {change.breaking && (
                              <span className="breaking-label">
                                Breaking
                              </span>
                            )}
                          </div>
                          <p>{change.description}</p>
                          {change.endpoint && (
                            <code>{change.endpoint}</code>
                          )}
                          <span className="change-date">
                            {formatDate(change.detectedAt)}
                          </span>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </div>

              <aside className="detail-side">
                <div className="panel quick-info-panel">
                  <h3>Dependency summary</h3>
                  <div className="quick-info-row">
                    <span>
                      <ArrowUpRight size={16} />
                      Total changes
                    </span>
                    <strong>{selectedDependency.changeCount}</strong>
                  </div>
                  <div className="quick-info-row">
                    <span>
                      <ArrowDownRight size={16} />
                      Breaking changes
                    </span>
                    <strong className="breaking-text">
                      {selectedDependency.breakingChangeCount}
                    </strong>
                  </div>
                  <div className="quick-info-row">
                    <span>
                      <Clock3 size={16} />
                      Registered
                    </span>
                    <strong>
                      {new Intl.DateTimeFormat(undefined, {
                        dateStyle: "medium",
                      }).format(new Date(selectedDependency.createdAt))}
                    </strong>
                  </div>
                </div>

                <div className="info-callout">
                  <CircleHelp size={19} />
                  <div>
                    <strong>How checks work</strong>
                    <p>
                      A check fetches the current specification, compares it
                      with the saved baseline, and records detected changes.
                    </p>
                  </div>
                </div>

                <button
                  className="button button-secondary button-full"
                  type="button"
                  onClick={() => {
                    setSelectedDependency(null);
                    setChanges([]);
                  }}
                >
                  Back to overview
                </button>
              </aside>
            </section>
          )}

          <footer className="page-footer">
            <span>API Dependency Radar</span>
            <span>
              <ShieldCheck size={14} />
              Your dependencies. Your workspace.
            </span>
          </footer>
        </div>
      </main>

      {showAddForm && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setShowAddForm(false);
            }
          }}
        >
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-dependency-title"
          >
            <div className="modal-heading">
              <div className="modal-title-icon">
                <FilePlus2 size={21} />
              </div>
              <button
                className="icon-button"
                type="button"
                aria-label="Close registration form"
                onClick={() => setShowAddForm(false)}
              >
                <X size={19} />
              </button>
            </div>

            <h2 id="add-dependency-title">Register an API dependency</h2>
            <p className="modal-description">
              Add an OpenAPI specification URL to begin tracking changes.
            </p>

            <form className="dependency-form" onSubmit={handleCreateDependency}>
              <label htmlFor="dependency-name">Dependency name *</label>
              <input
                id="dependency-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Payments API"
                maxLength={120}
                required
              />

              <label htmlFor="provider-name">Provider name</label>
              <input
                id="provider-name"
                value={providerName}
                onChange={(event) => setProviderName(event.target.value)}
                placeholder="Acme Inc."
                maxLength={120}
              />

              <label htmlFor="source-url">OpenAPI specification URL *</label>
              <input
                id="source-url"
                type="url"
                value={sourceUrl}
                onChange={(event) => setSourceUrl(event.target.value)}
                placeholder="https://api.example.com/openapi.json"
                maxLength={2048}
                required
              />
              <p className="field-hint">
                Enter the URL of the actual OpenAPI document, not just the
                API's homepage.
              </p>

              <label htmlFor="dependency-description">Description</label>
              <textarea
                id="dependency-description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="What does your application use this API for?"
                maxLength={1000}
                rows={3}
              />

              <div className="form-note">
                <ShieldCheck size={17} />
                <span>
                  The server validates the source URL and specification before
                  saving the dependency.
                </span>
              </div>

              {pageError && (
                <div className="inline-error" role="alert">
                  <AlertTriangle size={17} />
                  <span>{pageError}</span>
                </div>
              )}

              <div className="modal-actions">
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => setShowAddForm(false)}
                  disabled={savingDependency}
                >
                  Cancel
                </button>
                <button
                  className="button button-primary"
                  type="submit"
                  disabled={savingDependency}
                >
                  {savingDependency ? (
                    <LoaderCircle size={17} className="spin" />
                  ) : (
                    <Plus size={17} />
                  )}
                  {savingDependency ? "Registering..." : "Register API"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}

export default App;