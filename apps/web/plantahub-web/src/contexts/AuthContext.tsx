/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { AUTH_SESSION_EXPIRED_EVENT, resetSessionExpiredFlag } from '../lib/auth-events';
import {
  getCurrentAuthUser,
  loginRequest,
  logoutRequest,
  registerRequest,
} from '../services/auth.service';

export type UserRole = 'USER' | 'ADMIN';

type AuthUser = {
  fullName: string | null;
  firstName: string | null;
  email: string | null;
  role: UserRole | null;
};

type LoginPayload = {
  email: string;
  password: string;
};

type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

type AuthContextValue = {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: AuthUser | null;
  /**
   * Somente para a interface decidir o que mostrar. A autorizacao de verdade acontece no
   * servidor, em cada endpoint /v1/admin/**: qualquer um pode alterar isto no navegador.
   */
  isAdmin: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
};

const LEGACY_AUTH_KEYS = ['token', 'tokenType', 'userFullName', 'userEmail'];

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function getFirstName(fullName: string | null) {
  if (!fullName) return null;
  return fullName.trim().split(' ')[0] ?? null;
}

/** O Spring Security costuma emitir "ROLE_ADMIN"; a API devolve "ADMIN". Aceita os dois. */
function normalizeRole(value?: string | null): UserRole | null {
  if (!value) return null;
  return value.trim().toUpperCase().replace(/^ROLE_/, '') === 'ADMIN' ? 'ADMIN' : 'USER';
}

function toAuthUser(payload: {
  fullName?: string | null;
  email?: string | null;
  role?: string | null;
}): AuthUser {
  const fullName = payload.fullName ?? null;

  return {
    fullName,
    firstName: getFirstName(fullName),
    email: payload.email ?? null,
    role: normalizeRole(payload.role),
  };
}

function clearLegacyAuthStorage() {
  for (const key of LEGACY_AUTH_KEYS) {
    localStorage.removeItem(key);
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshSession = useCallback(async () => {
    try {
      const response = await getCurrentAuthUser();
      setUser(toAuthUser(response));
      resetSessionExpiredFlag();
    } catch {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    async function loadSession() {
      clearLegacyAuthStorage();
      await refreshSession();
      setIsLoading(false);
    }

    void loadSession();
  }, [refreshSession]);

  useEffect(() => {
    function handleSessionExpired() {
      const currentPath = window.location.pathname + window.location.search;
      const existingParams = new URLSearchParams(window.location.search);
      const redirect =
        window.location.pathname === '/login'
          ? existingParams.get('redirect') || '/'
          : currentPath;

      clearLegacyAuthStorage();
      setUser(null);

      navigate(`/login?expired=1&redirect=${encodeURIComponent(redirect)}`, { replace: true });
    }

    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);

    return () => {
      window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
    };
  }, [navigate]);

  const login = useCallback(async ({ email, password }: LoginPayload) => {
    const response = await loginRequest({ email, password });
    clearLegacyAuthStorage();
    resetSessionExpiredFlag();
    setUser(toAuthUser({ ...response, email: response.email ?? email }));
  }, []);

  const register = useCallback(async ({ fullName, email, password }: RegisterPayload) => {
    await registerRequest({ fullName, email, password });
    const response = await loginRequest({ email, password });
    clearLegacyAuthStorage();
    resetSessionExpiredFlag();
    setUser(toAuthUser({ ...response, fullName: response.fullName ?? fullName, email }));
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } finally {
      clearLegacyAuthStorage();
      resetSessionExpiredFlag();
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: !!user,
      isLoading,
      user,
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
      refreshSession,
    }),
    [user, isLoading, login, register, logout, refreshSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
}
