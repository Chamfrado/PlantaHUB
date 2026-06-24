import { Check, Leaf, Timer, Zap } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

type HeroProps = {
  title?: string;
  subtitle?: string;
  primaryCtaText?: string;
  secondaryCtaText?: string;
  imageSrc?: string;
};

export default function Hero({
  title = 'Projetos Arquitetônicos\nProntos para Construir',
  subtitle = ' Transforme sua obra em realidade com agilidade e segurança. Projetos completos em PDF, desenvolvidos por profissionais e prontos para execução. Escolha, adquira e comece a construir hoje mesmo.',
  primaryCtaText = 'Explorar nossos Projetos',
  secondaryCtaText = 'Como Funciona',
  imageSrc,
}: HeroProps) {
  const navigate = useNavigate();

  const onPrimaryClick = () => navigate('/produtos?category=casas');
  const onSecondaryClick = () => navigate('/sobre');

  return (
    <section className="overflow-hidden bg-brand-light">
      <div className="mx-auto w-full max-w-7xl px-6 py-14 md:py-20">
        <div className="grid min-w-0 md:grid-cols-2 gap-10 items-center">
          {/* LEFT */}
          <div className="min-w-0 max-w-[calc(100vw-3rem)] md:max-w-none">
            {/* badge */}
            <div className="inline-flex w-full max-w-full items-start gap-2 rounded-2xl border border-orange-100 bg-white px-4 py-2 text-sm font-semibold text-primary-600 sm:w-auto sm:items-center sm:rounded-full">
              <span className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-white">
                <Zap className="text-primary-500" />
              </span>
              <span className="min-w-0 break-words leading-snug">Reduza o planejamento da sua construção em até 60%</span>
            </div>

            {/* title */}
            <h1 className="mt-8 text-4xl md:text-5xl font-extrabold tracking-tight text-brand-black whitespace-pre-line">
              {title}
            </h1>

            {/* subtitle */}
            <p className="mt-6 text-base md:text-lg leading-relaxed text-brand-muted max-w-xl">
              {subtitle}
            </p>

            {/* CTAs */}
            <div className="mt-8 grid gap-4 sm:flex sm:flex-wrap sm:items-center">
              <button
                onClick={onPrimaryClick}
                className="w-full px-6 py-3 rounded-xl bg-primary-500 text-white font-semibold shadow-sm hover:bg-primary-600 transition cursor-pointer sm:w-auto"
              >
                {primaryCtaText}
              </button>

              <button
                onClick={onSecondaryClick}
                className="w-full px-6 py-3 rounded-xl border border-neutral-300 bg-white text-brand-black font-semibold cursor-pointer hover:border-brand-black hover:bg-white transition sm:w-auto"
              >
                {secondaryCtaText}
              </button>
            </div>

            {/* mini features */}

            <div className="mt-10 grid sm:grid-cols-3 gap-6">
              <FeatureItem
                title="Projetos Aprovados"
                desc="Projetos acompanham ART e RRT. Garantindo Conformidade e Segurança"
                icon={
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-orange-50 text-primary-600">
                    <Check />
                  </span>
                }
              />
              <FeatureItem
                title="Design Inteligente"
                desc="Soluções Concientes e Funcionais"
                icon={
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-green-50 text-brand-green">
                    <Leaf />
                  </span>
                }
              />
              <FeatureItem
                title="Download Imediato"
                desc="Acesso instantâneo após a compra"
                icon={
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-orange-50 text-primary-600">
                    <Timer />
                  </span>
                }
              />
            </div>
          </div>

          {/* RIGHT */}
          <div className="relative min-w-0 max-w-[calc(100vw-3rem)] md:max-w-none">
            {/* soft background block (como no print) */}
            <div className="absolute -right-2 -bottom-3 md:-right-6 md:-bottom-6 h-[88%] w-[88%] rounded-3xl bg-primary-500" />

            {/* image card */}
            <div className="relative rounded-3xl bg-white shadow-lg border border-neutral-200 overflow-hidden">
              <div className="aspect-4/3 w-full bg-neutral-50">
                {imageSrc ? (
                  <img
                    src={imageSrc}
                    alt="Project preview"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="h-full w-full flex items-center justify-center text-neutral-400">
                    Image/3D Preview here
                  </div>
                )}
              </div>

              {/* opcional: pequena “moldura” interna */}
              <div className="h-6 bg-white" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function FeatureItem({
  title,
  desc,
  icon,
}: {
  title: string;
  desc: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-3">
      {icon}
      <div>
        <div className="text-sm font-semibold text-brand-black">{title}</div>
        <div className="text-xs text-brand-muted mt-1">{desc}</div>
      </div>
    </div>
  );
}
