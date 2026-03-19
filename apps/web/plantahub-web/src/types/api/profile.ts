export type ProfileResponse = {
  email: string;
  fullName: string;
  cpf?: string | null;
  phoneNumber?: string | null;
  role?: string;
  cpfLocked: boolean;
  profileCompleted: boolean;
};

export type ProfileStatusResponse = {
  profileCompleted: boolean;
  cpfLocked: boolean;
  missingFields: string[];
};

export type UpdateProfileRequest = {
  fullName?: string;
  cpf?: string;
  phoneNumber?: string;
};

export type DeleteProfileRequest = {
  confirmationText: string;
};

export type DeleteProfileResponse = {
  message?: string;
  deleted?: boolean;
};