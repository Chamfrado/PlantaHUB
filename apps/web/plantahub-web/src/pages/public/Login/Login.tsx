import LoginForm from '../../../components/auth/LoginForm';

export default function Login() {
  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-12 md:py-20">
        <div className="grid lg:grid-cols-2 gap-10 items-center">
          {/* Left content */}
          <div className="max-w-xl">
            <span className="inline-flex items-center rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
              Acesse sua conta
            </span>

            <h1 className="mt-6 text-4xl md:text-5xl font-extrabold tracking-tight text-neutral-900">
              Entre na sua conta PlantaHUB
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-neutral-600">
              Gerencie seus projetos, acesse seus downloads e acompanhe suas compras em um só lugar.
            </p>

            <div className="mt-8 grid sm:grid-cols-2 gap-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-5">
                <div className="text-base font-bold text-neutral-900">Downloads imediatos</div>
                <p className="mt-2 text-sm text-neutral-600">
                  Acesse seus arquivos BIM, DWG e PDF sempre que precisar.
                </p>
              </div>

              <div className="rounded-2xl border border-neutral-200 bg-white p-5">
                <div className="text-base font-bold text-neutral-900">Histórico de compras</div>
                <p className="mt-2 text-sm text-neutral-600">
                  Consulte produtos adquiridos e acompanhe seus pedidos.
                </p>
              </div>
            </div>
          </div>

          {/* Right card */}
          <div className="flex justify-center lg:justify-end">
            <LoginForm />
          </div>
        </div>
      </div>
    </section>
  );
}
