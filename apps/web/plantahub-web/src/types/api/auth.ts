export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken?: string | null;
  tokenType?: string | null;
  fullName?: string | null;
  email?: string | null;
  /** "USER" ou "ADMIN". Devolvido tanto no /login quanto no /me. */
  role?: string | null;
};

export type RegisterRequest = {
  email: string;
  password: string;
  fullName: string;
  /** Opcional. Só serve para recuperar a senha por SMS. */
  phoneNumber?: string;
};

export type PasswordResetChannel = 'EMAIL' | 'SMS';

/** Canais que a tela pode oferecer. O SMS some quando não há provedor configurado. */
export type PasswordResetChannels = {
  email: boolean;
  sms: boolean;
};

export type PasswordResetRequest = {
  email: string;
  channel: PasswordResetChannel;
};

export type PasswordResetVerifyRequest = {
  email: string;
  code: string;
};

export type PasswordResetVerifyResponse = {
  resetToken: string;
};

export type PasswordResetConfirmRequest = {
  resetToken: string;
  newPassword: string;
};
