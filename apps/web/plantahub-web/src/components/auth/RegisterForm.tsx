import { Chrome, Lock, Mail, User } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { saveAuth } from '../../lib/auth';
import { login, register } from '../../services/auth.service';

export default function RegisterForm() {
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
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

    try {
      setLoading(true);
      setError(null);

      await register({
        fullName,
        email,
        password,
      });

      const auth = await login({ email, password });
      saveAuth(auth.accessToken, auth.tokenType);

      navigate('/');
    } catch (err) {
      console.error(err);
      setError('Não foi possível criar sua conta.');
    } finally {
      setLoading(false);
    }
  }

  function handleGoogleRegister() {
    console.log('register com Google');
  }

  return (
    <div className="w-full max-w-md rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="text-center">
        <h2 className="text-2xl font-extrabold text-neutral-900">Criar conta</h2>
        <p className="mt-2 text-sm text-neutral-600">Cadastre-se para continuar</p>
      </div>

      <button
        type="button"
        onClick={handleGoogleRegister}
        className="mt-6 w-full inline-flex items-center justify-center gap-3 rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm font-semibold text-neutral-800 hover:bg-neutral-50 transition"
      >
        <Chrome className="h-5 w-5" />
        Continuar com Google
      </button>

      <div className="my-6 flex items-center gap-4">
        <div className="h-px flex-1 bg-neutral-200" />
        <span className="text-xs font-medium uppercase tracking-wide text-neutral-400">ou</span>
        <div className="h-px flex-1 bg-neutral-200" />
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
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
