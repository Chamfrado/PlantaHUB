import { http } from '../lib/http';
import type {
  AuthResponse,
  LoginRequest,
  PasswordResetChannels,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  PasswordResetVerifyRequest,
  PasswordResetVerifyResponse,
  RegisterRequest,
} from '../types/api/auth';

export async function loginRequest(payload: LoginRequest): Promise<AuthResponse> {
  return http<AuthResponse>('/v1/auth/login', {
    method: 'POST',
    body: payload,
  });
}

export async function getCurrentAuthUser(): Promise<AuthResponse> {
  return http<AuthResponse>('/v1/auth/me');
}

export async function logoutRequest(): Promise<void> {
  return http<void>('/v1/auth/logout', {
    method: 'POST',
  });
}

export async function registerRequest(payload: RegisterRequest): Promise<void> {
  await http<void>('/v1/auth/register', {
    method: 'POST',
    body: payload,
  });
}

export async function getPasswordResetChannels(): Promise<PasswordResetChannels> {
  return http<PasswordResetChannels>('/v1/auth/password-reset/channels');
}

/** Responde igual exista a conta ou não: a tela nunca sabe se o e-mail está cadastrado. */
export async function requestPasswordReset(payload: PasswordResetRequest): Promise<void> {
  await http<void>('/v1/auth/password-reset/request', {
    method: 'POST',
    body: payload,
  });
}

export async function verifyPasswordResetCode(
  payload: PasswordResetVerifyRequest
): Promise<PasswordResetVerifyResponse> {
  return http<PasswordResetVerifyResponse>('/v1/auth/password-reset/verify', {
    method: 'POST',
    body: payload,
  });
}

export async function confirmPasswordReset(payload: PasswordResetConfirmRequest): Promise<void> {
  await http<void>('/v1/auth/password-reset/confirm', {
    method: 'POST',
    body: payload,
  });
}
