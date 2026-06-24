import RegisterForm from '../../../components/auth/RegisterForm';

export default function Register() {
  return (
    <section className="min-h-[calc(100vh-64px)] bg-brand-light">
      <div className="max-w-7xl mx-auto px-6 py-12 md:py-20">
        <div className="grid lg:grid-cols-2 gap-10 items-center">
          <div className="max-w-xl">
            <span className="inline-flex items-center rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary-600 border border-orange-100">
              Crie sua conta
            </span>

            <h1 className="mt-6 text-4xl md:text-5xl font-extrabold tracking-tight text-brand-black">
              Comece sua jornada com a PlantaHUB
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-brand-muted">
              Cadastre-se para acessar projetos profissionais, downloads imediatos e acompanhar suas
              compras em um só lugar.
            </p>

            <div className="mt-8 grid sm:grid-cols-2 gap-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                <div className="text-base font-bold text-brand-black">Conta centralizada</div>
                <p className="mt-2 text-sm text-brand-muted">
                  Organize seus pedidos, arquivos e favoritos com facilidade.
                </p>
              </div>

              <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                <div className="text-base font-bold text-brand-black">Acesso rápido</div>
                <p className="mt-2 text-sm text-brand-muted">
                  Entre e baixe seus projetos sempre que precisar.
                </p>
              </div>
            </div>
          </div>

          <div className="flex justify-center lg:justify-end">
            <RegisterForm />
          </div>
        </div>
      </div>
    </section>
  );
}
