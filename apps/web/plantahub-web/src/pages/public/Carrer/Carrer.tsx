import {
  ArrowRight,
  CheckCircle2,
  FileText,
  Handshake,
  Layers3,
  Mail,
  ShieldCheck,
} from 'lucide-react';
import { sectionByKey, useSitePage, useSiteSettings } from '../../../hooks/useSitePage';

const BENEFIT_ICONS = [
  <Handshake className="h-5 w-5" key="handshake" />,
  <Layers3 className="h-5 w-5" key="layers" />,
  <ShieldCheck className="h-5 w-5" key="shield" />,
];

export default function Carrer() {
  const { content } = useSitePage('trabalhe-conosco');
  const settings = useSiteSettings();

  // Os icones continuam no codigo e sao casados por posicao: eles fazem parte do desenho
  // da pagina, nao do texto que o administrador edita.
  const benefits = (sectionByKey(content, 'beneficios')?.items ?? []).map((item, i) => ({
    ...item,
    icon: BENEFIT_ICONS[i] ?? null,
  }));

  const steps = sectionByKey(content, 'como-funciona')?.items ?? [];
  const requirements = sectionByKey(content, 'requisitos')?.items ?? [];

  const partnerships = settings?.partnershipsEmail ?? '';
  const proposalHref = partnerships
    ? `mailto:${partnerships}?subject=${encodeURIComponent('Quero publicar minhas plantas na PlantaHUB')}`
    : '#';



  return (
    <section className="bg-white">
      <div className="border-b border-neutral-200 bg-brand-light">
        <div className="mx-auto max-w-7xl px-6 py-16 md:py-20">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full bg-orange-50 px-4 py-2 text-sm font-semibold text-primary-600">
              Trabalhe conosco
            </span>

            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-brand-black md:text-5xl">{content.headline}</h1>

            <p className="mt-5 text-lg leading-relaxed text-brand-muted">{content.intro}</p>

            <div className="mt-8 flex flex-wrap gap-3">
              <a
                href={proposalHref}
                className="inline-flex items-center gap-2 rounded-2xl bg-primary-500 px-6 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Enviar proposta
                <ArrowRight className="h-4 w-4" />
              </a>

              <a
                href="#como-funciona"
                className="inline-flex items-center rounded-2xl border border-neutral-300 bg-white px-6 py-3 font-semibold text-brand-black transition hover:bg-neutral-100"
              >
                Entender o processo
              </a>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="grid gap-6 md:grid-cols-3">
          {benefits.map(item => (
            <div
              key={item.title}
              className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-orange-50 text-primary-600">
                {item.icon}
              </div>

              <h2 className="mt-4 text-lg font-extrabold text-brand-black">{item.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-brand-muted">{item.text}</p>
            </div>
          ))}
        </div>
      </div>

      <div id="como-funciona" className="bg-brand-light">
        <div className="mx-auto max-w-7xl px-6 py-16">
          <div className="max-w-3xl">
            <h2 className="text-3xl font-extrabold text-brand-black">{sectionByKey(content, 'como-funciona')?.title}</h2>
            <p className="mt-3 text-brand-muted">
              Nosso processo foi pensado para facilitar a entrada de novos parceiros e garantir
              consistência no catálogo da plataforma.
            </p>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-2">
            {steps.map((step, index) => (
              <div
                key={step.title}
                className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm"
              >
                <div className="text-sm font-extrabold text-primary-600">
                  {String(index + 1).padStart(2, '0')}
                </div>
                <h3 className="mt-2 text-xl font-extrabold text-brand-black">{step.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-brand-muted">{step.text}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-orange-50 text-primary-600">
                <FileText className="h-5 w-5" />
              </div>
              <h2 className="text-2xl font-extrabold text-brand-black">{sectionByKey(content, 'requisitos')?.title}</h2>
            </div>

            <div className="mt-6 space-y-4">
              {requirements.map(item => (
                <div key={item.text} className="flex items-start gap-3">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-green-600" />
                  <p className="text-sm leading-relaxed text-brand-muted">{item.text}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-neutral-200 bg-brand-light p-8 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-white text-primary-600">
                <Mail className="h-5 w-5" />
              </div>
              <h2 className="text-2xl font-extrabold text-brand-black">Canal de contato</h2>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-brand-muted">
              Envie sua apresentação, links, portfólio e uma breve explicação sobre os tipos de
              plantas que você deseja comercializar.
            </p>

            <div className="mt-6 rounded-2xl border border-neutral-200 bg-white p-5">
              <div className="text-xs font-semibold uppercase tracking-wide text-brand-muted">
                E-mail sugerido
              </div>
              <a
                href={proposalHref}
                className="mt-2 block break-words text-lg font-extrabold text-primary-600 hover:underline"
              >
                parcerias@plantahub.com.br
              </a>
            </div>

            <div className="mt-6">
              <a
                href={proposalHref}
                className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Entrar em contato
                <ArrowRight className="h-4 w-4" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
