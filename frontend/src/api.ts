const API_BASE_URL = (
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const AUTH_STORAGE_KEY = "api-radar-auth";

export interface UserProfile {
    id: string;
    email: string;
    createdAt: string;
}

export interface ApiDependency {
    id: string;
    name: string;
    providerName: string | null;
    sourceUrl: string;
    sourceType: string;
    description: string | null;
    status: string;
    lastCheckedAt: string | null;
    createdAt: string;
    updatedAt: string;
    changeCount: number;
    breakingChangeCount: number;
}

export interface ApiChange {
    id: string;
    dependencyId: string;
    sourceType: string;
    type: string;
    endpoint: string;
    description: string;
    breaking: boolean;
    detectedAt: string;
}

export interface CheckResult {
    dependencyId: string;
    checkedAt: string;
    changesDetected: number;
    breakingChangesDetected: number;
    changes: ApiChange[];
}

export interface CreateDependencyRequest {
    name: string;
    providerName: string;
    sourceUrl: string;
    description: string;
}

interface ApiErrorBody {
    message?: string;
    error?: string;
    details?: string[];
}

function getAuthHeader(): string | null {
    return sessionStorage.getItem(AUTH_STORAGE_KEY);
}

export function setCredentials(email: string, password: string): void {
    const bytes = new TextEncoder().encode(`${email}:${password}`);
    let binary = "";

    bytes.forEach((byte) => {
        binary += String.fromCharCode(byte);
    });

    sessionStorage.setItem(AUTH_STORAGE_KEY, `Basic ${btoa(binary)}`);
}

export function clearCredentials(): void {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

async function request<T>(
    path: string,
    options: RequestInit = {},
    authenticated = true,
): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");

    if (options.body) {
        headers.set("Content-Type", "application/json");
    }

    if (authenticated) {
        const authorization = getAuthHeader();

        if (!authorization) {
            throw new Error("Please sign in to continue.");
        }

        headers.set("Authorization", authorization);
    }

    let response: Response;

    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers,
        });
    } catch {
        throw new Error(
            `Cannot connect to the API at ${API_BASE_URL}. Check that the backend is running and the API URL is correct.`,
        );
    }

    if (response.status === 401) {
        if (authenticated) {
            clearCredentials();
        }

        throw new Error(
            authenticated
                ? "Your session has expired or your credentials are invalid. Please sign in again."
                : "Invalid email or password.",
        );
    }

    if (!response.ok) {
        let message = `Request failed with status ${response.status}.`;

        try {
            const body = (await response.json()) as ApiErrorBody;

            if (body.message) {
                message = body.message;
            } else if (body.details?.length) {
                message = body.details.join(" ");
            } else if (body.error) {
                message = body.error;
            }
        } catch {
            // The response may not contain a JSON error body.
        }

        throw new Error(message);
    }

    if (response.status === 204 || response.status === 201 && !response.headers.get("content-type")) {
        return undefined as T;
    }

    if (response.status === 204) {
        return undefined as T;
    }

    const contentType = response.headers.get("content-type") || "";

    if (!contentType.includes("application/json")) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

export const api = {
    async registerAccount(email: string, password: string): Promise<void> {
        await request<void>(
            "/api/auth/register",
            {
                method: "POST",
                body: JSON.stringify({ email, password }),
            },
            false,
        );
    },

    getCurrentUser(): Promise<UserProfile> {
        return request<UserProfile>("/api/auth/me");
    },

    listDependencies(): Promise<ApiDependency[]> {
        return request<ApiDependency[]>("/api/dependencies");
    },

    createDependency(
        input: CreateDependencyRequest,
    ): Promise<ApiDependency> {
        return request<ApiDependency>("/api/dependencies", {
            method: "POST",
            body: JSON.stringify(input),
        });
    },

    getDependency(id: string): Promise<ApiDependency> {
        return request<ApiDependency>(
            `/api/dependencies/${encodeURIComponent(id)}`,
        );
    },

    getChanges(id: string): Promise<ApiChange[]> {
        return request<ApiChange[]>(
            `/api/dependencies/${encodeURIComponent(id)}/changes`,
        );
    },

    checkDependency(id: string): Promise<CheckResult> {
        return request<CheckResult>(
            `/api/dependencies/${encodeURIComponent(id)}/check`,
            { method: "POST" },
        );
    },

    async deleteDependency(id: string): Promise<void> {
        await request<void>(
            `/api/dependencies/${encodeURIComponent(id)}`,
            { method: "DELETE" },
        );
    },
};