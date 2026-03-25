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
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 py-14 md:py-20">
        <div className="grid md:grid-cols-2 gap-10 items-center">
          {/* LEFT */}
          <div>
            {/* badge */}
            <div className="inline-flex items-center gap-2 rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-full text-white">
                <Zap className="text-primary-500" />
              </span>
              Reduza o planejamento da sua construção em até 60%
            </div>

            {/* title */}
            <h1 className="mt-8 text-4xl md:text-5xl font-extrabold tracking-tight text-neutral-900 whitespace-pre-line">
              {title}
            </h1>

            {/* subtitle */}
            <p className="mt-6 text-base md:text-lg leading-relaxed text-neutral-600 max-w-xl">
              {subtitle}
            </p>

            {/* CTAs */}
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <button
                onClick={onPrimaryClick}
                className="px-6 py-3 rounded-xl bg-primary-500 text-white font-semibold shadow-sm hover:bg-primary-600 transition cursor-pointer"
              >
                {primaryCtaText}
              </button>

              <button
                onClick={onSecondaryClick}
                className="px-6 py-3 rounded-xl border border-neutral-300 bg-white text-neutral-900 font-semibold cursor-pointer hover:bg-neutral-50 transition"
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
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-orange-50 text-success-500">
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
          <div className="relative">
            {/* soft background block (como no print) */}
            <div className="absolute -right-2 -bottom-3 md:-right-6 md:-bottom-6 h-[88%] w-[88%] rounded-3xl bg-orange-50" />

            {/* image card */}
            <div className="relative rounded-3xl bg-white shadow-lg border border-neutral-100 overflow-hidden">
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
        <div className="text-sm font-semibold text-neutral-900">{title}</div>
        <div className="text-xs text-neutral-500 mt-1">{desc}</div>
      </div>
    </div>
  );
}
