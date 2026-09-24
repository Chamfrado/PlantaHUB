// src/pages/legal/PrivacyPolicyPage.tsx
import { ArrowRight, FileText, Lock, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import SiteSections, { SectionIndex } from '../../components/site/SiteSections';
import { useSitePage } from '../../hooks/useSitePage';

export default function PrivacyPolicyPage() {
  const { content } = useSitePage('privacidade');

  return (
    <div className="bg-white">
      {/* HERO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-14 md:py-18">
          <div className="text-sm text-neutral-500">
            <Link className="hover:text-neutral-900" to="/">
              Home
            </Link>
            <span className="mx-2">/</span>
            <span className="text-neutral-800 font-semibold">Política de Privacidade</span>
          </div>

          <div className="mt-8">
            <span className="inline-flex items-center rounded-full bg-orange-50 text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">
              LGPD
            </span>

            <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-neutral-900 leading-tight">{content.headline}</h1>

            <p className="mt-4 text-neutral-600 leading-relaxed max-w-3xl">{content.intro}</p>

            <div className="mt-6 flex flex-wrap items-center gap-6 text-xs font-semibold text-neutral-600">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" />
                Segurança
              </span>
              <span className="inline-flex items-center gap-2">
                <Lock className="h-4 w-4 text-primary-500" />
                Privacidade
              </span>
              <span className="inline-flex items-center gap-2">
                <FileText className="h-4 w-4 text-primary-500" />
                Transparência
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* CONTENT */}
      <section className="bg-neutral-50">
        <div className="max-w-7xl mx-auto px-6 py-14">
          <div className="grid gap-8 lg:grid-cols-12">
            {/* left nav */}
            <aside className="lg:col-span-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sticky top-6">
                <div className="text-sm font-extrabold text-neutral-900">Nesta página</div>
                <SectionIndex sections={content.sections} />

                <div className="mt-6 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                  <div className="text-xs font-extrabold text-neutral-900">Última atualização</div>
                  <div className="mt-1 text-sm text-neutral-700">27/01/2026</div>
                </div>

                <div className="mt-4">
                  <Link
                    to="/legal/termos"
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 text-white font-semibold py-3 hover:bg-primary-600 transition"
                  >
                    Ver Termos de Serviço <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            </aside>

            {/* main */}
            <main className="lg:col-span-8">
              <SiteSections sections={content.sections} />
            </main>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-primary-500">
        <div className="max-w-7xl mx-auto px-6 py-14 text-center">
          <h2 className="text-3xl font-extrabold text-white">Precisa falar com a gente?</h2>
          <p className="mt-2 text-white/90">
            Para solicitações de privacidade, dúvidas ou suporte, estamos disponíveis.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/contato"
              className="rounded-xl bg-white text-neutral-900 font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2"
            >
              Ir para Contato <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              to="/produtos"
              className="rounded-xl border border-white/40 text-white font-semibold px-6 py-3 hover:bg-white/10 transition"
            >
              Ver catálogo
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

/* ---------- UI helpers ---------- */



