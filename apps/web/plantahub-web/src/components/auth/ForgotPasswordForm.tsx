import { KeyRound, Lock, Mail, MessageSquare } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../../lib/api-error';
import {
  confirmPasswordReset,
  getPasswordResetChannels,
  requestPasswordReset,
  verifyPasswordResetCode,
} from '../../services/auth.service';
import type { PasswordResetChannel } from '../../types/api/auth';
import { useToast } from '../ui/use-toast';

type Step = 'request' | 'verify' | 'reset';

const RESEND_COOLDOWN_SECONDS = 60;

const ERROR_MESSAGES: Record<string, string> = {
  invalid_or_expired_code: 'Código inválido ou expirado. Confira os números ou peça um novo código.',
  invalid_or_expired_token: 'O prazo para definir a nova senha acabou. Peça um novo código.',
  too_many_requests: 'Muitas tentativas seguidas. Aguarde um pouco e tente de novo.',
  channel_unavailable: 'O envio por SMS está indisponível no momento. Use o e-mail.',
};

function errorMessage(error: unknown, fallback: string) {
  const message = getApiErrorMessage(error, fallback);
  return ERROR_MESSAGES[message] ?? message;
}

function errorCode(error: unknown) {
  return getApiErrorMessage(error, '');
}

export default function ForgotPasswordForm() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [step, setStep] = useState<Step>('request');
  const [smsAvailable, setSmsAvailable] = useState(false);

  const [email, setEmail] = useState('');
  const [channel, setChannel] = useState<PasswordResetChannel>('EMAIL');
  const [code, setCode] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    let active = true;

    // Se a consulta falhar, fica só o e-mail: oferecer um SMS que não chega seria pior.
    getPasswordResetChannels()
      .then(channels => {
        if (active) setSmsAvailable(channels.sms);
      })
      .catch(() => undefined);

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setTimeout(() => setCooldown(value => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  async function sendCode() {
    await requestPasswordReset({ email: email.trim(), channel });
    setCooldown(RESEND_COOLDOWN_SECONDS);
  }

  async function handleRequest(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    try {
      setLoading(true);
      setError(null);
      await sendCode();
      setCode('');
      setStep('verify');
    } catch (err) {
      setError(errorMessage(err, 'Não foi possível enviar o código. Tente novamente.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    try {
      setLoading(true);
      setError(null);
      await sendCode();
      showToast({ variant: 'success', title: 'Enviamos um novo código' });
    } catch (err) {
      setError(errorMessage(err, 'Não foi possível reenviar o código.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleVerify(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    try {
      setLoading(true);
      setError(null);
      const response = await verifyPasswordResetCode({ email: email.trim(), code });
      setResetToken(response.resetToken);
      setStep('reset');
    } catch (err) {
      setError(errorMessage(err, 'Não foi possível validar o código.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleReset(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (password.length < 8) {
      setError('A senha precisa ter pelo menos 8 caracteres.');
      return;
    }

    if (password !== confirmPassword) {
      setError('As senhas não coincidem.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await confirmPasswordReset({ resetToken, newPassword: password });
      showToast({
        variant: 'success',
        title: 'Senha alterada',
        description: 'Entre com a sua nova senha.',
      });
      navigate('/login', { replace: true });
    } catch (err) {
      if (errorCode(err) === 'invalid_or_expired_token') {
        setResetToken('');
        setPassword('');
        setConfirmPassword('');
        setStep('request');
      }
      setError(errorMessage(err, 'Não foi possível alterar a senha.'));
    } finally {
      setLoading(false);
    }
  }

  function backToRequest() {
    setError(null);
    setCode('');
    setStep('request');
  }

  const inputBox =
    'flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500';
  const inputClass =
    'w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400';
  const submitClass =
    'w-full rounded-xl bg-primary-500 px-4 py-3 text-sm font-semibold text-white hover:bg-primary-600 transition disabled:cursor-not-allowed disabled:opacity-70';

  return (
    <div className="w-full max-w-md rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="text-center">
        <h2 className="text-2xl font-extrabold text-neutral-900">
          {step === 'reset' ? 'Nova senha' : 'Recuperar senha'}
        </h2>
        <p className="mt-2 text-sm text-neutral-600">
          {step === 'request' && 'Informe o e-mail da sua conta e escolha como receber o código.'}
          {step === 'verify' &&
            (channel === 'EMAIL'
              ? `Se houver uma conta com ${email.trim()}, enviamos um código de 6 dígitos para esse e-mail.`
              : `Se houver uma conta com ${email.trim()} e um celular cadastrado, enviamos um código de 6 dígitos por SMS.`)}
          {step === 'reset' && 'Escolha uma senha nova para a sua conta.'}
        </p>
      </div>

      {step === 'request' ? (
        <form onSubmit={handleRequest} className="mt-6 space-y-4">
          <div>
            <label htmlFor="email" className="mb-2 block text-sm font-semibold text-neutral-800">
              E-mail
            </label>
            <div className={inputBox}>
              <Mail className="h-5 w-5 text-neutral-400" />
              <input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="seuemail@exemplo.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className={inputClass}
                required
              />
            </div>
          </div>

          {smsAvailable ? (
            <fieldset>
              <legend className="mb-2 block text-sm font-semibold text-neutral-800">
                Receber o código por
              </legend>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <ChannelOption
                  value="EMAIL"
                  selected={channel === 'EMAIL'}
                  onSelect={setChannel}
                  icon={<Mail className="h-5 w-5" />}
                  title="E-mail"
                  description="No e-mail da conta"
                />
                <ChannelOption
                  value="SMS"
                  selected={channel === 'SMS'}
                  onSelect={setChannel}
                  icon={<MessageSquare className="h-5 w-5" />}
                  title="SMS"
                  description="No celular cadastrado"
                />
              </div>
            </fieldset>
          ) : null}

          {error ? <p className="text-sm font-medium text-red-600">{error}</p> : null}

          <button type="submit" disabled={loading} className={submitClass}>
            {loading ? 'Enviando...' : 'Enviar código'}
          </button>
        </form>
      ) : null}

      {step === 'verify' ? (
        <form onSubmit={handleVerify} className="mt-6 space-y-4">
          <div>
            <label htmlFor="code" className="mb-2 block text-sm font-semibold text-neutral-800">
              Código
            </label>
            <div className={inputBox}>
              <KeyRound className="h-5 w-5 text-neutral-400" />
              <input
                id="code"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                pattern="\d{6}"
                maxLength={6}
                placeholder="000000"
                value={code}
                onChange={e => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                className={`${inputClass} tracking-[0.4em]`}
                required
              />
            </div>
          </div>

          {error ? <p className="text-sm font-medium text-red-600">{error}</p> : null}

          <button type="submit" disabled={loading || code.length !== 6} className={submitClass}>
            {loading ? 'Validando...' : 'Validar código'}
          </button>

          <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <button
              type="button"
              onClick={handleResend}
              disabled={loading || cooldown > 0}
              className="font-medium text-primary-600 hover:text-primary-700 disabled:cursor-not-allowed disabled:text-neutral-400"
            >
              {cooldown > 0 ? `Reenviar código em ${cooldown}s` : 'Reenviar código'}
            </button>
            <button
              type="button"
              onClick={backToRequest}
              className="font-medium text-neutral-600 hover:text-neutral-800"
            >
              {smsAvailable ? 'Trocar e-mail ou forma de envio' : 'Trocar e-mail'}
            </button>
          </div>
        </form>
      ) : null}

      {step === 'reset' ? (
        <form onSubmit={handleReset} className="mt-6 space-y-4">
          <div>
            <label htmlFor="newPassword" className="mb-2 block text-sm font-semibold text-neutral-800">
              Nova senha
            </label>
            <div className={inputBox}>
              <Lock className="h-5 w-5 text-neutral-400" />
              <input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                placeholder="Mínimo de 8 caracteres"
                minLength={8}
                maxLength={72}
                value={password}
                onChange={e => setPassword(e.target.value)}
                className={inputClass}
                required
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="confirmNewPassword"
              className="mb-2 block text-sm font-semibold text-neutral-800"
            >
              Confirmar nova senha
            </label>
            <div className={inputBox}>
              <Lock className="h-5 w-5 text-neutral-400" />
              <input
                id="confirmNewPassword"
                type="password"
                autoComplete="new-password"
                placeholder="Repita a nova senha"
                maxLength={72}
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                className={inputClass}
                required
              />
            </div>
          </div>

          {error ? <p className="text-sm font-medium text-red-600">{error}</p> : null}

          <button type="submit" disabled={loading} className={submitClass}>
            {loading ? 'Salvando...' : 'Salvar nova senha'}
          </button>
        </form>
      ) : null}

      <p className="mt-6 text-center text-sm text-neutral-600">
        Lembrou a senha?{' '}
        <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-700">
          Entrar
        </Link>
      </p>
    </div>
  );
}

type ChannelOptionProps = {
  value: PasswordResetChannel;
  selected: boolean;
  onSelect: (value: PasswordResetChannel) => void;
  icon: React.ReactNode;
  title: string;
  description: string;
};

function ChannelOption({ value, selected, onSelect, icon, title, description }: ChannelOptionProps) {
  return (
    <label
      className={`flex min-w-0 cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 transition focus-within:ring-2 focus-within:ring-primary-300 ${
        selected
          ? 'border-primary-500 bg-orange-50 text-primary-700'
          : 'border-neutral-300 bg-white text-neutral-700 hover:border-neutral-400'
      }`}
    >
      <input
        type="radio"
        name="channel"
        value={value}
        checked={selected}
        onChange={() => onSelect(value)}
        className="sr-only"
      />
      {icon}
      <span className="min-w-0">
        <span className="block text-sm font-semibold">{title}</span>
        <span className="block text-xs text-neutral-500">{description}</span>
      </span>
    </label>
  );
}
