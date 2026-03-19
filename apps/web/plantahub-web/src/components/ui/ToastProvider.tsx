import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { useCallback, useMemo, useState, type ReactNode } from 'react';
import { ToastContext, type ShowToastInput, type ToastItem } from './toast-context';

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(item => item.id !== id));
  }, []);

  const showToast = useCallback(
    (toast: ShowToastInput) => {
      const id = crypto.randomUUID();

      setToasts(prev => [...prev, { ...toast, id }]);

      window.setTimeout(() => {
        removeToast(id);
      }, 4000);
    },
    [removeToast]
  );

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}

      <div className="pointer-events-none fixed right-4 top-4 z-100 flex w-full max-w-sm flex-col gap-3">
        {toasts.map(toast => (
          <ToastCard key={toast.id} toast={toast} onClose={() => removeToast(toast.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastCard({ toast, onClose }: { toast: ToastItem; onClose: () => void }) {
  const styles = {
    success: {
      icon: <CheckCircle2 className="h-5 w-5 text-emerald-600" />,
      box: 'border-emerald-200 bg-emerald-50',
      title: 'text-emerald-900',
      description: 'text-emerald-800',
    },
    error: {
      icon: <AlertCircle className="h-5 w-5 text-red-600" />,
      box: 'border-red-200 bg-red-50',
      title: 'text-red-900',
      description: 'text-red-800',
    },
    info: {
      icon: <Info className="h-5 w-5 text-primary-600" />,
      box: 'border-orange-200 bg-orange-50',
      title: 'text-neutral-900',
      description: 'text-neutral-700',
    },
  }[toast.variant];

  return (
    <div
      className={[
        'pointer-events-auto rounded-2xl border p-4 shadow-lg backdrop-blur',
        styles.box,
      ].join(' ')}
    >
      <div className="flex items-start gap-3">
        <div className="mt-0.5 shrink-0">{styles.icon}</div>

        <div className="min-w-0 flex-1">
          <div className={['font-extrabold', styles.title].join(' ')}>{toast.title}</div>
          {toast.description ? (
            <p className={['mt-1 text-sm leading-relaxed', styles.description].join(' ')}>
              {toast.description}
            </p>
          ) : null}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-1 text-neutral-500 transition hover:bg-white/70 hover:text-neutral-800"
          aria-label="Fechar notificação"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
