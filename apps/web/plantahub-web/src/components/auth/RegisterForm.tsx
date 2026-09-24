import { Lock, Mail, Phone, User } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getApiErrorMessage } from '../../lib/api-error';
import { formatPhoneInput, isValidPhone } from '../../lib/phone';

export default function RegisterForm() {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (password !== confirmPassword) {
      setError('As senhas não coincidem.');
      return;
    }

    if (phoneNumber && !isValidPhone(phoneNumber)) {
      setError('Informe o celular com DDD, por exemplo (11) 98888-7777.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      await register({
        fullName,
        email,
        password,
        phoneNumber: phoneNumber || undefined,
      });

      navigate('/');
    } catch (err) {
      console.error(err);
      const code = getApiErrorMessage(err, '');
      setError(
        code === 'email_already_in_use'
          ? 'Já existe uma conta com este e-mail.'
          : code === 'phone_invalid'
            ? 'Celular inválido. Informe o número com DDD.'
            : 'Não foi possível criar sua conta.'
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="w-full max-w-md rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="text-center">
        <h2 className="text-2xl font-extrabold text-neutral-900">Criar conta</h2>
        <p className="mt-2 text-sm text-neutral-600">Cadastre-se para continuar</p>
      </div>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label htmlFor="fullName" className="mb-2 block text-sm font-semibold text-neutral-800">
            Nome completo
          </label>

          <div className="flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500">
            <User className="h-5 w-5 text-neutral-400" />
            <input
              id="fullName"
              type="text"
              placeholder="Seu nome completo"
              value={fullName}
              onChange={e => setFullName(e.target.value)}
              className="w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400"
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="email" className="mb-2 block text-sm font-semibold text-neutral-800">
            E-mail
          </label>

          <div className="flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500">
            <Mail className="h-5 w-5 text-neutral-400" />
            <input
              id="email"
              type="email"
              placeholder="seuemail@exemplo.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400"
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="phoneNumber" className="mb-2 block text-sm font-semibold text-neutral-800">
            Celular <span className="font-normal text-neutral-500">(opcional)</span>
          </label>

          <div className="flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500">
            <Phone className="h-5 w-5 text-neutral-400" />
            <input
              id="phoneNumber"
              type="tel"
              inputMode="tel"
              autoComplete="tel-national"
              placeholder="(11) 98888-7777"
              value={phoneNumber}
              onChange={e => setPhoneNumber(formatPhoneInput(e.target.value))}
              aria-describedby="phoneNumber-help"
              className="w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400"
            />
          </div>
          <p id="phoneNumber-help" className="mt-1 text-xs text-neutral-500">
            Usado para recuperar sua senha por SMS.
          </p>
        </div>

        <div>
          <label htmlFor="password" className="mb-2 block text-sm font-semibold text-neutral-800">
            Senha
          </label>

          <div className="flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500">
            <Lock className="h-5 w-5 text-neutral-400" />
            <input
              id="password"
              type="password"
              placeholder="Digite sua senha"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400"
              required
            />
          </div>
        </div>

        <div>
          <label
            htmlFor="confirmPassword"
            className="mb-2 block text-sm font-semibold text-neutral-800"
          >
            Confirmar senha
          </label>

          <div className="flex items-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-primary-500">
            <Lock className="h-5 w-5 text-neutral-400" />
            <input
              id="confirmPassword"
              type="password"
              placeholder="Confirme sua senha"
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              className="w-full bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400"
              required
            />
          </div>
        </div>

        {error ? <p className="text-sm font-medium text-red-600">{error}</p> : null}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-primary-500 px-4 py-3 text-sm font-semibold text-white hover:bg-primary-600 transition disabled:cursor-not-allowed disabled:opacity-70"
        >
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-neutral-600">
        Já tem conta?{' '}
        <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-700">
          Entrar
        </Link>
      </p>
    </div>
  );
}
