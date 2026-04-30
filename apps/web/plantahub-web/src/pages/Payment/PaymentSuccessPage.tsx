import { CheckCircle2, Clock, Library } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function PaymentSuccessPage() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-20">
      <section className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-50 text-orange-600">
          <CheckCircle2 className="h-7 w-7" />
        </div>

        <p className="mt-6 text-sm font-semibold uppercase tracking-wide text-orange-600">
          Pagamento recebido
        </p>

        <h1 className="mt-3 text-3xl font-bold text-neutral-950">
          Estamos confirmando seu pagamento
        </h1>

        <p className="mt-4 text-base leading-7 text-neutral-600">
          Você retornou do checkout da InfinitePay. Assim que a confirmação for processada, seus
          arquivos serão liberados automaticamente na sua biblioteca.
        </p>

        <div className="mt-6 rounded-2xl border border-orange-100 bg-orange-50 p-4">
          <div className="flex gap-3">
            <Clock className="mt-0.5 h-5 w-5 shrink-0 text-orange-600" />
            <p className="text-sm leading-6 text-neutral-700">
              Se os arquivos ainda não aparecerem, aguarde alguns instantes e atualize sua
              biblioteca. A liberação depende da confirmação enviada pela InfinitePay.
            </p>
          </div>
        </div>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link
            to="/biblioteca"
            className="inline-flex items-center justify-center gap-2 rounded-2xl bg-orange-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-orange-700"
          >
            <Library className="h-4 w-4" />
            Ir para minha biblioteca
          </Link>

          <Link
            to="/carrinho"
            className="inline-flex items-center justify-center rounded-2xl border border-neutral-200 px-5 py-3 text-sm font-bold text-neutral-700 transition hover:bg-neutral-50"
          >
            Voltar ao carrinho
          </Link>
        </div>
      </section>
    </main>
  );
}
