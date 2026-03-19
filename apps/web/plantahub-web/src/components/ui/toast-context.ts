import { createContext } from 'react';

type ToastVariant = 'success' | 'error' | 'info';

export type ToastItem = {
  id: string;
  title: string;
  description?: string;
  variant: ToastVariant;
};

export type ShowToastInput = Omit<ToastItem, 'id'>;

export type ToastContextValue = {
  showToast: (toast: ShowToastInput) => void;
};

export const ToastContext = createContext<ToastContextValue | undefined>(undefined);