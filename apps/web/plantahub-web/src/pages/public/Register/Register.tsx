import RegisterForm from '../../../components/auth/RegisterForm';

export default function Register() {
  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-12 md:py-20">
        <div className="grid lg:grid-cols-2 gap-10 items-center">
          <div className="max-w-xl">
            <span className="inline-flex items-center rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
              Crie sua conta
            </span>

            <h1 className="mt-6 text-4xl md:text-5xl font-extrabold tracking-tight text-neutral-900">
              Comece sua jornada com a PlantaHUB
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-neutral-600">
              Cadastre-se para acessar projetos profissionais, downloads imediatos e acompanhar suas
              compras em um só lugar.
            </p>

            <div className="mt-8 grid sm:grid-cols-2 gap-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-5">
                <div className="text-base font-bold text-neutral-900">Conta centralizada</div>
                <p className="mt-2 text-sm text-neutral-600">
                  Organize seus pedidos, arquivos e favoritos com facilidade.
                </p>
              </div>

              <div className="rounded-2xl border border-neutral-200 bg-white p-5">
                <div className="text-base font-bold text-neutral-900">Acesso rápido</div>
                <p className="mt-2 text-sm text-neutral-600">
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
