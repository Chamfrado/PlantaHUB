const TOKEN_KEY = 'token';
const TOKEN_TYPE_KEY = 'tokenType';

export function saveAuth(accessToken: string, tokenType: string) {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(TOKEN_TYPE_KEY, tokenType);
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getTokenType() {
  return localStorage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer';
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_TYPE_KEY);
}

export function isAuthenticated() {
  return !!getToken();
}