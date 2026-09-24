import ForgotPasswordForm from '../../../components/auth/ForgotPasswordForm';

export default function ForgotPassword() {
  return (
    <section className="min-h-[calc(100vh-64px)] bg-brand-light">
      <div className="max-w-7xl mx-auto px-6 py-12 md:py-20">
        <div className="grid lg:grid-cols-2 gap-10 items-center">
          {/* Left content */}
          <div className="max-w-xl">
            <span className="inline-flex items-center rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary-600 border border-orange-100">
              Recuperar acesso
            </span>

            <h1 className="mt-6 text-4xl md:text-5xl font-extrabold tracking-tight text-brand-black">
              Esqueceu sua senha?
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-brand-muted">
              Enviamos um código de 6 dígitos para o seu e-mail ou celular cadastrado. Com ele, você
              define uma senha nova em poucos passos.
            </p>

            <div className="mt-8 grid sm:grid-cols-2 gap-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                <div className="text-base font-bold text-brand-black">Código temporário</div>
                <p className="mt-2 text-sm text-brand-muted">
                  O código vale por poucos minutos e só pode ser usado uma vez.
                </p>
              </div>

              <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                <div className="text-base font-bold text-brand-black">Sessões encerradas</div>
                <p className="mt-2 text-sm text-brand-muted">
                  Ao trocar a senha, todos os acessos abertos com a senha antiga são encerrados.
                </p>
              </div>
            </div>
          </div>

          {/* Right card */}
          <div className="flex justify-center lg:justify-end">
            <ForgotPasswordForm />
          </div>
        </div>
      </div>
    </section>
  );
}
