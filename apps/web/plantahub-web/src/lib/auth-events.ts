export const AUTH_SESSION_EXPIRED_EVENT = 'auth:session-expired';

let sessionExpiredDispatched = false;

export function dispatchSessionExpiredEvent() {
  if (sessionExpiredDispatched) return;

  sessionExpiredDispatched = true;
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_EXPIRED_EVENT));
}

export function resetSessionExpiredFlag() {
  sessionExpiredDispatched = false;
}